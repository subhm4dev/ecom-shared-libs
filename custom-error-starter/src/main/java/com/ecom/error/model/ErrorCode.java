package com.ecom.error.model;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    EMAIL_TAKEN(HttpStatus.CONFLICT),
    PHONE_TAKEN(HttpStatus.CONFLICT),
    BAD_CREDENTIALS(HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED),
    INSUFFICIENT_SCOPE(HttpStatus.FORBIDDEN),
    ADDRESS_DUPLICATE(HttpStatus.CONFLICT),
    SKU_REQUIRED(HttpStatus.BAD_REQUEST),
    INSUFFICIENT_STOCK(HttpStatus.CONFLICT),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND),
    PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND),
    ADDRESS_NOT_FOUND(HttpStatus.NOT_FOUND),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND),
    CATEGORY_NAME_ALREADY_EXISTS(HttpStatus.CONFLICT),
    CATEGORY_HAS_PRODUCTS(HttpStatus.CONFLICT),
    CATEGORY_HAS_CHILDREN(HttpStatus.CONFLICT),
    INVALID_PARENT_CATEGORY(HttpStatus.BAD_REQUEST),
    LOCATION_NOT_FOUND(HttpStatus.NOT_FOUND),
    LOCATION_NAME_ALREADY_EXISTS(HttpStatus.CONFLICT),
    SKU_ALREADY_EXISTS(HttpStatus.CONFLICT);

    private final HttpStatus httpStatus;

    ErrorCode(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
