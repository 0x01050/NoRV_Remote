package com.norv.remote;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.crowdfire.cfalertdialog.CFAlertDialog;
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

public class NoRVConfirm extends AppCompatActivity {

    private KLoadingSpin startSpin = null;
    private KLoadingSpin cancelSpin = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.norv_confirm);

        hideSystemUI();

        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
        final SimpleExoPlayer player = ExoPlayerFactory.newSimpleInstance(this, trackSelector);
        final PlayerView playerView = findViewById(R.id.confirm_norv_player);
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

        findViewById(R.id.confirm_cancel_deposition).setOnClickListener(view -> new CFAlertDialog.Builder(NoRVConfirm.this)
            .setDialogStyle(CFAlertDialog.CFAlertStyle.BOTTOM_SHEET)
            .setTextGravity(Gravity.CENTER_HORIZONTAL)
            .setTitle("Cancel Deposition?")
            .setMessage("Looks like you are going to cancel the deposition. Click Yes to cancel.")
            .setCancelable(false)
            .addButton("YES", -1, -1, CFAlertDialog.CFAlertActionStyle.POSITIVE, CFAlertDialog.CFAlertActionAlignment.JUSTIFIED, (dialog, which) -> {
                dialog.dismiss();
                NoRVConfirm.this.cancelDeposition();
            })
            .addButton("NO", -1, -1, CFAlertDialog.CFAlertActionStyle.NEGATIVE, CFAlertDialog.CFAlertActionAlignment.JUSTIFIED, (dialog, which) -> dialog.dismiss())
            .show()
        );
        findViewById(R.id.confirm_start_deposition).setOnClickListener(view -> new CFAlertDialog.Builder(NoRVConfirm.this)
            .setDialogStyle(CFAlertDialog.CFAlertStyle.BOTTOM_SHEET)
            .setTextGravity(Gravity.CENTER_HORIZONTAL)
            .setTitle("Start Deposition?")
            .setMessage("Looks like you are going to start the deposition. Click Yes to start.")
            .setCancelable(false)
            .addButton("YES", -1, -1, CFAlertDialog.CFAlertActionStyle.POSITIVE, CFAlertDialog.CFAlertActionAlignment.JUSTIFIED, (dialog, which) -> {
                dialog.dismiss();
                NoRVConfirm.this.startDeposition();
            })
            .addButton("NO", -1, -1, CFAlertDialog.CFAlertActionStyle.NEGATIVE, CFAlertDialog.CFAlertActionAlignment.JUSTIFIED, (dialog, which) -> dialog.dismiss())
            .show()
        );

        findViewById(R.id.confirm_dialog_accept).setOnClickListener(view -> {
            NoRVConfirm.this.dismissConfirm();
            NoRVConfirm.this.runDeposition();
        });
        findViewById(R.id.confirm_dialog_cancel).setOnClickListener(view -> NoRVConfirm.this.dismissConfirm());

        startSpin = findViewById(R.id.norv_confirm_start_spin);
        cancelSpin = findViewById(R.id.norv_confirm_cancel_spin);
    }

    private void cancelDeposition()
    {
        cancelSpin.startAnimation();
        cancelSpin.setIsVisible(true);
        cancelSpin.setVisibility(View.VISIBLE);
        NoRVApi.getInstance().controlDeposition("cancelDeposition", null, new NoRVApi.ApiListener() {
            @Override
            public void onSuccess(String respMsg) {
            }

            @Override
            public void onFailure(String errorMsg) {
                cancelSpin.stopAnimation();
                cancelSpin.setVisibility(View.INVISIBLE);
                Toast.makeText(NoRVConfirm.this, errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startDeposition()
    {
        findViewById(R.id.confirm_dialog_container).setVisibility(View.VISIBLE);
    }

    private void dismissConfirm()
    {
        findViewById(R.id.confirm_dialog_container).setVisibility(View.INVISIBLE);
    }

    private void runDeposition()
    {
        startSpin.startAnimation();
        startSpin.setIsVisible(true);
        startSpin.setVisibility(View.VISIBLE);
        NoRVApi.getInstance().controlDeposition("startDeposition", null, new NoRVApi.ApiListener() {
            @Override
            public void onSuccess(String respMsg) {
            }

            @Override
            public void onFailure(String errorMsg) {
                startSpin.stopAnimation();
                startSpin.setVisibility(View.INVISIBLE);
                Toast.makeText(NoRVConfirm.this, errorMsg, Toast.LENGTH_SHORT).show();
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
                    else if(respMsg.equals(NoRVConst.STARTED))
                        gotoRTMPScreen();
                    else if(respMsg.equals(NoRVConst.PAUSED))
                        gotoPauseScreen();
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
        Intent activityIntent = new Intent(NoRVConfirm.this, NoRVActivity.class);
        startActivity(activityIntent);
        finish();
    }

    private void gotoRTMPScreen() {
        NoRVRTMP rtmpService = NoRVRTMP.getInstance();
        if(rtmpService != null)
            rtmpService.showWindow();
        else
            startService(new Intent(NoRVConfirm.this, NoRVRTMP.class));
        finish();
    }

    private void gotoPauseScreen() {
        Intent pauseIntent = new Intent(NoRVConfirm.this, NoRVPause.class);
        startActivity(pauseIntent);
        finish();
    }
}