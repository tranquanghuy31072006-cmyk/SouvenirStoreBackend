package com.learning.souvenirstorebackend.configuration;

import com.learning.souvenirstorebackend.entity.Role;
import com.learning.souvenirstorebackend.entity.User;
import com.learning.souvenirstorebackend.repository.RoleRepository;
import com.learning.souvenirstorebackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DataInitializerTest {

    @Mock
    RoleRepository roleRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    DataInitializer dataInitializer;

    @Captor
    ArgumentCaptor<User> userCaptor;
    @Captor
    ArgumentCaptor<Role> roleCaptor;

    private static final ApplicationArguments NO_ARGS = null;

    private Map<String, Role> roleStore;

    @BeforeEach
    void setUp() {
        roleStore = new HashMap<>();
    }

    private void wireFindById() {
        when(roleRepository.findById(anyString())).thenAnswer(inv -> {
            String name = inv.getArgument(0);
            return Optional.ofNullable(roleStore.get(name));
        });
    }

    private void wireRoleSave() {
        when(roleRepository.save(any(Role.class))).thenAnswer(inv -> {
            Role r = inv.getArgument(0);
            roleStore.put(r.getName(), r);
            return r;
        });
    }

    @Test
    void run_whenRolesNotExist_createsAllThreeRoles() {
        wireFindById();
        wireRoleSave();
        when(userRepository.countByRole_Name(anyString())).thenReturn(0L);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");

        dataInitializer.run(NO_ARGS);

        verify(roleRepository, times(3)).save(roleCaptor.capture());
        assertThat(roleCaptor.getAllValues())
                .extracting(Role::getName)
                .containsExactlyInAnyOrder("ADMIN", "STAFF", "CUSTOMER");
    }

    @Test
    void run_whenRolesExist_doesNotCreateDuplicates() {
        roleStore.put("ADMIN", Role.builder().name("ADMIN").build());
        roleStore.put("STAFF", Role.builder().name("STAFF").build());
        roleStore.put("CUSTOMER", Role.builder().name("CUSTOMER").build());
        wireFindById();
        when(userRepository.countByRole_Name(anyString())).thenReturn(1L);

        dataInitializer.run(NO_ARGS);

        verify(roleRepository, never()).save(any(Role.class));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void run_whenRolesExistAndNoUsers_createsDefaultUsersForAdminAndStaffOnly() {
        roleStore.put("ADMIN", Role.builder().name("ADMIN").build());
        roleStore.put("STAFF", Role.builder().name("STAFF").build());
        roleStore.put("CUSTOMER", Role.builder().name("CUSTOMER").build());
        wireFindById();
        when(userRepository.countByRole_Name(anyString())).thenReturn(0L);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        dataInitializer.run(NO_ARGS);

        verify(userRepository, times(2)).save(userCaptor.capture());
        assertThat(userCaptor.getAllValues())
                .extracting(u -> u.getRole().getName())
                .containsExactlyInAnyOrder("ADMIN", "STAFF");
    }

    @Test
    void run_whenRoleHasOneUser_doesNotCreateUser() {
        roleStore.put("ADMIN", Role.builder().name("ADMIN").build());
        roleStore.put("STAFF", Role.builder().name("STAFF").build());
        roleStore.put("CUSTOMER", Role.builder().name("CUSTOMER").build());
        wireFindById();
        when(userRepository.countByRole_Name(anyString())).thenReturn(1L);

        dataInitializer.run(NO_ARGS);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void run_whenRoleHasMultipleUsers_throwsIllegalArgument() {
        roleStore.put("ADMIN", Role.builder().name("ADMIN").build());
        roleStore.put("STAFF", Role.builder().name("STAFF").build());
        roleStore.put("CUSTOMER", Role.builder().name("CUSTOMER").build());
        wireFindById();
        when(userRepository.countByRole_Name("ADMIN")).thenReturn(2L);

        assertThatThrownBy(() -> dataInitializer.run(NO_ARGS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("more than one ADMIN");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void run_whenRoleMissingForDefaultUser_throwsIllegalArgument() {
        when(roleRepository.findById(anyString())).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.countByRole_Name("ADMIN")).thenReturn(0L);

        assertThatThrownBy(() -> dataInitializer.run(NO_ARGS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Role ADMIN not found");
    }

    @Test
    void run_encodesDefaultPasswordBeforeSave() {
        roleStore.put("ADMIN", Role.builder().name("ADMIN").build());
        roleStore.put("STAFF", Role.builder().name("STAFF").build());
        roleStore.put("CUSTOMER", Role.builder().name("CUSTOMER").build());
        wireFindById();
        when(userRepository.countByRole_Name(anyString())).thenReturn(0L);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        dataInitializer.run(NO_ARGS);

        verify(passwordEncoder, atLeastOnce()).encode(anyString());
        verify(userRepository, times(2)).save(userCaptor.capture());
        assertThat(userCaptor.getAllValues())
                .allMatch(u -> "hashed".equals(u.getPassword()))
                .noneMatch(u -> "admin@12345".equals(u.getPassword()))
                .noneMatch(u -> "staff@12345".equals(u.getPassword()));
    }

    @Test
    void run_twice_isIdempotent() {
        roleStore.put("ADMIN", Role.builder().name("ADMIN").build());
        roleStore.put("STAFF", Role.builder().name("STAFF").build());
        roleStore.put("CUSTOMER", Role.builder().name("CUSTOMER").build());
        wireFindById();
        when(userRepository.countByRole_Name("ADMIN")).thenReturn(0L, 1L);
        when(userRepository.countByRole_Name("STAFF")).thenReturn(0L, 1L);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        dataInitializer.run(NO_ARGS);
        dataInitializer.run(NO_ARGS);

        verify(userRepository, times(2)).save(any(User.class));
        verify(roleRepository, never()).save(any(Role.class));
    }
}