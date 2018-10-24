package org.mozilla.vrbrowser.browser;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.mozilla.geckoview.MediaElement;

public class Media implements MediaElement.Delegate {
    private static final String LOGTAG = "VRB";
    private double mDuration = -1.0f;
    private boolean mIsFullscreen = false;
    private double mCurrentTime  = 0.0f;
    private MediaElement.Metadata mMetaData;
    private double mPlaybackRate = 1.0f;
    private int mReadyState = MediaElement.MEDIA_STATE_LOADING;
    private int mPlaybackState = MediaElement.MEDIA_READY_STATE_HAVE_NOTHING;
    private double mVolume = 1.0f;
    private boolean mIsMuted = false;
    private boolean mIsUnloaded = false;
    private org.mozilla.geckoview.MediaElement mMedia;
    private MediaElement.Delegate mDelegate;

    public Media(@NonNull MediaElement aMediaElement) {
        mMedia = aMediaElement;
        aMediaElement.setDelegate(this);
    }

    public void setDelegate(@Nullable MediaElement.Delegate aDelegate) {
        mDelegate = aDelegate;
    }

    public double getDuration() {
        return mDuration;
    }

    public boolean isFullscreen() {
        return mIsFullscreen;
    }

    public double getCurrentTime() {
        return mCurrentTime;
    }

    public MediaElement.Metadata getMetaData() {
        return mMetaData;
    }

    public double getPlaybackRate() {
        return mPlaybackRate;
    }

    public int getReadyState() {
        return mReadyState;
    }

    public int getPlaybackState() {
        return mPlaybackState;
    }

    public double getVolume() {
        return mVolume;
    }

    public boolean isMuted() {
        return mIsMuted;
    }

    public boolean isUnloaded() {
        return mIsUnloaded;
    }

    public MediaElement getMediaElement() {
        return mMedia;
    }

    public void seek(double aTime) {
        mMedia.seek(aTime);
    }

    public void play() {
        mMedia.play();
    }

    public void pause() {
        mMedia.pause();
    }

    public void setVolume(double aVolume) {
        mMedia.setVolume(aVolume);
    }

    public void setMuted(boolean aIsMuted) {
        mMedia.setMuted(aIsMuted);
    }

    public void unload() {
        mIsUnloaded = true;
        mDelegate = null;
    }

    public int getWidth() {
        return mMetaData != null ? (int)mMetaData.width : 0;
    }

    public int getHeight() {
        return mMetaData != null ? (int)mMetaData.height : 0;
    }

    // Media Element delegate
    @Override
    public void onPlaybackStateChange(MediaElement mediaElement, int playbackState) {
        Log.e(LOGTAG, "makelele onPlaybackStateChange:" + playbackState);
        mPlaybackState = playbackState;
        if (mDelegate != null) {
            mDelegate.onPlaybackStateChange(mediaElement, playbackState);
        }
    }

    @Override
    public void onReadyStateChange(MediaElement mediaElement, int readyState) {
        Log.e(LOGTAG, "makelele onReadyStateChange:" + readyState);
        mReadyState = readyState;
        if (mDelegate != null) {
            mDelegate.onReadyStateChange(mediaElement, readyState);
        }
    }

    @Override
    public void onMetadataChange(MediaElement mediaElement, MediaElement.Metadata metaData) {
        Log.e(LOGTAG, "makelele onMetadataChange: " + metaData.currentSource + " " + metaData.width + " " + metaData.height + " " + metaData.duration + " " + metaData.isSeekable + " " + metaData.videoTrackCount + " " + metaData.audioTrackCount);
        mMetaData = metaData;
        if (mDelegate != null) {
            mDelegate.onMetadataChange(mediaElement, metaData);
        }
    }

    @Override
    public void onLoadProgress(MediaElement mediaElement, MediaElement.LoadProgressInfo progressInfo) {
        Log.e(LOGTAG, "makelele onProgress:" + progressInfo.loadedBytes + " " + progressInfo.totalBytes);
        if (progressInfo.buffered != null) {
            for (MediaElement.LoadProgressInfo.TimeRange range: progressInfo.buffered) {
                Log.e(LOGTAG, "makelele buffered: " + range.start + " " + range.end);
            }
        }
        if (mDelegate != null) {
            mDelegate.onLoadProgress(mediaElement, progressInfo);
        }
    }

    @Override
    public void onVolumeChange(MediaElement mediaElement, double volume, boolean muted) {
        Log.e(LOGTAG, "makelele onVolumeChange:" + volume  + " " + muted);
        mVolume = volume;
        mIsMuted = muted;
        if (mDelegate != null) {
            mDelegate.onVolumeChange(mediaElement, volume, muted);
        }
    }

    @Override
    public void onTimeChange(MediaElement mediaElement, double time) {
        Log.e(LOGTAG, "makelele onTimeUpdate:" + time);
        mCurrentTime = time;
        if (mDelegate != null) {
            mDelegate.onTimeChange(mediaElement, time);
        }
    }

    @Override
    public void onPlaybackRateChange(MediaElement mediaElement, double rate) {
        Log.e(LOGTAG, "makelele onPlaybackRateChange:" + rate);
        mPlaybackRate = rate;
        if (mDelegate != null) {
            mDelegate.onPlaybackRateChange(mediaElement, rate);
        }
    }

    @Override
    public void onFullscreenChange(MediaElement mediaElement, boolean fullscreen) {
        Log.e(LOGTAG, "makelele onFullScreenChange:" + fullscreen);
        mIsFullscreen = fullscreen;
        if (mDelegate != null) {
            mDelegate.onFullscreenChange(mediaElement, fullscreen);
        }
    }

    @Override
    public void onError(MediaElement mediaElement, int code) {
        Log.e(LOGTAG, "makelele onError:" + code);
        if (mDelegate != null) {
            mDelegate.onError(mediaElement, code);
        }
    }

}
