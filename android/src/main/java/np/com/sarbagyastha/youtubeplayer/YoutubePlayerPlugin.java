// Copyright 2019 Sarbagya Dhaubanjar. All rights reserved.
// Use of this source code is governed by a Apache license that can be found
// in the LICENSE file.

/// Youtube Player
/// Author: Sarbagya Dhaubanjar
///
///                               ,,
///    .M"""bgd                  *MM
///   ,MI    "Y                   MM
///   `MMb.      ,6"Yb.  `7Mb,od8 MM,dMMb.   ,6"Yb.  .P"Ybmmm `7M'   `MF',6"Yb.
///     `YMMNq. 8)   MM    MM' "' MM    `Mb 8)   MM :MI  I8     VA   ,V 8)   MM
///   .     `MM  ,pm9MM    MM     MM     M8  ,pm9MM  WmmmP"      VA ,V   ,pm9MM
///   Mb     dM 8M   MM    MM     MM.   ,M9 8M   MM 8M            VVV   8M   MM
///   P"Ybmmd"  `Moo9^Yo..JMML.   P^YbmdP'  `Moo9^Yo.YMMMMMb      ,V    `Moo9^Yo.
///                                              6'     dP    ,V
///                                              Ybmmmd'   OOb"
///
///
///                ,,                           ,,                             ,,
///   `7MM"""Yb. `7MM                          *MM                             db
///     MM    `Yb. MM                           MM
///     MM     `Mb MMpMMMb.   ,6"Yb.`7MM  `7MM  MM,dMMb.   ,6"Yb.  `7MMpMMMb.`7MM  ,6"Yb.  `7Mb,od8
///     MM      MM MM    MM  8)   MM  MM    MM  MM    `Mb 8)   MM    MM    MM  MM 8)   MM    MM' "'
///     MM     ,MP MM    MM   ,pm9MM  MM    MM  MM     M8  ,pm9MM    MM    MM  MM  ,pm9MM    MM
///     MM    ,dP' MM    MM  8M   MM  MM    MM  MM.   ,M9 8M   MM    MM    MM  MM 8M   MM    MM
///   .JMMmmmdP' .JMML  JMML.`Moo9^Yo.`Mbod"YML.P^YbmdP'  `Moo9^Yo..JMML  JMML.MM `Moo9^Yo..JMML.
///                                                                         QO MP
///                                                                         `bmP

/// Website: https://sarbagyastha.com.np
/// Github: https://github.com/sarbagyastha/youtube_player
///

package np.com.sarbagyastha.youtubeplayer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Player.DefaultEventListener;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.view.FlutterNativeView;
import io.flutter.view.TextureRegistry;

import static com.google.android.exoplayer2.Player.REPEAT_MODE_ALL;
import static com.google.android.exoplayer2.Player.REPEAT_MODE_OFF;


public class YoutubePlayerPlugin implements MethodCallHandler {

    private static final String TAG = "YoutubePlayerPlugin";

    private static class YoutubePlayer {

        private static String liveStreamUrl;

        private SimpleExoPlayer exoPlayer;

        private Surface surface;

        private final TextureRegistry.SurfaceTextureEntry textureEntry;

        private QueuingEventSink eventSink = new QueuingEventSink();

        private final EventChannel eventChannel;

        private boolean isInitialized = false;

        YoutubePlayer(
                Context context,
                EventChannel eventChannel,
                TextureRegistry.SurfaceTextureEntry textureEntry,
                String dataSource,
                Result result) {
            this.eventChannel = eventChannel;
            this.textureEntry = textureEntry;

            loadStreamLinks(context, dataSource, result);
        }

        @SuppressLint("StaticFieldLeak")
        private void loadStreamLinks(Context context, String url, Result result){
            TrackSelector trackSelector = new DefaultTrackSelector();
            exoPlayer = ExoPlayerFactory.newSimpleInstance(context, trackSelector);
            DataSource.Factory dataSourceFactory;
            dataSourceFactory =
                    new DefaultHttpDataSourceFactory(
                            "ExoPlayer",
                            null,
                            DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS,
                            DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS,
                            true);
            Uri vUri = Uri.parse(url);
            MediaSource mediaSource =  new ExtractorMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(vUri);;
            exoPlayer.prepare(mediaSource);
            setupYoutubePlayer(eventChannel, textureEntry, result);
        }

