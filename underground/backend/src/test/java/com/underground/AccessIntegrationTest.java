package com.underground;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.underground.identity.api.IdentityDirectory;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccessIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate db;
    @Autowired IdentityDirectory identities;
    static final String PASSWORD = "a-secure-password";

    @BeforeEach void clean() {
        db.update("DELETE FROM profiles");
        db.update("DELETE FROM identity_accounts");
    }

    String registration(String email, String role, boolean adult) throws Exception {
        return json.writeValueAsString(Map.of("email", email, "password", PASSWORD, "role", role, "adultConfirmed", adult));
    }

    String register(String email) throws Exception {
        var result = mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json")
                .content(registration(email, "CLIENT", true)))
            .andExpect(status().isCreated()).andExpect(jsonPath("verificationStatus").value("PENDING"))
            .andExpect(jsonPath("password").doesNotExist()).andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    MockHttpSession login(String email) throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/auth/login").with(csrf()).param("email", email).param("password", PASSWORD))
            .andExpect(status().isNoContent()).andReturn().getRequest().getSession(false);
    }

    @Test void registrationRulesAndHash() throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json").content(registration("one@example.org", "ADMIN", true))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json").content(registration("one@example.org", "CLIENT", false))).andExpect(status().isBadRequest());
        String id = register("ONE@example.org");
        assertThat(identities.eligibleForMatching(UUID.fromString(id))).isFalse();
        assertThat(db.queryForObject("SELECT password_hash FROM identity_accounts WHERE id=?", String.class, id)).startsWith("$2a$").isNotEqualTo(PASSWORD);
        mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json").content(registration("one@example.org", "HOST", true))).andExpect(status().isConflict());
        mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json").content(registration("two@example.org", "HOST", true))).andExpect(status().isCreated());
    }

    @Test void realLoginCsrfLogoutAndPrivateAccess() throws Exception {
        String first = register("one@example.org");
        String second = register("two@example.org");
        mvc.perform(get("/api/accounts/me")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").with(csrf()).param("email", "one@example.org").param("password", "wrong")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").param("email", "one@example.org").param("password", PASSWORD)).andExpect(status().isForbidden());
        MockHttpSession session = login("ONE@example.org");
        mvc.perform(get("/api/accounts/me").session(session)).andExpect(status().isOk()).andExpect(jsonPath("id").value(first));
        mvc.perform(get("/api/accounts/" + second).session(session)).andExpect(status().isForbidden());
        mvc.perform(put("/api/profiles/me").session(session).contentType("application/json").content("{\"displayName\":\"Name\",\"bio\":\"Bio\"}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/logout").session(session).with(csrf())).andExpect(status().isNoContent());
        assertThat(session.isInvalid()).isTrue();
        mvc.perform(get("/api/accounts/me")).andExpect(status().isUnauthorized());
    }

    @Test void eligibilityPublicProjectionAndOwnership() throws Exception {
        String first = register("one@example.org");
        String second = register("two@example.org");
        MockHttpSession session = login("one@example.org");
        mvc.perform(put("/api/profiles/me").session(session).with(csrf()).contentType("application/json").content("{\"displayName\":\"Name\",\"bio\":\"Bio\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("eligibleForMatching").value(false));
        mvc.perform(get("/api/profiles/me").session(session)).andExpect(status().isOk());
        mvc.perform(get("/api/profiles/" + first).session(session)).andExpect(status().isForbidden());
        mvc.perform(put("/api/profiles/" + first).session(login("two@example.org")).with(csrf()).contentType("application/json").content("{}"))
            .andExpect(status().isForbidden());
        // Fixture only: no HTTP endpoint may approve verification.
        db.update("UPDATE identity_accounts SET verification_status='VERIFIED' WHERE id=?", first);
        assertThat(identities.eligibleForMatching(UUID.fromString(first))).isTrue();
        mvc.perform(get("/api/profiles/" + second).session(session)).andExpect(status().isNotFound());
        var publicResult = mvc.perform(get("/api/profiles/" + first).session(session)).andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(publicResult.getResponse().getContentAsString()).size()).isEqualTo(3);
        mvc.perform(get("/api/profiles/" + first)).andExpect(status().isUnauthorized());
        db.update("UPDATE identity_accounts SET verification_status='VERIFIED' WHERE id=?", second);
        MockHttpSession other = login("two@example.org");
        mvc.perform(get("/api/profiles/" + first).session(other)).andExpect(status().isOk())
            .andExpect(jsonPath("email").doesNotExist()).andExpect(jsonPath("passwordHash").doesNotExist())
            .andExpect(jsonPath("eligibleForMatching").doesNotExist());
        mvc.perform(get("/api/profiles/me").session(other)).andExpect(status().isNotFound());
        db.update("UPDATE identity_accounts SET verification_status='REJECTED' WHERE id=?", first);
        assertThat(identities.eligibleForMatching(UUID.fromString(first))).isFalse();
        mvc.perform(get("/api/profiles/" + first).session(session)).andExpect(status().isForbidden());
    }

    @Test void cannotInjectVerificationOrOwnerAndAdminHasNoBypass() throws Exception {
        String id = register("one@example.org");
        mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json")
            .content(registration("two@example.org", "CLIENT", true).replace("}", ",\"verificationStatus\":\"VERIFIED\"}"))).andExpect(status().isBadRequest());
        mvc.perform(put("/api/profiles/me").session(login("one@example.org")).with(csrf()).contentType("application/json")
            .content("{\"displayName\":\"Name\",\"bio\":\"Bio\",\"accountId\":\"other\"}")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/accounts/" + id).with(user(UUID.randomUUID().toString()).roles("ADMIN"))).andExpect(status().isForbidden());
    }

    @Test void browserCsrfTokenWorksAndSessionRotates() throws Exception {
        register("browser@example.org");
        var csrfResult = mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        var token = json.readTree(csrfResult.getResponse().getContentAsString());
        MockHttpSession anonymous = (MockHttpSession) csrfResult.getRequest().getSession(false);
        String oldId = anonymous.getId();
        var loginResult = mvc.perform(post("/api/auth/login").session(anonymous)
            .header(token.get("headerName").asText(), token.get("token").asText())
            .param("email", "browser@example.org").param("password", PASSWORD))
            .andExpect(status().isNoContent()).andReturn();
        MockHttpSession authenticated = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(authenticated.getId()).isNotEqualTo(oldId);
        mvc.perform(post("/api/auth/logout").session(authenticated)
            .header(token.get("headerName").asText(), token.get("token").asText())).andExpect(status().isForbidden());
        var newCsrf = mvc.perform(get("/api/auth/csrf").session(authenticated)).andReturn();
        var newToken = json.readTree(newCsrf.getResponse().getContentAsString());
        mvc.perform(post("/api/auth/logout").session(authenticated)
            .header(newToken.get("headerName").asText(), newToken.get("token").asText())).andExpect(status().isNoContent());
    }
    @Test void csrfAndOpenApiAvailable() throws Exception {
        mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andExpect(jsonPath("token").isNotEmpty());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andExpect(jsonPath("paths['/api/auth/register']").exists());
    }
}
