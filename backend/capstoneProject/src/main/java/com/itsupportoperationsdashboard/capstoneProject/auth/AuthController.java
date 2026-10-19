package com.itsupportoperationsdashboard.capstoneProject.auth;

import com.itsupportoperationsdashboard.capstoneProject.auth.AuthDtos.*;
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

    public AuthController(AuthenticationManager manager, SecurityContextRepository contexts, CsrfTokenRepository csrfTokens) {
        this.manager = manager;
        this.contexts = contexts;
        this.csrfTokens = csrfTokens;
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

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AppPrincipal principal) {
        return principal.profile();
    }
}
