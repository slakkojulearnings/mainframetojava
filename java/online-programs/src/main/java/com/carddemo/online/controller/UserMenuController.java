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
@RequestMapping("/api/user")
public class UserMenuController {

    @GetMapping("/menu")
    public ResponseEntity<?> getUserMenu(HttpSession session) {
        CardDemoCommarea commarea = (CardDemoCommarea) session.getAttribute("commarea");

        if (commarea == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Not authenticated"));
        }

        List<MenuOption> options = new ArrayList<>();
        options.add(new MenuOption(1, "Account View", "COACTVWC"));
        options.add(new MenuOption(2, "Account Update", "COACTUPC"));
        options.add(new MenuOption(3, "Credit Card List", "COCRDLIC"));
        options.add(new MenuOption(4, "Credit Card View", "COCRDSLC"));
        options.add(new MenuOption(5, "Credit Card Update", "COCRDUPC"));
        options.add(new MenuOption(6, "Transaction List", "COTRN00C"));
        options.add(new MenuOption(7, "Transaction View", "COTRN01C"));
        options.add(new MenuOption(8, "Transaction Add", "COTRN02C"));
        options.add(new MenuOption(9, "Transaction Reports", "CORPT00C"));
        options.add(new MenuOption(10, "Bill Payment", "COBIL00C"));

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
