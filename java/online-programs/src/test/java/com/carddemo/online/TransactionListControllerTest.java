package com.carddemo.online;

import com.carddemo.vsam.record.TransactionRecord;
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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class TransactionListControllerTest {

    private static Path tranPath;

    static {
        try {
            String tmpDir = System.getProperty("java.io.tmpdir");
            tranPath = Paths.get(tmpDir, "transact_list_" + System.nanoTime() + ".bin");

            TransactionRecord tran1 = new TransactionRecord(
                "0000000000000001", "4532111111111111", "PUR", "5411", "POS",
                new BigDecimal("45.50"), "Gas", "20240615120000", "20240615120030",
                "MERCH001", "Shell", "NY", "10001"
            );
            TransactionRecord tran2 = new TransactionRecord(
                "0000000000000002", "4532111111111111", "WTD", "6000", "ATM",
                new BigDecimal("100.00"), "ATM", "20240615130000", "20240615130030",
                "ATM001", "ATM", "NY", "10001"
            );
            TransactionRecord tran3 = new TransactionRecord(
                "0000000000000003", "4532222222222222", "PUR", "5411", "POS",
                new BigDecimal("25.00"), "Coffee", "20240615140000", "20240615140030",
                "MERCH002", "Starbucks", "NY", "10001"
            );

            byte[] data = new byte[tran1.encode().length * 3];
            System.arraycopy(tran1.encode(), 0, data, 0, tran1.encode().length);
            System.arraycopy(tran2.encode(), 0, data, tran1.encode().length, tran2.encode().length);
            System.arraycopy(tran3.encode(), 0, data, tran1.encode().length * 2, tran3.encode().length);
            Files.write(tranPath, data);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("carddemo.tran.path", () -> tranPath.toString());
    }

    @Test
    void listAllTransactions() throws Exception {
        CardDemoCommarea commarea = new CardDemoCommarea();
        commarea.userId = "USER0001";
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("commarea", commarea);

        mockMvc.perform(get("/api/transactions")
                .session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.transactions", hasSize(3)))
            .andExpect(jsonPath("$.transactions[0].tranId").value("0000000000000001"));
    }

    @Test
    void filterByCardNum() throws Exception {
        CardDemoCommarea commarea = new CardDemoCommarea();
        commarea.userId = "USER0001";
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("commarea", commarea);

        mockMvc.perform(get("/api/transactions?cardNum=4532111111111111")
                .session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.transactions", hasSize(2)));
    }

    @Test
    void noSession() throws Exception {
        mockMvc.perform(get("/api/transactions"))
            .andExpect(status().isForbidden());
    }
}
