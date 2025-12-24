package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.exception.PaymentProcessingException;
import com.checkout.payment.gateway.model.PostAcquiringBankResponse;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class AcquiringBankService {

  private static final Logger LOG = LoggerFactory.getLogger(AcquiringBankService.class);
  private static final String BANK_URL = "http://localhost:8080/payments";
  private final RestTemplate restTemplate;

  public AcquiringBankService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  /**
   * Processes a payment request through the Acquiring Bank.
   *
   * @param bankRequest the payment request
   * @return the bank response
   * @throws PaymentProcessingException if the bank request fails
   */
  public PostAcquiringBankResponse processPayment(PostPaymentRequest bankRequest) throws PaymentProcessingException {
    LOG.debug("Processing payment through Acquiring Bank.");
    try {
      PostAcquiringBankResponse response = restTemplate.postForObject(
          BANK_URL,
          bankRequest,
          PostAcquiringBankResponse.class
      );
      LOG.debug("Payment processed successfully");
      return response;
    } catch (HttpClientErrorException.BadRequest e) {
      LOG.error("Bad request sent to Acquiring Bank: {}", e.getResponseBodyAsString());
      throw new PaymentProcessingException("Invalid payment request", e);
    } catch (HttpServerErrorException.ServiceUnavailable e) {
      LOG.error("Acquiring Bank service is unavailable: {}", e.getResponseBodyAsString());
      throw new PaymentProcessingException("Acquiring Bank unavailable", e);
    } catch (RestClientException e) {
      // In production code, we should be implementing exponential backoff with jitter
      // and a circuit breaker (e.g., using Resilience4j) for transient failures
      LOG.error("Error processing payment with Acquiring Bank: {}", e.getMessage());
      throw new PaymentProcessingException("Bank request failed", e);
    }
  }
}
