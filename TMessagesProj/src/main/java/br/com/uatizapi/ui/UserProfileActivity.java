/*
 * This is the source code of Telegram for Android v. 1.3.2.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013.
 */

package br.com.uatizapi.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import br.com.uatizapi.android.AndroidUtilities;
import br.com.uatizapi.PhoneFormat.PhoneFormat;
import br.com.uatizapi.android.LocaleController;
import br.com.uatizapi.android.SendMessagesHelper;
import br.com.uatizapi.messenger.TLRPC;
import br.com.uatizapi.android.ContactsController;
import br.com.uatizapi.messenger.FileLog;
import br.com.uatizapi.android.MessagesController;
import br.com.uatizapi.android.NotificationCenter;
import br.com.uatizapi.android.MessageObject;
import br.com.uatizapi.ui.Adapters.BaseFragmentAdapter;
import br.com.uatizapi.ui.Views.ActionBar.ActionBarLayer;
import br.com.uatizapi.ui.Views.ActionBar.ActionBarMenu;
import br.com.uatizapi.ui.Views.ActionBar.ActionBarMenuItem;
import br.com.uatizapi.ui.Views.BackupImageView;
import br.com.uatizapi.ui.Views.ActionBar.BaseFragment;
import br.com.uatizapi.ui.Views.IdenticonView;
import br.com.uatizapi.ui.Views.SettingsSectionLayout;

import java.util.ArrayList;

public class UserProfileActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate, MessagesActivity.MessagesActivityDelegate, PhotoViewer.PhotoViewerProvider {
    private ListView listView;
    private ListAdapter listAdapter;
    private int user_id;
    private int totalMediaCount = -1;
    private boolean creatingChat = false;
    private long dialog_id;
    private TLRPC.EncryptedChat currentEncryptedChat;
    private boolean userBlocked = false;

    private final static int add_contact = 1;
    private final static int block_contact = 2;
    private final static int share_contact = 3;
    private final static int edit_contact = 4;
    private final static int delete_contact = 5;

    private int avatarRow;
    private int phoneSectionRow;
    private int phoneRow;
    private int usernameRow;
    private int settingsSectionRow;
    private int settingsTimerRow;
    private int settingsKeyRow;
    private int settingsNotificationsRow;
    private int sharedMediaSectionRow;
    private int sharedMediaRow;
    private int rowCount = 0;

    public UserProfileActivity(Bundle args) {
        super(args);
    }

