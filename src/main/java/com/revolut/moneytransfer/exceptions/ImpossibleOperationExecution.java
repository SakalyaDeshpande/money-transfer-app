package com.revolut.moneytransfer.exceptions;

/**
 * Runtime exception which is used to be thrown when operation could not be executed. Most of time
 * when some unexpected SQLException occurred.
 */
public class ImpossibleOperationExecution extends RuntimeException {
    public ImpossibleOperationExecution(Throwable cause) {
        super(cause);
    }
}
