package com.carddemo.online;

import com.carddemo.vsam.record.AccountRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class BillPaymentControllerTest {

    private static Path acctPath;

    static {
        try {
            String tmpDir = System.getProperty("java.io.tmpdir");
            acctPath = Paths.get(tmpDir, "acctdat_bill_" + System.nanoTime() + ".bin");

            AccountRecord acct = new AccountRecord(
                "12345678901", "A", new BigDecimal("5000.00"), new BigDecimal("10000.00"),
                new BigDecimal("2000.00"), new BigDecimal("1500.00"), new BigDecimal("800.00"),
                "20200101", "20250131", "20200101", "GRP001"
            );
            Files.write(acctPath, acct.encode());
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("carddemo.acct.path", () -> acctPath.toString());
    }

    @Test
    void processPaymentSuccess() throws Exception {
        CardDemoCommarea commarea = new CardDemoCommarea();
        commarea.userId = "USER0001";
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("commarea", commarea);

        String paymentBody = "{\"accountId\":\"12345678901\",\"paymentAmount\":1000.00}";

        mockMvc.perform(post("/api/bill-payment")
                .contentType("application/json")
                .content(paymentBody)
                .session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accountId").value("12345678901"))
            .andExpect(jsonPath("$.paymentAmount").value("1000.00"))
            .andExpect(jsonPath("$.newBalance").value("4000.00"));
    }

    @Test
    void paymentExceedsBalance() throws Exception {
        CardDemoCommarea commarea = new CardDemoCommarea();
        commarea.userId = "USER0001";
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("commarea", commarea);

        String paymentBody = "{\"accountId\":\"12345678901\",\"paymentAmount\":6000.00}";

        mockMvc.perform(post("/api/bill-payment")
                .contentType("application/json")
                .content(paymentBody)
                .session(session))
            .andExpect(status().isBadRequest());
    }

    @Test
    void invalidAmount() throws Exception {
        CardDemoCommarea commarea = new CardDemoCommarea();
        commarea.userId = "USER0001";
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("commarea", commarea);

        String paymentBody = "{\"accountId\":\"12345678901\",\"paymentAmount\":0}";

        mockMvc.perform(post("/api/bill-payment")
                .contentType("application/json")
                .content(paymentBody)
                .session(session))
            .andExpect(status().isBadRequest());
    }

    @Test
    void noSession() throws Exception {
        String paymentBody = "{\"accountId\":\"12345678901\",\"paymentAmount\":1000.00}";

        mockMvc.perform(post("/api/bill-payment")
                .contentType("application/json")
                .content(paymentBody))
            .andExpect(status().isForbidden());
    }
}
