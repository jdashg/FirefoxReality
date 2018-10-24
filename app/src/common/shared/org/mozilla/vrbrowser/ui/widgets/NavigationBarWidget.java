/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.vrbrowser.ui.widgets;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.mozilla.geckoview.AllowOrDeny;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoSessionSettings;
import org.mozilla.vrbrowser.*;
import org.mozilla.vrbrowser.audio.AudioEngine;
import org.mozilla.vrbrowser.browser.Media;
import org.mozilla.vrbrowser.browser.SessionStore;
import org.mozilla.vrbrowser.browser.SettingsStore;
import org.mozilla.vrbrowser.utils.AnimationHelper;
import org.mozilla.vrbrowser.ui.views.CustomUIButton;
import org.mozilla.vrbrowser.ui.views.NavigationURLBar;
import org.mozilla.vrbrowser.ui.views.UIButton;
import org.mozilla.vrbrowser.ui.views.UITextButton;

import java.util.ArrayList;
import java.util.Arrays;

public class NavigationBarWidget extends UIWidget implements GeckoSession.NavigationDelegate,
        GeckoSession.ProgressDelegate, GeckoSession.ContentDelegate,
        WidgetManagerDelegate.UpdateListener, SessionStore.SessionChangeListener,
        NavigationURLBar.NavigationURLBarDelegate, VoiceSearchWidget.VoiceSearchDelegate,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String LOGTAG = "VRB";

    private AudioEngine mAudio;
    private UIButton mBackButton;
    private UIButton mForwardButton;
    private UIButton mReloadButton;
    private UIButton mHomeButton;
    private UIButton mServoButton;
    private NavigationURLBar mURLBar;
    private ViewGroup mNavigationContainer;
    private ViewGroup mFullScreenModeContainer;
    private ViewGroup mResizeModeContainer;
    private BrowserWidget mBrowserWidget;
    private boolean mIsLoading;
    private boolean mIsInFullScreenMode;
    private boolean mIsResizing;
    private boolean mIsInVRVideo;
    private Runnable mResizeBackHandler;
    private Runnable mFullScreenBackHandler;
    private Runnable mVRVideoBackHandler;
    private UIButton mResizeEnterButton;
    private UIButton mResizeExitButton;
    private UIButton mFullScreenExitButton;
    private UIButton mBrightnessButton;
    private UIButton mFullScreenResizeButton;
    private UIButton mProjectionButton;
    private UITextButton mPreset0;
    private UITextButton mPreset1;
    private UITextButton mPreset2;
    private UITextButton mPreset3;
    private ArrayList<CustomUIButton> mButtons;
    private VoiceSearchWidget mVoiceSearchWidget;
    private Context mAppContext;
    private SharedPreferences mPrefs;
    private VideoProjectionMenuWidget mVideoProjectionWidget;
    private BrightnessMenuWidget mBrigthnessWidget;
    private MediaControlsWidget mMediaControlsWidget;

    public NavigationBarWidget(Context aContext) {
        super(aContext);
        initialize(aContext);
    }

    public NavigationBarWidget(Context aContext, AttributeSet aAttrs) {
        super(aContext, aAttrs);
        initialize(aContext);
    }

    public NavigationBarWidget(Context aContext, AttributeSet aAttrs, int aDefStyle) {
        super(aContext, aAttrs, aDefStyle);
        initialize(aContext);
    }

    private void initialize(Context aContext) {
        mAppContext = aContext.getApplicationContext();
        inflate(aContext, R.layout.navigation_bar, this);
        mAudio = AudioEngine.fromContext(aContext);
        mBackButton = findViewById(R.id.backButton);
        mForwardButton = findViewById(R.id.forwardButton);
        mReloadButton = findViewById(R.id.reloadButton);
        mHomeButton = findViewById(R.id.homeButton);
        mServoButton = findViewById(R.id.servoButton);
        mURLBar = findViewById(R.id.urlBar);
        mNavigationContainer = findViewById(R.id.navigationBarContainer);
        mFullScreenModeContainer = findViewById(R.id.fullScreenModeContainer);
        mResizeModeContainer = findViewById(R.id.resizeModeContainer);
        mFullScreenExitButton = findViewById(R.id.fullScreenExitButton);
        mBrightnessButton = findViewById(R.id.brightnessButton);
        mFullScreenResizeButton = findViewById(R.id.fullScreenResizeEnterButton);
        mProjectionButton = findViewById(R.id.projectionButton);


        mResizeBackHandler = () -> exitResizeMode(true);

        mFullScreenBackHandler = this::exitFullScreenMode;
        mVRVideoBackHandler = this::exitVRVideo;

        mBackButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                v.requestFocusFromTouch();
                if (SessionStore.get().canGoBack())
                    SessionStore.get().goBack();
                else if (SessionStore.get().canUnstackSession())
                    SessionStore.get().unstackSession();

                if (mAudio != null) {
                    mAudio.playSound(AudioEngine.Sound.BACK);
                }
            }
        });

        mForwardButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                v.requestFocusFromTouch();
                SessionStore.get().goForward();
                if (mAudio != null) {
                    mAudio.playSound(AudioEngine.Sound.CLICK);
                }
            }
        });

        mReloadButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                v.requestFocusFromTouch();
                if (mIsLoading) {
                    SessionStore.get().stop();
                } else {
                    SessionStore.get().reload();
                }
                if (mAudio != null) {
                    mAudio.playSound(AudioEngine.Sound.CLICK);
                }
            }
        });

        mHomeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                v.requestFocusFromTouch();
                SessionStore.get().loadUri(SessionStore.get().getHomeUri());
                if (mAudio != null) {
                    mAudio.playSound(AudioEngine.Sound.CLICK);
                }
            }
        });

        mServoButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                v.requestFocusFromTouch();
                SessionStore.get().toggleServo();
                if (mAudio != null) {
                    mAudio.playSound(AudioEngine.Sound.CLICK);
                }
            }
        });

        mResizeEnterButton = findViewById(R.id.resizeEnterButton);
        mResizeExitButton = findViewById(R.id.resizeExitButton);
        mPreset0 = findViewById(R.id.resizePreset0);
        mPreset1 = findViewById(R.id.resizePreset1);
        mPreset2 = findViewById(R.id.resizePreset2);
        mPreset3 = findViewById(R.id.resizePreset3);

        mResizeEnterButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                view.requestFocusFromTouch();
                enterResizeMode();
                if (mAudio != null) {
                    mAudio.playSound(AudioEngine.Sound.CLICK);
                }
            }
        });

        mResizeExitButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                view.requestFocusFromTouch();
                exitResizeMode(true);
                if (mAudio != null) {
                    mAudio.playSound(AudioEngine.Sound.CLICK);
                }
            }
        });

        mFullScreenResizeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                view.requestFocusFromTouch();
                enterResizeMode();
                if (mAudio != null) {
                    mAudio.playSound(AudioEngine.Sound.CLICK);
                }
            }
        });

        mFullScreenExitButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                view.requestFocusFromTouch();
                exitFullScreenMode();
                if (mAudio != null) {
                    mAudio.playSound(AudioEngine.Sound.CLICK);
                }
            }
        });

        mProjectionButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                view.requestFocusFromTouch();
                if (mAudio != null) {
                    mAudio.playSound(AudioEngine.Sound.CLICK);
                }

                boolean visible = mVideoProjectionWidget.getPlacement().visible;
                closeFloatingMenus();
                if (!visible) {
                    mVideoProjectionWidget.getPlacement().visible = true;
                    mWidgetManager.updateWidget(mVideoProjectionWidget);
                }
            }
        });

        mBrightnessButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                view.requestFocusFromTouch();
                if (mAudio != null) {
                    mAudio.playSound(AudioEngine.Sound.CLICK);
                }
                boolean visible = mBrigthnessWidget.getPlacement().visible;
                closeFloatingMenus();
                if (!visible) {
                    mBrigthnessWidget.getPlacement().visible = true;
                    float anchor = 0.5f + (float)mBrightnessButton.getMeasuredWidth() / (float)NavigationBarWidget.this.getMeasuredWidth();
                    mBrigthnessWidget.getPlacement().parentAnchorX = anchor;
                    mWidgetManager.updateWidget(mBrigthnessWidget);
                }
            }
        });



        mPreset0.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                view.requestFocusFromTouch();
                setResizePreset(0.5f);
                if (mAudio != null) {
                    mAudio.playSound(AudioEngine.Sound.CLICK);
                }
            }
        });

        mPreset1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                view.requestFocusFromTouch();
                setResizePreset(1.0f);
                if (mAudio != null) {
                    mAudio.playSound(AudioEngine.Sound.CLICK);
                }
            }
        });

        mPreset2.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                view.requestFocusFromTouch();
                setResizePreset(2.0f);
                if (mAudio != null) {
                    mAudio.playSound(AudioEngine.Sound.CLICK);
                }
            }
        });

        mPreset3.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                view.requestFocusFromTouch();
                setResizePreset(3.0f);
                if (mAudio != null) {
                    mAudio.playSound(AudioEngine.Sound.CLICK);
                }
            }
        });

        mButtons = new ArrayList<>();
        mButtons.addAll(Arrays.<CustomUIButton>asList(
                mBackButton, mForwardButton, mReloadButton, mHomeButton, mResizeEnterButton, mResizeExitButton,
                mServoButton, mPreset0, mPreset1, mPreset2, mPreset3));

        mURLBar.setDelegate(this);

        SessionStore.get().addNavigationListener(this);
        SessionStore.get().addProgressListener(this);
        SessionStore.get().addContentListener(this);
        mWidgetManager.addUpdateListener(this);

        mVoiceSearchWidget = createChild(VoiceSearchWidget.class, false);
        mVoiceSearchWidget.setDelegate(this);

        SessionStore.get().addSessionChangeListener(this);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(mAppContext);
        mPrefs.registerOnSharedPreferenceChangeListener(this);
        updateServoButton();
    }

    @Override
    public void releaseWidget() {
        mWidgetManager.removeUpdateListener(this);
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
        SessionStore.get().removeNavigationListener(this);
        SessionStore.get().removeProgressListener(this);
        SessionStore.get().removeContentListener(this);
        SessionStore.get().removeSessionChangeListener(this);
        mBrowserWidget = null;
        super.releaseWidget();
    }

    @Override
    protected void initializeWidgetPlacement(WidgetPlacement aPlacement) {
        aPlacement.width = WidgetPlacement.dpDimension(getContext(), R.dimen.navigation_bar_width);
        aPlacement.worldWidth = WidgetPlacement.floatDimension(getContext(), R.dimen.browser_world_width);
        aPlacement.height = WidgetPlacement.dpDimension(getContext(), R.dimen.navigation_bar_height);
        aPlacement.anchorX = 0.5f;
        aPlacement.anchorY = 1.0f;
        aPlacement.parentAnchorX = 0.5f;
        aPlacement.parentAnchorY = 0.0f;
        aPlacement.translationY = -35;
        aPlacement.opaque = false;
    }

    public void setBrowserWidget(BrowserWidget aWidget) {
        if (aWidget != null) {
            mWidgetPlacement.parentHandle = aWidget.getHandle();
        }
        mBrowserWidget = aWidget;
    }

    private void enterFullScreenMode() {
        if (mIsInFullScreenMode) {
            return;
        }
        mWidgetManager.pushBackHandler(mFullScreenBackHandler);
        mIsInFullScreenMode = true;
        AnimationHelper.fadeIn(mFullScreenModeContainer, AnimationHelper.FADE_ANIMATION_DURATION, null);

        AnimationHelper.fadeOut(mNavigationContainer, 0, null);

        mWidgetManager.pushWorldBrightness(this, WidgetManagerDelegate.DEFAULT_DIM_BRIGHTNESS);

        mWidgetManager.setTrayVisible(false);

        if (mVideoProjectionWidget == null) {
            mVideoProjectionWidget = new VideoProjectionMenuWidget(getContext());
            mVideoProjectionWidget.setParentWidget(this);
            mWidgetManager.addWidget(mVideoProjectionWidget);
            mVideoProjectionWidget.setDelegate((projection )-> {
                enterVRVideo(projection);
            });
        }
        if (mBrigthnessWidget == null) {
            mBrigthnessWidget = new BrightnessMenuWidget(getContext());
            mBrigthnessWidget.setParentWidget(this);
            mWidgetManager.addWidget(mBrigthnessWidget);
        }
        closeFloatingMenus();
        mWidgetManager.pushWorldBrightness(mBrigthnessWidget, mBrigthnessWidget.getSelectedBrightness());
    }

    private void exitFullScreenMode() {
        if (!mIsInFullScreenMode) {
            return;
        }
        mIsInFullScreenMode = false;
        mWidgetManager.popBackHandler(mFullScreenBackHandler);

        AnimationHelper.fadeIn(mNavigationContainer, AnimationHelper.FADE_ANIMATION_DURATION, null);

        mWidgetManager.popWorldBrightness(this);
        AnimationHelper.fadeOut(mFullScreenModeContainer, 0, null);

        if (SessionStore.get().isInFullScreen()) {
            SessionStore.get().exitFullScreen();
        }

        mWidgetManager.setTrayVisible(true);
        closeFloatingMenus();
        mWidgetManager.popWorldBrightness(mBrigthnessWidget);
    }

    private void enterResizeMode() {
        if (mIsResizing) {
            return;
        }
        mIsResizing = true;
        mWidgetManager.startWidgetResize(mBrowserWidget);
        AnimationHelper.fadeIn(mResizeModeContainer, AnimationHelper.FADE_ANIMATION_DURATION, null);
        if (mIsInFullScreenMode) {
            AnimationHelper.fadeOut(mFullScreenModeContainer, 0, null);
        } else {
            AnimationHelper.fadeOut(mNavigationContainer, 0, null);
        }
        mWidgetManager.pushBackHandler(mResizeBackHandler);
        closeFloatingMenus();
    }

    private void exitResizeMode(boolean aCommitChanges) {
        if (!mIsResizing) {
            return;
        }
        mIsResizing = false;
        mWidgetManager.finishWidgetResize(mBrowserWidget);
        if (mIsInFullScreenMode) {
            AnimationHelper.fadeIn(mFullScreenModeContainer, AnimationHelper.FADE_ANIMATION_DURATION, null);
        } else {
            AnimationHelper.fadeIn(mNavigationContainer, AnimationHelper.FADE_ANIMATION_DURATION, null);
        }
        AnimationHelper.fadeOut(mResizeModeContainer, 0, new Runnable() {
            @Override
            public void run() {
                onWidgetUpdate(mBrowserWidget);
            }
        });
        mWidgetManager.popBackHandler(mResizeBackHandler);
        closeFloatingMenus();
    }

    private void enterVRVideo(@VideoProjectionMenuWidget.VideoProjectionFlags int aProjection) {
        if (mIsInVRVideo) {
            return;
        }
        mIsInVRVideo = true;
        mWidgetManager.pushBackHandler(mVRVideoBackHandler);

        Media fullscreenMedia = SessionStore.get().getFullScreenVideo();

        mWidgetPlacement.visible = false;
        mWidgetManager.updateWidget(this);
        mBrowserWidget.getPlacement().visible = false;
        if (fullscreenMedia != null && fullscreenMedia.getWidth() > 0 && fullscreenMedia.getHeight() > 0) {
            mBrowserWidget.enableVRVideoMode(fullscreenMedia.getWidth(), fullscreenMedia.getHeight());
        }
        mWidgetManager.updateWidget(mBrowserWidget);

        closeFloatingMenus();

        if (mMediaControlsWidget == null) {
            mMediaControlsWidget = new MediaControlsWidget(getContext());
            mWidgetManager.addWidget(mMediaControlsWidget);
        }
        mMediaControlsWidget.setMedia(fullscreenMedia);
        mMediaControlsWidget.getPlacement().visible = true;
        mWidgetManager.updateWidget(mMediaControlsWidget);
        mWidgetManager.showVRVideo(mBrowserWidget.getHandle(), aProjection);
    }

    private void exitVRVideo() {
        if (!mIsInVRVideo) {
            return;
        }
        mIsInVRVideo = false;
        mWidgetManager.popBackHandler(mVRVideoBackHandler);
        mWidgetManager.hideVRVideo();

        mWidgetPlacement.visible = true;
        mWidgetManager.updateWidget(this);
        mBrowserWidget.disableVRVideoMode();
        mBrowserWidget.getPlacement().visible = true;
        mWidgetManager.updateWidget(mBrowserWidget);
    }

    private void setResizePreset(float aResizeMode) {
        mBrowserWidget.setBrowserSize(
                SettingsStore.getInstance(getContext()).getWindowWidth(),
                SettingsStore.getInstance(getContext()).getWindowHeight(),
                aResizeMode);
    }

    public boolean isInFocusMode() {
        return mIsInFullScreenMode;
    }

    public void showVoiceSearch() {
        mURLBar.showVoiceSearch(true);
    }

    public void updateServoButton() {
        if (SettingsStore.getInstance(mAppContext).isServoEnabled()) {
            mServoButton.setVisibility(View.VISIBLE);
        } else {
            mServoButton.setVisibility(View.GONE);
        }
    }

    private void closeFloatingMenus() {
        if (mVideoProjectionWidget!= null && mVideoProjectionWidget.getPlacement().visible) {
            mVideoProjectionWidget.getPlacement().visible = false;
            mWidgetManager.updateWidget(mVideoProjectionWidget);
        }
        if (mBrigthnessWidget != null && mBrigthnessWidget.getPlacement().visible) {
            mBrigthnessWidget.getPlacement().visible = false;
            mWidgetManager.updateWidget(mBrigthnessWidget);
        }
    }

    @Override
    public GeckoResult<GeckoSession> onNewSession(@NonNull GeckoSession aSession, @NonNull String aUri) {
        return null;
    }

    @Override
    public GeckoResult<String> onLoadError(GeckoSession session, String uri, int category, int error) {
        return null;
    }

    public void release() {
        SessionStore.get().removeNavigationListener(this);
        SessionStore.get().removeProgressListener(this);
    }

    @Override
    public void onLocationChange(GeckoSession session, String url) {
        if (mURLBar != null) {
            Log.d(LOGTAG, "Got location change");
            mURLBar.setURL(url);
            mReloadButton.setEnabled(true);
        }
    }

    @Override
    public void onCanGoBack(GeckoSession aSession, boolean canGoBack) {
        if (mBackButton != null) {
            boolean enableBackButton = SessionStore.get().canUnstackSession() | canGoBack;

            Log.d(LOGTAG, "Got onCanGoBack: " + (enableBackButton ? "true" : "false"));
            mBackButton.setEnabled(enableBackButton);
            mBackButton.setHovered(false);
            mBackButton.setClickable(enableBackButton);
        }
    }

    @Override
    public void onCanGoForward(GeckoSession aSession, boolean canGoForward) {
        if (mForwardButton != null) {
            Log.d(LOGTAG, "Got onCanGoForward: " + (canGoForward ? "true" : "false"));
            mForwardButton.setEnabled(canGoForward);
            mForwardButton.setHovered(false);
            mForwardButton.setClickable(canGoForward);
        }
    }

    @Override
    public @Nullable GeckoResult<AllowOrDeny> onLoadRequest(GeckoSession aSession, @NonNull LoadRequest aRequest) {
        if (mURLBar != null) {
            Log.d(LOGTAG, "Got onLoadUri");
            mURLBar.setURL(aRequest.uri);
        }

        final GeckoResult<AllowOrDeny> result = new GeckoResult<>();

        Uri uri = Uri.parse(aRequest.uri);
        if ("file".equalsIgnoreCase(uri.getScheme())
                && !mWidgetManager.isPermissionGranted(android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
            mWidgetManager.requestPermission(
                    aRequest.uri,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    new GeckoSession.PermissionDelegate.Callback() {
                        @Override
                        public void grant() {
                            result.complete(AllowOrDeny.ALLOW);
                        }

                        @Override
                        public void reject() {
                            result.complete(AllowOrDeny.DENY);
                        }
                    });
            return result;
        }

        result.complete(AllowOrDeny.ALLOW);
        return result;
    }

    // Progress Listener
    @Override
    public void onPageStart(GeckoSession aSession, String aUri) {
        if (mURLBar != null) {
            Log.d(LOGTAG, "Got onPageStart");
            mURLBar.setURL(aUri);
        }
        mIsLoading = true;
        mURLBar.setIsLoading(true);
        if (mReloadButton != null) {
            mReloadButton.setImageResource(R.drawable.ic_icon_exit);
        }
        if (mIsInFullScreenMode && !mIsResizing) {
            AnimationHelper.fadeIn(mURLBar, 0, null);
        }
    }

    @Override
    public void onPageStop(GeckoSession aSession, boolean b) {
        mIsLoading = false;
        mURLBar.setIsLoading(false);
        if (mReloadButton != null) {
            mReloadButton.setImageResource(R.drawable.ic_icon_reload);
        }
        if (mIsInFullScreenMode) {
            AnimationHelper.fadeOut(mURLBar, 0, null);
        }
    }

    @Override
    public void onProgressChange(GeckoSession session, int progress) {

    }

    @Override
    public void onSecurityChange(GeckoSession geckoSession, SecurityInformation securityInformation) {
        if (mURLBar != null) {
            boolean isSecure = securityInformation.isSecure;
            mURLBar.setIsInsecure(!isSecure);
        }
    }

    // Content delegate

    @Override
    public void onTitleChange(GeckoSession session, String title) {

    }

    @Override
    public void onFocusRequest(GeckoSession session) {

    }

    @Override
    public void onCloseRequest(GeckoSession session) {

    }

    @Override
    public void onFullScreen(GeckoSession session, boolean aFullScreen) {
        if (aFullScreen) {
            if (!mIsInFullScreenMode) {
                enterFullScreenMode();
            }
            if (mIsResizing) {
                exitResizeMode(false);
            }

        } else {
            exitFullScreenMode();
        }
    }

    @Override
    public void onContextMenu(GeckoSession session, int screenX, int screenY, String uri, int elementType, String elementSrc) {

    }

    @Override
    public void onExternalResponse(GeckoSession session, GeckoSession.WebResponseInfo response) {

    }

    @Override
    public void onCrash(GeckoSession session) {

    }

    // WidgetManagerDelegate.UpdateListener
    @Override
    public void onWidgetUpdate(Widget aWidget) {
        if (aWidget != mBrowserWidget || mIsResizing) {
            return;
        }

        // Browser window may have been resized, adjust the navigation bar
        float targetWidth = aWidget.getPlacement().worldWidth;
        float defaultWidth = WidgetPlacement.floatDimension(getContext(), R.dimen.browser_world_width);
        targetWidth = Math.max(defaultWidth, targetWidth);
        targetWidth = Math.min(targetWidth, defaultWidth * 1.5f);

        float ratio = targetWidth / defaultWidth;
        mWidgetPlacement.worldWidth = targetWidth;
        mWidgetPlacement.width = (int) (WidgetPlacement.dpDimension(getContext(), R.dimen.navigation_bar_width) * ratio);
        mWidgetManager.updateWidget(this);
    }

    // SessionStore.SessionChangeListener
    @Override
    public void onNewSession(GeckoSession aSession, int aId) {

    }

    @Override
    public void onRemoveSession(GeckoSession aSession, int aId) {

    }

    @Override
    public void onCurrentSessionChange(GeckoSession aSession, int aId) {
        boolean isPrivateMode  = aSession.getSettings().getBoolean(GeckoSessionSettings.USE_PRIVATE_MODE);
        mURLBar.setPrivateMode(isPrivateMode);

        for (CustomUIButton button : mButtons) {
            button.setPrivateMode(isPrivateMode);
        }
    }

    @Override
    public void OnVoiceSearchClicked() {
        if (mVoiceSearchWidget.getPlacement().visible) {
            mVoiceSearchWidget.hide();

        } else {
            mVoiceSearchWidget.show();
        }
    }

    @Override
    public void OnVoiceSearchResult(String transcription, float confidance) {
        mURLBar.handleURLEdit(transcription);
    }

    @Override
    public void OnVoiceSearchCanceled() {
        // Nothing to do yet
    }

    @Override
    public void OnVoiceSearchError() {
        // Nothing to do yet
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key == mAppContext.getString(R.string.settings_key_servo)) {
            updateServoButton();
        }
    }
}
