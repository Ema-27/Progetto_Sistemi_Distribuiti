package com.example.progetto_sistemidistribuiti.controllers;

import com.example.progetto_sistemidistribuiti.dto.UserProfileDto;
import com.example.progetto_sistemidistribuiti.model.UserProfile;
import com.example.progetto_sistemidistribuiti.service.UserService;
import com.example.progetto_sistemidistribuiti.support.InvalidCredentialsException;
import com.example.progetto_sistemidistribuiti.support.MailUserAlreadyExistsException;
import com.example.progetto_sistemidistribuiti.support.UserNotFoundException;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

//@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> create(@Valid @RequestBody UserProfileDto user) {
        userService.createUser(user);
        Map<String, String> response = new HashMap<>();
        response.put("message", "User created");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }


    @GetMapping
    public List<UserProfile> all() {
        return userService.getAllUsers();
    }

    @GetMapping("/get/{id}")
    @RolesAllowed({"admin", "user"})
    public ResponseEntity<UserProfile> get(@PathVariable String id) {
        try {
            UserProfile user = userService.getUserById(id);
            return new ResponseEntity<>(user, HttpStatus.OK);
        } catch (UserNotFoundException e) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/delete/{id}")
    @RolesAllowed("admin")
    public ResponseEntity<?> delete(@PathVariable String id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody UserProfileDto loginDto) {
        try {
            String token = userService.loginUser(loginDto.getUsername(), loginDto.getTemporaryPassword());
            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            return ResponseEntity.ok(response);
        }catch (InvalidCredentialsException e) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
    }

}
