package com.datang.smarthelmet.notifications;

/*
NotificationsManager.java
Copyright (C) 2018  Belledonne Communications, Grenoble, France

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

import static android.content.Context.NOTIFICATION_SERVICE;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import com.datang.smarthelmet.R;
import com.datang.smarthelmet.SmartHelmetManager;
import com.datang.smarthelmet.SmartHelmetService;
import com.datang.smarthelmet.activities.DialerActivity;
import com.datang.smarthelmet.call.CallActivity;
import com.datang.smarthelmet.call.CallIncomingActivity;
import com.datang.smarthelmet.call.CallOutgoingActivity;
import com.datang.smarthelmet.chat.ChatActivity;
import com.datang.smarthelmet.compatibility.Compatibility;
import com.datang.smarthelmet.contacts.ContactsManager;
import com.datang.smarthelmet.contacts.SmartHelmetContact;
import com.datang.smarthelmet.history.HistoryActivity;
import com.datang.smarthelmet.settings.LinphonePreferences;
import com.datang.smarthelmet.utils.FileUtils;
import com.datang.smarthelmet.utils.ImageUtils;
import com.datang.smarthelmet.utils.MediaScannerListener;
import com.datang.smarthelmet.utils.SmartHelmetUtils;
import java.io.File;
import java.util.HashMap;
import org.linphone.core.Address;
import org.linphone.core.Call;
import org.linphone.core.ChatMessage;
import org.linphone.core.ChatRoom;
import org.linphone.core.ChatRoomCapabilities;
import org.linphone.core.Content;
import org.linphone.core.Core;
import org.linphone.core.CoreListenerStub;
import org.linphone.core.Reason;
import org.linphone.core.tools.Log;

public class NotificationsManager {
    private static final int SERVICE_NOTIF_ID = 1;
    private static final int MISSED_CALLS_NOTIF_ID = 2;
    private static final int IN_APP_NOTIF_ID = 3;

    private final Context mContext;
    private final NotificationManager mNM;
    private final HashMap<String, Notifiable> mChatNotifMap;
    private final HashMap<String, Notifiable> mCallNotifMap;
    private int mLastNotificationId;
    private final Notification mServiceNotification;
    private int mCurrentForegroundServiceNotification;
    private String mCurrentChatRoomAddress;
    private CoreListenerStub mListener;

    public NotificationsManager(Context context) {
        mContext = context;
        mChatNotifMap = new HashMap<>();
        mCallNotifMap = new HashMap<>();
        mCurrentForegroundServiceNotification = 0;
        mCurrentChatRoomAddress = null;

        mNM = (NotificationManager) mContext.getSystemService(NOTIFICATION_SERVICE);
        mNM.cancelAll();

        mLastNotificationId = 5; // Do not conflict with hardcoded notifications ids !

        Compatibility.createNotificationChannels(mContext);

        Bitmap bm = null;
        try {
            bm = BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.ic_launcher);
        } catch (Exception e) {
            Log.e(e);
        }

        Intent notifIntent = new Intent(mContext, DialerActivity.class);
        notifIntent.putExtra("Notification", true);
        addFlagsToIntent(notifIntent);

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        mContext, SERVICE_NOTIF_ID, notifIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mServiceNotification =
                Compatibility.createNotification(
                        mContext,
                        mContext.getString(R.string.service_name),
                        "",
                        R.drawable.linphone_notification_icon,
                        R.mipmap.ic_launcher,
                        bm,
                        pendingIntent,
                        Notification.PRIORITY_MIN);

        if (isServiceNotificationDisplayed()) {
            startForeground();
        }

        mListener =
                new CoreListenerStub() {
                    @Override
                    public void onMessageSent(Core core, ChatRoom room, ChatMessage message) {
                        if (room.hasCapability(ChatRoomCapabilities.OneToOne.toInt())) {
                            Compatibility.createChatShortcuts(mContext);
                        }
                    }

                    @Override
                    public void onMessageReceived(
                            Core core, final ChatRoom cr, final ChatMessage message) {
                        if (message.isOutgoing()
                                || mContext.getResources().getBoolean(R.bool.disable_chat)
                                || mContext.getResources()
                                        .getBoolean(R.bool.disable_chat_message_notification)) {
                            return;
                        }

                        if (mCurrentChatRoomAddress != null
                                && mCurrentChatRoomAddress.equals(
                                        cr.getPeerAddress().asStringUriOnly())) {
                            Log.i(
                                    "[Notifications Manager] Message received for currently displayed chat room, do not make a notification");
                            return;
                        }

                        if (message.getErrorInfo() != null
                                && message.getErrorInfo().getReason()
                                        == Reason.UnsupportedContent) {
                            Log.w(
                                    "[Notifications Manager] Message received but content is unsupported, do not notify it");
                            return;
                        }

                        if (!message.hasTextContent()
                                && message.getFileTransferInformation() == null) {
                            Log.w(
                                    "[Notifications Manager] Message has no text or file transfer information to display, ignoring it...");
                            return;
                        }

                        final Address from = message.getFromAddress();
                        final SmartHelmetContact contact =
                                ContactsManager.getInstance().findContactFromAddress(from);
                        final String textMessage =
                                (message.hasTextContent())
                                        ? message.getTextContent()
                                        : mContext.getString(
                                                R.string.content_description_incoming_file);

                        String file = null;
                        for (Content c : message.getContents()) {
                            if (c.isFile()) {
                                file = c.getFilePath();
                                SmartHelmetManager.getInstance()
                                        .getMediaScanner()
                                        .scanFile(
                                                new File(file),
                                                new MediaScannerListener() {
                                                    @Override
                                                    public void onMediaScanned(
                                                            String path, Uri uri) {
                                                        createNotification(
                                                                cr,
                                                                contact,
                                                                from,
                                                                textMessage,
                                                                message.getTime(),
                                                                uri,
                                                                FileUtils.getMimeFromFile(path));
                                                    }
                                                });
                                break;
                            }
                        }

                        if (file == null) {
                            createNotification(
                                    cr, contact, from, textMessage, message.getTime(), null, null);
                        }

                        if (cr.hasCapability(ChatRoomCapabilities.OneToOne.toInt())) {
                            Compatibility.createChatShortcuts(mContext);
                        }
                    }
                };
    }

    public void onCoreReady() {
        Core core = SmartHelmetManager.getCore();
        if (core != null) {
            core.addListener(mListener);
        }
    }

    public void destroy() {
        mNM.cancelAll();
        Core core = SmartHelmetManager.getCore();
        if (core != null) {
            core.removeListener(mListener);
        }
    }

    private void addFlagsToIntent(Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
    }

    public void startForeground() {
        SmartHelmetService.instance().startForeground(SERVICE_NOTIF_ID, mServiceNotification);
        mCurrentForegroundServiceNotification = SERVICE_NOTIF_ID;
    }

    private void startForeground(Notification notification, int id) {
        SmartHelmetService.instance().startForeground(id, notification);
        mCurrentForegroundServiceNotification = id;
    }

    public void stopForeground() {
        SmartHelmetService.instance().stopForeground(true);
        mCurrentForegroundServiceNotification = 0;
    }

    public void removeForegroundServiceNotificationIfPossible() {
        if (mCurrentForegroundServiceNotification == SERVICE_NOTIF_ID
                && !isServiceNotificationDisplayed()) {
            stopForeground();
        }
    }

    public void setCurrentlyDisplayedChatRoom(String address) {
        mCurrentChatRoomAddress = address;
        if (address != null) {
            resetMessageNotifCount(address);
        }
    }

    public void sendNotification(int id, Notification notif) {
        mNM.notify(id, notif);
    }

    public void resetMessageNotifCount(String address) {
        Notifiable notif = mChatNotifMap.get(address);
        if (notif != null) {
            notif.resetMessages();
            mNM.cancel(notif.getNotificationId());
        }
    }

    private boolean isServiceNotificationDisplayed() {
        return LinphonePreferences.instance().getServiceNotificationVisibility();
    }

    public String getSipUriForNotificationId(int notificationId) {
        for (String addr : mChatNotifMap.keySet()) {
            if (mChatNotifMap.get(addr).getNotificationId() == notificationId) {
                return addr;
            }
        }
        return null;
    }

    public void displayGroupChatMessageNotification(
            String subject,
            String conferenceAddress,
            String fromName,
            Uri fromPictureUri,
            String message,
            Address localIdentity,
            long timestamp,
            Uri filePath,
            String fileMime) {

        Bitmap bm = ImageUtils.getRoundBitmapFromUri(mContext, fromPictureUri);
        Notifiable notif = mChatNotifMap.get(conferenceAddress);
        NotifiableMessage notifMessage =
                new NotifiableMessage(message, fromName, timestamp, filePath, fileMime);
        if (notif == null) {
            notif = new Notifiable(mLastNotificationId);
            mLastNotificationId += 1;
            mChatNotifMap.put(conferenceAddress, notif);
        }

        notifMessage.setSenderBitmap(bm);
        notif.addMessage(notifMessage);
        notif.setIsGroup(true);
        notif.setGroupTitle(subject);
        notif.setMyself(SmartHelmetUtils.getAddressDisplayName(localIdentity));
        notif.setLocalIdentity(localIdentity.asString());

        Intent notifIntent = new Intent(mContext, ChatActivity.class);
        notifIntent.putExtra("RemoteSipUri", conferenceAddress);
        notifIntent.putExtra("LocalSipUri", localIdentity.asStringUriOnly());
        addFlagsToIntent(notifIntent);

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        mContext,
                        notif.getNotificationId(),
                        notifIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification =
                Compatibility.createMessageNotification(
                        mContext,
                        notif,
                        subject,
                        mContext.getString(R.string.group_chat_notif)
                                .replace("%1", fromName)
                                .replace("%2", message),
                        bm,
                        pendingIntent);
        sendNotification(notif.getNotificationId(), notification);
    }

    public void displayMessageNotification(
            String fromSipUri,
            String fromName,
            Uri fromPictureUri,
            String message,
            Address localIdentity,
            long timestamp,
            Uri filePath,
            String fileMime) {
        if (fromName == null) {
            fromName = fromSipUri;
        }

        Bitmap bm = ImageUtils.getRoundBitmapFromUri(mContext, fromPictureUri);
        Notifiable notif = mChatNotifMap.get(fromSipUri);
        NotifiableMessage notifMessage =
                new NotifiableMessage(message, fromName, timestamp, filePath, fileMime);
        if (notif == null) {
            notif = new Notifiable(mLastNotificationId);
            mLastNotificationId += 1;
            mChatNotifMap.put(fromSipUri, notif);
        }

        notifMessage.setSenderBitmap(bm);
        notif.addMessage(notifMessage);
        notif.setIsGroup(false);
        notif.setMyself(SmartHelmetUtils.getAddressDisplayName(localIdentity));
        notif.setLocalIdentity(localIdentity.asString());

        Intent notifIntent = new Intent(mContext, ChatActivity.class);
        notifIntent.putExtra("RemoteSipUri", fromSipUri);
        notifIntent.putExtra("LocalSipUri", localIdentity.asStringUriOnly());
        addFlagsToIntent(notifIntent);

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        mContext,
                        notif.getNotificationId(),
                        notifIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification =
                Compatibility.createMessageNotification(
                        mContext, notif, fromName, message, bm, pendingIntent);
        sendNotification(notif.getNotificationId(), notification);
    }

    public void displayMissedCallNotification(Call call) {
        Intent missedCallNotifIntent = new Intent(mContext, HistoryActivity.class);
        addFlagsToIntent(missedCallNotifIntent);

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        mContext,
                        MISSED_CALLS_NOTIF_ID,
                        missedCallNotifIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);

        int missedCallCount = SmartHelmetManager.getCore().getMissedCallsCount();
        String body;
        if (missedCallCount > 1) {
            body =
                    mContext.getString(R.string.missed_calls_notif_body)
                            .replace("%i", String.valueOf(missedCallCount));
        } else {
            Address address = call.getRemoteAddress();
            SmartHelmetContact c = ContactsManager.getInstance().findContactFromAddress(address);
            if (c != null) {
                body = c.getFullName();
            } else {
                body = address.getDisplayName();
                if (body == null) {
                    body = address.asStringUriOnly();
                }
            }
        }

        Notification notif =
                Compatibility.createMissedCallNotification(
                        mContext,
                        mContext.getString(R.string.missed_calls_notif_title),
                        body,
                        pendingIntent);
        sendNotification(MISSED_CALLS_NOTIF_ID, notif);
    }

    public void displayCallNotification(Call call) {
        if (call == null) return;

        Intent callNotifIntent;
        if (call.getState() == Call.State.IncomingReceived
                || call.getState() == Call.State.IncomingEarlyMedia) {
            callNotifIntent = new Intent(mContext, CallIncomingActivity.class);
        } else if (call.getState() == Call.State.OutgoingInit
                || call.getState() == Call.State.OutgoingProgress
                || call.getState() == Call.State.OutgoingRinging
                || call.getState() == Call.State.OutgoingEarlyMedia) {
            callNotifIntent = new Intent(mContext, CallOutgoingActivity.class);
        } else {
            callNotifIntent = new Intent(mContext, CallActivity.class);
        }
        callNotifIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        mContext, 0, callNotifIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Address address = call.getRemoteAddress();
        String addressAsString = address.asStringUriOnly();
        Notifiable notif = mCallNotifMap.get(addressAsString);
        if (notif == null) {
            notif = new Notifiable(mLastNotificationId);
            mLastNotificationId += 1;
            mCallNotifMap.put(addressAsString, notif);
        }

        int notificationTextId;
        int iconId;
        switch (call.getState()) {
            case Released:
            case End:
                if (mCurrentForegroundServiceNotification == notif.getNotificationId()) {
                    // Call is released, remove service notification to allow for an other call to
                    // be service notification
                    stopForeground();
                }
                mNM.cancel(notif.getNotificationId());
                mCallNotifMap.remove(addressAsString);
                return;
            case Paused:
            case PausedByRemote:
            case Pausing:
                iconId = R.drawable.topbar_call_notification;
                notificationTextId = R.string.incall_notif_paused;
                break;
            case IncomingEarlyMedia:
            case IncomingReceived:
                iconId = R.drawable.topbar_call_notification;
                notificationTextId = R.string.incall_notif_incoming;
                break;
            case OutgoingEarlyMedia:
            case OutgoingInit:
            case OutgoingProgress:
            case OutgoingRinging:
                iconId = R.drawable.topbar_call_notification;
                notificationTextId = R.string.incall_notif_outgoing;
                break;
            default:
                if (call.getCurrentParams().videoEnabled()) {
                    iconId = R.drawable.topbar_videocall_notification;
                    notificationTextId = R.string.incall_notif_video;
                } else {
                    iconId = R.drawable.topbar_call_notification;
                    notificationTextId = R.string.incall_notif_active;
                }
                break;
        }

        if (notif.getIconResourceId() == iconId
                && notif.getTextResourceId() == notificationTextId) {
            // Notification hasn't changed, do not "update" it to avoid blinking
            return;
        }
        notif.setIconResourceId(iconId);
        notif.setTextResourceId(notificationTextId);

        SmartHelmetContact contact = ContactsManager.getInstance().findContactFromAddress(address);
        Uri pictureUri = contact != null ? contact.getPhotoUri() : null;
        Bitmap bm = ImageUtils.getRoundBitmapFromUri(mContext, pictureUri);
        String name = SmartHelmetUtils.getAddressDisplayName(address);

        boolean showAnswerAction =
                call.getState() == Call.State.IncomingReceived
                        || call.getState() == Call.State.IncomingEarlyMedia;
        Notification notification =
                Compatibility.createInCallNotification(
                        mContext,
                        notif.getNotificationId(),
                        showAnswerAction,
                        mContext.getString(notificationTextId),
                        iconId,
                        bm,
                        name,
                        pendingIntent);

        if (!isServiceNotificationDisplayed()) {
            if (call.getCore().getCallsNb() == 0) {
                stopForeground();
            } else {
                if (mCurrentForegroundServiceNotification == 0) {
                    startForeground(notification, notif.getNotificationId());
                } else {
                    sendNotification(notif.getNotificationId(), notification);
                }
            }
        }
    }

    public String getSipUriForCallNotificationId(int notificationId) {
        for (String addr : mCallNotifMap.keySet()) {
            if (mCallNotifMap.get(addr).getNotificationId() == notificationId) {
                return addr;
            }
        }
        return null;
    }

    /*public void displayInappNotification(String message) {
        Intent notifIntent = new Intent(mContext, InAppPurchaseActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        mContext, IN_APP_NOTIF_ID, notifIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notif =
                Compatibility.createSimpleNotification(
                        mContext,
                        mContext.getString(R.string.inapp_notification_title),
                        message,
                        pendingIntent);
        sendNotification(IN_APP_NOTIF_ID, notif);
    }*/

    public void dismissNotification(int notifId) {
        mNM.cancel(notifId);
    }

    private void createNotification(
            ChatRoom cr,
            SmartHelmetContact contact,
            Address from,
            String textMessage,
            long time,
            Uri file,
            String mime) {
        if (cr.hasCapability(ChatRoomCapabilities.OneToOne.toInt())) {
            if (contact != null) {
                displayMessageNotification(
                        cr.getPeerAddress().asStringUriOnly(),
                        contact.getFullName(),
                        contact.getThumbnailUri(),
                        textMessage,
                        cr.getLocalAddress(),
                        time,
                        file,
                        mime);
            } else {
                displayMessageNotification(
                        cr.getPeerAddress().asStringUriOnly(),
                        from.getUsername(),
                        null,
                        textMessage,
                        cr.getLocalAddress(),
                        time,
                        file,
                        mime);
            }
        } else {
            String subject = cr.getSubject();
            if (contact != null) {
                displayGroupChatMessageNotification(
                        subject,
                        cr.getPeerAddress().asStringUriOnly(),
                        contact.getFullName(),
                        contact.getThumbnailUri(),
                        textMessage,
                        cr.getLocalAddress(),
                        time,
                        file,
                        mime);
            } else {
                displayGroupChatMessageNotification(
                        subject,
                        cr.getPeerAddress().asStringUriOnly(),
                        from.getUsername(),
                        null,
                        textMessage,
                        cr.getLocalAddress(),
                        time,
                        file,
                        mime);
            }
        }
    }
}
