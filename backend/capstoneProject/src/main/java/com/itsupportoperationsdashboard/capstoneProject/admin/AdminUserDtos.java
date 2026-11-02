package com.itsupportoperationsdashboard.capstoneProject.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AdminUserDtos {
    private AdminUserDtos() {}

    public record CreateRequest(@NotBlank @Size(max = 100) String name,
                                @NotBlank @Email @Size(max = 255) String email,
                                @NotBlank @Size(min = 8, max = 72) String password,
                                @NotBlank @Size(max = 20) String role) {
        @Override public String toString() { return "CreateRequest[credentials redacted]"; }
    }
}

