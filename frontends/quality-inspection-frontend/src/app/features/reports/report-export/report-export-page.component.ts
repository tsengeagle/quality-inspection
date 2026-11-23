import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subject, finalize, merge, takeUntil } from 'rxjs';

import { ActorContext } from '@core/actor-context/actor-context.model';
import { ActorContextService } from '@core/actor-context/actor-context.service';
import { AccessControlService } from '@shared/access/access-control.service';
import { formatTaipeiDateTime } from '@shared/time/taipei-time';
import { CsvFileSaver } from '../services/csv-file-saver.service';
import {
  ExportCriteria,
  ExportDownloadError,
  ExportPageResult,
  ExportRowView,
  REVIEW_COMPLETION,
  ReportExportService
} from '../services/report-export.service';
import { EXPORT_COLUMNS, ExportField, MASKED_LABEL, buildExportCsv, exportCsvFilename, isMasked } from './export-csv';
import { isOrderedRange, previousFullMonth, toExportRange } from './export-date-range';

/** UI-017 §3.4：預覽表格每頁筆數（API 預設）。 */
export const EXPORT_PREVIEW_PAGE_SIZE = 100;

export type ExportPageStatus = 'initial' | 'loading' | 'loaded' | 'error' | 'forbidden';

export interface ExportForm {
  reportType: string;
  startDate: string;
  endDate: string;
  hospitalId: string;
}

/** 已送出的查詢條件；換頁沿用，不受表單後續修改影響。 */
interface AppliedQuery {
  startDate: string;
  endDate: string;
  hospitalId: string;
  criteria: ExportCriteria;
}

const EMPTY_VALUE = '—';
const TIME_FIELDS: readonly ExportField[] = ['createdAt', 'closedAt'];

@Component({
  selector: 'app-report-export-page',
  templateUrl: './report-export-page.component.html',
  styleUrls: ['./report-export-page.component.scss']
})
export class ReportExportPageComponent implements OnInit, OnDestroy {

  readonly columns = EXPORT_COLUMNS;
  readonly reportTypes = [{ value: REVIEW_COMPLETION, label: '審查完成' }];

  form: ExportForm = { reportType: REVIEW_COMPLETION, startDate: '', endDate: '', hospitalId: '' };

  isSystemAdmin = false;
  status: ExportPageStatus = 'initial';
  result: ExportPageResult | null = null;
  errorMessage = '';
  applied: AppliedQuery | null = null;

  downloading = false;
  downloadProgress: { done: number; total: number } | null = null;
  downloadError = '';

  private readonly destroy$ = new Subject<void>();
  /** 身分或授權範圍改變時發出，用來取消上一個身分發出的查詢與下載（D-033）。 */
  private readonly contextReset$ = new Subject<void>();
  private readonly cancel$ = merge(this.destroy$, this.contextReset$);
  private contextKey: string | null = null;

  constructor(
    private readonly exportService: ReportExportService,
    private readonly fileSaver: CsvFileSaver,
    private readonly actorContextService: ActorContextService,
    private readonly accessControlService: AccessControlService
  ) {}

  ngOnInit(): void {
    const range = previousFullMonth(new Date());
    this.form.startDate = range.startDate;
    this.form.endDate = range.endDate;

    this.actorContextService.context$
      .pipe(takeUntil(this.destroy$))
      .subscribe(context => this.applyActorContext(context));
  }

  /**
   * 院區條件依「是否持有 SYSTEM_ADMIN」決定，與後端 ActorContext.readScopeHospitalId 一致（多角色帳號也是）。
   * 身分、院區或角色改變時，取消進行中的請求並清除結果，不保留上一個身分取得的資料。
   */
  private applyActorContext(context: ActorContext): void {
    this.isSystemAdmin = this.accessControlService.hasAnyRole('SYSTEM_ADMIN', context);
    const key = JSON.stringify([
      context.userId, context.hospitalId, context.currentRole, [...context.roles].sort()
    ]);
    if (this.contextKey !== null && this.contextKey !== key) {
      this.resetForNewContext();
    }
    this.contextKey = key;
  }

