package com.example.lenslink.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.core.content.ContextCompat;
import com.example.lenslink.R;
import com.example.lenslink.utils.UiUtils;
import java.util.ArrayList;
import java.util.List;

/** Icon + label bottom navigation. Each role supplies its own items, which is how RBAC shapes the nav. */
public class BottomNavBar extends LinearLayout {

    public static class Item {
        final int id; final int icon; final String label;
        public Item(@IdRes int id, @DrawableRes int icon, String label) { this.id = id; this.icon = icon; this.label = label; }
    }

    public interface OnItemSelected { void onSelected(int id); }

    private final List<Item> items = new ArrayList<>();
    private final List<View> cells = new ArrayList<>();
    private OnItemSelected listener;

    public BottomNavBar(Context c) { this(c, null); }

    public BottomNavBar(Context c, AttributeSet attrs) {
        super(c, attrs);
        setOrientation(HORIZONTAL);
        setBackgroundResource(R.drawable.bg_nav);
        setPadding(0, UiUtils.dp(c, 8), 0, UiUtils.dp(c, 6));
    }

    public void setItems(List<Item> newItems) {
        removeAllViews();
        items.clear(); cells.clear();
        items.addAll(newItems);
        Context c = getContext();
        for (Item item : items) {
            LinearLayout cell = new LinearLayout(c);
            cell.setOrientation(VERTICAL);
            cell.setGravity(Gravity.CENTER_HORIZONTAL);
            LayoutParams lp = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
            cell.setLayoutParams(lp);

            FrameLayout pill = new FrameLayout(c);
            pill.setId(View.generateViewId());
            pill.setLayoutParams(new LayoutParams(UiUtils.dp(c, 56), UiUtils.dp(c, 30)));
            ImageView icon = new ImageView(c);
            icon.setImageResource(item.icon);
            FrameLayout.LayoutParams ilp = new FrameLayout.LayoutParams(UiUtils.dp(c, 22), UiUtils.dp(c, 22), Gravity.CENTER);
            icon.setLayoutParams(ilp);
            pill.addView(icon);

            TextView label = new TextView(c);
            label.setText(item.label);
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            label.setGravity(Gravity.CENTER);
            label.setSingleLine(true);
            LayoutParams llp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            llp.topMargin = UiUtils.dp(c, 3);
            label.setLayoutParams(llp);

            cell.addView(pill);
            cell.addView(label);
            cell.setContentDescription(item.label);
            final int id = item.id;
            cell.setOnClickListener(v -> { select(id); if (listener != null) listener.onSelected(id); });
            addView(cell);
            cells.add(cell);
        }
    }

    public void setOnItemSelectedListener(OnItemSelected l) { this.listener = l; }

    /** Highlights an item without firing the listener. */
    public void select(@IdRes int id) {
        Context c = getContext();
        for (int i = 0; i < items.size(); i++) {
            boolean on = items.get(i).id == id;
            LinearLayout cell = (LinearLayout) cells.get(i);
            FrameLayout pill = (FrameLayout) cell.getChildAt(0);
            ImageView icon = (ImageView) pill.getChildAt(0);
            TextView label = (TextView) cell.getChildAt(1);
            pill.setBackgroundResource(on ? R.drawable.bg_nav_active : 0);
            int color = ContextCompat.getColor(c, on ? R.color.text_primary : R.color.text_secondary);
            icon.setColorFilter(color);
            label.setTextColor(color);
            label.setTypeface(null, on ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        }
    }
}
