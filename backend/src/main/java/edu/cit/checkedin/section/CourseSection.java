package edu.cit.checkedin.section;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import edu.cit.checkedin.auth.User;

/**
 * A class a professor handles, e.g. "IT342 G01, First Semester 2026-2027", with its weekly sessions.
 * One row in the "course_sections" table (see the ERD). Covers FR-010 (create), FR-011 (edit and
 * archive: an archived section is read-only and hidden from the dashboard) and FR-012 (sessions).
 */
@Entity
@Table(name = "course_sections")
public class CourseSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "section_id")
    private Long sectionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "professor_id", nullable = false)
    private User professor;

    // Saved in capitals, e.g. "IT342"
    @Column(name = "subject_code", length = 15, nullable = false)
    private String subjectCode;

    @Column(name = "subject_title", length = 120, nullable = false)
    private String subjectTitle;

    // Saved in capitals, e.g. "G01"
    @Column(name = "section_code", length = 10, nullable = false)
    private String sectionCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "semester", length = 10, nullable = false)
    private Semester semester;

    // Two years in a row, e.g. "2026-2027"
    @Column(name = "school_year", columnDefinition = "char(9)", nullable = false)
    private String schoolYear;

    @Column(name = "is_archived", nullable = false)
    private boolean archived;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // The weekly sessions; removing one from this list deletes its row (orphanRemoval)
    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClassSession> sessions = new ArrayList<>();

    /** Empty constructor required by JPA. Our own code uses the constructor below. */
    protected CourseSection() {
    }

    public CourseSection(User professor, String subjectCode, String subjectTitle, String sectionCode,
                         Semester semester, String schoolYear) {
        this.professor = professor;
        this.subjectCode = subjectCode;
        this.subjectTitle = subjectTitle;
        this.sectionCode = sectionCode;
        this.semester = semester;
        this.schoolYear = schoolYear;
        this.archived = false;
    }

    /** Sets the creation time automatically right before the section is first saved. */
    @PrePersist
    private void recordCreationTime() {
        this.createdAt = LocalDateTime.now();
    }

    /** Adds a weekly session and links it back to this section. */
    public void addSession(ClassSession session) {
        session.setSection(this);
        sessions.add(session);
    }

    public void removeSession(ClassSession session) {
        sessions.remove(session);
    }

    /** FR-011: an archived section becomes read-only and leaves the dashboard. There is no un-archive. */
    public void archive() {
        this.archived = true;
    }

    /** True when both sections are in the same semester and school year (used by the FR-013 check). */
    public boolean isSameTermAs(Semester otherSemester, String otherSchoolYear) {
        return semester == otherSemester && schoolYear.equals(otherSchoolYear);
    }

    // ---------- Getters and setters (no setters for the ID, professor and creation time) ----------

    public Long getSectionId() {
        return sectionId;
    }

    public User getProfessor() {
        return professor;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public String getSubjectTitle() {
        return subjectTitle;
    }

    public void setSubjectTitle(String subjectTitle) {
        this.subjectTitle = subjectTitle;
    }

    public String getSectionCode() {
        return sectionCode;
    }

    public void setSectionCode(String sectionCode) {
        this.sectionCode = sectionCode;
    }

    public Semester getSemester() {
        return semester;
    }

    public void setSemester(Semester semester) {
        this.semester = semester;
    }

    public String getSchoolYear() {
        return schoolYear;
    }

    public void setSchoolYear(String schoolYear) {
        this.schoolYear = schoolYear;
    }

    public boolean isArchived() {
        return archived;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<ClassSession> getSessions() {
        return sessions;
    }
}
