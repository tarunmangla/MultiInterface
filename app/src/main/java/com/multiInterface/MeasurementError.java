package com.multiInterface;

public class MeasurementError extends Exception {
    public MeasurementError(String reason) {
        super(reason);
    }
    public MeasurementError(String reason, Throwable e) {
        super(reason, e);
    }
}