package edu.cit.checkedin.password;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

/** Tests the password sign-in limit (Increment 2 / SRS v1.4): 5 wrong passwords -> blocked for 15 minutes. */
class PasswordLockoutTest {

    private static final PasswordSettings SETTINGS = new PasswordSettings(5, 15);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 25, 21, 0);

    @Test
    void fourWrongPasswordsDoNotBlock() {
        PasswordLockout lockout = afterWrongTries(4, 1);

        assertThat(lockout.isLocked(NOW.plusMinutes(4))).isFalse();
        assertThat(lockout.getWrongTries()).isEqualTo(4);
    }

    @Test
    void theFifthWrongPasswordBlocksForFifteenMinutes() {
        PasswordLockout lockout = afterWrongTries(5, 1); // the 5th try is at NOW + 4 minutes

        assertThat(lockout.isLocked(NOW.plusMinutes(4))).isTrue();
        assertThat(lockout.isLocked(NOW.plusMinutes(18).plusSeconds(59))).isTrue();
        assertThat(lockout.isLocked(NOW.plusMinutes(19))).isFalse(); // 15 minutes after the 5th try
    }

    @Test
    void afterTheLockoutTheCountStartsAgain() {
        PasswordLockout lockout = afterWrongTries(5, 1);
        lockout.recordWrongTry(NOW.plusMinutes(20), SETTINGS);

        assertThat(lockout.isLocked(NOW.plusMinutes(20))).isFalse();
        assertThat(lockout.getWrongTries()).isEqualTo(1);
    }

    @Test
    void wrongTriesOlderThanFifteenMinutesNoLongerCount() {
        PasswordLockout lockout = afterWrongTries(4, 1); // last one at NOW + 3 minutes
        lockout.recordWrongTry(NOW.plusMinutes(30), SETTINGS);

        assertThat(lockout.isLocked(NOW.plusMinutes(30))).isFalse();
        assertThat(lockout.getWrongTries()).isEqualTo(1);
    }

    // A lockout after the given number of wrong tries, one every minutesApart minutes starting at NOW
    private static PasswordLockout afterWrongTries(int tries, int minutesApart) {
        PasswordLockout lockout = new PasswordLockout("juan.delacruz@cit.edu");
        for (int i = 0; i < tries; i++) {
            lockout.recordWrongTry(NOW.plusMinutes((long) i * minutesApart), SETTINGS);
        }
        return lockout;
    }
}
