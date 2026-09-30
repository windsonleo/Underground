package com.underground.profiles;

import com.underground.identity.api.IdentityDirectory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@RestController
@RequestMapping("/api/profiles")
class ProfileController {
    private final JdbcTemplate db;
    private final IdentityDirectory identities;

    ProfileController(JdbcTemplate db, IdentityDirectory identities) {
        this.db = db;
        this.identities = identities;
    }

    @GetMapping("/me")
    PrivateProfile own(Principal principal) {
        UUID id = UUID.fromString(principal.getName());
        PublicProfile profile = find(id);
        return new PrivateProfile(id, profile.displayName(), profile.bio(), identities.eligibleForMatching(id));
    }

    @PutMapping("/me")
    @Transactional
    PrivateProfile save(Principal principal, @Valid @RequestBody ProfileInput input) {
        UUID id = UUID.fromString(principal.getName());
        int updated = db.update("UPDATE profiles SET display_name=?,bio=? WHERE account_id=?",
            input.displayName().strip(), input.bio(), id.toString());
        if (updated == 0) db.update("INSERT INTO profiles (account_id,display_name,bio) VALUES (?,?,?)",
            id.toString(), input.displayName().strip(), input.bio());
        return own(principal);
    }

    @GetMapping("/{id}")
    PublicProfile publicProfile(Principal principal, @PathVariable UUID id) {
        if (!identities.eligibleForMatching(UUID.fromString(principal.getName()))) {
            throw new ResponseStatusException(FORBIDDEN, "VERIFICATION_REQUIRED");
        }
        if (!identities.eligibleForMatching(id)) throw new ResponseStatusException(NOT_FOUND);
        return find(id);
    }

    private PublicProfile find(UUID id) {
        return db.query("SELECT account_id,display_name,bio FROM profiles WHERE account_id=?",
            (rs, row) -> new PublicProfile(UUID.fromString(rs.getString("account_id")), rs.getString("display_name"), rs.getString("bio")),
            id.toString()).stream().findFirst().orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
    }

    record ProfileInput(@NotBlank @Size(max=80) String displayName, @NotNull @Size(max=1000) String bio) {}
    record PublicProfile(UUID id, String displayName, String bio) {}
    record PrivateProfile(UUID id, String displayName, String bio, boolean eligibleForMatching) {}
}
