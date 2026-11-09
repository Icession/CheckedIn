package edu.cit.checkedin.auth;

/**
 * The two kinds of users in CheckedIn.
 * Covers FR-004 (Professor role for emails on the professor list, Student role for everyone else)
 * and FR-008 (each role sees only its own pages).
 */
public enum Role {
    PROFESSOR,
    STUDENT
}
