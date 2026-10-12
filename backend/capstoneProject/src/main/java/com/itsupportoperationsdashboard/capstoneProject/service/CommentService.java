package com.itsupportoperationsdashboard.capstoneProject.service;

import com.itsupportoperationsdashboard.capstoneProject.model.Comment;
import com.itsupportoperationsdashboard.capstoneProject.model.Ticket;
import com.itsupportoperationsdashboard.capstoneProject.model.User;
import com.itsupportoperationsdashboard.capstoneProject.repository.CommentRepository;
import com.itsupportoperationsdashboard.capstoneProject.repository.TicketRepository;
import com.itsupportoperationsdashboard.capstoneProject.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    public CommentService(
            CommentRepository commentRepository,
            TicketRepository ticketRepository,
            UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
    }

    public Comment saveComment(Comment comment) {
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
        Comment existingComment = getCommentById(commentId);

        existingComment.setCommentText(commentDetails.getCommentText());

        return commentRepository.save(existingComment);
    }

    @Transactional
    public void deleteComment(Long commentId) {
        Comment comment = getCommentById(commentId);
        commentRepository.delete(comment);
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
