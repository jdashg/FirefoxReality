/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.vrbrowser.ui.widgets;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import org.mozilla.vrbrowser.R;
import org.mozilla.geckoview.MediaElement;
import org.mozilla.vrbrowser.browser.Media;
import org.mozilla.vrbrowser.ui.views.MediaSeekBar;
import org.mozilla.vrbrowser.ui.views.UIButton;
import org.mozilla.vrbrowser.ui.views.VolumeControl;

public class MediaControlsWidget extends UIWidget implements MediaElement.Delegate {

    private static final String LOGTAG = "VRB";
    private Media mMedia;
    private MediaSeekBar mSeekBar;
    private VolumeControl mVolumeControl;
    private UIButton mMediaPlayButton;
    private UIButton mMediaSeekBackButton;
    private UIButton mMediaSeekForwardButton;
    private UIButton mMediaProjectionButton;
    private UIButton mMediaVolumeButton;
    private UIButton mMediaBackButton;
    private Drawable mPlayIcon;
    private Drawable mPauseIcon;
    private Drawable mVolumeIcon;
    private Drawable mMutedIcon;

    public MediaControlsWidget(Context aContext) {
        super(aContext);
        initialize(aContext);
    }

    public MediaControlsWidget(Context aContext, AttributeSet aAttrs) {
        super(aContext, aAttrs);
        initialize(aContext);
    }

    public MediaControlsWidget(Context aContext, AttributeSet aAttrs, int aDefStyle) {
        super(aContext, aAttrs, aDefStyle);
        initialize(aContext);
    }

    private void initialize(Context aContext) {
        inflate(aContext, R.layout.media_controls, this);

        mSeekBar = findViewById(R.id.mediaControlSeekBar);
        mVolumeControl = findViewById(R.id.volumeControl);
        mMediaPlayButton = findViewById(R.id.mediaPlayButton);
        mMediaSeekBackButton = findViewById(R.id.mediaSeekBackwardButton);
        mMediaSeekForwardButton = findViewById(R.id.mediaSeekForwardButton);
        mMediaProjectionButton = findViewById(R.id.mediaProjectionButton);
        mMediaVolumeButton = findViewById(R.id.mediaVolumeButton);
        mMediaBackButton = findViewById(R.id.mediaBackButton);
        mPlayIcon = aContext.getDrawable(R.drawable.ic_icon_media_play);
        mPauseIcon = aContext.getDrawable(R.drawable.ic_icon_media_pause);
        mMutedIcon = aContext.getDrawable(R.drawable.ic_icon_meda_volume_muted);
        mVolumeIcon = aContext.getDrawable(R.drawable.ic_icon_media_volume);

        mMediaPlayButton.setOnClickListener(v -> {
            if (mMedia.getPlaybackState() == MediaElement.MEDIA_STATE_PLAYING) {
                mMedia.pause();
            } else {
                mMedia.play();
            }
        });

        mMediaSeekBackButton.setOnClickListener(v -> {
            mMedia.seek(Math.max(0, mMedia.getCurrentTime() - 10.0f));

        });

        mMediaSeekForwardButton.setOnClickListener(v -> {
            double t = mMedia.getCurrentTime() + 30;
            if (mMedia.getMetaData().duration > 0) {
                t = Math.min(mMedia.getMetaData().duration, t);
            }
            mMedia.seek(t);
        });

        mMediaProjectionButton.setOnClickListener(v -> {

        });

        mMediaVolumeButton.setOnClickListener(v -> {
            if (mMedia.isMuted()) {
                mMedia.setMuted(false);
            } else {
                mMedia.setMuted(true);
                mVolumeControl.setVolume(0);
            }
        });

        mMediaBackButton.setOnClickListener(v -> {

        });

        mSeekBar.setDelegate(t -> mMedia.seek(t));
        mVolumeControl.setDelegate(v -> {
            mMedia.setVolume(v);
            if (mMedia.isMuted()) {
                mMedia.setMuted(false);
            }
        });
    }

