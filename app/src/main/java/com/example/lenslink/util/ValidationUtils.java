package com.example.lenslink.util;

import android.util.Patterns;

import androidx.annotation.StringRes;

import com.example.lenslink.R;

import java.util.regex.Pattern;

public final class ValidationUtils {

    public static final int VALID = 0;

    private static final int MIN_NAME_LENGTH = 2;
    private static final int MIN_PASSWORD_LENGTH = 8;

    // Optional leading +, then 9 to 15 digits (spaces/dashes are stripped first).
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9]{9,15}$");

    private ValidationUtils() {
    }

    @StringRes
    public static int validateFullName(String fullName) {
        if (isBlank(fullName)) return R.string.error_name_required;
        if (fullName.trim().length() < MIN_NAME_LENGTH) return R.string.error_name_too_short;
        return VALID;
    }

    @StringRes
    public static int validateEmail(String email) {
        if (isBlank(email)) return R.string.error_email_required;
        if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            return R.string.error_email_invalid;
        }
        return VALID;
    }

    @StringRes
    public static int validatePhone(String phone) {
        if (isBlank(phone)) return VALID;
        String digitsOnly = phone.trim().replaceAll("[\\s-]", "");
        if (!PHONE_PATTERN.matcher(digitsOnly).matches()) return R.string.error_phone_invalid;
        return VALID;
    }

    @StringRes
    public static int validatePassword(String password) {
        if (password == null || password.isEmpty()) return R.string.error_password_required;
        if (password.length() < MIN_PASSWORD_LENGTH) return R.string.error_password_too_short;

        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
        }
        if (!(hasUpper && hasLower && hasDigit)) return R.string.error_password_weak;
        return VALID;
    }

    @StringRes
    public static int validateLoginPassword(String password) {
        return (password == null || password.isEmpty())
                ? R.string.error_password_required
                : VALID;
    }

    @StringRes
    public static int validateConfirmPassword(String password, String confirmPassword) {
        if (confirmPassword == null || confirmPassword.isEmpty()) {
            return R.string.error_confirm_required;
        }
        if (!confirmPassword.equals(password)) return R.string.error_passwords_mismatch;
        return VALID;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}