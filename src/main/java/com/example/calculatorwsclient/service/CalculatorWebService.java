package com.example.calculatorwsclient.service;


import org.apache.axis2.client.async.AxisCallback;

import java.util.concurrent.CompletableFuture;

public interface CalculatorWebService {
    CompletableFuture<Integer> performAsyncAddOperation(int A, int B);

    CompletableFuture<Integer> performAsyncVirtualThreadAddOperation(int A, int B);

    int performSyncAddOperation(int A, int B) throws Exception;

    void performAxisAsync(int A, int B, AxisCallback axisCallback) throws Exception;
}
