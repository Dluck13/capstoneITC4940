package com.itsupportoperationsdashboard.capstoneProject.auth;

import com.itsupportoperationsdashboard.capstoneProject.auth.AuthDtos.ErrorResponse;
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

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    ResponseEntity<ErrorResponse> invalidRequest() {
        return ResponseEntity.badRequest().body(new ErrorResponse(400, "Invalid login request"));
    }
}
