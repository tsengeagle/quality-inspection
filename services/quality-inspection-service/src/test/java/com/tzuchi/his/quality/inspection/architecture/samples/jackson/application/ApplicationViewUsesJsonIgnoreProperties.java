package com.tzuchi.his.quality.inspection.architecture.samples.jackson.application;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** 違規樣本：標註在類別上。 */
@JsonIgnoreProperties({"hidden"})
public class ApplicationViewUsesJsonIgnoreProperties {
    public String getHidden() {
        return "sample";
    }
}
