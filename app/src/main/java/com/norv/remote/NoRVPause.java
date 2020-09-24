package com.norv.remote;

import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.example.kloadingspin.KLoadingSpin;
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

public class NoRVPause extends AppCompatActivity {
    private KLoadingSpin resumeSpin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.norv_pause);

        hideSystemUI();

        final int DELAY = 1000;
        ColorDrawable baseColor = new ColorDrawable(0xff00ff00);
        ColorDrawable targetColor = new ColorDrawable(0xffff0000);
        AnimationDrawable layoutBackground = new AnimationDrawable();
        layoutBackground.addFrame(baseColor, DELAY);
        layoutBackground.addFrame(targetColor, DELAY);
        findViewById(R.id.pause_background).setBackground(layoutBackground);
        layoutBackground.start();

        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
        final SimpleExoPlayer player = ExoPlayerFactory.newSimpleInstance(this, trackSelector);
        final PlayerView playerView = findViewById(R.id.pause_norv_player);
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

        findViewById(R.id.pause_resume_deposition).setOnClickListener(view -> NoRVPause.this.resumeDeposition());

        resumeSpin = findViewById(R.id.norv_pause_resume_spin);
    }

    private void resumeDeposition() {
        resumeSpin.startAnimation();
        resumeSpin.setIsVisible(true);
        resumeSpin.setVisibility(View.VISIBLE);
        NoRVApi.getInstance().controlDeposition("resumeDeposition", null, new NoRVApi.ApiListener() {
            @Override
            public void onSuccess(String respMsg) {
            }

            @Override
            public void onFailure(String errorMsg) {
                resumeSpin.stopAnimation();
                resumeSpin.setVisibility(View.INVISIBLE);
                Toast.makeText(NoRVPause.this, errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onBackPressed()
    {
    }

    Handler handler = new Handler(Looper.getMainLooper());
    int interval = 1000;
    Runnable checkStatus = new Runnable() {
        @Override
        public void run() {
            NoRVApi.getInstance().getStatus(new NoRVApi.ApiListener() {
                @Override
                public void onSuccess(String respMsg) {
                    if(respMsg.equals(NoRVConst.STOPPED))
                        gotoHomeScreen();
                    else if(respMsg.equals(NoRVConst.LOADED))
                        gotoConfirmScreen();
                    else if(respMsg.equals(NoRVConst.STARTED))
                        gotoRTMPScreen();
                    else
                        handler.postDelayed(checkStatus, interval);
                }

                @Override
                public void onFailure(String errorMsg) {
                    Log.e("NoRV Get Status", errorMsg);
                    handler.postDelayed(checkStatus, interval);
                }
            });
        }
    };

    @Override
    protected void onPause() {
        handler.removeCallbacks(checkStatus);
        super.onPause();
    }

    @Override
    protected void onResume() {
        handler.postDelayed(checkStatus, interval);
        hideSystemUI();
        super.onResume();
    }

    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
        ActionBar actionBar = this.getSupportActionBar();
        if(actionBar != null)
            actionBar.hide();
    }

    private void gotoHomeScreen() {
        Intent activityIntent = new Intent(NoRVPause.this, NoRVActivity.class);
        startActivity(activityIntent);
        finish();
    }

    private void gotoConfirmScreen() {
        Intent confirmIntent = new Intent(NoRVPause.this, NoRVConfirm.class);
        startActivity(confirmIntent);
        finish();
    }

    private void gotoRTMPScreen() {
        NoRVRTMP rtmpService = NoRVRTMP.getInstance();
        if(rtmpService != null)
            rtmpService.showWindow();
        else
            startService(new Intent(NoRVPause.this, NoRVRTMP.class));
        finish();
    }

}