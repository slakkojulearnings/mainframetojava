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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class TransactionViewControllerTest {

    private static Path tranPath;

    static {
        try {
            String tmpDir = System.getProperty("java.io.tmpdir");
            tranPath = Paths.get(tmpDir, "transact_" + System.nanoTime() + ".bin");

            TransactionRecord tran = new TransactionRecord(
                "0000000000000001",  // tranId
                "4532123456789012",  // cardNum
                "PUR",               // typeCode
                "5411",              // catCode
                "POS",               // source
                new BigDecimal("45.50"),  // amount
                "Gas Station",       // desc
                "20240615120000",    // origTs
                "20240615120030",    // procTs
                "MERCH001",          // merchantId
                "Shell Gas",         // merchantName
                "New York",          // merchantCity
                "10001"              // merchantZip
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
    void happyPath() throws Exception {
        CardDemoCommarea commarea = new CardDemoCommarea();
        commarea.userId = "USER0001";
        commarea.userType = "U";
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("commarea", commarea);

        mockMvc.perform(get("/api/transaction/0000000000000001")
                .session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tranId").value("0000000000000001"))
            .andExpect(jsonPath("$.cardNum").value("4532123456789012"))
            .andExpect(jsonPath("$.amount").value("45.50"))
            .andExpect(jsonPath("$.merchantName").value("Shell Gas"));
    }

    @Test
    void transactionNotFound() throws Exception {
        CardDemoCommarea commarea = new CardDemoCommarea();
        commarea.userId = "USER0001";
        commarea.userType = "U";
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("commarea", commarea);

        mockMvc.perform(get("/api/transaction/9999999999999999")
                .session(session))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("Transaction ID NOT found in Transaction master."));
    }

    @Test
    void blankTranId() throws Exception {
        CardDemoCommarea commarea = new CardDemoCommarea();
        commarea.userId = "USER0001";
        commarea.userType = "U";
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("commarea", commarea);

        mockMvc.perform(get("/api/transaction/ ")
                .session(session))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Tran ID can NOT be empty and must be 16 characters"));
    }

    @Test
    void invalidTranIdLength() throws Exception {
        CardDemoCommarea commarea = new CardDemoCommarea();
        commarea.userId = "USER0001";
        commarea.userType = "U";
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("commarea", commarea);

        mockMvc.perform(get("/api/transaction/123")
                .session(session))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Tran ID can NOT be empty and must be 16 characters"));
    }

    @Test
    void noSession() throws Exception {
        mockMvc.perform(get("/api/transaction/0000000000000001"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("Not authenticated"));
    }
}
