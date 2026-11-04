package app.myfavpodcasts.shared.security;

import app.myfavpodcasts.shared.domain.DomainException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUsers {

    private CurrentUsers() {
    }

    public static AuthPrincipal require() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthPrincipal principal)) {
            throw new DomainException("UNAUTHENTICATED");
        }
        return principal;
    }
}
