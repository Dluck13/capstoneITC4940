package com.itsupportoperationsdashboard.capstoneProject.ticket;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.itsupportoperationsdashboard.capstoneProject.model.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public final class TicketDtos {
    private TicketDtos() {}

    // Extra client fields cannot set ownership, status, assignment, or persistence IDs.
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CreateRequest(
            @NotBlank @Size(max = 150) String title,
            @NotBlank @Size(max = 10000) String description,
            @NotBlank @Pattern(regexp = "LOW|MEDIUM|HIGH") String priority,
            @NotNull @Positive Long categoryId) {}
    public record Person(Long id, String name) {}
    public record AssignRequest(@NotNull @Positive Long technicianId) {}
    public record StatusRequest(@NotBlank @Size(max = 30) String status) {}
    public record ResolveRequest(@NotBlank @Size(max = 10000) String resolutionNotes) {}
    public record CategoryResponse(Long id, String name) {}
    public record ListResponse(Long id, String title, String priority, String status,
            Person requester, Person assignedTechnician, CategoryResponse category, LocalDateTime createdAt) {}
    public record DetailResponse(Long id, String title, String description, String priority, String status,
            Person requester, Person assignedTechnician, CategoryResponse category,
            LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime resolvedAt, String resolutionNotes) {}

    private static Person person(User user) {
        return user == null ? null : new Person(user.getUserId(), user.getName());
    }
    private static CategoryResponse category(Category category) {
        return category == null ? null : new CategoryResponse(category.getCategoryId(), category.getCategoryName());
    }
    public static ListResponse summary(Ticket ticket) {
        return new ListResponse(ticket.getTicketId(), ticket.getTitle(), ticket.getPriority(), ticket.getStatus(),
                person(ticket.getRequester()), person(ticket.getAssignedTo()), category(ticket.getCategory()),
                ticket.getCreatedAt());
    }
    public static DetailResponse detail(Ticket ticket) {
        return new DetailResponse(ticket.getTicketId(), ticket.getTitle(), ticket.getDescription(),
                ticket.getPriority(), ticket.getStatus(), person(ticket.getRequester()),
                person(ticket.getAssignedTo()), category(ticket.getCategory()), ticket.getCreatedAt(),
                ticket.getUpdatedAt(), ticket.getResolvedAt(), ticket.getResolutionNotes());
    }
}
