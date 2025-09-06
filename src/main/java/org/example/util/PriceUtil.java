package org.example.util;

import java.text.NumberFormat;
import java.util.Locale;

public class PriceUtil {
    private static final NumberFormat NF = NumberFormat.getInstance(Locale.KOREA);
    public static String format(int price) {
        return NF.format(price);
    }
}