    @Override
    protected void initializeWidgetPlacement(WidgetPlacement aPlacement) {
        Context context = getContext();
        aPlacement.width = WidgetPlacement.dpDimension(context, R.dimen.media_controls_container_width);
        aPlacement.height = WidgetPlacement.dpDimension(context, R.dimen.media_controls_container_height);
        aPlacement.translationY = WidgetPlacement.unitFromMeters(context, R.dimen.media_controls_world_y);
        aPlacement.translationZ = WidgetPlacement.unitFromMeters(context, R.dimen.media_controls_world_z);
        aPlacement.anchorX = 0.45f;
        aPlacement.anchorY = 0.5f;
        aPlacement.parentAnchorX = 0.5f;
        aPlacement.parentAnchorY = 0.5f;
    }

    @Override
    public void releaseWidget() {
        super.releaseWidget();
    }

    public void setMedia(Media aMedia) {
        if (mMedia == aMedia) {
            return;
        }
        if (mMedia != null) {
            mMedia.setDelegate(null);
        }
        mMedia = aMedia;
        if (mMedia == null) {
            return;
        }
        onMetadataChange(mMedia.getMediaElement(), mMedia.getMetaData());
        onVolumeChange(mMedia.getMediaElement(), mMedia.getVolume(), mMedia.isMuted());
        onTimeChange(mMedia.getMediaElement(), mMedia.getCurrentTime());
        onVolumeChange(mMedia.getMediaElement(), mMedia.getVolume(), mMedia.isMuted());
        onReadyStateChange(mMedia.getMediaElement(), mMedia.getReadyState());
        onPlaybackStateChange(mMedia.getMediaElement(), mMedia.getPlaybackState());
        mMedia.setDelegate(this);
    }

    // Media Element delegate
    @Override
    public void onPlaybackStateChange(MediaElement mediaElement, int playbackState) {
        if (playbackState == MediaElement.MEDIA_STATE_PLAYING) {
            mMediaPlayButton.setImageDrawable(mPauseIcon);
        } else {
            mMediaPlayButton.setImageDrawable(mPlayIcon);
        }
    }


    @Override
    public void onReadyStateChange(MediaElement mediaElement, int readyState) {

    }

    @Override
    public void onMetadataChange(MediaElement mediaElement, MediaElement.Metadata metaData) {
        mSeekBar.setDuration(metaData.duration);
        if (metaData.audioTrackCount == 0) {
            mMediaVolumeButton.setImageDrawable(mMutedIcon);
            mMediaVolumeButton.setEnabled(false);
        } else {
            mMediaVolumeButton.setEnabled(true);
        }
        mSeekBar.setSeekable(metaData.isSeekable);
    }

    @Override
    public void onLoadProgress(MediaElement mediaElement, MediaElement.LoadProgressInfo progressInfo) {
        if (progressInfo.buffered != null) {
            mSeekBar.setBuffered(progressInfo.buffered[progressInfo.buffered.length -1].end);
        }
    }

    @Override
    public void onVolumeChange(MediaElement mediaElement, double volume, boolean muted) {
        if (!mMediaVolumeButton.isEnabled()) {
            return;
        }
        mMediaVolumeButton.setImageDrawable(muted ? mMutedIcon : mVolumeIcon);
        mVolumeControl.setVolume(volume);
        mVolumeControl.setMuted(muted);
    }

    @Override
    public void onTimeChange(MediaElement mediaElement, double time) {
        mSeekBar.setCurrentTime(time);
    }

    @Override
    public void onPlaybackRateChange(MediaElement mediaElement, double rate) {

    }

    @Override
    public void onFullscreenChange(MediaElement mediaElement, boolean fullscreen) {
        if (!fullscreen) {
            hide();
        }
    }

    @Override
    public void onError(MediaElement mediaElement, int code) {

    }

}
