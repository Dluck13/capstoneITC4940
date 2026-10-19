package com.itsupportoperationsdashboard.capstoneProject.ticket;

import com.itsupportoperationsdashboard.capstoneProject.auth.AuthDtos.ErrorResponse;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice(assignableTypes = TicketController.class)
public class TicketExceptionHandler {
    @ExceptionHandler(EntityNotFoundException.class)
    ResponseEntity<ErrorResponse> notFound() {
        return ResponseEntity.status(404).body(new ErrorResponse(404, "Ticket not found"));
    }
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class, IllegalArgumentException.class})
    ResponseEntity<ErrorResponse> invalidRequest() {
        return ResponseEntity.badRequest().body(new ErrorResponse(400,
                "Invalid ticket request; check fields, active category, target technician, status transition, and comment text or closed status"));
    }
}
