package com.cryptoshield.cryptoshield.user.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse{
    String email;
    String userName;
    String accessToken;
    String refreshToken;
}
