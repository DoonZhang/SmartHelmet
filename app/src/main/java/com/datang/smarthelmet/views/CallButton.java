package com.datang.smarthelmet.views;

/*
CallButton.java
Copyright (C) 2017 Belledonne Communications, Grenoble, France

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

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import com.datang.smarthelmet.SmartHelmetManager;
import com.datang.smarthelmet.settings.LinphonePreferences;
import org.linphone.core.Call;
import org.linphone.core.CallLog;
import org.linphone.core.Core;
import org.linphone.core.ProxyConfig;

@SuppressLint("AppCompatCustomView")
public class CallButton extends ImageView implements OnClickListener, AddressAware {
    private AddressText mAddress;
    private boolean mIsTransfer;

    public CallButton(Context context, AttributeSet attrs) {
        super(context, attrs);

        mIsTransfer = false;
        setOnClickListener(this);
    }

    public void setAddressWidget(AddressText a) {
        mAddress = a;
    }

    public void setIsTransfer(boolean isTransfer) {
        mIsTransfer = isTransfer;
    }

    public void onClick(View v) {
        if (mAddress.getText().length() > 0) {
            if (mIsTransfer) {
                Core core = SmartHelmetManager.getCore();
                if (core.getCurrentCall() == null) {
                    return;
                }
                core.getCurrentCall().transfer(mAddress.getText().toString());
            } else {
                SmartHelmetManager.getCallManager().newOutgoingCall(mAddress);
            }
        } else {
            if (LinphonePreferences.instance().isBisFeatureEnabled()) {
                Core core = SmartHelmetManager.getCore();
                CallLog[] logs = core.getCallLogs();
                CallLog log = null;
                for (CallLog l : logs) {
                    if (l.getDir() == Call.Dir.Outgoing) {
                        log = l;
                        break;
                    }
                }
                if (log == null) {
                    return;
                }

                ProxyConfig lpc = core.getDefaultProxyConfig();
                if (lpc != null && log.getToAddress().getDomain().equals(lpc.getDomain())) {
                    mAddress.setText(log.getToAddress().getUsername());
                } else {
                    mAddress.setText(log.getToAddress().asStringUriOnly());
                }
                mAddress.setSelection(mAddress.getText().toString().length());
                mAddress.setDisplayedName(log.getToAddress().getDisplayName());
            }
        }
    }
}
