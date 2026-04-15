import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CaseSummaryHeaderComponent } from './case-summary-header.component';

describe('CaseSummaryHeaderComponent', () => {
  let fixture: ComponentFixture<CaseSummaryHeaderComponent>;
  let component: CaseSummaryHeaderComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [CaseSummaryHeaderComponent],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    fixture = TestBed.createComponent(CaseSummaryHeaderComponent);
    component = fixture.componentInstance;
  });

  it('renders the case identity and status metadata', () => {
    component.caseId = 'CASE-001';
    component.caseTitle = '案件摘要';
    component.stageLabel = '初審';
    component.status = 'PENDING';

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('.case-summary-header__case-id')?.textContent).toContain('CASE-001');
    expect(element.querySelector('.case-summary-header__title')?.textContent).toContain('案件摘要');
    expect(element.querySelector('.case-summary-header__stage')?.textContent).toContain('初審');
    expect(element.querySelector('.case-summary-header__status')?.textContent).toContain('PENDING');
  });

  it('keeps the inline projection notice slot when metadata-only mode is enabled', () => {
    component.showProjectionNotice = true;

    fixture.detectChanges();

    const projectionBanner = fixture.nativeElement.querySelector('app-projection-notice-banner');
    expect(projectionBanner).toBeTruthy();
  });
});
