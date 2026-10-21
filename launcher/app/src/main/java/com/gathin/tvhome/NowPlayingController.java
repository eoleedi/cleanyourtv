package com.gathin.tvhome;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Visible-Home-only media observation. No polling, notification reading or artwork URL loading. */
final class NowPlayingController {
    interface Listener { void onChanged(Snapshot snapshot); }
    interface SessionSource {
        void add(Runnable callback);
        void remove();
        List<MediaController> get();
    }
    static final class Snapshot {
        final String packageName, title, artist, appLabel;
        final Bitmap artwork;
        final boolean playing, canToggle;
        Snapshot(String packageName, String title, String artist, String appLabel,
                 Bitmap artwork, boolean playing, boolean canToggle) {
            this.packageName = packageName;
            this.title = title;
            this.artist = artist;
            this.appLabel = appLabel;
            this.artwork = artwork;
            this.playing = playing;
            this.canToggle = canToggle;
        }
    }

    private final Context context;
    private final Listener listener;
    private final SessionSource source;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<MediaSession.Token, Watch> watches = new HashMap<>();
    private final Set<MediaSession.Token> destroyed = new HashSet<>();
    private final Runnable refresh = this::refresh;
    private final Runnable accessChanged = () -> handler.post(refresh);
    private MediaController selected;
    private boolean started;

    NowPlayingController(Context context, Listener listener) {
        this(context, listener, new AndroidSessions(context));
    }
    NowPlayingController(Context context, Listener listener, SessionSource source) {
        this.context = context;
        this.listener = listener;
        this.source = source;
    }

    void start() {
        if (started) return;
        started = true;
        MediaAccessService.add(accessChanged);
        source.add(refresh);
        refresh();
    }
    void stop() {
        if (!started) return;
        started = false;
        MediaAccessService.remove(accessChanged);
        handler.removeCallbacks(refresh);
        source.remove();
        clear();
    }
    private void clear() {
        for (Watch watch : watches.values()) watch.controller.unregisterCallback(watch.callback);
        watches.clear();
        selected = null;
        listener.onChanged(null);
    }

    private void refresh() {
        if (!started) return;
        try {
            List<MediaController> sessions = source.get();
            Set<MediaSession.Token> reported = new HashSet<>();
            for (MediaController controller : sessions) reported.add(controller.getSessionToken());
            // Keep a tombstone while the source still reports a dead token, even across stop/start.
            // Once the service drops it, it cannot be selected again and the reference can go.
            destroyed.retainAll(reported);
            Set<MediaSession.Token> present = new HashSet<>();
            List<NowPlayingSelection.Candidate> candidates = new ArrayList<>();
            for (MediaController controller : sessions) {
                MediaSession.Token token = controller.getSessionToken();
                if (destroyed.contains(token)) continue;
                present.add(token);
                if (!watches.containsKey(token)) {
                    Watch watch = new Watch(controller);
                    watches.put(token, watch);
                    controller.registerCallback(watch.callback, handler);
                }
                PlaybackState state = controller.getPlaybackState();
                candidates.add(new NowPlayingSelection.Candidate(token, true,
                        state == null ? PlaybackState.STATE_NONE : state.getState()));
            }
            for (MediaSession.Token token : new ArrayList<>(watches.keySet())) {
                if (!present.contains(token)) {
                    Watch removed = watches.remove(token);
                    removed.controller.unregisterCallback(removed.callback);
                }
            }
            Object previous = selected == null ? null : selected.getSessionToken();
            NowPlayingSelection.Candidate best = NowPlayingSelection.choose(candidates, previous);
            selected = best == null ? null : watches.get(best.key).controller;
            listener.onChanged(selected == null ? null : snapshot(selected));
        } catch (SecurityException | IllegalStateException unavailable) {
            clear();
        }
    }

    private final class Watch {
        final MediaController controller;
        final MediaController.Callback callback = new MediaController.Callback() {
            @Override public void onMetadataChanged(MediaMetadata metadata) { refresh(); }
            @Override public void onPlaybackStateChanged(PlaybackState state) { refresh(); }
            @Override public void onSessionDestroyed() {
                destroyed.add(controller.getSessionToken());
                refresh();
            }
        };
        Watch(MediaController controller) { this.controller = controller; }
    }

