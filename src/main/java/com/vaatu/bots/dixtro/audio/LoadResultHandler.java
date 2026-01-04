package com.vaatu.bots.dixtro.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.vaatu.bots.dixtro.embed.EmbedFactory;
import com.vaatu.bots.dixtro.message.FailedToLoadMessage;
import com.vaatu.bots.dixtro.message.NotFoundMessage;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.List;

@Slf4j
public class LoadResultHandler implements AudioLoadResultHandler {
    private final GuildTrackManager trackManager;
    private final String requesterName;

    public LoadResultHandler(GuildTrackManager trackManager, String requesterName) {
        this.trackManager = trackManager;
        this.requesterName = requesterName != null ? requesterName : "Unknown";
    }

    private void connectToVoice() {
        if (!this.trackManager.isVoiceConnected()) {
            this.trackManager.connectVoiceManager();
        }
    }

    @Override
    public void trackLoaded(AudioTrack audioTrack) {
        connectToVoice();
        log.info("Song: {} Loaded", audioTrack.getInfo().title);
        QueuedTrack queuedTrack = new QueuedTrack(audioTrack, requesterName);
        // Set as currently playing BEFORE starting to ensure onTrackStart has the info
        trackManager.setCurrentlyPlaying(queuedTrack);
        if (!trackManager.getAudioPlayer().startTrack(audioTrack, true)) {
            // Track didn't start, add to queue instead
            trackManager.setCurrentlyPlaying(null);
            trackManager.getQueue().add(queuedTrack);
        }
    }

    @Override
    public void playlistLoaded(AudioPlaylist audioPlaylist) {
        connectToVoice();
        List<AudioTrack> tracks = audioPlaylist.getTracks();
        AudioTrack starterTrack = tracks.removeFirst();

        if (audioPlaylist.isSearchResult()) {
            trackLoaded(starterTrack);
        } else {
            trackManager.announceInChannel("🗒️ Loading playlist: " + (tracks.size() + 1) + " Songs 😎");
            QueuedTrack starterQueuedTrack = new QueuedTrack(starterTrack, requesterName);
            // Set as currently playing BEFORE starting
            trackManager.setCurrentlyPlaying(starterQueuedTrack);
            if (!trackManager.getAudioPlayer().startTrack(starterTrack, true)) {
                // Track didn't start, add all to queue
                trackManager.setCurrentlyPlaying(null);
                tracks.add(0, starterTrack);
            }

            for (AudioTrack t : tracks) {
                trackManager.getQueue().add(new QueuedTrack(t, requesterName));
            }
        }
    }

    @Override
    public void noMatches() {
        log.error("Error at finding track.");
        MessageEmbed errorEmbed = EmbedFactory.createUserErrorEmbed(new NotFoundMessage().getMessage());
        trackManager.announceInChannel(errorEmbed);
    }

    @Override
    public void loadFailed(FriendlyException e) {
        log.error("Error at loading track: {}", e.getMessage());
        MessageEmbed errorEmbed = EmbedFactory.createUserErrorEmbed(new FailedToLoadMessage().getMessage());
        trackManager.announceInChannel(errorEmbed);
    }
}
