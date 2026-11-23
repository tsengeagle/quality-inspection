package com.tzuchi.his.quality.inspection.acceptance;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 範例執行器送出 HTTP 請求的唯一途徑（D-074，規格格式 v2.1 的 http 步驟）。
 *
 * <p>執行器本身只懂規格；請求怎麼送、api-key 怎麼組、回應的 JSON 怎麼讀，由實作決定。</p>
 */
public interface HttpInvoker {

    /**
     * 跑 http 範例之前的檢查（D-074 第 7 點）：程式不得以設定覆寫操作者的院區。
     *
     * @throws RunnerProblem 環境不能跑 http 範例
     */
    void checkReady();

    /** @throws RunnerProblem 請求送不出去 */
    Reply send(Request request);

    /** 一個請求。anonymous 時不帶 api-key，user、hospital、roleClaims 不使用。 */
    final class Request {
        public final String method;
        public final String path;
        /** 已代入變數；沒有就是空的。 */
        public final Map<String, Object> query;
        /** 已代入變數的 body，轉成 JSON 送出；沒有寫 body 時是 null。 */
        public final Object body;
        /** 原封不動送出的本文；沒有寫 body_raw 時是 null。 */
        public final String bodyRaw;
        public final boolean anonymous;
        public final String user;
        /** 可以是 null：操作者沒有院區。 */
        public final String hospital;
        /** 操作者的角色經 spec-profile.yaml 的 role_claims 轉換後的 role_name。 */
        public final List<String> roleClaims;

        public Request(String method, String path, Map<String, Object> query, Object body, String bodyRaw,
                       boolean anonymous, String user, String hospital, List<String> roleClaims) {
            this.method = method;
            this.path = path;
            this.query = query == null ? Collections.emptyMap() : query;
            this.body = body;
            this.bodyRaw = bodyRaw;
            this.anonymous = anonymous;
            this.user = user;
            this.hospital = hospital;
            this.roleClaims = roleClaims == null ? Collections.emptyList() : roleClaims;
        }
    }

    /** 一個回應。 */
    final class Reply {
        public final int status;
        /** 回應本文的文字；沒有本文是空字串。 */
        public final String body;
        /** 本文讀成的 JSON（對應表、清單、單一值）；本文是空的或不是 JSON 時是 null。 */
        public final Object json;

        public Reply(int status, String body, Object json) {
            this.status = status;
            this.body = body == null ? "" : body;
            this.json = json;
        }
    }
}
