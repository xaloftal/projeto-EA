package PSM.UserManagement.api.auth;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody AuthRequest request, HttpServletRequest httpRequest) {
        return authService.register(request, httpRequest);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest request, HttpServletRequest httpRequest) {
        return authService.login(request, httpRequest);
    }

    @GetMapping("/me")
    public AuthenticatedUserDTO me(HttpServletRequest httpRequest) {
        return new AuthenticatedUserDTO(authService.currentUser(httpRequest));
    }

    @PostMapping("/logout")
    public void logout(HttpServletRequest httpRequest) {
        authService.logout(httpRequest);
    }
}