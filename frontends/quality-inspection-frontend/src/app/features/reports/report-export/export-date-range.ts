/**
 * UI-017 §3.1、§4：日期區間的預設值與送出轉換。
 *
 * 畫面上的日期是瀏覽器所在時區的日曆日期（`YYYY-MM-DD`），迄日含當天。
 * API 是含起不含迄的 UTC 時間點，因此 `to` 送迄日「次日」00:00（本地）。
 */

export interface DateRangeInput {
  startDate: string;
  endDate: string;
}

export interface ExportRange {
  from: string;
  to: string;
}

const DATE_PATTERN = /^(\d{4})-(\d{2})-(\d{2})$/;

function pad(value: number): string {
  return String(value).padStart(2, '0');
}

function formatLocalDate(date: Date): string {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

/** 解析 `YYYY-MM-DD` 為本地日期 00:00；格式錯誤或日期不存在時回 null。 */
function parseLocalDate(value: string): Date | null {
  const match = DATE_PATTERN.exec(value ?? '');
  if (!match) {
    return null;
  }
  const year = Number(match[1]);
  const month = Number(match[2]) - 1;
  const day = Number(match[3]);
  const date = new Date(year, month, day);
  if (date.getFullYear() !== year || date.getMonth() !== month || date.getDate() !== day) {
    return null;
  }
  return date;
}

/** 上一個整月（UI-017 §3.1 預設區間）。 */
export function previousFullMonth(today: Date): DateRangeInput {
  const start = new Date(today.getFullYear(), today.getMonth() - 1, 1);
  const end = new Date(today.getFullYear(), today.getMonth(), 0);
  return { startDate: formatLocalDate(start), endDate: formatLocalDate(end) };
}

/** 起迄日都合法，且起日不晚於迄日。 */
export function isOrderedRange(startDate: string, endDate: string): boolean {
  const start = parseLocalDate(startDate);
  const end = parseLocalDate(endDate);
  return start !== null && end !== null && start.getTime() <= end.getTime();
}

/** 轉成 API 的 from／to（UTC ISO-8601）。呼叫前須先以 isOrderedRange 檢查。 */
export function toExportRange(startDate: string, endDate: string): ExportRange {
  const start = parseLocalDate(startDate);
  const end = parseLocalDate(endDate);
  if (!start || !end) {
    throw new Error(`Invalid export date range: ${startDate} ~ ${endDate}`);
  }
  const dayAfterEnd = new Date(end.getFullYear(), end.getMonth(), end.getDate() + 1);
  return { from: start.toISOString(), to: dayAfterEnd.toISOString() };
}