    private Snapshot snapshot(MediaController controller) {
        String label = controller.getPackageName();
        try {
            PackageManager pm = context.getPackageManager();
            label = pm.getApplicationLabel(pm.getApplicationInfo(label, 0)).toString();
        } catch (PackageManager.NameNotFoundException ignored) { }
        MediaMetadata metadata = controller.getMetadata();
        String title = text(metadata, MediaMetadata.METADATA_KEY_DISPLAY_TITLE,
                text(metadata, MediaMetadata.METADATA_KEY_TITLE, label));
        String artist = text(metadata, MediaMetadata.METADATA_KEY_ARTIST,
                text(metadata, MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, label));
        Bitmap artwork = null;
        if (metadata != null) {
            artwork = metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);
            if (artwork == null) artwork = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
            if (artwork == null) artwork = metadata.getBitmap(MediaMetadata.METADATA_KEY_ART);
            if (artwork != null && !artwork.isRecycled()) {
                int max = Math.max(artwork.getWidth(), artwork.getHeight());
                if (max > 256) artwork = Bitmap.createScaledBitmap(artwork,
                        Math.max(1, artwork.getWidth() * 256 / max),
                        Math.max(1, artwork.getHeight() * 256 / max), true);
            } else artwork = null;
        }
        PlaybackState state = controller.getPlaybackState();
        boolean playing = state != null && state.getState() != PlaybackState.STATE_PAUSED;
        return new Snapshot(controller.getPackageName(), title, artist, label,
                artwork, playing, canToggle(state, playing));
    }
    private static String text(MediaMetadata metadata, String key, String fallback) {
        CharSequence value = metadata == null ? null : metadata.getText(key);
        return value == null || value.toString().trim().isEmpty() ? fallback : value.toString();
    }
    private static boolean canToggle(PlaybackState state, boolean playing) {
        if (state == null) return false;
        long action = playing ? PlaybackState.ACTION_PAUSE : PlaybackState.ACTION_PLAY;
        return (state.getActions() & (action | PlaybackState.ACTION_PLAY_PAUSE)) != 0;
    }

    void playPause() {
        MediaController target = selected;
        if (!started || target == null) return;
        try {
            PlaybackState state = target.getPlaybackState();
            if (state == null || state.getState() == PlaybackState.STATE_STOPPED
                    || state.getState() == PlaybackState.STATE_NONE || state.getState() == PlaybackState.STATE_ERROR) {
                refresh();
                return;
            }
            boolean playing = state.getState() != PlaybackState.STATE_PAUSED;
            if (!canToggle(state, playing)) return;
            if (playing) target.getTransportControls().pause();
            else target.getTransportControls().play();
        } catch (SecurityException | IllegalStateException unavailable) { refresh(); }
    }
    void openApp() {
        if (!started || selected == null) return;
        try {
            PendingIntent activity = selected.getSessionActivity();
            if (activity != null) {
                try { activity.send(); return; }
                catch (PendingIntent.CanceledException ignored) { }
            }
            Intent intent = context.getPackageManager().getLeanbackLaunchIntentForPackage(selected.getPackageName());
            if (intent != null) context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (SecurityException | android.content.ActivityNotFoundException | IllegalStateException unavailable) {
            refresh();
        }
    }

    private static final class AndroidSessions implements SessionSource {
        final Context context;
        final MediaSessionManager manager;
        final ComponentName component;
        final Handler handler = new Handler(Looper.getMainLooper());
        Runnable callback;
        boolean registered;
        final MediaSessionManager.OnActiveSessionsChangedListener changed = sessions -> {
            if (callback != null) callback.run();
        };
        AndroidSessions(Context context) {
            this.context = context;
            manager = (MediaSessionManager) context.getSystemService(Context.MEDIA_SESSION_SERVICE);
            component = new ComponentName(context, MediaAccessService.class);
        }
        public void add(Runnable callback) { this.callback = callback; }
        public void remove() {
            callback = null;
            if (registered) {
                manager.removeOnActiveSessionsChangedListener(changed);
                registered = false;
            }
        }
        public List<MediaController> get() {
            String enabled = Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners");
            boolean allowed = false;
            if (enabled != null) for (String name : enabled.split(":")) {
                if (component.equals(ComponentName.unflattenFromString(name))) allowed = true;
            }
            if (!allowed || manager == null) {
                if (registered) {
                    manager.removeOnActiveSessionsChangedListener(changed);
                    registered = false;
                }
                throw new SecurityException("Media access not enabled");
            }
            if (!registered) {
                manager.addOnActiveSessionsChangedListener(changed, component, handler);
                registered = true;
            }
            return manager.getActiveSessions(component);
        }
    }
}
