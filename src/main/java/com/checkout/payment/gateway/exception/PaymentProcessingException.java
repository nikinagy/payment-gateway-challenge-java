package com.checkout.payment.gateway.exception;

public class PaymentProcessingException extends RuntimeException {

  public PaymentProcessingException(String message, Throwable cause) {
    super(message, cause);
  }
}
