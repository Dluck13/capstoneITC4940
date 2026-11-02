package com.itsupportoperationsdashboard.capstoneProject.auth;

import com.itsupportoperationsdashboard.capstoneProject.auth.AuthDtos.ErrorResponse;
import com.itsupportoperationsdashboard.capstoneProject.service.DuplicateEmailException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AuthController.class)
public class AuthExceptionHandler {
    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ErrorResponse> authenticationFailure() {
        return ResponseEntity.status(401).body(new ErrorResponse(401, "Invalid email or password"));
    }

    @ExceptionHandler(DuplicateEmailException.class)
    ResponseEntity<ErrorResponse> duplicateEmail(DuplicateEmailException exception) {
        return ResponseEntity.status(409).body(new ErrorResponse(409, exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ErrorResponse> invalidDomainRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new ErrorResponse(400, exception.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    ResponseEntity<ErrorResponse> invalidRequest(HttpServletRequest request) {
        String message = request.getRequestURI().endsWith("/login")
                ? "Invalid login request"
                : "Invalid account request";
        return ResponseEntity.badRequest().body(new ErrorResponse(400, message));
    }
}
