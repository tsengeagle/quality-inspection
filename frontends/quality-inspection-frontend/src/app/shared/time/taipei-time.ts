/**
 * 畫面上的時間一律經過這裡（D-075 第 5 點；憲法草案第一條第 3 點）。
 *
 * 後端輸出的是 UTC 的時間點（例如 2026-11-26T15:59:59Z）。畫面固定以 Asia/Taipei 顯示與輸入，
 * 不依賴瀏覽器的時區。台北沒有日光節約時間，固定是 UTC+08:00。
 */
export const TAIPEI_OFFSET = '+08:00';

const TAIPEI_OFFSET_MS = 8 * 60 * 60 * 1000;
/** 帶時區的 ISO-8601 時間。不帶時區的值不是合憲的輸出，不猜它的時區。 */
const ZONED_ISO = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2}(\.\d+)?)?(Z|[+-]\d{2}:\d{2})$/;

function pad(value: number): string {
  return value < 10 ? `0${value}` : String(value);
}

/** 時間點在台北的牆上時刻；value 不是帶時區的 ISO-8601 時間就回傳 null。 */
function taipeiParts(value: string | null | undefined): { date: string; hm: string; s: string } | null {
  if (!value || !ZONED_ISO.test(value)) {
    return null;
  }
  const epoch = Date.parse(value);
  if (Number.isNaN(epoch)) {
    return null;
  }
  const shifted = new Date(epoch + TAIPEI_OFFSET_MS);
  return {
    date: `${shifted.getUTCFullYear()}-${pad(shifted.getUTCMonth() + 1)}-${pad(shifted.getUTCDate())}`,
    hm: `${pad(shifted.getUTCHours())}:${pad(shifted.getUTCMinutes())}`,
    s: pad(shifted.getUTCSeconds())
  };
}

/**
 * 顯示用：台北時間的 `YYYY-MM-DD HH:mm`（withSeconds 時是 `YYYY-MM-DD HH:mm:ss`）。
 * 沒有值回傳 fallback；讀不懂的值原樣回傳，讓問題看得見。
 */
export function formatTaipeiDateTime(value: string | null | undefined, fallback = '—', withSeconds = false): string {
  if (!value) {
    return fallback;
  }
  const parts = taipeiParts(value);
  if (!parts) {
    return value;
  }
  return `${parts.date} ${parts.hm}${withSeconds ? `:${parts.s}` : ''}`;
}

/** 填入 `<input type="datetime-local">`：台北時間的 `YYYY-MM-DDTHH:mm`；沒有值或讀不懂回傳空字串。 */
export function toTaipeiDateTimeLocalInput(value: string | null | undefined): string {
  const parts = taipeiParts(value);
  return parts ? `${parts.date}T${parts.hm}` : '';
}

/**
 * `<input type="datetime-local">` 的值當成台北時間，加上 +08:00 送出（D-073 第 4 點）。
 * 格式不對回傳 null。
 */
export function fromTaipeiDateTimeLocalInput(value: string): string | null {
  const trimmed = value.trim();
  if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(trimmed)) {
    return `${trimmed}:00${TAIPEI_OFFSET}`;
  }
  if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$/.test(trimmed)) {
    return `${trimmed}${TAIPEI_OFFSET}`;
  }
  return null;
}
