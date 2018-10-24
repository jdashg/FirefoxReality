package org.mozilla.vrbrowser.ui.views;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import org.mozilla.vrbrowser.R;

public class MediaSeekBar extends LinearLayout implements SeekBar.OnSeekBarChangeListener {

    private SeekBar mSeekBar;
    private TextView mLeftText;
    private TextView mRightText;
    private double mDuration;
    private double mCurrentTime;
    private double mBuffered;
    private boolean mTouching;
    private boolean mSeekable = true;
    private Delegate mDelegate;

    public MediaSeekBar(Context context) {
        super(context);
        initialize();
    }

    public MediaSeekBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public MediaSeekBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    public MediaSeekBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize();
    }

    public interface Delegate {
        void onSeek(double aTargetTime);
    }

    private void initialize() {
        inflate(getContext(), R.layout.media_controls_seek_bar, this);
        mSeekBar = findViewById(R.id.mediaSeekBar);
        mLeftText = findViewById(R.id.mediaSeekLeftLabel);
        mRightText = findViewById(R.id.mediaSeekRightLabel);
        mLeftText.setText("0:00");
        mRightText.setText("0:00");
        mSeekBar.setProgress(100);
        mSeekBar.setOnSeekBarChangeListener(this);
        mSeekBar.setEnabled(false);
    }

    private String formatTime(double aSeconds) {
        final int total = (int)Math.floor(aSeconds);

        final int minutes = total / 60;
        final int seconds = total % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    public void setDelegate(Delegate aDelegate) {
        mDelegate = aDelegate;
    }

    public void setCurrentTime(double aTime) {
        mCurrentTime = aTime;
        if (mSeekable) {
            mLeftText.setText(formatTime(aTime));
            updateProgress();
        }
    }

    public void setDuration(double aDuration) {
        mDuration = aDuration;
        mRightText.setText(formatTime(aDuration));
        if (mDuration > 0 && mSeekable) {
            updateProgress();
            updateBufferedProgress();
            mSeekBar.setEnabled(true);
        }
    }

    public void setSeekable(boolean aSeekable) {
        if (mSeekable == aSeekable) {
            return;
        }
        mSeekable = aSeekable;
        if (mSeekable) {
            mLeftText.setText(formatTime(mCurrentTime));
            if (mDuration > 0) {
                updateProgress();
                updateBufferedProgress();
            }
        } else {
            mLeftText.setText("LIVE");
            mSeekBar.setProgress(mSeekBar.getMax());
        }

        mRightText.setVisibility(mSeekable ? View.VISIBLE : View.GONE);
        mSeekBar.getThumb().mutate().setAlpha(mSeekable ? 255 : 0);
        mSeekBar.setEnabled(mSeekable && mDuration > 0);
    }

    public void setBuffered(double aBuffered) {
        mBuffered = aBuffered;
        if (mSeekable) {
            updateBufferedProgress();
        }
    }

    private void updateProgress() {
        if (mTouching || mDuration <= 0) {
            return;
        }
        double t = mCurrentTime / mDuration;
        mSeekBar.setProgress((int)(t * mSeekBar.getMax()));
    }

    private void updateBufferedProgress() {
        if (mDuration <= 0) {
            mSeekBar.setSecondaryProgress(0);
            return;
        }
        double t = mBuffered / mDuration;
        mSeekBar.setSecondaryProgress((int)(t * mSeekBar.getMax()));
    }

    // SeekBar.OnSeekBarChangeListener
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser && mDelegate != null) {
            double t = mDuration * (double) progress / (double) seekBar.getMax();
            mDelegate.onSeek(t);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mTouching = true;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        mTouching = false;
    }
}
