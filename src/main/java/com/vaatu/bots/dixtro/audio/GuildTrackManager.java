package com.vaatu.bots.dixtro.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.managers.AudioManager;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
@Getter
public class GuildTrackManager {
    private final AudioPlayerSendHandler audioPlayerSendHandler;
    private final AudioPlayerManager audioPlayerManager;
    private final BlockingQueue<QueuedTrack> queue;
    private final AudioChannelUnion voiceChannel;
    private final MessageChannelUnion channelUnion;
    private final TrackScheduler trackScheduler;
    private final AudioPlayer audioPlayer;
    private final Guild guild;
    private QueuedTrack currentlyPlaying;

    public GuildTrackManager(Guild guild, MessageChannelUnion messageChannelUnion, AudioChannelUnion voiceChannel) {
        this.channelUnion = messageChannelUnion;
        this.voiceChannel = voiceChannel;
        this.guild = guild;
        this.queue = new LinkedBlockingQueue<>();
        this.audioPlayerManager = new DefaultAudioPlayerManager();
        YoutubeAudioSourceManager audioSourceManager = new YoutubeAudioSourceManager(true);
        this.audioPlayerManager.registerSourceManager(audioSourceManager);
        AudioSourceManagers.registerRemoteSources(this.audioPlayerManager);
        this.audioPlayer = this.audioPlayerManager.createPlayer();
        this.trackScheduler = new TrackScheduler(this);
        this.audioPlayer.addListener(this.trackScheduler);
        this.audioPlayerSendHandler = new AudioPlayerSendHandler(this.audioPlayer);
    }

    private boolean isSourceURL(String source) {
        try {
            new URI(source);
            return true;
        } catch (URISyntaxException ex) {
            return false;
        }
    }

    public boolean trackIsEmpty() {
        return audioPlayer.getPlayingTrack() == null & this.queue.isEmpty();
    }

    public void announceInChannel(String message) {
        this.channelUnion.sendMessage(message).queue();
    }

    public void announceInChannel(MessageEmbed embed) {
        this.channelUnion.sendMessageEmbeds(embed).queue();
    }

    public boolean isVoiceConnected() {
        AudioManager guildAudioManager = this.guild.getAudioManager();
        return guildAudioManager.isConnected();
    }

    public void connectVoiceManager() {
        AudioManager guildAudioManager = this.guild.getAudioManager();
        guildAudioManager.openAudioConnection(this.voiceChannel);
        guildAudioManager.setSendingHandler(this.audioPlayerSendHandler);
    }

    public void destroyInstance() {
        this.audioPlayer.destroy();
        this.queue.clear();
    }

    public void disconnectVoiceManager() {
        AudioManager audioManager = this.guild.getAudioManager();
        if (audioManager.isConnected() && trackIsEmpty()) {
            audioManager.closeAudioConnection();
        } else {
            log.error("Unable to disconnect from a not connected channel.");
        }
    }

    public void skipTrack() {
        QueuedTrack next = queue.poll();
        if (next != null) {
            log.info("Skipped song.");
            this.currentlyPlaying = next;
            this.audioPlayer.startTrack(next.getTrack(), false);
        } else {
            log.info("Disconnecting from skip command...");
            this.currentlyPlaying = null;
            this.audioPlayer.stopTrack();
        }
    }

    public void loadTrack(String source) {
        this.loadTrack(source, "Unknown");
    }

    public void loadTrack(String source, String requesterName) {
        boolean isURL = this.isSourceURL(source);
        // Create a new LoadResultHandler that captures the requester name for enqueuing
        LoadResultHandler handler = new LoadResultHandler(this, requesterName);
        if (isURL) {
            this.audioPlayerManager.loadItem(source, handler);
        } else {
            this.audioPlayerManager.loadItem("ytsearch:" + source, handler);
        }
    }

    public BlockingQueue<QueuedTrack> getQueue() {
        return queue;
    }

    public String getCurrentTrackAddedBy() {
        if (currentlyPlaying != null) {
            return currentlyPlaying.getAddedBy();
        }
        return "Unknown";
    }

    public void setCurrentlyPlaying(QueuedTrack queuedTrack) {
        this.currentlyPlaying = queuedTrack;
    }

    public QueuedTrack getCurrentlyPlaying() {
        return currentlyPlaying;
    }
}
