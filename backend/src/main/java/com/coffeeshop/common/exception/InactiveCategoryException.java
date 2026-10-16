package com.coffeeshop.common.exception;

public class InactiveCategoryException extends RuntimeException {

    public InactiveCategoryException(Long categoryId) {
        super("Category is inactive and cannot be assigned to a Product: " + categoryId);
    }
}