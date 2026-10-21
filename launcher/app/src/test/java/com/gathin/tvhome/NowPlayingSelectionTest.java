package com.gathin.tvhome;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.*;

public class NowPlayingSelectionTest {
    private NowPlayingSelection.Candidate candidate(String key, boolean active, int state) {
        return new NowPlayingSelection.Candidate(key, active, state);
    }

    @Test public void emptyAndStoppedHide() {
        assertNull(NowPlayingSelection.choose(Collections.emptyList(), null));
        assertNull(NowPlayingSelection.choose(Arrays.asList(candidate("none", true, 0),
                candidate("stopped", true, 1), candidate("error", true, 7)), null));
    }
    @Test public void playingBeatsPausedAndInactive() {
        NowPlayingSelection.Candidate paused = candidate("paused", true, 2);
        NowPlayingSelection.Candidate playing = candidate("playing", true, 3);
        assertSame(playing, NowPlayingSelection.choose(Arrays.asList(
                candidate("inactive", false, 3), paused, playing), "paused"));
    }
    @Test public void pausedCanResume() {
        NowPlayingSelection.Candidate paused = candidate("paused", true, 2);
        assertSame(paused, NowPlayingSelection.choose(Collections.singletonList(paused), null));
    }
    @Test public void equalRankKeepsPrevious() {
        NowPlayingSelection.Candidate first = candidate("first", true, 3);
        NowPlayingSelection.Candidate previous = candidate("previous", true, 6);
        assertSame(previous, NowPlayingSelection.choose(Arrays.asList(first, previous), "previous"));
    }
    @Test public void disappearanceSelectsNextOrHides() {
        NowPlayingSelection.Candidate other = candidate("other", true, 8);
        assertSame(other, NowPlayingSelection.choose(Collections.singletonList(other), "destroyed"));
        assertNull(NowPlayingSelection.choose(Collections.emptyList(), "destroyed"));
    }
}
