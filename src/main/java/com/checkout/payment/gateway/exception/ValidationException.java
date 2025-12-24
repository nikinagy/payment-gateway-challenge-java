package com.checkout.payment.gateway.exception;

/**
 * Exception thrown when validation of post payment request data fails.
 */
public class ValidationException extends RuntimeException {

  public ValidationException(String message) {
    super(message);
  }
}
