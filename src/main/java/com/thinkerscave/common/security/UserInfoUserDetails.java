package com.thinkerscave.common.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.thinkerscave.common.menum.domain.Role;
import com.thinkerscave.common.usrm.domain.User;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class UserInfoUserDetails implements UserDetails {

	
	private static final long serialVersionUID = 1L;
	private Long userId;
	private String name;
	private String password;
	private List<GrantedAuthority> 	authorities;
	private Long roleId;
		
	public UserInfoUserDetails(User user) {
		this.userId=user.getId();
        this.name = user.getUserName();
        this.password = user.getPassword();
        this.authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getRoleName()))
                .collect(Collectors.toList());
        Role role = user.getRoles().iterator().next();
        this.roleId = role.getRoleId();
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
		return true;
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