        private String readStream(InputStream in) {
            BufferedReader reader = null;
            StringBuffer response = new StringBuffer();
            try {
                reader = new BufferedReader(new InputStreamReader(in));
                String line = "";
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return response.toString();
        }

        @SuppressWarnings("deprecation")
        private void setupYoutubePlayer(
                EventChannel eventChannel,
                TextureRegistry.SurfaceTextureEntry textureEntry,
                Result result) {

            eventChannel.setStreamHandler(
                    new EventChannel.StreamHandler() {
                        @Override
                        public void onListen(Object o, EventChannel.EventSink sink) {
                            eventSink.setDelegate(sink);
                        }

                        @Override
                        public void onCancel(Object o) {
                            eventSink.setDelegate(null);
                        }
                    });

            surface = new Surface(textureEntry.surfaceTexture());
            exoPlayer.setVideoSurface(surface);
            exoPlayer.addListener(
                    new DefaultEventListener() {

                        @Override
                        public void onPlayerStateChanged(final boolean playWhenReady, final int playbackState) {
                            super.onPlayerStateChanged(playWhenReady, playbackState);
                            if (playbackState == Player.STATE_BUFFERING) {
                                Map<String, Object> event = new HashMap<>();
                                event.put("event", "bufferingUpdate");
                                List<Integer> range = Arrays.asList(0, exoPlayer.getBufferedPercentage());
                                // iOS supports a list of buffered ranges, so here is a list with a single range.
                                event.put("values", Collections.singletonList(range));
                                eventSink.success(event);
                            } else if (playbackState == Player.STATE_READY && !isInitialized) {
                                isInitialized = true;
                                sendInitialized();
                            }
                        }

                        @Override
                        public void onPlayerError(final ExoPlaybackException error) {
                            super.onPlayerError(error);
                            if (eventSink != null) {
                                eventSink.error("VideoError", "Youtube player had error " + error, null);
                            }
                        }
                    });

            Map<String, Object> reply = new HashMap<>();
            reply.put("textureId", textureEntry.id());
            result.success(reply);
        }


        void play() {
            exoPlayer.setPlayWhenReady(true);
        }

        void pause() {
            exoPlayer.setPlayWhenReady(false);
        }

        void setLooping(boolean value) {
            exoPlayer.setRepeatMode(value ? REPEAT_MODE_ALL : REPEAT_MODE_OFF);
        }

        void setVolume(double value) {
            float bracketedValue = (float) Math.max(0.0, Math.min(1.0, value));
            exoPlayer.setVolume(bracketedValue);
        }

        void seekTo(int location) {
            exoPlayer.seekTo(location);
        }

        long getPosition() {
            return exoPlayer.getCurrentPosition();
        }

        @SuppressWarnings("SuspiciousNameCombination")
        private void sendInitialized() {
            if (isInitialized) {
                Map<String, Object> event = new HashMap<>();
                event.put("event", "initialized");
                event.put("duration", exoPlayer.getDuration());
                if (exoPlayer.getVideoFormat() != null) {
                    Format videoFormat = exoPlayer.getVideoFormat();
                    int width = videoFormat.width;
                    int height = videoFormat.height;
                    int rotationDegrees = videoFormat.rotationDegrees;
                    // Switch the width/height if video was taken in portrait mode
                    if (rotationDegrees == 90 || rotationDegrees == 270) {
                        width = exoPlayer.getVideoFormat().height;
                        height = exoPlayer.getVideoFormat().width;
                    }
                    event.put("width", width);
                    event.put("height", height);
                }
                eventSink.success(event);
            }
        }

        void dispose() {
            if (isInitialized) {
                exoPlayer.stop();
            }
            textureEntry.release();
            eventChannel.setStreamHandler(null);
            if (surface != null) {
                surface.release();
            }
            if (exoPlayer != null) {
                exoPlayer.release();
            }
        }
    }

    public static void registerWith(Registrar registrar) {
        final YoutubePlayerPlugin plugin = new YoutubePlayerPlugin(registrar);
        final MethodChannel channel =
                new MethodChannel(registrar.messenger(), "sarbagyastha.com.np/youtubePlayer");
        channel.setMethodCallHandler(plugin);
        registrar.addViewDestroyListener(
                new PluginRegistry.ViewDestroyListener() {
                    @Override
                    public boolean onViewDestroy(FlutterNativeView view) {
                        plugin.onDestroy();
                        return false; // We are not interested in assuming ownership of the NativeView.
                    }
                });
    }

    private YoutubePlayerPlugin(Registrar registrar) {
        this.registrar = registrar;
        this.videoPlayers = new HashMap<>();
    }

