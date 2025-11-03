package com.ecom.error.model;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    EMAIL_TAKEN(HttpStatus.CONFLICT),
    PHONE_TAKEN(HttpStatus.CONFLICT),
    BAD_CREDENTIALS(HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED),
    INSUFFICIENT_SCOPE(HttpStatus.FORBIDDEN),
    ADDRESS_DUPLICATE(HttpStatus.CONFLICT),
    SKU_REQUIRED(HttpStatus.BAD_REQUEST),
    INSUFFICIENT_STOCK(HttpStatus.CONFLICT),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND),
    PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND);

    private final HttpStatus httpStatus;

    ErrorCode(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
