package io.hhplus.tdd;

import io.hhplus.tdd.point.*;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(PointController.class)
public class PointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PointService pointService;

    private final long userId = 1L;
    private final long initialPoints = 100L;

    private UserPoint userPoint;
    private List<PointHistory> pointHistories;

    @BeforeEach
    void setUp() {
        userPoint = new UserPoint(userId, initialPoints, System.currentTimeMillis());
        pointHistories = List.of(
                new PointHistory(1, userId, 50, TransactionType.CHARGE, System.currentTimeMillis()),
                new PointHistory(2, userId, 20, TransactionType.USE, System.currentTimeMillis())
        );
    }

    @Test
    @DisplayName("특정 사용자 ID의 현재 포인트 반환 확인")
    void testGetPoint() throws Exception {
        Mockito.when(pointService.getId(userId)).thenReturn(userPoint);

        mockMvc.perform(get("/point/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(initialPoints));
    }

    @Test
    @DisplayName("요청된 금액이 사용자 포인트에 추가되었는지 확인")
    void testChargePoint() throws Exception {
        long chargeAmount = 50L;
        UserPoint updatedUserPoint = new UserPoint(userId, initialPoints + chargeAmount, System.currentTimeMillis());

        Mockito.when(pointService.chargePoint(eq(userId), eq(chargeAmount), any(Long.class)))
                .thenReturn(updatedUserPoint);

        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(chargeAmount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(updatedUserPoint.point()));
        System.out.println("충전완료 금액" + updatedUserPoint.point());
    }

    @Test
    @DisplayName("충전 시 최대 잔고 초과 확인")
    void testChargePoint_exceedsMaxBalance() throws Exception {
        long chargeAmount = 900_000L;

        Mockito.when(pointService.chargePoint(eq(userId), eq(chargeAmount), any(Long.class)))
                .thenThrow(new IllegalArgumentException("최대 잔고를 초과할 수 없습니다."));

        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(chargeAmount)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("최대 잔고를 초과할 수 없습니다."));
    }


    @Test
    @DisplayName("요청된 금액만큼 사용자 포인트가 감소하는지 확인")
    void testUsePoint() throws Exception {
        long useAmount = 30L;
        UserPoint updatedUserPoint = new UserPoint(userId, initialPoints - useAmount, System.currentTimeMillis());

        Mockito.when(pointService.usePoint(eq(userId), eq(useAmount), any(Long.class)))
                .thenReturn(updatedUserPoint);

        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(useAmount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(updatedUserPoint.point()));
    }

    @Test
    @DisplayName("잔고가 부족한 경우 400 에러와 오류 메시지 반환 확인")
    void testUsePoint_insufficientBalance() throws Exception {
        long useAmount = 150L;

        Mockito.when(pointService.usePoint(eq(userId), eq(useAmount), any(Long.class)))
                .thenThrow(new IllegalArgumentException("포인트가 부족합니다."));

        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(useAmount)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("포인트가 부족합니다."));
    }

    @Test
    @DisplayName("특정 사용자 ID의 포인트 거래 내역 반환 확인")
    void testGetPointHistories() throws Exception {
        Mockito.when(pointService.getPointHistory(userId)).thenReturn(pointHistories);

        mockMvc.perform(get("/point/{id}/histories", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(pointHistories.size()))
                .andExpect(jsonPath("$[0].userId").value(userId))
                .andExpect(jsonPath("$[0].amount").value(50));
    }


}
