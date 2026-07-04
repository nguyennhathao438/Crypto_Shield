package com.cryptoshield.cryptoshield.user.service;

import com.cryptoshield.cryptoshield.user.dto.RedisToken;
import com.cryptoshield.cryptoshield.user.dto.UserRequest;
import com.cryptoshield.cryptoshield.user.dto.UserResponse;
import com.cryptoshield.cryptoshield.user.entity.InvalidateToken;
import com.cryptoshield.cryptoshield.user.entity.User;
import com.cryptoshield.cryptoshield.user.repository.InvalidateTokenRepository;
import com.cryptoshield.cryptoshield.user.repository.RedisTokenRepository;
import com.cryptoshield.cryptoshield.user.repository.UserRepository;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
@Slf4j
public class UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private InvalidateTokenRepository invalidateTokenRepository;
    @Autowired
    private RedisTokenRepository redisTokenRepository;
    @Autowired
    PasswordEncoder pwdEncoder;
    @Value("${jwt.signerKey}")
    protected String SIGNER_KEY;
    public UserResponse registerUser(UserRequest request){
        if(userRepository.existsByEmail(request.getEmail())){
            throw new IllegalArgumentException("Email existed");
        }
        User user = User.builder()
                .email(request.getEmail())
                .password(pwdEncoder.encode(request.getPassword()))
                .username(request.getUsername())
                .build();
        userRepository.save(user);
        return UserResponse.builder()
                .email(user.getEmail())
                .accessToken(generateToken(user))
                .refreshToken(generateRefreshToken(user))
                .build();
    }
    public UserResponse login(UserRequest request){
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow(()-> new IllegalArgumentException("User does not exist"));
        if(pwdEncoder.matches(request.getPassword(), user.getPassword())){
            throw new IllegalArgumentException("Password wrong");
        }
        String token = generateToken(user);
        return UserResponse.builder()
                .email(user.getEmail())
                .userName(user.getUsername())
                .accessToken(generateToken(user))
                .refreshToken(generateRefreshToken(user))
                .build();
    }
    public void logout(String accessToken,String refreshToken){
        String uuid = "";
        Date expiryTime;
        try {
            SignedJWT signedJWT = SignedJWT.parse(accessToken);
            uuid = signedJWT.getJWTClaimsSet().getJWTID().toString();
            expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();
        } catch (ParseException e) {
            log.warn("parse token failed");
            throw new RuntimeException(e);
        }
        long ttl = (expiryTime.getTime() - System.currentTimeMillis()) / 1000;
        if (ttl > 0) {
            redisTokenRepository.save(
                    RedisToken.builder()
                            .jwtId(uuid)
                            .expiredTime(ttl)
                            .build()
            );
        }
        invalidateTokenRepository.deleteById(refreshToken);
    }
    //Token
    public UserResponse refreshToken(String refreshToken) throws ParseException, JOSEException {
        InvalidateToken token = invalidateTokenRepository
                .findById(refreshToken)
                .orElseThrow(() ->
                        new SecurityException("Unauthenticated"));

        if (token.getExpiryTime().before(new Date())) {
            throw new SecurityException("Token not valid");
        }

        User user = token.getUser();

        String accessToken = generateToken(user);
        return UserResponse.builder()
                .accessToken(accessToken)
                .userName(user.getUsername())
                .build();
    }
    public boolean verifyRefreshToken(String refreshToken){
        InvalidateToken invalidateToken= invalidateTokenRepository.findById(refreshToken).orElseThrow(() -> new SecurityException("Unauthenticated"));
        if(!invalidateToken.getExpiryTime().after(new Date()))
            throw new SecurityException("Token not valid");
        return true;
    }
    public SignedJWT verifyToken(String token) throws JOSEException, ParseException {

        JWSVerifier verifier =  new MACVerifier(SIGNER_KEY.getBytes());

        SignedJWT signedJWT = SignedJWT.parse(token);
        Date expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();

        boolean verified = signedJWT.verify(verifier);
        if (!verified)
            throw new SecurityException("Unauthenticated");
        if (!expiryTime.after(new Date()))
            throw new SecurityException("Token not valid");
        if (redisTokenRepository.existsById(signedJWT.getJWTClaimsSet().getJWTID()))
            throw new SecurityException("Unauthenticated");
        return signedJWT;
    }

    public String generateToken(User user){
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS256); // Định nghĩa thuật toán trong Header
        JWTClaimsSet jwtClaimSet = new JWTClaimsSet.Builder()
                .subject(user.getEmail())// email người dùng
                .issuer("cryptoshield.com")// ai phát hành ??
                .issueTime(new Date())// thời gian phát hành
                .expirationTime(Date.from(Instant.now().plus(15, ChronoUnit.MINUTES)))// thời gian hết hạn
                .build();
        Payload payload = new Payload(jwtClaimSet.toJSONObject());// Đóng gói payload vào jwsobject
        JWSObject jwsObject = new JWSObject(header, payload);
        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY));// Ký JWT bằng khóa bí mật
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.warn("Error generate token");
            throw new RuntimeException(e);
        }
    }
    private String generateRefreshToken(User user) {
        String token = UUID.randomUUID().toString();
        InvalidateToken invalidateToken =InvalidateToken.builder()
                .id(token)
                .expiryTime(Date.from(Instant.now().plus(7, ChronoUnit.DAYS)))
        .build();

        invalidateTokenRepository.save(invalidateToken);
        return token;
    }
}
