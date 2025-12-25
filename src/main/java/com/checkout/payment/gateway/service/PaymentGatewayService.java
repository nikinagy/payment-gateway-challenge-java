package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.exception.PaymentProcessingException;
import com.checkout.payment.gateway.exception.ValidationException;
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
  private final PaymentValidator paymentValidator;
  private final AcquiringBankService acquiringBankService;

  public PaymentGatewayService(
      PaymentsRepository paymentsRepository,
      PaymentValidator paymentValidator,
      AcquiringBankService acquiringBankService
  ) {
    this.paymentsRepository = paymentsRepository;
    this.paymentValidator = paymentValidator;
    this.acquiringBankService = acquiringBankService;
  }

  /**
   * Retrieves a payment by its unique identifier.
   * @param id The payment id which will be used to retrieve the payment details
   * @return the payment response
   * @throws EventProcessingException if payment not found
   */
  public GetPaymentResponse getPaymentById(UUID id) {
    LOG.debug("Requesting access to payment with ID {}", id);
    // Reusing PostPaymentResponse as a simple in-memory storage model
    // as per the provided test repository
    PostPaymentResponse stored = paymentsRepository.get(id)
        .orElseThrow(() -> new EventProcessingException("Invalid ID"));
    return GetPaymentResponse.from(stored);
  }

  /**
   * Processes a payment request, validates it and sends it to acquiring bank.
   * @param paymentRequest the payment request to process
   * @return the payment response
   */
  public PostPaymentResponse processPayment(PostPaymentRequest paymentRequest) {
    UUID paymentId = UUID.randomUUID();
    LOG.debug("Processing payment with ID {}", paymentId);

    // Validating the request
    try {
      paymentValidator.validate(paymentRequest);
    } catch (ValidationException e) {
      LOG.warn("Payment validation failed: {}", e.getMessage());
      // Storing the rejected payment due to validation failure
      return createAndPersistResponse(paymentId, PaymentStatus.REJECTED, paymentRequest);
    }

    try {
      // Sending payment to acquiring bank
      var bankResponse = acquiringBankService.processPayment(paymentRequest);
      var response = createAndPersistResponse(
          paymentId,
          bankResponse.isAuthorized() ? PaymentStatus.AUTHORIZED : PaymentStatus.DECLINED,
          paymentRequest
      );
      LOG.debug("Payment with ID {} processed successfully with status {}", paymentId, response.getStatus());
      return response;
    } catch (PaymentProcessingException e) {
      LOG.warn("Acquiring bank processing failed: {}", e.getMessage());
      // Storing the rejected payment due to bank failure
      return createAndPersistResponse(paymentId, PaymentStatus.REJECTED, paymentRequest);
    }
  }

  private PostPaymentResponse createAndPersistResponse(
      UUID paymentId,
      PaymentStatus status,
      PostPaymentRequest paymentRequest
  ) {
    var response = new PostPaymentResponse(paymentId, status, paymentRequest);
    paymentsRepository.add(response);
    LOG.debug("Payment {} persisted with status {}", paymentId, status);
    return response;
  }
}
