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
public class UserMenuControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void userGetsMenu() throws Exception {
        // Create session with regular user commarea
        CardDemoCommarea commarea = new CardDemoCommarea();
        commarea.userId = "USER0001";
        commarea.userType = "U";
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("commarea", commarea);

        mockMvc.perform(get("/api/user/menu")
                .session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.options", hasSize(10)))
            .andExpect(jsonPath("$.options[0].number").value(1))
            .andExpect(jsonPath("$.options[0].name").value("Account View"))
            .andExpect(jsonPath("$.options[0].program").value("COACTVWC"))
            .andExpect(jsonPath("$.options[9].number").value(10))
            .andExpect(jsonPath("$.options[9].name").value("Bill Payment"))
            .andExpect(jsonPath("$.options[9].program").value("COBIL00C"));
    }

    @Test
    void adminAlsoGetsUserMenu() throws Exception {
        // Create session with admin commarea
        CardDemoCommarea commarea = new CardDemoCommarea();
        commarea.userId = "ADMIN001";
        commarea.userType = "A";
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("commarea", commarea);

        mockMvc.perform(get("/api/user/menu")
                .session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.options", hasSize(10)));
    }

    @Test
    void noSessionBlocked() throws Exception {
        mockMvc.perform(get("/api/user/menu"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("Not authenticated"));
    }
}
