package com.tickets.entrypoints.controller;

import com.tickets.domain.ports.UserService;
import com.tickets.domain.model.User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @PostMapping("/register")
    public User register(@RequestBody User user) {
        return service.register(user);
    }

    @PostMapping("/login")
    public String login(@RequestBody User user) {
        return service.login(user.getUsername(), user.getPassword())
                ? "Login OK"
                : "Error";
    }
}