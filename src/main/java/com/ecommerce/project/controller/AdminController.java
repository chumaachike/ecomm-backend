package com.ecommerce.project.controller;

import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.AppRole;
import com.ecommerce.project.model.Role;
import com.ecommerce.project.model.User;
import com.ecommerce.project.repositories.RoleRepository;
import com.ecommerce.project.repositories.UserRepository;
import com.ecommerce.project.security.response.MessageResponse;
import com.ecommerce.project.util.AuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("api/admin")
public class AdminController {
    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AuthUtil authUtil;

    @Autowired
    private UserRepository userRepository;

    private Role resolveRole(String role) {
        return switch (role.toLowerCase()) {
            case "admin" -> roleRepository.findByRoleName(AppRole.ROLE_ADMIN)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found"));
            case "seller" -> roleRepository.findByRoleName(AppRole.ROLE_SELLER)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found"));
            default -> throw new IllegalArgumentException("Invalid role: " + role);
        };
    }


    @PostMapping("/add-role")
    public ResponseEntity<?> addRoleToUser(@RequestParam(name = "userId") Long userId,
                                           @RequestParam(name = "role") String role) {
        // Find the user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));

        // Resolve the role
        Role roleToAdd;
        try {
            roleToAdd = resolveRole(role);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }

        // Check if the user already has the role
        if (user.getRoles().contains(roleToAdd)) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: User already has this role."));
        }

        // Add the role to the user
        user.getRoles().add(roleToAdd);
        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("Role added successfully to user."));
    }


    @PostMapping("/remove-role")
    public ResponseEntity<MessageResponse> removeRole(
            @RequestParam(name = "userId") Long userId, @RequestParam(name = "role") String role
    ){
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new ResourceNotFoundException("User", "userId", userId));

        Role roleTORemove;
        try {
            roleTORemove = resolveRole(role);
        }catch (IllegalArgumentException e){
            return ResponseEntity.badRequest().body(new MessageResponse("Error" + e.getMessage()));
        }

        if (!user.getRoles().contains(roleTORemove)) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: user does not have this role"));
        }

        user.getRoles().remove(roleTORemove);
        userRepository.save(user);

        return new ResponseEntity<>(new MessageResponse("Role has been successfully removed."), HttpStatus.OK);
    }


}
