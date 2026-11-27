import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CASE_STATUSES } from '../../../models/case-status';
import { CaseWorkQueueFilterBarComponent } from './case-work-queue-filter-bar.component';

describe('CaseWorkQueueFilterBarComponent', () => {
  let fixture: ComponentFixture<CaseWorkQueueFilterBarComponent>;
  let component: CaseWorkQueueFilterBarComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ declarations: [CaseWorkQueueFilterBarComponent] }).compileComponents();
    fixture = TestBed.createComponent(CaseWorkQueueFilterBarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // RC-LST-02：後端只接受 CaseStatus 的值，其他值回 400 BAD_REQUEST。
  // 原本的「待處理」「進行中」送出 PENDING、IN_PROGRESS，不是後端的狀態值。
  it('only offers statuses the backend accepts', () => {
    const values = component.availableStatuses.map(item => item.value);

    expect(values.length).toBeGreaterThan(0);
    for (const value of values) {
      expect(CASE_STATUSES as readonly string[]).withContext(value).toContain(value);
    }
  });

  it('offers the single-status filters of UI-009 §4.2', () => {
    expect(component.availableStatuses).toEqual([
      { value: 'SELECTED_PENDING_ASSIGNMENT', label: '待分派' },
      { value: 'PENDING_INITIAL_REVIEW', label: '待初審' },
      { value: 'INITIAL_REVIEW_IN_PROGRESS', label: '初審中' },
      { value: 'PENDING_REREVIEW', label: '待複審' },
      { value: 'REREVIEW_IN_PROGRESS', label: '複審中' },
      { value: 'CLOSED', label: '已結案' },
      { value: 'CANCELLED', label: '已取消' }
    ]);
  });

  it('emits the backend status value of the clicked chip', () => {
    const emitted: string[] = [];
    component.filterChange.subscribe(value => emitted.push(value));

    const chips = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('.filter-chip')) as HTMLButtonElement[];
    chips.forEach(chip => chip.click());

    expect(emitted).toEqual(component.availableStatuses.map(item => item.value));
  });

  it('marks no chip active when no status is applied', () => {
    component.currentStatus = undefined;
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).querySelectorAll('.filter-chip.active').length).toBe(0);
  });
});