  private resetForNewContext(): void {
    this.contextReset$.next();
    this.status = 'initial';
    this.result = null;
    this.applied = null;
    this.errorMessage = '';
    this.downloading = false;
    this.downloadProgress = null;
    this.downloadError = '';
    if (!this.isSystemAdmin) {
      this.form.hospitalId = '';
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get rangeValid(): boolean {
    return isOrderedRange(this.form.startDate, this.form.endDate);
  }

  get controlsLocked(): boolean {
    return this.status === 'loading' || this.downloading;
  }

  get canQuery(): boolean {
    return this.rangeValid && !this.controlsLocked;
  }

  /** 只有「目前表單條件」查過且為零筆時才停用下載；條件改了就不沿用舊結果（UI-017 §6）。 */
  get canDownload(): boolean {
    const knownEmpty = this.status === 'loaded'
      && this.result?.totalElements === 0
      && this.formMatchesApplied();
    return this.rangeValid && !this.controlsLocked && !knownEmpty;
  }

  get scopeLabel(): string {
    if (!this.applied) {
      return '';
    }
    if (!this.isSystemAdmin) {
      return '本院';
    }
    return this.applied.hospitalId || '全部院區';
  }

  get hasPreviousPage(): boolean {
    return !!this.result && this.result.page > 0;
  }

  get hasNextPage(): boolean {
    return !!this.result && this.result.page + 1 < this.result.totalPages;
  }

  query(): void {
    if (!this.canQuery) {
      return;
    }
    const hospitalId = this.currentHospitalId();
    this.applied = {
      startDate: this.form.startDate,
      endDate: this.form.endDate,
      hospitalId,
      criteria: this.buildCriteria(hospitalId)
    };
    this.loadPage(0);
  }

  goToPage(page: number): void {
    if (!this.applied || this.controlsLocked || page < 0 || (this.result && page >= this.result.totalPages)) {
      return;
    }
    this.loadPage(page);
  }

  download(): void {
    if (!this.canDownload) {
      return;
    }
    const startDate = this.form.startDate;
    const endDate = this.form.endDate;
    const hospitalId = this.currentHospitalId();
    const criteria = this.buildCriteria(hospitalId);

    this.downloading = true;
    this.downloadError = '';
    this.downloadProgress = { done: 0, total: 0 };

    this.exportService
      .fetchAllRows(criteria, (done, total) => (this.downloadProgress = { done, total }))
      .pipe(
        takeUntil(this.cancel$),
        finalize(() => {
          this.downloading = false;
          this.downloadProgress = null;
        })
      )
      .subscribe({
        next: rows => this.fileSaver.save(exportCsvFilename(startDate, endDate), buildExportCsv(rows)),
        error: error => {
          // UI-017 §6：403 一律進入無權限狀態，並清除先前預覽的資料。
          if (error instanceof HttpErrorResponse && error.status === 403) {
            this.handleQueryError(error);
            return;
          }
          this.downloadError = error instanceof ExportDownloadError && error.reason === 'DATA_CHANGED'
            ? '資料在下載期間有變動，請重新下載'
            : '下載失敗，未產生檔案，請稍後再試';
        }
      });
  }

  /** 表格儲存格：被遮罩顯示「已遮罩」，空值顯示「—」；createdAt、closedAt 以台北時間顯示（D-075 第 5、6 點）。 */
  cellText(row: ExportRowView, field: ExportField): string {
    if (isMasked(row, field)) {
      return MASKED_LABEL;
    }
    const value = row[field];
    if (value === null || value === undefined || value === '') {
      return EMPTY_VALUE;
    }
    return TIME_FIELDS.includes(field) ? formatTaipeiDateTime(String(value), EMPTY_VALUE, true) : String(value);
  }

  /** 有值、沒被遮罩的時間欄位：用 time 元素，datetime 屬性保留 API 回傳的 UTC 值。 */
  isTimeCell(row: ExportRowView, field: ExportField): boolean {
    return TIME_FIELDS.includes(field) && !!row[field] && !isMasked(row, field);
  }

  isMaskedCell(row: ExportRowView, field: ExportField): boolean {
    return isMasked(row, field);
  }

  percent(rate: number): string {
    return `${(rate * 100).toFixed(1)}%`;
  }

  trackByCaseId(_index: number, row: ExportRowView): string {
    return row.caseId;
  }

  private currentHospitalId(): string {
    return this.isSystemAdmin ? this.form.hospitalId.trim() : '';
  }

  private formMatchesApplied(): boolean {
    return !!this.applied
      && this.applied.startDate === this.form.startDate
      && this.applied.endDate === this.form.endDate
      && this.applied.hospitalId === this.currentHospitalId();
  }

  private buildCriteria(hospitalId: string): ExportCriteria {
    const range = toExportRange(this.form.startDate, this.form.endDate);
    return hospitalId ? { ...range, hospitalId } : range;
  }

  private loadPage(page: number): void {
    const applied = this.applied;
    if (!applied) {
      return;
    }
    this.status = 'loading';
    this.errorMessage = '';

    this.exportService
      .fetchPage({ ...applied.criteria, page, size: EXPORT_PREVIEW_PAGE_SIZE })
      .pipe(takeUntil(this.cancel$))
      .subscribe({
        next: result => {
          this.result = result;
          this.status = 'loaded';
        },
        error: (error: unknown) => this.handleQueryError(error)
      });
  }

  private handleQueryError(error: unknown): void {
    this.result = null;
    if (error instanceof HttpErrorResponse && error.status === 403) {
      this.status = 'forbidden';
      return;
    }
    this.status = 'error';
    const backendMessage = error instanceof HttpErrorResponse && error.status === 400
      ? error.error?.error?.message
      : null;
    this.errorMessage = typeof backendMessage === 'string' && backendMessage.trim()
      ? backendMessage
      : '查詢失敗，請稍後再試';
  }
}
