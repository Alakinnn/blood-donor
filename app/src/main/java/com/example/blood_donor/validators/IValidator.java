package com.example.blood_donor.validators;

import com.example.blood_donor.errors.AppException;

public interface IValidator<T> {
    void validate(T request) throws AppException;
}