package com.datang.smarthelmet.settings;

/*
MenuSettingsFragment.java
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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import com.datang.smarthelmet.R;
import com.datang.smarthelmet.SmartHelmetManager;
import com.datang.smarthelmet.settings.widget.BasicSetting;
import com.datang.smarthelmet.settings.widget.LedSetting;
import com.datang.smarthelmet.settings.widget.SettingListenerBase;
import com.datang.smarthelmet.utils.SmartHelmetUtils;
import org.linphone.core.Core;
import org.linphone.core.ProxyConfig;

public class MenuSettingsFragment extends SettingsFragment {
    private View mRootView;
    private BasicSetting mTunnel,
            mAudio,
            mVideo,
            mCall,
            mChat,
            mNetwork,
            mAdvanced,
            mContact,
            mSystem,
            mTTS,
            mGPS;
    private LinearLayout mAccounts;
    private TextView mAccountsHeader;

    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.settings, container, false);

        loadSettings();
        setListeners();

        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        updateValues();
    }

    private void loadSettings() {
        mAccounts = mRootView.findViewById(R.id.accounts_settings_list);
        mAccountsHeader = mRootView.findViewById(R.id.accounts_settings_list_header);

        mTunnel = mRootView.findViewById(R.id.pref_tunnel);

        mAudio = mRootView.findViewById(R.id.pref_audio);

        mVideo = mRootView.findViewById(R.id.pref_video);

        mCall = mRootView.findViewById(R.id.pref_call);

        mChat = mRootView.findViewById(R.id.pref_chat);

        mNetwork = mRootView.findViewById(R.id.pref_network);

        mAdvanced = mRootView.findViewById(R.id.pref_advanced);

        mContact = mRootView.findViewById(R.id.pref_contact);

        mGPS = mRootView.findViewById(R.id.pref_gps);

        mTTS = mRootView.findViewById(R.id.pref_tts);

        mSystem = mRootView.findViewById(R.id.pref_system);
    }

    private void setListeners() {
        mTunnel.setListener(
                new SettingListenerBase() {
                    @Override
                    public void onClicked() {
                        ((SettingsActivity) getActivity())
                                .showSettings(
                                        new TunnelSettingsFragment(),
                                        getString(R.string.pref_tunnel_title));
                    }
                });

        mAudio.setListener(
                new SettingListenerBase() {
                    @Override
                    public void onClicked() {
                        ((SettingsActivity) getActivity())
                                .showSettings(
                                        new AudioSettingsFragment(),
                                        getString(R.string.pref_audio_title));
                    }
                });

        mVideo.setListener(
                new SettingListenerBase() {
                    @Override
                    public void onClicked() {
                        ((SettingsActivity) getActivity())
                                .showSettings(
                                        new VideoSettingsFragment(),
                                        getString(R.string.pref_video_title));
                    }
                });

        mCall.setListener(
                new SettingListenerBase() {
                    @Override
                    public void onClicked() {
                        ((SettingsActivity) getActivity())
                                .showSettings(
                                        new CallSettingsFragment(),
                                        getString(R.string.pref_call_title));
                    }
                });

        mChat.setListener(
                new SettingListenerBase() {
                    @Override
                    public void onClicked() {
                        ((SettingsActivity) getActivity())
                                .showSettings(
                                        new ChatSettingsFragment(),
                                        getString(R.string.pref_chat_title));
                    }
                });

        mNetwork.setListener(
                new SettingListenerBase() {
                    @Override
                    public void onClicked() {
                        ((SettingsActivity) getActivity())
                                .showSettings(
                                        new NetworkSettingsFragment(),
                                        getString(R.string.pref_network_title));
                    }
                });

        mAdvanced.setListener(
                new SettingListenerBase() {
                    @Override
                    public void onClicked() {
                        ((SettingsActivity) getActivity())
                                .showSettings(
                                        new AdvancedSettingsFragment(),
                                        getString(R.string.pref_advanced_title));
                    }
                });

        mContact.setListener(
                new SettingListenerBase() {
                    @Override
                    public void onClicked() {
                        ((SettingsActivity) getActivity())
                                .showSettings(
                                        new ContactSettingsFragment(),
                                        getString(R.string.pref_contact_title));
                    }
                });

        mGPS.setListener(
                new SettingListenerBase() {
                    @Override
                    public void onClicked() {
                        ((SettingsActivity) getActivity())
                                .showSettings(
                                        new GPSSettingsFragment(),
                                        getString(R.string.pref_gps_title));
                    }
                });

        mTTS.setListener(
                new SettingListenerBase() {
                    @Override
                    public void onClicked() {
                        ((SettingsActivity) getActivity())
                                .showSettings(
                                        new TTSSettingsFragment(),
                                        getString(R.string.pref_tts_title));
                    }
                });

        mSystem.setListener(
                new SettingListenerBase() {
                    @Override
                    public void onClicked() {
                        ((SettingsActivity) getActivity())
                                .showSettings(
                                        new SystemSettingsFragment(),
                                        getString(R.string.pref_system_title));
                    }
                });
    }

    private void updateValues() {
        Core core = SmartHelmetManager.getCore();
        if (core != null) {
            mTunnel.setVisibility(core.tunnelAvailable() ? View.VISIBLE : View.GONE);
            initAccounts(core);
        }

        if (getResources().getBoolean(R.bool.hide_accounts)) {
            mAccounts.setVisibility(View.GONE);
            mAccountsHeader.setVisibility(View.GONE);
        }
    }

    private void initAccounts(Core core) {
        mAccounts.removeAllViews();
        ProxyConfig[] proxyConfigs = core.getProxyConfigList();

        if (proxyConfigs == null || proxyConfigs.length == 0) {
            mAccountsHeader.setVisibility(View.GONE);
        } else {
            mAccountsHeader.setVisibility(View.VISIBLE);
            int i = 0;
            for (ProxyConfig proxyConfig : proxyConfigs) {
                final LedSetting account = new LedSetting(getActivity());
                account.setTitle(
                        SmartHelmetUtils.getDisplayableAddress(proxyConfig.getIdentityAddress()));

                if (proxyConfig.equals(core.getDefaultProxyConfig())) {
                    account.setSubtitle(getString(R.string.default_account_flag));
                }

                switch (proxyConfig.getState()) {
                    case Ok:
                        account.setColor(LedSetting.Color.GREEN);
                        break;
                    case Failed:
                        account.setColor(LedSetting.Color.RED);
                        break;
                    case Progress:
                        account.setColor(LedSetting.Color.ORANGE);
                        break;
                    case None:
                    case Cleared:
                        account.setColor(LedSetting.Color.GRAY);
                        break;
                }

                final int accountIndex = i;
                account.setListener(
                        new SettingListenerBase() {
                            @Override
                            public void onClicked() {
                                ((SettingsActivity) getActivity())
                                        .showAccountSettings(accountIndex, true);
                            }
                        });

                mAccounts.addView(account);
                i += 1;
            }
        }
    }
}
