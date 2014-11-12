/*
 * This is the source code of Telegram for Android v. 1.3.2.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013.
 */

package br.com.uatizapi.ui;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import br.com.uatizapi.android.AndroidUtilities;
import br.com.uatizapi.PhoneFormat.PhoneFormat;
import br.com.uatizapi.android.ContactsController;
import br.com.uatizapi.android.LocaleController;
import br.com.uatizapi.messenger.TLRPC;
import br.com.uatizapi.android.MessagesController;
import br.com.uatizapi.android.NotificationCenter;
import br.com.uatizapi.ui.Views.BackupImageView;
import br.com.uatizapi.ui.Views.ActionBar.BaseFragment;

public class ContactAddActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {
    private int user_id;
    private String phone = null;
    private View doneButton;
    private EditText firstNameField;
    private EditText lastNameField;
    private BackupImageView avatarImage;
    private TextView onlineText;
    private TextView phoneText;

    public ContactAddActivity(Bundle args) {
        super(args);
    }

    @Override
    public boolean onFragmentCreate() {
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.updateInterfaces);
        user_id = getArguments().getInt("user_id", 0);
        phone = getArguments().getString("phone");
        TLRPC.User user = MessagesController.getInstance().getUser(user_id);
        return user != null && super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.updateInterfaces);
    }

    @Override
    public View createView(LayoutInflater inflater, ViewGroup container) {
        if (fragmentView == null) {
            actionBarLayer.setCustomView(br.com.uatizapi.messenger.R.layout.settings_do_action_layout);
            Button cancelButton = (Button)actionBarLayer.findViewById(br.com.uatizapi.messenger.R.id.cancel_button);
            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    finishFragment();
                }
            });
            doneButton = actionBarLayer.findViewById(br.com.uatizapi.messenger.R.id.done_button);
            doneButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (firstNameField.getText().length() != 0) {
                        TLRPC.User user = MessagesController.getInstance().getUser(user_id);
                        user.first_name = firstNameField.getText().toString();
                        user.last_name = lastNameField.getText().toString();
                        ContactsController.getInstance().addContact(user);
                        finishFragment();
                        NotificationCenter.getInstance().postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_NAME);
                    }
                }
            });

            cancelButton.setText(LocaleController.getString("Cancel", br.com.uatizapi.messenger.R.string.Cancel).toUpperCase());
            TextView textView = (TextView)doneButton.findViewById(br.com.uatizapi.messenger.R.id.done_button_text);
            textView.setText(LocaleController.getString("Done", br.com.uatizapi.messenger.R.string.Done).toUpperCase());

            fragmentView = inflater.inflate(br.com.uatizapi.messenger.R.layout.contact_add_layout, container, false);

            TLRPC.User user = MessagesController.getInstance().getUser(user_id);
            if (user.phone == null) {
                if (phone != null) {
                    user.phone = PhoneFormat.stripExceptNumbers(phone);
                }
            }

            onlineText = (TextView)fragmentView.findViewById(br.com.uatizapi.messenger.R.id.settings_online);
            avatarImage = (BackupImageView)fragmentView.findViewById(br.com.uatizapi.messenger.R.id.settings_avatar_image);
            avatarImage.processDetach = false;
            phoneText = (TextView)fragmentView.findViewById(br.com.uatizapi.messenger.R.id.settings_name);
            Typeface typeface = AndroidUtilities.getTypeface("fonts/rmedium.ttf");
            phoneText.setTypeface(typeface);

            firstNameField = (EditText)fragmentView.findViewById(br.com.uatizapi.messenger.R.id.first_name_field);
            firstNameField.setHint(LocaleController.getString("FirstName", br.com.uatizapi.messenger.R.string.FirstName));
            firstNameField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                    if (i == EditorInfo.IME_ACTION_NEXT) {
                        lastNameField.requestFocus();
                        lastNameField.setSelection(lastNameField.length());
                        return true;
                    }
                    return false;
                }
            });
            lastNameField = (EditText)fragmentView.findViewById(br.com.uatizapi.messenger.R.id.last_name_field);
            lastNameField.setHint(LocaleController.getString("LastName", br.com.uatizapi.messenger.R.string.LastName));
            lastNameField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                    if (i == EditorInfo.IME_ACTION_DONE) {
                        doneButton.performClick();
                        return true;
                    }
                    return false;
                }
            });

            if (user != null) {
                firstNameField.setText(user.first_name);
                firstNameField.setSelection(firstNameField.length());
                lastNameField.setText(user.last_name);
            }

            updateAvatarLayout();
        } else {
            ViewGroup parent = (ViewGroup)fragmentView.getParent();
            if (parent != null) {
                parent.removeView(fragmentView);
            }
        }
        return fragmentView;
    }

    private void updateAvatarLayout() {
        if (phoneText == null) {
            return;
        }
        TLRPC.User user = MessagesController.getInstance().getUser(user_id);
        if (user == null) {
            return;
        }
        phoneText.setText(PhoneFormat.getInstance().format("+" + user.phone));
        onlineText.setText(LocaleController.formatUserStatus(user));

        TLRPC.FileLocation photo = null;
        if (user.photo != null) {
            photo = user.photo.photo_small;
        }
        avatarImage.setImage(photo, "50_50", AndroidUtilities.getUserAvatarForId(user.id));
    }

    public void didReceivedNotification(int id, Object... args) {
        if (id == NotificationCenter.updateInterfaces) {
            int mask = (Integer)args[0];
            if ((mask & MessagesController.UPDATE_MASK_AVATAR) != 0 || (mask & MessagesController.UPDATE_MASK_STATUS) != 0) {
                updateAvatarLayout();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
        boolean animations = preferences.getBoolean("view_animations", true);
        if (!animations) {
            firstNameField.requestFocus();
            AndroidUtilities.showKeyboard(firstNameField);
        }
    }

    @Override
    public void onOpenAnimationEnd() {
        firstNameField.requestFocus();
        AndroidUtilities.showKeyboard(firstNameField);
    }
}
