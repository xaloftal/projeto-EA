package PSM.UserManagement.api.auth;

public record AuthRequest(String name, String email, String password) {
}