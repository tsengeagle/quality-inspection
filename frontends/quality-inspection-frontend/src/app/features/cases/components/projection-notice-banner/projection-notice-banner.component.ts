import { Component, Input } from '@angular/core';

/** C04 — ProjectionNoticeBanner: 宣告 metadata-only projection 限制（僅 METADATA_ONLY 顯示）。 */
@Component({
  selector: 'app-projection-notice-banner',
  templateUrl: './projection-notice-banner.component.html',
  styleUrls: ['./projection-notice-banner.component.scss']
})
export class ProjectionNoticeBannerComponent {
  /** 僅當 METADATA_ONLY projection 時為 true */
  @Input() show = false;
}
