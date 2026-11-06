package app.myfavpodcasts.identity.application;

import app.myfavpodcasts.identity.domain.PasswordHasher;
import app.myfavpodcasts.identity.domain.PasswordPolicy;
import app.myfavpodcasts.identity.domain.PasswordResetRepository;
import app.myfavpodcasts.identity.domain.User;
import app.myfavpodcasts.identity.domain.UserRepository;
import app.myfavpodcasts.shared.domain.DomainException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChangePassword {

    private final UserRepository users;
    private final PasswordHasher passwords;
    private final PasswordResetRepository resets;

    public ChangePassword(UserRepository users, PasswordHasher passwords, PasswordResetRepository resets) {
        this.users = users;
        this.passwords = passwords;
        this.resets = resets;
    }

    @Transactional
    public void execute(UUID userId, String currentPassword, String nextPassword) {
        PasswordPolicy.check(nextPassword);
        User user = users.findById(userId).orElseThrow(() -> new DomainException("UNAUTHENTICATED"));
        if (currentPassword == null || !passwords.matches(currentPassword, user.passwordHash())) {
            throw new DomainException("INVALID_CREDENTIALS");
        }
        users.save(user.withPassword(passwords.hash(nextPassword)));
        resets.deleteByUserId(userId);
    }
}
