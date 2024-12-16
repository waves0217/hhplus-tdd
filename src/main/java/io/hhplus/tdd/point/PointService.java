package io.hhplus.tdd.point;

import io.hhplus.tdd.ErrorResponse;
import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import lombok.RequiredArgsConstructor;
import org.apache.catalina.User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PointService {
    private static final long maxBalnce = 1_000_000;

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    public UserPoint getId(long id) {
        return userPointTable.selectById(id);
    }

    public List<PointHistory> getPointHistory(long id) {
        return pointHistoryTable.selectAllByUserId(id);
    }

    public UserPoint chargePoint(long id, long amount, long chargeDate) {
        UserPoint userPoint = userPointTable.selectById(id);
        if(userPoint.point() + amount > maxBalnce) {
            throw new IllegalArgumentException("최대 잔고를 초과할 수 없습니다.");
        }

        UserPoint chargedUserPoint = userPointTable.insertOrUpdate(id, userPoint.point() + amount);

        pointHistoryTable.insert(id,amount,TransactionType.CHARGE,chargeDate);

        return chargedUserPoint;
    }

    public UserPoint usePoint(long id, long amount, long useDate) {
        UserPoint userPoint = userPointTable.selectById(id);

        if(userPoint.point() < amount){
            throw new IllegalArgumentException("포인트가 부족합니다.");
        }

        UserPoint useUserPoint = userPointTable.insertOrUpdate(id, userPoint.point() - amount);
        pointHistoryTable.insert(id,amount,TransactionType.USE,useDate);
        return useUserPoint;
    }

}