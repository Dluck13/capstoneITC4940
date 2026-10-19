package com.itsupportoperationsdashboard.capstoneProject.service;

import com.itsupportoperationsdashboard.capstoneProject.model.Comment;
import com.itsupportoperationsdashboard.capstoneProject.model.Ticket;
import com.itsupportoperationsdashboard.capstoneProject.model.User;
import com.itsupportoperationsdashboard.capstoneProject.repository.CommentRepository;
import com.itsupportoperationsdashboard.capstoneProject.repository.TicketRepository;
import com.itsupportoperationsdashboard.capstoneProject.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import com.itsupportoperationsdashboard.capstoneProject.security.TicketAccess;
import com.itsupportoperationsdashboard.capstoneProject.ticket.CommentDtos;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TicketAccess ticketAccess;

    public CommentService(
            CommentRepository commentRepository,
            TicketRepository ticketRepository,
            UserRepository userRepository, TicketAccess ticketAccess) {
        this.commentRepository = commentRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.ticketAccess = ticketAccess;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@ticketAccess.canCreate(authentication)")
    public List<CommentDtos.Response> listForCurrentUser(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found"));
        if (!ticketAccess.canView(SecurityContextHolder.getContext().getAuthentication(),
                ticket.getRequester().getUserId())) {
            throw new AccessDeniedException("Access denied");
        }
        return commentRepository.findByTicket_TicketIdOrderByCreatedAtAscCommentIdAsc(ticketId)
                .stream().map(CommentDtos::response).toList();
    }

    @Transactional
    @PreAuthorize("@ticketAccess.canCreate(authentication)")
    public CommentDtos.Response createForCurrentUser(Long ticketId, String text) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        // Share the workflow lock so a future closure cannot race with comment creation.
        Ticket ticket = ticketRepository.findForUpdate(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found"));
        if (!ticketAccess.canComment(authentication, ticket.getRequester().getUserId())) {
            throw new AccessDeniedException("Access denied");
        }
        if ("CLOSED".equals(ticket.getStatus())) {
            throw new IllegalArgumentException("Cannot comment on a closed ticket");
        }
        if (text == null || text.isBlank() || text.length() > 10000) {
            throw new IllegalArgumentException("Comment text must contain 1 to 10000 characters");
        }
        User author = userRepository.findById(ticketAccess.currentUserId(authentication))
                .filter(User::isActive)
                .orElseThrow(() -> new AccessDeniedException("Active author required"));
        Comment comment = new Comment();
        comment.setTicket(ticket);
        comment.setUser(author);
        comment.setCommentText(text);
        return CommentDtos.response(commentRepository.save(comment));
    }

    public Comment saveComment(Comment comment) {
        requireInternalPersistenceCall();
        comment.setCommentId(null);
        comment.setTicket(findTicket(comment.getTicket()));
        comment.setUser(findUser(comment.getUser()));
        return commentRepository.save(comment);
    }

    public List<Comment> getAllComments() {
        return commentRepository.findAll();
    }

    public Comment getCommentById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Comment not found with id: " + commentId));
    }

    public List<Comment> getCommentsByTicketId(Long ticketId) {
        return commentRepository.findByTicket_TicketId(ticketId);
    }

    @Transactional
    public Comment updateComment(Long commentId, Comment commentDetails) {
        requireInternalPersistenceCall();
        Comment existingComment = getCommentById(commentId);

        existingComment.setCommentText(commentDetails.getCommentText());

        return commentRepository.save(existingComment);
    }

    @Transactional
    public void deleteComment(Long commentId) {
        requireInternalPersistenceCall();
        Comment comment = getCommentById(commentId);
        commentRepository.delete(comment);
    }

    private void requireInternalPersistenceCall() {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            throw new AccessDeniedException("Use the secured comment operations");
        }
    }

    private Ticket findTicket(Ticket ticket) {
        if (ticket == null || ticket.getTicketId() == null) {
            throw new IllegalArgumentException("Comment ticket is required.");
        }

        return ticketRepository.findById(ticket.getTicketId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Ticket not found with id: " + ticket.getTicketId()));
    }

    private User findUser(User user) {
        if (user == null || user.getUserId() == null) {
            throw new IllegalArgumentException("Comment user is required.");
        }

        return userRepository.findById(user.getUserId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "User not found with id: " + user.getUserId()));
    }
}
