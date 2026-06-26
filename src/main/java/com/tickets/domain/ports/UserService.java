package com.tickets.domain.ports;

import com.tickets.domain.model.User;
import com.tickets.infrastructure.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository repo;

    public UserService(UserRepository repo) {
        this.repo = repo;
    }

    public User register(User user) {
        return repo.save(user);
    }

    public boolean login(String username, String password) {
        Optional<User> user = repo.findByUsername(username);

        return user.isPresent() && user.get().getPassword().equals(password);
    }
}