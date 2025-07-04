package com.gateway.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
public class MetricAnalysis {
    private double mean;
    private double median;
    private double stdDev;
    private TrendAnalysis trend;
    private List<Double> outliers = new ArrayList<>();
    private double forecast;
    private Instant analysisTime;

    public MetricAnalysis() {
        this.trend = new TrendAnalysis(0.0, TrendDirection.STABLE);
        this.analysisTime = Instant.now();
    }

    // Helper method to create an empty analysis
    public static MetricAnalysis empty() {
        return new MetricAnalysis()
                .setMean(0.0)
                .setMedian(0.0)
                .setStdDev(0.0)
                .setTrend(new TrendAnalysis(0.0, TrendDirection.STABLE))
                .setForecast(0.0);
    }

    // Get a summary of the analysis
    public Map<String, Object> getSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("mean", formatDouble(mean));
        summary.put("median", formatDouble(median));
        summary.put("standardDeviation", formatDouble(stdDev));
        summary.put("trend", trend.getDirection().toString());
        summary.put("percentageChange", formatDouble(trend.getPercentageChange()));
        summary.put("outliers", outliers.size());
        summary.put("forecast", formatDouble(forecast));
        summary.put("analysisTime", analysisTime.toString());
        return summary;
    }

    // New method to create a serializable representation
    public Map<String, Object> toSerializableMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("mean", mean);
        map.put("median", median);
        map.put("stdDev", stdDev);

        Map<String, Object> trendMap = new HashMap<>();
        trendMap.put("percentageChange", trend.getPercentageChange());
        trendMap.put("direction", trend.getDirection().toString());
        map.put("trend", trendMap);

        map.put("outliers", outliers);
        map.put("forecast", forecast);
        map.put("analysisTime", analysisTime.toString());
        map.put("recommendations", getRecommendations());

        return map;
    }

    // Check if the metric is showing concerning patterns
    public boolean isConcerning() {
        return trend.getDirection().isSignificant() ||
                !outliers.isEmpty() ||
                stdDev > mean * 0.5; // High variability
    }

    // Get health status based on the analysis
    public String getHealthStatus() {
        if (!isConcerning()) {
            return "HEALTHY";
        }

        if (trend.getDirection() == TrendDirection.INCREASING && outliers.size() > 3) {
            return "CRITICAL";
        }

        return "WARNING";
    }

    // Format metrics for display
    public String getFormattedSummary() {
        return String.format(
                "Mean: %.2f, Median: %.2f, StdDev: %.2f, Trend: %s (%.2f%%), Outliers: %d, Forecast: %.2f",
                mean, median, stdDev, trend.getDirection(), trend.getPercentageChange(), outliers.size(), forecast);
    }

    // Helper method to format doubles
    private double formatDouble(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    // Get recommendations based on the analysis
    public List<String> getRecommendations() {
        List<String> recommendations = new ArrayList<>();

        if (trend.getDirection() == TrendDirection.INCREASING) {
            if (trend.getPercentageChange() > 20) {
                recommendations.add("URGENT: Resource usage is increasing rapidly, investigate immediately.");
            } else {
                recommendations.add("Resource usage is trending upward, monitor closely.");
            }
        }

        if (outliers.size() > 0) {
            recommendations.add("Detected " + outliers.size() + " outliers. Check for unusual system behavior.");
        }

        if (stdDev > mean * 0.5) {
            recommendations.add("High variability detected. Consider stabilizing the workload.");
        }

        if (recommendations.isEmpty()) {
            recommendations.add("System is operating normally. No action needed.");
        }

        return recommendations;
    }
}