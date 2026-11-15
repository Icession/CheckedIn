package edu.cit.checkedin.auth;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Decides whether an email belongs to a professor or a student, from the professor list in the
 * settings (PROFESSOR_EMAILS in backend/.env). Covers FR-004 and FR-008.
 * The list is kept in memory, so asking is cheap: SecurityConfig asks on EVERY request, which means
 * someone taken off the list loses professor access right away, not only at their next sign-in.
 */
@Service
public class RoleService {

    private final Set<String> professorEmails;

    public RoleService(EmailAddressChecker emailAddressChecker,
                       @Value("${checkedin.auth.professor-emails}") List<String> professorEmails) {
        // Cleaned up like every other email (lowercase, no spaces); empty entries are ignored
        this.professorEmails = professorEmails.stream()
                .map(emailAddressChecker::normalize)
                .filter(email -> !email.isEmpty())
                .collect(Collectors.toSet());
    }

    /** Professor if the email is on the professor list, otherwise Student. */
    public Role roleFor(String verifiedEmail) {
        return professorEmails.contains(verifiedEmail) ? Role.PROFESSOR : Role.STUDENT;
    }
}
