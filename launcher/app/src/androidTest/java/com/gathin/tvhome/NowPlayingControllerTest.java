package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import android.os.Handler;
import android.os.Looper;
import static org.junit.Assert.*;

public class NowPlayingControllerTest {
    static final class Source implements NowPlayingController.SessionSource {
        final List<MediaController> sessions = new ArrayList<>();
        Runnable changed;
        boolean denied;
        int adds, removes;
        public void add(Runnable callback) { changed = callback; adds++; }
        public void remove() { changed = null; removes++; }
        public List<MediaController> get() {
            if (denied) throw new SecurityException("test denial");
            return sessions;
        }
    }

    @Test public void denialRevocationAndStopHideAndRelease() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            Source source = new Source();
            source.denied = true;
            AtomicReference<NowPlayingController.Snapshot> shown = new AtomicReference<>();
            NowPlayingController controller = new NowPlayingController(context, shown::set, source);
            controller.start();
            assertNull(shown.get());
            controller.start();
            assertEquals(1, source.adds);
            controller.stop();
            assertNull(shown.get());
            assertNull(source.changed);
            assertEquals(1, source.removes);
            controller.start();
            assertEquals(2, source.adds);
            controller.stop();
        });
    }

    @Test public void realSessionMetadataActionsAndBoundedArtwork() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            MediaSession session = new MediaSession(context, "NowPlaying-test");
            Source source = new Source();
            Bitmap artwork = Bitmap.createBitmap(800, 400, Bitmap.Config.ARGB_8888);
            session.setMetadata(new MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE, "Test song")
                    .putString(MediaMetadata.METADATA_KEY_ARTIST, "Test artist")
                    .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, artwork).build());
            session.setPlaybackState(new PlaybackState.Builder().setState(PlaybackState.STATE_PLAYING, 0, 1)
                    .setActions(PlaybackState.ACTION_PAUSE).build());
            session.setActive(true);
            source.sessions.add(session.getController());
            AtomicReference<NowPlayingController.Snapshot> shown = new AtomicReference<>();
            NowPlayingController controller = new NowPlayingController(context, shown::set, source);
            try {
                controller.start();
                assertNotNull(shown.get());
                assertEquals("Test song", shown.get().title);
                assertEquals("Test artist", shown.get().artist);
                assertTrue(shown.get().canToggle);
                assertTrue(shown.get().playing);
                assertEquals(256, shown.get().artwork.getWidth());
                assertEquals(128, shown.get().artwork.getHeight());
                source.denied = true;
                source.changed.run();
                assertNull(shown.get());
            } finally {
                controller.stop();
                session.release();
                artwork.recycle();
            }
        });
    }

    @Test public void transportTargetsOnlySelectedSessionAndDestroyedSessionHides() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        MediaSession selected = new MediaSession(context, "selected-test");
        MediaSession other = new MediaSession(context, "other-test");
        CountDownLatch paused = new CountDownLatch(1);
        CountDownLatch wrongTarget = new CountDownLatch(1);
        selected.setCallback(new MediaSession.Callback() {
            @Override public void onPause() { paused.countDown(); }
        }, new Handler(Looper.getMainLooper()));
        other.setCallback(new MediaSession.Callback() {
            @Override public void onPause() { wrongTarget.countDown(); }
        }, new Handler(Looper.getMainLooper()));
        selected.setPlaybackState(new PlaybackState.Builder().setState(PlaybackState.STATE_PLAYING, 0, 1)
                .setActions(PlaybackState.ACTION_PAUSE).build());
        other.setPlaybackState(new PlaybackState.Builder().setState(PlaybackState.STATE_PAUSED, 0, 0)
                .setActions(PlaybackState.ACTION_PLAY).build());
        Source source = new Source();
        source.sessions.add(selected.getController());
        source.sessions.add(other.getController());
        AtomicReference<NowPlayingController.Snapshot> shown = new AtomicReference<>();
        CountDownLatch hidden = new CountDownLatch(1);
        NowPlayingController controller = new NowPlayingController(context, snapshot -> {
            shown.set(snapshot);
            if (snapshot == null) hidden.countDown();
        }, source);
        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                controller.start();
                assertNotNull(shown.get());
                // Missing metadata falls back to the owning application's label.
                assertEquals(shown.get().appLabel, shown.get().title);
                controller.playPause();
            });
            assertTrue("Pause callback must reach the selected session", paused.await(3, TimeUnit.SECONDS));
            assertEquals(1, wrongTarget.getCount());
            source.sessions.remove(1);
            selected.release();
            // UI idle does not drain an in-flight Binder session-destroyed callback.
            assertTrue("Destroyed session must hide after its callback", hidden.await(3, TimeUnit.SECONDS));
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                source.changed.run();
                assertNull(shown.get());
                controller.stop();
                source.denied = false;
                controller.start();
                assertNull("A destroyed token must not reappear", shown.get());
            });
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(controller::stop);
            selected.release();
            other.release();
        }
    }
}
