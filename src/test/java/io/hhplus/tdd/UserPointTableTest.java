package io.hhplus.tdd;

import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UserPointTableTest {

    private UserPointTable userPointTable;

    @BeforeEach
    void setUp() {
        userPointTable = new UserPointTable();
    }

    @Test
    @DisplayName("사용자 조회 시, 포인트 정보 확인")
    void testGetUser(){
        //given
        long userId = 1;
        long initPoints = 10000;
        userPointTable.insertOrUpdate(userId, initPoints);

        //when
        UserPoint userPoint = userPointTable.selectById(userId);

        //then
        assertNotNull(userPoint);
        assertEquals(userId,userPoint.id());
        assertEquals(initPoints,userPoint.point());
    }

    @Test
    @DisplayName("존재하지 않는 사용자 조회 시, 기본값 확인")
    void testFailGetUser() {
        //given
        long failUserId = 22;

        //when
        UserPoint userPoint = userPointTable.selectById(failUserId);

        //then
        assertNotNull(userPoint);
        assertEquals(failUserId,userPoint.id());
        assertEquals(0,userPoint.point());
    }



}