    private final Map<Long, YoutubePlayer> videoPlayers;

    private final Registrar registrar;

    void onDestroy() {
        // The whole FlutterView is being destroyed. Here we release resources acquired for all instances
        // of YoutubePlayer. Once https://github.com/flutter/flutter/issues/19358 is resolved this may
        // be replaced with just asserting that videoPlayers.isEmpty().
        // https://github.com/flutter/flutter/issues/20989 tracks this.
        for (YoutubePlayer player : videoPlayers.values()) {
            player.dispose();
        }
        videoPlayers.clear();
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        TextureRegistry textures = registrar.textures();
        if (textures == null) {
            result.error("no_activity", "video_player plugin requires a foreground activity", null);
            return;
        }
        switch (call.method) {
            case "init":
                for (YoutubePlayer player : videoPlayers.values()) {
                    player.dispose();
                }
                videoPlayers.clear();
                break;
            case "create":
            {
                TextureRegistry.SurfaceTextureEntry handle = textures.createSurfaceTexture();
                EventChannel eventChannel =
                        new EventChannel(
                                registrar.messenger(), "sarbagyastha.com.np/youtubePlayer/videoEvents" + handle.id());

                YoutubePlayer player;
                if (call.argument("asset") != null) {
                    String assetLookupKey;
                    if (call.argument("package") != null) {
                        assetLookupKey =
                                registrar.lookupKeyForAsset(
                                        (String) call.argument("asset"), (String) call.argument("package"));
                    } else {
                        assetLookupKey = registrar.lookupKeyForAsset((String) call.argument("asset"));
                    }
                    player =
                            new YoutubePlayer(
                                    registrar.context(),
                                    eventChannel,
                                    handle,
                                    "asset:///" + assetLookupKey,
                                    result);
                    videoPlayers.put(handle.id(), player);
                } else {
                    player =
                            new YoutubePlayer(
                                    registrar.context(),
                                    eventChannel,
                                    handle,
                                    (String) call.argument("uri"),
                                    result);
                    videoPlayers.put(handle.id(), player);
                }
                break;
            }
            //Screen Start
            case "brightness":
                result.success(getBrightness());
                break;
            case "setBrightness":
                double brightness = call.argument("brightness");
                WindowManager.LayoutParams layoutParams = registrar.activity().getWindow().getAttributes();
                layoutParams.screenBrightness = (float)brightness;
                registrar.activity().getWindow().setAttributes(layoutParams);
                result.success(null);
                break;
            case "isKeptOn":
                int flags = registrar.activity().getWindow().getAttributes().flags;
                result.success((flags & WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0) ;
                break;
            case "keepOn":
                Boolean on = call.argument("on");
                if (on) {
                    Log.i(TAG,"Keeping screen on");
                    registrar.activity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
                else{
                    Log.i(TAG,"Not keeping screen on");
                    registrar.activity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
                result.success(null);
                break;
            //Screen End
            default:
            {
                long textureId = ((Number) call.argument("textureId")).longValue();
                YoutubePlayer player = videoPlayers.get(textureId);
                if (player == null) {
                    result.error(
                            "Unknown textureId",
                            "No video player associated with texture id " + textureId,
                            null);
                    return;
                }
                onMethodCall(call, result, textureId, player);
                break;
            }
        }
    }

    private void onMethodCall(MethodCall call, Result result, long textureId, YoutubePlayer player) {
        switch (call.method) {
            case "setLooping":
                player.setLooping((Boolean) call.argument("looping"));
                result.success(null);
                break;
            case "setVolume":
                player.setVolume((Double) call.argument("volume"));
                result.success(null);
                break;
            case "play":
                player.play();
                result.success(null);
                break;
            case "pause":
                player.pause();
                result.success(null);
                break;
            case "seekTo":
                int location = ((Number) call.argument("location")).intValue();
                player.seekTo(location);
                result.success(null);
                break;
            case "position":
                result.success(player.getPosition());
                break;
            case "dispose":
                player.dispose();
                videoPlayers.remove(textureId);
                result.success(null);
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    private float getBrightness(){
        float result = registrar.activity().getWindow().getAttributes().screenBrightness;
        if (result < 0) { // the application is using the system brightness
            try {
                result = Settings.System.getInt(registrar.context().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS) / (float)255;
            } catch (Settings.SettingNotFoundException e) {
                result = 1.0f;
                e.printStackTrace();
            }
        }
        return result;
    }

}
