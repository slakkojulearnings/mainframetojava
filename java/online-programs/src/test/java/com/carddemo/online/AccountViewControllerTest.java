package com.carddemo.online;

import com.carddemo.vsam.record.AccountRecord;
import com.carddemo.vsam.record.CardXrefRecord;
import com.carddemo.vsam.record.CustomerRecord;
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
public class AccountViewControllerTest {

    private static Path xrefPath;
    private static Path acctPath;
    private static Path custPath;

    static {
        try {
            String tmpDir = System.getProperty("java.io.tmpdir");
            xrefPath = Paths.get(tmpDir, "cardxref_" + System.nanoTime() + ".bin");
            acctPath = Paths.get(tmpDir, "acctdat_" + System.nanoTime() + ".bin");
            custPath = Paths.get(tmpDir, "custdat_" + System.nanoTime() + ".bin");

            // Create xref record: cardNum (16), custId (9), accountId (11), spare (14) = 50 bytes
            CardXrefRecord xref = new CardXrefRecord(
                "4532123456789012",  // cardNum
                "000000123",         // custId
                "12345678901"        // accountId
            );
            Files.write(xrefPath, xref.encode());

            // Create account record
            AccountRecord acct = new AccountRecord(
                "12345678901",      // accountId
                "A",                // activeStatus
                new BigDecimal("5000.00"),  // currBal
                new BigDecimal("10000.00"), // creditLimit
                new BigDecimal("2000.00"),  // cashCreditLimit
                new BigDecimal("1500.00"),  // currCycCredit
                new BigDecimal("800.00"),   // currCycDebit
                "20200101",         // openDate
                "20250131",         // expirationDate
                "20200101",         // reissueDate
                "GRP001"            // groupId
            );
            Files.write(acctPath, acct.encode());

            // Create customer record
            CustomerRecord cust = new CustomerRecord(
                "000000123",        // custId
                "123456789",        // ssn
                "750",              // ficoScore
                "19800515",         // dob
                "John",             // firstName
                "Michael",          // middleName
                "Doe",              // lastName
                "123 Main St",      // addrLine1
                "Apt 4B",           // addrLine2
                "",                 // addrLine3
                "NY",               // addrStateCode
                "10001",            // addrZip
                "US",               // addrCountryCode
                "2125551234",       // phone1
                "2125559876",       // phone2
                "DL",               // govtId
                "123456789",        // eftAccountId
                "Y"                 // priCardHolderInd
            );
            Files.write(custPath, cust.encode());
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("carddemo.xref.path", () -> xrefPath.toString());
        registry.add("carddemo.acct.path", () -> acctPath.toString());
        registry.add("carddemo.cust.path", () -> custPath.toString());
    }

    @Test
    void happyPath() throws Exception {
        CardDemoCommarea commarea = new CardDemoCommarea();
        commarea.userId = "USER0001";
        commarea.userType = "U";
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("commarea", commarea);

        mockMvc.perform(get("/api/account/12345678901")
                .session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accountId").value("12345678901"))
            .andExpect(jsonPath("$.custId").value("000000123"))
            .andExpect(jsonPath("$.firstName").value("John"))
            .andExpect(jsonPath("$.currBal").value("5000.00"))
            .andExpect(jsonPath("$.cardNum").value("4532123456789012"));
    }

    @Test
    void accountNotFoundInXref() throws Exception {
        CardDemoCommarea commarea = new CardDemoCommarea();
        commarea.userId = "USER0001";
        commarea.userType = "U";
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("commarea", commarea);

        mockMvc.perform(get("/api/account/99999999999")
                .session(session))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("Account: 99999999999 not found in Cross ref file."));
    }

    @Test
    void invalidAcctId() throws Exception {
        CardDemoCommarea commarea = new CardDemoCommarea();
        commarea.userId = "USER0001";
        commarea.userType = "U";
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("commarea", commarea);

        mockMvc.perform(get("/api/account/abc")
                .session(session))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Account Filter must be a non-zero 11 digit number"));
    }

    @Test
    void blankAcctId() throws Exception {
        CardDemoCommarea commarea = new CardDemoCommarea();
        commarea.userId = "USER0001";
        commarea.userType = "U";
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("commarea", commarea);

        mockMvc.perform(get("/api/account/ ")
                .session(session))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Account number not provided"));
    }

    @Test
    void noSession() throws Exception {
        mockMvc.perform(get("/api/account/12345678901"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("Not authenticated"));
    }
}
