package com.gathin.tvhome;

import java.util.List;
import java.util.Objects;

/** Pure selection policy; state values match Android PlaybackState without a framework dependency. */
final class NowPlayingSelection {
    static final class Candidate {
        final Object key;
        final boolean active;
        final int state;
        Candidate(Object key, boolean active, int state) {
            this.key = key;
            this.active = active;
            this.state = state;
        }
    }

    static Candidate choose(List<Candidate> sessions, Object previousKey) {
        Candidate best = null;
        int bestRank = 0;
        for (Candidate candidate : sessions) {
            int rank = candidate.active ? rank(candidate.state) : 0;
            if (rank > bestRank || rank > 0 && rank == bestRank
                    && Objects.equals(candidate.key, previousKey)) {
                best = candidate;
                bestRank = rank;
            }
        }
        return best;
    }

    private static int rank(int state) {
        if (state == 2) return 1; // paused: retain a useful Resume control
        if (state == 3 || state == 4 || state == 5 || state == 6 || state == 8) return 2;
        return 0;
    }
}
