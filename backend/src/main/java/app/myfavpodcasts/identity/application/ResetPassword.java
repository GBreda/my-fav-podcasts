package app.myfavpodcasts.identity.application;

import app.myfavpodcasts.identity.domain.PasswordHasher;
import app.myfavpodcasts.identity.domain.PasswordPolicy;
import app.myfavpodcasts.identity.domain.PasswordReset;
import app.myfavpodcasts.identity.domain.PasswordResetRepository;
import app.myfavpodcasts.identity.domain.ResetTokens;
import app.myfavpodcasts.identity.domain.User;
import app.myfavpodcasts.identity.domain.UserRepository;
import app.myfavpodcasts.shared.domain.DomainException;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResetPassword {

    private final UserRepository users;
    private final PasswordResetRepository resets;
    private final PasswordHasher passwords;

    public ResetPassword(UserRepository users, PasswordResetRepository resets, PasswordHasher passwords) {
        this.users = users;
        this.resets = resets;
        this.passwords = passwords;
    }

    @Transactional
    public void execute(String rawToken, String password) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new DomainException("RESET_TOKEN_INVALID");
        }
        PasswordReset reset = resets.findByTokenHash(ResetTokens.hash(rawToken.trim()))
                .orElseThrow(() -> new DomainException("RESET_TOKEN_INVALID"));
        Instant now = Instant.now();
        if (!reset.usable(now)) {
            throw new DomainException("RESET_TOKEN_INVALID");
        }
        PasswordPolicy.check(password);
        User user = users.findById(reset.userId()).orElseThrow(() -> new DomainException("RESET_TOKEN_INVALID"));
        users.save(user.withPassword(passwords.hash(password)));
        resets.save(reset.used(now));
    }
}
