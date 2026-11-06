package app.myfavpodcasts.identity.infrastructure;

import app.myfavpodcasts.identity.domain.SessionTokens;
import app.myfavpodcasts.identity.domain.User;
import app.myfavpodcasts.shared.security.JwtService;
import org.springframework.stereotype.Component;

@Component
public class JwtSessionTokens implements SessionTokens {

    private final JwtService tokens;

    public JwtSessionTokens(JwtService tokens) {
        this.tokens = tokens;
    }

    @Override
    public String issue(User user) {
        return tokens.issue(user.id(), user.email().value(), user.displayName().value());
    }
}
