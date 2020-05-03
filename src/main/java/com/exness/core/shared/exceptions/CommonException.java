package com.exness.core.shared.exceptions;

public class CommonException extends RuntimeException {

    public CommonException(String errorMessage) {
        super(errorMessage);
    }

    public CommonException(String errorMessage, Exception ex) {
        super(errorMessage, ex);
    }
}
