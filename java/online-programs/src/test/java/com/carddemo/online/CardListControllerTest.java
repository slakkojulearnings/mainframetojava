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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class CardListControllerTest {

    private static Path cardPath;

    static {
        try {
            String tmpDir = System.getProperty("java.io.tmpdir");
            cardPath = Paths.get(tmpDir, "carddat_list_" + System.nanoTime() + ".bin");

            CardRecord card1 = new CardRecord("4532111111111111", "12345678901", "John Doe", "A", "202501");
            CardRecord card2 = new CardRecord("4532222222222222", "12345678901", "Jane Doe", "A", "202501");
            CardRecord card3 = new CardRecord("4532333333333333", "98765432109", "Bob Smith", "I", "202412");

            byte[] data = new byte[card1.encode().length * 3];
            System.arraycopy(card1.encode(), 0, data, 0, card1.encode().length);
            System.arraycopy(card2.encode(), 0, data, card1.encode().length, card2.encode().length);
            System.arraycopy(card3.encode(), 0, data, card1.encode().length * 2, card3.encode().length);
            Files.write(cardPath, data);
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
    void listAllCards() throws Exception {
        CardDemoCommarea commarea = new CardDemoCommarea();
        commarea.userId = "USER0001";
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("commarea", commarea);

        mockMvc.perform(get("/api/cards")
                .session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cards", hasSize(3)))
            .andExpect(jsonPath("$.cards[0].cardNum").value("4532111111111111"));
    }

    @Test
    void filterByAccountId() throws Exception {
        CardDemoCommarea commarea = new CardDemoCommarea();
        commarea.userId = "USER0001";
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("commarea", commarea);

        mockMvc.perform(get("/api/cards?accountId=12345678901")
                .session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cards", hasSize(2)));
    }

    @Test
    void noSession() throws Exception {
        mockMvc.perform(get("/api/cards"))
            .andExpect(status().isForbidden());
    }
}
