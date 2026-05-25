package com.carddemo.online;

import com.carddemo.vsam.record.AccountRecord;
import com.carddemo.vsam.record.CustomerRecord;
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
public class ReportsControllerTest {

    private static Path acctPath;
    private static Path custPath;
    private static Path tranPath;

    static {
        try {
            String tmpDir = System.getProperty("java.io.tmpdir");
            acctPath = Paths.get(tmpDir, "acctdat_rpt_" + System.nanoTime() + ".bin");
            custPath = Paths.get(tmpDir, "custdat_rpt_" + System.nanoTime() + ".bin");
            tranPath = Paths.get(tmpDir, "transact_rpt_" + System.nanoTime() + ".bin");

            AccountRecord acct = new AccountRecord(
                "12345678901", "A", new BigDecimal("5000.00"), new BigDecimal("10000.00"),
                new BigDecimal("2000.00"), new BigDecimal("1500.00"), new BigDecimal("800.00"),
                "20200101", "20250131", "20200101", "GRP001"
            );
            Files.write(acctPath, acct.encode());

            CustomerRecord cust = new CustomerRecord(
                "000000001", "123456789", "750", "19800515", "John", "Michael", "Doe",
                "123 Main St", "Apt 4B", "", "NY", "10001", "US", "2125551234",
                "2125559876", "DL", "123456789", "Y"
            );
            Files.write(custPath, cust.encode());

            TransactionRecord tran = new TransactionRecord(
                "0000000000000001", "4532123456789012", "PUR", "5411", "POS",
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
        registry.add("carddemo.acct.path", () -> acctPath.toString());
        registry.add("carddemo.cust.path", () -> custPath.toString());
        registry.add("carddemo.tran.path", () -> tranPath.toString());
    }

    @Test
    void getAccountStatement() throws Exception {
        CardDemoCommarea commarea = new CardDemoCommarea();
        commarea.userId = "USER0001";
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("commarea", commarea);

        mockMvc.perform(get("/api/report/account-statement/12345678901")
                .session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accountId").value("12345678901"))
            .andExpect(jsonPath("$.currentBalance").value("5000.00"));
    }

    @Test
    void accountNotFound() throws Exception {
        CardDemoCommarea commarea = new CardDemoCommarea();
        commarea.userId = "USER0001";
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("commarea", commarea);

        mockMvc.perform(get("/api/report/account-statement/99999999999")
                .session(session))
            .andExpect(status().isNotFound());
    }

    @Test
    void noSession() throws Exception {
        mockMvc.perform(get("/api/report/account-statement/12345678901"))
            .andExpect(status().isForbidden());
    }
}
