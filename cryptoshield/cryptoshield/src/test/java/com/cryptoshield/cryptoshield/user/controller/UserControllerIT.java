package com.cryptoshield.cryptoshield.user.controller;

import com.cryptoshield.cryptoshield.user.dto.UserRequest;
import com.cryptoshield.cryptoshield.user.dto.UserResponse;
import com.cryptoshield.cryptoshield.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@WithMockUser
class UserControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void loginEndpointShouldReturnOkWithUserPayload() throws Exception {
        UserResponse response = UserResponse.builder()
                .email("user@example.com")
                .userName("user")
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .build();

        when(userService.login(any(UserRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@example.com\",\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.result.email").value("user@example.com"))
                .andExpect(jsonPath("$.result.userName").value("user"));
    }

    @Test
    void logoutEndpointShouldReturnOkAndClearCookie() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", "Bearer access-token")
                .cookie(new jakarta.servlet.http.Cookie("refreshToken", "refresh-token")))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("refreshToken=;")));
    }

    @Test
    void registerEndpointShouldReturnOkWithRegisteredUser() throws Exception {
        UserResponse response = UserResponse.builder()
                .email("new@example.com")
                .userName("new")
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .build();

        when(userService.registerUser(any(UserRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"new@example.com\",\"password\":\"secret\",\"username\":\"new\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.result.email").value("new@example.com"));
    }
}
