package com.gbreda.fila.web;

import org.springframework.http.HttpStatus;

public class InvalidFeedException extends ApiException {

    public InvalidFeedException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }
}
