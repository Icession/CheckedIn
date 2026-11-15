package edu.cit.checkedin.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

/** Checks how the professor list from the settings decides the role (FR-004). */
class RoleServiceTest {

    private final EmailAddressChecker emailAddressChecker = new EmailAddressChecker(List.of("cit.edu"));

    @Test
    void anEmailOnTheListIsAProfessorEveryoneElseIsAStudent() {
        RoleService roleService = new RoleService(emailAddressChecker, List.of("maria.santos@cit.edu"));

        assertThat(roleService.roleFor("maria.santos@cit.edu")).isEqualTo(Role.PROFESSOR);
        assertThat(roleService.roleFor("juan.delacruz@cit.edu")).isEqualTo(Role.STUDENT);
    }

    @Test
    void capitalsSpacesAndEmptyEntriesInTheListDoNotMatter() {
        // Written messily on purpose, like a hand-edited .env line
        RoleService roleService = new RoleService(emailAddressChecker, List.of("  Maria.Santos@CIT.edu ", ""));

        assertThat(roleService.roleFor("maria.santos@cit.edu")).isEqualTo(Role.PROFESSOR);
        assertThat(roleService.roleFor("")).isEqualTo(Role.STUDENT);
    }

    @Test
    void anEmptyListMakesEveryoneAStudent() {
        RoleService roleService = new RoleService(emailAddressChecker, List.of());

        assertThat(roleService.roleFor("maria.santos@cit.edu")).isEqualTo(Role.STUDENT);
    }
}
