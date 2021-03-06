package com.datang.smarthelmet.contacts;

/*
ContactsAdapter.java
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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SectionIndexer;
import androidx.annotation.NonNull;
import com.datang.smarthelmet.R;
import com.datang.smarthelmet.settings.LinphonePreferences;
import com.datang.smarthelmet.utils.SelectableAdapter;
import com.datang.smarthelmet.utils.SelectableHelper;
import com.datang.smarthelmet.views.ContactAvatar;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ContactsAdapter extends SelectableAdapter<ContactViewHolder>
        implements SectionIndexer {
    private List<SmartHelmetContact> mContacts;
    private String[] mSections;
    private ArrayList<String> mSectionsList;
    private Map<String, Integer> mMap = new LinkedHashMap<>();
    private final ContactViewHolder.ClickListener mClickListener;
    private final Context mContext;
    private boolean mIsSearchMode;

    ContactsAdapter(
            Context context,
            List<SmartHelmetContact> contactsList,
            ContactViewHolder.ClickListener clickListener,
            SelectableHelper helper) {
        super(helper);
        mContext = context;
        updateDataSet(contactsList);
        mClickListener = clickListener;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.contact_cell, parent, false);
        return new ContactViewHolder(v, mClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, final int position) {
        SmartHelmetContact contact = (SmartHelmetContact) getItem(position);

        holder.name.setText(contact.getFullName());

        if (!mIsSearchMode) {
            String fullName = contact.getFullName();
            if (fullName != null && !fullName.isEmpty()) {
                holder.separatorText.setText(String.valueOf(fullName.charAt(0)));
            }
        }
        holder.separator.setVisibility(
                mIsSearchMode
                                || (getPositionForSection(getSectionForPosition(position))
                                        != position)
                        ? View.GONE
                        : View.VISIBLE);
        holder.linphoneFriend.setVisibility(contact.isInFriendList() ? View.VISIBLE : View.GONE);

        ContactAvatar.displayAvatar(contact, holder.avatarLayout);

        boolean isOrgVisible = LinphonePreferences.instance().isDisplayContactOrganization();
        String org = contact.getOrganization();
        if (org != null && !org.isEmpty() && isOrgVisible) {
            holder.organization.setText(org);
            holder.organization.setVisibility(View.VISIBLE);
        } else {
            holder.organization.setVisibility(View.GONE);
        }

        holder.delete.setVisibility(isEditionEnabled() ? View.VISIBLE : View.INVISIBLE);
        holder.delete.setChecked(isSelected(position));
    }

    @Override
    public int getItemCount() {
        return mContacts.size();
    }

    public Object getItem(int position) {
        if (position >= getItemCount()) return null;
        return mContacts.get(position);
    }

    public void setIsSearchMode(boolean set) {
        mIsSearchMode = set;
    }

    public long getItemId(int position) {
        return position;
    }

    public void updateDataSet(List<SmartHelmetContact> contactsList) {
        mContacts = contactsList;

        mMap = new LinkedHashMap<>();
        String prevLetter = null;
        for (int i = 0; i < mContacts.size(); i++) {
            SmartHelmetContact contact = mContacts.get(i);
            String fullName = contact.getFullName();
            if (fullName == null || fullName.isEmpty()) {
                continue;
            }
            String firstLetter = fullName.substring(0, 1).toUpperCase(Locale.getDefault());
            if (!firstLetter.equals(prevLetter)) {
                prevLetter = firstLetter;
                mMap.put(firstLetter, i);
            }
        }
        mSectionsList = new ArrayList<>(mMap.keySet());
        mSections = new String[mSectionsList.size()];
        mSectionsList.toArray(mSections);

        notifyDataSetChanged();
    }

    @Override
    public Object[] getSections() {
        return mSections;
    }

    @Override
    public int getPositionForSection(int sectionIndex) {
        if (sectionIndex >= mSections.length || sectionIndex < 0) {
            return 0;
        }
        return mMap.get(mSections[sectionIndex]);
    }

    @Override
    public int getSectionForPosition(int position) {
        if (position >= mContacts.size() || position < 0) {
            return 0;
        }
        SmartHelmetContact contact = mContacts.get(position);
        String fullName = contact.getFullName();
        if (fullName == null || fullName.isEmpty()) {
            return 0;
        }
        String letter = fullName.substring(0, 1).toUpperCase(Locale.getDefault());
        return mSectionsList.indexOf(letter);
    }
}