    @Override
    public boolean onFragmentCreate() {
        user_id = arguments.getInt("user_id", 0);
        dialog_id = arguments.getLong("dialog_id", 0);
        if (dialog_id != 0) {
            currentEncryptedChat = MessagesController.getInstance().getEncryptedChat((int)(dialog_id >> 32));
        }
        updateRowsIds();
        if (MessagesController.getInstance().getUser(user_id) == null) {
            return false;
        }
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.updateInterfaces);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.contactsDidLoaded);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.mediaCountDidLoaded);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.encryptedChatCreated);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.encryptedChatUpdated);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.blockedUsersDidLoaded);
        userBlocked = MessagesController.getInstance().blockedUsers.contains(user_id);

        MessagesController.getInstance().loadFullUser(MessagesController.getInstance().getUser(user_id), classGuid);

        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.updateInterfaces);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.contactsDidLoaded);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.mediaCountDidLoaded);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.encryptedChatCreated);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.encryptedChatUpdated);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.blockedUsersDidLoaded);

        MessagesController.getInstance().cancelLoadFullUser(user_id);
    }

    private void updateRowsIds() {
        rowCount = 0;
        avatarRow = rowCount++;
        phoneSectionRow = rowCount++;
        phoneRow = rowCount++;
        TLRPC.User user = MessagesController.getInstance().getUser(user_id);
        if (user != null && user.username != null && user.username.length() > 0) {
            usernameRow = rowCount++;
        } else {
            usernameRow = -1;
        }
        settingsSectionRow = rowCount++;
        if (currentEncryptedChat instanceof TLRPC.TL_encryptedChat) {
            settingsTimerRow = rowCount++;
            settingsKeyRow = rowCount++;
        } else {
            settingsTimerRow = -1;
            settingsKeyRow = -1;
        }
        settingsNotificationsRow = rowCount++;
        sharedMediaSectionRow = rowCount++;
        sharedMediaRow = rowCount++;
    }

    @Override
    public View createView(LayoutInflater inflater, ViewGroup container) {
        if (fragmentView == null) {
            actionBarLayer.setDisplayHomeAsUpEnabled(true, br.com.uatizapi.messenger.R.drawable.ic_ab_back);
            actionBarLayer.setBackOverlay(br.com.uatizapi.messenger.R.layout.updating_state_layout);
            if (dialog_id != 0) {
                actionBarLayer.setTitle(LocaleController.getString("SecretTitle", br.com.uatizapi.messenger.R.string.SecretTitle));
                actionBarLayer.setTitleIcon(br.com.uatizapi.messenger.R.drawable.ic_lock_white, AndroidUtilities.dp(4));
            } else {
                actionBarLayer.setTitle(LocaleController.getString("ContactInfo", br.com.uatizapi.messenger.R.string.ContactInfo));
            }
            actionBarLayer.setActionBarMenuOnItemClick(new ActionBarLayer.ActionBarMenuOnItemClick() {
                @Override
                public void onItemClick(final int id) {
                    if (id == -1) {
                        finishFragment();
                    } else if (id == block_contact) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                        if (!userBlocked) {
                            builder.setMessage(LocaleController.getString("AreYouSureBlockContact", br.com.uatizapi.messenger.R.string.AreYouSureBlockContact));
                        } else {
                            builder.setMessage(LocaleController.getString("AreYouSureUnblockContact", br.com.uatizapi.messenger.R.string.AreYouSureUnblockContact));
                        }
                        builder.setTitle(LocaleController.getString("AppName", br.com.uatizapi.messenger.R.string.AppName));
                        builder.setPositiveButton(LocaleController.getString("OK", br.com.uatizapi.messenger.R.string.OK), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if (!userBlocked) {
                                    MessagesController.getInstance().blockUser(user_id);
                                } else {
                                    MessagesController.getInstance().unblockUser(user_id);
                                }
                            }
                        });
                        builder.setNegativeButton(LocaleController.getString("Cancel", br.com.uatizapi.messenger.R.string.Cancel), null);
                        showAlertDialog(builder);
                    } else if (id == add_contact) {
                        TLRPC.User user = MessagesController.getInstance().getUser(user_id);
                        Bundle args = new Bundle();
                        args.putInt("user_id", user.id);
                        presentFragment(new ContactAddActivity(args));
                    } else if (id == share_contact) {
                        Bundle args = new Bundle();
                        args.putBoolean("onlySelect", true);
                        args.putBoolean("serverOnly", true);
                        MessagesActivity fragment = new MessagesActivity(args);
                        fragment.setDelegate(UserProfileActivity.this);
                        presentFragment(fragment);
                    } else if (id == edit_contact) {
                        Bundle args = new Bundle();
                        args.putInt("user_id", user_id);
                        presentFragment(new ContactAddActivity(args));
                    } else if (id == delete_contact) {
                        final TLRPC.User user = MessagesController.getInstance().getUser(user_id);
                        if (user == null || getParentActivity() == null) {
                            return;
                        }
                        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                        builder.setMessage(LocaleController.getString("AreYouSureDeleteContact", br.com.uatizapi.messenger.R.string.AreYouSureDeleteContact));
                        builder.setTitle(LocaleController.getString("AppName", br.com.uatizapi.messenger.R.string.AppName));
                        builder.setPositiveButton(LocaleController.getString("OK", br.com.uatizapi.messenger.R.string.OK), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ArrayList<TLRPC.User> arrayList = new ArrayList<TLRPC.User>();
                                arrayList.add(user);
                                ContactsController.getInstance().deleteContact(arrayList);
                            }
                        });
                        builder.setNegativeButton(LocaleController.getString("Cancel", br.com.uatizapi.messenger.R.string.Cancel), null);
                        showAlertDialog(builder);
                    }
                }
            });

            createActionBarMenu();

            fragmentView = inflater.inflate(br.com.uatizapi.messenger.R.layout.user_profile_layout, container, false);
            listAdapter = new ListAdapter(getParentActivity());

            TextView textView = (TextView)fragmentView.findViewById(br.com.uatizapi.messenger.R.id.start_secret_button_text);
            textView.setText(LocaleController.getString("StartEncryptedChat", br.com.uatizapi.messenger.R.string.StartEncryptedChat));

            View startSecretButton = fragmentView.findViewById(br.com.uatizapi.messenger.R.id.start_secret_button);
            startSecretButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (getParentActivity() == null) {
                        return;
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setMessage(LocaleController.getString("AreYouSureSecretChat", br.com.uatizapi.messenger.R.string.AreYouSureSecretChat));
                    builder.setTitle(LocaleController.getString("AppName", br.com.uatizapi.messenger.R.string.AppName));
                    builder.setPositiveButton(LocaleController.getString("OK", br.com.uatizapi.messenger.R.string.OK), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            creatingChat = true;
                            MessagesController.getInstance().startSecretChat(getParentActivity(), MessagesController.getInstance().getUser(user_id));
                        }
                    });
                    builder.setNegativeButton(LocaleController.getString("Cancel", br.com.uatizapi.messenger.R.string.Cancel), null);
                    showAlertDialog(builder);
                }
            });
            if (dialog_id == 0) {
                startSecretButton.setVisibility(View.VISIBLE);
            } else {
                startSecretButton.setVisibility(View.GONE);
            }

            listView = (ListView)fragmentView.findViewById(br.com.uatizapi.messenger.R.id.listView);
            listView.setAdapter(listAdapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, final int i, long l) {
                    if (i == sharedMediaRow) {
                        Bundle args = new Bundle();
                        if (dialog_id != 0) {
                            args.putLong("dialog_id", dialog_id);
                        } else {
                            args.putLong("dialog_id", user_id);
                        }
                        presentFragment(new MediaActivity(args));
                    } else if (i == settingsKeyRow) {
                        Bundle args = new Bundle();
                        args.putInt("chat_id", (int)(dialog_id >> 32));
                        presentFragment(new IdenticonActivity(args));
                    } else if (i == settingsTimerRow) {
                        if (getParentActivity() == null) {
                            return;
                        }
                        showAlertDialog(AndroidUtilities.buildTTLAlert(getParentActivity(), currentEncryptedChat));
                    } else if (i == settingsNotificationsRow) {
                        Bundle args = new Bundle();
                        args.putLong("dialog_id", dialog_id == 0 ? user_id : dialog_id);
                        presentFragment(new ProfileNotificationsActivity(args));
                    }
                }
            });
            if (dialog_id != 0) {
                MessagesController.getInstance().getMediaCount(dialog_id, classGuid, true);
            } else {
                MessagesController.getInstance().getMediaCount(user_id, classGuid, true);
            }
        } else {
            ViewGroup parent = (ViewGroup)fragmentView.getParent();
            if (parent != null) {
                parent.removeView(fragmentView);
            }
        }
        return fragmentView;
    }

    public void didReceivedNotification(int id, final Object... args) {
        if (id == NotificationCenter.updateInterfaces) {
            int mask = (Integer)args[0];
            if ((mask & MessagesController.UPDATE_MASK_AVATAR) != 0 || (mask & MessagesController.UPDATE_MASK_NAME) != 0) {
                updateRowsIds();
                if (listView != null) {
                    listView.invalidateViews();
                }
            }
        } else if (id == NotificationCenter.contactsDidLoaded) {
            createActionBarMenu();
        } else if (id == NotificationCenter.mediaCountDidLoaded) {
            long uid = (Long)args[0];
            if (uid > 0 && user_id == uid && dialog_id == 0 || dialog_id != 0 && dialog_id == uid) {
                totalMediaCount = (Integer)args[1];
                if (listView != null) {
                    listView.invalidateViews();
                }
            }
        } else if (id == NotificationCenter.encryptedChatCreated) {
            if (creatingChat) {
                AndroidUtilities.RunOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        NotificationCenter.getInstance().postNotificationName(NotificationCenter.closeChats);
                        TLRPC.EncryptedChat encryptedChat = (TLRPC.EncryptedChat)args[0];
                        Bundle args2 = new Bundle();
                        args2.putInt("enc_id", encryptedChat.id);
                        presentFragment(new ChatActivity(args2), true);
                    }
                });
            }
        } else if (id == NotificationCenter.encryptedChatUpdated) {
            TLRPC.EncryptedChat chat = (TLRPC.EncryptedChat)args[0];
            if (currentEncryptedChat != null && chat.id == currentEncryptedChat.id) {
                currentEncryptedChat = chat;
                updateRowsIds();
                if (listAdapter != null) {
                    listAdapter.notifyDataSetChanged();
                }
            }
        } else if (id == NotificationCenter.blockedUsersDidLoaded) {
            boolean oldValue = userBlocked;
            userBlocked = MessagesController.getInstance().blockedUsers.contains(user_id);
            if (oldValue != userBlocked) {
                createActionBarMenu();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public PhotoViewer.PlaceProviderObject getPlaceForPhoto(MessageObject messageObject, TLRPC.FileLocation fileLocation, int index) {
        if (fileLocation == null) {
            return null;
        }
        TLRPC.User user = MessagesController.getInstance().getUser(user_id);
        if (user != null && user.photo != null && user.photo.photo_big != null) {
            TLRPC.FileLocation photoBig = user.photo.photo_big;
            if (photoBig.local_id == fileLocation.local_id && photoBig.volume_id == fileLocation.volume_id && photoBig.dc_id == fileLocation.dc_id) {
                int count = listView.getChildCount();
                for (int a = 0; a < count; a++) {
                    View view = listView.getChildAt(a);
                    BackupImageView avatarImage = (BackupImageView)view.findViewById(br.com.uatizapi.messenger.R.id.settings_avatar_image);
                    if (avatarImage != null) {
                        int coords[] = new int[2];
                        avatarImage.getLocationInWindow(coords);
                        PhotoViewer.PlaceProviderObject object = new PhotoViewer.PlaceProviderObject();
                        object.viewX = coords[0];
                        object.viewY = coords[1] - AndroidUtilities.statusBarHeight;
                        object.parentView = listView;
                        object.imageReceiver = avatarImage.imageReceiver;
                        object.user_id = user_id;
                        object.thumb = object.imageReceiver.getBitmap();
                        object.size = -1;
                        return object;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void willSwitchFromPhoto(MessageObject messageObject, TLRPC.FileLocation fileLocation, int index) { }

    @Override
    public void willHidePhotoViewer() { }

    @Override
    public boolean isPhotoChecked(int index) { return false; }

    @Override
    public void setPhotoChecked(int index) { }

    @Override
    public void cancelButtonPressed() { }

    @Override
    public void sendButtonPressed(int index) { }

    @Override
    public int getSelectedCount() { return 0; }

    private void createActionBarMenu() {
        ActionBarMenu menu = actionBarLayer.createMenu();
        menu.clearItems();

        if (ContactsController.getInstance().contactsDict.get(user_id) == null) {
            TLRPC.User user = MessagesController.getInstance().getUser(user_id);
            if (user == null) {
                return;
            }
            ActionBarMenuItem item = menu.addItem(0, br.com.uatizapi.messenger.R.drawable.ic_ab_other);
            if (user.phone != null && user.phone.length() != 0) {
                item.addSubItem(add_contact, LocaleController.getString("AddContact", br.com.uatizapi.messenger.R.string.AddContact), 0);
                item.addSubItem(share_contact, LocaleController.getString("ShareContact", br.com.uatizapi.messenger.R.string.ShareContact), 0);
                item.addSubItem(block_contact, !userBlocked ? LocaleController.getString("BlockContact", br.com.uatizapi.messenger.R.string.BlockContact) : LocaleController.getString("Unblock", br.com.uatizapi.messenger.R.string.Unblock), 0);
            } else {
                item.addSubItem(block_contact, !userBlocked ? LocaleController.getString("BlockContact", br.com.uatizapi.messenger.R.string.BlockContact) : LocaleController.getString("Unblock", br.com.uatizapi.messenger.R.string.Unblock), 0);
            }
        } else {
            ActionBarMenuItem item = menu.addItem(0, br.com.uatizapi.messenger.R.drawable.ic_ab_other);
            item.addSubItem(share_contact, LocaleController.getString("ShareContact", br.com.uatizapi.messenger.R.string.ShareContact), 0);
            item.addSubItem(block_contact, !userBlocked ? LocaleController.getString("BlockContact", br.com.uatizapi.messenger.R.string.BlockContact) : LocaleController.getString("Unblock", br.com.uatizapi.messenger.R.string.Unblock), 0);
            item.addSubItem(edit_contact, LocaleController.getString("EditContact", br.com.uatizapi.messenger.R.string.EditContact), 0);
            item.addSubItem(delete_contact, LocaleController.getString("DeleteContact", br.com.uatizapi.messenger.R.string.DeleteContact), 0);
        }
    }

    @Override
    protected void onDialogDismiss() {
        if (listView != null) {
            listView.invalidateViews();
        }
    }

    @Override
    public void didSelectDialog(MessagesActivity messageFragment, long dialog_id, boolean param) {
        if (dialog_id != 0) {
            Bundle args = new Bundle();
            args.putBoolean("scrollToTopOnResume", true);
            NotificationCenter.getInstance().postNotificationName(NotificationCenter.closeChats);
            int lower_part = (int)dialog_id;
            if (lower_part != 0) {
                if (lower_part > 0) {
                    args.putInt("user_id", lower_part);
                } else if (lower_part < 0) {
                    args.putInt("chat_id", -lower_part);
                }
            } else {
                args.putInt("enc_id", (int)(dialog_id >> 32));
            }
            presentFragment(new ChatActivity(args), true);
            removeSelfFromStack();
            TLRPC.User user = MessagesController.getInstance().getUser(user_id);
            SendMessagesHelper.getInstance().sendMessage(user, dialog_id);
        }
    }

    private class ListAdapter extends BaseFragmentAdapter {
        private Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int i) {
            return i == phoneRow || i == settingsTimerRow || i == settingsKeyRow || i == settingsNotificationsRow || i == sharedMediaRow;
        }

        @Override
        public int getCount() {
            return rowCount;
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            int type = getItemViewType(i);
            if (type == 0) {
                BackupImageView avatarImage;
                TextView onlineText;
                TLRPC.User user = MessagesController.getInstance().getUser(user_id);
                if (view == null) {
                    LayoutInflater li = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    view = li.inflate(br.com.uatizapi.messenger.R.layout.user_profile_avatar_layout, viewGroup, false);

                    onlineText = (TextView)view.findViewById(br.com.uatizapi.messenger.R.id.settings_online);
                    avatarImage = (BackupImageView)view.findViewById(br.com.uatizapi.messenger.R.id.settings_avatar_image);
                    avatarImage.processDetach = false;
                    avatarImage.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            TLRPC.User user = MessagesController.getInstance().getUser(user_id);
                            if (user.photo != null && user.photo.photo_big != null) {
                                PhotoViewer.getInstance().setParentActivity(getParentActivity());
                                PhotoViewer.getInstance().openPhoto(user.photo.photo_big, UserProfileActivity.this);
                            }
                        }
                    });
                } else {
                    avatarImage = (BackupImageView)view.findViewById(br.com.uatizapi.messenger.R.id.settings_avatar_image);
                    onlineText = (TextView)view.findViewById(br.com.uatizapi.messenger.R.id.settings_online);
                }
                TextView textView = (TextView)view.findViewById(br.com.uatizapi.messenger.R.id.settings_name);
                Typeface typeface = AndroidUtilities.getTypeface("fonts/rmedium.ttf");
                textView.setTypeface(typeface);

                textView.setText(ContactsController.formatName(user.first_name, user.last_name));
                onlineText.setText(LocaleController.formatUserStatus(user));

                TLRPC.FileLocation photo = null;
                TLRPC.FileLocation photoBig = null;
                if (user.photo != null) {
                    photo = user.photo.photo_small;
                    photoBig = user.photo.photo_big;
                }
                avatarImage.setImage(photo, "50_50", AndroidUtilities.getUserAvatarForId(user.id));
                avatarImage.imageReceiver.setVisible(!PhotoViewer.getInstance().isShowingImage(photoBig), false);
                return view;
            } else if (type == 1) {
                if (view == null) {
                    view = new SettingsSectionLayout(mContext);
                }
                if (i == phoneSectionRow) {
                    ((SettingsSectionLayout) view).setText(LocaleController.getString("Info", br.com.uatizapi.messenger.R.string.Info));
                } else if (i == settingsSectionRow) {
                    ((SettingsSectionLayout) view).setText(LocaleController.getString("SETTINGS", br.com.uatizapi.messenger.R.string.SETTINGS));
                } else if (i == sharedMediaSectionRow) {
                    ((SettingsSectionLayout) view).setText(LocaleController.getString("SHAREDMEDIA", br.com.uatizapi.messenger.R.string.SHAREDMEDIA));
                }
            } else if (type == 2) {
                final TLRPC.User user = MessagesController.getInstance().getUser(user_id);
                if (view == null) {
                    LayoutInflater li = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    view = li.inflate(br.com.uatizapi.messenger.R.layout.user_profile_phone_layout, viewGroup, false);
                    view.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (user.phone == null || user.phone.length() == 0 || getParentActivity() == null) {
                                return;
                            }

                            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());

                            builder.setItems(new CharSequence[] {LocaleController.getString("Copy", br.com.uatizapi.messenger.R.string.Copy)}, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    if (i == 0) {
                                        int sdk = android.os.Build.VERSION.SDK_INT;
                                        if(sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
                                            android.text.ClipboardManager clipboard = (android.text.ClipboardManager)ApplicationLoader.applicationContext.getSystemService(Context.CLIPBOARD_SERVICE);
                                            clipboard.setText(user.phone);
                                        } else {
                                            android.content.ClipboardManager clipboard = (android.content.ClipboardManager)ApplicationLoader.applicationContext.getSystemService(Context.CLIPBOARD_SERVICE);
                                            android.content.ClipData clip = android.content.ClipData.newPlainText("label", user.phone);
                                            clipboard.setPrimaryClip(clip);
                                        }
                                    }
                                }
                            });
                            showAlertDialog(builder);
                        }
                    });
                    ImageButton button = (ImageButton)view.findViewById(br.com.uatizapi.messenger.R.id.settings_edit_name);
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            TLRPC.User user = MessagesController.getInstance().getUser(user_id);
                            if (user == null || user instanceof TLRPC.TL_userEmpty) {
                                return;
                            }
                            NotificationCenter.getInstance().postNotificationName(NotificationCenter.closeChats);
                            Bundle args = new Bundle();
                            args.putInt("user_id", user_id);
                            presentFragment(new ChatActivity(args), true);
                        }
                    });
                    button = (ImageButton)view.findViewById(br.com.uatizapi.messenger.R.id.settings_call_phone);
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            try {
                                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:+" + user.phone));
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                getParentActivity().startActivity(intent);
                            } catch (Exception e) {
                                FileLog.e("tmessages", e);
                            }
                        }
                    });
                }
                ImageButton button = (ImageButton)view.findViewById(br.com.uatizapi.messenger.R.id.settings_call_phone);
                button.setVisibility(user.phone == null || user.phone.length() == 0 ? View.GONE : View.VISIBLE);

                TextView textView = (TextView)view.findViewById(br.com.uatizapi.messenger.R.id.settings_row_text);
                TextView detailTextView = (TextView)view.findViewById(br.com.uatizapi.messenger.R.id.settings_row_text_detail);
                View divider = view.findViewById(br.com.uatizapi.messenger.R.id.settings_row_divider);
                if (i == phoneRow) {
                    if (user.phone != null && user.phone.length() != 0) {
                        textView.setText(PhoneFormat.getInstance().format("+" + user.phone));
                    } else {
                        textView.setText(LocaleController.getString("NumberUnknown", br.com.uatizapi.messenger.R.string.NumberUnknown));
                    }
                    divider.setVisibility(usernameRow != -1 ? View.VISIBLE : View.INVISIBLE);
                    detailTextView.setText(LocaleController.getString("PhoneMobile", br.com.uatizapi.messenger.R.string.PhoneMobile));
                }
            } else if (type == 3) {
                if (view == null) {
                    LayoutInflater li = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    view = li.inflate(br.com.uatizapi.messenger.R.layout.user_profile_leftright_row_layout, viewGroup, false);
                }
                TextView textView = (TextView)view.findViewById(br.com.uatizapi.messenger.R.id.settings_row_text);
                TextView detailTextView = (TextView)view.findViewById(br.com.uatizapi.messenger.R.id.settings_row_text_detail);

                View divider = view.findViewById(br.com.uatizapi.messenger.R.id.settings_row_divider);
                if (i == sharedMediaRow) {
                    textView.setText(LocaleController.getString("SharedMedia", br.com.uatizapi.messenger.R.string.SharedMedia));
                    if (totalMediaCount == -1) {
                        detailTextView.setText(LocaleController.getString("Loading", br.com.uatizapi.messenger.R.string.Loading));
                    } else {
                        detailTextView.setText(String.format("%d", totalMediaCount));
                    }
                    divider.setVisibility(View.INVISIBLE);
                } else if (i == settingsTimerRow) {
                    TLRPC.EncryptedChat encryptedChat = MessagesController.getInstance().getEncryptedChat((int)(dialog_id >> 32));
                    textView.setText(LocaleController.getString("MessageLifetime", br.com.uatizapi.messenger.R.string.MessageLifetime));
                    divider.setVisibility(View.VISIBLE);
                    if (encryptedChat.ttl == 0) {
                        detailTextView.setText(LocaleController.getString("ShortMessageLifetimeForever", br.com.uatizapi.messenger.R.string.ShortMessageLifetimeForever));
                    } else {
                        detailTextView.setText(AndroidUtilities.formatTTLString(encryptedChat.ttl));
                    }
                } else if (i == usernameRow) {
                    TLRPC.User user = MessagesController.getInstance().getUser(user_id);
                    textView.setText(LocaleController.getString("Username", br.com.uatizapi.messenger.R.string.Username));
                    if (user != null && user.username != null && user.username.length() != 0) {
                        detailTextView.setText("@" + user.username);
                    } else {
                        detailTextView.setText("-");
                    }
                    divider.setVisibility(View.INVISIBLE);
                }
            } else if (type == 4) {
                if (view == null) {
                    LayoutInflater li = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    view = li.inflate(br.com.uatizapi.messenger.R.layout.user_profile_identicon_layout, viewGroup, false);
                }
                TextView textView = (TextView)view.findViewById(br.com.uatizapi.messenger.R.id.settings_row_text);
                View divider = view.findViewById(br.com.uatizapi.messenger.R.id.settings_row_divider);
                divider.setVisibility(View.VISIBLE);
                IdenticonView identiconView = (IdenticonView)view.findViewById(br.com.uatizapi.messenger.R.id.identicon_view);
                TLRPC.EncryptedChat encryptedChat = MessagesController.getInstance().getEncryptedChat((int)(dialog_id >> 32));
                identiconView.setBytes(encryptedChat.auth_key);
                textView.setText(LocaleController.getString("EncryptionKey", br.com.uatizapi.messenger.R.string.EncryptionKey));
            } else if (type == 5) {
                if (view == null) {
                    LayoutInflater li = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    view = li.inflate(br.com.uatizapi.messenger.R.layout.settings_row_button_layout, viewGroup, false);
                }
                TextView textView = (TextView)view.findViewById(br.com.uatizapi.messenger.R.id.settings_row_text);
                View divider = view.findViewById(br.com.uatizapi.messenger.R.id.settings_row_divider);
                if (i == settingsNotificationsRow) {
                    textView.setText(LocaleController.getString("NotificationsAndSounds", br.com.uatizapi.messenger.R.string.NotificationsAndSounds));
                    divider.setVisibility(View.INVISIBLE);
                }
            }
            return view;
        }

        @Override
        public int getItemViewType(int i) {
            if (i == avatarRow) {
                return 0;
            } else if (i == phoneSectionRow || i == settingsSectionRow || i == sharedMediaSectionRow) {
                return 1;
            } else if (i == phoneRow) {
                return 2;
            } else if (i == sharedMediaRow || i == settingsTimerRow || i == usernameRow) {
                return 3;
            } else if (i == settingsKeyRow) {
                return 4;
            } else if (i == settingsNotificationsRow) {
                return 5;
            }
            return 0;
        }

        @Override
        public int getViewTypeCount() {
            return 6;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }
    }
}
