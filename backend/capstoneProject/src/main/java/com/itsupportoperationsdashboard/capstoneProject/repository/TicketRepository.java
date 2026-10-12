package com.itsupportoperationsdashboard.capstoneProject.repository;

import com.itsupportoperationsdashboard.capstoneProject.model.Ticket;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByStatus(String status);

    List<Ticket> findByRequester_UserId(Long userId);

    List<Ticket> findByAssignedTo_UserId(Long userId);
}
