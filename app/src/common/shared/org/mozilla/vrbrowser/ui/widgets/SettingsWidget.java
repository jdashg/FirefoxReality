/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.vrbrowser.ui.widgets;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;

import org.mozilla.geckoview.GeckoSession;
import org.mozilla.vrbrowser.BuildConfig;
import org.mozilla.vrbrowser.R;
import org.mozilla.vrbrowser.audio.AudioEngine;
import org.mozilla.vrbrowser.browser.SessionStore;
import org.mozilla.vrbrowser.browser.SettingsStore;
import org.mozilla.vrbrowser.ui.views.SettingsButton;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class SettingsWidget extends UIWidget implements WidgetManagerDelegate.FocusChangeListener {

    private static final String LOGTAG = "VRB";

    private AudioEngine mAudio;
    private int mDeveloperOptionsDialogHandle = -1;
    private TextView mBuildText;

    class VersionGestureListener extends GestureDetector.SimpleOnGestureListener {

        private boolean mIsHash;

        @Override
        public boolean onDoubleTap(MotionEvent event) {
            if (mIsHash)
                mBuildText.setText(versionCodeToDate(BuildConfig.VERSION_CODE));
            else
                mBuildText.setText(BuildConfig.GIT_HASH);

            mIsHash = !mIsHash;

            return true;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }
    }

    public SettingsWidget(Context aContext) {
        super(aContext);
        initialize(aContext);
    }

    public SettingsWidget(Context aContext, AttributeSet aAttrs) {
        super(aContext, aAttrs);
        initialize(aContext);
    }

    public SettingsWidget(Context aContext, AttributeSet aAttrs, int aDefStyle) {
        super(aContext, aAttrs, aDefStyle);
        initialize(aContext);
    }

    private void initialize(Context aContext) {
        inflate(aContext, R.layout.settings, this);

        mWidgetManager.addFocusChangeListener(this);

        ImageButton cancelButton = findViewById(R.id.settingsCancelButton);

        cancelButton.setOnClickListener(v -> {
            if (mAudio != null) {
                mAudio.playSound(AudioEngine.Sound.CLICK);
            }

            onDismiss();
        });

        SettingsButton privacyButton = findViewById(R.id.privacyButton);
        privacyButton.setOnClickListener(view -> {
            if (mAudio != null) {
                mAudio.playSound(AudioEngine.Sound.CLICK);
            }

            onSettingsPrivacyClick();
        });

        final TextView crashReportingSwitchText  = findViewById(R.id.crash_reporting_switch_text);
        Switch crashReportingSwitch  = findViewById(R.id.crash_reporting_switch);
        crashReportingSwitch.setChecked(SettingsStore.getInstance(getContext()).isCrashReportingEnabled());
        crashReportingSwitchText.setText(crashReportingSwitch.isChecked() ? getContext().getString(R.string.on) : getContext().getString(R.string.off));
        crashReportingSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
            if (mAudio != null) {
                mAudio.playSound(AudioEngine.Sound.CLICK);
            }

            crashReportingSwitchText.setText(b ? getContext().getString(R.string.on) : getContext().getString(R.string.off));
            onSettingsCrashReportingChange(b);
        });
        crashReportingSwitch.setSoundEffectsEnabled(false);

        final TextView crashTelemetrySwitchText  = findViewById(R.id.telemetry_switch_text);
        Switch telemetrySwitch  = findViewById(R.id.telemetry_switch);
        telemetrySwitch.setChecked(SettingsStore.getInstance(getContext()).isTelemetryEnabled());
        crashTelemetrySwitchText.setText(telemetrySwitch.isChecked() ? getContext().getString(R.string.on) : getContext().getString(R.string.off));
        telemetrySwitch.setOnCheckedChangeListener((compoundButton, b) -> {
            if (mAudio != null) {
                mAudio.playSound(AudioEngine.Sound.CLICK);
            }

            crashTelemetrySwitchText.setText(b ? getContext().getString(R.string.on) : getContext().getString(R.string.off));
            onSettingsTelemetryChange(b);
        });
        telemetrySwitch.setSoundEffectsEnabled(false);

        TextView versionText = findViewById(R.id.versionText);
        try {
            PackageInfo pInfo = getContext().getPackageManager().getPackageInfo(getContext().getPackageName(), 0);
            versionText.setText(String.format(getResources().getString(R.string.settings_version), pInfo.versionName));

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        mBuildText = findViewById(R.id.buildText);
        mBuildText.setText(versionCodeToDate(BuildConfig.VERSION_CODE));

        ViewGroup versionLayout = findViewById(R.id.versionLayout);
        final GestureDetector gd = new GestureDetector(getContext(), new VersionGestureListener());
        versionLayout.setOnTouchListener((view, motionEvent) -> {
            if (gd.onTouchEvent(motionEvent)) {
                return true;
            }
            return view.onTouchEvent(motionEvent);
        });

        SettingsButton reportButton = findViewById(R.id.reportButton);
        reportButton.setOnClickListener(view -> {
            if (mAudio != null) {
                mAudio.playSound(AudioEngine.Sound.CLICK);
            }

            onSettingsReportClick();
        });

        SettingsButton developerOptionsButton = findViewById(R.id.developerOptionsButton);
        developerOptionsButton.setOnClickListener(view -> {
            if (mAudio != null) {
                mAudio.playSound(AudioEngine.Sound.CLICK);
            }

            onDeveloperOptionsClick();
        });

        mAudio = AudioEngine.fromContext(aContext);
    }

    @Override
    public void releaseWidget() {
        mWidgetManager.removeFocusChangeListener(this);

        super.releaseWidget();
    }

    @Override
    protected void initializeWidgetPlacement(WidgetPlacement aPlacement) {
        aPlacement.visible = false;
        aPlacement.width =  WidgetPlacement.dpDimension(getContext(), R.dimen.settings_width);
        aPlacement.height = WidgetPlacement.dpDimension(getContext(), R.dimen.settings_height);
        aPlacement.parentAnchorX = 0.5f;
        aPlacement.parentAnchorY = 0.5f;
        aPlacement.anchorX = 0.5f;
        aPlacement.anchorY = 0.5f;
        aPlacement.translationY = WidgetPlacement.unitFromMeters(getContext(), R.dimen.settings_world_y);
        aPlacement.translationZ = WidgetPlacement.unitFromMeters(getContext(), R.dimen.settings_world_z);
    }

    private void onSettingsCrashReportingChange(boolean isEnabled) {
        SettingsStore.getInstance(getContext()).setCrashReportingEnabled(isEnabled);
    }

    private void onSettingsTelemetryChange(boolean isEnabled) {
        SettingsStore.getInstance(getContext()).setTelemetryEnabled(isEnabled);
        // TODO: Waiting for Telemetry to be merged
    }

    private void onSettingsPrivacyClick() {
        GeckoSession session = SessionStore.get().getCurrentSession();
        if (session == null) {
            int sessionId = SessionStore.get().createSession();
            SessionStore.get().setCurrentSession(sessionId);
        }

        SessionStore.get().loadUri(getContext().getString(R.string.private_policy_url));

        hide();
    }

    private void onSettingsReportClick() {
        String url = SessionStore.get().getCurrentUri();

        GeckoSession session = SessionStore.get().getCurrentSession();
        if (session == null) {
            int sessionId = SessionStore.get().createSession();
            SessionStore.get().setCurrentSession(sessionId);
        }

        try {
            if (url == null) {
                // In case the user had no active sessions when reporting, just leave the URL field empty.
                url = "";
            } else if (url.startsWith("jar:") || url.startsWith("resource:") || url.startsWith("about:")) {
                url = "";
            } else if (SessionStore.get().isHomeUri(url)) {
                // Use the original URL (without any hash).
                url = SessionStore.get().getHomeUri();
            }

            url = URLEncoder.encode(url, "UTF-8");

        } catch (UnsupportedEncodingException e) {
            Log.e(LOGTAG, "Cannot encode URL");
        }
        SessionStore.get().loadUri(getContext().getString(R.string.private_report_url, url));

        hide();
    }

    private void onDeveloperOptionsClick() {
        showDeveloperOptionsDialog();
    }

    /**
     * The version code is composed like: yDDDHHmm
     *  * y   = Double digit year, with 16 substracted: 2017 -> 17 -> 1
     *  * DDD = Day of the year, pad with zeros if needed: September 6th -> 249
     *  * HH  = Hour in day (00-23)
     *  * mm  = Minute in hour
     *
     * For September 6th, 2017, 9:41 am this will generate the versionCode: 12490941 (1-249-09-41).
     *
     * For local debug builds we use a fixed versionCode to not mess with the caching mechanism of the build
     * system. The fixed local build number is 1.
     *
     * @param aVersionCode Application version code minus the leading architecture digit.
     * @return String The converted date in the format yyyy-MM-dd
     */
    private String versionCodeToDate(final int aVersionCode) {
        String versionCode = Integer.toString(aVersionCode);

        String formatted;
        try {
            int year = Integer.parseInt(versionCode.substring(0, 1)) + 2016;
            int dayOfYear = Integer.parseInt(versionCode.substring(1, 4));

            GregorianCalendar cal = (GregorianCalendar)GregorianCalendar.getInstance();
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.DAY_OF_YEAR, dayOfYear);

            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            formatted = format.format(cal.getTime());

        } catch (StringIndexOutOfBoundsException e) {
            formatted = getContext().getString(R.string.settings_version_developer);
        }

        return formatted;
    }

    private void showDeveloperOptionsDialog() {
        mWidgetManager.pushWorldBrightness(this, WidgetManagerDelegate.DEFAULT_DIM_BRIGHTNESS);
        hide();
        UIWidget widget = getChild(mDeveloperOptionsDialogHandle);
        if (widget == null) {
            widget = createChild(DeveloperOptionsWidget.class, false);
            mDeveloperOptionsDialogHandle = widget.getHandle();
            widget.setDelegate(() -> onDeveloperOptionsDialogDismissed());
        }

        widget.show();
    }

    private void onDeveloperOptionsDialogDismissed() {
        mWidgetManager.popWorldBrightness(this);
        show();
    }

    // WindowManagerDelegate.FocusChangeListener
    @Override
    public void onGlobalFocusChanged(View oldFocus, View newFocus) {
        boolean dismiss = false;
        UIWidget widget = getChild(mDeveloperOptionsDialogHandle);
        if (widget != null && oldFocus == widget && !widget.isChild(newFocus) && widget.isVisible()) {
            dismiss = true;

        } else if (oldFocus == this && isVisible()) {
            dismiss = true;
        }

        if (dismiss) {
            onDismiss();
        }
    }

    @Override
    public void show() {
        super.show();

        mWidgetManager.pushWorldBrightness(this, WidgetManagerDelegate.DEFAULT_DIM_BRIGHTNESS);
    }

    @Override
    public void hide() {
        super.hide();

        mWidgetManager.popWorldBrightness(this);
    }

}
