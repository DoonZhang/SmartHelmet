package com.datang.smarthelmet.settings;

/*
TunnelSettingsFragment.java
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
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import com.datang.smarthelmet.R;
import com.datang.smarthelmet.settings.widget.ListSetting;
import com.datang.smarthelmet.settings.widget.SettingListenerBase;
import com.datang.smarthelmet.settings.widget.TextSetting;
import org.linphone.core.tools.Log;

public class TunnelSettingsFragment extends SettingsFragment {
    private View mRootView;
    private LinphonePreferences mPrefs;

    private TextSetting mHost, mPort;
    private ListSetting mMode;

    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.settings_tunnel, container, false);

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
        mHost = mRootView.findViewById(R.id.pref_tunnel_host);

        mPort = mRootView.findViewById(R.id.pref_tunnel_port);
        mPort.setInputType(InputType.TYPE_CLASS_NUMBER);

        mMode = mRootView.findViewById(R.id.pref_tunnel_mode);
    }

    private void setListeners() {
        mHost.setListener(
                new SettingListenerBase() {
                    @Override
                    public void onTextValueChanged(String newValue) {
                        mPrefs.setTunnelHost(newValue);
                    }
                });

        mPort.setListener(
                new SettingListenerBase() {
                    @Override
                    public void onTextValueChanged(String newValue) {
                        try {
                            mPrefs.setTunnelPort(Integer.valueOf(newValue));
                        } catch (NumberFormatException nfe) {
                            Log.e(nfe);
                        }
                    }
                });

        mMode.setListener(
                new SettingListenerBase() {
                    @Override
                    public void onListValueChanged(int position, String newLabel, String newValue) {
                        mPrefs.setTunnelMode(newValue);
                    }
                });
    }

    private void updateValues() {
        mHost.setValue(mPrefs.getTunnelHost());

        mPort.setValue(mPrefs.getTunnelPort());

        mMode.setValue(mPrefs.getTunnelMode());

        setListeners();
    }
}
