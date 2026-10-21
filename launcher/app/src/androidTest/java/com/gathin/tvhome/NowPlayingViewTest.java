package com.gathin.tvhome;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Locale;
import static org.junit.Assert.*;

public class NowPlayingViewTest {
    @Test public void compactAndHebrewLayoutsReserveSpaceWithoutOverlap() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context base = InstrumentationRegistry.getInstrumentation().getTargetContext();
            for (String language : new String[]{"en", "iw"}) {
                Configuration configuration = new Configuration(base.getResources().getConfiguration());
                configuration.setLocale(new Locale(language));
                Context context = new ContextThemeWrapper(base.createConfigurationContext(configuration), R.style.Theme_TvHome);
                LayoutInflater inflater = LayoutInflater.from(context).cloneInContext(context);
                LegacyFonts.install(inflater);
                FrameLayout root = (FrameLayout) inflater.inflate(R.layout.activity_main, null, false);
                NowPlayingView panel = root.findViewById(R.id.nowPlayingPanel);
                panel.bind(new NowPlayingController.Snapshot(context.getPackageName(),
                        "A very long title that must stay inside the panel", "Artist", "App", null, true, true));
                float density = context.getResources().getDisplayMetrics().density;
                for (int[] size : new int[][]{{960, 540}, {480, 320}}) {
                    int width = Math.round(size[0] * density), height = Math.round(size[1] * density);
                    root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
                    root.layout(0, 0, width, height);
                    Rect media = bounds(root, panel);
                    Rect grid = bounds(root, root.findViewById(R.id.grid));
                    Rect header = bounds(root, (View) root.findViewById(R.id.leftBlock).getParent());
                    assertFalse("Panel intersects grid", Rect.intersects(media, grid));
                    assertFalse("Panel intersects header", Rect.intersects(media, header));
                    assertTrue(media.left >= 0 && media.right <= width);
                    assertTrue(media.height() > 0);
                }
            }
        });
    }
    private static Rect bounds(FrameLayout root, View view) {
        Rect rect = new Rect();
        view.getDrawingRect(rect);
        root.offsetDescendantRectToMyCoords(view, rect);
        return rect;
    }
    @Test public void hiddenEmptyAndAccessibleControls() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context context = new ContextThemeWrapper(InstrumentationRegistry.getInstrumentation().getTargetContext(), R.style.Theme_TvHome);
            NowPlayingView view = new NowPlayingView(context, null);
            assertEquals(View.GONE, view.getVisibility());
            AtomicInteger toggles = new AtomicInteger();
            AtomicInteger opens = new AtomicInteger();
            AtomicInteger up = new AtomicInteger();
            view.setActions(toggles::incrementAndGet, opens::incrementAndGet, up::incrementAndGet, () -> {});
            view.bind(new NowPlayingController.Snapshot(context.getPackageName(), "A long track title", "Artist", "App", null, true, false));
            assertEquals(View.VISIBLE, view.getVisibility());
            assertEquals("A long track title", ((TextView)view.findViewById(R.id.nowPlayingTitle)).getText().toString());
            View toggle = view.findViewById(R.id.nowPlayingToggle);
            assertFalse(toggle.isEnabled());
            assertFalse(toggle.isFocusable());
            View open = view.findViewById(R.id.nowPlayingOpen);
            open.performClick();
            assertEquals(1, opens.get());
            view.bind(new NowPlayingController.Snapshot(context.getPackageName(), "Track", "Artist", "App", null, false, true));
            assertTrue(toggle.isEnabled());
            toggle.performClick();
            assertEquals(1, toggles.get());
            toggle.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP));
            assertEquals(1, up.get());
            view.bind(null);
            assertEquals(View.GONE, view.getVisibility());
        });
    }
}
