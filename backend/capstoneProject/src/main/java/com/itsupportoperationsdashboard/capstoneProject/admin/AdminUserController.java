package com.itsupportoperationsdashboard.capstoneProject.admin;

import com.itsupportoperationsdashboard.capstoneProject.admin.AdminUserDtos.CreateRequest;
import com.itsupportoperationsdashboard.capstoneProject.auth.AuthDtos.UserResponse;
import com.itsupportoperationsdashboard.capstoneProject.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
    private final UserService users;

    public AdminUserController(UserService users) {
        this.users = users;
    }

    @PostMapping
    public UserResponse create(@Valid @RequestBody CreateRequest request) {
        var user = users.createSupportAccount(request.name(), request.email(), request.password(), request.role());
        return new UserResponse(user.getUserId(), user.getName(), user.getEmail(), user.getRole());
    }
}

