package com.tzuchi.his.quality.inspection.infrastructure.persistence;

import org.hibernate.dialect.Oracle12cDialect;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorNoOpImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;

import java.sql.Types;

/**
 * 測試用的 Oracle 12c 方言。SQL 的產生方式與 {@link Oracle12cDialect} 完全相同，只調整兩件與 schema 驗證有關的事：
 *
 * <ol>
 *   <li>不在啟動時讀取 sequence 資訊（{@code select * from all_sequences}）：H2 的 Oracle 相容模式沒有
 *       Oracle 的系統檢視表 {@code ALL_SEQUENCES}；本系統的 schema 也沒有 sequence（自動編號是 identity 欄位）。</li>
 *   <li>{@code ddl-auto=validate} 比對欄位型別時，把 {@code NUMERIC}／{@code DECIMAL} 視為與整數、布林型別等價。
 *       Oracle 的 {@code NUMBER(19,0)}、{@code NUMBER(1,0)} 由 JDBC 回報為 {@code NUMERIC}／{@code DECIMAL}，
 *       Hibernate 5 卻期望 {@code BIGINT}、{@code BIT} 等型別，不放寬就一律失敗（真的 Oracle 也是如此）。
 *       只放寬數字家族之間的比對；缺表、缺欄、文字與數字不符仍會失敗。</li>
 * </ol>
 *
 * 正式環境用 {@link Oracle12cDialect}，schema 由 Flyway 負責、不做 {@code validate}（D-045）。
 */
public class H2OracleCompatibleDialect extends Oracle12cDialect {

    @Override
    public String getQuerySequencesString() {
        return null;
    }

    @Override
    public SequenceInformationExtractor getSequenceInformationExtractor() {
        return SequenceInformationExtractorNoOpImpl.INSTANCE;
    }

    @Override
    public boolean equivalentTypes(int typeCode1, int typeCode2) {
        return super.equivalentTypes(typeCode1, typeCode2)
                || (isOracleNumber(typeCode1) && isIntegralOrBoolean(typeCode2))
                || (isIntegralOrBoolean(typeCode1) && isOracleNumber(typeCode2));
    }

    private static boolean isOracleNumber(int typeCode) {
        return typeCode == Types.NUMERIC || typeCode == Types.DECIMAL;
    }

    private static boolean isIntegralOrBoolean(int typeCode) {
        return typeCode == Types.BIGINT || typeCode == Types.INTEGER || typeCode == Types.SMALLINT
                || typeCode == Types.TINYINT || typeCode == Types.BIT || typeCode == Types.BOOLEAN;
    }
}
