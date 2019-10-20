package com.revolut.moneytransfer.model;

import com.revolut.moneytransfer.exceptions.ObjectModificationException;

/**
 * Used mostly in the {@link ObjectModificationException} to
 * specify the particular type of the exception
 */
public enum ExceptionType {
    OBJECT_IS_MALFORMED("The entity passed has been malformed"),
    OBJECT_IS_NOT_FOUND("The entity with provided ID has not been found"),
    COULD_NOT_OBTAIN_ID("The system could not generate ID for this entity. Creation is failed."),
    UNEXPECTED_EXCEPTION("Unexpected exception");

    private String message;

    ExceptionType(String message) {
        this.message = message;
    }

    public String getMessage() {return message;}

    @Override
    public String toString() {
        return message;
    }
}
