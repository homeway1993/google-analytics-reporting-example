package com.example.gareporting;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class GoogleAnalyticCampaignPerformance {

    private long id;
    private String campaignId;
    private String campaignName;
    private long click;
    private BigDecimal cost;
    private BigDecimal cpc;
    private String source;
    private Instant date;
    private Instant createTime;
    private Instant updateTime;

}
