package com.gathin.tvhome;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

/** Android 7 cannot inflate font resources or render variable fonts. */
final class LegacyFonts {
    private LegacyFonts() { }

    static Typeface get(Context context, int font) {
        if (Build.VERSION.SDK_INT < 26) {
            if (font == R.font.outfit_light) font = R.font.outfit_legacy_light;
            else if (font == R.font.outfit_extralight) font = R.font.outfit_legacy_extralight;
            else if (font == R.font.outfit_medium) font = R.font.outfit_legacy_medium;
            else if (font == R.font.outfit_regular || font == R.font.outfit) font = R.font.outfit_legacy_regular;
        }
        return ResourcesCompat.getFont(context, font);
    }

    /** Install before inflating; cloned popup/adapter inflaters inherit this factory. */
    static void install(LayoutInflater inflater) {
        if (Build.VERSION.SDK_INT >= 26) return;
        inflater.setFactory2(new LayoutInflater.Factory2() {
            @Override public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
                return onCreateView(name, context, attrs);
            }

            @Override public View onCreateView(String name, Context context, AttributeSet attrs) {
                if (!"TextView".equals(name) && !"android.widget.TextView".equals(name)) return null;
                TextView view = new TextView(context, attrs);
                TypedArray values = context.obtainStyledAttributes(attrs,
                        new int[]{android.R.attr.fontFamily}, android.R.attr.textViewStyle, 0);
                int font;
                try {
                    font = values.getResourceId(0, 0);
                } finally {
                    values.recycle();
                }
                if (font != 0) view.setTypeface(get(context, font));
                return view;
            }
        });
    }
}
