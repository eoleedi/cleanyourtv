package com.gathin.tvhome;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import static org.junit.Assert.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;

/** Run on the actual API-24 TV; a host JVM alone cannot prove desugaring/font inflation. */
public class Android7CompatibilityTest {
    @Test public void testDesugaredDateAndZone() {
        assertEquals(29, LocalDate.of(2024, 2, 28).plusDays(1).getDayOfMonth());
        assertNotNull(ZoneId.systemDefault().getRules());
    }

    @Test public void testBundledFontWeightsLoad() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        for (int font : new int[]{R.font.outfit_extralight, R.font.outfit_light,
                R.font.outfit_regular, R.font.outfit_medium}) {
            Typeface face = LegacyFonts.get(context, font);
            assertNotNull(face);
            assertFalse("Bundled font must not silently become system default", Typeface.DEFAULT.equals(face));
        }
    }

    @Test public void testDefaultAndHebrewInflationAndClonedInflater() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context base = InstrumentationRegistry.getInstrumentation().getTargetContext();
            for (String language : new String[]{"en", "iw"}) {
                Configuration config = new Configuration(base.getResources().getConfiguration());
                config.setLocale(new Locale(language));
                Context themed = new ContextThemeWrapper(base.createConfigurationContext(config), R.style.Theme_TvHome);
                LayoutInflater inflater = LayoutInflater.from(themed).cloneInContext(themed);
                LegacyFonts.install(inflater);
                View home = inflater.inflate(R.layout.activity_main, null, false);
                TextView headline = home.findViewById(R.id.raceHeadline);
                assertEquals(LegacyFonts.get(themed, R.font.outfit_light), headline.getTypeface());
                TextView medium = home.findViewById(R.id.raceKicker);
                assertEquals(LegacyFonts.get(themed, R.font.outfit_medium), medium.getTypeface());
                LayoutInflater clone = inflater.cloneInContext(themed);
                assertNotNull(clone.inflate(R.layout.item_input, null, false));
                assertNotNull(clone.inflate(R.layout.dream_clock, null, false));
            }
        });
    }
}
