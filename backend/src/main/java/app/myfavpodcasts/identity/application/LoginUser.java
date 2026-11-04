package app.myfavpodcasts.identity.application;

import app.myfavpodcasts.identity.domain.Email;
import app.myfavpodcasts.identity.domain.PasswordHasher;
import app.myfavpodcasts.identity.domain.SessionTokens;
import app.myfavpodcasts.identity.domain.User;
import app.myfavpodcasts.identity.domain.UserRepository;
import app.myfavpodcasts.shared.domain.DomainException;
import org.springframework.stereotype.Service;

@Service
public class LoginUser {

    private final UserRepository users;
    private final PasswordHasher passwords;
    private final SessionTokens tokens;

    public LoginUser(UserRepository users, PasswordHasher passwords, SessionTokens tokens) {
        this.users = users;
        this.passwords = passwords;
        this.tokens = tokens;
    }

    public AuthResult execute(String email, String password) {
        Email parsed;
        try {
            parsed = new Email(email);
        } catch (DomainException exception) {
            throw new DomainException("INVALID_CREDENTIALS");
        }
        User user = users.findByEmail(parsed).orElseThrow(() -> new DomainException("INVALID_CREDENTIALS"));
        if (password == null || !passwords.matches(password, user.passwordHash())) {
            throw new DomainException("INVALID_CREDENTIALS");
        }
        return new AuthResult(tokens.issue(user), user);
    }
}
