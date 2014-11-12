/*
 * This is the source code of Telegram for Android v. 1.3.2.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013.
 */

package br.com.uatizapi.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import br.com.uatizapi.android.AndroidUtilities;
import br.com.uatizapi.android.LocaleController;
import br.com.uatizapi.android.NotificationsController;
import br.com.uatizapi.android.NotificationCenter;
import br.com.uatizapi.messenger.TLObject;
import br.com.uatizapi.messenger.TLRPC;
import br.com.uatizapi.messenger.ConnectionsManager;
import br.com.uatizapi.messenger.FileLog;
import br.com.uatizapi.android.MessagesController;
import br.com.uatizapi.messenger.RPCRequest;
import br.com.uatizapi.ui.Adapters.BaseFragmentAdapter;
import br.com.uatizapi.ui.Views.ActionBar.ActionBarLayer;
import br.com.uatizapi.ui.Views.ActionBar.BaseFragment;
import br.com.uatizapi.ui.Views.ColorPickerView;
import br.com.uatizapi.ui.Views.SettingsSectionLayout;

public class SettingsNotificationsActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {
    private ListView listView;
    private boolean reseting = false;

    private int notificationsServiceRow;
    private int messageSectionRow;
    private int messageAlertRow;
    private int messagePreviewRow;
    private int messageVibrateRow;
    private int messageSoundRow;
    private int messageLedRow;
    private int messagePopupNotificationRow;
    private int groupSectionRow;
    private int groupAlertRow;
    private int groupPreviewRow;
    private int groupVibrateRow;
    private int groupSoundRow;
    private int groupLedRow;
    private int groupPopupNotificationRow;
    private int inappSectionRow;
    private int inappSoundRow;
    private int inappVibrateRow;
    private int inappPreviewRow;
    private int eventsSectionRow;
    private int contactJoinedRow;
    private int otherSectionRow;
    private int badgeNumberRow;
    private int pebbleAlertRow;
    private int resetSectionRow;
    private int resetNotificationsRow;
    private int rowCount = 0;

