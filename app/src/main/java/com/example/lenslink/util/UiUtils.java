package com.example.lenslink.util;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import android.text.Editable;
import android.text.TextWatcher;

import android.content.Context;

import com.example.lenslink.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicatorSpec;
import com.google.android.material.progressindicator.IndeterminateDrawable;

public final class UiUtils {

    private UiUtils() {
    }

    public static void enableEdgeToEdge(AppCompatActivity activity) {
        SystemBarStyle lightBars = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT);
        EdgeToEdge.enable(activity, lightBars, lightBars);
    }

    public static void applySystemBarInsets(View root) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }

    public static boolean applyFieldError(TextInputLayout layout, int errorResId) {
        if (errorResId == ValidationUtils.VALID) {
            layout.setError(null);
            return true;
        }
        layout.setError(layout.getContext().getString(errorResId));
        return false;
    }

    public static void clearErrorOnEdit(TextInputEditText editText, TextInputLayout layout) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                layout.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    public static void hideKeyboard(Activity activity) {
        View view = activity.getCurrentFocus();
        if (view == null) view = activity.getWindow().getDecorView();
        InputMethodManager imm =
                (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public static String textOf(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    public static String rawTextOf(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString();
    }

    public static void setButtonLoading(MaterialButton button, boolean loading) {
        button.setClickable(!loading);

        if (!loading) {
            button.setIcon(null);
            return;
        }

        Context context = button.getContext();
        CircularProgressIndicatorSpec spec = new CircularProgressIndicatorSpec(
                context, null, 0, R.style.LensLink_Progress_Button);
        IndeterminateDrawable<CircularProgressIndicatorSpec> spinner =
                IndeterminateDrawable.createCircularDrawable(context, spec);

        button.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
        button.setIconPadding(context.getResources()
                .getDimensionPixelSize(R.dimen.lenslink_button_icon_padding));
        button.setIcon(spinner);
    }
}