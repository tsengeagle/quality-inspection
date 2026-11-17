import { DOCUMENT } from '@angular/common';
import { Inject, Injectable } from '@angular/core';

/** 在瀏覽器觸發 CSV 檔案下載。內容已含 BOM，Blob 以 UTF-8 編碼。 */
@Injectable({ providedIn: 'root' })
export class CsvFileSaver {

  constructor(@Inject(DOCUMENT) private readonly document: Document) {}

  save(filename: string, content: string): void {
    const blob = new Blob([content], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const anchor = this.document.createElement('a');
    anchor.href = url;
    anchor.download = filename;
    anchor.style.display = 'none';
    this.document.body.appendChild(anchor);
    try {
      anchor.click();
    } finally {
      anchor.remove();
      setTimeout(() => URL.revokeObjectURL(url), 0);
    }
  }
}
