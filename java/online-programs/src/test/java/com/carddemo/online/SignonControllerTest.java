package com.carddemo.online;

import com.carddemo.online.controller.SignonController;
import com.carddemo.online.dto.SignonRequest;
import com.carddemo.vsam.record.UserSecurityRecord;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "carddemo.usrsec.path=${usrsec.path}"
})
public class SignonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @TempDir
    static Path tempDir;

    private static String usrsecPath;

    @BeforeEach
    void setUp() throws Exception {
        // Create test USRSEC file with two records
        Path usrsecFile = tempDir.resolve("USRSEC.bin");

        // Admin user: "ADMIN001", "ADMIN", "Doe", password "ADMIN01", type "A"
        UserSecurityRecord adminUser = new UserSecurityRecord(
            "ADMIN001", "Admin", "Doe", "ADMIN01", "A", new byte[80]
        );

        // Regular user: "USER0001", "John", "Smith", password "USER0001", type "U"
        UserSecurityRecord regularUser = new UserSecurityRecord(
            "USER0001", "John", "Smith", "USER0001", "U", new byte[80]
        );

        // Write both records
        byte[] data = new byte[160];
        System.arraycopy(adminUser.encode(), 0, data, 0, 80);
        System.arraycopy(regularUser.encode(), 0, data, 80, 80);
        Files.write(usrsecFile, data);

        usrsecPath = usrsecFile.toString();
        System.setProperty("usrsec.path", usrsecPath);
    }

    @Test
    void successfulAdminSignon() throws Exception {
        mockMvc.perform(post("/api/signon")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"userId":"ADMIN001","password":"ADMIN01"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value("ADMIN001"))
            .andExpect(jsonPath("$.userType").value("A"))
            .andExpect(jsonPath("$.nextProgram").value("COADM01C"))
            .andExpect(jsonPath("$.fullName").value("Admin Doe"));
    }

    @Test
    void successfulUserSignon() throws Exception {
        mockMvc.perform(post("/api/signon")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"userId":"USER0001","password":"USER0001"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value("USER0001"))
            .andExpect(jsonPath("$.userType").value("U"))
            .andExpect(jsonPath("$.nextProgram").value("COMEN01C"))
            .andExpect(jsonPath("$.fullName").value("John Smith"));
    }

    @Test
    void wrongPassword() throws Exception {
        mockMvc.perform(post("/api/signon")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"userId":"ADMIN001","password":"WRONGPWD"}
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("Wrong Password. Try again ..."));
    }

    @Test
    void unknownUser() throws Exception {
        mockMvc.perform(post("/api/signon")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"userId":"BADUSER","password":"BADPWD"}
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("Invalid user ID ..."));
    }

    @Test
    void blankUserId() throws Exception {
        mockMvc.perform(post("/api/signon")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"userId":"","password":"ADMIN01"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Please enter User ID ..."));
    }

    @Test
    void blankPassword() throws Exception {
        mockMvc.perform(post("/api/signon")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"userId":"ADMIN001","password":""}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Please enter Password ..."));
    }
}
