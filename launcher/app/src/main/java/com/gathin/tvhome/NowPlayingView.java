package com.gathin.tvhome;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Small event-driven media panel; no artwork fetches or timers. */
public final class NowPlayingView extends LinearLayout {
    private final ImageView artwork;
    private final TextView title, artist, toggle, open;
    private Runnable onToggle = () -> {}, onOpen = () -> {}, onUp = () -> {}, onDown = () -> {};

    public NowPlayingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(HORIZONTAL);
        int padding = dp(10);
        setPadding(padding, padding, padding, padding);
        GradientDrawable background = new GradientDrawable();
        background.setColor(0xE6202023);
        background.setCornerRadius(dp(16));
        background.setStroke(dp(1), 0x40FFFFFF);
        setBackground(background);
        LayoutInflater.from(context).inflate(R.layout.panel_now_playing, this, true);
        artwork = findViewById(R.id.nowPlayingArt);
        title = findViewById(R.id.nowPlayingTitle);
        artist = findViewById(R.id.nowPlayingArtist);
        toggle = findViewById(R.id.nowPlayingToggle);
        open = findViewById(R.id.nowPlayingOpen);
        toggle.setOnClickListener(v -> onToggle.run());
        open.setOnClickListener(v -> onOpen.run());
        View.OnKeyListener navigation = (view, key, event) -> {
            boolean direction = key == KeyEvent.KEYCODE_DPAD_UP || key == KeyEvent.KEYCODE_DPAD_DOWN
                    || key == KeyEvent.KEYCODE_DPAD_LEFT || key == KeyEvent.KEYCODE_DPAD_RIGHT;
            if (!direction) return false;
            if (event.getAction() != KeyEvent.ACTION_DOWN) return true;
            // Clamp this short row; Up/Down always leave it along the documented route.
            if (key == KeyEvent.KEYCODE_DPAD_UP) {
                onUp.run();
            } else if (key == KeyEvent.KEYCODE_DPAD_DOWN) {
                onDown.run();
            } else if (key == KeyEvent.KEYCODE_DPAD_RIGHT && view == toggle) open.requestFocus();
            else if (key == KeyEvent.KEYCODE_DPAD_LEFT && view == open && toggle.isEnabled()) toggle.requestFocus();
            return true;
        };
        toggle.setOnKeyListener(navigation);
        open.setOnKeyListener(navigation);
        setVisibility(GONE);
    }
    void setActions(Runnable toggle, Runnable open, Runnable up, Runnable down) {
        onToggle = toggle;
        onOpen = open;
        onUp = up;
        onDown = down;
    }
    boolean focusControls() {
        return getVisibility() == VISIBLE && (toggle.isEnabled() ? toggle : open).requestFocus();
    }
    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int available = MeasureSpec.getSize(widthMeasureSpec);
        int width = MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED
                ? dp(430) : Math.min(available, dp(430));
        super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), heightMeasureSpec);
    }
    void bind(NowPlayingController.Snapshot snapshot) {
        if (snapshot == null) {
            boolean restore = hasFocus();
            artwork.setImageDrawable(null);
            title.setText("");
            artist.setText("");
            setVisibility(GONE);
            if (restore) onDown.run();
            return;
        }
        title.setText(snapshot.title);
        artist.setText(snapshot.artist);
        if (snapshot.artwork != null) artwork.setImageBitmap(snapshot.artwork);
        else {
            try { artwork.setImageDrawable(getContext().getPackageManager().getApplicationIcon(snapshot.packageName)); }
            catch (PackageManager.NameNotFoundException missing) { artwork.setImageResource(R.mipmap.ic_launcher); }
        }
        toggle.setText(snapshot.playing ? R.string.media_pause : R.string.media_play);
        boolean moveFocus = toggle.isFocused() && !snapshot.canToggle;
        toggle.setFocusable(snapshot.canToggle);
        toggle.setEnabled(snapshot.canToggle);
        toggle.setAlpha(snapshot.canToggle ? 1f : 0.4f);
        setVisibility(VISIBLE);
        if (moveFocus) open.requestFocus();
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
