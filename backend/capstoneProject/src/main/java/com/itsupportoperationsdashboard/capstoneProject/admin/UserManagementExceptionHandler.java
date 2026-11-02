package com.itsupportoperationsdashboard.capstoneProject.admin;

import com.itsupportoperationsdashboard.capstoneProject.auth.AuthDtos.ErrorResponse;
import com.itsupportoperationsdashboard.capstoneProject.service.DuplicateEmailException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AdminUserController.class)
public class UserManagementExceptionHandler {
    @ExceptionHandler(DuplicateEmailException.class)
    ResponseEntity<ErrorResponse> duplicateEmail(DuplicateEmailException exception) {
        return ResponseEntity.status(409).body(new ErrorResponse(409, exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ErrorResponse> invalidDomainRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new ErrorResponse(400, exception.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    ResponseEntity<ErrorResponse> invalidBody() {
        return ResponseEntity.badRequest().body(new ErrorResponse(400, "Invalid account request"));
    }
}

