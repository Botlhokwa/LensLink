package com.example.lenslink.utils;

import android.app.Activity;
import android.view.View;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

/** Edge-to-edge handling so content no longer slides under the status bar / gesture bar / keyboard. */
public final class SystemBars {
    private SystemBars() {}

    public static void apply(Activity activity, View root, View bottomBar) {
        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);
        WindowCompat.getInsetsController(activity.getWindow(), root).setAppearanceLightStatusBars(true);
        WindowCompat.getInsetsController(activity.getWindow(), root).setAppearanceLightNavigationBars(true);

        final int baseBarBottom = bottomBar == null ? 0 : bottomBar.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            boolean imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime()) && ime.bottom > 0;

            if (bottomBar != null) {
                bottomBar.setVisibility(imeVisible ? View.GONE : View.VISIBLE);
                bottomBar.setPadding(bottomBar.getPaddingLeft(), bottomBar.getPaddingTop(),
                    bottomBar.getPaddingRight(), baseBarBottom + bars.bottom);
                v.setPadding(bars.left, bars.top, bars.right, imeVisible ? ime.bottom : 0);
            } else {
                v.setPadding(bars.left, bars.top, bars.right, imeVisible ? ime.bottom : bars.bottom);
            }
            return WindowInsetsCompat.CONSUMED;
        });
        ViewCompat.requestApplyInsets(root);
    }
}
