package com.cryptoshield.cryptoshield.user.service;

import com.cryptoshield.cryptoshield.user.dto.RedisToken;
import com.cryptoshield.cryptoshield.user.dto.UserRequest;
import com.cryptoshield.cryptoshield.user.dto.UserResponse;
import com.cryptoshield.cryptoshield.user.entity.InvalidateToken;
import com.cryptoshield.cryptoshield.user.entity.User;
import com.cryptoshield.cryptoshield.user.repository.InvalidateTokenRepository;
import com.cryptoshield.cryptoshield.user.repository.RedisTokenRepository;
import com.cryptoshield.cryptoshield.user.repository.UserRepository;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private InvalidateTokenRepository invalidateTokenRepository;

    @Mock
    private RedisTokenRepository redisTokenRepository;

    @Mock
    private PasswordEncoder pwdEncoder;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        userService.SIGNER_KEY = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    }

    @Test
    void registerUserShouldCreateUserAndReturnTokens() {
        UserRequest request = UserRequest.builder()
                .email("user@example.com")
                .password("secret")
                .username("user")
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(pwdEncoder.encode(request.getPassword())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.registerUser(request);

        assertThat(response.getEmail()).isEqualTo(request.getEmail());
        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isNotBlank();
        verify(userRepository).save(any(User.class));
        verify(invalidateTokenRepository).save(any(InvalidateToken.class));
    }

    @Test
    void registerUserShouldThrowWhenEmailAlreadyExists() {
        UserRequest request = UserRequest.builder().email("existing@example.com").password("secret").build();
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> userService.registerUser(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email existed");
    }

    @Test
    void loginShouldReturnTokensWhenPasswordIsWrong() {
        User user = User.builder().email("user@example.com").username("user").password("encoded-password").build();
        UserRequest request = UserRequest.builder().email("user@example.com").password("secret").build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(pwdEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(false);

        UserResponse response = userService.login(request);

        assertThat(response.getEmail()).isEqualTo(user.getEmail());
        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isNotBlank();
    }

    @Test
    void loginShouldThrowWhenCredentialsAreInvalid() {
        User user = User.builder().email("user@example.com").username("user").password("encoded-password").build();
        UserRequest request = UserRequest.builder().email("user@example.com").password("secret").build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(pwdEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(true);

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Password wrong");
    }

    @Test
    void logoutShouldPersistRevocationAndDeleteRefreshToken() throws JOSEException, ParseException {
        String accessToken = createAccessToken("jwt-1", Date.from(Instant.now().plus(5, ChronoUnit.MINUTES)));

        userService.logout(accessToken, "refresh-token");

        verify(redisTokenRepository).save(any(RedisToken.class));
        verify(invalidateTokenRepository).deleteById("refresh-token");
    }

    @Test
    void refreshTokenShouldReturnNewAccessTokenWhenRefreshTokenIsValid() throws ParseException, JOSEException {
        InvalidateToken token = InvalidateToken.builder()
                .id("refresh-token")
                .expiryTime(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)))
                .user(User.builder().email("user@example.com").username("user").build())
                .build();

        when(invalidateTokenRepository.findById("refresh-token")).thenReturn(Optional.of(token));

        UserResponse response = userService.refreshToken("refresh-token");

        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getUserName()).isEqualTo("user");
    }

    @Test
    void refreshTokenShouldThrowWhenRefreshTokenIsExpired() {
        InvalidateToken token = InvalidateToken.builder()
                .id("refresh-token")
                .expiryTime(Date.from(Instant.now().minus(1, ChronoUnit.DAYS)))
                .build();

        when(invalidateTokenRepository.findById("refresh-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> userService.refreshToken("refresh-token"))
                .isInstanceOf(SecurityException.class)
                .hasMessage("Token not valid");
    }

    @Test
    void verifyRefreshTokenShouldReturnTrueForValidToken() {
        InvalidateToken token = InvalidateToken.builder()
                .id("refresh-token")
                .expiryTime(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)))
                .build();

        when(invalidateTokenRepository.findById("refresh-token")).thenReturn(Optional.of(token));

        assertThat(userService.verifyRefreshToken("refresh-token")).isTrue();
    }

    @Test
    void verifyRefreshTokenShouldThrowWhenTokenExpired() {
        InvalidateToken token = InvalidateToken.builder()
                .id("refresh-token")
                .expiryTime(Date.from(Instant.now().minus(1, ChronoUnit.DAYS)))
                .build();

        when(invalidateTokenRepository.findById("refresh-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> userService.verifyRefreshToken("refresh-token"))
                .isInstanceOf(SecurityException.class)
                .hasMessage("Token not valid");
    }

    @Test
    void verifyTokenShouldReturnSignedJwtWhenTokenIsValid() throws JOSEException, ParseException {
        String token = createAccessToken("jwt-1", Date.from(Instant.now().plus(5, ChronoUnit.MINUTES)));
        when(redisTokenRepository.existsById("jwt-1")).thenReturn(false);

        SignedJWT signedJWT = userService.verifyToken(token);

        assertThat(signedJWT.getJWTClaimsSet().getJWTID()).isEqualTo("jwt-1");
    }

    @Test
    void verifyTokenShouldThrowWhenTokenWasRevoked() throws JOSEException {
        String token = createAccessToken("jwt-1", Date.from(Instant.now().plus(5, ChronoUnit.MINUTES)));
        when(redisTokenRepository.existsById("jwt-1")).thenReturn(true);

        assertThatThrownBy(() -> userService.verifyToken(token))
                .isInstanceOf(SecurityException.class)
                .hasMessage("Unauthenticated");
    }

    @Test
    void generateTokenShouldCreateAValidSignedToken() {
        User user = User.builder().email("user@example.com").build();

        String token = userService.generateToken(user);

        assertThat(token).isNotBlank();
    }

    private String createAccessToken(String jwtId, Date expiryTime) throws JOSEException {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .jwtID(jwtId)
                .expirationTime(expiryTime)
                .build();
        SignedJWT signedJWT = new SignedJWT(header, claims);
        signedJWT.sign(new MACSigner(userService.SIGNER_KEY));
        return signedJWT.serialize();
    }
}
