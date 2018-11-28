package com.jersay.waterbubble;

import android.content.Context;
import android.util.TypedValue;

/**
 * Created by Jersay on 2018/11/28.
 */
public class Utils {

    public static float dp2px(Context context, int value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, context.getResources().getDisplayMetrics());
    }
}
