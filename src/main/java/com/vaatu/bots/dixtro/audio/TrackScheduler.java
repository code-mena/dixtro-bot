package com.vaatu.bots.dixtro.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.vaatu.bots.dixtro.embed.EmbedFactory;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.MessageEmbed;

@RequiredArgsConstructor
public class TrackScheduler extends AudioEventAdapter {
    private final GuildTrackManager trackManager;

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        String addedBy = trackManager.getCurrentlyPlaying() != null 
            ? trackManager.getCurrentlyPlaying().getAddedBy() 
            : "Unknown";
        MessageEmbed embed = EmbedFactory.createSongEmbed(track.getInfo(), addedBy);
        trackManager.announceInChannel(embed);
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        com.vaatu.bots.dixtro.audio.QueuedTrack next = trackManager.getQueue().poll();
        if (next != null && (endReason.mayStartNext)) {
            trackManager.setCurrentlyPlaying(next);
            player.startTrack(next.getTrack(), false);
            // announce will be handled in onTrackStart when the player actually starts the
            // track
        } else if (next == null & trackManager.trackIsEmpty()) {
            trackManager.setCurrentlyPlaying(null);
            MessageEmbed embed = EmbedFactory.createDefault("🥳 Finished tracks");
            trackManager.announceInChannel(embed);
            trackManager.disconnectVoiceManager();
        }
    }

    @Override
    public void onPlayerPause(AudioPlayer player) {
        MessageEmbed embed = EmbedFactory.createDefault("▶️ Track paused.");
        trackManager.announceInChannel(embed);
    }

    @Override
    public void onPlayerResume(AudioPlayer player) {
        MessageEmbed embed = EmbedFactory.createDefault("⏸️ Track resumed.");
        trackManager.announceInChannel(embed);
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        boolean lastSong = trackManager.getQueue().isEmpty();
        if (exception.severity.equals(FriendlyException.Severity.SUSPICIOUS) && lastSong) {
            trackManager.disconnectVoiceManager();
        }
    }
}
