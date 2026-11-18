import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subject, merge, takeUntil } from 'rxjs';

import { ActorContext, CanonicalActorRole } from '@core/actor-context/actor-context.model';
import { ActorContextService } from '@core/actor-context/actor-context.service';
import { AccessControlService } from '@shared/access/access-control.service';
import { isOrderedRange, previousFullMonth, toExportRange } from '../report-export/export-date-range';
import { DashboardCriteria, DashboardSummary, ReportDashboardService } from '../services/report-dashboard.service';

/** 與 route data 相同的角色（UI-015 §1）；自動查詢前再確認一次，避免在身分切換的中間狀態送出請求。 */
const REPORT_ROLES: CanonicalActorRole[] = ['HOSPITAL_MANAGER', 'AUDITOR', 'SYSTEM_ADMIN'];

export type DashboardPageStatus = 'idle' | 'loading' | 'loaded' | 'error' | 'forbidden';

/** none：兩個日期都空（不限期間）；partial：只填一個；reversed：起日晚於迄日或日期不合法。 */
export type DashboardRangeState = 'none' | 'valid' | 'partial' | 'reversed';

export interface DashboardForm {
  startDate: string;
  endDate: string;
  projectId: string;
  hospitalId: string;
}

interface AppliedQuery {
  startDate: string;
  endDate: string;
  projectId: string;
  hospitalId: string;
}

type CountField = 'totalCases' | 'closedCases' | 'rereviewedCases' | 'overdueCases';
type RateField = 'completionRate' | 'rereviewRate' | 'overdueRate';

export interface CountCard {
  field: CountField;
  label: string;
  hint: string;
}

export interface RateCard {
  field: RateField;
  label: string;
  numerator: CountField;
  hint: string;
}

/** UI-015 §3.3：卡片與說明文字。 */
export const COUNT_CARDS: readonly CountCard[] = [
  { field: 'totalCases', label: '總案件數', hint: '有日期區間時，只計入建立時間在區間內的案件' },
  { field: 'closedCases', label: '已結案', hint: '狀態為已結案' },
  { field: 'rereviewedCases', label: '有複審', hint: '已指派複審醫師（不代表複審已完成）' },
  { field: 'overdueCases', label: '逾期', hint: '查詢當下已超過期限、且尚未結案或取消' }
];

export const RATE_CARDS: readonly RateCard[] = [
  { field: 'completionRate', label: '完成率', numerator: 'closedCases', hint: '已結案 ÷ 總案件數' },
  { field: 'rereviewRate', label: '複審率', numerator: 'rereviewedCases', hint: '有複審 ÷ 總案件數' },
  { field: 'overdueRate', label: '逾期率', numerator: 'overdueCases', hint: '逾期 ÷ 總案件數' }
];

@Component({
  selector: 'app-report-dashboard-page',
  templateUrl: './report-dashboard-page.component.html',
  styleUrls: ['./report-dashboard-page.component.scss']
})
export class ReportDashboardPageComponent implements OnInit, OnDestroy {

  readonly countCards = COUNT_CARDS;
  readonly rateCards = RATE_CARDS;

  form: DashboardForm = { startDate: '', endDate: '', projectId: '', hospitalId: '' };

  isSystemAdmin = false;
  /** 目前 context 已解析且持有報表角色；手動與自動查詢都以此為前提。 */
  reportAuthorized = false;
  status: DashboardPageStatus = 'idle';
  summary: DashboardSummary | null = null;
  errorMessage = '';
  applied: AppliedQuery | null = null;

  private readonly destroy$ = new Subject<void>();
  /** 身分或授權範圍改變時發出，用來取消上一個身分發出的查詢（D-033）。 */
  private readonly contextReset$ = new Subject<void>();
  private readonly cancel$ = merge(this.destroy$, this.contextReset$);
  private contextKey: string | null = null;

  constructor(
    private readonly dashboardService: ReportDashboardService,
    private readonly actorContextService: ActorContextService,
    private readonly accessControlService: AccessControlService
  ) {}

