package edu.cit.checkedin.password;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** Database access for PasswordLockout rows (Increment 2 / SRS v1.4: password sign-in limit). */
public interface PasswordLockoutRepository extends JpaRepository<PasswordLockout, Long> {

    Optional<PasswordLockout> findByEmail(String email);
}
