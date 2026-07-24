package com.tiendanube.orchestration.controller;

import com.tiendanube.orchestration.controller.request.CreateTransactionRequest;
import com.tiendanube.orchestration.controller.response.OrchestrationResponse;
import com.tiendanube.orchestration.controller.response.ReceivableResponse;
import com.tiendanube.orchestration.controller.response.TransactionResponse;
import com.tiendanube.orchestration.service.OrchestrationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrchestrationService orchestrationService;

    @Test
    @DisplayName("POST /transactions should return 201 Created with OrchestrationResponse")
    void shouldCreateTransactionSuccessfully() throws Exception {
        final TransactionResponse transactionResponse = new TransactionResponse(
                "100", "340.50", "T-Shirt", "debit_card", "3486", "Fonsi Julian", "04/28"
        );
        final ReceivableResponse receivableResponse = new ReceivableResponse(
                "200", "100", "paid", "2026-07-21", "340.50", "6.81", "333.69"
        );
        final OrchestrationResponse response = new OrchestrationResponse(transactionResponse, receivableResponse);

        Mockito.when(orchestrationService.createTransaction(any(CreateTransactionRequest.class)))
                .thenReturn(response);

        final String payload = """
                {
                  "value": "340.50",
                  "description": "T-Shirt",
                  "method": "debit_card",
                  "cardNumber": "4532123456783486",
                  "cardHolderName": "Fonsi Julian",
                  "cardExpirationDate": "04/28",
                  "cardCvv": "290"
                }
                """;

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transaction.id").value("100"))
                .andExpect(jsonPath("$.transaction.cardNumber").value("3486"))
                .andExpect(jsonPath("$.receivable.id").value("200"))
                .andExpect(jsonPath("$.receivable.transaction_id").value("100"));
    }

    @Test
    @DisplayName("GET /transactions/{id} should return 200 OK with TransactionResponse")
    void shouldGetTransactionById() throws Exception {
        final TransactionResponse transactionResponse = new TransactionResponse(
                "100", "340.50", "T-Shirt", "debit_card", "3486", "Fonsi Julian", "04/28"
        );
        Mockito.when(orchestrationService.getTransactionById("100")).thenReturn(transactionResponse);

        mockMvc.perform(get("/transactions/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("100"))
                .andExpect(jsonPath("$.cardNumber").value("3486"));
    }

    @Test
    @DisplayName("GET /transactions should return 200 OK with list of transactions")
    void shouldGetAllTransactions() throws Exception {
        final TransactionResponse transactionResponse = new TransactionResponse(
                "100", "340.50", "T-Shirt", "debit_card", "3486", "Fonsi Julian", "04/28"
        );
        Mockito.when(orchestrationService.getAllTransactions()).thenReturn(List.of(transactionResponse));

        mockMvc.perform(get("/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("100"));
    }
}
