package com.gathin.tvhome;

import android.service.notification.NotificationListenerService;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/** Opt-in authorization for media sessions. Notification contents are never inspected. */
public final class MediaAccessService extends NotificationListenerService {
    private static final Set<Runnable> listeners = new CopyOnWriteArraySet<>();
    static void add(Runnable listener) { listeners.add(listener); }
    static void remove(Runnable listener) { listeners.remove(listener); }
    @Override public void onListenerConnected() { notifyAccessChanged(); }
    @Override public void onListenerDisconnected() { notifyAccessChanged(); }
    private void notifyAccessChanged() {
        for (Runnable listener : listeners) listener.run();
    }
}
