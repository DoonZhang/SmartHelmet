package com.datang.smarthelmet.chat;

/*
ChatMessagesGenericAdapter.java
Copyright (C) 2018 Belledonne Communications, Grenoble, France

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

import com.datang.smarthelmet.contacts.SmartHelmetContact;
import java.util.ArrayList;
import org.linphone.core.EventLog;

interface ChatMessagesGenericAdapter {
    void addToHistory(EventLog log);

    void addAllToHistory(ArrayList<EventLog> logs);

    void setContacts(ArrayList<SmartHelmetContact> participants);

    void refresh(EventLog[] history);

    void clear();

    Object getItem(int i);

    void removeItem(int i);
}
