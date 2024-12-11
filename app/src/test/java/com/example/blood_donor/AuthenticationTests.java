package com.example.blood_donor;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.example.blood_donor.database.DatabaseHelper;
import com.example.blood_donor.dto.auth.AuthResponse;
import com.example.blood_donor.dto.auth.LoginRequest;
import com.example.blood_donor.dto.auth.RegisterRequest;
import com.example.blood_donor.errors.AppException;
import com.example.blood_donor.errors.ErrorCode;
import com.example.blood_donor.models.response.ApiResponse;
import com.example.blood_donor.models.user.User;
import com.example.blood_donor.models.user.UserType;
import com.example.blood_donor.repositories.ISessionRepository;
import com.example.blood_donor.repositories.IUserRepository;
import com.example.blood_donor.services.AuthService;
import com.example.blood_donor.services.UserService;
import com.example.blood_donor.validators.RegisterRequestValidator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Optional;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {33})
public class AuthenticationTests {
    @Mock
    private IUserRepository userRepository;

    @Mock
    private ISessionRepository sessionRepository;

    private UserService userService;
    private AuthService authService;
    private RegisterRequestValidator registerValidator;
    private DatabaseHelper dbHelper;
    private Context context;

    @Before
    public void setup() {
        // Initialize Mockito annotations
        MockitoAnnotations.openMocks(this);

        context = RuntimeEnvironment.application.getApplicationContext();
        dbHelper = new DatabaseHelper(context);
        authService = new AuthService();
        registerValidator = new RegisterRequestValidator();

        // Initialize UserService with mocked dependencies
        userService = new UserService(
                userRepository,
                sessionRepository,
                authService,
                registerValidator
        );
    }

    @Test
    public void testDatabaseInitialization() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        assertTrue("Database should be opened successfully", db.isOpen());

        // Verify tables exist
        assertTrue("Users table should exist",
                isTableExists(db, "users"));
        assertTrue("Sessions table should exist",
                isTableExists(db, "sessions"));
    }

    @Test
    public void testSuccessfulRegistration() throws AppException {
        // Arrange
        RegisterRequest request = createValidRegisterRequest();
        User mockUser = createMockUser();

        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.createUser(any())).thenReturn(Optional.of(mockUser));

        // Act
        ApiResponse<AuthResponse> response = userService.register(request);

        // Assert
        assertTrue("Registration should be successful", response.isSuccess());
        assertNotNull("Response should contain user data", response.getData().getUser());
        assertNotNull("Response should contain token", response.getData().getToken());

        verify(userRepository).existsByEmail(request.getEmail());
        verify(userRepository).createUser(any(User.class));
        verify(sessionRepository).saveSession(any(), any());
    }

    @Test
    public void testRegistrationWithExistingEmail() throws AppException {
        // Arrange
        RegisterRequest request = createValidRegisterRequest();
        when(userRepository.existsByEmail(any())).thenReturn(true);

        // Act
        ApiResponse<AuthResponse> response = userService.register(request);

        // Assert
        assertFalse("Registration should fail", response.isSuccess());
        assertEquals("Should return EMAIL_ALREADY_EXISTS error",
                ErrorCode.EMAIL_ALREADY_EXISTS, response.getErrorCode());

        verify(userRepository).existsByEmail(request.getEmail());
        verify(userRepository, never()).createUser(any());
        verify(sessionRepository, never()).saveSession(any(), any());
    }

    @Test
    public void testSuccessfulLogin() throws AppException {
        // Arrange
        LoginRequest request = createLoginRequest();
        User mockUser = createMockUser();
        String hashedPassword = authService.hashPassword("password123");
        mockUser.setPassword(hashedPassword);

        when(userRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.of(mockUser));

        // Act
        ApiResponse<AuthResponse> response = userService.login(request);

        // Assert
        assertTrue("Login should be successful", response.isSuccess());
        assertNotNull("Response should contain user data", response.getData().getUser());
        assertNotNull("Response should contain token", response.getData().getToken());

        verify(userRepository).findByEmail(request.getEmail());
        verify(sessionRepository).saveSession(any(), eq(mockUser.getUserId()));
    }

    // Helper methods
    private boolean isTableExists(SQLiteDatabase db, String tableName) {
        return dbHelper.isTableExists(tableName);
    }

    private RegisterRequest createValidRegisterRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setFullName("Test User");
        request.setDateOfBirth(946684800000L); // 2000-01-01
        request.setBloodType("A+");
        request.setGender("M");
        return request;
    }

    private LoginRequest createLoginRequest() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");
        return request;
    }

    private User createMockUser() {
        return new User(
                "test-user-id",
                "test@example.com",
                "hashedPassword",
                "Test User",
                946684800000L,
                null,
                UserType.DONOR,
                "A+",
                "M"
        );
    }
}