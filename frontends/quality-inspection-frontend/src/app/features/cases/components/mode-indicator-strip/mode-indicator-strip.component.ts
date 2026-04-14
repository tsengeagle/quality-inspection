import { Component, Input } from '@angular/core';

/** C03 — ModeIndicatorStrip: 顯示 page mode（ASSIGN/REVISION/READONLY…）與行為語意。
 *  usedByPages: UI-010/011/012/013/014 */
@Component({
  selector: 'app-mode-indicator-strip',
  templateUrl: './mode-indicator-strip.component.html',
  styleUrls: ['./mode-indicator-strip.component.scss']
})
export class ModeIndicatorStripComponent {
  /** mode resolver 輸出的 display label，例如「初審」、「複審」、「例外修正（已結案）」等 */
  @Input() modeLabel = '';
}
