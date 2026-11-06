package app.myfavpodcasts.identity.domain;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetRepository {

    Optional<PasswordReset> findByTokenHash(String tokenHash);

    PasswordReset save(PasswordReset reset);

    void deleteByUserId(UUID userId);
}
