package com.norv.player;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.ext.rtmp.RtmpDataSourceFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;

public class NoRVRTMP extends Service {
    private WindowManager mWindowManager;
    private View mFloatingView;
    private static int LEFTMOST = -100000;
    private static int RIGHTMOST = 100000;

    private static NoRVRTMP _instance = null;
    public static NoRVRTMP getInstance() {
        return _instance;
    }
    public NoRVRTMP() {
        _instance = this;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("InflateParams")
    @Override
    public void onCreate() {
        super.onCreate();

        try {
            mFloatingView = LayoutInflater.from(this).inflate(R.layout.norv_rtmp, null);

            final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);
            params.gravity = Gravity.TOP;
            params.x = LEFTMOST;
            mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            mWindowManager.addView(mFloatingView, params);

            mFloatingView.findViewById(R.id.rtmp_close_button).setOnClickListener(view -> {
                throw new NullPointerException();
            });

            mFloatingView.findViewById(R.id.rtmp_left_button).setOnClickListener(view -> {
                params.x = LEFTMOST;
                mWindowManager.updateViewLayout(mFloatingView, params);
                mFloatingView.findViewById(R.id.rtmp_left_button).setVisibility(View.INVISIBLE);
                mFloatingView.findViewById(R.id.rtmp_right_button).setVisibility(View.VISIBLE);
            });
            mFloatingView.findViewById(R.id.rtmp_right_button).setOnClickListener(view -> {
                params.x = RIGHTMOST;
                mWindowManager.updateViewLayout(mFloatingView, params);
                mFloatingView.findViewById(R.id.rtmp_left_button).setVisibility(View.VISIBLE);
                mFloatingView.findViewById(R.id.rtmp_right_button).setVisibility(View.INVISIBLE);
            });

            BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
            TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
            TrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
            final SimpleExoPlayer player = ExoPlayerFactory.newSimpleInstance(this, trackSelector);
            final PlayerView playerView = mFloatingView.findViewById(R.id.rtmp_norv_player);
            playerView.setPlayer(player);
            playerView.setUseController(false);
            playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH);
            RtmpDataSourceFactory rtmpDataSourceFactory = new RtmpDataSourceFactory();
            final ExtractorMediaSource videoSource = new ExtractorMediaSource
                    .Factory(rtmpDataSourceFactory)
                    .createMediaSource(Uri.parse(BuildConfig.RTMP_SERVER));
            player.prepare(videoSource);
            player.setPlayWhenReady(true);
            player.addListener(new Player.EventListener() {
                @Override
                public void onTimelineChanged(Timeline timeline, Object manifest, int reason) { }
                @Override
                public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) { }
                @Override
                public void onRepeatModeChanged(int repeatMode) { }
                @Override
                public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) { }
                @Override
                public void onPositionDiscontinuity(int reason) { }
                @Override
                public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) { }
                @Override
                public void onSeekProcessed() { }
                @Override
                public void onLoadingChanged(boolean isLoading) { }

                @Override
                public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                    if (!playWhenReady) {
                        player.setPlayWhenReady(true);
                    } else if (playbackState == Player.STATE_ENDED) {
                        player.stop();
                        player.prepare(videoSource);
                        player.setPlayWhenReady(true);
                    }
                }

                @Override
                public void onPlayerError(ExoPlaybackException error) {
                    player.stop();
                    player.prepare(videoSource);
                    player.setPlayWhenReady(true);
                }
            });

            mFloatingView.findViewById(R.id.rtmp_pause_deposition).setOnClickListener(view -> NoRVRTMP.this.pauseDeposition());
            mFloatingView.findViewById(R.id.rtmp_end_deposition).setOnClickListener(view -> NoRVRTMP.this.endDeposition());
        }
        catch (Exception e) {
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mFloatingView != null) mWindowManager.removeView(mFloatingView);
    }

    public void showWindow() {
        if(mFloatingView != null) {
            mFloatingView.setVisibility(View.VISIBLE);
        }
    }
    public void hideWindow() {
        if(mFloatingView != null) {
            mFloatingView.setVisibility(View.INVISIBLE);
        }
    }

    private void pauseDeposition() {
        hideWindow();
        Intent pauseIntent = new Intent(NoRVRTMP.this, NoRVPause.class);
        pauseIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(pauseIntent);
    }

    private void endDeposition() {
        hideWindow();
        Intent endIntent = new Intent(NoRVRTMP.this, NoRVEnd.class);
        endIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(endIntent);
    }
}