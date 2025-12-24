package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.model.GetPaymentResponse;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PaymentGatewayService {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentGatewayService.class);

  private final PaymentsRepository paymentsRepository;

  public PaymentGatewayService(PaymentsRepository paymentsRepository) {
    this.paymentsRepository = paymentsRepository;
  }

  /**
   * Retrieves a payment by its unique identifier.
   * @param id The payment id which will be used to retrieve the payment details
   * @return the payment response
   * @throws EventProcessingException if payment not found
   */
  public GetPaymentResponse getPaymentById(UUID id) {
    LOG.debug("Requesting access to to payment with ID {}", id);
    // Reusing PostPaymentResponse as a simple in-memory storage model
    // as per the provided test repository
    PostPaymentResponse stored = paymentsRepository.get(id)
        .orElseThrow(() -> new EventProcessingException("Invalid ID"));
    return GetPaymentResponse.from(stored);
  }

  /**
   * Processes a payment request, validates it and sends it to acquiring bank.
   * @param paymentRequest the payment request to process
   * @return the generated payment UUID
   * TODO return PostPaymentResponse instead of UUID
   */
  public UUID processPayment(PostPaymentRequest paymentRequest) {
    return UUID.randomUUID();
  }
}
