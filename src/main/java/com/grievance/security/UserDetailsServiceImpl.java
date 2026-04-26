package com.grievance.security;

import com.grievance.model.User;
import com.grievance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found: " + username));

        // FIX 1: Use Collections.singletonList() instead of List.of().
        //         Spring Security internally calls Collection.contains() on the
        //         authorities list inside some paths; singletonList is safer and
        //         avoids any unmodifiable-collection edge cases at runtime.
        //
        // FIX 2: Use the explicit UserDetailsService import rather than the
        //         fully-qualified inner class path — prevents "cannot find symbol"
        //         when the wildcard import org.springframework.security.core.userdetails.*
        //         is removed or the IDE resolves ambiguity differently.
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(
                        new SimpleGrantedAuthority(user.getRole())
                )
        );
    }
}