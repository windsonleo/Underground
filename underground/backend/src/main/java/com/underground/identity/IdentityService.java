package com.underground.identity;

import com.underground.identity.api.IdentityDirectory;
import java.util.Locale;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@Service
class IdentityService implements IdentityDirectory, UserDetailsService {
    private final JdbcTemplate db;
    private final PasswordEncoder passwords;

    IdentityService(JdbcTemplate db, PasswordEncoder passwords) {
        this.db = db;
        this.passwords = passwords;
    }

    static String normalize(String email) { return email.strip().toLowerCase(Locale.ROOT); }

    @Transactional
    AccountView register(IdentityController.Registration input) {
        if (input.role() == Role.ADMIN) throw new ResponseStatusException(BAD_REQUEST, "ROLE_NOT_ALLOWED");
        UUID id = UUID.randomUUID();
        db.update("INSERT INTO identity_accounts (id,email,password_hash,role,adult_confirmed,verification_status) VALUES (?,?,?,?,?,?)",
            id.toString(), normalize(input.email()), passwords.encode(input.password()), input.role().name(), true, "PENDING");
        return account(id);
    }

    AccountView account(UUID id) {
        return db.query("SELECT id,email,role,adult_confirmed,verification_status FROM identity_accounts WHERE id=?",
            (rs, row) -> new AccountView(UUID.fromString(rs.getString("id")), rs.getString("email"),
                Role.valueOf(rs.getString("role")), rs.getBoolean("adult_confirmed"), rs.getString("verification_status")),
            id.toString()).stream().findFirst().orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        return db.query("SELECT id,password_hash,role FROM identity_accounts WHERE email=?",
            (rs, row) -> User.withUsername(rs.getString("id")).password(rs.getString("password_hash"))
                .roles(rs.getString("role")).build(),
            normalize(email)).stream().findFirst().orElseThrow(() -> new UsernameNotFoundException("INVALID_CREDENTIALS"));
    }

    @Override
    public boolean eligibleForMatching(UUID id) {
        return Boolean.TRUE.equals(db.queryForObject(
            "SELECT COUNT(*) > 0 FROM identity_accounts WHERE id=? AND adult_confirmed=TRUE AND verification_status='VERIFIED' AND role IN ('CLIENT','HOST')",
            Boolean.class, id.toString()));
    }

    enum Role { CLIENT, HOST, ADMIN }
    record AccountView(UUID id, String email, Role role, boolean adultConfirmed, String verificationStatus) {}
}
