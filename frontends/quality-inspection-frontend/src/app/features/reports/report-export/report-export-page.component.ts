import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subject, finalize, takeUntil } from 'rxjs';

import { ActorContextService } from '@core/actor-context/actor-context.service';
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

  constructor(
    private readonly exportService: ReportExportService,
    private readonly fileSaver: CsvFileSaver,
    private readonly actorContextService: ActorContextService
  ) {}

  ngOnInit(): void {
    const range = previousFullMonth(new Date());
    this.form.startDate = range.startDate;
    this.form.endDate = range.endDate;

    this.actorContextService.context$
      .pipe(takeUntil(this.destroy$))
      .subscribe(context => {
        this.isSystemAdmin = context.currentRole === 'SYSTEM_ADMIN';
      });
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

  get canDownload(): boolean {
    const emptyResult = this.status === 'loaded' && this.result?.totalElements === 0;
    return this.rangeValid && !this.controlsLocked && !emptyResult;
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
    const hospitalId = this.isSystemAdmin ? this.form.hospitalId.trim() : '';
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
    const hospitalId = this.isSystemAdmin ? this.form.hospitalId.trim() : '';
    const criteria = this.buildCriteria(hospitalId);

    this.downloading = true;
    this.downloadError = '';
    this.downloadProgress = { done: 0, total: 0 };

    this.exportService
      .fetchAllRows(criteria, (done, total) => (this.downloadProgress = { done, total }))
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.downloading = false;
          this.downloadProgress = null;
        })
      )
      .subscribe({
        next: rows => this.fileSaver.save(exportCsvFilename(startDate, endDate), buildExportCsv(rows)),
        error: error => {
          this.downloadError = error instanceof ExportDownloadError && error.reason === 'DATA_CHANGED'
            ? '資料在下載期間有變動，請重新下載'
            : '下載失敗，未產生檔案，請稍後再試';
        }
      });
  }

  /** 表格儲存格：被遮罩顯示「已遮罩」，空值顯示「—」；closedAt 原樣顯示。 */
  cellText(row: ExportRowView, field: ExportField): string {
    if (isMasked(row, field)) {
      return MASKED_LABEL;
    }
    const value = row[field];
    return value === null || value === undefined || value === '' ? EMPTY_VALUE : String(value);
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
      .pipe(takeUntil(this.destroy$))
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
