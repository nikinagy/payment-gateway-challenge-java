package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.exception.PaymentProcessingException;
import com.checkout.payment.gateway.exception.ValidationException;
import com.checkout.payment.gateway.model.GetPaymentResponse;
import com.checkout.payment.gateway.model.PostAcquiringBankResponse;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PaymentGatewayServiceTest {

  @Mock
  private PaymentsRepository paymentsRepository;

  @Mock
  private PaymentValidator paymentValidator;

  @Mock
  private AcquiringBankService acquiringBankService;

  private PaymentGatewayService service;
  private PostPaymentRequest paymentRequest;

  @BeforeEach
  void setUp() {
    service = new PaymentGatewayService(paymentsRepository, paymentValidator, acquiringBankService);
    paymentRequest = new PostPaymentRequest();
    paymentRequest.setCardNumber("1234567890123456");
    paymentRequest.setExpiryMonth(12);
    paymentRequest.setExpiryYear(2025);
    paymentRequest.setCurrency("GBP");
    paymentRequest.setAmount(100);
  }

  @Nested
  @DisplayName("Get Payment Information Tests")
  class GetPaymentTests {

    @Test
    @DisplayName("getPaymentById returns payment when found")
    void getPaymentById_ReturnsPayment_WhenFound() {
      UUID paymentId = UUID.randomUUID();
      PostPaymentResponse storedPayment = new PostPaymentResponse();

      when(paymentsRepository.get(paymentId)).thenReturn(Optional.of(storedPayment));

      GetPaymentResponse result = service.getPaymentById(paymentId);
      assertThat(result).isNotNull();
      verify(paymentsRepository).get(paymentId);
    }

    @Test
    @DisplayName("getPaymentById throws EventProcessingException when payment not found")
    void getPaymentById_ThrowsEventProcessingException_WhenNotFound() {
      UUID paymentId = UUID.randomUUID();

      when(paymentsRepository.get(paymentId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.getPaymentById(paymentId))
          .isInstanceOf(EventProcessingException.class)
          .hasMessageContaining("Invalid ID");
      verify(paymentsRepository).get(paymentId);
    }
  }

  @Nested
  @DisplayName("Process Payment Tests")
  class ProcessPaymentTests {

    @Test
    @DisplayName("processPayment returns AUTHORIZED when bank authorizes payment")
    void processPayment_ReturnsAuthorized_WhenBankAuthorizes() {
      PostAcquiringBankResponse bankResponse = new PostAcquiringBankResponse();
      bankResponse.setAuthorized(true);
      bankResponse.setAuthorizationCode("AUTH123");

      when(acquiringBankService.processPayment(paymentRequest)).thenReturn(bankResponse);

      PostPaymentResponse result = service.processPayment(paymentRequest);

      assertThat(result).isNotNull();
      assertThat(result.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
      assertThat(result.getCardNumberLastFour()).isEqualTo("3456");
      assertThat(result.getAmount()).isEqualTo(100);
      assertThat(result.getCurrency()).isEqualTo("GBP");

      verify(paymentValidator).validate(paymentRequest);
      verify(acquiringBankService).processPayment(paymentRequest);
      verify(paymentsRepository).add(result);
    }

    @Test
    @DisplayName("processPayment returns DECLINED when bank declines payment")
    void processPayment_ReturnsDeclined_WhenBankDeclines() {
      PostAcquiringBankResponse bankResponse = new PostAcquiringBankResponse();
      bankResponse.setAuthorized(false);

      when(acquiringBankService.processPayment(paymentRequest)).thenReturn(bankResponse);

      PostPaymentResponse result = service.processPayment(paymentRequest);

      assertThat(result).isNotNull();
      assertThat(result.getStatus()).isEqualTo(PaymentStatus.DECLINED);
      assertThat(result.getCardNumberLastFour()).isEqualTo("3456");
      assertThat(result.getAmount()).isEqualTo(100);
      assertThat(result.getCurrency()).isEqualTo("GBP");

      verify(paymentsRepository).add(result);
    }

    @Test
    @DisplayName("processPayment persists payment with correct details")
    void processPayment_PersistsPaymentWithCorrectDetails() {
      PostAcquiringBankResponse bankResponse = new PostAcquiringBankResponse();
      bankResponse.setAuthorized(true);

      when(acquiringBankService.processPayment(paymentRequest)).thenReturn(bankResponse);

      service.processPayment(paymentRequest);

      ArgumentCaptor<PostPaymentResponse> captor = ArgumentCaptor.forClass(PostPaymentResponse.class);
      verify(paymentsRepository).add(captor.capture());

      PostPaymentResponse captured = captor.getValue();
      assertThat(captured.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
      assertThat(captured.getCardNumberLastFour()).isEqualTo("3456");
      assertThat(captured.getCurrency()).isEqualTo("GBP");
      assertThat(captured.getAmount()).isEqualTo(100);
      assertThat(captured.getExpiryMonth()).isEqualTo(12);
      assertThat(captured.getExpiryYear()).isEqualTo(2025);
    }

    @Test
    @DisplayName("processPayment returns REJECTED when validation fails")
    void processPayment_ReturnsRejected_WhenValidationFails() {
      doThrow(new ValidationException("Invalid card number"))
          .when(paymentValidator)
          .validate(paymentRequest);

      PostPaymentResponse result = service.processPayment(paymentRequest);

      assertThat(result).isNotNull();
      assertThat(result.getStatus()).isEqualTo(PaymentStatus.REJECTED);

      verify(acquiringBankService, never()).processPayment(paymentRequest);
      verify(paymentsRepository).add(result);
    }

    @Test
    @DisplayName("processPayment persists rejected payment when validation fails")
    void processPayment_PersistsRejectedPayment_WhenValidationFails() {
      doThrow(new ValidationException("Invalid card"))
          .when(paymentValidator)
          .validate(paymentRequest);

      service.processPayment(paymentRequest);

      ArgumentCaptor<PostPaymentResponse> captor = ArgumentCaptor.forClass(PostPaymentResponse.class);
      verify(paymentsRepository).add(captor.capture());

      PostPaymentResponse captured = captor.getValue();
      assertThat(captured.getStatus()).isEqualTo(PaymentStatus.REJECTED);
      assertThat(captured.getCardNumberLastFour()).isEqualTo("3456");
      assertThat(captured.getCurrency()).isEqualTo("GBP");
      assertThat(captured.getAmount()).isEqualTo(100);
      assertThat(captured.getExpiryMonth()).isEqualTo(12);
      assertThat(captured.getExpiryYear()).isEqualTo(2025);
    }

    @Test
    @DisplayName("processPayment returns REJECTED when bank processing fails")
    void processPayment_ReturnsRejected_WhenBankFails() {
      doThrow(new PaymentProcessingException("Bank unavailable", null))
          .when(acquiringBankService)
          .processPayment(paymentRequest);

      PostPaymentResponse result = service.processPayment(paymentRequest);

      assertThat(result).isNotNull();
      assertThat(result.getStatus()).isEqualTo(PaymentStatus.REJECTED);

      verify(paymentsRepository).add(result);
    }

    @Test
    @DisplayName("processPayment persists rejected payment when bank fails")
    void processPayment_PersistsRejectedPayment_WhenBankFails() {
      doThrow(new PaymentProcessingException("Connection timeout", null))
          .when(acquiringBankService)
          .processPayment(paymentRequest);

      service.processPayment(paymentRequest);

      ArgumentCaptor<PostPaymentResponse> captor = ArgumentCaptor.forClass(PostPaymentResponse.class);
      verify(paymentsRepository).add(captor.capture());

      PostPaymentResponse captured = captor.getValue();
      assertThat(captured.getStatus()).isEqualTo(PaymentStatus.REJECTED);
      assertThat(captured.getCardNumberLastFour()).isEqualTo("3456");
      assertThat(captured.getCurrency()).isEqualTo("GBP");
      assertThat(captured.getAmount()).isEqualTo(100);
      assertThat(captured.getExpiryMonth()).isEqualTo(12);
      assertThat(captured.getExpiryYear()).isEqualTo(2025);
    }

    @Test
    @DisplayName("processPayment generates unique paymentId for each request")
    void processPayment_GeneratesUniquePaymentIds() {
      PostAcquiringBankResponse bankResponse = new PostAcquiringBankResponse();
      bankResponse.setAuthorized(true);

      when(acquiringBankService.processPayment(paymentRequest)).thenReturn(bankResponse);

      PostPaymentResponse result1 = service.processPayment(paymentRequest);
      PostPaymentResponse result2 = service.processPayment(paymentRequest);

      assertThat(result1.getId()).isNotEqualTo(result2.getId());
    }

    @Test
    @DisplayName("processPayment handles minimum amount")
    void processPayment_HandlesMinimumAmount() {
      paymentRequest.setAmount(1);
      PostAcquiringBankResponse bankResponse = new PostAcquiringBankResponse();
      bankResponse.setAuthorized(true);

      when(acquiringBankService.processPayment(paymentRequest)).thenReturn(bankResponse);

      PostPaymentResponse result = service.processPayment(paymentRequest);
      assertThat(result.getAmount()).isEqualTo(1);
    }

    @Test
    @DisplayName("processPayment handles large amount")
    void processPayment_HandlesLargeAmount() {
      paymentRequest.setAmount(999999999);
      PostAcquiringBankResponse bankResponse = new PostAcquiringBankResponse();
      bankResponse.setAuthorized(true);

      when(acquiringBankService.processPayment(paymentRequest)).thenReturn(bankResponse);

      PostPaymentResponse result = service.processPayment(paymentRequest);
      assertThat(result.getAmount()).isEqualTo(999999999);
    }

    @Test
    @DisplayName("processPayment handles different currencies")
    void processPayment_HandlesDifferentCurrencies() {
      paymentRequest.setCurrency("USD");
      PostAcquiringBankResponse bankResponse = new PostAcquiringBankResponse();
      bankResponse.setAuthorized(true);

      when(acquiringBankService.processPayment(paymentRequest)).thenReturn(bankResponse);

      PostPaymentResponse result = service.processPayment(paymentRequest);
      assertThat(result.getCurrency()).isEqualTo("USD");
    }

    @Test
    @DisplayName("processPayment extracts last 4 digits of card number correctly")
    void processPayment_ExtractsLastFourDigits_Correctly() {
      paymentRequest.setCardNumber("4532015112830366");
      PostAcquiringBankResponse bankResponse = new PostAcquiringBankResponse();
      bankResponse.setAuthorized(true);

      when(acquiringBankService.processPayment(paymentRequest)).thenReturn(bankResponse);

      PostPaymentResponse result = service.processPayment(paymentRequest);
      assertThat(result.getCardNumberLastFour()).isEqualTo("0366");
    }
  }
}
