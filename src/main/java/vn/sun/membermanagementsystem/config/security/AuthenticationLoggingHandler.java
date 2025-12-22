package vn.sun.membermanagementsystem.config.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;
import vn.sun.membermanagementsystem.entities.ActivityLog;
import vn.sun.membermanagementsystem.entities.User;
import vn.sun.membermanagementsystem.repositories.ActivityLogRepository;
import vn.sun.membermanagementsystem.repositories.UserRepository;

import java.io.IOException;
import java.time.LocalDateTime;


@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationLoggingHandler implements AuthenticationSuccessHandler, LogoutSuccessHandler, AuthenticationFailureHandler {

    private final ActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        String email = authentication.getName();
        logAuthEvent("LOGIN", email, "Admin login successful", request);
        
        response.sendRedirect(request.getContextPath() + "/admin/users");
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response,
                                Authentication authentication) throws IOException, ServletException {
        if (authentication != null) {
            String email = authentication.getName();
            logAuthEvent("LOGOUT", email, "Admin logout successful", request);
        }
        
        response.sendRedirect(request.getContextPath() + "/admin/login?logout=true");
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        String email = request.getParameter("username");
        logAuthEvent("LOGIN_FAILED", email, "Login failed: " + exception.getMessage(), request);
        
        response.sendRedirect(request.getContextPath() + "/admin/login?error=true");
    }

    public void logApiLogin(String email, HttpServletRequest request) {
        logAuthEvent("API_LOGIN", email, "User API login successful", request);
    }

    public void logApiLogout(String email, HttpServletRequest request) {
        logAuthEvent("API_LOGOUT", email, "User API logout", request);
    }

    private void logAuthEvent(String action, String email, String description, HttpServletRequest request) {
        try {
            Long userId = null;
            if (email != null) {
                userId = userRepository.findByEmail(email)
                        .map(User::getId)
                        .orElse(null);
            }

            ActivityLog activityLog = ActivityLog.builder()
                    .action(action)
                    .entityType("AUTH")
                    .entityId(userId)
                    .userId(userId)
                    .description(description + " - Email: " + email)
                    .ipAddress(getClientIpAddress(request))
                    .userAgent(request.getHeader("User-Agent"))
                    .createdAt(LocalDateTime.now())
                    .build();

            activityLogRepository.save(activityLog);
            log.info("Auth activity logged: {} for user {}", action, email);
        } catch (Exception e) {
            log.error("Failed to log auth activity: {}", e.getMessage());
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
