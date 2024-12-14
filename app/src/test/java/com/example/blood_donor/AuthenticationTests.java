package com.example.blood_donor;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.example.blood_donor.dto.auth.AuthResponse;
import com.example.blood_donor.dto.auth.DonorRegisterRequest;
import com.example.blood_donor.dto.auth.ManagerRegisterRequest;
import com.example.blood_donor.errors.AppException;
import com.example.blood_donor.errors.ErrorCode;
import com.example.blood_donor.models.user.User;
import com.example.blood_donor.models.user.UserType;
import com.example.blood_donor.repositories.IUserRepository;
import com.example.blood_donor.services.AuthService;
import com.example.blood_donor.services.UserService;
import com.example.blood_donor.repositories.ISessionRepository;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.UUID;

public class AuthenticationTests {
    @Mock
    private IUserRepository userRepository;

    @Mock
    private ISessionRepository sessionRepository;

    @Mock
    private AuthService authService;

    private UserService userService;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        userService = new UserService(
                userRepository,
                sessionRepository,
                authService
        );
    }

    private DonorRegisterRequest createValidDonorRequest() {
        DonorRegisterRequest request = new DonorRegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setFullName("Test User");
        request.setDateOfBirth(946684800000L);
        request.setBloodType("A+");
        request.setGender("M");
        return request;
    }

    private ManagerRegisterRequest createValidManagerRequest() {
        ManagerRegisterRequest request = new ManagerRegisterRequest();
        request.setEmail("manager@example.com");
        request.setPassword("password123");
        request.setFullName("Manager User");
        request.setDateOfBirth(946684800000L);
        request.setPhoneNumber("+61400000000");
        request.setGender("F");
        return request;
    }

    @Test
    public void testDonorRegistration() throws AppException {
        // Arrange
        DonorRegisterRequest request = createValidDonorRequest();

        // Mock dependencies
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(authService.hashPassword(request.getPassword())).thenReturn("hashedPassword");

        User mockUser = new User(
                UUID.randomUUID().toString(),
                request.getEmail(),
                "hashedPassword",
                request.getFullName(),
                request.getDateOfBirth(),
                null,
                UserType.DONOR,
                request.getBloodType(),
                request.getGender()
        );
        when(userRepository.createUser(any())).thenReturn(Optional.of(mockUser));
        when(authService.generateToken(any())).thenReturn("testToken");

        // Act
        AuthResponse response = userService.registerDonor(request).getData();

        // Assert
        assertNotNull(response);
        assertEquals(UserType.DONOR, response.getUser().getUserType());
        assertEquals(request.getEmail(), response.getUser().getEmail());
    }

    @Test
    public void testManagerRegistration() throws AppException {
        // Arrange
        ManagerRegisterRequest request = createValidManagerRequest();

        // Mock dependencies
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(authService.hashPassword(request.getPassword())).thenReturn("hashedPassword");

        User mockUser = new User(
                UUID.randomUUID().toString(),
                request.getEmail(),
                "hashedPassword",
                request.getFullName(),
                request.getDateOfBirth(),
                request.getPhoneNumber(),
                UserType.SITE_MANAGER,
                null,
                request.getGender()
        );
        when(userRepository.createUser(any())).thenReturn(Optional.of(mockUser));
        when(authService.generateToken(any())).thenReturn("testToken");

        // Act
        AuthResponse response = userService.registerManager(request).getData();

        // Assert
        assertNotNull(response);
        assertEquals(UserType.SITE_MANAGER, response.getUser().getUserType());
        assertEquals(request.getEmail(), response.getUser().getEmail());
    }

    @Test(expected = AppException.class)
    public void testExistingEmailForDonor() throws AppException {
        DonorRegisterRequest request = createValidDonorRequest();
        when(userRepository.existsByEmail(eq(request.getEmail())))
                .thenReturn(true);

        userService.registerDonor(request).throwErrorIfPresent();
    }

    @Test(expected = AppException.class)
    public void testExistingEmailForManager() throws AppException {
        ManagerRegisterRequest request = createValidManagerRequest();
        when(userRepository.existsByEmail(eq(request.getEmail())))
                .thenReturn(true);

        userService.registerManager(request).throwErrorIfPresent();
    }
}