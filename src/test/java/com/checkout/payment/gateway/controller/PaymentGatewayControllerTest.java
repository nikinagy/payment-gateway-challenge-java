package com.checkout.payment.gateway.controller;


import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

/**
 * Note: The tests in this test class are integration tests rather than pure unit tests.
 * I kept them as integration tests because they verify real Spring wiring and application
 * behavior, which seems aligned with the existing test intent.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PaymentGatewayControllerTest {

  @Autowired
  private MockMvc mvc;
  @Autowired
  PaymentsRepository paymentsRepository;

  static GenericContainer<?> bankSimulator;

  // Start the testcontainer with the bank simulator imposters before all tests
  @BeforeAll
  static void startContainer() {
    bankSimulator = new GenericContainer<>("bbyars/mountebank:2.8.1")
        .withExposedPorts(2525, 8080)
        .withCommand(
            "--configfile", "/var/imposters/bank_simulator.ejs", "--allowInjection"
        )
        .withCopyFileToContainer(
            MountableFile.forHostPath("imposters/bank_simulator.ejs"),
            "/var/imposters/bank_simulator.ejs"
        )
        .waitingFor(Wait.forListeningPort().withStartupTimeout(java.time.Duration.ofSeconds(60)))
        .waitingFor(Wait.forListeningPort().forPorts(2525, 8080));
    bankSimulator.start();
  }

  @AfterAll
  static void stopContainer() {
    if (bankSimulator != null) {
      bankSimulator.stop();
    }
  }

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add(
        "acquiring.bank.url",
        () -> "http://" + bankSimulator.getHost() + ":" + bankSimulator.getMappedPort(8080)
    );
  }

  @Test
  void whenPaymentWithIdExistThenCorrectPaymentIsReturned() throws Exception {
    PostPaymentResponse payment = new PostPaymentResponse();
    payment.setId(UUID.randomUUID());
    payment.setAmount(10);
    payment.setCurrency("USD");
    payment.setStatus(PaymentStatus.AUTHORIZED);
    payment.setExpiryMonth(12);
    payment.setExpiryYear(2024);
    payment.setCardNumberLastFour("4321");

    paymentsRepository.add(payment);

    mvc.perform(MockMvcRequestBuilders.get("/payment/" + payment.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(payment.getStatus().getName()))
        .andExpect(jsonPath("$.cardNumberLastFour").value(payment.getCardNumberLastFour()))
        .andExpect(jsonPath("$.expiryMonth").value(payment.getExpiryMonth()))
        .andExpect(jsonPath("$.expiryYear").value(payment.getExpiryYear()))
        .andExpect(jsonPath("$.currency").value(payment.getCurrency()))
        .andExpect(jsonPath("$.amount").value(payment.getAmount()));
  }

  @Test
  void whenPaymentWithIdDoesNotExistThen404IsReturned() throws Exception {
    mvc.perform(MockMvcRequestBuilders.get("/payment/" + UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Page not found"));
  }

  @Test
  @DisplayName("processPayment returns AUTHORIZED status when payment succeeds")
  void processPayment_ReturnsAuthorizedStatus_WhenPaymentSucceeds() throws Exception {
    String requestBody = """
      {
        "card_number": "4532015112830369",
        "expiry_month": 12,
        "expiry_year": 2026,
        "cvv": "123",
        "amount": 100,
        "currency": "GBP"
      }
      """;

    mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType("application/json")
            .content(requestBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(PaymentStatus.AUTHORIZED.getName()))
        .andExpect(jsonPath("$.id").isNotEmpty())
        .andExpect(jsonPath("$.card_number_last_four").value("0369"))
        .andExpect(jsonPath("$.expiry_month").value(12))
        .andExpect(jsonPath("$.expiry_year").value(2026))
        .andExpect(jsonPath("$.amount").value(100))
        .andExpect(jsonPath("$.currency").value("GBP"));
  }

  @Test
  @DisplayName("processPayment returns REJECTED status when card number is missing")
  void processPayment_ReturnsRejected_WhenCardNumberIsMissing() throws Exception {
    String requestBody = """
    {
      "expiry_month": 12,
      "expiry_year": 2026,
      "cvv": "123",
      "amount": 100,
      "currency": "GBP"
    }
    """;

    mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType("application/json")
            .content(requestBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(PaymentStatus.REJECTED.getName()));
  }

  @Test
  @DisplayName("processPayment returns REJECTED when amount is negative")
  void processPayment_ReturnsRejected_WhenAmountIsNegative() throws Exception {
    String requestBody = """
    {
      "card_number": "4532015112830369",
      "expiry_month": 12,
      "expiry_year": 2026,
      "cvv": "123",
      "amount": -100,
      "currency": "GBP"
    }
    """;

    mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType("application/json")
            .content(requestBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(PaymentStatus.REJECTED.getName()));
  }

  @Test
  @DisplayName("processPayment returns REJECTED when amount is zero")
  void processPayment_ReturnsRejected_WhenAmountIsZero() throws Exception {
    String requestBody = """
    {
      "card_number": "4532015112830369",
      "expiry_month": 12,
      "expiry_year": 2026,
      "cvv": "123",
      "amount": 0,
      "currency": "GBP"
    }
    """;

    mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType("application/json")
            .content(requestBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(PaymentStatus.REJECTED.getName()));
  }

  @Test
  @DisplayName("processPayment returns REJECTED when expiry month is invalid")
  void processPayment_ReturnsRejected_WhenExpiryMonthIsInvalid() throws Exception {
    String requestBody = """
    {
      "card_number": "4532015112830369",
      "expiry_month": 13,
      "expiry_year": 2026,
      "cvv": "123",
      "amount": 100,
      "currency": "GBP"
    }
    """;

    mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType("application/json")
            .content(requestBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(PaymentStatus.REJECTED.getName()));
  }

  @Test
  @DisplayName("processPayment returns REJECTED when expiry year is in the past")
  void processPayment_ReturnsRejected_WhenExpiryYearIsInPast() throws Exception {
    String requestBody = """
    {
      "card_number": "4532015112830369",
      "expiry_month": 12,
      "expiry_year": 2020,
      "cvv": "123",
      "amount": 100,
      "currency": "GBP"
    }
    """;

    mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType("application/json")
            .content(requestBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(PaymentStatus.REJECTED.getName()));
  }

  @Test
  @DisplayName("processPayment returns REJECTED when CVV is missing")
  void processPayment_ReturnsRejected_WhenCvvIsMissing() throws Exception {
    String requestBody = """
    {
      "card_number": "4532015112830369",
      "expiry_month": 12,
      "expiry_year": 2026,
      "amount": 100,
      "currency": "GBP"
    }
    """;

    mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType("application/json")
            .content(requestBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(PaymentStatus.REJECTED.getName()));
  }

  @Test
  @DisplayName("processPayment returns REJECTED when currency is missing")
  void processPayment_ReturnsRejected_WhenCurrencyIsMissing() throws Exception {
    String requestBody = """
    {
      "card_number": "4532015112830369",
      "expiry_month": 12,
      "expiry_year": 2026,
      "cvv": "123",
      "amount": 100
    }
    """;

    mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType("application/json")
            .content(requestBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(PaymentStatus.REJECTED.getName()));
  }

  @Test
  @DisplayName("processPayment returns REJECTED when currency is not valid")
  void processPayment_ReturnsRejected_WhenCurrencyIsNotValid() throws Exception {
    String requestBody = """
    {
      "card_number": "4532015112830369",
      "expiry_month": 12,
      "expiry_year": 2026,
      "cvv": "123",
      "amount": 100,
      "currency": "HUF"
    }
    """;

    mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType("application/json")
            .content(requestBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(PaymentStatus.REJECTED.getName()));
  }

  @Test
  @DisplayName("processPayment returns REJECTED when CVV is invalid format")
  void processPayment_ReturnsRejected_WhenCvvIsInvalidFormat() throws Exception {
    String requestBody = """
    {
      "card_number": "4532015112830369",
      "expiry_month": 12,
      "expiry_year": 2026,
      "cvv": "12",
      "amount": 100,
      "currency": "GBP"
    }
    """;

    mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType("application/json")
            .content(requestBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(PaymentStatus.REJECTED.getName()));
  }

  @Test
  @DisplayName("processPayment returns REJECTED when card number is invalid format")
  void processPayment_ReturnsRejected_WhenCardNumberIsInvalidFormat() throws Exception {
    String requestBody = """
    {
      "card_number": "1234",
      "expiry_month": 12,
      "expiry_year": 2026,
      "cvv": "123",
      "amount": 100,
      "currency": "GBP"
    }
    """;

    mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType("application/json")
            .content(requestBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(PaymentStatus.REJECTED.getName()));
  }

  @Test
  @DisplayName("processPayment with large amount succeeds")
  void processPayment_WithLargeAmount_Succeeds() throws Exception {
    String requestBody = """
    {
      "card_number": "4532015112830369",
      "expiry_month": 12,
      "expiry_year": 2026,
      "cvv": "123",
      "amount": 999999,
      "currency": "GBP"
    }
    """;

    mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType("application/json")
            .content(requestBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.amount").value(999999));
  }

  @Test
  @DisplayName("processPayment with different currency succeeds")
  void processPayment_WithDifferentCurrency_Succeeds() throws Exception {
    String requestBody = """
    {
      "card_number": "4532015112830369",
      "expiry_month": 12,
      "expiry_year": 2026,
      "cvv": "123",
      "amount": 100,
      "currency": "EUR"
    }
    """;

    mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType("application/json")
            .content(requestBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currency").value("EUR"));
  }
}
