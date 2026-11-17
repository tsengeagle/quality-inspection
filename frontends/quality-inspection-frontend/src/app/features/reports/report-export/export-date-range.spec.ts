import { isOrderedRange, previousFullMonth, toExportRange } from './export-date-range';

// UI-017 §3.1、§4：預設上一個整月；迄日含當天，送出的 to 為迄日次日 00:00（本地）。
describe('export-date-range', () => {
  describe('previousFullMonth', () => {
    it('returns the whole previous month', () => {
      expect(previousFullMonth(new Date(2026, 10, 17, 9, 30)))
        .toEqual({ startDate: '2026-10-01', endDate: '2026-10-31' });
    });

    it('crosses the year boundary in January', () => {
      expect(previousFullMonth(new Date(2027, 0, 1)))
        .toEqual({ startDate: '2026-12-01', endDate: '2026-12-31' });
    });

    it('uses the real last day of February', () => {
      expect(previousFullMonth(new Date(2026, 2, 31)))
        .toEqual({ startDate: '2026-02-01', endDate: '2026-02-28' });
      expect(previousFullMonth(new Date(2028, 2, 1)))
        .toEqual({ startDate: '2028-02-01', endDate: '2028-02-29' });
    });
  });

  describe('toExportRange', () => {
    it('sends local midnight of the start date and of the day after the end date, as UTC instants', () => {
      expect(toExportRange('2026-10-01', '2026-10-31')).toEqual({
        from: new Date(2026, 9, 1).toISOString(),
        to: new Date(2026, 10, 1).toISOString()
      });
    });

    it('rolls the end date over the year boundary', () => {
      expect(toExportRange('2026-12-01', '2026-12-31').to).toBe(new Date(2027, 0, 1).toISOString());
    });

    it('covers a single day when start and end are equal', () => {
      expect(toExportRange('2026-10-05', '2026-10-05')).toEqual({
        from: new Date(2026, 9, 5).toISOString(),
        to: new Date(2026, 9, 6).toISOString()
      });
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
