package com.revolut.moneytransfer.model;

/**
 * The exception wrapper object to be return to the client as a response when some error will be occurred
 */
public class ApplicationException {
    private String type;
    private String name;
    private String message;

    public ApplicationException() {
    }

    public ApplicationException(ExceptionType exceptionType, String message) {
        this.type = exceptionType.name();
        this.name = exceptionType.getMessage();
        this.message = message;
    }

    public ApplicationException(String type, String name, String message) {
        this.type = type;
        this.name = name;
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getMessage() {
        return message;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
