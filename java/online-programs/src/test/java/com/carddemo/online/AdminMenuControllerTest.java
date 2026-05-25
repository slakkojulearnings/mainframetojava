package com.carddemo.online;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AdminMenuControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void adminGetsMenu() throws Exception {
        // Create session with admin commarea
        CardDemoCommarea commarea = new CardDemoCommarea();
        commarea.userId = "ADMIN001";
        commarea.userType = "A";
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("commarea", commarea);

        mockMvc.perform(get("/api/admin/menu")
                .session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.options", hasSize(4)))
            .andExpect(jsonPath("$.options[0].number").value(1))
            .andExpect(jsonPath("$.options[0].name").value("User List (Security)"))
            .andExpect(jsonPath("$.options[0].program").value("COUSR00C"))
            .andExpect(jsonPath("$.options[3].number").value(4))
            .andExpect(jsonPath("$.options[3].name").value("User Delete (Security)"))
            .andExpect(jsonPath("$.options[3].program").value("COUSR03C"));
    }

    @Test
    void userNotAuthorized() throws Exception {
        // Create session with regular user commarea
        CardDemoCommarea commarea = new CardDemoCommarea();
        commarea.userId = "USER0001";
        commarea.userType = "U";
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("commarea", commarea);

        mockMvc.perform(get("/api/admin/menu")
                .session(session))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("Not authorized"));
    }

    @Test
    void noSessionBlocked() throws Exception {
        mockMvc.perform(get("/api/admin/menu"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("Not authenticated"));
    }
}
