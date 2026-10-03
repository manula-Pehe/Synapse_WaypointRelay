package com.synapse.waypoint.core.order.repository;

import java.time.LocalDate;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.synapse.waypoint.core.order.entity.OrderRun;
import com.synapse.waypoint.core.order.entity.OrderRunId;

public interface OrderRunRepository extends JpaRepository<OrderRun, OrderRunId> {

    /** Creates the run row if no one has yet; safe when two requests race. */
    @Modifying
    @Query(value = "INSERT INTO order_runs (run_date, depot) VALUES (:runDate, :depot) ON CONFLICT DO NOTHING",
            nativeQuery = true)
    void createIfAbsent(@Param("runDate") LocalDate runDate, @Param("depot") String depot);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from OrderRun r where r.id = :id")
    Optional<OrderRun> findForUpdate(@Param("id") OrderRunId id);

    @Query("""
            select r from OrderRun r
            where r.id.runDate = :runDate and lower(r.id.depot) = lower(:depot)""")
    Optional<OrderRun> findByRunDateAndDepot(@Param("runDate") LocalDate runDate, @Param("depot") String depot);
}
