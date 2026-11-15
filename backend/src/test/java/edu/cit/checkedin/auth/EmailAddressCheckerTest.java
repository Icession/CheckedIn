package edu.cit.checkedin.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

/** Checks email clean-up and the @cit.edu domain rule (FR-002). */
class EmailAddressCheckerTest {

    private final EmailAddressChecker checker = new EmailAddressChecker(List.of("cit.edu"));

    @Test
    void normalizeTrimsSpacesAndMakesLowercase() {
        assertThat(checker.normalize("  Juan.DelaCruz@CIT.edu ")).isEqualTo("juan.delacruz@cit.edu");
    }

    @Test
    void allowsCitEduAddresses() {
        assertThat(checker.isAllowed("juan.delacruz@cit.edu")).isTrue();
    }

    @Test
    void rejectsOtherDomains() {
        assertThat(checker.isAllowed("juan@gmail.com")).isFalse();
    }

    @Test
    void rejectsLookAlikeDomainsThatOnlyEndInCitEdu() {
        assertThat(checker.isAllowed("juan@evil-cit.edu")).isFalse();
        assertThat(checker.isAllowed("juan@notcit.edu")).isFalse();
    }
}
