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

public class SynchronizedTest {

    @Test
    @DisplayName("한 사용자가 여러 번의 동시 요청을 보냈을 때, 데이터가 일관성을 보장하는지 테스트")
    void testSynchronizedConcurrency() throws InterruptedException {
        //given
        long userId = 1L;
        PointService pointService = new PointService(new UserPointTable(), new PointHistoryTable());

        //사용자 초기 포인트 설정
        pointService.chargePoint(userId, 1_000L, System.currentTimeMillis());
        System.out.println("초기 포인트: " + pointService.getId(userId).point());

        //스레드 풀 및 CountDownLatch 설정
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(threadCount);

        //when
        for (int i = 0; i < threadCount; i++) {
            int threadNum = i;
            executorService.submit(() -> {
                long requestTime = System.currentTimeMillis();
                try {
                    if (threadNum % 2 == 0) {
                        pointService.chargePoint(userId, 10L, System.currentTimeMillis());
                        System.out.println("스레드 " + threadNum + ": 10 포인트 충전");
                    } else {
                        pointService.usePoint(userId, 10L, System.currentTimeMillis());
                        System.out.println("스레드 " + threadNum + ": 10 포인트 사용");
                    }
                } finally {
                    long finishTime = System.currentTimeMillis(); // 요청 처리 완료 시간
                    System.out.println("요청 " + threadNum + ": 처리 시간 = " + (finishTime - requestTime) + "ms");
                    latch.countDown();
                }
            });
        }

        //모든 작업이 끝날 때까지 대기
        latch.await();
        executorService.shutdown();

        //then
        // 초기 1,000 포인트 + 50번 충전(500) - 50번 사용(500) = 1,000
        UserPoint userPoint = pointService.getId(userId);
        System.out.println("최종 포인트: " + userPoint.point());

        assertEquals(1_000L, userPoint.point());
    }

    @Test
    @DisplayName("동기화 없이 동시 요청 시 데이터 불일치 확인")
    void testConcurrencyWithoutSynchronization() throws InterruptedException {
        //given
        long userId = 1L;
        PointService pointService = new PointService(new UserPointTable(), new PointHistoryTable());
        pointService.chargePoint(userId, 1_000L, System.currentTimeMillis());
        System.out.println("초기 포인트: " + pointService.getId(userId).point());

        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(threadCount);

        //when
        for (int i = 0; i < threadCount; i++) {
            int finalI = i;
            executorService.submit(() -> {
                try {
                    if (finalI % 2 == 0) {
                        pointService.chargePoint(userId, 10L, System.currentTimeMillis());
                    } else {
                        pointService.usePoint(userId, 10L, System.currentTimeMillis());
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        //then
        UserPoint userPoint = pointService.getId(userId);
        System.out.println("최종 포인트: " + userPoint.point());
    }

}
