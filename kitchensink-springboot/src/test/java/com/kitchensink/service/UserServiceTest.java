package com.kitchensink.service;

import com.kitchensink.exception.ResourceConflictException;
import com.kitchensink.exception.ResourceNotFoundException;
import com.kitchensink.model.Role;
import com.kitchensink.model.User;
import com.kitchensink.model.UserRole;
import com.kitchensink.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private InputSanitizationService sanitizationService;

    @Mock
    private RoleService roleService;

    @Mock
    private UserRoleService userRoleService;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private Role testRole;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId("user-1");
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setIsdCode("+91");
        testUser.setPhoneNumber("9876543210");
        testUser.setEmailEncrypted("encrypted-email");
        testUser.setPhoneNumberEncrypted("encrypted-phone");
        testUser.setEmailHash("email-hash");
        testUser.setPhoneNumberHash("phone-hash");
        testUser.setRegistrationDate(LocalDateTime.now());
        testUser.setStatus("ACTIVE");

        testRole = new Role();
        testRole.setId("role-1");
        testRole.setName("USER");
    }

    @Test
    @DisplayName("Should create user successfully")
    void testCreateUser_Success() {
        when(sanitizationService.sanitizeForName("Test User")).thenReturn("Test User");
        when(sanitizationService.sanitizeForEmail("test@example.com")).thenReturn("test@example.com");
        when(sanitizationService.sanitizeForPhone("9876543210")).thenReturn("9876543210");
        when(roleService.createRoleIfNotExists("USER", "USER role")).thenReturn(testRole);
        when(encryptionService.encrypt("test@example.com")).thenReturn("encrypted-email");
        when(encryptionService.encrypt("9876543210")).thenReturn("encrypted-phone");
        when(encryptionService.hash("test@example.com")).thenReturn("email-hash");
        when(encryptionService.hash("9876543210")).thenReturn("phone-hash");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userRoleService.assignRoleToUser("user-1", "role-1")).thenReturn(new UserRole("user-1", "role-1"));

        User created = userService.createUser("Test User", "test@example.com", "+91", "9876543210", "USER",
                "01-01-1990", "Address", "City", "Country");

        assertThat(created).isNotNull();
        assertThat(created.getId()).isEqualTo("user-1");
        verify(userRepository).save(any(User.class));
        verify(userRoleService).assignRoleToUser("user-1", "role-1");
    }

    @Test
    @DisplayName("Should throw exception on duplicate email")
    void testCreateUser_DuplicateEmail() {
        when(sanitizationService.sanitizeForName(anyString())).thenReturn("Test User");
        when(sanitizationService.sanitizeForEmail(anyString())).thenReturn("test@example.com");
        when(sanitizationService.sanitizeForPhone(anyString())).thenReturn("9876543210");
        when(roleService.createRoleIfNotExists(anyString(), anyString())).thenReturn(testRole);
        when(encryptionService.encrypt(anyString())).thenReturn("encrypted");
        when(encryptionService.hash(anyString())).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenThrow(new DuplicateKeyException("Duplicate key: emailHash"));

        assertThatThrownBy(() -> userService.createUser("Test User", "test@example.com", "+91", "9876543210", "USER",
                null, null, null, null))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Email already exists");
    }

    @Test
    @DisplayName("Should get user by email successfully")
    void testGetUserByEmail_Success() {
        when(encryptionService.hash("test@example.com")).thenReturn("email-hash");
        when(userRepository.findByEmailHash("email-hash")).thenReturn(Optional.of(testUser));
        when(encryptionService.decrypt("encrypted-email")).thenReturn("test@example.com");
        when(encryptionService.decrypt("encrypted-phone")).thenReturn("9876543210");

        Optional<User> result = userService.getUserByEmail("test@example.com");

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should get user by ID successfully")
    void testGetUserById_Success() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
        when(encryptionService.decrypt("encrypted-email")).thenReturn("test@example.com");
        when(encryptionService.decrypt("encrypted-phone")).thenReturn("9876543210");

        User result = userService.getUserById("user-1");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("user-1");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should throw exception when user not found by ID")
    void testGetUserById_NotFound() {
        when(userRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById("nonexistent"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should search users by name")
    void testSearchUsersByName() {
        when(sanitizationService.sanitizeForName("Test")).thenReturn("Test");
        when(userRepository.searchByName(".*Test.*")).thenReturn(Collections.singletonList(testUser));
        when(encryptionService.decrypt("encrypted-email")).thenReturn("test@example.com");
        when(encryptionService.decrypt("encrypted-phone")).thenReturn("9876543210");

        List<User> result = userService.searchUsersByName("Test");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Test User");
    }

    @Test
    @DisplayName("Should update user successfully")
    void testUpdateUser_Success() {
        User existingUser = new User();
        existingUser.setId("user-1");
        existingUser.setName("Old Name");
        existingUser.setEmail("old@example.com");
        existingUser.setEmailEncrypted("old-encrypted");
        existingUser.setEmailHash("old-hash");

        when(userRepository.findById("user-1")).thenReturn(Optional.of(existingUser));
        when(encryptionService.decrypt("old-encrypted")).thenReturn("old@example.com");
        when(sanitizationService.sanitizeForName("New Name")).thenReturn("New Name");
        when(encryptionService.encrypt("new@example.com")).thenReturn("new-encrypted");
        when(encryptionService.hash("new@example.com")).thenReturn("new-hash");
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        User updated = userService.updateUser("user-1", "New Name", "new@example.com", "+91", "9876543211",
                "01-01-1995", "New Address", "New City", "New Country");

        assertThat(updated).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should delete user successfully")
    void testDeleteUser_Success() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
        doNothing().when(userRepository).deleteById("user-1");

        userService.deleteUser("user-1");

        verify(userRepository).deleteById("user-1");
    }

    @Test
    @DisplayName("Should update user email successfully")
    void testUpdateUserEmail() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
        when(encryptionService.decrypt("encrypted-email")).thenReturn("test@example.com");
        when(encryptionService.encrypt(anyString())).thenAnswer(invocation -> {
            String arg = invocation.getArgument(0);
            if (arg == null) return "encrypted-null";
            if ("new@example.com".equals(arg)) {
                return "new-encrypted";
            }
            return "encrypted-" + arg;
        });
        when(encryptionService.hash(anyString())).thenAnswer(invocation -> {
            String arg = invocation.getArgument(0);
            if (arg == null) return "hash-null";
            if ("new@example.com".equals(arg)) {
                return "new-hash";
            }
            return "hash-" + arg;
        });
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User updated = userService.updateUserEmail("user-1", "new@example.com");

        assertThat(updated).isNotNull();
        verify(encryptionService).encrypt("new@example.com");
    }

    @Test
    @DisplayName("Should update user phone number successfully")
    void testUpdateUserPhoneNumber() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
        when(encryptionService.decrypt("encrypted-email")).thenReturn("test@example.com");
        when(encryptionService.decrypt("encrypted-phone")).thenReturn("9876543210");
        when(encryptionService.encrypt(anyString())).thenAnswer(invocation -> {
            String arg = invocation.getArgument(0);
            if (arg == null) return "encrypted-null";
            if ("9876543211".equals(arg)) {
                return "new-encrypted-phone";
            }
            return "encrypted-" + arg;
        });
        when(encryptionService.hash(anyString())).thenAnswer(invocation -> {
            String arg = invocation.getArgument(0);
            if (arg == null) return "hash-null";
            if ("9876543211".equals(arg)) {
                return "new-phone-hash";
            }
            return "hash-" + arg;
        });
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User updated = userService.updateUserPhoneNumber("user-1", "9876543211");

        assertThat(updated).isNotNull();
        verify(encryptionService).encrypt("9876543211");
    }

    @Test
    @DisplayName("Should update last login date")
    void testUpdateLastLoginDate() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        userService.updateLastLoginDate("user-1");

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should get all users excluding admins with cursor pagination - first page")
    void testGetAllUsersExcludingAdminsCursor_FirstPage() {
        Role adminRole = new Role();
        adminRole.setId("admin-role-1");
        adminRole.setName("ADMIN");

        User user1 = new User();
        user1.setId("user-1");
        user1.setName("User One");
        user1.setEmailEncrypted("encrypted-1");
        user1.setPhoneNumberEncrypted("encrypted-phone-1");

        User user2 = new User();
        user2.setId("user-2");
        user2.setName("User Two");
        user2.setEmailEncrypted("encrypted-2");
        user2.setPhoneNumberEncrypted("encrypted-phone-2");

        when(roleService.getRoleByName("ADMIN")).thenReturn(adminRole);
        when(userRoleService.getAllUserIdsByRoleId("admin-role-1")).thenReturn(List.of("admin-1", "admin-2"));
        when(userRepository.findByIdNotInOrderByNameAsc(eq(List.of("admin-1", "admin-2")), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user1, user2), Pageable.ofSize(11), 2));
        when(encryptionService.decrypt(anyString())).thenAnswer(invocation -> {
            String arg = invocation.getArgument(0);
            if (arg.contains("email")) return "decrypted-email";
            if (arg.contains("phone")) return "decrypted-phone";
            return arg;
        });

        com.kitchensink.dto.CursorPageResponse<User> result = 
                userService.getAllUsersExcludingAdminsCursor(null, 10, com.kitchensink.enums.Direction.NEXT);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getNextCursor()).isEqualTo("user-2");
        assertThat(result.isHasNext()).isFalse();
        assertThat(result.isHasPrevious()).isFalse();
    }

    @Test
    @DisplayName("Should get all users excluding admins with cursor pagination - next page")
    void testGetAllUsersExcludingAdminsCursor_NextPage() {
        Role adminRole = new Role();
        adminRole.setId("admin-role-1");
        adminRole.setName("ADMIN");

        User user3 = new User();
        user3.setId("user-3");
        user3.setName("User Three");
        user3.setEmailEncrypted("encrypted-3");
        user3.setPhoneNumberEncrypted("encrypted-phone-3");

        when(roleService.getRoleByName("ADMIN")).thenReturn(adminRole);
        when(userRoleService.getAllUserIdsByRoleId("admin-role-1")).thenReturn(List.of("admin-1"));
        when(userRepository.findByIdNotInAndIdGreaterThanOrderByIdAsc(
                eq(List.of("admin-1")), eq("user-2"), any(Pageable.class)))
                .thenReturn(List.of(user3));
        when(userRepository.existsByIdNotInAndIdGreaterThan(eq(List.of("admin-1")), eq("user-3")))
                .thenReturn(false);
        when(encryptionService.decrypt(anyString())).thenAnswer(invocation -> {
            String arg = invocation.getArgument(0);
            if (arg.contains("email")) return "decrypted-email";
            if (arg.contains("phone")) return "decrypted-phone";
            return arg;
        });

        com.kitchensink.dto.CursorPageResponse<User> result = 
                userService.getAllUsersExcludingAdminsCursor("user-2", 10, com.kitchensink.enums.Direction.NEXT);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getNextCursor()).isEqualTo("user-3");
        assertThat(result.isHasNext()).isFalse();
        assertThat(result.isHasPrevious()).isTrue();
        assertThat(result.getPreviousCursor()).isEqualTo("user-2");
    }

    @Test
    @DisplayName("Should get all users excluding admins with cursor pagination - previous page")
    void testGetAllUsersExcludingAdminsCursor_PreviousPage() {
        Role adminRole = new Role();
        adminRole.setId("admin-role-1");
        adminRole.setName("ADMIN");

        User user1 = new User();
        user1.setId("user-1");
        user1.setName("User One");
        user1.setEmailEncrypted("encrypted-1");
        user1.setPhoneNumberEncrypted("encrypted-phone-1");

        when(roleService.getRoleByName("ADMIN")).thenReturn(adminRole);
        when(userRoleService.getAllUserIdsByRoleId("admin-role-1")).thenReturn(List.of("admin-1"));
        when(userRepository.findByIdNotInAndIdLessThanOrderByIdAsc(
                eq(List.of("admin-1")), eq("user-2"), any(Pageable.class)))
                .thenReturn(List.of(user1));
        when(userRepository.existsByIdNotInAndIdLessThan(eq(List.of("admin-1")), eq("user-1")))
                .thenReturn(false);
        when(encryptionService.decrypt(anyString())).thenAnswer(invocation -> {
            String arg = invocation.getArgument(0);
            if (arg.contains("email")) return "decrypted-email";
            if (arg.contains("phone")) return "decrypted-phone";
            return arg;
        });

        com.kitchensink.dto.CursorPageResponse<User> result = 
                userService.getAllUsersExcludingAdminsCursor("user-2", 10, com.kitchensink.enums.Direction.PREV);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getPreviousCursor()).isEqualTo("user-1");
        assertThat(result.isHasPrevious()).isFalse();
        assertThat(result.isHasNext()).isTrue();
        assertThat(result.getNextCursor()).isEqualTo("user-2");
    }

    @Test
    @DisplayName("Should return empty result when admin role not found")
    void testGetAllUsersExcludingAdminsCursor_AdminRoleNotFound() {
        when(roleService.getRoleByName("ADMIN")).thenThrow(new ResourceNotFoundException("Role", "ADMIN"));

        com.kitchensink.dto.CursorPageResponse<User> result = 
                userService.getAllUsersExcludingAdminsCursor(null, 10, com.kitchensink.enums.Direction.NEXT);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getSize()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should return empty result when no admin users exist")
    void testGetAllUsersExcludingAdminsCursor_NoAdminUsers() {
        Role adminRole = new Role();
        adminRole.setId("admin-role-1");
        adminRole.setName("ADMIN");

        when(roleService.getRoleByName("ADMIN")).thenReturn(adminRole);
        when(userRoleService.getAllUserIdsByRoleId("admin-role-1")).thenReturn(Collections.emptyList());

        com.kitchensink.dto.CursorPageResponse<User> result = 
                userService.getAllUsersExcludingAdminsCursor(null, 10, com.kitchensink.enums.Direction.NEXT);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getSize()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should handle invalid size parameter")
    void testGetAllUsersExcludingAdminsCursor_InvalidSize() {
        Role adminRole = new Role();
        adminRole.setId("admin-role-1");
        adminRole.setName("ADMIN");

        when(roleService.getRoleByName("ADMIN")).thenReturn(adminRole);
        when(userRoleService.getAllUserIdsByRoleId("admin-role-1")).thenReturn(List.of("admin-1"));
        when(userRepository.findByIdNotInOrderByNameAsc(eq(List.of("admin-1")), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList(), Pageable.ofSize(11), 0));
        lenient().when(encryptionService.decrypt(anyString())).thenReturn("decrypted");

        // Test with size > 100 (should default to 10)
        com.kitchensink.dto.CursorPageResponse<User> result = 
                userService.getAllUsersExcludingAdminsCursor(null, 200, com.kitchensink.enums.Direction.NEXT);

        assertThat(result).isNotNull();
        verify(userRepository).findByIdNotInOrderByNameAsc(eq(List.of("admin-1")), 
                argThat(pageable -> pageable.getPageSize() == 11)); // size + 1
    }

    @Test
    @DisplayName("Should handle previous page with null cursor")
    void testGetAllUsersExcludingAdminsCursor_PreviousPageNoCursor() {
        Role adminRole = new Role();
        adminRole.setId("admin-role-1");
        adminRole.setName("ADMIN");

        when(roleService.getRoleByName("ADMIN")).thenReturn(adminRole);
        when(userRoleService.getAllUserIdsByRoleId("admin-role-1")).thenReturn(List.of("admin-1"));

        com.kitchensink.dto.CursorPageResponse<User> result = 
                userService.getAllUsersExcludingAdminsCursor(null, 10, com.kitchensink.enums.Direction.PREV);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getSize()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should handle hasNext correctly when more records exist")
    void testGetAllUsersExcludingAdminsCursor_HasNext() {
        Role adminRole = new Role();
        adminRole.setId("admin-role-1");
        adminRole.setName("ADMIN");

        User user1 = new User();
        user1.setId("user-1");
        user1.setName("User One");
        user1.setEmailEncrypted("encrypted-1");
        user1.setPhoneNumberEncrypted("encrypted-phone-1");

        User user2 = new User();
        user2.setId("user-2");
        user2.setName("User Two");
        user2.setEmailEncrypted("encrypted-2");
        user2.setPhoneNumberEncrypted("encrypted-phone-2");

        when(roleService.getRoleByName("ADMIN")).thenReturn(adminRole);
        when(userRoleService.getAllUserIdsByRoleId("admin-role-1")).thenReturn(List.of("admin-1"));
        when(userRepository.findByIdNotInAndIdGreaterThanOrderByIdAsc(
                eq(List.of("admin-1")), eq("user-0"), any(Pageable.class)))
                .thenReturn(List.of(user1, user2)); // 2 records, size=1, so hasNext=true
        lenient().when(userRepository.existsByIdNotInAndIdGreaterThan(anyList(), anyString()))
                .thenReturn(true);
        when(userRepository.existsByIdNotInAndIdGreaterThan(eq(List.of("admin-1")), eq("user-1")))
                .thenReturn(true);
        when(encryptionService.decrypt(anyString())).thenAnswer(invocation -> {
            String arg = invocation.getArgument(0);
            if (arg.contains("email")) return "decrypted-email";
            if (arg.contains("phone")) return "decrypted-phone";
            return arg;
        });

        com.kitchensink.dto.CursorPageResponse<User> result = 
                userService.getAllUsersExcludingAdminsCursor("user-0", 1, com.kitchensink.enums.Direction.NEXT);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.isHasNext()).isTrue();
        assertThat(result.getNextCursor()).isEqualTo("user-1");
    }
}

