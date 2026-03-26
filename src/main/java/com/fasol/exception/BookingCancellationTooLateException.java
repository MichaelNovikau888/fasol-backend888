package com.fasol.exception;
public class BookingCancellationTooLateException extends RuntimeException {
    public BookingCancellationTooLateException(String msg) { super(msg); }
}
