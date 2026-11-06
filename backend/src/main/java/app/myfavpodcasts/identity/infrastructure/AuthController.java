package app.myfavpodcasts.identity.infrastructure;

import app.myfavpodcasts.identity.application.AuthResult;
import app.myfavpodcasts.identity.application.ChangePassword;
import app.myfavpodcasts.identity.application.LoginUser;
import app.myfavpodcasts.identity.application.RegisterUser;
import app.myfavpodcasts.identity.domain.User;
import app.myfavpodcasts.shared.security.AuthPrincipal;
import app.myfavpodcasts.shared.security.CurrentUsers;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final RegisterUser registerUser;
    private final LoginUser loginUser;
    private final ChangePassword changePassword;

    public AuthController(RegisterUser registerUser, LoginUser loginUser, ChangePassword changePassword) {
        this.registerUser = registerUser;
        this.loginUser = loginUser;
        this.changePassword = changePassword;
    }

    @PostMapping("/api/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    AuthResponse register(@Valid @RequestBody RegisterBody body) {
        return AuthResponse.from(registerUser.execute(body.email(), body.password(), body.displayName()));
    }

    @PostMapping("/api/auth/login")
    AuthResponse login(@Valid @RequestBody LoginBody body) {
        return AuthResponse.from(loginUser.execute(body.email(), body.password()));
    }

    @GetMapping("/api/auth/me")
    UserResponse me() {
        AuthPrincipal principal = CurrentUsers.require();
        return new UserResponse(principal.id(), principal.email(), principal.displayName());
    }

    @PostMapping("/api/auth/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void password(@Valid @RequestBody PasswordBody body) {
        changePassword.execute(CurrentUsers.require().id(), body.currentPassword(), body.nextPassword());
    }

    public record RegisterBody(
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank @Size(min = 2, max = 40) String displayName
    ) {
    }

    public record LoginBody(
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(max = 72) String password
    ) {
    }

    public record PasswordBody(
            @NotBlank @Size(max = 72) String currentPassword,
            @NotBlank @Size(min = 8, max = 72) String nextPassword
    ) {
    }

    public record UserResponse(UUID id, String email, String displayName) {
    }

    public record AuthResponse(String token, UserResponse user) {
        static AuthResponse from(AuthResult result) {
            User user = result.user();
            return new AuthResponse(result.token(), new UserResponse(user.id(), user.email().value(), user.displayName().value()));
        }
    }
}
