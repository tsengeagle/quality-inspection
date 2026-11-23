import { formatTaipeiDateTime, fromTaipeiDateTimeLocalInput, toTaipeiDateTimeLocalInput } from './taipei-time';

describe('taipei-time (D-075 第 5 點)', () => {
  describe('formatTaipeiDateTime', () => {
    it('shows a UTC instant as Taipei wall time', () => {
      expect(formatTaipeiDateTime('2026-11-26T15:59:59Z')).toBe('2026-11-26 23:59');
    });

    it('crosses the date line when Taipei is already on the next day', () => {
      expect(formatTaipeiDateTime('2026-12-31T16:00:00Z')).toBe('2027-01-01 00:00');
    });

    it('accepts any offset and fractional seconds', () => {
      expect(formatTaipeiDateTime('2026-11-26T23:59:59+08:00')).toBe('2026-11-26 23:59');
      expect(formatTaipeiDateTime('2026-11-26T15:59:59.123456Z', '—', true)).toBe('2026-11-26 23:59:59');
    });

    it('returns the fallback for a missing value', () => {
      expect(formatTaipeiDateTime(null)).toBe('—');
      expect(formatTaipeiDateTime(undefined, '尚未設定')).toBe('尚未設定');
      expect(formatTaipeiDateTime('')).toBe('—');
    });

    it('does not guess the zone of a value without one', () => {
      expect(formatTaipeiDateTime('2026-11-26T23:59:59')).toBe('2026-11-26T23:59:59');
      expect(formatTaipeiDateTime('not a time')).toBe('not a time');
    });
  });

  describe('toTaipeiDateTimeLocalInput', () => {
    it('fills the input with Taipei wall time', () => {
      expect(toTaipeiDateTimeLocalInput('2026-11-26T15:59:00Z')).toBe('2026-11-26T23:59');
    });

    it('is empty for a missing or unreadable value', () => {
      expect(toTaipeiDateTimeLocalInput(undefined)).toBe('');
      expect(toTaipeiDateTimeLocalInput('2026-11-26T23:59:00')).toBe('');
    });
  });

  describe('fromTaipeiDateTimeLocalInput', () => {
    it('sends the wall time as Taipei time', () => {
      expect(fromTaipeiDateTimeLocalInput('2026-11-26T23:59')).toBe('2026-11-26T23:59:00+08:00');
      expect(fromTaipeiDateTimeLocalInput(' 2026-11-26T23:59:30 ')).toBe('2026-11-26T23:59:30+08:00');
    });

    it('rejects anything else', () => {
      expect(fromTaipeiDateTimeLocalInput('')).toBeNull();
      expect(fromTaipeiDateTimeLocalInput('2026-11-26')).toBeNull();
    });

    it('round-trips with the value shown in the input', () => {
      const shown = toTaipeiDateTimeLocalInput('2026-11-26T15:59:00Z');
      expect(Date.parse(fromTaipeiDateTimeLocalInput(shown) as string)).toBe(Date.parse('2026-11-26T15:59:00Z'));
    });
  });
});
