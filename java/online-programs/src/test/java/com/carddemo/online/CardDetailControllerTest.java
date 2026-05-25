package com.carddemo.online;

import com.carddemo.vsam.record.CardRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class CardDetailControllerTest {

    private static Path cardPath;

    static {
        try {
            String tmpDir = System.getProperty("java.io.tmpdir");
            cardPath = Paths.get(tmpDir, "carddat_" + System.nanoTime() + ".bin");

            CardRecord card = new CardRecord(
                "4532123456789012", // cardNum
                "12345678901",      // accountId
                "John Michael Doe", // embossedName
                "A",                // activeStatus
                "202501"            // expirationDate
            );

            Files.write(cardPath, card.encode());
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("carddemo.card.path", () -> cardPath.toString());
    }

    @Test
    void happyPath() throws Exception {
        CardDemoCommarea commarea = new CardDemoCommarea();
        commarea.userId = "USER0001";
        commarea.userType = "U";
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("commarea", commarea);

        mockMvc.perform(get("/api/card/4532123456789012")
                .session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.acctId").value("12345678901"))
            .andExpect(jsonPath("$.embossedName").value("John Michael Doe"))
            .andExpect(jsonPath("$.activeStatus").value("A"))
            .andExpect(jsonPath("$.expirationDate").value("202501"));
    }

    @Test
    void cardNotFound() throws Exception {
        CardDemoCommarea commarea = new CardDemoCommarea();
        commarea.userId = "USER0001";
        commarea.userType = "U";
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("commarea", commarea);

        mockMvc.perform(get("/api/card/9999999999999999")
                .session(session))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("Did not find cards for this search condition"));
    }

    @Test
    void invalidCardNum() throws Exception {
        CardDemoCommarea commarea = new CardDemoCommarea();
        commarea.userId = "USER0001";
        commarea.userType = "U";
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("commarea", commarea);

        mockMvc.perform(get("/api/card/abcd")
                .session(session))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Card number must be numeric"));
    }

    @Test
    void blankCardNum() throws Exception {
        CardDemoCommarea commarea = new CardDemoCommarea();
        commarea.userId = "USER0001";
        commarea.userType = "U";
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("commarea", commarea);

        mockMvc.perform(get("/api/card/ ")
                .session(session))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Card number is required"));
    }

    @Test
    void zeroCardNum() throws Exception {
        CardDemoCommarea commarea = new CardDemoCommarea();
        commarea.userId = "USER0001";
        commarea.userType = "U";
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("commarea", commarea);

        mockMvc.perform(get("/api/card/0000000000000000")
                .session(session))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Card number cannot be zero"));
    }

    @Test
    void noSession() throws Exception {
        mockMvc.perform(get("/api/card/4532123456789012"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("Not authenticated"));
    }
}
