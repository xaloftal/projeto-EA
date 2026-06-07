package PSM.UserManagement.api.user;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a user with the given identifier cannot be found.
 * <p>
 * Annotated with {@code @ResponseStatus(HttpStatus.NOT_FOUND)} so that Spring MVC
 * automatically translates it into a 404 response instead of the default 500.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message);
    }
}