    @Override
    public boolean onFragmentCreate() {
        notificationsServiceRow = rowCount++;
        messageSectionRow = rowCount++;
        messageAlertRow = rowCount++;
        messagePreviewRow = rowCount++;
        messageVibrateRow = rowCount++;
        messageLedRow = rowCount++;
        messagePopupNotificationRow = rowCount++;
        messageSoundRow = rowCount++;
        groupSectionRow = rowCount++;
        groupAlertRow = rowCount++;
        groupPreviewRow = rowCount++;
        groupVibrateRow = rowCount++;
        groupLedRow = rowCount++;
        groupPopupNotificationRow = rowCount++;
        groupSoundRow = rowCount++;
        inappSectionRow = rowCount++;
        inappSoundRow = rowCount++;
        inappVibrateRow = rowCount++;
        inappPreviewRow = rowCount++;
        eventsSectionRow = rowCount++;
        contactJoinedRow = rowCount++;
        otherSectionRow = rowCount++;
        badgeNumberRow = rowCount++;
        pebbleAlertRow = rowCount++;
        resetSectionRow = rowCount++;
        resetNotificationsRow = rowCount++;

        NotificationCenter.getInstance().addObserver(this, NotificationCenter.notificationsSettingsUpdated);

        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.notificationsSettingsUpdated);
    }

    @Override
    public View createView(LayoutInflater inflater, ViewGroup container) {
        if (fragmentView == null) {
            actionBarLayer.setDisplayHomeAsUpEnabled(true, br.com.uatizapi.messenger.R.drawable.ic_ab_back);
            actionBarLayer.setBackOverlay(br.com.uatizapi.messenger.R.layout.updating_state_layout);
            actionBarLayer.setTitle(LocaleController.getString("NotificationsAndSounds", br.com.uatizapi.messenger.R.string.NotificationsAndSounds));
            actionBarLayer.setActionBarMenuOnItemClick(new ActionBarLayer.ActionBarMenuOnItemClick() {
                @Override
                public void onItemClick(int id) {
                    if (id == -1) {
                        finishFragment();
                    }
                }
            });

            fragmentView = inflater.inflate(br.com.uatizapi.messenger.R.layout.settings_layout, container, false);
            final ListAdapter listAdapter = new ListAdapter(getParentActivity());
            listView = (ListView)fragmentView.findViewById(br.com.uatizapi.messenger.R.id.listView);
            listView.setAdapter(listAdapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, final int i, long l) {
                    if (i == messageAlertRow || i == groupAlertRow) {
                        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                        SharedPreferences.Editor editor = preferences.edit();
                        boolean enabled;
                        if (i == messageAlertRow) {
                            enabled = preferences.getBoolean("EnableAll", true);
                            editor.putBoolean("EnableAll", !enabled);
                        } else if (i == groupAlertRow) {
                            enabled = preferences.getBoolean("EnableGroup", true);
                            editor.putBoolean("EnableGroup", !enabled);
                        }
                        editor.commit();
                        listView.invalidateViews();
                        updateServerNotificationsSettings(i == groupAlertRow);
                    } else if (i == messagePreviewRow || i == groupPreviewRow) {
                        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                        SharedPreferences.Editor editor = preferences.edit();
                        boolean enabled;
                        if (i == messagePreviewRow) {
                            enabled = preferences.getBoolean("EnablePreviewAll", true);
                            editor.putBoolean("EnablePreviewAll", !enabled);
                        } else if (i == groupPreviewRow) {
                            enabled = preferences.getBoolean("EnablePreviewGroup", true);
                            editor.putBoolean("EnablePreviewGroup", !enabled);
                        }
                        editor.commit();
                        listView.invalidateViews();
                        updateServerNotificationsSettings(i == groupPreviewRow);
                    } else if (i == messageSoundRow || i == groupSoundRow) {
                        try {
                            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                            Intent tmpIntent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                            tmpIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
                            tmpIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
                            tmpIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
                            Uri currentSound = null;

                            String defaultPath = null;
                            Uri defaultUri = Settings.System.DEFAULT_NOTIFICATION_URI;
                            if (defaultUri != null) {
                                defaultPath = defaultUri.getPath();
                            }

                            if (i == messageSoundRow) {
                                String path = preferences.getString("GlobalSoundPath", defaultPath);
                                if (path != null && !path.equals("NoSound")) {
                                    if (path.equals(defaultPath)) {
                                        currentSound = defaultUri;
                                    } else {
                                        currentSound = Uri.parse(path);
                                    }
                                }
                            } else if (i == groupSoundRow) {
                                String path = preferences.getString("GroupSoundPath", defaultPath);
                                if (path != null && !path.equals("NoSound")) {
                                    if (path.equals(defaultPath)) {
                                        currentSound = defaultUri;
                                    } else {
                                        currentSound = Uri.parse(path);
                                    }
                                }
                            }
                            tmpIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentSound);
                            startActivityForResult(tmpIntent, i);
                        } catch (Exception e) {
                            FileLog.e("tmessages", e);
                        }
                    } else if (i == resetNotificationsRow) {
                        if (reseting) {
                            return;
                        }
                        reseting = true;
                        TLRPC.TL_account_resetNotifySettings req = new TLRPC.TL_account_resetNotifySettings();
                        ConnectionsManager.getInstance().performRpc(req, new RPCRequest.RPCRequestDelegate() {
                            @Override
                            public void run(TLObject response, TLRPC.TL_error error) {
                                AndroidUtilities.RunOnUIThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        MessagesController.getInstance().enableJoined = true;
                                        reseting = false;
                                        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                                        SharedPreferences.Editor editor = preferences.edit();
                                        editor.clear();
                                        editor.commit();
                                        if (listView != null) {
                                            listView.invalidateViews();
                                        }
                                        if (getParentActivity() != null) {
                                            Toast toast = Toast.makeText(getParentActivity(), LocaleController.getString("ResetNotificationsText", br.com.uatizapi.messenger.R.string.ResetNotificationsText), Toast.LENGTH_SHORT);
                                            toast.show();
                                        }
                                    }
                                });
                            }
                        });
                    } else if (i == inappSoundRow) {
                        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                        SharedPreferences.Editor editor = preferences.edit();
                        boolean enabled = preferences.getBoolean("EnableInAppSounds", true);
                        editor.putBoolean("EnableInAppSounds", !enabled);
                        editor.commit();
                        listView.invalidateViews();
                    } else if (i == inappVibrateRow) {
                        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                        SharedPreferences.Editor editor = preferences.edit();
                        boolean enabled = preferences.getBoolean("EnableInAppVibrate", true);
                        editor.putBoolean("EnableInAppVibrate", !enabled);
                        editor.commit();
                        listView.invalidateViews();
                    } else if (i == inappPreviewRow) {
                        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                        SharedPreferences.Editor editor = preferences.edit();
                        boolean enabled = preferences.getBoolean("EnableInAppPreview", true);
                        editor.putBoolean("EnableInAppPreview", !enabled);
                        editor.commit();
                        listView.invalidateViews();
                    } else if (i == contactJoinedRow) {
                        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                        SharedPreferences.Editor editor = preferences.edit();
                        boolean enabled = preferences.getBoolean("EnableContactJoined", true);
                        MessagesController.getInstance().enableJoined = !enabled;
                        editor.putBoolean("EnableContactJoined", !enabled);
                        editor.commit();
                        listView.invalidateViews();
                    } else if (i == pebbleAlertRow) {
                        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                        SharedPreferences.Editor editor = preferences.edit();
                        boolean enabled = preferences.getBoolean("EnablePebbleNotifications", false);
                        editor.putBoolean("EnablePebbleNotifications", !enabled);
                        editor.commit();
                        listView.invalidateViews();
                    } else if (i == badgeNumberRow) {
                        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                        SharedPreferences.Editor editor = preferences.edit();
                        boolean enabled = preferences.getBoolean("badgeNumber", true);
                        editor.putBoolean("badgeNumber", !enabled);
                        editor.commit();
                        listView.invalidateViews();
                        NotificationsController.getInstance().setBadgeEnabled(!enabled);
                    } else if (i == notificationsServiceRow) {
                        final SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                        boolean enabled = preferences.getBoolean("pushService", true);
                        if (!enabled) {
                            final SharedPreferences.Editor editor = preferences.edit();
                            editor.putBoolean("pushService", !enabled);
                            editor.commit();
                            listView.invalidateViews();
                            ApplicationLoader.startPushService();
                        } else {
                            if (getParentActivity() == null) {
                                return;
                            }
                            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                            builder.setMessage(LocaleController.getString("NotificationsServiceDisableInfo", br.com.uatizapi.messenger.R.string.NotificationsServiceDisableInfo));
                            builder.setTitle(LocaleController.getString("AppName", br.com.uatizapi.messenger.R.string.AppName));
                            builder.setPositiveButton(LocaleController.getString("OK", br.com.uatizapi.messenger.R.string.OK), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    ApplicationLoader.stopPushService();
                                    final SharedPreferences.Editor editor = preferences.edit();
                                    editor.putBoolean("pushService", false);
                                    editor.commit();
                                    listView.invalidateViews();
                                }
                            });
                            builder.setNegativeButton(LocaleController.getString("Cancel", br.com.uatizapi.messenger.R.string.Cancel), null);
                            showAlertDialog(builder);
                        }
                    } else if (i == messageLedRow || i == groupLedRow) {
                        if (getParentActivity() == null) {
                            return;
                        }

                        LayoutInflater li = (LayoutInflater)getParentActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                        view = li.inflate(br.com.uatizapi.messenger.R.layout.settings_color_dialog_layout, null, false);
                        final ColorPickerView colorPickerView = (ColorPickerView)view.findViewById(br.com.uatizapi.messenger.R.id.color_picker);

                        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                        if (i == messageLedRow) {
                            colorPickerView.setOldCenterColor(preferences.getInt("MessagesLed", 0xff00ff00));
                        } else if (i == groupLedRow) {
                            colorPickerView.setOldCenterColor(preferences.getInt("GroupLed", 0xff00ff00));
                        }

                        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                        builder.setTitle(LocaleController.getString("LedColor", br.com.uatizapi.messenger.R.string.LedColor));
                        builder.setView(view);
                        builder.setPositiveButton(LocaleController.getString("Set", br.com.uatizapi.messenger.R.string.Set), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int which) {
                                final SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                                SharedPreferences.Editor editor = preferences.edit();
                                if (i == messageLedRow) {
                                    editor.putInt("MessagesLed", colorPickerView.getColor());
                                } else if (i == groupLedRow) {
                                    editor.putInt("GroupLed", colorPickerView.getColor());
                                }
                                editor.commit();
                                listView.invalidateViews();
                            }
                        });
                        builder.setNeutralButton(LocaleController.getString("Disabled", br.com.uatizapi.messenger.R.string.Disabled), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                final SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                                SharedPreferences.Editor editor = preferences.edit();
                                if (i == messageLedRow) {
                                    editor.putInt("MessagesLed", 0);
                                } else if (i == groupLedRow) {
                                    editor.putInt("GroupLed", 0);
                                }
                                editor.commit();
                                listView.invalidateViews();
                            }
                        });
                        showAlertDialog(builder);
                    } else if (i == messagePopupNotificationRow || i == groupPopupNotificationRow) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                        builder.setTitle(LocaleController.getString("PopupNotification", br.com.uatizapi.messenger.R.string.PopupNotification));
                        builder.setItems(new CharSequence[] {
                                LocaleController.getString("NoPopup", br.com.uatizapi.messenger.R.string.NoPopup),
                                LocaleController.getString("OnlyWhenScreenOn", br.com.uatizapi.messenger.R.string.OnlyWhenScreenOn),
                                LocaleController.getString("OnlyWhenScreenOff", br.com.uatizapi.messenger.R.string.OnlyWhenScreenOff),
                                LocaleController.getString("AlwaysShowPopup", br.com.uatizapi.messenger.R.string.AlwaysShowPopup)
                        }, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                                SharedPreferences.Editor editor = preferences.edit();
                                if (i == messagePopupNotificationRow) {
                                    editor.putInt("popupAll", which);
                                } else if (i == groupPopupNotificationRow) {
                                    editor.putInt("popupGroup", which);
                                }
                                editor.commit();
                                if (listView != null) {
                                    listView.invalidateViews();
                                }
                            }
                        });
                        builder.setNegativeButton(LocaleController.getString("Cancel", br.com.uatizapi.messenger.R.string.Cancel), null);
                        showAlertDialog(builder);
                    } else if (i == messageVibrateRow || i == groupVibrateRow) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                        builder.setTitle(LocaleController.getString("Vibrate", br.com.uatizapi.messenger.R.string.Vibrate));
                        builder.setItems(new CharSequence[] {
                                LocaleController.getString("Disabled", br.com.uatizapi.messenger.R.string.Disabled),
                                LocaleController.getString("Default", br.com.uatizapi.messenger.R.string.Default),
                                LocaleController.getString("Short", br.com.uatizapi.messenger.R.string.Short),
                                LocaleController.getString("Long", br.com.uatizapi.messenger.R.string.Long)
                        }, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                                SharedPreferences.Editor editor = preferences.edit();
                                String param = "vibrate_messages";
                                if (i == groupVibrateRow) {
                                    param = "vibrate_group";
                                }
                                if (which == 0) {
                                    editor.putInt(param, 2);
                                } else if (which == 1) {
                                    editor.putInt(param, 0);
                                } else if (which == 2) {
                                    editor.putInt(param, 1);
                                } else if (which == 3) {
                                    editor.putInt(param, 3);
                                }
                                editor.commit();
                                if (listView != null) {
                                    listView.invalidateViews();
                                }
                            }
                        });
                        builder.setNegativeButton(LocaleController.getString("Cancel", br.com.uatizapi.messenger.R.string.Cancel), null);
                        showAlertDialog(builder);
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

    public void updateServerNotificationsSettings(boolean group) {
        //disable global settings sync
        /*SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
        TLRPC.TL_account_updateNotifySettings req = new TLRPC.TL_account_updateNotifySettings();
        req.settings = new TLRPC.TL_inputPeerNotifySettings();
        req.settings.sound = "default";
        req.settings.events_mask = 0;
        if (!group) {
            req.peer = new TLRPC.TL_inputNotifyUsers();
            req.settings.mute_until = preferences.getBoolean("EnableAll", true) ? 0 : Integer.MAX_VALUE;
            req.settings.show_previews = preferences.getBoolean("EnablePreviewAll", true);
        } else {
            req.peer = new TLRPC.TL_inputNotifyChats();
            req.settings.mute_until = preferences.getBoolean("EnableGroup", true) ? 0 : Integer.MAX_VALUE;
            req.settings.show_previews = preferences.getBoolean("EnablePreviewGroup", true);
        }
        ConnectionsManager.getInstance().performRpc(req, new RPCRequest.RPCRequestDelegate() {
            @Override
            public void run(TLObject response, TLRPC.TL_error error) {

            }
        });*/
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            Uri ringtone = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            String name = null;
            if (ringtone != null) {
                Ringtone rng = RingtoneManager.getRingtone(getParentActivity(), ringtone);
                if (rng != null) {
                    if(ringtone.equals(Settings.System.DEFAULT_NOTIFICATION_URI)) {
                        name = LocaleController.getString("Default", br.com.uatizapi.messenger.R.string.Default);
                    } else {
                        name = rng.getTitle(getParentActivity());
                    }
                    rng.stop();
                }
            }

            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();

            if (requestCode == messageSoundRow) {
                if (name != null && ringtone != null) {
                    editor.putString("GlobalSound", name);
                    editor.putString("GlobalSoundPath", ringtone.toString());
                } else {
                    editor.putString("GlobalSound", "NoSound");
                    editor.putString("GlobalSoundPath", "NoSound");
                }
            } else if (requestCode == groupSoundRow) {
                if (name != null && ringtone != null) {
                    editor.putString("GroupSound", name);
                    editor.putString("GroupSoundPath", ringtone.toString());
                } else {
                    editor.putString("GroupSound", "NoSound");
                    editor.putString("GroupSoundPath", "NoSound");
                }
            }
            editor.commit();
            listView.invalidateViews();
        }
    }

    @Override
    public void didReceivedNotification(int id, Object... args) {
        if (id == NotificationCenter.notificationsSettingsUpdated) {
            listView.invalidateViews();
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
            return !(i == messageSectionRow || i == groupSectionRow || i == inappSectionRow || i == eventsSectionRow || i == otherSectionRow || i == resetSectionRow);
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
                if (view == null) {
                    view = new SettingsSectionLayout(mContext);
                }
                if (i == messageSectionRow) {
                    ((SettingsSectionLayout) view).setText(LocaleController.getString("MessageNotifications", br.com.uatizapi.messenger.R.string.MessageNotifications));
                } else if (i == groupSectionRow) {
                    ((SettingsSectionLayout) view).setText(LocaleController.getString("GroupNotifications", br.com.uatizapi.messenger.R.string.GroupNotifications));
                } else if (i == inappSectionRow) {
                    ((SettingsSectionLayout) view).setText(LocaleController.getString("InAppNotifications", br.com.uatizapi.messenger.R.string.InAppNotifications));
                } else if (i == eventsSectionRow) {
                    ((SettingsSectionLayout) view).setText(LocaleController.getString("Events", br.com.uatizapi.messenger.R.string.Events));
                } else if (i == otherSectionRow) {
                    ((SettingsSectionLayout) view).setText(LocaleController.getString("PhoneOther", br.com.uatizapi.messenger.R.string.PhoneOther));
                } else if (i == resetSectionRow) {
                    ((SettingsSectionLayout) view).setText(LocaleController.getString("Reset", br.com.uatizapi.messenger.R.string.Reset));
                }
            } if (type == 1) {
                if (view == null) {
                    LayoutInflater li = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    view = li.inflate(br.com.uatizapi.messenger.R.layout.settings_row_check_notify_layout, viewGroup, false);
                }
                TextView textView = (TextView)view.findViewById(br.com.uatizapi.messenger.R.id.settings_row_text);
                View divider = view.findViewById(br.com.uatizapi.messenger.R.id.settings_row_divider);

                ImageView checkButton = (ImageView)view.findViewById(br.com.uatizapi.messenger.R.id.settings_row_check_button);
                SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                boolean enabled = false;
                boolean enabledAll = preferences.getBoolean("EnableAll", true);
                boolean enabledGroup = preferences.getBoolean("EnableGroup", true);

                if (i == messageAlertRow || i == groupAlertRow) {
                    if (i == messageAlertRow) {
                        enabled = enabledAll;
                    } else if (i == groupAlertRow) {
                        enabled = enabledGroup;
                    }
                    textView.setText(LocaleController.getString("Alert", br.com.uatizapi.messenger.R.string.Alert));
                    divider.setVisibility(View.VISIBLE);
                } else if (i == messagePreviewRow || i == groupPreviewRow) {
                    if (i == messagePreviewRow) {
                        enabled = preferences.getBoolean("EnablePreviewAll", true);
                    } else if (i == groupPreviewRow) {
                        enabled = preferences.getBoolean("EnablePreviewGroup", true);
                    }
                    textView.setText(LocaleController.getString("MessagePreview", br.com.uatizapi.messenger.R.string.MessagePreview));
                    divider.setVisibility(View.VISIBLE);
                } else if (i == inappSoundRow) {
                    enabled = preferences.getBoolean("EnableInAppSounds", true);
                    textView.setText(LocaleController.getString("InAppSounds", br.com.uatizapi.messenger.R.string.InAppSounds));
                    divider.setVisibility(View.VISIBLE);
                } else if (i == inappVibrateRow) {
                    enabled = preferences.getBoolean("EnableInAppVibrate", true);
                    textView.setText(LocaleController.getString("InAppVibrate", br.com.uatizapi.messenger.R.string.InAppVibrate));
                    divider.setVisibility(View.VISIBLE);
                } else if (i == inappPreviewRow) {
                    enabled = preferences.getBoolean("EnableInAppPreview", true);
                    textView.setText(LocaleController.getString("InAppPreview", br.com.uatizapi.messenger.R.string.InAppPreview));
                    divider.setVisibility(View.INVISIBLE);
                } else if (i == contactJoinedRow) {
                    enabled = preferences.getBoolean("EnableContactJoined", true);
                    textView.setText(LocaleController.getString("ContactJoined", br.com.uatizapi.messenger.R.string.ContactJoined));
                    divider.setVisibility(View.INVISIBLE);
                } else if (i == pebbleAlertRow) {
                    enabled = preferences.getBoolean("EnablePebbleNotifications", false);
                    textView.setText(LocaleController.getString("Pebble", br.com.uatizapi.messenger.R.string.Pebble));
                    divider.setVisibility(View.INVISIBLE);
                } else if (i == notificationsServiceRow) {
                    enabled = preferences.getBoolean("pushService", true);
                    textView.setText(LocaleController.getString("NotificationsService", br.com.uatizapi.messenger.R.string.NotificationsService));
                    divider.setVisibility(View.INVISIBLE);
                } else if (i == badgeNumberRow) {
                    enabled = preferences.getBoolean("badgeNumber", true);
                    textView.setText(LocaleController.getString("BadgeNumber", br.com.uatizapi.messenger.R.string.BadgeNumber));
                    divider.setVisibility(View.VISIBLE);
                }
                if (enabled) {
                    checkButton.setImageResource(br.com.uatizapi.messenger.R.drawable.btn_check_on);
                } else {
                    checkButton.setImageResource(br.com.uatizapi.messenger.R.drawable.btn_check_off);
                }
            } else if (type == 2) {
                if (view == null) {
                    LayoutInflater li = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    view = li.inflate(br.com.uatizapi.messenger.R.layout.settings_row_detail_layout, viewGroup, false);
                }
                TextView textView = (TextView)view.findViewById(br.com.uatizapi.messenger.R.id.settings_row_text);
                TextView textViewDetail = (TextView)view.findViewById(br.com.uatizapi.messenger.R.id.settings_row_text_detail);
                View divider = view.findViewById(br.com.uatizapi.messenger.R.id.settings_row_divider);
                SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                boolean enabledAll = preferences.getBoolean("EnableAll", true);
                boolean enabledGroup = preferences.getBoolean("EnableGroup", true);

                if (i == messageSoundRow || i == groupSoundRow) {
                    String name = null;
                    if (i == messageSoundRow) {
                        name = preferences.getString("GlobalSound", LocaleController.getString("Default", br.com.uatizapi.messenger.R.string.Default));
                    } else if (i == groupSoundRow) {
                        name = preferences.getString("GroupSound", LocaleController.getString("Default", br.com.uatizapi.messenger.R.string.Default));
                    }
                    if (name.equals("NoSound")) {
                        textViewDetail.setText(LocaleController.getString("NoSound", br.com.uatizapi.messenger.R.string.NoSound));
                    } else {
                        textViewDetail.setText(name);
                    }
                    textView.setText(LocaleController.getString("Sound", br.com.uatizapi.messenger.R.string.Sound));
                    divider.setVisibility(View.INVISIBLE);
                } else if (i == resetNotificationsRow) {
                    textView.setText(LocaleController.getString("ResetAllNotifications", br.com.uatizapi.messenger.R.string.ResetAllNotifications));
                    textViewDetail.setText(LocaleController.getString("UndoAllCustom", br.com.uatizapi.messenger.R.string.UndoAllCustom));
                    divider.setVisibility(View.INVISIBLE);
                } else if (i == messagePopupNotificationRow || i == groupPopupNotificationRow) {
                    textView.setText(LocaleController.getString("PopupNotification", br.com.uatizapi.messenger.R.string.PopupNotification));
                    int option = 0;
                    if (i == messagePopupNotificationRow) {
                        option = preferences.getInt("popupAll", 0);
                    } else if (i == groupPopupNotificationRow) {
                        option = preferences.getInt("popupGroup", 0);
                    }
                    if (option == 0) {
                        textViewDetail.setText(LocaleController.getString("NoPopup", br.com.uatizapi.messenger.R.string.NoPopup));
                    } else if (option == 1) {
                        textViewDetail.setText(LocaleController.getString("OnlyWhenScreenOn", br.com.uatizapi.messenger.R.string.OnlyWhenScreenOn));
                    } else if (option == 2) {
                        textViewDetail.setText(LocaleController.getString("OnlyWhenScreenOff", br.com.uatizapi.messenger.R.string.OnlyWhenScreenOff));
                    } else if (option == 3) {
                        textViewDetail.setText(LocaleController.getString("AlwaysShowPopup", br.com.uatizapi.messenger.R.string.AlwaysShowPopup));
                    }
                    divider.setVisibility(View.VISIBLE);
                }
            } else if (type == 3) {
                if (view == null) {
                    LayoutInflater li = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    view = li.inflate(br.com.uatizapi.messenger.R.layout.settings_row_color_layout, viewGroup, false);
                }
                TextView textView = (TextView)view.findViewById(br.com.uatizapi.messenger.R.id.settings_row_text);
                View colorView = view.findViewById(br.com.uatizapi.messenger.R.id.settings_color);
                View divider = view.findViewById(br.com.uatizapi.messenger.R.id.settings_row_divider);
                textView.setText(LocaleController.getString("LedColor", br.com.uatizapi.messenger.R.string.LedColor));
                SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                if (i == messageLedRow) {
                    colorView.setBackgroundColor(preferences.getInt("MessagesLed", 0xff00ff00));
                } else if (i == groupLedRow) {
                    colorView.setBackgroundColor(preferences.getInt("GroupLed", 0xff00ff00));
                }
                divider.setVisibility(View.VISIBLE);
            } else if (type == 4) {
                if (view == null) {
                    LayoutInflater li = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    view = li.inflate(br.com.uatizapi.messenger.R.layout.user_profile_leftright_row_layout, viewGroup, false);
                }
                TextView textView = (TextView)view.findViewById(br.com.uatizapi.messenger.R.id.settings_row_text);
                TextView detailTextView = (TextView)view.findViewById(br.com.uatizapi.messenger.R.id.settings_row_text_detail);

                View divider = view.findViewById(br.com.uatizapi.messenger.R.id.settings_row_divider);
                SharedPreferences preferences = mContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                int value = 0;
                textView.setText(LocaleController.getString("Vibrate", br.com.uatizapi.messenger.R.string.Vibrate));
                divider.setVisibility(View.VISIBLE);
                if (i == messageVibrateRow) {
                    value = preferences.getInt("vibrate_messages", 0);
                } else if (i == groupVibrateRow) {
                    value = preferences.getInt("vibrate_group", 0);
                }
                if (value == 0) {
                    detailTextView.setText(LocaleController.getString("Default", br.com.uatizapi.messenger.R.string.Default));
                } else if (value == 1) {
                    detailTextView.setText(LocaleController.getString("Short", br.com.uatizapi.messenger.R.string.Short));
                } else if (value == 2) {
                    detailTextView.setText(LocaleController.getString("Disabled", br.com.uatizapi.messenger.R.string.Disabled));
                } else if (value == 3) {
                    detailTextView.setText(LocaleController.getString("Long", br.com.uatizapi.messenger.R.string.Long));
                }
            }
            return view;
        }

        @Override
        public int getItemViewType(int i) {
            if (i == messageSectionRow || i == groupSectionRow || i == inappSectionRow || i == eventsSectionRow || i == otherSectionRow || i == resetSectionRow) {
                return 0;
            } else if (i == messageAlertRow || i == messagePreviewRow ||
                    i == groupAlertRow || i == groupPreviewRow ||
                    i == inappSoundRow || i == inappVibrateRow || i == inappPreviewRow ||
                    i == contactJoinedRow ||
                    i == pebbleAlertRow || i == notificationsServiceRow || i == badgeNumberRow) {
                return 1;
            } else if (i == messageLedRow || i == groupLedRow) {
                return 3;
            } else if (i == groupVibrateRow || i == messageVibrateRow) {
                return 4;
            } else {
                return 2;
            }
        }

        @Override
        public int getViewTypeCount() {
            return 5;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }
    }
}
