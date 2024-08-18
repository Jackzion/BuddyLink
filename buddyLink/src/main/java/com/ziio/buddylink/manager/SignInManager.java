package com.ziio.buddylink.manager;

import com.ziio.buddylink.model.vo.SignInInfoVO;
import com.ziio.buddylink.utils.DateUtils;
import org.redisson.api.RBitSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 基于 bitMap 的 signInManager , 签到器
 */
@Component
public class SignInManager {
    @Resource
    private RedissonClient redissonClient;

    // 查看 今天签到否 （查看 bitmap 第 i 位)
    public boolean isSignIn(String key){
        int days = DateUtils.getGapFromFirstDay();
        RBitSet bitSet = redissonClient.getBitSet(key);
        return bitSet.get(days);
    }

    // 签到 （置 bitmap 第 i 位 为 1 -- true）
    public void signIn(String key) {
        RBitSet bitSet = redissonClient.getBitSet(key);
        int days = DateUtils.getGapFromFirstDay();
        bitSet.set(days, true);
    }

    // 查看签到信息
    public SignInInfoVO getSignInInfo(String key) {
        RBitSet bitSet = redissonClient.getBitSet(key);
        SignInInfoVO signInInfoVO = new SignInInfoVO();
        // 查看今天是否签到
        signInInfoVO.setIsSignedIn(this.isSignIn(key));
        // 查看签到次数
        signInInfoVO.setSignedInDayNum((int) bitSet.cardinality());
        // 查看签到日期
        List<Integer> signedInDateIndexList = new ArrayList<>();
        List<Date> signedInDateList = new ArrayList<>();
        LocalDate today = LocalDate.now();
        if (redissonClient.getKeys().countExists(key) > 0 && bitSet.length() > 0) {
            // 获取已签到的日期索引
            for (int i = 0; i < bitSet.length(); i++) {
                if (bitSet.get(i)) {
                    signedInDateIndexList.add(i);
                }
            }
            signedInDateList = signedInDateIndexList.stream().map(signedInDateIndex -> {
                // 计算与今天 and signedDate 差值 , 用 minus day 转换为 localDate
                LocalDate signedInLocalDate = today.minusDays(DateUtils.getGapFromFirstDay() - signedInDateIndex);
                // 转换为本地时区
                return Date.from(signedInLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
            }).collect(Collectors.toList());
        }
//        LocalDate date1 = LocalDate.of(2024, Month.AUGUST, 3);
//        signedInDateList.add(java.sql.Date.valueOf(date1));
//        LocalDate date2 = LocalDate.of(2024, Month.AUGUST, 8);
//        signedInDateList.add(java.sql.Date.valueOf(date2));
        System.out.println(signedInDateList);
        signInInfoVO.setSignedInDates(signedInDateList);
        return signInInfoVO;
    }
}
