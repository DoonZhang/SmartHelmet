package com.datang.smarthelmet.settings;

/*
AudioSettingsFragment.java
Copyright (C) 2019 Belledonne Communications, Grenoble, France

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.Nullable;
import com.datang.smarthelmet.R;
import com.datang.smarthelmet.SmartHelmetManager;
import com.datang.smarthelmet.settings.widget.BasicSetting;
import com.datang.smarthelmet.settings.widget.ListSetting;
import com.datang.smarthelmet.settings.widget.SettingListenerBase;
import com.datang.smarthelmet.settings.widget.SwitchSetting;
import com.datang.smarthelmet.settings.widget.TextSetting;
import org.linphone.core.Core;
import org.linphone.core.CoreListenerStub;
import org.linphone.core.EcCalibratorStatus;
import org.linphone.core.PayloadType;

public class AudioSettingsFragment extends SettingsFragment {
    private View mRootView;
    private LinphonePreferences mPrefs;

    private SwitchSetting mEchoCanceller, mAdaptiveRateControl;
    private TextSetting mMicGain, mSpeakerGain;
    private ListSetting mCodecBitrateLimit;
    private BasicSetting mEchoCalibration, mEchoTester;
    private LinearLayout mAudioCodecs;

    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.settings_audio, container, false);

        loadSettings();

        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        mPrefs = LinphonePreferences.instance();

        updateValues();
    }

    private void loadSettings() {
        mEchoCanceller = mRootView.findViewById(R.id.pref_echo_cancellation);

        mAdaptiveRateControl = mRootView.findViewById(R.id.pref_adaptive_rate_control);

        mMicGain = mRootView.findViewById(R.id.pref_mic_gain_db);
        mMicGain.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        mSpeakerGain = mRootView.findViewById(R.id.pref_playback_gain_db);
        mSpeakerGain.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        mCodecBitrateLimit = mRootView.findViewById(R.id.pref_codec_bitrate_limit);

        mEchoCalibration = mRootView.findViewById(R.id.pref_echo_canceller_calibration);

        mEchoTester = mRootView.findViewById(R.id.pref_echo_tester);

        mAudioCodecs = mRootView.findViewById(R.id.pref_audio_codecs);
    }

    private void setListeners() {
        mEchoCanceller.setListener(
                new SettingListenerBase() {
                    @Override
                    public void onBoolValueChanged(boolean newValue) {
                        mPrefs.setEchoCancellation(newValue);
                    }
                });

        mAdaptiveRateControl.setListener(
                new SettingListenerBase() {
                    @Override
                    public void onBoolValueChanged(boolean newValue) {
                        mPrefs.enableAdaptiveRateControl(newValue);
                    }
                });

        mMicGain.setListener(
                new SettingListenerBase() {
                    @Override
                    public void onTextValueChanged(String newValue) {
                        mPrefs.setMicGainDb(Float.valueOf(newValue));
                    }
                });

        mSpeakerGain.setListener(
                new SettingListenerBase() {
                    @Override
                    public void onTextValueChanged(String newValue) {
                        mPrefs.setPlaybackGainDb(Float.valueOf(newValue));
                    }
                });

        mCodecBitrateLimit.setListener(
                new SettingListenerBase() {
                    @Override
                    public void onListValueChanged(int position, String newLabel, String newValue) {
                        int bitrate = Integer.valueOf(newValue);
                        mPrefs.setCodecBitrateLimit(bitrate);

                        Core core = SmartHelmetManager.getCore();
                        for (final PayloadType pt : core.getAudioPayloadTypes()) {
                            if (pt.isVbr()) {
                                pt.setNormalBitrate(bitrate);
                            }
                        }
                    }
                });

        mEchoCalibration.setListener(
                new SettingListenerBase() {
                    @Override
                    public void onClicked() {
                        mEchoCalibration.setSubtitle(getString(R.string.ec_calibrating));

                        int recordAudio =
                                getActivity()
                                        .getPackageManager()
                                        .checkPermission(
                                                Manifest.permission.RECORD_AUDIO,
                                                getActivity().getPackageName());
                        if (recordAudio == PackageManager.PERMISSION_GRANTED) {
                            startEchoCancellerCalibration();
                        } else {
                            ((SettingsActivity) getActivity())
                                    .requestPermissionIfNotGranted(
                                            Manifest.permission.RECORD_AUDIO);
                        }
                    }
                });

        mEchoTester.setListener(
                new SettingListenerBase() {
                    @Override
                    public void onClicked() {
                        int recordAudio =
                                getActivity()
                                        .getPackageManager()
                                        .checkPermission(
                                                Manifest.permission.RECORD_AUDIO,
                                                getActivity().getPackageName());
                        if (recordAudio == PackageManager.PERMISSION_GRANTED) {
                            if (SmartHelmetManager.getAudioManager().getEchoTesterStatus()) {
                                stopEchoTester();
                            } else {
                                startEchoTester();
                            }
                        } else {
                            ((SettingsActivity) getActivity())
                                    .requestPermissionIfNotGranted(
                                            Manifest.permission.RECORD_AUDIO);
                        }
                    }
                });
    }

    private void updateValues() {
        mEchoCanceller.setChecked(mPrefs.echoCancellationEnabled());

        mAdaptiveRateControl.setChecked(mPrefs.adaptiveRateControlEnabled());

        mMicGain.setValue(mPrefs.getMicGainDb());

        mSpeakerGain.setValue(mPrefs.getPlaybackGainDb());

        mCodecBitrateLimit.setValue(mPrefs.getCodecBitrateLimit());

        if (mPrefs.echoCancellationEnabled()) {
            mEchoCalibration.setSubtitle(
                    String.format(
                            getString(R.string.ec_calibrated),
                            String.valueOf(mPrefs.getEchoCalibration())));
        }

        populateAudioCodecs();

        setListeners();
    }

    private void populateAudioCodecs() {
        mAudioCodecs.removeAllViews();
        Core core = SmartHelmetManager.getCore();
        if (core != null) {
            for (final PayloadType pt : core.getAudioPayloadTypes()) {
                final SwitchSetting codec = new SwitchSetting(getActivity());
                codec.setTitle(pt.getMimeType());
                /* Special case */
                if (pt.getMimeType().equals("mpeg4-generic")) {
                    codec.setTitle("AAC-ELD");
                }

                codec.setSubtitle(pt.getClockRate() + " Hz");
                if (pt.enabled()) {
                    // Never use codec.setChecked(pt.enabled) !
                    codec.setChecked(true);
                }
                codec.setListener(
                        new SettingListenerBase() {
                            @Override
                            public void onBoolValueChanged(boolean newValue) {
                                pt.enable(newValue);
                            }
                        });

                mAudioCodecs.addView(codec);
            }
        }
    }

    private void startEchoTester() {
        SmartHelmetManager.getAudioManager().startEchoTester();
        mEchoTester.setSubtitle("Is running");
    }

    private void stopEchoTester() {
        SmartHelmetManager.getAudioManager().stopEchoTester();
        mEchoTester.setSubtitle("Is stopped");
    }

    private void startEchoCancellerCalibration() {
        if (SmartHelmetManager.getAudioManager().getEchoTesterStatus()) stopEchoTester();
        SmartHelmetManager.getCore()
                .addListener(
                        new CoreListenerStub() {
                            @Override
                            public void onEcCalibrationResult(
                                    Core core, EcCalibratorStatus status, int delayMs) {
                                if (status == EcCalibratorStatus.InProgress) return;
                                core.removeListener(this);
                                SmartHelmetManager.getAudioManager().routeAudioToEarPiece();

                                if (status == EcCalibratorStatus.DoneNoEcho) {
                                    mEchoCalibration.setSubtitle(getString(R.string.no_echo));
                                } else if (status == EcCalibratorStatus.Done) {
                                    mEchoCalibration.setSubtitle(
                                            String.format(
                                                    getString(R.string.ec_calibrated),
                                                    String.valueOf(delayMs)));
                                } else if (status == EcCalibratorStatus.Failed) {
                                    mEchoCalibration.setSubtitle(getString(R.string.failed));
                                }
                                mEchoCanceller.setChecked(status != EcCalibratorStatus.DoneNoEcho);
                                ((AudioManager)
                                                getActivity()
                                                        .getSystemService(Context.AUDIO_SERVICE))
                                        .setMode(AudioManager.MODE_NORMAL);
                            }
                        });
        SmartHelmetManager.getAudioManager().startEcCalibration();
    }
}
