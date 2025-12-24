package com.checkout.payment.gateway.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enum representing the status of a payment transaction.
 * Each status is associated with a specific string value for JSON serialization.
 * The possible statuses are:
 * <p>
 * Authorized - the payment was authorized by the call to the acquiring bank
 * Declined - the payment was declined by the call to the acquiring bank
 * Rejected - No payment could be created as invalid information was supplied to the payment gateway,
 * and therefore it has rejected the request without calling the acquiring bank.
 * </p>
 */
public enum PaymentStatus {
  AUTHORIZED("Authorized"),
  DECLINED("Declined"),
  REJECTED("Rejected");

  private final String name;

  PaymentStatus(String name) {
    this.name = name;
  }

  @JsonValue
  public String getName() {
    return this.name;
  }
}
