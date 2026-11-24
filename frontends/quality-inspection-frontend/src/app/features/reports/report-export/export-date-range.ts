import { TAIPEI_OFFSET, TAIPEI_OFFSET_MS } from '@shared/time/taipei-time';

/**
 * UI-017 §3.1、§4；UI-015 §4：日期區間的預設值與送出轉換（D-077）。
 *
 * 畫面上的日期是 `Asia/Taipei` 的日曆日期（`YYYY-MM-DD`），迄日含當天，不依瀏覽器的時區。
 * API 是含起不含迄的 UTC 時間點：`from` 送起日 00:00 `+08:00`，`to` 送迄日「次日」00:00 `+08:00`。
 * 這裡只用 UTC 的日期運算，不讀瀏覽器本地的年月日。
 */

export interface DateRangeInput {
  startDate: string;
  endDate: string;
}

export interface ExportRange {
  from: string;
  to: string;
}

interface CalendarDate {
  year: number;
  /** 0 起算的月份，與 `Date.UTC` 相同。 */
  month: number;
  day: number;
}

const DATE_PATTERN = /^(\d{4})-(\d{2})-(\d{2})$/;

function pad(value: number): string {
  return String(value).padStart(2, '0');
}

/** 以 UTC 的年月日（溢位自動進位）組出 `YYYY-MM-DD`。 */
function formatCalendarDate(year: number, month: number, day: number): string {
  const date = new Date(Date.UTC(year, month, day));
  return `${date.getUTCFullYear()}-${pad(date.getUTCMonth() + 1)}-${pad(date.getUTCDate())}`;
}

/** 解析 `YYYY-MM-DD`；格式錯誤或日期不存在時回 null。 */
function parseCalendarDate(value: string): CalendarDate | null {
  const match = DATE_PATTERN.exec(value ?? '');
  if (!match) {
    return null;
  }
  const year = Number(match[1]);
  const month = Number(match[2]) - 1;
  const day = Number(match[3]);
  const date = new Date(Date.UTC(year, month, day));
  if (date.getUTCFullYear() !== year || date.getUTCMonth() !== month || date.getUTCDate() !== day) {
    return null;
  }
  return { year, month, day };
}

/** 台北某日 00:00 的 UTC ISO-8601 時間點。 */
function taipeiMidnight(date: string): string {
  return new Date(`${date}T00:00:00${TAIPEI_OFFSET}`).toISOString();
}

/** 上一個整月（UI-017 §3.1 預設區間），以 `now` 在台北的日期計算（D-077 第 2 點）。 */
export function previousFullMonth(now: Date): DateRangeInput {
  const taipei = new Date(now.getTime() + TAIPEI_OFFSET_MS);
  const year = taipei.getUTCFullYear();
  const month = taipei.getUTCMonth();
  return {
    startDate: formatCalendarDate(year, month - 1, 1),
    endDate: formatCalendarDate(year, month, 0)
  };
}

/** 起迄日都合法，且起日不晚於迄日。 */
export function isOrderedRange(startDate: string, endDate: string): boolean {
  const start = parseCalendarDate(startDate);
  const end = parseCalendarDate(endDate);
  return start !== null && end !== null
    && Date.UTC(start.year, start.month, start.day) <= Date.UTC(end.year, end.month, end.day);
}

/** 轉成 API 的 from／to（UTC ISO-8601）。呼叫前須先以 isOrderedRange 檢查。 */
export function toExportRange(startDate: string, endDate: string): ExportRange {
  const start = parseCalendarDate(startDate);
  const end = parseCalendarDate(endDate);
  if (!start || !end) {
    throw new Error(`Invalid export date range: ${startDate} ~ ${endDate}`);
  }
  const dayAfterEnd = formatCalendarDate(end.year, end.month, end.day + 1);
  return { from: taipeiMidnight(startDate), to: taipeiMidnight(dayAfterEnd) };
}
