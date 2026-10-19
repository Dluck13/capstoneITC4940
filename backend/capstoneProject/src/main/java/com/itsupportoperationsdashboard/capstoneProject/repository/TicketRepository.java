package com.itsupportoperationsdashboard.capstoneProject.repository;

import com.itsupportoperationsdashboard.capstoneProject.model.Ticket;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Ticket t where t.ticketId = :id")
    Optional<Ticket> findForUpdate(@Param("id") Long id);

    List<Ticket> findByStatus(String status);

    List<Ticket> findByRequester_UserId(Long userId);

    List<Ticket> findByAssignedTo_UserId(Long userId);
}
