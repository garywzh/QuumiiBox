package org.garywzh.quumiibox.ui;

import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.exoplayer.AspectRatioFrameLayout;
import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaCodecTrackRenderer;
import com.google.android.exoplayer.MediaCodecUtil;
import com.google.android.exoplayer.util.Util;
import com.umeng.analytics.MobclickAgent;

import org.garywzh.quumiibox.AppContext;
import org.garywzh.quumiibox.BuildConfig;
import org.garywzh.quumiibox.R;
import org.garywzh.quumiibox.model.Item;
import org.garywzh.quumiibox.model.VideoInfo;
import org.garywzh.quumiibox.network.NetworkHelper;
import org.garywzh.quumiibox.ui.fragment.CommentListFragment;
import org.garywzh.quumiibox.ui.fragment.ItemHeaderFragment;
import org.garywzh.quumiibox.ui.player.CustomMediaController;
import org.garywzh.quumiibox.ui.player.DemoPlayer;
import org.garywzh.quumiibox.ui.player.DemoPlayer.RendererBuilder;
import org.garywzh.quumiibox.ui.player.EventLogger;
import org.garywzh.quumiibox.ui.player.ExtractorRendererBuilder;
import org.garywzh.quumiibox.util.LogUtils;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;

import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class VideoActivity extends AppCompatActivity implements SurfaceHolder.Callback, DemoPlayer.Listener {
    private static final String TAG = VideoActivity.class.getSimpleName();

    private static final CookieManager defaultCookieManager;

    static {
        defaultCookieManager = new CookieManager();
        defaultCookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
    }

    private EventLogger eventLogger;
    private CustomMediaController mediaController;
    private View shutterView;
    private AspectRatioFrameLayout videoFrame;
    private FrameLayout videoRoot;
    private SurfaceView surfaceView;

    private DemoPlayer player;
    private boolean playerNeedsPrepare;

    private long playerPosition;
    private boolean isFullScreen;

    private DisplayMetrics displayMetrics;
    private String contentUri;
    private boolean shouldPortrait;
    private int contentType;
    private String vid;
    private Subscription mSubscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        isFullScreen = false;
        Item mItem = getIntent().getExtras().getParcelable("item");
        if (mItem != null)
            vid = mItem.vid;

        videoRoot = (FrameLayout) findViewById(R.id.video_root);
        videoRoot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleControlsVisibility();
            }
        });

        shutterView = findViewById(R.id.shutter);
        videoFrame = (AspectRatioFrameLayout) findViewById(R.id.video_frame);

        displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        initVideoRootAspectRatio();

        surfaceView = (SurfaceView) findViewById(R.id.surface_view);
        surfaceView.getHolder().addCallback(this);
        mediaController = new CustomMediaController(this);
        mediaController.setAnchorView(videoRoot);

        CookieHandler currentHandler = CookieHandler.getDefault();
        if (currentHandler != defaultCookieManager) {
            CookieHandler.setDefault(defaultCookieManager);
        }
        final Fragment itemHeaderFragment = ItemHeaderFragment.newInstance(mItem);
        final Fragment commentListFragment = CommentListFragment.newInstance(mItem.blogid);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.headerview, itemHeaderFragment)
                .replace(R.id.comments, commentListFragment)
                .commit();
    }

    @Override
    protected void onStart() {
        super.onStart();
        showControls();
        if (contentUri != null)
            preparePlayer(false);
        else
            fetchVideoInfo();
    }

    private void fetchVideoInfo() {
        mSubscription = NetworkHelper.getApiService()
                .fectchVideoInfo(vid)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<VideoInfo>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        Toast.makeText(AppContext.getInstance(), R.string.toast_connection_exception, Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onNext(VideoInfo videoInfo) {
                        if (videoInfo == null || videoInfo.url == null) {
                            Toast.makeText(AppContext.getInstance(), "cannot get video link", Toast.LENGTH_LONG).show();
                        } else {
                            contentUri = videoInfo.url;
                            LogUtils.d(TAG, contentUri);
                            if (BuildConfig.DEBUG) {
                                Toast.makeText(AppContext.getInstance(), contentUri, Toast.LENGTH_SHORT).show();
                            }
                            contentType = Util.TYPE_OTHER;
                            if (player == null) {
                                preparePlayer(true);
                            } else {
                                player.setBackgrounded(false);
                            }
                        }
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        releasePlayer();
        shutterView.setVisibility(View.VISIBLE);
        if (mSubscription != null && !mSubscription.isUnsubscribed()) {
            mSubscription.unsubscribe();
            mSubscription = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }

    @Override
    public void onBackPressed() {
        if (isFullScreen) {
            toggleFullScreen();
        } else
            super.onBackPressed();
    }

    public void toggleFullScreen() {
        if (!isFullScreen) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

            if (!shouldPortrait)
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            isFullScreen = true;

            videoRoot.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, shouldPortrait ? LinearLayout.LayoutParams.MATCH_PARENT : displayMetrics.widthPixels));
        } else {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(0);
            if (!shouldPortrait)
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            isFullScreen = false;

            initVideoRootAspectRatio();
        }
        mediaController.updateFullScreen();
    }

    private void initVideoRootAspectRatio() {
        final int height = (int) Math.ceil(displayMetrics.widthPixels / 1.777f);
        videoRoot.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height));
    }

    public boolean isFullScreen() {
        return isFullScreen;
    }

    private RendererBuilder getRendererBuilder() {
        String userAgent = Util.getUserAgent(this, "ExoPlayer");
        switch (contentType) {
            case Util.TYPE_OTHER:
                return new ExtractorRendererBuilder(this, userAgent, Uri.parse(contentUri));
            default:
                throw new IllegalStateException("Unsupported type: " + contentType);
        }
    }

    private void preparePlayer(boolean playWhenReady) {
        if (player == null) {
            player = new DemoPlayer(getRendererBuilder());
            player.addListener(this);
            player.addListener(mediaController);
            player.seekTo(playerPosition);
            playerNeedsPrepare = true;
            mediaController.setMediaPlayer(player.getPlayerControl());
            mediaController.setEnabled(true);
            eventLogger = new EventLogger();
            eventLogger.startSession();
            player.addListener(eventLogger);
            player.setInfoListener(eventLogger);
            player.setInternalErrorListener(eventLogger);
        }
        if (playerNeedsPrepare) {
            player.prepare();
            playerNeedsPrepare = false;
        }
        player.setSurface(surfaceView.getHolder().getSurface());
        player.setPlayWhenReady(playWhenReady);
    }

    private void releasePlayer() {
        if (player != null) {
            playerPosition = player.getCurrentPosition();
            player.getPlayerControl().pause();
            player.release();
            player = null;
            eventLogger.endSession();
            eventLogger = null;
        }
    }

    // DemoPlayer.Listener implementation

    @Override
    public void onStateChanged(boolean playWhenReady, int playbackState) {
        if (playbackState == ExoPlayer.STATE_ENDED && isFullScreen()) {
            toggleFullScreen();
        }
    }

    @Override
    public void onError(Exception e) {
        String errorString = null;
        if (e instanceof ExoPlaybackException
                && e.getCause() instanceof MediaCodecTrackRenderer.DecoderInitializationException) {
            // Special case for decoder initialization failures.
            MediaCodecTrackRenderer.DecoderInitializationException decoderInitializationException =
                    (MediaCodecTrackRenderer.DecoderInitializationException) e.getCause();
            if (decoderInitializationException.decoderName == null) {
                if (decoderInitializationException.getCause() instanceof MediaCodecUtil.DecoderQueryException) {
                    errorString = "error_querying_decoders";
                } else if (decoderInitializationException.secureDecoderRequired) {
                    errorString = "error_no_secure_decoder";
                } else {
                    errorString = "error_no_decoder";
                }
            } else {
                errorString = "error_instantiating_decoder";
            }
        }
        if (errorString != null) {
            Toast.makeText(getApplicationContext(), errorString, Toast.LENGTH_LONG).show();
        }
        playerNeedsPrepare = true;
        showControls();
    }

    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees,
                                   float pixelWidthAspectRatio) {
        shutterView.setVisibility(View.GONE);
        float aspectRatio = height == 0 ? 1 : (width * pixelWidthAspectRatio) / height;
        LogUtils.d(TAG, "AspectRatio: " + String.valueOf(aspectRatio));
        //宽高比小于1的视频在全屏时显示为竖直方向
        shouldPortrait = aspectRatio < 1;
        videoFrame.setAspectRatio(aspectRatio);
    }

    // User controls
    private void toggleControlsVisibility() {
        mediaController.toggleControlsVisibility();
    }

    private void showControls() {
        mediaController.showControls();
    }

    // SurfaceHolder.Callback implementation
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (player != null) {
            player.setSurface(holder.getSurface());
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // Do nothing.
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (player != null) {
            player.blockingClearSurface();
        }
    }
}
