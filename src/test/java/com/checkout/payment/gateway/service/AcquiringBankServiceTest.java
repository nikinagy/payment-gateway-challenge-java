package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.exception.PaymentProcessingException;
import com.checkout.payment.gateway.model.PostAcquiringBankResponse;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AcquiringBankServiceTest {

  private static final String BANK_URL = "http://localhost:8080/payments";

  @Mock
  private RestTemplate restTemplate;

  private AcquiringBankService service;
  private PostPaymentRequest paymentRequest;
  private PostAcquiringBankResponse bankResponse;

  @BeforeEach
  void setUp() {
    service = new AcquiringBankService(restTemplate);
    paymentRequest = new PostPaymentRequest();
    bankResponse = new PostAcquiringBankResponse();
  }

  @Test
  @DisplayName("processPayment succeeds when bank returns valid response")
  void processPayment_Succeeds() {
    bankResponse.setAuthorized(true);
    bankResponse.setAuthorizationCode("AUTH123");
    when(restTemplate.postForObject(BANK_URL, paymentRequest, PostAcquiringBankResponse.class))
        .thenReturn(bankResponse);

    PostAcquiringBankResponse result = service.processPayment(paymentRequest);
    assertThat(result).isNotNull();
    assertThat(result.isAuthorized()).isTrue();
    assertThat(result.getAuthorizationCode()).isEqualTo("AUTH123");
  }

  @Test
  @DisplayName("processPayment throws PaymentProcessingException on BadRequest")
  void processPayment_ThrowsOnBadRequest() {
    when(restTemplate.postForObject(anyString(), eq(paymentRequest), eq(PostAcquiringBankResponse.class)))
        .thenThrow(
            HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                HttpHeaders.EMPTY,
                null,
                null
            )
        );

    assertThatThrownBy(() -> service.processPayment(paymentRequest))
        .isInstanceOf(PaymentProcessingException.class)
        .hasMessageContaining("Invalid payment request");
  }

  @Test
  @DisplayName("processPayment throws PaymentProcessingException on ServiceUnavailable")
  void processPayment_ThrowsOnServiceUnavailable() {
    when(restTemplate.postForObject(anyString(), eq(paymentRequest), eq(PostAcquiringBankResponse.class)))
        .thenThrow(
            HttpServerErrorException.create(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Bank down",
                HttpHeaders.EMPTY,
                null,
                null
            )
        );

    assertThatThrownBy(() -> service.processPayment(paymentRequest))
        .isInstanceOf(PaymentProcessingException.class)
        .hasMessageContaining("Bank unavailable");
  }

  @Test
  @DisplayName("processPayment throws PaymentProcessingException on connection timeout")
  void processPayment_ThrowsOnTimeout() {
    when(restTemplate.postForObject(anyString(), eq(paymentRequest), eq(PostAcquiringBankResponse.class)))
        .thenThrow(new RestClientException("Connection timeout"));

    assertThatThrownBy(() -> service.processPayment(paymentRequest))
        .isInstanceOf(PaymentProcessingException.class)
        .hasMessageContaining("Bank request failed");
  }
}
