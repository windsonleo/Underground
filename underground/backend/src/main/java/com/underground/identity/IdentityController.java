package com.underground.identity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
class IdentityController {
    private final IdentityService identity;
    IdentityController(IdentityService identity) { this.identity = identity; }

    @GetMapping("/auth/csrf")
    CsrfView csrf(CsrfToken token) { return new CsrfView(token.getHeaderName(), token.getToken()); }

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    IdentityService.AccountView register(@Valid @RequestBody Registration input) { return identity.register(input); }

    @GetMapping("/accounts/me")
    IdentityService.AccountView me(Principal principal) { return identity.account(UUID.fromString(principal.getName())); }

    record Registration(
        @NotBlank @Email @Size(max=254) String email,
        @NotBlank @Size(min=12, max=64) @Pattern(regexp="[\\x20-\\x7E]+") String password,
        @NotNull IdentityService.Role role,
        @NotNull @AssertTrue Boolean adultConfirmed) {
        @Override public String toString() { return "Registration[REDACTED]"; }
    }
    record CsrfView(String headerName, String token) {}
}
