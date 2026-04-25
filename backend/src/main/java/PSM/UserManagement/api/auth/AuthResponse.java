package PSM.UserManagement.api.auth;

public record AuthResponse(AuthenticatedUserDTO user, String token) {
}