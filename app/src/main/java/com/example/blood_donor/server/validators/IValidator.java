package com.example.blood_donor.server.validators;

import com.example.blood_donor.server.errors.AppException;

public interface IValidator<T> {
    void validate(T request) throws AppException;
}