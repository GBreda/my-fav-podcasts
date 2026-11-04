package app.myfavpodcasts.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.myfavpodcasts.identity.domain.DisplayName;
import app.myfavpodcasts.identity.domain.Email;
import app.myfavpodcasts.identity.domain.PasswordPolicy;
import app.myfavpodcasts.shared.domain.DomainException;
import org.junit.jupiter.api.Test;

class IdentityRulesTest {

    @Test
    void normalizesEmailAndDisplayName() {
        assertThat(new Email("  Ana@Example.COM ").value()).isEqualTo("ana@example.com");
        assertThat(new DisplayName("  Ana   Souza ").value()).isEqualTo("Ana Souza");
    }

    @Test
    void rejectsWeakIdentityData() {
        assertThatThrownBy(() -> new Email("ana")).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> new DisplayName("A")).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> PasswordPolicy.check("curta")).isInstanceOf(DomainException.class);
    }
}
