package com.example.lenslink.utils;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.example.lenslink.R;
import com.example.lenslink.models.Message;
import java.util.Date;
import java.util.Locale;

public final class UiUtils {
    private UiUtils() {}

    public static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics()));
    }

    public static String money(double v) {
        return "R" + String.format(Locale.US, "%,.0f", v).replace(',', ' ');
    }

    public static String initial(String name) {
        if (name == null || name.trim().isEmpty()) return "?";
        return name.trim().substring(0, 1).toUpperCase(Locale.getDefault());
    }

    public static String categoryFor(String serviceTitle) {
        String t = serviceTitle == null ? "" : serviceTitle.toLowerCase(Locale.ROOT);
        if (t.contains("wedding")) return "WEDDING";
        if (t.contains("graduation")) return "GRADUATION";
        return "PORTRAIT";
    }

    /** Coloured pill for Pending / Confirmed / Completed / Cancelled. */
    public static void styleStatus(TextView tv, String status) {
        Context c = tv.getContext();
        int fg, bg;
        if (FirestoreConstants.STATUS_CONFIRMED.equalsIgnoreCase(status)) {
            fg = R.color.status_confirmed; bg = R.color.status_confirmed_bg;
        } else if (FirestoreConstants.STATUS_CANCELLED.equalsIgnoreCase(status)) {
            fg = R.color.status_cancelled; bg = R.color.status_cancelled_bg;
        } else if (FirestoreConstants.STATUS_COMPLETED.equalsIgnoreCase(status)) {
            fg = R.color.status_completed; bg = R.color.status_completed_bg;
        } else {
            fg = R.color.status_pending; bg = R.color.status_pending_bg;
        }
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(dp(c, 12));
        d.setColor(ContextCompat.getColor(c, bg));
        tv.setBackground(d);
        tv.setTextColor(ContextCompat.getColor(c, fg));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setPadding(dp(c, 10), dp(c, 3), dp(c, 10), dp(c, 3));
        tv.setText(status);
    }

    public static TextView text(Context c, String s, int styleSp, int colorRes, boolean bold) {
        TextView tv = new TextView(c);
        tv.setText(s);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, styleSp);
        tv.setTextColor(ContextCompat.getColor(c, colorRes));
        if (bold) tv.setTypeface(null, android.graphics.Typeface.BOLD);
        return tv;
    }

    public static LinearLayout card(Context c) {
        LinearLayout card = new LinearLayout(c);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card_white);
        int p = dp(c, 18);
        card.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(c, 12));
        card.setLayoutParams(lp);
        return card;
    }

    /** Empty-state line that tells people what to do next. */
    public static TextView emptyState(Context c, String message) {
        TextView tv = text(c, message, 14, R.color.text_secondary, false);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(c, 24), dp(c, 40), dp(c, 24), dp(c, 40));
        return tv;
    }

    /** Chat bubble used by both roles. Own messages sit right in dark, others left in white. */
    public static void addBubble(LinearLayout container, ScrollView scroller, String body, String caption, boolean mine) {
        Context c = container.getContext();
        LinearLayout wrap = new LinearLayout(c);
        wrap.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        wlp.setMargins(0, dp(c, 4), 0, dp(c, 4));
        wrap.setLayoutParams(wlp);

        TextView tv = new TextView(c);
        tv.setText(body);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tv.setLineSpacing(0, 1.12f);
        tv.setBackgroundResource(mine ? R.drawable.bg_bubble_mine : R.drawable.bg_bubble_other);
        tv.setTextColor(ContextCompat.getColor(c, mine ? R.color.surface_white : R.color.text_primary));
        tv.setPadding(dp(c, 16), dp(c, 12), dp(c, 16), dp(c, 12));
        tv.setMaxWidth((int) (c.getResources().getDisplayMetrics().widthPixels * 0.78f));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = mine ? Gravity.END : Gravity.START;
        tv.setLayoutParams(lp);
        wrap.addView(tv);

        if (caption != null && !caption.isEmpty()) {
            TextView cap = text(c, caption, 11, R.color.text_secondary, false);
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            clp.gravity = mine ? Gravity.END : Gravity.START;
            clp.setMargins(dp(c, 6), dp(c, 3), dp(c, 6), 0);
            cap.setLayoutParams(clp);
            wrap.addView(cap);
        }
        container.addView(wrap);
        if (scroller != null) scroller.post(() -> scroller.fullScroll(View.FOCUS_DOWN));
    }

    public static String timeCaption(Context c, Message m, String who) {
        String t = DateFormat.getTimeFormat(c).format(new Date(m.getTimestamp()));
        return who + " \u2022 " + t;
    }

    public static void strike(TextView tv, boolean on) {
        if (on) tv.setPaintFlags(tv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        else tv.setPaintFlags(tv.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
    }
}
