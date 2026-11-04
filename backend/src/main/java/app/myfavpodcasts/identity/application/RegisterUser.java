package app.myfavpodcasts.identity.application;

import app.myfavpodcasts.identity.domain.DisplayName;
import app.myfavpodcasts.identity.domain.Email;
import app.myfavpodcasts.identity.domain.PasswordHasher;
import app.myfavpodcasts.identity.domain.PasswordPolicy;
import app.myfavpodcasts.identity.domain.SessionTokens;
import app.myfavpodcasts.identity.domain.User;
import app.myfavpodcasts.identity.domain.UserRepository;
import app.myfavpodcasts.shared.domain.DomainException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class RegisterUser {

    private final UserRepository users;
    private final PasswordHasher passwords;
    private final SessionTokens tokens;

    public RegisterUser(UserRepository users, PasswordHasher passwords, SessionTokens tokens) {
        this.users = users;
        this.passwords = passwords;
        this.tokens = tokens;
    }

    public AuthResult execute(String email, String password, String displayName) {
        PasswordPolicy.check(password);
        Email parsed = new Email(email);
        DisplayName name = new DisplayName(displayName);
        if (users.findByEmail(parsed).isPresent()) {
            throw new DomainException("EMAIL_TAKEN");
        }
        User user = User.register(parsed, name, passwords.hash(password));
        try {
            user = users.save(user);
        } catch (DataIntegrityViolationException exception) {
            throw new DomainException("EMAIL_TAKEN");
        }
        return new AuthResult(tokens.issue(user), user);
    }
}
