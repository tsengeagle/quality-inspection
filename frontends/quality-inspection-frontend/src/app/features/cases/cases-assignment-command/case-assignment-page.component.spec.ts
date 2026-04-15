import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';

import { CaseSummaryHeaderComponent } from '../components/case-summary-header/case-summary-header.component';
import { ModeIndicatorStripComponent } from '../components/mode-indicator-strip/mode-indicator-strip.component';
import { ProjectionNoticeBannerComponent } from '../components/projection-notice-banner/projection-notice-banner.component';
import { StatusOrBlockedBannerComponent } from '../components/status-or-blocked-banner/status-or-blocked-banner.component';
import { CaseAssignmentContext, CaseAssignmentPageComponent } from './case-assignment-page.component';

describe('CaseAssignmentPageComponent', () => {
  let fixture: ComponentFixture<CaseAssignmentPageComponent>;
  let component: CaseAssignmentPageComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [
        CaseAssignmentPageComponent,
        CaseSummaryHeaderComponent,
        ProjectionNoticeBannerComponent,
        StatusOrBlockedBannerComponent,
        ModeIndicatorStripComponent
      ],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap({ caseId: 'CASE-011' })
            }
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CaseAssignmentPageComponent);
    component = fixture.componentInstance;
  });

  function buildContext(overrides: Partial<CaseAssignmentContext> = {}): CaseAssignmentContext {
    return {
      caseId: 'CASE-011',
      caseTitle: '案件分派測試骨架',
      status: 'SELECTED_PENDING_ASSIGNMENT',
      stageLabel: '待分派',
      reviewerAssigned: false,
      rereviewStageActive: false,
      blockedReason: null,
      dueDateHint: '截止日 helper slot 目前僅提供 placeholder，不含互動。',
      ...overrides
    };
  }

  function render(context: CaseAssignmentContext): void {
    fixture.detectChanges();
    component.applyContext(context);
    fixture.detectChanges();
  }

  it('covers ASSIGN_INITIAL baseline with reviewer section and A3 submit zone', () => {
    render(buildContext());

    expect(component.mode).toBe('ASSIGN_INITIAL');
    expect(fixture.nativeElement.querySelector('.reviewer-assignment-section')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('.rereviewer-assignment-section')).toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="assignment-submit-zone"]')).not.toBeNull();
  });

  it('covers REASSIGN_INITIAL baseline and keeps reviewer section visible', () => {
    render(buildContext({ reviewerAssigned: true, reviewerName: 'Dr. Initial' }));

    expect(component.mode).toBe('REASSIGN_INITIAL');
    expect(fixture.nativeElement.textContent).toContain('Dr. Initial');
    expect(fixture.nativeElement.querySelector('.reviewer-assignment-section')).not.toBeNull();
  });

  it('covers MIXED_ASSIGNMENT baseline and shows both reviewer/rereviewer sections', () => {
    render(buildContext({ reviewerAssigned: true, rereviewStageActive: true, rereviewerName: 'Dr. Rereview' }));

    expect(component.mode).toBe('MIXED_ASSIGNMENT');
    expect(fixture.nativeElement.querySelector('.reviewer-assignment-section')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('.rereviewer-assignment-section')).not.toBeNull();
  });

  it('covers READONLY_BLOCKED baseline and shows the blocked banner only', () => {
    render(buildContext({ status: 'CLOSED', blockedReason: 'CASE_ALREADY_CLOSED' }));

    expect(component.mode).toBe('READONLY_BLOCKED');
    expect(fixture.nativeElement.querySelector('app-status-or-blocked-banner')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('.reviewer-assignment-section')).toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="assignment-submit-zone"]')).toBeNull();
  });

  it('keeps picker and due-date helper slots as non-functional placeholders', () => {
    render(buildContext());

    expect(fixture.nativeElement.querySelector('[data-testid="reviewer-picker-slot"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="assignment-due-date-helper-slot"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('button[disabled]')?.textContent).toContain('placeholder');
  });
});
