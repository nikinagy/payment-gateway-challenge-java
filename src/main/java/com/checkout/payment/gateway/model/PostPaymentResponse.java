package com.checkout.payment.gateway.model;

import com.checkout.payment.gateway.enums.PaymentStatus;
import java.util.UUID;

/**
 * Response returned after a payment request was sent successfully to the acquiring bank.
 * Contains the payment identifier, status, and masked card details.
 * Card numbers are masked to show only the last 4 digits for compliance.
 */
public class PostPaymentResponse {
  private UUID id;
  private PaymentStatus status;
  // Changed to String in case number starts with leading zeros
  private String cardNumberLastFour;
  private int expiryMonth;
  private int expiryYear;
  private String currency;
  private int amount;

  public PostPaymentResponse() { }

  public PostPaymentResponse(UUID paymentId, PaymentStatus status, PostPaymentRequest request) {
    this.id = paymentId;
    this.status = status;
    this.cardNumberLastFour = request.getCardNumberLastFour();
    this.expiryMonth = request.getExpiryMonth();
    this.expiryYear = request.getExpiryYear();
    this.currency = request.getCurrency();
    this.amount = request.getAmount();
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public PaymentStatus getStatus() {
    return status;
  }

  public void setStatus(PaymentStatus status) {
    this.status = status;
  }

  public String getCardNumberLastFour() {
    return cardNumberLastFour;
  }

  public void setCardNumberLastFour(String cardNumberLastFour) {
    this.cardNumberLastFour = cardNumberLastFour;
  }

  public int getExpiryMonth() {
    return expiryMonth;
  }

  public void setExpiryMonth(int expiryMonth) {
    this.expiryMonth = expiryMonth;
  }

  public int getExpiryYear() {
    return expiryYear;
  }

  public void setExpiryYear(int expiryYear) {
    this.expiryYear = expiryYear;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public int getAmount() {
    return amount;
  }

  public void setAmount(int amount) {
    this.amount = amount;
  }

  @Override
  public String toString() {
    return "GetPaymentResponse{" +
        "id=" + id +
        ", status=" + status +
        ", cardNumberLastFour=" + cardNumberLastFour +
        ", expiryMonth=" + expiryMonth +
        ", expiryYear=" + expiryYear +
        ", currency='" + currency + '\'' +
        ", amount=" + amount +
        '}';
  }
}
