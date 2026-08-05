package com.thinkerscave.security.service.impl;

import com.thinkerscave.access.entity.User;
import com.thinkerscave.access.entity.UserRole;
import com.thinkerscave.access.repository.UserRepository;
import com.thinkerscave.shared.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final PublicSchemaUserLookupService publicSchemaUserLookupService;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Resolves against whatever tenant schema is already active for this request
        // (set upstream by TenantFilter/AuthServiceImpl): "public" for Platform logins
        // (SUPER_ADMIN, Customer/Organization Owner) and the org's own schema for
        // Organization Admin / Staff / Teacher / Student / Parent logins. Do not
        // override the tenant context here — each account type physically lives in
        // exactly one schema and forcing "public" breaks every tenant-schema-only user.
        //
        // If not found in the ambient schema, fall back to an explicit public-schema
        // lookup (in a separate transaction — see PublicSchemaUserLookupService) so
        // Organization Owner / SUPER_ADMIN accounts still authenticate correctly even
        // when the ambient transaction is bound to a tenant institution's own schema
        // (e.g. an Owner logging into a specific organization).
        User user = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .or(() -> lookupInPublicSchema(username))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        List<SimpleGrantedAuthority> authorities = user.getUserRoles().stream()
                .filter(UserRole::getActive)
                .map(ur -> new SimpleGrantedAuthority(ur.getRole().getRoleType().name()))
                .toList();

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(Boolean.TRUE.equals(user.getAccountLocked()))
                .credentialsExpired(false)
                .disabled(!user.getStatus().name().equals("ACTIVE"))
                .build();
    }

    private Optional<User> lookupInPublicSchema(String username) {
        // TenantContext must be switched BEFORE calling the REQUIRES_NEW proxied method —
        // the new transaction's connection/tenant is resolved at proxy entry, not on the
        // first line of the callee's method body.
        String previousTenant = TenantContext.getTenant();
        try {
            TenantContext.setTenant("public");
            return publicSchemaUserLookupService.findAnyInPublicSchema(username);
        } finally {
            TenantContext.setTenant(previousTenant);
        }
    }
}
