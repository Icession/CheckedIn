package edu.cit.checkedin.section;

import java.time.DayOfWeek;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.cit.checkedin.auth.User;

/**
 * Database queries for weekly sessions. Covers FR-016 (today's sessions on the dashboard).
 */
public interface ClassSessionRepository extends JpaRepository<ClassSession, Long> {

    // FR-016: the professor's sessions on one weekday, in active sections only, earliest first
    @Query("""
            SELECT session FROM ClassSession session JOIN FETCH session.section section
            WHERE section.professor = :professor
              AND section.archived = false
              AND session.dayOfWeek = :day
            ORDER BY session.startTime""")
    List<ClassSession> findActiveSessionsOn(@Param("professor") User professor, @Param("day") DayOfWeek day);
}
