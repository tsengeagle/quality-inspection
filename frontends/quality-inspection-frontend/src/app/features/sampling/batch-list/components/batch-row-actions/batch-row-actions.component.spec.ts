import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BatchRowActionsComponent } from './batch-row-actions.component';

describe('BatchRowActionsComponent', () => {
  let fixture: ComponentFixture<BatchRowActionsComponent>;
  let component: BatchRowActionsComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [BatchRowActionsComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(BatchRowActionsComponent);
    component = fixture.componentInstance;
    component.batch = {
      batchId: 'BATCH-FAILED-001',
      batchName: '失敗批次',
      status: 'FAILED',
      createdAt: '2026-04-14T09:20:00+08:00',
      projectId: 'PROJECT-001',
      projectName: '2026 年第二季品質審查',
      retryOfBatchId: null,
      selectedCount: null,
      canRetry: true
    };
  });

  it('shows retry for FAILED rows in the HOSPITAL_MANAGER baseline', () => {
    component.role = 'HOSPITAL_MANAGER';

    fixture.detectChanges();

    const buttons = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('button')).map(button => (button as HTMLButtonElement).textContent?.trim());
    expect(buttons).toContain('查看詳情');
    expect(buttons).toContain('查看案件');
    expect(buttons).toContain('重跑');
  });

  it('keeps SYSTEM_ADMIN in metadata-only actions baseline', () => {
    component.role = 'SYSTEM_ADMIN';

    fixture.detectChanges();

    const buttons = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('button')).map(button => (button as HTMLButtonElement).textContent?.trim());
    expect(buttons).toContain('查看詳情');
    expect(buttons).not.toContain('查看案件');
    expect(buttons).not.toContain('重跑');
  });
});