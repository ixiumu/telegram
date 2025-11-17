

package top.qwq2333.nullgram.utils;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import kotlin.jvm.functions.Function0;

public class Assertions {

    private Assertions() {
    }

    public static void check(boolean condition) {
        check(condition, (String) null);
    }

    public static void check(boolean condition, @Nullable String msg) {
        if (!condition) {
            if (TextUtils.isEmpty(msg)) {
                msg = "check failed";
            }
            throw new AssertionError(msg);
        }
    }

    public static void check(boolean condition, @NonNull Function0<String> expr) {
        if (!condition) {
            String msg = expr.invoke();
            throw new AssertionError(msg);
        }
    }

}
