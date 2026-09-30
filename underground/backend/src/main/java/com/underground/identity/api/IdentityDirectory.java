package com.underground.identity.api;

import java.util.UUID;

/** Public boundary: exposes eligibility, never credentials or private account data. */
public interface IdentityDirectory {
    boolean eligibleForMatching(UUID accountId);
}
