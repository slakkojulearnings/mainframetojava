package com.carddemo.online.controller;

import com.carddemo.online.CardDemoCommarea;
import com.carddemo.online.dto.SignonRequest;
import com.carddemo.online.dto.SignonResponse;
import com.carddemo.vsam.FixedRecordReader;
import com.carddemo.vsam.record.UserSecurityRecord;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class SignonController {

    @Value("${carddemo.usrsec.path}")
    private String usrsecPath;

    @PostMapping("/signon")
    public ResponseEntity<?> signon(@RequestBody SignonRequest request, HttpSession session) {
        try {
            // Validate input
            String userId = (request.userId() != null) ? request.userId().trim().toUpperCase() : "";
            String password = (request.password() != null) ? request.password().trim().toUpperCase() : "";

            if (userId.isEmpty()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Please enter User ID ..."));
            }
            if (password.isEmpty()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Please enter Password ..."));
            }

            // Read USRSEC file by key
            Path usrsecFilePath = Paths.get(usrsecPath);
            try (FixedRecordReader reader = FixedRecordReader.open(usrsecFilePath, 80, FixedRecordReader.Mode.RAW)) {
                Optional<byte[]> raw = reader.readByKey(userId.getBytes());
                if (raw.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(new ErrorResponse("Invalid user ID ..."));
                }

                UserSecurityRecord user = UserSecurityRecord.decode(raw.get());

                // Verify password
                if (!password.equals(user.password().trim().toUpperCase())) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(new ErrorResponse("Wrong Password. Try again ..."));
                }

                // Build commarea
                CardDemoCommarea commarea = new CardDemoCommarea();
                commarea.fromTranId = "CC00";
                commarea.fromProgram = "COSGN00C";
                commarea.userId = userId;
                commarea.userType = user.userType();
                commarea.pgmContext = 0;
                commarea.custFname = user.firstName();
                commarea.custLname = user.lastName();

                // Store in session
                session.setAttribute("commarea", commarea);

                // Determine next program based on user type
                String nextProgram = "A".equals(user.userType()) ? "COADM01C" : "COMEN01C";

                return ResponseEntity.ok(new SignonResponse(
                    user.userId(),
                    user.userType(),
                    (user.firstName().trim() + " " + user.lastName().trim()).trim(),
                    nextProgram
                ));
            }

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("ERROR: " + e.getMessage()));
        }
    }

    static class ErrorResponse {
        private final String error;

        ErrorResponse(String error) {
            this.error = error;
        }

        public String error() { return error; }
    }
}