  ngOnInit(): void {
    const range = previousFullMonth(new Date());
    this.form.startDate = range.startDate;
    this.form.endDate = range.endDate;

    // context$ 是 BehaviorSubject：第一次發出時即以預設條件查詢（UI-015 §3.1）。
    this.actorContextService.context$
      .pipe(takeUntil(this.destroy$))
      .subscribe(context => this.applyActorContext(context));
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get rangeState(): DashboardRangeState {
    const start = this.form.startDate;
    const end = this.form.endDate;
    if (!start && !end) {
      return 'none';
    }
    if (!start || !end) {
      return 'partial';
    }
    return isOrderedRange(start, end) ? 'valid' : 'reversed';
  }

  get controlsLocked(): boolean {
    return this.status === 'loading';
  }

  get canQuery(): boolean {
    const state = this.rangeState;
    return this.reportAuthorized && (state === 'none' || state === 'valid') && !this.controlsLocked;
  }

  get rangeLabel(): string {
    if (!this.applied) {
      return '';
    }
    return this.applied.startDate
      ? `${this.applied.startDate} ～ ${this.applied.endDate}（含迄日）`
      : '不限期間';
  }

  get projectLabel(): string {
    return this.applied?.projectId || '全部專案';
  }

  get scopeLabel(): string {
    if (!this.isSystemAdmin) {
      return '本院';
    }
    return this.applied?.hospitalId || '全部院區';
  }

  clearRange(): void {
    if (this.controlsLocked) {
      return;
    }
    this.form.startDate = '';
    this.form.endDate = '';
  }

  query(): void {
    if (!this.canQuery) {
      return;
    }
    const applied: AppliedQuery = {
      startDate: this.form.startDate,
      endDate: this.form.endDate,
      projectId: this.form.projectId.trim(),
      hospitalId: this.isSystemAdmin ? this.form.hospitalId.trim() : ''
    };
    this.applied = applied;
    this.status = 'loading';
    this.errorMessage = '';
    this.summary = null;

    this.dashboardService
      .fetchSummary(this.buildCriteria(applied))
      .pipe(takeUntil(this.cancel$))
      .subscribe({
        next: summary => {
          this.summary = summary;
          this.status = 'loaded';
        },
        error: (error: unknown) => this.handleError(error)
      });
  }

  count(field: CountField): number {
    return this.summary ? this.summary[field] : 0;
  }

  /** 比率取自 API（0～1），只做顯示格式，不重算。 */
  percent(field: RateField): string {
    return this.summary ? `${(this.summary[field] * 100).toFixed(1)}%` : '';
  }

  /**
   * 院區條件依「是否持有 SYSTEM_ADMIN」決定，與後端 ActorContext.readScopeHospitalId 一致（多角色帳號也是）。
   * 身分、院區或角色改變時，取消進行中的查詢、清除數字，再以目前條件重新查詢。
   */
  private applyActorContext(context: ActorContext): void {
    this.isSystemAdmin = this.accessControlService.hasAnyRole('SYSTEM_ADMIN', context);
    this.reportAuthorized = this.canAutoQuery(context);
    const key = JSON.stringify([
      context.userId, context.hospitalId, context.currentRole, [...context.roles].sort()
    ]);
    const first = this.contextKey === null;
    const changed = !first && this.contextKey !== key;
    this.contextKey = key;

    if (changed) {
      this.contextReset$.next();
      this.summary = null;
      this.applied = null;
      this.errorMessage = '';
      this.status = 'idle';
      if (!this.isSystemAdmin) {
        this.form.hospitalId = '';
      }
    }
    if (first || changed) {
      // hosted 模式下 shell 的使用者、角色清單、目前角色是分開送達的：
      // 身分尚未解析完成、或不持有報表角色時不送查詢，等下一次 context 更新再查。
      if (this.reportAuthorized) {
        this.query();
      } else {
        this.status = 'idle';
      }
    }
  }

  private canAutoQuery(context: ActorContext): boolean {
    return this.accessControlService.isResolvedContext(context)
      && this.accessControlService.hasAnyRole(REPORT_ROLES, context);
  }

  private buildCriteria(applied: AppliedQuery): DashboardCriteria {
    const criteria: DashboardCriteria = {};
    if (applied.startDate && applied.endDate) {
      Object.assign(criteria, toExportRange(applied.startDate, applied.endDate));
    }
    if (applied.projectId) {
      criteria.projectId = applied.projectId;
    }
    if (applied.hospitalId) {
      criteria.hospitalId = applied.hospitalId;
    }
    return criteria;
  }

  private handleError(error: unknown): void {
    this.summary = null;
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
