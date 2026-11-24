import { isOrderedRange, previousFullMonth, toExportRange } from './export-date-range';

// UI-017 §3.1、§4；D-077：日期是 Asia/Taipei 的日曆日期；預設上一個整月（以台北的今天計算）；
// 迄日含當天，送出的 to 為迄日次日 00:00 +08:00。預期值寫死 UTC 字串，不依執行環境的時區。
describe('export-date-range', () => {
  describe('previousFullMonth', () => {
    it('returns the whole previous month', () => {
      expect(previousFullMonth(new Date('2026-11-17T09:30:00+08:00')))
        .toEqual({ startDate: '2026-10-01', endDate: '2026-10-31' });
    });

    it('crosses the year boundary in January', () => {
      expect(previousFullMonth(new Date('2027-01-01T00:00:00+08:00')))
        .toEqual({ startDate: '2026-12-01', endDate: '2026-12-31' });
    });

    it('uses the real last day of February', () => {
      expect(previousFullMonth(new Date('2026-03-31T12:00:00+08:00')))
        .toEqual({ startDate: '2026-02-01', endDate: '2026-02-28' });
      expect(previousFullMonth(new Date('2028-03-01T12:00:00+08:00')))
        .toEqual({ startDate: '2028-02-01', endDate: '2028-02-29' });
    });

    it('uses the Taipei date when UTC is still in the previous month (D-077 第 2 點)', () => {
      // 台北已是 2026-12-01 01:00
      expect(previousFullMonth(new Date('2026-11-30T17:00:00Z')))
        .toEqual({ startDate: '2026-11-01', endDate: '2026-11-30' });
    });

    it('stays in the current Taipei month just before Taipei midnight', () => {
      // 台北是 2026-11-30 23:59:59，UTC 是同一天 15:59:59
      expect(previousFullMonth(new Date('2026-11-30T15:59:59Z')))
        .toEqual({ startDate: '2026-10-01', endDate: '2026-10-31' });
    });
  });

  describe('toExportRange', () => {
    it('sends Taipei midnight of the start date and of the day after the end date, as UTC instants', () => {
      expect(toExportRange('2026-10-01', '2026-10-31')).toEqual({
        from: '2026-09-30T16:00:00.000Z',
        to: '2026-10-31T16:00:00.000Z'
      });
    });

    it('rolls the end date over the year boundary', () => {
      expect(toExportRange('2026-12-01', '2026-12-31')).toEqual({
        from: '2026-11-30T16:00:00.000Z',
        to: '2026-12-31T16:00:00.000Z'
      });
    });

    it('rolls the end date over the end of February', () => {
      expect(toExportRange('2028-02-01', '2028-02-29').to).toBe('2028-02-29T16:00:00.000Z');
    });

    it('covers a single day when start and end are equal', () => {
      expect(toExportRange('2026-10-05', '2026-10-05')).toEqual({
        from: '2026-10-04T16:00:00.000Z',
        to: '2026-10-05T16:00:00.000Z'
      });
    });

    it('rejects malformed dates', () => {
      expect(() => toExportRange('2026/10/01', '2026-10-31')).toThrowError(/Invalid export date range/);
    });
  });

  describe('isOrderedRange', () => {
    it('accepts equal or ordered dates', () => {
      expect(isOrderedRange('2026-10-01', '2026-10-31')).toBeTrue();
      expect(isOrderedRange('2026-10-05', '2026-10-05')).toBeTrue();
    });

    it('rejects a start date later than the end date', () => {
      expect(isOrderedRange('2026-11-01', '2026-10-31')).toBeFalse();
    });

    it('rejects missing or malformed dates', () => {
      expect(isOrderedRange('', '2026-10-31')).toBeFalse();
      expect(isOrderedRange('2026-10-01', '')).toBeFalse();
      expect(isOrderedRange('2026/10/01', '2026-10-31')).toBeFalse();
      expect(isOrderedRange('2026-02-30', '2026-03-01')).toBeFalse();
    });
  });
});
