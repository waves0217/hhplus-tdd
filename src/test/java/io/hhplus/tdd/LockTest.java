package io.hhplus.tdd;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointService;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LockTest {

    @Test
    @DisplayName("한 사용자가 여러 번의 동시 요청을 보냈을 때, 데이터가 일관성을 보장하는지 테스트")
    void testLockConcurrency() throws InterruptedException {
        //given
        long userId = 1L;
        PointService pointService = new PointService(new UserPointTable(), new PointHistoryTable());

        //초기 포인트 설정
        pointService.chargePoint(userId, 1_000L, System.currentTimeMillis());

        //스레드 풀 CountDownLatch 설정
        int threadCount = 20;
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(threadCount);

        //when
        for (int i = 0; i < threadCount; i++) {
            int threadNum = i;
            executorService.submit(() -> {
                try {
                    if (threadNum % 2 == 0) {
                        pointService.chargePoint(userId, 10L, System.currentTimeMillis());
                    } else {
                        pointService.usePoint(userId, 10L, System.currentTimeMillis());
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // 모든 작업이 끝날 때까지 대기
        latch.await();
        executorService.shutdown();

        //then
        UserPoint userPoint = pointService.getId(userId);
        System.out.println("최종 포인트: " + userPoint.point());
        assertEquals(1_000L, userPoint.point());
    }
}
