package com.itsupportoperationsdashboard.capstoneProject.auth;

import com.itsupportoperationsdashboard.capstoneProject.auth.AuthDtos.*;
import com.itsupportoperationsdashboard.capstoneProject.service.UserService;
import com.itsupportoperationsdashboard.capstoneProject.security.AppPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager manager;
    private final SecurityContextRepository contexts;
    private final CsrfTokenRepository csrfTokens;
    private final UserService users;

    public AuthController(AuthenticationManager manager, SecurityContextRepository contexts,
                          CsrfTokenRepository csrfTokens, UserService users) {
        this.manager = manager;
        this.contexts = contexts;
        this.csrfTokens = csrfTokens;
        this.users = users;
    }

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getHeaderName(), token.getToken());
    }

    @PostMapping("/login")
    public UserResponse login(@Valid @RequestBody LoginRequest credentials,
                              HttpServletRequest request, HttpServletResponse response) {
        var authentication = manager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(
                credentials.email(), credentials.password()));
        new ChangeSessionIdAuthenticationStrategy().onAuthentication(authentication, request, response);
        csrfTokens.saveToken(null, request, response);
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context, request, response);
        return ((AppPrincipal) authentication.getPrincipal()).profile();
    }

    @PostMapping("/register")
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        var user = users.registerEndUser(request.name(), request.email(), request.password());
        return new UserResponse(user.getUserId(), user.getName(), user.getEmail(), "END_USER");
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AppPrincipal principal) {
        return principal.profile();
    }
}
