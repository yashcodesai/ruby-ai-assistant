package com.ruby.ruby.integration;

/** An online service (weather, Wikipedia) could not be reached or answered badly. */
public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
