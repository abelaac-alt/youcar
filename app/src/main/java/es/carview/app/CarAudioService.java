package es.carview.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Audio-only Android Auto media service. Never supplies video to the car host.
 * The phone's video browser is not an Android Auto activity.
 */
public class CarAudioService extends MediaBrowserServiceCompat {
    private static final String TAG = "CarAudioService";
    private static final String ROOT = "audio_root";
    private static final String CHANNEL = "carview_audio";
    private static final int NOTIFICATION_ID = 112;
    private MediaSessionCompat session;
    private MediaPlayer player;
    private String currentTrackUri;
    private boolean preparing;
    private AudioManager audioManager;
    private AudioFocusRequest audioFocus;
    private boolean hasAudioFocus;

    @Override
    public void onCreate() {
        super.onCreate();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        session = new MediaSessionCompat(this, "CarView Audio");
        session.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        session.setCallback(new MediaSessionCompat.Callback() {
            @Override public void onPlay() { resume(); }
            @Override public void onPlayFromMediaId(String mediaId, Bundle extras) {
                playTrack(mediaId);
            }
            @Override public void onPause() { pausePlayback(); }
            @Override public void onStop() { stopPlayback(); }
            @Override public void onSkipToNext() { skip(1); }
            @Override public void onSkipToPrevious() { skip(-1); }
            @Override public void onSeekTo(long pos) {
                if (player != null && !preparing) {
                    player.seekTo((int) Math.min(Math.max(pos, 0), Integer.MAX_VALUE));
                    setState(PlaybackStateCompat.STATE_PLAYING, null);
                }
            }
        });
        setSessionToken(session.getSessionToken());
        session.setActive(true);
        setState(PlaybackStateCompat.STATE_STOPPED, null);
        createNotificationChannel();
    }

    @Override
    public BrowserRoot onGetRoot(String packageName, int uid, Bundle rootHints) {
        return new BrowserRoot(ROOT, null);
    }

    @Override
    public void onLoadChildren(String parentId,
                               Result<List<MediaBrowserCompat.MediaItem>> result) {
        List<MediaBrowserCompat.MediaItem> items = new ArrayList<>();
        if (ROOT.equals(parentId)) {
            for (LocalAudioLibrary.Track track : LocalAudioLibrary.load(this)) {
                MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                        .setMediaId(track.uri)
                        .setTitle(track.title)
                        .setSubtitle("Archivo de audio local")
                        .build();
                items.add(new MediaBrowserCompat.MediaItem(description,
                        MediaBrowserCompat.MediaItem.FLAG_PLAYABLE));
            }
        }
        result.sendResult(items);
    }

