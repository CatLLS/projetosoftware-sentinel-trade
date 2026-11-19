package com.sentineltrade.api;

import com.sentineltrade.domain.user.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/** The authenticated caller, as read from the JWT. */
record CurrentUser(Long id, boolean canReadAll) {

    static CurrentUser of(Authentication authentication) {
        boolean canReadAll = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals(Role.AUDITOR.authority()) || a.equals(Role.ADMIN.authority()));
        return new CurrentUser(Long.valueOf(authentication.getName()), canReadAll);
    }
}
