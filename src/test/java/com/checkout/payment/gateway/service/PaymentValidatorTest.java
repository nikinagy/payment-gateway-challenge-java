package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.exception.ValidationException;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PaymentValidatorTest {

  private PaymentValidator validator;
  private PostPaymentRequest request;

  @BeforeEach
  void setUp() {
    validator = new PaymentValidator();
    request = new PostPaymentRequest();
    request.setCardNumber("12345678901234");
    request.setExpiryMonth(12);
    request.setExpiryYear(YearMonth.now().plusYears(1).getYear());
    request.setCurrency("USD");
    request.setAmount(100);
    request.setCvv("123");
  }

  @Test
  @DisplayName("validate() throws ValidationException when request is null")
  void validate_shouldThrowValidationException_whenRequestIsNull() {
     ValidationException ex =
         assertThrows(ValidationException.class, () -> validator.validate(null));
     assertThat(ex.getMessage()).contains("Request cannot be null");
  }

  @Test
  @DisplayName("validate() succeeds when all fields are valid")
  void validate_succeeds_whenAllFieldsAreValid() {
    assertDoesNotThrow(() -> validator.validate(request));
  }

  @Nested
  @DisplayName("Card Number Validation Tests")
  class CardNumberValidationTest {

    @Test
    @DisplayName("validate() throws ValidationException when card number is null")
    void validate_shouldThrowValidationException_whenCardNumberIsNull() {
      request.setCardNumber(null);
      ValidationException ex = assertThrows(
          ValidationException.class,
          () -> validator.validate(request)
      );
      assertThat(ex.getMessage()).contains("Card number is missing from the request");
    }

    @Test
    @DisplayName("validate() throws ValidationException when card number is too short")
    void validate_shouldThrowValidationException_whenCardNumberIsTooShort() {
      request.setCardNumber("1234567890123");
      ValidationException ex = assertThrows(
          ValidationException.class,
          () -> validator.validate(request)
      );
      assertThat(ex.getMessage()).contains("Card number");
    }

    @Test
    @DisplayName("validate() throws ValidationException when card number is too long")
    void validate_shouldThrowValidationException_whenCardNumberIsTooLong() {
      request.setCardNumber("123456789012345678901");
      ValidationException ex = assertThrows(
          ValidationException.class,
          () -> validator.validate(request)
      );
      assertThat(ex.getMessage()).contains("Card number");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234567890123a", "12345678901234!", "abcd5678901234"})
    @DisplayName("validate() throws ValidationException when card number is non-numeric")
    void validate_shouldThrowValidationException_whenCardNumberIsNonNumeric(String cardNumber) {
      request.setCardNumber(cardNumber);
      ValidationException ex = assertThrows(
          ValidationException.class,
          () -> validator.validate(request)
      );
      assertThat(ex.getMessage()).contains("Card number");
    }

    @Test
    @DisplayName("validate() succeeds when card number has 14 digits")
    void validate_succeeds_whenCardNumberHas14Digits() {
      request.setCardNumber("12345678901234");
      assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    @DisplayName("validate() succeeds when card number has 19 digits")
    void validate_succeeds_whenCardNumberHas19Digits() {
      request.setCardNumber("1234567890123456789");
      assertDoesNotThrow(() -> validator.validate(request));
    }
  }

  @Nested
  @DisplayName("Expiration Validation Tests")
  class ExpirationValidationTest {

    // Expiry month tests
    @Test
    @DisplayName("validate() throws ValidationException when expiry month is less than 1")
    void validate_shouldThrowValidationException_whenExpiryMonthIsLessThan1() {
      request.setExpiryMonth(0);
      ValidationException ex = assertThrows(
          ValidationException.class,
          () -> validator.validate(request)
      );
      assertThat(ex.getMessage()).contains("Expiry month");
    }

    @Test
    @DisplayName("validate() throws ValidationException when expiry month is greater than 12")
    void validate_shouldThrowValidationException_whenExpiryMonthIsHigherThan12() {
      request.setExpiryMonth(13);
      ValidationException ex = assertThrows(
          ValidationException.class,
          () -> validator.validate(request)
      );
      assertThat(ex.getMessage()).contains("Expiry month");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 6, 12})
    @DisplayName("validate() succeeds when expiry month is valid")
    void validate_succeeds_whenExpiryMothIsValid(int month) {
      request.setExpiryMonth(month);
      assertDoesNotThrow(() -> validator.validate(request));
    }

    // Expiration date tests
    @Test
    @DisplayName("validate() throws ValidationException when card is expired")
    void validate_shouldThrowValidationException_whenCardIsExpired() {
      request.setExpiryMonth(1);
      request.setExpiryYear(YearMonth.now().minusYears(1).getYear());
      ValidationException ex = assertThrows(
          ValidationException.class,
          () -> validator.validate(request)
      );
      assertThat(ex.getMessage()).contains("Card has expired");
    }

    @Test
    @DisplayName("validate() throws ValidationException when month is in the past this year")
    void validate_shouldThrowValidationException_whenMonthIsPast() {
      YearMonth now = YearMonth.now();
      request.setExpiryMonth(now.getMonthValue());
      request.setExpiryYear(now.getYear());
      ValidationException ex = assertThrows(
          ValidationException.class,
          () -> validator.validate(request)
      );
      assertThat(ex.getMessage()).contains("Card has expired");
    }

    @Test
    @DisplayName("validate() succeeds when expiry date is valid")
    void validate_succeeds_whenExpiryDateIsValid() {
      YearMonth future = YearMonth.now().plusMonths(1);
      request.setExpiryMonth(future.getMonthValue());
      request.setExpiryYear(future.getYear());
      assertDoesNotThrow(() -> validator.validate(request));
    }
  }

  @Nested
  @DisplayName("Currency Validation Tests")
  class CurrencyValidationTest {

    @Test
    @DisplayName("validate() throws ValidationException when currency is null")
    void validate_shouldThrowValidationException_whenCurrencyIsNull() {
      request.setCurrency(null);
      ValidationException ex = assertThrows(
          ValidationException.class,
          () -> validator.validate(request)
      );
      assertThat(ex.getMessage()).contains("Currency is missing from the request");
    }

    @Test
    @DisplayName("validate() throws ValidationException when currency is too short")
    void validate_shouldThrowValidationException_whenCurrencyIsTooShort() {
      request.setCurrency("US");
      ValidationException ex = assertThrows(
          ValidationException.class,
          () -> validator.validate(request)
      );
      assertThat(ex.getMessage()).contains("Unsupported currency");
    }

    @Test
    @DisplayName("validate() throws ValidationException when currency is too long")
    void validate_shouldThrowValidationException_whenCurrencyIsTooLong() {
      request.setCurrency("USDA");
      ValidationException ex = assertThrows(
          ValidationException.class,
          () -> validator.validate(request)
      );
      assertThat(ex.getMessage()).contains("Unsupported currency");
    }

    @Test
    @DisplayName("validate() throws ValidationException when currency is unsupported")
    void validate_shouldThrowValidationException_whenCurrencyIsUnsupported() {
      // Using hungarian forints
      request.setCurrency("HUF");
      ValidationException ex = assertThrows(
          ValidationException.class,
          () -> validator.validate(request)
      );
      assertThat(ex.getMessage()).contains("Unsupported currency");
    }

    @Test
    @DisplayName("validate() succeeds when currency is in lowercase")
    void validate_succeeds_whenCurrencyIsInLowercase() {
      request.setCurrency("usd");
      assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    @DisplayName("validate() succeeds when currency is in mixed case")
    void validate_succeeds_whenCurrencyIsInMixedCase() {
      request.setCurrency("UsD");
      assertDoesNotThrow(() -> validator.validate(request));
    }
  }

  @Nested
  @DisplayName("Amount Validation Tests")
  class AmountValidationTest {

    @Test
    @DisplayName("validate() throws ValidationException when amount is zero")
    void validate_shouldThrowValidationException_whenCAmountIsZero() {
      request.setAmount(0);
      ValidationException ex = assertThrows(
          ValidationException.class,
          () -> validator.validate(request)
      );
      assertThat(ex.getMessage()).contains("Amount");
    }

    @Test
    @DisplayName("validate() throws ValidationException when amount is negative")
    void validate_shouldThrowValidationException_whenCAmountIsNegative() {
      request.setAmount(-100);
      ValidationException ex = assertThrows(
          ValidationException.class,
          () -> validator.validate(request)
      );
      assertThat(ex.getMessage()).contains("Amount");
    }

    @Test
    @DisplayName("validate() succeeds when amount is positive")
    void validate_succeeds_whenAmountIsPositive() {
      request.setAmount(1);
      assertDoesNotThrow(() -> validator.validate(request));
    }
  }

  @Nested
  @DisplayName("CVV Validation Tests")
  class CVVValidationTest {

    @Test
    @DisplayName("validate() throws ValidationException when CVV is null")
    void validate_shouldThrowValidationException_whenCVVIsNull() {
      request.setCvv(null);
      ValidationException ex = assertThrows(
          ValidationException.class,
          () -> validator.validate(request)
      );
      assertThat(ex.getMessage()).contains("CVV is missing from the request");
    }

    @Test
    @DisplayName("validate() throws ValidationException when CVV is too short")
    void validate_shouldThrowValidationException_whenCVVIsTooShort() {
      request.setCvv("12");
      ValidationException ex = assertThrows(
          ValidationException.class,
          () -> validator.validate(request)
      );
      assertThat(ex.getMessage()).contains("CVV");
    }

    @Test
    @DisplayName("validate() throws ValidationException when CVV is too long")
    void validate_shouldThrowValidationException_whenCVVIsTooLong() {
      request.setCvv("12345");
      ValidationException ex = assertThrows(
          ValidationException.class,
          () -> validator.validate(request)
      );
      assertThat(ex.getMessage()).contains("CVV");
    }

    @ParameterizedTest
    @ValueSource(strings = {"12a", "1a34", "abc"})
    @DisplayName("validate() throws ValidationException when CVV is non-numeric")
    void validate_shouldThrowValidationException_whenCVVIsNonNumeric(String cvv) {
      request.setCvv(cvv);
      ValidationException ex = assertThrows(
          ValidationException.class,
          () -> validator.validate(request)
      );
      assertThat(ex.getMessage()).contains("CVV");
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "1234"})
    @DisplayName("validate() succeeds when CVV is valid")
    void validate_succeeds_whenCVVIsValid(String cvv) {
      request.setCvv(cvv);
      assertDoesNotThrow(() -> validator.validate(request));
    }
  }
}
