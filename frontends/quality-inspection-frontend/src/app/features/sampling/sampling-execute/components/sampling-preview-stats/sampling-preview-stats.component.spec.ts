import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SamplingPreviewStatsComponent } from './sampling-preview-stats.component';

describe('SamplingPreviewStatsComponent', () => {
  let fixture: ComponentFixture<SamplingPreviewStatsComponent>;
  let component: SamplingPreviewStatsComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [SamplingPreviewStatsComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(SamplingPreviewStatsComponent);
    component = fixture.componentInstance;
    component.stats = {
      populationCount: 120,
      eligibleCount: 80,
      excludedCount: 40
    };
  });

  it('keeps the Wave 4 preview stats baseline at exactly three cards', () => {
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelectorAll('.stat-card').length).toBe(3);
    expect(fixture.nativeElement.textContent).toContain('母體件數');
    expect(fixture.nativeElement.textContent).not.toContain('抽中件數');
  });
});
