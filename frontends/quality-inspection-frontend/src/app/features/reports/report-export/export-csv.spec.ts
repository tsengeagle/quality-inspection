import { ExportRowView } from '../services/report-export.service';
import { buildExportCsv, exportCsvFilename, EXPORT_COLUMNS } from './export-csv';

function row(overrides: Partial<ExportRowView> = {}): ExportRowView {
  return {
    caseId: 'CASE-001',
    projectId: 'PRJ-001',
    hospitalId: 'H001',
    status: 'CLOSED',
    reviewerId: 'reviewer-1',
    rereviewerId: null,
    initialScore: 85.5,
    finalScore: 90,
    createdAt: '2026-10-02T01:00:00Z',
    closedAt: '2026-10-05T01:30:00Z',
    maskedFields: [],
    ...overrides
  };
}

function lines(csv: string): string[] {
  return csv.slice(1).split('\r\n');
}

// UI-017 §5.2
describe('buildExportCsv', () => {
  it('starts with a UTF-8 BOM and uses CRLF line endings', () => {
    const csv = buildExportCsv([row()]);
    expect(csv.charAt(0)).toBe('﻿');
    expect(csv.slice(1)).not.toMatch(/(^|[^\r])\n/);
    expect(csv.endsWith('\r\n')).toBeTrue();
  });

  it('writes the same headers as the table, in the same order', () => {
    expect(lines(buildExportCsv([]))[0]).toBe(
      '案件,專案,院區,狀態,初審醫師,複審醫師,初審分數,最終分數,建立時間,結案時間'
    );
    expect(EXPORT_COLUMNS.map(c => c.field)).toEqual([
      'caseId', 'projectId', 'hospitalId', 'status', 'reviewerId', 'rereviewerId',
      'initialScore', 'finalScore', 'createdAt', 'closedAt'
    ]);
  });

  it('writes createdAt and closedAt as the UTC instants the API returned (D-075)', () => {
    expect(lines(buildExportCsv([row()]))[1]).toBe(
      'CASE-001,PRJ-001,H001,CLOSED,reviewer-1,,85.5,90,2026-10-02T01:00:00Z,2026-10-05T01:30:00Z'
    );
  });

  it('writes 已遮罩 for masked scores and never 0 or blank', () => {
    const line = lines(buildExportCsv([
      row({ initialScore: null, finalScore: null, maskedFields: ['initialScore', 'finalScore'] })
    ]))[1];
    expect(line.split(',').slice(6, 8)).toEqual(['已遮罩', '已遮罩']);
  });

  it('writes an empty string for null values that are not masked', () => {
    const line = lines(buildExportCsv([row({ finalScore: null, closedAt: null })]))[1];
    const cells = line.split(',');
    expect(cells[7]).toBe('');
    expect(cells[9]).toBe('');
  });

  it('quotes values containing a comma, a double quote or a line break (RFC 4180)', () => {
    const line = lines(buildExportCsv([row({ projectId: 'A,B', reviewerId: 'say "hi"', rereviewerId: 'x\ny' })]))
      .slice(1).join('\r\n');
    expect(line).toContain('"A,B"');
    expect(line).toContain('"say ""hi"""');
    expect(line).toContain('"x\ny"');
  });

  it('prefixes a single quote to values that could start a formula', () => {
    const csv = buildExportCsv([
      row({ caseId: '=SUM(A1)' }),
      row({ caseId: '+1' }),
      row({ caseId: '-1' }),
      row({ caseId: '@cmd' }),
      row({ caseId: '\tTAB' }),
      row({ caseId: '\rCR' })
    ]);
    expect(csv).toContain("\r\n'=SUM(A1),");
    expect(csv).toContain("\r\n'+1,");
    expect(csv).toContain("\r\n'-1,");
    expect(csv).toContain("\r\n'@cmd,");
    expect(csv).toContain("\r\n'\tTAB,");
    expect(csv).toContain("\r\n\"'\rCR\",");
  });
});

describe('exportCsvFilename', () => {
  it('names the file after the report type and the on-screen dates', () => {
    expect(exportCsvFilename('2026-10-01', '2026-10-31')).toBe('review-completion_2026-10-01_2026-10-31.csv');
  });
});
