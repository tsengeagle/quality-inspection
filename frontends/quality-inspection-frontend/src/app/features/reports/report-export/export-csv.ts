import { ExportRowView } from '../services/report-export.service';

/**
 * UI-017 §3.4、§5.2：表格與 CSV 共用的欄位定義與 CSV 產生。
 */

export type ExportField =
  | 'caseId' | 'projectId' | 'hospitalId' | 'status' | 'reviewerId' | 'rereviewerId'
  | 'initialScore' | 'finalScore' | 'createdAt' | 'closedAt';

export interface ExportColumn {
  field: ExportField;
  header: string;
}

export const MASKED_LABEL = '已遮罩';

/** 順序與標題同時用於表格與 CSV。 */
export const EXPORT_COLUMNS: readonly ExportColumn[] = [
  { field: 'caseId', header: '案件' },
  { field: 'projectId', header: '專案' },
  { field: 'hospitalId', header: '院區' },
  { field: 'status', header: '狀態' },
  { field: 'reviewerId', header: '初審醫師' },
  { field: 'rereviewerId', header: '複審醫師' },
  { field: 'initialScore', header: '初審分數' },
  { field: 'finalScore', header: '最終分數' },
  { field: 'createdAt', header: '建立時間' },
  { field: 'closedAt', header: '結案時間' }
];

const BOM = '﻿';
const CRLF = '\r\n';
const FORMULA_PREFIX = /^[=+\-@\t\r]/;
const NEEDS_QUOTES = /[",\r\n]/;

export function isMasked(row: ExportRowView, field: ExportField): boolean {
  return row.maskedFields.includes(field);
}

/**
 * CSV 儲存格的原始值：被遮罩寫「已遮罩」，null 寫空字串。
 * createdAt 與 closedAt 都是 UTC 的時間點，原樣寫入 API 回傳的字串，不換算（D-075 第 6 點）。
 */
function rawCell(row: ExportRowView, field: ExportField): string {
  if (isMasked(row, field)) {
    return MASKED_LABEL;
  }
  const value = row[field];
  return value === null || value === undefined ? '' : String(value);
}

function escapeCell(value: string): string {
  const guarded = FORMULA_PREFIX.test(value) ? `'${value}` : value;
  return NEEDS_QUOTES.test(guarded) ? `"${guarded.replace(/"/g, '""')}"` : guarded;
}

function toLine(cells: string[]): string {
  return cells.map(escapeCell).join(',') + CRLF;
}

/** UTF-8 BOM＋標題列＋資料列，CRLF 換行，RFC 4180 跳脫，防公式注入。 */
export function buildExportCsv(rows: readonly ExportRowView[]): string {
  const header = toLine(EXPORT_COLUMNS.map(c => c.header));
  const body = rows.map(row => toLine(EXPORT_COLUMNS.map(c => rawCell(row, c.field)))).join('');
  return BOM + header + body;
}

export function exportCsvFilename(startDate: string, endDate: string): string {
  return `review-completion_${startDate}_${endDate}.csv`;
}
