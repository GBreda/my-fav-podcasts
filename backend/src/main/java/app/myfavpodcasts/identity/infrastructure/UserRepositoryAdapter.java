package app.myfavpodcasts.identity.infrastructure;

import app.myfavpodcasts.identity.domain.Email;
import app.myfavpodcasts.identity.domain.User;
import app.myfavpodcasts.identity.domain.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository users;

    public UserRepositoryAdapter(UserJpaRepository users) {
        this.users = users;
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return users.findByEmail(email.value()).map(UserEntity::toDomain);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return users.findById(id).map(UserEntity::toDomain);
    }

    @Override
    public User save(User user) {
        UserEntity entity = users.findById(user.id()).orElseGet(() -> UserEntity.from(user));
        entity.applyPassword(user.passwordHash());
        return users.save(entity).toDomain();
    }
}
