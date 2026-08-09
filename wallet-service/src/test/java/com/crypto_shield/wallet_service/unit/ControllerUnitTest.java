package com.crypto_shield.wallet_service.unit;

import com.crypto_shield.wallet_service.controller.PositionController;
import com.crypto_shield.wallet_service.controller.WalletController;
import com.crypto_shield.wallet_service.dto.request.ClosePositionRequest;
import com.crypto_shield.wallet_service.dto.request.OpenPositionRequest;
import com.crypto_shield.wallet_service.dto.response.CheckBalanceResponse;
import com.crypto_shield.wallet_service.dto.response.ClosePositionResponse;
import com.crypto_shield.wallet_service.dto.response.OpenPositionResponse;
import com.crypto_shield.wallet_service.dto.response.PositionResponse;
import com.crypto_shield.wallet_service.dto.response.WalletResponse;
import com.crypto_shield.wallet_service.enums.PositionStatus;
import com.crypto_shield.wallet_service.service.ClosePositionService;
import com.crypto_shield.wallet_service.service.PositionService;
import com.crypto_shield.wallet_service.service.WalletPositionService;
import com.crypto_shield.wallet_service.service.WalletService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Controller Unit Tests")
class ControllerUnitTest {

    @Test
    @DisplayName("Should delegate wallet endpoints to wallet service")
    void walletController_allEndpoints_wrapServiceResults() {
        // Given
        UUID userId = UUID.randomUUID();
        WalletService walletService = mock(WalletService.class);
        WalletController controller = new WalletController(walletService);
        WalletResponse walletResponse = WalletResponse.builder()
                .balance(new BigDecimal("1000"))
                .blockBalance(BigDecimal.ZERO)
                .build();
        CheckBalanceResponse balanceResponse = CheckBalanceResponse.builder()
                .success(true)
                .message("enough balance")
                .build();
        when(walletService.createWallet(userId)).thenReturn(walletResponse);
        when(walletService.getWalletByUser(userId)).thenReturn(walletResponse);
        when(walletService.checkBalance(userId, BigDecimal.TEN)).thenReturn(balanceResponse);

        // When
        var created = controller.createWallet(userId);
        var fetched = controller.getWalletByUserId(userId);
        var checked = controller.checkBalance(userId, BigDecimal.TEN);

        // Then
        assertThat(created.getResult()).isSameAs(walletResponse);
        assertThat(fetched.getResult()).isSameAs(walletResponse);
        assertThat(checked.getResult()).isSameAs(balanceResponse);
        verify(walletService).createWallet(userId);
        verify(walletService).getWalletByUser(userId);
        verify(walletService).checkBalance(userId, BigDecimal.TEN);
    }

    @Test
    @DisplayName("Should return ok when open position succeeds")
    void openPosition_successResponse_returnsOk() {
        // Given
        WalletPositionService walletPositionService = mock(WalletPositionService.class);
        PositionController controller = positionController(walletPositionService, mock(PositionService.class), mock(ClosePositionService.class));
        OpenPositionRequest request = new OpenPositionRequest();
        OpenPositionResponse response = OpenPositionResponse.builder().success(true).build();
        when(walletPositionService.openPosition(request)).thenReturn(response);

        // When
        var result = controller.openPosition(request);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(response);
    }

    @Test
    @DisplayName("Should return bad request when open position service returns unsuccessful response")
    void openPosition_unsuccessfulResponse_returnsBadRequest() {
        // Given
        WalletPositionService walletPositionService = mock(WalletPositionService.class);
        PositionController controller = positionController(walletPositionService, mock(PositionService.class), mock(ClosePositionService.class));
        OpenPositionRequest request = new OpenPositionRequest();
        OpenPositionResponse response = OpenPositionResponse.builder().success(false).build();
        when(walletPositionService.openPosition(request)).thenReturn(response);

        // When
        var result = controller.openPosition(request);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(result.getBody()).isSameAs(response);
    }

    @Test
    @DisplayName("Should return ok when close position succeeds")
    void closenPosition_successResponse_returnsOk() {
        // Given
        ClosePositionService closePositionService = mock(ClosePositionService.class);
        PositionController controller = positionController(mock(WalletPositionService.class), mock(PositionService.class), closePositionService);
        ClosePositionRequest request = ClosePositionRequest.builder().build();
        ClosePositionResponse response = ClosePositionResponse.builder().success(true).build();
        when(closePositionService.closePosition(request)).thenReturn(response);

        // When
        var result = controller.closenPosition(request);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(response);
    }

    @Test
    @DisplayName("Should return bad request when close position service returns unsuccessful response")
    void closenPosition_unsuccessfulResponse_returnsBadRequest() {
        // Given
        ClosePositionService closePositionService = mock(ClosePositionService.class);
        PositionController controller = positionController(mock(WalletPositionService.class), mock(PositionService.class), closePositionService);
        ClosePositionRequest request = ClosePositionRequest.builder().build();
        ClosePositionResponse response = ClosePositionResponse.builder().success(false).build();
        when(closePositionService.closePosition(request)).thenReturn(response);

        // When
        var result = controller.closenPosition(request);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(result.getBody()).isSameAs(response);
    }

    @Test
    @DisplayName("Should delegate stream and get by id endpoints")
    void positionController_readEndpoints_delegateToPositionService() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID positionId = UUID.randomUUID();
        PositionService positionService = mock(PositionService.class);
        PositionController controller = positionController(mock(WalletPositionService.class), positionService, mock(ClosePositionService.class));
        PositionResponse positionResponse = PositionResponse.builder()
                .positionId(positionId)
                .symbol("BTCUSDT")
                .status(PositionStatus.OPEN)
                .build();
        Flux<List<PositionResponse>> stream = Flux.just(List.of(positionResponse));
        when(positionService.streamPositions(userId)).thenReturn(stream);
        when(positionService.getPositionById(positionId)).thenReturn(positionResponse);

        // When
        var streamResult = controller.streamPositions(userId);
        var getResult = controller.getById(positionId);

        // Then
        assertThat(streamResult.blockFirst()).containsExactly(positionResponse);
        assertThat(getResult.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResult.getBody()).isSameAs(positionResponse);
    }

    private PositionController positionController(
            WalletPositionService walletPositionService,
            PositionService positionService,
            ClosePositionService closePositionService
    ) {
        return new PositionController(walletPositionService, positionService, closePositionService);
    }
}
