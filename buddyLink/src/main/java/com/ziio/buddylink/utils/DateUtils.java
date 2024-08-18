package com.ziio.buddylink.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class DateUtils {

    /**
     * 获取 今天日期 和 第一天（一月一日） 的相差天数
     * @return
     */
    public static int getGapFromFirstDay(){
        LocalDate today = LocalDate.now();
        LocalDate firstDayOfYear = LocalDate.of(today.getYear(), 1, 1);
        long days = ChronoUnit.DAYS.between(firstDayOfYear, today);
        return (int)days;
    }

    public static int getNowYear() {
        return LocalDate.now().getYear();
    }
}
