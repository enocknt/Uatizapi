/*
 * This is the source code of Telegram for Android v. 1.3.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2014.
 */

package br.com.uatizapi.ui.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import br.com.uatizapi.android.LocaleController;
import br.com.uatizapi.messenger.TLRPC;
import br.com.uatizapi.android.ContactsController;
import br.com.uatizapi.android.MessagesController;
import br.com.uatizapi.messenger.R;
import br.com.uatizapi.ui.Cells.ChatOrUserCell;
import br.com.uatizapi.ui.Views.SectionedBaseAdapter;
import br.com.uatizapi.ui.Views.SettingsSectionLayout;

import java.util.ArrayList;
import java.util.HashMap;

public class ContactsActivityAdapter extends SectionedBaseAdapter {
    private Context mContext;
    private boolean onlyUsers;
    private boolean usersAsSections;
    private HashMap<Integer, TLRPC.User> ignoreUsers;

    public ContactsActivityAdapter(Context context, boolean arg1, boolean arg2, HashMap<Integer, TLRPC.User> arg3) {
        mContext = context;
        onlyUsers = arg1;
        usersAsSections = arg2;
        ignoreUsers = arg3;
    }

    @Override
    public Object getItem(int section, int position) {
        return null;
    }

    @Override
    public long getItemId(int section, int position) {
        return 0;
    }

    @Override
    public int getSectionCount() {
        int count = 0;
        if (usersAsSections) {
            count += ContactsController.getInstance().sortedUsersSectionsArray.size();
        } else {
            count++;
        }
        if (!onlyUsers) {
            count += ContactsController.getInstance().sortedContactsSectionsArray.size();
        }
        return count;
    }

    @Override
    public int getCountForSection(int section) {
        if (usersAsSections) {
            if (section < ContactsController.getInstance().sortedUsersSectionsArray.size()) {
                ArrayList<TLRPC.TL_contact> arr = ContactsController.getInstance().usersSectionsDict.get(ContactsController.getInstance().sortedUsersSectionsArray.get(section));
                return arr.size();
            }
        } else {
            if (section == 0) {
                return ContactsController.getInstance().contacts.size() + 1;
            }
        }
        ArrayList<ContactsController.Contact> arr = ContactsController.getInstance().contactsSectionsDict.get(ContactsController.getInstance().sortedContactsSectionsArray.get(section - 1));
        return arr.size();
    }

    @Override
    public View getItemView(int section, int position, View convertView, ViewGroup parent) {

        TLRPC.User user = null;
        int count = 0;
        if (usersAsSections) {
            if (section < ContactsController.getInstance().sortedUsersSectionsArray.size()) {
                ArrayList<TLRPC.TL_contact> arr = ContactsController.getInstance().usersSectionsDict.get(ContactsController.getInstance().sortedUsersSectionsArray.get(section));
                user = MessagesController.getInstance().getUser(arr.get(position).user_id);
                count = arr.size();
            }
        } else {
            if (section == 0) {
                if (position == 0) {
                    if (convertView == null) {
                        LayoutInflater li = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                        convertView = li.inflate(R.layout.contacts_invite_row_layout, parent, false);
                        TextView textView = (TextView)convertView.findViewById(R.id.messages_list_row_name);
                        textView.setText(LocaleController.getString("InviteFriends", R.string.InviteFriends));
                    }
                    View divider = convertView.findViewById(R.id.settings_row_divider);
                    if (ContactsController.getInstance().contacts.isEmpty()) {
                        divider.setVisibility(View.INVISIBLE);
                    } else {
                        divider.setVisibility(View.VISIBLE);
                    }
                    return convertView;
                }
                user = MessagesController.getInstance().getUser(ContactsController.getInstance().contacts.get(position - 1).user_id);
                count = ContactsController.getInstance().contacts.size();
            }
        }
        if (user != null) {
            if (convertView == null) {
                convertView = new ChatOrUserCell(mContext);
                ((ChatOrUserCell)convertView).usePadding = false;
            }

            ((ChatOrUserCell)convertView).setData(user, null, null, null, null);

            if (ignoreUsers != null) {
                if (ignoreUsers.containsKey(user.id)) {
                    ((ChatOrUserCell)convertView).drawAlpha = 0.5f;
                } else {
                    ((ChatOrUserCell)convertView).drawAlpha = 1.0f;
                }
            }

            ((ChatOrUserCell) convertView).useSeparator = position != count - 1;

            return convertView;
        }

        TextView textView;
        if (convertView == null) {
            LayoutInflater li = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = li.inflate(R.layout.settings_row_button_layout, parent, false);
            textView = (TextView)convertView.findViewById(R.id.settings_row_text);
        } else {
            textView = (TextView)convertView.findViewById(R.id.settings_row_text);
        }

        View divider = convertView.findViewById(R.id.settings_row_divider);
        ArrayList<ContactsController.Contact> arr = ContactsController.getInstance().contactsSectionsDict.get(ContactsController.getInstance().sortedContactsSectionsArray.get(section - 1));
        ContactsController.Contact contact = arr.get(position);
        if (divider != null) {
            if (position == arr.size() - 1) {
                divider.setVisibility(View.INVISIBLE);
            } else {
                divider.setVisibility(View.VISIBLE);
            }
        }
        if (contact.first_name != null && contact.last_name != null) {
            textView.setText(contact.first_name + " " + contact.last_name);
        } else if (contact.first_name != null && contact.last_name == null) {
            textView.setText(contact.first_name);
        } else {
            textView.setText(contact.last_name);
        }
        return convertView;
    }

    @Override
    public int getItemViewType(int section, int position) {
        if (usersAsSections) {
            if (section < ContactsController.getInstance().sortedUsersSectionsArray.size()) {
                return 0;
            }
        } else if (section == 0) {
            if (position == 0) {
                return 2;
            }
            return 0;
        }
        return 1;
    }

    @Override
    public int getItemViewTypeCount() {
        return 3;
    }

    @Override
    public int getSectionHeaderViewType(int section) {
        if (usersAsSections) {
            if (section < ContactsController.getInstance().sortedUsersSectionsArray.size()) {
                return 1;
            }
        } else if (section == 0) {
            return 0;
        }
        return 1;
    }

    @Override
    public int getSectionHeaderViewTypeCount() {
        return 2;
    }

    @Override
    public View getSectionHeaderView(int section, View convertView, ViewGroup parent) {
        if (usersAsSections) {
            if (section < ContactsController.getInstance().sortedUsersSectionsArray.size()) {
                if (convertView == null) {
                    convertView = new SettingsSectionLayout(mContext);
                    convertView.setBackgroundColor(0xffffffff);
                }
                ((SettingsSectionLayout) convertView).setText(ContactsController.getInstance().sortedUsersSectionsArray.get(section));
                return convertView;
            }
        } else {
            if (section == 0) {
                if (convertView == null) {
                    LayoutInflater li = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    convertView = li.inflate(R.layout.empty_layout, parent, false);
                }
                return convertView;
            }
        }

        if (convertView == null) {
            convertView = new SettingsSectionLayout(mContext);
            convertView.setBackgroundColor(0xffffffff);
        }
        ((SettingsSectionLayout) convertView).setText(ContactsController.getInstance().sortedContactsSectionsArray.get(section - 1));
        return convertView;
    }
}
