package com.tzuchi.his.quality.inspection.architecture.samples.jackson.application;

import com.fasterxml.jackson.annotation.JsonIgnore;

/** 違規樣本：application 的讀取模型用 Jackson 的標註決定 HTTP 的輸出形狀。 */
public class ApplicationViewUsesJsonIgnore {
    @JsonIgnore
    public String getHidden() {
        return "sample";
    }
}
