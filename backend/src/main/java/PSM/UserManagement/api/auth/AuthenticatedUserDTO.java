package PSM.UserManagement.api.auth;

import java.util.UUID;

/**
 * DTO for authenticated user response, excludes lazy-loaded collections
 * to avoid Hibernate serialization issues
 */
public record AuthenticatedUserDTO(
    UUID id,
    String name,
    String email,
    float balance,
    boolean isAdmin
) {
    public AuthenticatedUserDTO(PSM.UserManagement.User user) {
        this(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getBalance(),
            user.isAdmin()
        );
    }
}
