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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class CardUpdateControllerTest {

    private static Path cardPath;

    static {
        try {
            String tmpDir = System.getProperty("java.io.tmpdir");
            cardPath = Paths.get(tmpDir, "carddat_upd_" + System.nanoTime() + ".bin");

            CardRecord card = new CardRecord(
                "4532123456789012",
                "12345678901",
                "John Michael Doe",
                "A",
                "202501"
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
    void updateCardSuccess() throws Exception {
        CardDemoCommarea commarea = new CardDemoCommarea();
        commarea.userId = "USER0001";
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("commarea", commarea);

        String updateBody = "{\"activeStatus\":\"I\"}";

        mockMvc.perform(post("/api/card/4532123456789012/update")
                .contentType("application/json")
                .content(updateBody)
                .session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cardNum").value("4532123456789012"))
            .andExpect(jsonPath("$.activeStatus").value("I"));
    }

    @Test
    void cardNotFound() throws Exception {
        CardDemoCommarea commarea = new CardDemoCommarea();
        commarea.userId = "USER0001";
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("commarea", commarea);

        String updateBody = "{\"activeStatus\":\"A\"}";

        mockMvc.perform(post("/api/card/9999999999999999/update")
                .contentType("application/json")
                .content(updateBody)
                .session(session))
            .andExpect(status().isNotFound());
    }

    @Test
    void invalidCardNum() throws Exception {
        CardDemoCommarea commarea = new CardDemoCommarea();
        commarea.userId = "USER0001";
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("commarea", commarea);

        String updateBody = "{\"activeStatus\":\"A\"}";

        mockMvc.perform(post("/api/card/abcd/update")
                .contentType("application/json")
                .content(updateBody)
                .session(session))
            .andExpect(status().isBadRequest());
    }

    @Test
    void noSession() throws Exception {
        String updateBody = "{\"activeStatus\":\"A\"}";

        mockMvc.perform(post("/api/card/4532123456789012/update")
                .contentType("application/json")
                .content(updateBody))
            .andExpect(status().isForbidden());
    }
}
