package com.itsupportoperationsdashboard.capstoneProject.service;

import com.itsupportoperationsdashboard.capstoneProject.model.Category;
import com.itsupportoperationsdashboard.capstoneProject.model.Ticket;
import com.itsupportoperationsdashboard.capstoneProject.model.User;
import com.itsupportoperationsdashboard.capstoneProject.repository.CategoryRepository;
import com.itsupportoperationsdashboard.capstoneProject.repository.CommentRepository;
import com.itsupportoperationsdashboard.capstoneProject.repository.TicketRepository;
import com.itsupportoperationsdashboard.capstoneProject.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final CommentRepository commentRepository;

    public TicketService(
            TicketRepository ticketRepository,
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            CommentRepository commentRepository) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.commentRepository = commentRepository;
    }

    public Ticket saveTicket(Ticket ticket) {
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
        Ticket ticket = getTicketById(ticketId);
        if (!commentRepository.findByTicket_TicketId(ticketId).isEmpty()) {
            throw new IllegalStateException("Cannot delete ticket with existing comments.");
        }
        ticketRepository.delete(ticket);
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
