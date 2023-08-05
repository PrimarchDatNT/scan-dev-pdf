package com.document.camerascanner.ads;

import android.annotation.SuppressLint;
import android.content.Context;
import android.provider.Settings;

import androidx.annotation.NonNull;

import com.document.camerascanner.BuildConfig;

import org.jetbrains.annotations.Contract;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdsUtil {

    private static final char[] DIGITS_LOWER = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    @NonNull
    public static String getTestDevice(Context context) {
        if (!BuildConfig.DEBUG) {
            return "";
        }

        try {
            @SuppressLint("HardwareIds") String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            return encodeMd5(androidId).toUpperCase(Locale.US);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String encodeMd5(String input) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] data = messageDigest.digest(input.getBytes(StandardCharsets.UTF_8));
            return md5Hex(data);
        } catch (Exception e) {
            return input;
        }
    }

    @NonNull
    @Contract("_ -> new")
    private static String md5Hex(byte[] data) {
        return new String(encodeHex(data));
    }

    @NonNull
    @Contract(pure = true)
    private static char[] encodeHex(@NonNull final byte[] data) {
        final int l = data.length;
        final char[] out = new char[l << 1];
        for (int i = 0, j = 0; i < l; i++) {
            out[j++] = DIGITS_LOWER[(0xF0 & data[i]) >>> 4];
            out[j++] = DIGITS_LOWER[0x0F & data[i]];
        }
        return out;
    }

    @NonNull
    @Contract("_ -> new")
    public static List<String> getTestDevices(Context context) {
        return new ArrayList<String>() {
            {
                this.add(getTestDevice(context));
            }
        };
    }
}
