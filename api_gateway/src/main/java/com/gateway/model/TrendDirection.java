package com.gateway.model;

public enum TrendDirection {
    INCREASING,
    DECREASING,
    STABLE;

    public boolean isSignificant() {
        return this != STABLE;
    }
}