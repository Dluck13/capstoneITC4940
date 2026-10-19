package com.itsupportoperationsdashboard.capstoneProject.repository;

import com.itsupportoperationsdashboard.capstoneProject.model.Comment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByTicket_TicketId(Long ticketId);

    List<Comment> findByTicket_TicketIdOrderByCreatedAtAscCommentIdAsc(Long ticketId);
}
