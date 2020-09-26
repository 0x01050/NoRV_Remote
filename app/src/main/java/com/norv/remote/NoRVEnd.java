package com.norv.remote;

import android.content.Intent;
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

public class NoRVEnd extends AppCompatActivity {
    private KLoadingSpin stopSpin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.norv_end);

        hideSystemUI();

        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
        final SimpleExoPlayer player = ExoPlayerFactory.newSimpleInstance(this, trackSelector);
        final PlayerView playerView = findViewById(R.id.end_norv_player);
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

        findViewById(R.id.accept_conclude_deposition).setOnClickListener(view -> NoRVEnd.this.acceptEndDeposition());
        findViewById(R.id.cancel_conclude_deposition).setOnClickListener(view -> NoRVEnd.this.cancelEndDeposition());

        stopSpin = findViewById(R.id.norv_end_stop_spin);
    }

    private void acceptEndDeposition() {
        stopSpin.startAnimation();
        stopSpin.setIsVisible(true);
        stopSpin.setVisibility(View.VISIBLE);
        NoRVApi.getInstance().controlDeposition("stopDeposition", null, new NoRVApi.ControlListener() {
            @Override
            public void onSuccess(String respMsg) {
            }

            @Override
            public void onFailure(String errorMsg) {
                stopSpin.stopAnimation();
                stopSpin.setVisibility(View.INVISIBLE);
                Toast.makeText(NoRVEnd.this, errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cancelEndDeposition() {
        NoRVRTMP service = NoRVRTMP.getInstance();
        if(service != null)
            service.showWindow();
        else
            startService(new Intent(NoRVEnd.this, NoRVRTMP.class));
        finish();
    }

    @Override
    public void onBackPressed()
    {
    }

    final Handler handler = new Handler(Looper.getMainLooper());
    final Runnable checkStatus = new Runnable() {
        @Override
        public void run() {
            NoRVApi.getInstance().getStatus(new NoRVApi.StatusListener() {
                @Override
                public void onSuccess(String status, String ignorable, String runningTime, String breaksNumber) {
                    switch (status) {
                        case NoRVConst.STOPPED:
                            gotoHomeScreen();
                            break;
                        case NoRVConst.LOADED:
                            gotoConfirmScreen();
                            break;
                        case NoRVConst.PAUSED:
                            gotoPauseScreen();
                            break;
                        default:
                            handler.postDelayed(checkStatus, NoRVConst.CheckStatusInterval);
                            break;
                    }
                    switch(ignorable) {
                        case "True":
                            findViewById(R.id.accept_conclude_deposition).setEnabled(false);
                            findViewById(R.id.cancel_conclude_deposition).setEnabled(false);
                            break;
                        default:
                            findViewById(R.id.accept_conclude_deposition).setEnabled(true);
                            findViewById(R.id.cancel_conclude_deposition).setEnabled(true);
                            break;
                    }
                }

                @Override
                public void onFailure(String errorMsg) {
                    Log.e("NoRV Get Status", errorMsg);
                    handler.postDelayed(checkStatus, NoRVConst.CheckStatusInterval);
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
        handler.postDelayed(checkStatus, NoRVConst.CheckStatusInterval);
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
        Intent activityIntent = new Intent(NoRVEnd.this, NoRVActivity.class);
        startActivity(activityIntent);
        finish();
    }

    private void gotoConfirmScreen() {
        Intent confirmIntent = new Intent(NoRVEnd.this, NoRVConfirm.class);
        startActivity(confirmIntent);
        finish();
    }

    private void gotoPauseScreen() {
        Intent pauseIntent = new Intent(NoRVEnd.this, NoRVPause.class);
        startActivity(pauseIntent);
        finish();
    }
}