    private void playTrack(String uri) {
        LocalAudioLibrary.Track track = LocalAudioLibrary.find(this, uri);
        if (track == null) {
            setState(PlaybackStateCompat.STATE_ERROR, "Archivo no disponible");
            return;
        }
        if (!requestFocus()) {
            setState(PlaybackStateCompat.STATE_PAUSED, "Audio no disponible");
            return;
        }
        releasePlayer();
        preparing = true;
        currentTrackUri = uri;
        setState(PlaybackStateCompat.STATE_BUFFERING, null);
        session.setMetadata(new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, uri)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "CarView · Audio local")
                .build());
        player = new MediaPlayer();
        player.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
        player.setOnPreparedListener(mp -> {
            preparing = false;
            mp.start();
            session.setMetadata(new MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, uri)
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.title)
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "CarView · Audio local")
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, mp.getDuration())
                    .build());
            setState(PlaybackStateCompat.STATE_PLAYING, null);
            showPlayingNotification(track.title);
        });
        player.setOnCompletionListener(mp -> stopPlayback());
        player.setOnErrorListener((mp, what, extra) -> {
            releasePlayer();
            setState(PlaybackStateCompat.STATE_ERROR, "No se pudo reproducir el archivo");
            stopForeground(STOP_FOREGROUND_REMOVE);
            return true;
        });
        try {
            player.setDataSource(this, Uri.parse(uri));
            player.prepareAsync();
        } catch (Exception ex) {
            Log.w(TAG, "Audio file could not be opened", ex);
            releasePlayer();
            setState(PlaybackStateCompat.STATE_ERROR, "Archivo inaccesible o formato no admitido");
        }
    }

    private void resume() {
        if (currentTrackUri == null) {
            List<LocalAudioLibrary.Track> tracks = LocalAudioLibrary.load(this);
            if (!tracks.isEmpty()) playTrack(tracks.get(0).uri);
            return;
        }
        if (player == null) {
            playTrack(currentTrackUri);
            return;
        }
        if (preparing) return;
        if (requestFocus()) {
            player.start();
            setState(PlaybackStateCompat.STATE_PLAYING, null);
            LocalAudioLibrary.Track track = LocalAudioLibrary.find(this, currentTrackUri);
            showPlayingNotification(track == null ? "Audio local" : track.title);
        }
    }

    private void pausePlayback() {
        if (player != null && !preparing && player.isPlaying()) player.pause();
        setState(PlaybackStateCompat.STATE_PAUSED, null);
        stopForeground(STOP_FOREGROUND_REMOVE);
    }

    private void stopPlayback() {
        releasePlayer();
        setState(PlaybackStateCompat.STATE_STOPPED, null);
        stopForeground(STOP_FOREGROUND_REMOVE);
        abandonFocus();
    }

    private void skip(int direction) {
        List<LocalAudioLibrary.Track> tracks = LocalAudioLibrary.load(this);
        if (tracks.isEmpty()) return;
        int index = 0;
        for (int i = 0; i < tracks.size(); i++) {
            if (tracks.get(i).uri.equals(currentTrackUri)) {
                index = i;
                break;
            }
        }
        int next = (index + direction + tracks.size()) % tracks.size();
        playTrack(tracks.get(next).uri);
    }

    private void releasePlayer() {
        preparing = false;
        if (player != null) {
            player.reset();
            player.release();
            player = null;
        }
    }

    private void setState(int state, String error) {
        long pos = 0;
        if (player != null && !preparing) {
            try { pos = player.getCurrentPosition(); }
            catch (IllegalStateException ignored) { }
        }
        PlaybackStateCompat.Builder builder = new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY
                        | PlaybackStateCompat.ACTION_PAUSE
                        | PlaybackStateCompat.ACTION_PLAY_PAUSE
                        | PlaybackStateCompat.ACTION_STOP
                        | PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                        | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                        | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                        | PlaybackStateCompat.ACTION_SEEK_TO)
                .setState(state, pos, state == PlaybackStateCompat.STATE_PLAYING ? 1f : 0f);
        if (error != null) builder.setErrorMessage(error);
        session.setPlaybackState(builder.build());
    }

    private boolean requestFocus() {
        if (hasAudioFocus) return true;
        if (audioManager == null) return false;
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
        audioFocus = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attributes)
                .setOnAudioFocusChangeListener(change -> {
                    if (change < 0) {
                        hasAudioFocus = false;
                        pausePlayback();
                    }
                }).build();
        hasAudioFocus = audioManager.requestAudioFocus(audioFocus)
                == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        return hasAudioFocus;
    }

    private void abandonFocus() {
        if (hasAudioFocus && audioManager != null && audioFocus != null) {
            audioManager.abandonAudioFocusRequest(audioFocus);
        }
        hasAudioFocus = false;
    }

    private void createNotificationChannel() {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.createNotificationChannel(new NotificationChannel(CHANNEL,
                "Reproducción de audio", NotificationManager.IMPORTANCE_LOW));
    }

    private void showPlayingNotification(String title) {
        Intent launch = new Intent(this, MainActivity.class);
        PendingIntent launchPending = PendingIntent.getActivity(this, 0, launch,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new Notification.Builder(this, CHANNEL)
                .setContentTitle(title)
                .setContentText("CarView · reproducción de audio")
                .setContentIntent(launchPending)
                .setSmallIcon(R.drawable.ic_video)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(Notification.CATEGORY_TRANSPORT)
                .build();
        try {
            startForeground(NOTIFICATION_ID, notification);
        } catch (RuntimeException ex) {
            Log.w(TAG, "Foreground service unavailable", ex);
        }
    }

    @Override
    public void onDestroy() {
        stopPlayback();
        session.setActive(false);
        session.release();
        super.onDestroy();
    }
}
