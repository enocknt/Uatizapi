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
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import br.com.uatizapi.android.AndroidUtilities;
import br.com.uatizapi.android.LocaleController;
import br.com.uatizapi.messenger.TLObject;
import br.com.uatizapi.messenger.TLRPC;
import br.com.uatizapi.android.ContactsController;
import br.com.uatizapi.messenger.FileLog;
import br.com.uatizapi.android.MessagesController;
import br.com.uatizapi.android.MessagesStorage;
import br.com.uatizapi.android.NotificationCenter;
import br.com.uatizapi.messenger.UserConfig;
import br.com.uatizapi.messenger.Utilities;
import br.com.uatizapi.ui.Adapters.BaseContactsSearchAdapter;
import br.com.uatizapi.ui.Cells.ChatOrUserCell;
import br.com.uatizapi.ui.Cells.DialogCell;
import br.com.uatizapi.ui.Views.ActionBar.ActionBarLayer;
import br.com.uatizapi.ui.Views.ActionBar.ActionBarMenu;
import br.com.uatizapi.ui.Views.ActionBar.ActionBarMenuItem;
import br.com.uatizapi.ui.Views.ActionBar.BaseFragment;
import br.com.uatizapi.ui.Views.SettingsSectionLayout;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MessagesActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {
    private ListView messagesListView;
    private MessagesAdapter messagesListViewAdapter;
    private TextView searchEmptyView;
    private View progressView;
    private View emptyView;
    private String selectAlertString;
    private String selectAlertStringGroup;
    private boolean serverOnly = false;

    private static boolean dialogsLoaded = false;
    private boolean searching = false;
    private boolean searchWas = false;
    private boolean onlySelect = false;
    private int activityToken = (int)(Utilities.random.nextDouble() * Integer.MAX_VALUE);
    private long selectedDialog;

    private MessagesActivityDelegate delegate;

    private long openedDialogId = 0;

    private final static int messages_list_menu_new_messages = 1;
    private final static int messages_list_menu_new_chat = 2;
    private final static int messages_list_menu_other = 6;
    private final static int messages_list_menu_new_secret_chat = 3;
    private final static int messages_list_menu_contacts = 4;
    private final static int messages_list_menu_settings = 5;
    private final static int messages_list_menu_new_broadcast = 6;

    public static interface MessagesActivityDelegate {
        public abstract void didSelectDialog(MessagesActivity fragment, long dialog_id, boolean param);
    }

    public MessagesActivity(Bundle args) {
        super(args);
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.dialogsNeedReload);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.emojiDidLoaded);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.updateInterfaces);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.reloadSearchResults);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.encryptedChatUpdated);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.contactsDidLoaded);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.appDidLogout);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.openedChatChanged);
        if (getArguments() != null) {
            onlySelect = arguments.getBoolean("onlySelect", false);
            serverOnly = arguments.getBoolean("serverOnly", false);
            selectAlertString = arguments.getString("selectAlertString");
            selectAlertStringGroup = arguments.getString("selectAlertStringGroup");
        }
        if (!dialogsLoaded) {
            MessagesController.getInstance().loadDialogs(0, 0, 100, true);
            dialogsLoaded = true;
        }
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.dialogsNeedReload);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.emojiDidLoaded);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.updateInterfaces);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.reloadSearchResults);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.encryptedChatUpdated);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.contactsDidLoaded);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.appDidLogout);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.openedChatChanged);
        delegate = null;
    }

    @Override
    public View createView(LayoutInflater inflater, ViewGroup container) {
        if (fragmentView == null) {
            ActionBarMenu menu = actionBarLayer.createMenu();
            menu.addItem(0, br.com.uatizapi.messenger.R.drawable.ic_ab_search).setIsSearchField(true).setActionBarMenuItemSearchListener(new ActionBarMenuItem.ActionBarMenuItemSearchListener() {
                @Override
                public void onSearchExpand() {
                    searching = true;
                    if (messagesListView != null) {
                        messagesListView.setEmptyView(searchEmptyView);
                    }
                    if (emptyView != null) {
                        emptyView.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onSearchCollapse() {
                    searching = false;
                    searchWas = false;
                    if (messagesListView != null) {
                        messagesListView.setEmptyView(emptyView);
                        searchEmptyView.setVisibility(View.GONE);
                    }
                    if (messagesListViewAdapter != null) {
                        messagesListViewAdapter.searchDialogs(null);
                    }
                }

                @Override
                public void onTextChanged(EditText editText) {
                    String text = editText.getText().toString();
                    if (messagesListViewAdapter != null) {
                        messagesListViewAdapter.searchDialogs(text);
                    }
                    if (text.length() != 0) {
                        searchWas = true;
                        if (messagesListViewAdapter != null) {
                            messagesListViewAdapter.notifyDataSetChanged();
                        }
                        if (searchEmptyView != null) {
                            messagesListView.setEmptyView(searchEmptyView);
                            emptyView.setVisibility(View.GONE);
                        }
                    }
                }
            });
            if (onlySelect) {
                actionBarLayer.setDisplayHomeAsUpEnabled(true, br.com.uatizapi.messenger.R.drawable.ic_ab_back);
                actionBarLayer.setTitle(LocaleController.getString("SelectChat", br.com.uatizapi.messenger.R.string.SelectChat));
            } else {
                actionBarLayer.setDisplayUseLogoEnabled(true, br.com.uatizapi.messenger.R.drawable.ic_ab_logo);
                actionBarLayer.setTitle(LocaleController.getString("AppName", br.com.uatizapi.messenger.R.string.AppName));
                menu.addItem(messages_list_menu_new_messages, br.com.uatizapi.messenger.R.drawable.ic_ab_compose);
                ActionBarMenuItem item = menu.addItem(0, br.com.uatizapi.messenger.R.drawable.ic_ab_other);
                item.addSubItem(messages_list_menu_new_chat, LocaleController.getString("NewGroup", br.com.uatizapi.messenger.R.string.NewGroup), 0);
                item.addSubItem(messages_list_menu_new_secret_chat, LocaleController.getString("NewSecretChat", br.com.uatizapi.messenger.R.string.NewSecretChat), 0);
                item.addSubItem(messages_list_menu_new_broadcast, LocaleController.getString("NewBroadcastList", br.com.uatizapi.messenger.R.string.NewBroadcastList), 0);
                item.addSubItem(messages_list_menu_contacts, LocaleController.getString("Contacts", br.com.uatizapi.messenger.R.string.Contacts), 0);
                item.addSubItem(messages_list_menu_settings, LocaleController.getString("Settings", br.com.uatizapi.messenger.R.string.Settings), 0);
            }
            actionBarLayer.setBackOverlay(br.com.uatizapi.messenger.R.layout.updating_state_layout);

            actionBarLayer.setActionBarMenuOnItemClick(new ActionBarLayer.ActionBarMenuOnItemClick() {
                @Override
                public void onItemClick(int id) {
                    if (id == messages_list_menu_settings) {
                        presentFragment(new SettingsActivity());
                    } else if (id == messages_list_menu_contacts) {
                        presentFragment(new ContactsActivity(null));
                    } else if (id == messages_list_menu_new_messages) {
                        Bundle args = new Bundle();
                        args.putBoolean("onlyUsers", true);
                        args.putBoolean("destroyAfterSelect", true);
                        args.putBoolean("usersAsSections", true);
                        presentFragment(new ContactsActivity(args));
                    } else if (id == messages_list_menu_new_secret_chat) {
                        Bundle args = new Bundle();
                        args.putBoolean("onlyUsers", true);
                        args.putBoolean("destroyAfterSelect", true);
                        args.putBoolean("usersAsSections", true);
                        args.putBoolean("createSecretChat", true);
                        presentFragment(new ContactsActivity(args));
                    } else if (id == messages_list_menu_new_chat) {
                        presentFragment(new GroupCreateActivity());
                    } else if (id == -1) {
                        if (onlySelect) {
                            finishFragment();
                        }
                    } else if (id == messages_list_menu_new_broadcast) {
                        Bundle args = new Bundle();
                        args.putBoolean("broadcast", true);
                        presentFragment(new GroupCreateActivity(args));
                    }
                }
            });

            searching = false;
            searchWas = false;

            fragmentView = inflater.inflate(br.com.uatizapi.messenger.R.layout.messages_list, container, false);

            messagesListViewAdapter = new MessagesAdapter(getParentActivity());

            messagesListView = (ListView)fragmentView.findViewById(br.com.uatizapi.messenger.R.id.messages_list_view);
            messagesListView.setAdapter(messagesListViewAdapter);

            progressView = fragmentView.findViewById(br.com.uatizapi.messenger.R.id.progressLayout);
            messagesListViewAdapter.notifyDataSetChanged();
            searchEmptyView = (TextView)fragmentView.findViewById(br.com.uatizapi.messenger.R.id.searchEmptyView);
            searchEmptyView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return true;
                }
            });
            searchEmptyView.setText(LocaleController.getString("NoResult", br.com.uatizapi.messenger.R.string.NoResult));
            emptyView = fragmentView.findViewById(br.com.uatizapi.messenger.R.id.list_empty_view);
            emptyView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return true;
                }
            });
            TextView textView = (TextView)fragmentView.findViewById(br.com.uatizapi.messenger.R.id.list_empty_view_text1);
            textView.setText(LocaleController.getString("NoChats", br.com.uatizapi.messenger.R.string.NoChats));
            textView = (TextView)fragmentView.findViewById(br.com.uatizapi.messenger.R.id.list_empty_view_text2);
            textView.setText(LocaleController.getString("NoChats", br.com.uatizapi.messenger.R.string.NoChatsHelp));

            if (MessagesController.getInstance().loadingDialogs && MessagesController.getInstance().dialogs.isEmpty()) {
                messagesListView.setEmptyView(null);
                searchEmptyView.setVisibility(View.GONE);
                emptyView.setVisibility(View.GONE);
                progressView.setVisibility(View.VISIBLE);
            } else {
                if (searching && searchWas) {
                    messagesListView.setEmptyView(searchEmptyView);
                    emptyView.setVisibility(View.GONE);
                } else {
                    messagesListView.setEmptyView(emptyView);
                    searchEmptyView.setVisibility(View.GONE);
                }
                progressView.setVisibility(View.GONE);
            }

            messagesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    if (messagesListViewAdapter == null) {
                        return;
                    }
                    TLObject obj = messagesListViewAdapter.getItem(i);
                    if (obj == null) {
                        return;
                    }
                    long dialog_id = 0;
                    if (obj instanceof TLRPC.User) {
                        dialog_id = ((TLRPC.User) obj).id;
                        if (messagesListViewAdapter.isGlobalSearch(i)) {
                            ArrayList<TLRPC.User> users = new ArrayList<TLRPC.User>();
                            users.add((TLRPC.User)obj);
                            MessagesController.getInstance().putUsers(users, false);
                            MessagesStorage.getInstance().putUsersAndChats(users, null, false, true);
                        }
                    } else if (obj instanceof TLRPC.Chat) {
                        if (((TLRPC.Chat) obj).id > 0) {
                            dialog_id = -((TLRPC.Chat) obj).id;
                        } else {
                            dialog_id = AndroidUtilities.makeBroadcastId(((TLRPC.Chat) obj).id);
                        }
                    } else if (obj instanceof TLRPC.EncryptedChat) {
                        dialog_id = ((long)((TLRPC.EncryptedChat) obj).id) << 32;
                    } else if (obj instanceof TLRPC.TL_dialog) {
                        dialog_id = ((TLRPC.TL_dialog) obj).id;
                    }

                    if (onlySelect) {
                        didSelectResult(dialog_id, true, false);
                    } else {
                        Bundle args = new Bundle();
                        int lower_part = (int)dialog_id;
                        int high_id = (int)(dialog_id >> 32);
                        if (lower_part != 0) {
                            if (high_id == 1) {
                                args.putInt("chat_id", lower_part);
                            } else {
                                if (lower_part > 0) {
                                    args.putInt("user_id", lower_part);
                                } else if (lower_part < 0) {
                                    args.putInt("chat_id", -lower_part);
                                }
                            }
                        } else {
                            args.putInt("enc_id", high_id);
                        }
                        if (AndroidUtilities.isTablet()) {
                            if (openedDialogId == dialog_id) {
                                return;
                            }
                            openedDialogId = dialog_id;
                        }
                        presentFragment(new ChatActivity(args));
                        updateVisibleRows(0);
                    }
                }
            });

            messagesListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                    if (onlySelect || searching && searchWas || getParentActivity() == null) {
                        return false;
                    }
                    TLRPC.TL_dialog dialog;
                    if (serverOnly) {
                        if (i >= MessagesController.getInstance().dialogsServerOnly.size()) {
                            return false;
                        }
                        dialog = MessagesController.getInstance().dialogsServerOnly.get(i);
                    } else {
                        if (i >= MessagesController.getInstance().dialogs.size()) {
                            return false;
                        }
                        dialog = MessagesController.getInstance().dialogs.get(i);
                    }
                    selectedDialog = dialog.id;

                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setTitle(LocaleController.getString("AppName", br.com.uatizapi.messenger.R.string.AppName));

                    int lower_id = (int)selectedDialog;
                    int high_id = (int)(selectedDialog >> 32);

                    if (lower_id < 0 && high_id != 1) {
                        builder.setItems(new CharSequence[]{LocaleController.getString("ClearHistory", br.com.uatizapi.messenger.R.string.ClearHistory), LocaleController.getString("DeleteChat", br.com.uatizapi.messenger.R.string.DeleteChat)}, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == 0) {
                                    MessagesController.getInstance().deleteDialog(selectedDialog, 0, true);
                                } else if (which == 1) {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                                    builder.setMessage(LocaleController.getString("AreYouSureDeleteAndExit", br.com.uatizapi.messenger.R.string.AreYouSureDeleteAndExit));
                                    builder.setTitle(LocaleController.getString("AppName", br.com.uatizapi.messenger.R.string.AppName));
                                    builder.setPositiveButton(LocaleController.getString("OK", br.com.uatizapi.messenger.R.string.OK), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            MessagesController.getInstance().deleteUserFromChat((int) -selectedDialog, MessagesController.getInstance().getUser(UserConfig.getClientUserId()), null);
                                            MessagesController.getInstance().deleteDialog(selectedDialog, 0, false);
                                            if (AndroidUtilities.isTablet()) {
                                                NotificationCenter.getInstance().postNotificationName(NotificationCenter.closeChats, selectedDialog);
                                            }
                                        }
                                    });
                                    builder.setNegativeButton(LocaleController.getString("Cancel", br.com.uatizapi.messenger.R.string.Cancel), null);
                                    showAlertDialog(builder);
                                }
                            }
                        });
                    } else {
                        builder.setItems(new CharSequence[]{LocaleController.getString("ClearHistory", br.com.uatizapi.messenger.R.string.ClearHistory), LocaleController.getString("Delete", br.com.uatizapi.messenger.R.string.Delete)}, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == 0) {
                                    MessagesController.getInstance().deleteDialog(selectedDialog, 0, true);
                                } else {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                                    builder.setMessage(LocaleController.getString("AreYouSureDeleteThisChat", br.com.uatizapi.messenger.R.string.AreYouSureDeleteThisChat));
                                    builder.setTitle(LocaleController.getString("AppName", br.com.uatizapi.messenger.R.string.AppName));
                                    builder.setPositiveButton(LocaleController.getString("OK", br.com.uatizapi.messenger.R.string.OK), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            MessagesController.getInstance().deleteDialog(selectedDialog, 0, false);
                                            if (AndroidUtilities.isTablet()) {
                                                NotificationCenter.getInstance().postNotificationName(NotificationCenter.closeChats, selectedDialog);
                                            }
                                        }
                                    });
                                    builder.setNegativeButton(LocaleController.getString("Cancel", br.com.uatizapi.messenger.R.string.Cancel), null);
                                    showAlertDialog(builder);
                                }
                            }
                        });
                    }
                    builder.setNegativeButton(LocaleController.getString("Cancel", br.com.uatizapi.messenger.R.string.Cancel), null);
                    showAlertDialog(builder);
                    return true;
                }
            });

            messagesListView.setOnScrollListener(new AbsListView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(AbsListView absListView, int i) {
                    if (i == SCROLL_STATE_TOUCH_SCROLL && searching && searchWas) {
                        AndroidUtilities.hideKeyboard(getParentActivity().getCurrentFocus());
                    }
                }

                @Override
                public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                    if (searching && searchWas) {
                        return;
                    }
                    if (visibleItemCount > 0) {
                        if (absListView.getLastVisiblePosition() == MessagesController.getInstance().dialogs.size() && !serverOnly || absListView.getLastVisiblePosition() == MessagesController.getInstance().dialogsServerOnly.size() && serverOnly) {
                            MessagesController.getInstance().loadDialogs(MessagesController.getInstance().dialogs.size(), MessagesController.getInstance().dialogsServerOnly.size(), 100, true);
                        }
                    }
                }
            });
        } else {
            ViewGroup parent = (ViewGroup)fragmentView.getParent();
            if (parent != null) {
                parent.removeView(fragmentView);
            }
        }
        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        showActionBar();
        if (messagesListViewAdapter != null) {
            messagesListViewAdapter.notifyDataSetChanged();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void didReceivedNotification(int id, Object... args) {
        if (id == NotificationCenter.dialogsNeedReload) {
            if (messagesListViewAdapter != null) {
                messagesListViewAdapter.notifyDataSetChanged();
            }
            if (messagesListView != null) {
                if (MessagesController.getInstance().loadingDialogs && MessagesController.getInstance().dialogs.isEmpty()) {
                    if (messagesListView.getEmptyView() != null) {
                        messagesListView.setEmptyView(null);
                    }
                    searchEmptyView.setVisibility(View.GONE);
                    emptyView.setVisibility(View.GONE);
                    progressView.setVisibility(View.VISIBLE);
                } else {
                    if (messagesListView.getEmptyView() == null) {
                        if (searching && searchWas) {
                            messagesListView.setEmptyView(searchEmptyView);
                            emptyView.setVisibility(View.GONE);
                        } else {
                            messagesListView.setEmptyView(emptyView);
                            searchEmptyView.setVisibility(View.GONE);
                        }
                    }
                    progressView.setVisibility(View.GONE);
                }
            }
        } else if (id == NotificationCenter.emojiDidLoaded) {
            if (messagesListView != null) {
                updateVisibleRows(0);
            }
        } else if (id == NotificationCenter.updateInterfaces) {
            updateVisibleRows((Integer)args[0]);
        } else if (id == NotificationCenter.reloadSearchResults) {
            int token = (Integer)args[0];
            if (token == activityToken) {
                messagesListViewAdapter.updateSearchResults((ArrayList<TLObject>) args[1], (ArrayList<CharSequence>) args[2], (ArrayList<TLRPC.User>) args[3]);
            }
        } else if (id == NotificationCenter.appDidLogout) {
            dialogsLoaded = false;
        } else if (id == NotificationCenter.encryptedChatUpdated) {
            updateVisibleRows(0);
        } else if (id == NotificationCenter.contactsDidLoaded) {
            updateVisibleRows(0);
        } else if (id == NotificationCenter.openedChatChanged) {
            if (!serverOnly && AndroidUtilities.isTablet()) {
                boolean close = (Boolean)args[1];
                long dialog_id = (Long)args[0];
                if (close) {
                    if (dialog_id == openedDialogId) {
                        openedDialogId = 0;
                    }
                } else {
                    openedDialogId = dialog_id;
                }
                updateVisibleRows(0);
            }
        }
    }

    private void updateVisibleRows(int mask) {
        if (messagesListView == null) {
            return;
        }
        int count = messagesListView.getChildCount();
        for (int a = 0; a < count; a++) {
            View child = messagesListView.getChildAt(a);
            if (child instanceof DialogCell) {
                DialogCell cell = (DialogCell) child;
                if (!serverOnly && AndroidUtilities.isTablet() && cell.getDialog() != null) {
                    if (cell.getDialog().id == openedDialogId) {
                        child.setBackgroundColor(0x0f000000);
                    } else {
                        child.setBackgroundColor(0);
                    }
                }
                cell.update(mask);
            } else if (child instanceof ChatOrUserCell) {
                ((ChatOrUserCell) child).update(mask);
            }
        }
    }

    public void setDelegate(MessagesActivityDelegate delegate) {
        this.delegate = delegate;
    }

    public MessagesActivityDelegate getDelegate() {
        return delegate;
    }

    private void didSelectResult(final long dialog_id, boolean useAlert, final boolean param) {
        if (useAlert && selectAlertString != null && selectAlertStringGroup != null) {
            if (getParentActivity() == null) {
                return;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
            builder.setTitle(LocaleController.getString("AppName", br.com.uatizapi.messenger.R.string.AppName));
            int lower_part = (int)dialog_id;
            int high_id = (int)(dialog_id >> 32);
            if (lower_part != 0) {
                if (high_id == 1) {
                    TLRPC.Chat chat = MessagesController.getInstance().getChat(lower_part);
                    if (chat == null) {
                        return;
                    }
                    builder.setMessage(LocaleController.formatStringSimple(selectAlertStringGroup, chat.title));
                } else {
                    if (lower_part > 0) {
                        TLRPC.User user = MessagesController.getInstance().getUser(lower_part);
                        if (user == null) {
                            return;
                        }
                        builder.setMessage(LocaleController.formatStringSimple(selectAlertString, ContactsController.formatName(user.first_name, user.last_name)));
                    } else if (lower_part < 0) {
                        TLRPC.Chat chat = MessagesController.getInstance().getChat(-lower_part);
                        if (chat == null) {
                            return;
                        }
                        builder.setMessage(LocaleController.formatStringSimple(selectAlertStringGroup, chat.title));
                    }
                }
            } else {
                TLRPC.EncryptedChat chat = MessagesController.getInstance().getEncryptedChat(high_id);
                TLRPC.User user = MessagesController.getInstance().getUser(chat.user_id);
                if (user == null) {
                    return;
                }
                builder.setMessage(LocaleController.formatStringSimple(selectAlertString, ContactsController.formatName(user.first_name, user.last_name)));
            }
            CheckBox checkBox = null;
            /*if (delegate instanceof ChatActivity) {
                checkBox = new CheckBox(getParentActivity());
                checkBox.setText(LocaleController.getString("ForwardFromMyName", R.string.ForwardFromMyName));
                checkBox.setChecked(false);
                builder.setView(checkBox);
            }*/
            final CheckBox checkBoxFinal = checkBox;
            builder.setPositiveButton(br.com.uatizapi.messenger.R.string.OK, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    didSelectResult(dialog_id, false, checkBoxFinal != null && checkBoxFinal.isChecked());
                }
            });
            builder.setNegativeButton(br.com.uatizapi.messenger.R.string.Cancel, null);
            showAlertDialog(builder);
            if (checkBox != null) {
                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams)checkBox.getLayoutParams();
                if (layoutParams != null) {
                    layoutParams.rightMargin = layoutParams.leftMargin = AndroidUtilities.dp(10);
                    checkBox.setLayoutParams(layoutParams);
                }
            }
        } else {
            if (delegate != null) {
                delegate.didSelectDialog(MessagesActivity.this, dialog_id, param);
                delegate = null;
            } else {
                finishFragment();
            }
        }
    }

    private class MessagesAdapter extends BaseContactsSearchAdapter {

        private Context mContext;
        private Timer searchTimer;
        private ArrayList<TLObject> searchResult = new ArrayList<TLObject>();
        private ArrayList<CharSequence> searchResultNames = new ArrayList<CharSequence>();

        public MessagesAdapter(Context context) {
            mContext = context;
        }

        public void updateSearchResults(final ArrayList<TLObject> result, final ArrayList<CharSequence> names, final ArrayList<TLRPC.User> encUsers) {
            AndroidUtilities.RunOnUIThread(new Runnable() {
                @Override
                public void run() {
                    for (TLObject obj : result) {
                        if (obj instanceof TLRPC.User) {
                            TLRPC.User user = (TLRPC.User) obj;
                            MessagesController.getInstance().putUser(user, true);
                        } else if (obj instanceof TLRPC.Chat) {
                            TLRPC.Chat chat = (TLRPC.Chat) obj;
                            MessagesController.getInstance().putChat(chat, true);
                        } else if (obj instanceof TLRPC.EncryptedChat) {
                            TLRPC.EncryptedChat chat = (TLRPC.EncryptedChat) obj;
                            MessagesController.getInstance().putEncryptedChat(chat, true);
                        }
                    }
                    for (TLRPC.User user : encUsers) {
                        MessagesController.getInstance().putUser(user, true);
                    }
                    searchResult = result;
                    searchResultNames = names;
                    if (searching) {
                        messagesListViewAdapter.notifyDataSetChanged();
                    }
                }
            });
        }

        public boolean isGlobalSearch(int i) {
            if (searching && searchWas) {
                int localCount = searchResult.size();
                int globalCount = globalSearch.size();
                if (i >= 0 && i < localCount) {
                    return false;
                } else if (i > localCount && i <= globalCount + localCount) {
                    return true;
                }
            }
            return false;
        }

        public void searchDialogs(final String query) {
            if (query == null) {
                searchResult.clear();
                searchResultNames.clear();
                queryServerSearch(null);
                notifyDataSetChanged();
            } else {
                try {
                    if (searchTimer != null) {
                        searchTimer.cancel();
                    }
                } catch (Exception e) {
                    FileLog.e("tmessages", e);
                }
                searchTimer = new Timer();
                searchTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            searchTimer.cancel();
                            searchTimer = null;
                        } catch (Exception e) {
                            FileLog.e("tmessages", e);
                        }
                        MessagesStorage.getInstance().searchDialogs(activityToken, query, !serverOnly);
                        AndroidUtilities.RunOnUIThread(new Runnable() {
                            @Override
                            public void run() {
                                queryServerSearch(query);
                            }
                        });
                    }
                }, 200, 300);
            }
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int i) {
            return !(searching && searchWas) || i != searchResult.size();
        }

        @Override
        public int getCount() {
            if (searching && searchWas) {
                int count = searchResult.size();
                int globalCount = globalSearch.size();
                if (globalCount != 0) {
                    count += globalCount + 1;
                }
                return count;
            }
            int count;
            if (serverOnly) {
                count = MessagesController.getInstance().dialogsServerOnly.size();
            } else {
                count = MessagesController.getInstance().dialogs.size();
            }
            if (count == 0 && MessagesController.getInstance().loadingDialogs) {
                return 0;
            }
            if (!MessagesController.getInstance().dialogsEndReached) {
                count++;
            }
            return count;
        }

        @Override
        public TLObject getItem(int i) {
            if (searching && searchWas) {
                int localCount = searchResult.size();
                int globalCount = globalSearch.size();
                if (i >= 0 && i < localCount) {
                    return searchResult.get(i);
                } else if (i > localCount && i <= globalCount + localCount) {
                    return globalSearch.get(i - localCount - 1);
                }
                return null;
            }
            if (serverOnly) {
                if (i < 0 || i >= MessagesController.getInstance().dialogsServerOnly.size()) {
                    return null;
                }
                return MessagesController.getInstance().dialogsServerOnly.get(i);
            } else {
                if (i < 0 || i >= MessagesController.getInstance().dialogs.size()) {
                    return null;
                }
                return MessagesController.getInstance().dialogs.get(i);
            }
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            int type = getItemViewType(i);

            if (type == 3) {
                if (view == null) {
                    view = new SettingsSectionLayout(mContext);
                    ((SettingsSectionLayout) view).setText(LocaleController.getString("GlobalSearch", br.com.uatizapi.messenger.R.string.GlobalSearch));
                    view.setPadding(AndroidUtilities.dp(11), 0, AndroidUtilities.dp(11), 0);
                }
            } else if (type == 2) {
                if (view == null) {
                    view = new ChatOrUserCell(mContext);
                }
                if (searching && searchWas) {
                    TLRPC.User user = null;
                    TLRPC.Chat chat = null;
                    TLRPC.EncryptedChat encryptedChat = null;

                    ((ChatOrUserCell) view).useSeparator = (i != getCount() - 1 && i != searchResult.size() - 1);
                    TLObject obj = getItem(i);
                    if (obj instanceof TLRPC.User) {
                        user = MessagesController.getInstance().getUser(((TLRPC.User) obj).id);
                        if (user == null) {
                            user = (TLRPC.User) obj;
                        }
                    } else if (obj instanceof TLRPC.Chat) {
                        chat = MessagesController.getInstance().getChat(((TLRPC.Chat) obj).id);
                    } else if (obj instanceof TLRPC.EncryptedChat) {
                        encryptedChat = MessagesController.getInstance().getEncryptedChat(((TLRPC.EncryptedChat) obj).id);
                        user = MessagesController.getInstance().getUser(encryptedChat.user_id);
                    }

                    CharSequence username = null;
                    CharSequence name = null;
                    if (i < searchResult.size()) {
                        name = searchResultNames.get(i);
                        if (name != null && user != null && user.username != null && user.username.length() > 0) {
                            if (name.toString().startsWith("@" + user.username)) {
                                username = name;
                                name = null;
                            }
                        }
                    } else if (i > searchResult.size() && user != null && user.username != null) {
                        try {
                            username = Html.fromHtml(String.format("<font color=\"#357aa8\">@%s</font>%s", user.username.substring(0, lastFoundUsername.length()), user.username.substring(lastFoundUsername.length())));
                        } catch (Exception e) {
                            username = user.username;
                            FileLog.e("tmessages", e);
                        }
                    }

                    ((ChatOrUserCell) view).setData(user, chat, encryptedChat, name, username);
                }
            } else if (type == 1) {
                if (view == null) {
                    LayoutInflater li = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    view = li.inflate(br.com.uatizapi.messenger.R.layout.loading_more_layout, viewGroup, false);
                }
            } else if (type == 0) {
                if (view == null) {
                    view = new DialogCell(mContext);
                }
                ((DialogCell) view).useSeparator = (i != getCount() - 1);
                if (serverOnly) {
                    ((DialogCell) view).setDialog(MessagesController.getInstance().dialogsServerOnly.get(i));
                } else {
                    TLRPC.TL_dialog dialog = MessagesController.getInstance().dialogs.get(i);
                    if (AndroidUtilities.isTablet()) {
                        if (dialog.id == openedDialogId) {
                            view.setBackgroundColor(0x0f000000);
                        } else {
                            view.setBackgroundColor(0);
                        }
                    }
                    ((DialogCell) view).setDialog(dialog);
                }
            }

            return view;
        }

        @Override
        public int getItemViewType(int i) {
            if (searching && searchWas) {
                if (i == searchResult.size()) {
                    return 3;
                }
                return 2;
            }
            if (serverOnly && i == MessagesController.getInstance().dialogsServerOnly.size() || !serverOnly && i == MessagesController.getInstance().dialogs.size()) {
                return 1;
            }
            return 0;
        }

        @Override
        public int getViewTypeCount() {
            return 4;
        }

        @Override
        public boolean isEmpty() {
            if (searching && searchWas) {
                return searchResult.size() == 0 && globalSearch.isEmpty();
            }
            if (MessagesController.getInstance().loadingDialogs && MessagesController.getInstance().dialogs.isEmpty()) {
                return false;
            }
            int count;
            if (serverOnly) {
                count = MessagesController.getInstance().dialogsServerOnly.size();
            } else {
                count = MessagesController.getInstance().dialogs.size();
            }
            if (count == 0 && MessagesController.getInstance().loadingDialogs) {
                return true;
            }
            if (!MessagesController.getInstance().dialogsEndReached) {
                count++;
            }
            return count == 0;
        }
    }
}
