package io.hhplus.tdd;


import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.PointService;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PointServiceTest {

    private PointService pointService;
    private UserPointTable userPointTable;
    private PointHistoryTable pointHistoryTable;

    private final long userId = 1L;
    private final long maxBalance = 1_000_000L;

    @BeforeEach
    void setUp() {
        userPointTable = mock(UserPointTable.class);
        pointHistoryTable = mock(PointHistoryTable.class);
        pointService = new PointService(userPointTable, pointHistoryTable);
    }

    @Test
    @DisplayName("포인트 충전 - 최대 잔고 초과 시 예외 발생")
    void testChargePoint_exceedMaxBalance() {
        //given
        long currentBalance = 950_000L;
        long chargeAmount = 100_000L;

        UserPoint userPoint = new UserPoint(userId, currentBalance, any(long.class));

        when(userPointTable.selectById(userId)).thenReturn(userPoint);

        // when then
        IllegalArgumentException exception =  assertThrows(IllegalArgumentException.class, () ->
                pointService.chargePoint(userId, chargeAmount, System.currentTimeMillis()));

        assertEquals("최대 잔고를 초과할 수 없습니다.", exception.getMessage());
        verify(userPointTable, times(1)).selectById(userId);
        verifyNoMoreInteractions(userPointTable,pointHistoryTable);

    }

    @Test
    @DisplayName("포인트 충전 - 정상 처리")
    void testChargePoint_success() {
        //given
        long currentBalance = 500_000L;
        long chargeAmount = 100_000L;

        UserPoint existingPoint = new UserPoint(userId, currentBalance, System.currentTimeMillis());
        UserPoint updatedPoint = new UserPoint(userId, currentBalance + chargeAmount, System.currentTimeMillis());

        when(userPointTable.selectById(userId)).thenReturn(existingPoint);
        when(userPointTable.insertOrUpdate(userId, currentBalance + chargeAmount)).thenReturn(updatedPoint);

        //when
        UserPoint result = pointService.chargePoint(userId, chargeAmount, System.currentTimeMillis());

        //then
        assertEquals(updatedPoint.point(), result.point());
        verify(userPointTable, times(1)).selectById(userId);
        verify(userPointTable, times(1)).insertOrUpdate(userId, currentBalance + chargeAmount);
        verify(pointHistoryTable, times(1)).insert(eq(userId), eq(chargeAmount), eq(TransactionType.CHARGE), anyLong());
    }

    @Test
    @DisplayName("포인트 사용 - 잔고 부족 시 예외 발생")
    void testUsePoint_insufficientBalance() {
        //given
        long currentBalance = 50_000L;
        long useAmount = 100_000L;

        UserPoint existingPoint = new UserPoint(userId, currentBalance, System.currentTimeMillis());

        when(userPointTable.selectById(userId)).thenReturn(existingPoint);

        //when then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                pointService.usePoint(userId, useAmount, System.currentTimeMillis()));

        assertEquals("포인트가 부족합니다.", exception.getMessage());
        verify(userPointTable, times(1)).selectById(userId);
        verifyNoMoreInteractions(userPointTable, pointHistoryTable);
    }

    @Test
    @DisplayName("존재하지 않는 사용자 ID에 대한 기본 포인트(0) 반환")
    void testGetPoint_failUser() {
        //given
        long failUserId = 999L;
        UserPoint defaultUserPoint = UserPoint.empty(failUserId);
        // Mock 설정: 존재하지 않는 사용자 ID에 대한 selectById 호출
        when(userPointTable.selectById(failUserId)).thenReturn(defaultUserPoint);

        //when
        UserPoint result = pointService.getId(failUserId);

        //then
        assertEquals(defaultUserPoint.id(), result.id());
        assertEquals(0, result.point());
        verify(userPointTable, times(1)).selectById(failUserId);
    }

    @Test
    @DisplayName("포인트 조회 - 정상 처리")
    void testGetPoint() {
        //given
        UserPoint userPoint = new UserPoint(userId, 500_000L, System.currentTimeMillis());

        when(userPointTable.selectById(userId)).thenReturn(userPoint);

        //when
        UserPoint result = pointService.getId(userId);

        //then
        assertEquals(userPoint, result);
        verify(userPointTable, times(1)).selectById(userId);
    }

    @Test
    @DisplayName("포인트 내역 조회")
    void testGetPointHistory() {
        //given
        List<PointHistory> histories = List.of(
                new PointHistory(1L, userId, 100_000L, TransactionType.CHARGE, System.currentTimeMillis()),
                new PointHistory(2L, userId, 20_000L, TransactionType.USE, System.currentTimeMillis())
        );
        when(pointHistoryTable.selectAllByUserId(userId)).thenReturn(histories);

        //when
        List<PointHistory> result = pointService.getPointHistory(userId);

        //then
        assertEquals(histories, result);
        verify(pointHistoryTable, times(1)).selectAllByUserId(userId);
    }

}
