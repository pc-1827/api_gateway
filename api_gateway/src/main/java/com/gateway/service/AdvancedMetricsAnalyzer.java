package com.gateway.service;

import com.gateway.model.MetricAnalysis;
import com.gateway.model.MetricPoint;
import com.gateway.model.TrendAnalysis;
import com.gateway.model.TrendDirection;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class AdvancedMetricsAnalyzer {

    public MetricAnalysis analyzeMetric(List<MetricPoint> points) {
        if (points.size() < 2) {
            return MetricAnalysis.empty();
        }

        double[] values = points.stream()
                .mapToDouble(MetricPoint::getValue)
                .toArray();

        return new MetricAnalysis()
                .setMean(calculateMean(values))
                .setMedian(calculateMedian(values))
                .setStdDev(calculateStdDev(values))
                .setTrend(calculateTrend(points))
                .setOutliers(detectOutliers(values))
                .setForecast(forecastNextValue(points))
                .setAnalysisTime(Instant.now());
    }

    private TrendAnalysis calculateTrend(List<MetricPoint> points) {
        if (points.size() < 2) {
            return new TrendAnalysis(0.0, TrendDirection.STABLE);
        }

        double recent = points.get(points.size() - 1).getValue();
        double previous = points.get(points.size() - 2).getValue();
        double percentageChange = ((recent - previous) / previous) * 100;

        TrendDirection direction = percentageChange > 5 ? TrendDirection.INCREASING
                : percentageChange < -5 ? TrendDirection.DECREASING : TrendDirection.STABLE;

        return new TrendAnalysis(percentageChange, direction);
    }

    private double calculateMean(double[] values) {
        return Arrays.stream(values).average().orElse(0.0);
    }

    private double calculateMedian(double[] values) {
        double[] sortedValues = Arrays.copyOf(values, values.length);
        Arrays.sort(sortedValues);

        int middle = sortedValues.length / 2;
        if (sortedValues.length % 2 == 0) {
            return (sortedValues[middle - 1] + sortedValues[middle]) / 2.0;
        } else {
            return sortedValues[middle];
        }
    }

    private double calculateStdDev(double[] values) {
        double mean = calculateMean(values);
        double variance = Arrays.stream(values)
                .map(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0.0);
        return Math.sqrt(variance);
    }

    private List<Double> detectOutliers(double[] values) {
        double mean = calculateMean(values);
        double stdDev = calculateStdDev(values);

        // Use 2 standard deviations as threshold for outliers
        double threshold = 2.0 * stdDev;

        List<Double> outliers = new ArrayList<>();
        for (double value : values) {
            if (Math.abs(value - mean) > threshold) {
                outliers.add(value);
            }
        }

        return outliers;
    }

    private double forecastNextValue(List<MetricPoint> points) {
        // Simple exponential smoothing
        double alpha = 0.3;
        double forecast = points.get(0).getValue();

        for (int i = 1; i < points.size(); i++) {
            forecast = alpha * points.get(i).getValue() + (1 - alpha) * forecast;
        }

        return forecast;
    }
}