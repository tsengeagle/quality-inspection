package com.tzuchi.his.quality.inspection.acceptance;

/** 一次 use case 呼叫的結果：成功（帶回傳值）或被拒絕（帶錯誤代碼）。 */
public final class Outcome {

    private final boolean success;
    private final Object value;
    private final String code;
    private final String detail;

    private Outcome(boolean success, Object value, String code, String detail) {
        this.success = success;
        this.value = value;
        this.code = code;
        this.detail = detail;
    }

    public static Outcome success(Object value) {
        return new Outcome(true, value, null, null);
    }

    /** code 可以是 null：例外沒有提供錯誤代碼。detail 是給人看的說明。 */
    public static Outcome rejected(String code, String detail) {
        return new Outcome(false, null, code, detail);
    }

    public boolean isSuccess() { return success; }
    public Object getValue() { return value; }
    public String getCode() { return code; }
    public String getDetail() { return detail; }

    @Override
    public String toString() {
        return success ? "SUCCESS" : "REJECTED " + code + (detail == null ? "" : "（" + detail + "）");
    }
}
