package com.vaatu.bots.dixtro.audio;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

/**
 * Wrapper for AudioTrack that stores who added the track (display name at
 * enqueue time).
 */
public class QueuedTrack {
    private final AudioTrack track;
    private final String addedBy;

    public QueuedTrack(AudioTrack track, String addedBy) {
        this.track = track;
        this.addedBy = addedBy != null ? addedBy : "Unknown";
    }

    public AudioTrack getTrack() {
        return track;
    }

    public String getAddedBy() {
        return addedBy;
    }
}
