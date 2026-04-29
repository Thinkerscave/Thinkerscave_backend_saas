package com.thinkerscave.common.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.thinkerscave.common.menum.domain.Role;
import com.thinkerscave.common.menum.domain.RoleMenuPrivilegeMapping;
import com.thinkerscave.common.usrm.domain.User;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class UserInfoUserDetails implements UserDetails {

	private static final long serialVersionUID = 1L;
	private Long userId;
	private String name;
	private String password;
	private List<GrantedAuthority> authorities;
	private Long roleId;
	private boolean blocked;

	public UserInfoUserDetails(User user, List<RoleMenuPrivilegeMapping> matrixMappings) {
		this.userId = user.getId();
		this.name = user.getUserName();
		this.password = user.getPassword();
		this.blocked = Boolean.TRUE.equals(user.getIsBlocked());

		// 1. Base Roles
		this.authorities = user.getRoles().stream()
				.flatMap(role -> {
					String roleName = role.getRoleName();
					String roleWithPrefix = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;
					String roleWithoutPrefix = roleName.replaceFirst("^ROLE_", "");
					return java.util.stream.Stream.of(
							new SimpleGrantedAuthority(roleWithPrefix),
							new SimpleGrantedAuthority(roleWithoutPrefix));
				})
				.collect(Collectors.toList());

		// 2. Matrix Privileges (e.g., COURSES_ADD, SYLLABUS_EDIT)
		if (matrixMappings != null) {
			for (RoleMenuPrivilegeMapping mapping : matrixMappings) {
				if (mapping.getSubMenu() != null && mapping.getPrivilege() != null) {
					String authString = mapping.getSubMenu().getSubMenuCode() + "_"
							+ mapping.getPrivilege().getPrivilegeName();
					this.authorities.add(new SimpleGrantedAuthority(authString));
				}
			}
		}

		if (user.getRoles() != null && !user.getRoles().isEmpty()) {
			Role role = user.getRoles().iterator().next();
			this.roleId = role.getRoleId();
		}
	}

	public Long getUserId() {
		return userId;
	}

	public Long getRoleId() {
		return roleId;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public String getUsername() {
		return name;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return !this.blocked;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

}
