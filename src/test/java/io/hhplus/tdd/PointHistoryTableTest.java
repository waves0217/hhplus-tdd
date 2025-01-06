package io.hhplus.tdd;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.point.PointHistory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import io.hhplus.tdd.point.TransactionType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PointHistoryTableTest {

    private final PointHistoryTable pointHistoryTable = new PointHistoryTable();

    @Test
    @DisplayName("사용자의 모든 포인트 거래 내역을 반환.")
    void testGetPointHistory() {
        // Given
        long userId = 1;
        pointHistoryTable.insert(userId, 10000, TransactionType.CHARGE, System.currentTimeMillis());
        pointHistoryTable.insert(userId, 4000, TransactionType.USE, System.currentTimeMillis());
        long otherUserId = 2;
        pointHistoryTable.insert(otherUserId, 500, TransactionType.CHARGE, System.currentTimeMillis());

        // When
        List<PointHistory> histories = pointHistoryTable.selectAllByUserId(userId);

        // Then
        assertNotNull(histories);
        assertEquals(2, histories.size());
        assertTrue(histories.stream().allMatch(history -> history.userId() == userId));
    }

    @Test
    @DisplayName("사용자가 거래 내역이 없을 경우 빈 리스트를 반환.")
    void testFailPointHistory() {
        // Given
        long failUserId = 999L;

        // When
        List<PointHistory> histories = pointHistoryTable.selectAllByUserId(failUserId);

        // Then
        assertNotNull(histories);
        assertTrue(histories.isEmpty());
    }
}
