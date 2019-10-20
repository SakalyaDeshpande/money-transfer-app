package com.revolut.moneytransfer.exceptions;

import com.revolut.moneytransfer.model.ExceptionType;

/**
 * The exception which is thrown once some validation or data consistency error detected. It has additional
 * field {@link ExceptionType} which specify additional nature of the exception
 */
public class ObjectModificationException extends Exception {
    private ExceptionType type;

    public ObjectModificationException(ExceptionType exceptionType, Throwable cause) {
        super(exceptionType.getMessage(), cause);
        type = exceptionType;
    }

    public ObjectModificationException(ExceptionType exceptionType) {
        super(exceptionType.getMessage());
        type = exceptionType;
    }

    public ObjectModificationException(ExceptionType exceptionType, String message) {
        super(exceptionType.getMessage() + ": " + message);
        type = exceptionType;
    }

    public ExceptionType getType() {
        return type;
    }
}
