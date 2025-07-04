package com.gateway.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrendAnalysis {
    private double percentageChange;
    private TrendDirection direction;

    public static TrendAnalysis fromValues(double current, double previous) {
        double percentageChange = previous == 0 ? 0 : ((current - previous) / previous) * 100;

        TrendDirection direction = TrendDirection.STABLE;
        if (percentageChange > 5) {
            direction = TrendDirection.INCREASING;
        } else if (percentageChange < -5) {
            direction = TrendDirection.DECREASING;
        }

        return new TrendAnalysis(percentageChange, direction);
    }
}