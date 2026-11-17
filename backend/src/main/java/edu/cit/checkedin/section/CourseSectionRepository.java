package edu.cit.checkedin.section;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import edu.cit.checkedin.auth.User;

/**
 * Database queries for sections. Covers FR-010 – FR-013.
 */
public interface CourseSectionRepository extends JpaRepository<CourseSection, Long> {

    // A professor's active (or archived) sections, with their sessions loaded in the same query,
    // because the overlap check (FR-013) needs every session at once
    @EntityGraph(attributePaths = "sessions")
    List<CourseSection> findByProfessorAndArchived(User professor, boolean archived);
}
