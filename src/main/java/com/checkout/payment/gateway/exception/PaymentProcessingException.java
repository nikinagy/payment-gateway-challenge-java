package com.checkout.payment.gateway.exception;

/**
 * Custom exception class for handling payment processing errors from acquiring bank.
 */
public class PaymentProcessingException extends RuntimeException {

  public PaymentProcessingException(String message, Throwable cause) {
    super(message, cause);
  }
}
