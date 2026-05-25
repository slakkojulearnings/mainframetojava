package com.carddemo.online.controller;

import com.carddemo.online.CardDemoCommarea;
import com.carddemo.online.dto.MenuOption;
import com.carddemo.online.dto.MenuResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminMenuController {

    @GetMapping("/menu")
    public ResponseEntity<?> getAdminMenu(HttpSession session) {
        CardDemoCommarea commarea = (CardDemoCommarea) session.getAttribute("commarea");

        if (commarea == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Not authenticated"));
        }

        if (!"A".equals(commarea.userType)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Not authorized"));
        }

        List<MenuOption> options = new ArrayList<>();
        options.add(new MenuOption(1, "User List (Security)", "COUSR00C"));
        options.add(new MenuOption(2, "User Add (Security)", "COUSR01C"));
        options.add(new MenuOption(3, "User Update (Security)", "COUSR02C"));
        options.add(new MenuOption(4, "User Delete (Security)", "COUSR03C"));

        return ResponseEntity.ok(new MenuResponse(options));
    }

    static class ErrorResponse {
        private final String error;

        ErrorResponse(String error) {
            this.error = error;
        }

        public String error() { return error; }
    }
}
