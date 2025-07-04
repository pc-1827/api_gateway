package com.gateway.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetricPoint {
    private String name;
    private double value;
    private long timestamp;
}