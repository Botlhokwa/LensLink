package com.example.lenslink.util;

import static org.junit.Assert.assertEquals;

import com.example.lenslink.R;

import org.junit.Test;

public class ValidationUtilsTest {

    // ---- Full name ----
    @Test
    public void fullName_blank_isRequired() {
        assertEquals(R.string.error_name_required, ValidationUtils.validateFullName("   "));
    }

    @Test
    public void fullName_oneCharacter_isTooShort() {
        assertEquals(R.string.error_name_too_short, ValidationUtils.validateFullName("A"));
    }

    @Test
    public void fullName_over60Characters_isTooLong() {
        assertEquals(R.string.error_name_too_long,
                ValidationUtils.validateFullName("A".repeat(61)));
    }

    @Test
    public void fullName_normalName_isValid() {
        assertEquals(ValidationUtils.VALID, ValidationUtils.validateFullName("Bee Lemme"));
    }

    // ---- Phone (optional) ----
    @Test
    public void phone_blank_isValid() {
        assertEquals(ValidationUtils.VALID, ValidationUtils.validatePhone(""));
    }

    @Test
    public void phone_localAndInternationalFormats_areValid() {
        assertEquals(ValidationUtils.VALID, ValidationUtils.validatePhone("0821234567"));
        assertEquals(ValidationUtils.VALID, ValidationUtils.validatePhone("082-123-4567"));
        assertEquals(ValidationUtils.VALID, ValidationUtils.validatePhone("+27 82 123 4567"));
    }

    @Test
    public void phone_tooShort_isInvalid() {
        assertEquals(R.string.error_phone_invalid, ValidationUtils.validatePhone("123"));
    }

    @Test
    public void phone_over20Characters_isInvalid() {
        assertEquals(R.string.error_phone_invalid,
                ValidationUtils.validatePhone("0".repeat(21)));
    }

    // ---- Password policy ----
    @Test
    public void password_empty_isRequired() {
        assertEquals(R.string.error_password_required, ValidationUtils.validatePassword(""));
    }

    @Test
    public void password_underEightCharacters_isTooShort() {
        assertEquals(R.string.error_password_too_short, ValidationUtils.validatePassword("Ab1"));
    }

    @Test
    public void password_missingRequiredCharacterTypes_isWeak() {
        assertEquals(R.string.error_password_weak, ValidationUtils.validatePassword("alllowercase1"));
        assertEquals(R.string.error_password_weak, ValidationUtils.validatePassword("ALLUPPER123"));
        assertEquals(R.string.error_password_weak, ValidationUtils.validatePassword("NoDigitsHere"));
    }

    @Test
    public void password_meetingPolicy_isValid() {
        assertEquals(ValidationUtils.VALID, ValidationUtils.validatePassword("GoodPass1"));
    }

    // ---- Confirm password ----
    @Test
    public void confirmPassword_empty_isRequired() {
        assertEquals(R.string.error_confirm_required,
                ValidationUtils.validateConfirmPassword("GoodPass1", ""));
    }

    @Test
    public void confirmPassword_mismatch_isRejected() {
        assertEquals(R.string.error_passwords_mismatch,
                ValidationUtils.validateConfirmPassword("GoodPass1", "GoodPass2"));
    }

    @Test
    public void confirmPassword_match_isValid() {
        assertEquals(ValidationUtils.VALID,
                ValidationUtils.validateConfirmPassword("GoodPass1", "GoodPass1"));
    }

    // ---- Login password (policy is NOT enforced at login) ----
    @Test
    public void loginPassword_empty_isRequired() {
        assertEquals(R.string.error_password_required, ValidationUtils.validateLoginPassword(""));
    }

    @Test
    public void loginPassword_anyNonEmptyValue_isValid() {
        assertEquals(ValidationUtils.VALID, ValidationUtils.validateLoginPassword("x"));
    }
}