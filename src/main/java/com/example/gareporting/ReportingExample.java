package com.example.gareporting;

import com.google.analytics.data.v1beta.*;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.common.collect.Lists;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

public class ReportingExample {

    private static final String serviceAccountBase64 = "__REPLACE_ME__";
    private static final String propertyId = "152434672";
    private static final String GA_DATE_FORMAT = "yyyyMMdd";
    private static final String GA_CAMPAIGN = "campaignName";
    private static final String GA_SOURCE = "source";
    private static final String GA_DATE = "date";
    private static final String GA_ADWORDS_CAMPAIGN_ID = "campaignId";
    private static final String GA_AD_CLICKS = "advertiserAdClicks";
    private static final String GA_AD_COST = "advertiserAdCost";
    private static final String GA_CPC = "advertiserAdCostPerClick";

    private static final List<Dimension> DIMENSIONS = Lists.newArrayList(GA_CAMPAIGN, GA_SOURCE, GA_DATE, GA_ADWORDS_CAMPAIGN_ID)
            .stream()
            .map(s -> Dimension.newBuilder().setName(s).build())
            .collect(Collectors.toList());

    private static final List<Metric> METRICS = Lists.newArrayList(GA_AD_CLICKS, GA_AD_COST, GA_CPC)
            .stream()
            .map(s -> Metric.newBuilder().setName(s).build())
            .collect(Collectors.toList());

    public static void main(String... args) throws Exception {
        GoogleCredentials credentials =
                GoogleCredentials.fromStream(new ByteArrayInputStream(Base64.getDecoder().decode(serviceAccountBase64)));

        BetaAnalyticsDataSettings dataSettings =
                BetaAnalyticsDataSettings.newBuilder()
                        .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                        .build();

        try (BetaAnalyticsDataClient analyticsData = BetaAnalyticsDataClient.create(dataSettings)) {

            RunReportRequest request = RunReportRequest.newBuilder()
                    .setProperty("properties/" + propertyId)
                    .addAllDimensions(DIMENSIONS)
                    .addAllMetrics(METRICS)
                    .addDateRanges(DateRange.newBuilder().setStartDate("2023-05-01").setEndDate("today"))
                    .build();

            RunReportResponse response = analyticsData.runReport(request);

            List<GoogleAnalyticCampaignPerformance> records = new ArrayList<>();
            for (Row row : response.getRowsList()) {
                GoogleAnalyticCampaignPerformance.GoogleAnalyticCampaignPerformanceBuilder builder =
                        GoogleAnalyticCampaignPerformance.builder();

                // dimension
                List<DimensionHeader> dimensionHeadersList = response.getDimensionHeadersList();
                for (int i = 0; i < dimensionHeadersList.size(); i++) {
                    switch (dimensionHeadersList.get(i).getName()) {
                        case GA_CAMPAIGN:
                            builder.campaignName(row.getDimensionValuesList().get(i).getValue());
                            break;
                        case GA_SOURCE:
                            builder.source(row.getDimensionValuesList().get(i).getValue());
                            break;
                        case GA_DATE:
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(GA_DATE_FORMAT);
                            Instant instant = LocalDate.parse(row.getDimensionValuesList().get(i).getValue(), formatter).atStartOfDay(ZoneOffset.UTC).toInstant();
                            builder.date(instant);
                            break;
                        case GA_ADWORDS_CAMPAIGN_ID:
                            builder.campaignId(row.getDimensionValuesList().get(i).getValue());
                        default:
                            break;
                    }
                }

                // metric
                List<MetricHeader> metricHeadersList = response.getMetricHeadersList();
                for (int i = 0; i < metricHeadersList.size(); i++) {
                    MetricValue metricValues = row.getMetricValues(i);

                    switch (metricHeadersList.get(i).getName()) {
                        case GA_AD_CLICKS:
                            builder.click(Long.parseLong(metricValues.getValue()));
                            break;
                        case GA_AD_COST:
                            builder.cost(new BigDecimal(metricValues.getValue()));
                            break;
                        case GA_CPC:
                            builder.cpc(new BigDecimal(metricValues.getValue()));
                            break;
                        default:
                            break;
                    }
                }

                records.add(builder.build());
            }

            for (GoogleAnalyticCampaignPerformance record : records) {
                System.out.println(record);
            }
        }
    }
}
