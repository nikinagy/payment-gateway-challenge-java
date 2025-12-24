package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.enums.SupportedCurrency;
import com.checkout.payment.gateway.exception.ValidationException;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import org.springframework.stereotype.Service;
import java.time.YearMonth;

@Service
public class PaymentValidator {

  private static final String CARD_NUMBER_PATTERN = "\\d{14,19}";
  private static final String CVV_PATTERN = "\\d{3,4}";

  /**
   * Validates the payment request.
   * <p>
   * The following requirements must be met:
   * </p>
   * <ul>
   *   <li>Card number: must be 14-19 digits and numeric</li>
   *   <li>Expiry month: must be between 1 and 12</li>
   *   <li>Expiry year: must be in the future</li>
   *   <li>Currency: must be a 3-letter code from the allowed enums</li>
   *   <li>Amount: must be an integer</li>
   *   <li>CVV: must be 3-4 digits and numeric</li>
   * </ul>
   * @param request The request to validate
   * @throws ValidationException if any validation rule fails
   */
  public void validate(PostPaymentRequest request) {
    if (request == null) {
      throw new ValidationException("Request cannot be null");
    }

    if (request.getCardNumber() == null) {
      throw new ValidationException("Card number is missing from the request");
    }

    if (!request.getCardNumber().matches(CARD_NUMBER_PATTERN)) {
      throw new ValidationException("Card number must be numeric and between 14 and 19 digits long");
    }

    if (request.getExpiryMonth() < 1 || request.getExpiryMonth() > 12) {
      throw new ValidationException("Expiry month must be between 1 and 12");
    }

    // Create a YearMonth for the card expiry
    YearMonth expiry = YearMonth.of(request.getExpiryYear(), request.getExpiryMonth());
    if (!expiry.isAfter(YearMonth.now())) {
      throw new ValidationException("Card has expired");
    }

    if (request.getCurrency() == null) {
      throw new ValidationException("Currency is missing from the request");
    }

    // No need to check for length as we will use enum to validate
    if (!isSupported(request.getCurrency())) {
      throw new ValidationException("Unsupported currency");
    }

    if (request.getAmount() <= 0) {
      throw new ValidationException("Amount must be greater than zero");
    }

    if (request.getCvv() == null) {
      throw new ValidationException("CVV is missing from the request");
    }

    if (!request.getCvv().matches(CVV_PATTERN)) {
      throw new ValidationException("CVV must be 3-4 digits and numeric");
    }
  }

  /**
   * Checks if the currency is supported.
   * @param currency The currency the payment was made in
   * @return {@code true} if supported; {@code false} otherwise
   */
  private boolean isSupported(String currency) {
    try {
      SupportedCurrency.valueOf(currency.toUpperCase());
      return true;
    } catch (IllegalArgumentException ex) {
      return false;
    }
  }
}
