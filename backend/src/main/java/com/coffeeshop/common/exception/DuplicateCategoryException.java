package com.coffeeshop.common.exception;

public class DuplicateCategoryException extends RuntimeException {

    public DuplicateCategoryException(String name) {
        super("Category name already exists: " + name);
    }
}