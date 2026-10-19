package com.itsupportoperationsdashboard.capstoneProject.service;

import com.itsupportoperationsdashboard.capstoneProject.model.Category;
import com.itsupportoperationsdashboard.capstoneProject.model.Ticket;
import com.itsupportoperationsdashboard.capstoneProject.model.User;
import com.itsupportoperationsdashboard.capstoneProject.repository.CategoryRepository;
import com.itsupportoperationsdashboard.capstoneProject.repository.CommentRepository;
import com.itsupportoperationsdashboard.capstoneProject.repository.TicketRepository;
import com.itsupportoperationsdashboard.capstoneProject.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import com.itsupportoperationsdashboard.capstoneProject.security.TicketAccess;
import com.itsupportoperationsdashboard.capstoneProject.security.AppPrincipal;
import com.itsupportoperationsdashboard.capstoneProject.ticket.TicketDtos;
import com.itsupportoperationsdashboard.capstoneProject.ticket.TicketDtos.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final CommentRepository commentRepository;
    private final TicketAccess ticketAccess;

    public TicketService(
            TicketRepository ticketRepository,
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            CommentRepository commentRepository, TicketAccess ticketAccess) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.commentRepository = commentRepository;
        this.ticketAccess = ticketAccess;
    }

    @Transactional
    @PreAuthorize("@ticketAccess.canCreate(authentication)")
    public DetailResponse createForCurrentUser(CreateRequest request) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        User requester = userRepository.findById(ticketAccess.currentUserId(authentication))
                .filter(User::isActive)
                .orElseThrow(() -> new AccessDeniedException("Active requester required"));
        Category category = categoryRepository.findById(request.categoryId())
                .filter(Category::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Active category required"));
        Ticket ticket = new Ticket();
        ticket.setTitle(request.title().trim());
        ticket.setDescription(request.description());
        ticket.setPriority(request.priority());
        ticket.setRequester(requester);
        ticket.setCategory(category);
        ticket.setStatus("OPEN");
        ticket.setAssignedTo(null);
        return TicketDtos.detail(ticketRepository.save(ticket));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@ticketAccess.canCreate(authentication)")
    public List<ListResponse> listForCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        List<Ticket> tickets = ticketAccess.isSupport(authentication)
                ? getAllTickets()
                : getTicketsByRequesterId(ticketAccess.currentUserId(authentication));
        return tickets.stream().map(TicketDtos::summary).toList();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@ticketAccess.canCreate(authentication)")
    public DetailResponse detailForCurrentUser(Long ticketId) {
        Ticket ticket = getTicketById(ticketId);
        if (!ticketAccess.canView(SecurityContextHolder.getContext().getAuthentication(),
                ticket.getRequester().getUserId())) {
            throw new AccessDeniedException("Access denied");
        }
        return TicketDtos.detail(ticket);
    }

    @Transactional
    @PreAuthorize("@ticketAccess.canAssign(authentication, #technicianId)")
    public DetailResponse assignForCurrentUser(Long ticketId, Long technicianId) {
        if (technicianId == null || technicianId <= 0) {
            throw new IllegalArgumentException("A technician ID is required");
        }
        Ticket ticket = ticketRepository.findForUpdate(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found"));
        if (!List.of("OPEN", "ASSIGNED", "IN_PROGRESS").contains(ticket.getStatus())) {
            throw new IllegalArgumentException("Ticket cannot be assigned in its current status");
        }
        User technician = userRepository.findById(technicianId)
                .filter(User::isActive)
                .filter(user -> "TECHNICIAN".equals(AppPrincipal.canonicalRole(user.getRole())))
                .orElseThrow(() -> new IllegalArgumentException("An active technician is required"));
        ticket.setAssignedTo(technician);
        if ("OPEN".equals(ticket.getStatus())) {
            ticket.setStatus("ASSIGNED");
        }
        // Flush before mapping so the response includes the @PreUpdate timestamp.
        return TicketDtos.detail(ticketRepository.saveAndFlush(ticket));
    }

    @Transactional
    @PreAuthorize("@ticketAccess.canUpdateStatus(authentication)")
    public DetailResponse updateStatusForCurrentUser(Long ticketId, String status) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if ("CLOSED".equals(status) && !ticketAccess.canClose(authentication)) {
            throw new AccessDeniedException("Only administrators may close tickets");
        }
        Ticket ticket = ticketRepository.findForUpdate(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found"));
        // Assignment owns OPEN -> ASSIGNED. Resolution and closure need dedicated operations.
        if (!"ASSIGNED".equals(ticket.getStatus()) || !"IN_PROGRESS".equals(status)
                || ticket.getAssignedTo() == null) {
            throw new IllegalArgumentException("Only assigned tickets may transition to IN_PROGRESS");
        }
        ticket.setStatus("IN_PROGRESS");
        return TicketDtos.detail(ticketRepository.saveAndFlush(ticket));
    }

    @Transactional
    @PreAuthorize("@ticketAccess.canResolve(authentication)")
    public DetailResponse resolveForCurrentUser(Long ticketId, String resolutionNotes) {
        Ticket ticket = ticketRepository.findForUpdate(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found"));
        if (!"IN_PROGRESS".equals(ticket.getStatus())) {
            throw new IllegalArgumentException("Only IN_PROGRESS tickets can be resolved");
        }
        if (resolutionNotes == null || resolutionNotes.isBlank() || resolutionNotes.length() > 10000) {
            throw new IllegalArgumentException("Resolution notes must contain 1 to 10000 characters");
        }
        ticket.setResolutionNotes(resolutionNotes);
        ticket.setResolvedAt(java.time.LocalDateTime.now());
        ticket.setStatus("RESOLVED");
        return TicketDtos.detail(ticketRepository.saveAndFlush(ticket));
    }

    @Transactional
    @PreAuthorize("@ticketAccess.canClose(authentication)")
    public DetailResponse closeForCurrentUser(Long ticketId) {
        Ticket ticket = ticketRepository.findForUpdate(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found"));
        if (!"RESOLVED".equals(ticket.getStatus()) || ticket.getResolutionNotes() == null
                || ticket.getResolutionNotes().isBlank() || ticket.getResolvedAt() == null) {
            throw new IllegalArgumentException("Closure requires a resolved ticket with notes and timestamp");
        }
        ticket.setStatus("CLOSED");
        return TicketDtos.detail(ticketRepository.saveAndFlush(ticket));
    }

    public Ticket saveTicket(Ticket ticket) {
        requireInternalPersistenceCall();
        ticket.setTicketId(null);
        ticket.setRequester(findUser(ticket.getRequester(), "requester"));
        ticket.setAssignedTo(findOptionalUser(ticket.getAssignedTo(), "assigned user"));
        ticket.setCategory(findOptionalCategory(ticket.getCategory()));
        return ticketRepository.save(ticket);
    }

    public List<Ticket> getAllTickets() {
        return ticketRepository.findAll();
    }

    public Ticket getTicketById(Long ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found with id: " + ticketId));
    }

    public List<Ticket> getTicketsByStatus(String status) {
        return ticketRepository.findByStatus(status);
    }

    public List<Ticket> getTicketsByRequesterId(Long userId) {
        return ticketRepository.findByRequester_UserId(userId);
    }

    public List<Ticket> getTicketsByAssignedToId(Long userId) {
        return ticketRepository.findByAssignedTo_UserId(userId);
    }

    @Transactional
    public Ticket updateTicket(Long ticketId, Ticket ticketDetails) {
        requireInternalPersistenceCall();
        Ticket existingTicket = getTicketById(ticketId);

        existingTicket.setTitle(ticketDetails.getTitle());
        existingTicket.setDescription(ticketDetails.getDescription());
        existingTicket.setStatus(ticketDetails.getStatus());
        existingTicket.setPriority(ticketDetails.getPriority());
        existingTicket.setRequester(findUser(ticketDetails.getRequester(), "requester"));
        existingTicket.setAssignedTo(findOptionalUser(ticketDetails.getAssignedTo(), "assigned user"));
        existingTicket.setCategory(findOptionalCategory(ticketDetails.getCategory()));
        existingTicket.setResolvedAt(ticketDetails.getResolvedAt());
        existingTicket.setResolutionNotes(ticketDetails.getResolutionNotes());

        return ticketRepository.save(existingTicket);
    }

    @Transactional
    public void deleteTicket(Long ticketId) {
        requireInternalPersistenceCall();
        Ticket ticket = getTicketById(ticketId);
        if (!commentRepository.findByTicket_TicketId(ticketId).isEmpty()) {
            throw new IllegalStateException("Cannot delete ticket with existing comments.");
        }
        ticketRepository.delete(ticket);
    }

    // Retain the V1 persistence API for internal setup/maintenance. Session callers
    // must use the secured workflow methods instead of bypassing lifecycle checks.
    private void requireInternalPersistenceCall() {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            throw new AccessDeniedException("Use the secured ticket workflow operations");
        }
    }

    private User findUser(User user, String relationshipName) {
        if (user == null || user.getUserId() == null) {
            throw new IllegalArgumentException("Ticket " + relationshipName + " is required.");
        }

        return userRepository.findById(user.getUserId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "User not found with id: " + user.getUserId()));
    }

    private User findOptionalUser(User user, String relationshipName) {
        if (user == null) {
            return null;
        }
        if (user.getUserId() == null) {
            throw new IllegalArgumentException("Ticket " + relationshipName + " id is required.");
        }

        return userRepository.findById(user.getUserId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "User not found with id: " + user.getUserId()));
    }

    private Category findOptionalCategory(Category category) {
        if (category == null) {
            return null;
        }
        if (category.getCategoryId() == null) {
            throw new IllegalArgumentException("Ticket category id is required.");
        }

        return categoryRepository.findById(category.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Category not found with id: " + category.getCategoryId()));
    }
}
