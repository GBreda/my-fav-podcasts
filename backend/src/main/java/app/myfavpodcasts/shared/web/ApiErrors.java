package app.myfavpodcasts.shared.web;

import app.myfavpodcasts.shared.domain.DomainException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiErrors {

    @ExceptionHandler(DomainException.class)
    ResponseEntity<Map<String, String>> domain(DomainException exception) {
        HttpStatus status = switch (exception.code()) {
            case "EMAIL_TAKEN", "ALREADY_FOLLOWING" -> HttpStatus.CONFLICT;
            case "INVALID_CREDENTIALS", "UNAUTHENTICATED" -> HttpStatus.UNAUTHORIZED;
            case "PODCAST_NOT_FOUND", "EPISODE_NOT_FOUND", "NOT_FOLLOWING" -> HttpStatus.NOT_FOUND;
            case "FEED_UNREADABLE" -> HttpStatus.UNPROCESSABLE_CONTENT;
            case "FEED_URL_BLOCKED", "VALIDATION", "RESET_TOKEN_INVALID" -> HttpStatus.BAD_REQUEST;
            case "DIRECTORY_UNAVAILABLE" -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(Map.of("code", exception.code()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> invalid(MethodArgumentNotValidException exception) {
        return ResponseEntity.badRequest().body(Map.of("code", "VALIDATION"));
    }
}
