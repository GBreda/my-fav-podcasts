package app.myfavpodcasts.identity.infrastructure;

import app.myfavpodcasts.identity.domain.PasswordReset;
import app.myfavpodcasts.identity.domain.PasswordResetRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class PasswordResetRepositoryAdapter implements PasswordResetRepository {

    private final PasswordResetJpaRepository resets;

    public PasswordResetRepositoryAdapter(PasswordResetJpaRepository resets) {
        this.resets = resets;
    }

    @Override
    public Optional<PasswordReset> findByTokenHash(String tokenHash) {
        return resets.findByTokenHash(tokenHash).map(PasswordResetEntity::toDomain);
    }

    @Override
    public PasswordReset save(PasswordReset reset) {
        PasswordResetEntity entity = resets.findById(reset.id()).orElseGet(() -> PasswordResetEntity.from(reset));
        entity.apply(reset);
        return resets.save(entity).toDomain();
    }

    @Override
    public void deleteByUserId(UUID userId) {
        resets.deleteByUserId(userId);
    }
}
