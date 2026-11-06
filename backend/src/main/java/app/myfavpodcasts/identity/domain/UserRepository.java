package app.myfavpodcasts.identity.domain;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    Optional<User> findByEmail(Email email);

    Optional<User> findById(UUID id);

    User save(User user);
}
