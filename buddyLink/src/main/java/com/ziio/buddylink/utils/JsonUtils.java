package com.ziio.buddylink.utils;

import java.util.List;

public class JsonUtils {
    public static String ListToJson(List<String> list) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append('[');
        for (int i = 0; i < list.size(); i++) {
            stringBuffer.append('"').append(list.get(i)).append('"');
            if (i < list.size() - 1) {
                stringBuffer.append(',');
            }
        }
        return stringBuffer.append(']').toString();
    }
}
