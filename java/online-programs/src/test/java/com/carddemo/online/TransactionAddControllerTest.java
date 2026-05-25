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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class TransactionAddControllerTest {

    private static Path tranPath;

    static {
        try {
            String tmpDir = System.getProperty("java.io.tmpdir");
            tranPath = Paths.get(tmpDir, "transact_add_" + System.nanoTime() + ".bin");

            TransactionRecord tran = new TransactionRecord(
                "0000000000000001", "4532111111111111", "PUR", "5411", "POS",
                new BigDecimal("45.50"), "Gas", "20240615120000", "20240615120030",
                "MERCH001", "Shell", "NY", "10001"
            );
            Files.write(tranPath, tran.encode());
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
    void addTransactionSuccess() throws Exception {
        CardDemoCommarea commarea = new CardDemoCommarea();
        commarea.userId = "USER0001";
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("commarea", commarea);

        String addBody = "{\"cardNum\":\"4532222222222222\",\"typeCode\":\"PUR\",\"catCode\":\"5411\"," +
                "\"source\":\"POS\",\"amount\":55.00,\"desc\":\"Coffee\",\"merchantId\":\"MERCH002\"," +
                "\"merchantName\":\"Starbucks\",\"merchantCity\":\"NY\",\"merchantZip\":\"10001\"}";

        mockMvc.perform(post("/api/transaction/add")
                .contentType("application/json")
                .content(addBody)
                .session(session))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.cardNum").value("4532222222222222"))
            .andExpect(jsonPath("$.amount").value("55.00"));
    }

    @Test
    void missingCardNum() throws Exception {
        CardDemoCommarea commarea = new CardDemoCommarea();
        commarea.userId = "USER0001";
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("commarea", commarea);

        String addBody = "{\"amount\":55.00}";

        mockMvc.perform(post("/api/transaction/add")
                .contentType("application/json")
                .content(addBody)
                .session(session))
            .andExpect(status().isBadRequest());
    }

    @Test
    void invalidAmount() throws Exception {
        CardDemoCommarea commarea = new CardDemoCommarea();
        commarea.userId = "USER0001";
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("commarea", commarea);

        String addBody = "{\"cardNum\":\"4532222222222222\",\"amount\":0}";

        mockMvc.perform(post("/api/transaction/add")
                .contentType("application/json")
                .content(addBody)
                .session(session))
            .andExpect(status().isBadRequest());
    }

    @Test
    void noSession() throws Exception {
        String addBody = "{\"cardNum\":\"4532222222222222\",\"amount\":55.00}";

        mockMvc.perform(post("/api/transaction/add")
                .contentType("application/json")
                .content(addBody))
            .andExpect(status().isForbidden());
    }
}
