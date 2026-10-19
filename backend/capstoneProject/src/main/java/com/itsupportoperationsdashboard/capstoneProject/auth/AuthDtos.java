package com.itsupportoperationsdashboard.capstoneProject.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {}
    public record LoginRequest(@NotBlank @Email @Size(max = 255) String email,
                               @NotBlank @Size(max = 72) String password) {
        @Override public String toString() { return "LoginRequest[credentials redacted]"; }
    }
    public record UserResponse(Long id, String name, String email, String role) implements java.io.Serializable {}
    public record CsrfResponse(String headerName, String token) {}
    public record ErrorResponse(int status, String message) {}
}
