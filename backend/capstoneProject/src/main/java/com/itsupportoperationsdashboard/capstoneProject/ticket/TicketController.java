package com.itsupportoperationsdashboard.capstoneProject.ticket;

import com.itsupportoperationsdashboard.capstoneProject.service.TicketService;
import com.itsupportoperationsdashboard.capstoneProject.service.CommentService;
import com.itsupportoperationsdashboard.capstoneProject.ticket.TicketDtos.*;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {
    private final TicketService tickets;
    private final CommentService comments;
    public TicketController(TicketService tickets, CommentService comments) {
        this.tickets = tickets;
        this.comments = comments;
    }

    @GetMapping("/{id}/comments")
    public List<CommentDtos.Response> comments(@PathVariable Long id) {
        return comments.listForCurrentUser(id);
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<CommentDtos.Response> addComment(
            @PathVariable Long id, @Valid @RequestBody CommentDtos.CreateRequest request) {
        return ResponseEntity.status(201).body(comments.createForCurrentUser(id, request.text()));
    }

    @PostMapping
    public ResponseEntity<DetailResponse> create(@Valid @RequestBody CreateRequest request) {
        var result = tickets.createForCurrentUser(request);
        return ResponseEntity.created(URI.create("/api/tickets/" + result.id())).body(result);
    }
    @GetMapping
    public List<ListResponse> list() { return tickets.listForCurrentUser(); }

    @GetMapping("/{id}")
    public DetailResponse detail(@PathVariable Long id) { return tickets.detailForCurrentUser(id); }

    @PatchMapping("/{id}/assign")
    public DetailResponse assign(@PathVariable Long id, @Valid @RequestBody AssignRequest request) {
        return tickets.assignForCurrentUser(id, request.technicianId());
    }

    @PatchMapping("/{id}/status")
    public DetailResponse updateStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        return tickets.updateStatusForCurrentUser(id, request.status());
    }

    @PatchMapping("/{id}/resolve")
    public DetailResponse resolve(@PathVariable Long id, @Valid @RequestBody ResolveRequest request) {
        return tickets.resolveForCurrentUser(id, request.resolutionNotes());
    }

    @PatchMapping("/{id}/close")
    public DetailResponse close(@PathVariable Long id) {
        return tickets.closeForCurrentUser(id);
    }
}
