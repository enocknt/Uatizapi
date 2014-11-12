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
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import br.com.uatizapi.android.AndroidUtilities;
import br.com.uatizapi.android.ContactsController;
import br.com.uatizapi.PhoneFormat.PhoneFormat;
import br.com.uatizapi.android.MediaController;
import br.com.uatizapi.messenger.BuildVars;
import br.com.uatizapi.android.LocaleController;
import br.com.uatizapi.messenger.FileLoader;
import br.com.uatizapi.messenger.R;
import br.com.uatizapi.messenger.SerializedData;
import br.com.uatizapi.messenger.TLClassStore;
import br.com.uatizapi.messenger.TLObject;
import br.com.uatizapi.messenger.TLRPC;
import br.com.uatizapi.messenger.ConnectionsManager;
import br.com.uatizapi.messenger.FileLog;
import br.com.uatizapi.android.MessagesController;
import br.com.uatizapi.android.MessagesStorage;
import br.com.uatizapi.android.NotificationCenter;
import br.com.uatizapi.messenger.RPCRequest;
import br.com.uatizapi.messenger.UserConfig;
import br.com.uatizapi.android.MessageObject;
import br.com.uatizapi.ui.Adapters.BaseFragmentAdapter;
import br.com.uatizapi.ui.Views.ActionBar.ActionBarLayer;
import br.com.uatizapi.ui.Views.AvatarUpdater;
import br.com.uatizapi.ui.Views.BackupImageView;
import br.com.uatizapi.ui.Views.ActionBar.BaseFragment;
import br.com.uatizapi.ui.Views.NumberPicker;
import br.com.uatizapi.ui.Views.SettingsSectionLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

public class SettingsActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate, PhotoViewer.PhotoViewerProvider {
    private ListView listView;
    private ListAdapter listAdapter;
    private AvatarUpdater avatarUpdater = new AvatarUpdater();

    private int profileRow;
    private int numberSectionRow;
    private int numberRow;
    private int usernameRow;
    private int settingsSectionRow;
    private int textSizeRow;
    private int enableAnimationsRow;
    private int notificationRow;
    private int blockedRow;
    private int backgroundRow;
    private int supportSectionRow;
    private int askQuestionRow;
    private int logoutRow;
    private int sendLogsRow;
    private int clearLogsRow;
    private int switchBackendButtonRow;
    private int messagesSectionRow;
    private int sendByEnterRow;
    private int terminateSessionsRow;
    private int mediaDownloadSection;
    private int mobileDownloadRow;
    private int wifiDownloadRow;
    private int roamingDownloadRow;
    private int saveToGalleryRow;
    private int uatizapiFaqRow;
    private int languageRow;
    private int versionRow;
    private int contactsSectionRow;
    private int contactsReimportRow;
    private int contactsSortRow;
    private int rowCount;

    private static class LinkMovementMethodMy extends LinkMovementMethod {
        @Override
        public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
            try {
                return super.onTouchEvent(widget, buffer, event);
            } catch (Exception e) {
                FileLog.e("tmessages", e);
            }
            return false;
        }
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        avatarUpdater.parentFragment = this;
        avatarUpdater.delegate = new AvatarUpdater.AvatarUpdaterDelegate() {
            @Override
            public void didUploadedPhoto(TLRPC.InputFile file, TLRPC.PhotoSize small, TLRPC.PhotoSize big) {
                TLRPC.TL_photos_uploadProfilePhoto req = new TLRPC.TL_photos_uploadProfilePhoto();
                req.caption = "";
                req.crop = new TLRPC.TL_inputPhotoCropAuto();
                req.file = file;
                req.geo_point = new TLRPC.TL_inputGeoPointEmpty();
                ConnectionsManager.getInstance().performRpc(req, new RPCRequest.RPCRequestDelegate() {
                    @Override
                    public void run(TLObject response, TLRPC.TL_error error) {
                        if (error == null) {
                            TLRPC.User user = MessagesController.getInstance().getUser(UserConfig.getClientUserId());
                            if (user == null) {
                                user = UserConfig.getCurrentUser();
                                if (user == null) {
                                    return;
                                }
                                MessagesController.getInstance().putUser(user, false);
                            } else {
                                UserConfig.setCurrentUser(user);
                            }
                            if (user == null) {
                                return;
                            }
                            TLRPC.TL_photos_photo photo = (TLRPC.TL_photos_photo)response;
                            ArrayList<TLRPC.PhotoSize> sizes = photo.photo.sizes;
                            TLRPC.PhotoSize smallSize = FileLoader.getClosestPhotoSizeWithSize(sizes, 100);
                            TLRPC.PhotoSize bigSize = FileLoader.getClosestPhotoSizeWithSize(sizes, 1000);
                            user.photo = new TLRPC.TL_userProfilePhoto();
                            user.photo.photo_id = photo.photo.id;
                            if (smallSize != null) {
                                user.photo.photo_small = smallSize.location;
                            }
                            if (bigSize != null) {
                                user.photo.photo_big = bigSize.location;
                            } else if (smallSize != null) {
                                user.photo.photo_small = smallSize.location;
                            }
                            MessagesStorage.getInstance().clearUserPhotos(user.id);
                            ArrayList<TLRPC.User> users = new ArrayList<TLRPC.User>();
                            users.add(user);
                            MessagesStorage.getInstance().putUsersAndChats(users, null, false, true);
                            AndroidUtilities.RunOnUIThread(new Runnable() {
                                @Override
                                public void run() {
                                    NotificationCenter.getInstance().postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
                                    UserConfig.saveConfig(true);
                                }
                            });
                        }
                    }
                });
            }
        };
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.updateInterfaces);


        rowCount = 0;
        profileRow = rowCount++;
        numberSectionRow = rowCount++;
        numberRow = rowCount++;
        usernameRow = rowCount++;
        settingsSectionRow = rowCount++;
        enableAnimationsRow = rowCount++;
        languageRow = rowCount++;
        notificationRow = rowCount++;
        blockedRow = rowCount++;
        backgroundRow = rowCount++;
        terminateSessionsRow = rowCount++;
        mediaDownloadSection = rowCount++;
        mobileDownloadRow = rowCount++;
        wifiDownloadRow = rowCount++;
        roamingDownloadRow = rowCount++;
        saveToGalleryRow = rowCount++;
        messagesSectionRow = rowCount++;
        textSizeRow = rowCount++;
        sendByEnterRow = rowCount++;
        //contactsSectionRow = rowCount++;
        //contactsSortRow = rowCount++;
        //contactsReimportRow = rowCount++;
        supportSectionRow = rowCount++;
        if (BuildVars.DEBUG_VERSION) {
            sendLogsRow = rowCount++;
            clearLogsRow = rowCount++;
            switchBackendButtonRow = rowCount++;
        }
        uatizapiFaqRow = rowCount++;
        askQuestionRow = rowCount++;
        logoutRow = rowCount++;
        versionRow = rowCount++;

        MessagesController.getInstance().loadFullUser(UserConfig.getCurrentUser(), classGuid);

        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        MessagesController.getInstance().cancelLoadFullUser(UserConfig.getClientUserId());
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.updateInterfaces);
        avatarUpdater.clear();
    }

    @Override
    public View createView(LayoutInflater inflater, ViewGroup container) {
        if (fragmentView == null) {
            actionBarLayer.setDisplayHomeAsUpEnabled(true, br.com.uatizapi.messenger.R.drawable.ic_ab_back);
            actionBarLayer.setBackOverlay(br.com.uatizapi.messenger.R.layout.updating_state_layout);
            actionBarLayer.setTitle(LocaleController.getString("Settings", br.com.uatizapi.messenger.R.string.Settings));
            actionBarLayer.setActionBarMenuOnItemClick(new ActionBarLayer.ActionBarMenuOnItemClick() {
                @Override
                public void onItemClick(int id) {
                    if (id == -1) {
                        finishFragment();
                    }
                }
            });

            fragmentView = inflater.inflate(br.com.uatizapi.messenger.R.layout.settings_layout, container, false);
            listAdapter = new ListAdapter(getParentActivity());
            listView = (ListView)fragmentView.findViewById(br.com.uatizapi.messenger.R.id.listView);
            listView.setAdapter(listAdapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, final int i, long l) {
                    if (i == textSizeRow) {
                        if (getParentActivity() == null) {
                            return;
                        }
                        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                        builder.setTitle(LocaleController.getString("TextSize", br.com.uatizapi.messenger.R.string.TextSize));
                        final NumberPicker numberPicker = new NumberPicker(getParentActivity());
                        numberPicker.setMinValue(12);
                        numberPicker.setMaxValue(30);
                        numberPicker.setValue(MessagesController.getInstance().fontSize);
                        builder.setView(numberPicker);
                        builder.setNegativeButton(LocaleController.getString("Done", br.com.uatizapi.messenger.R.string.Done), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                                SharedPreferences.Editor editor = preferences.edit();
                                editor.putInt("fons_size", numberPicker.getValue());
                                MessagesController.getInstance().fontSize = numberPicker.getValue();
                                editor.commit();
                                if (listView != null) {
                                    listView.invalidateViews();
                                }
                            }
                        });
                        showAlertDialog(builder);
                    } else if (i == enableAnimationsRow) {
                        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                        boolean animations = preferences.getBoolean("view_animations", true);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putBoolean("view_animations", !animations);
                        editor.commit();
                        if (listView != null) {
                            listView.invalidateViews();
                        }
                    } else if (i == notificationRow) {
                        presentFragment(new SettingsNotificationsActivity());
                    } else if (i == blockedRow) {
                        presentFragment(new SettingsBlockedUsersActivity());
                    } else if (i == backgroundRow) {
                        presentFragment(new SettingsWallpapersActivity());
                    } else if (i == askQuestionRow) {
                        if (getParentActivity() == null) {
                            return;
                        }
                        final TextView message = new TextView(getParentActivity());
                        message.setText(Html.fromHtml(LocaleController.getString("AskAQuestionInfo", br.com.uatizapi.messenger.R.string.AskAQuestionInfo)));
                        message.setTextSize(18);
                        message.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(5), AndroidUtilities.dp(8), AndroidUtilities.dp(6));
                        message.setMovementMethod(new LinkMovementMethodMy());

                        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                        builder.setView(message);
                        builder.setPositiveButton(LocaleController.getString("AskButton", br.com.uatizapi.messenger.R.string.AskButton), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                performAskAQuestion();
                            }
                        });
                        builder.setNegativeButton(LocaleController.getString("Cancel", br.com.uatizapi.messenger.R.string.Cancel), null);
                        showAlertDialog(builder);
                    } else if (i == sendLogsRow) {
                        sendLogs();
                    } else if (i == clearLogsRow) {
                        FileLog.cleanupLogs();
                    } else if (i == sendByEnterRow) {
                        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                        boolean send = preferences.getBoolean("send_by_enter", false);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putBoolean("send_by_enter", !send);
                        editor.commit();
                        if (listView != null) {
                            listView.invalidateViews();
                        }
                    } else if (i == saveToGalleryRow) {
                        MediaController.getInstance().toggleSaveToGallery();
                        if (listView != null) {
                            listView.invalidateViews();
                        }
                    } else if (i == terminateSessionsRow) {
                        if (getParentActivity() == null) {
                            return;
                        }
                        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                        builder.setMessage(LocaleController.getString("AreYouSureSessions", br.com.uatizapi.messenger.R.string.AreYouSureSessions));
                        builder.setTitle(LocaleController.getString("AppName", br.com.uatizapi.messenger.R.string.AppName));
                        builder.setPositiveButton(LocaleController.getString("OK", br.com.uatizapi.messenger.R.string.OK), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                TLRPC.TL_auth_resetAuthorizations req = new TLRPC.TL_auth_resetAuthorizations();
                                ConnectionsManager.getInstance().performRpc(req, new RPCRequest.RPCRequestDelegate() {
                                    @Override
                                    public void run(final TLObject response, final TLRPC.TL_error error) {
                                        AndroidUtilities.RunOnUIThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (getParentActivity() == null) {
                                                    return;
                                                }
                                                if (error == null && response instanceof TLRPC.TL_boolTrue) {
                                                    Toast toast = Toast.makeText(getParentActivity(), LocaleController.getString("TerminateAllSessions", br.com.uatizapi.messenger.R.string.TerminateAllSessions), Toast.LENGTH_SHORT);
                                                    toast.show();
                                                } else {
                                                    Toast toast = Toast.makeText(getParentActivity(), LocaleController.getString("UnknownError", br.com.uatizapi.messenger.R.string.UnknownError), Toast.LENGTH_SHORT);
                                                    toast.show();
                                                }
                                            }
                                        });
                                        UserConfig.registeredForPush = false;
                                        UserConfig.registeredForInternalPush = false;
                                        UserConfig.saveConfig(false);
                                        MessagesController.getInstance().registerForPush(UserConfig.pushString);
                                        ConnectionsManager.getInstance().initPushConnection();
                                    }
                                });
                            }
                        });
                        builder.setNegativeButton(LocaleController.getString("Cancel", br.com.uatizapi.messenger.R.string.Cancel), null);
                        showAlertDialog(builder);
                    } else if (i == languageRow) {
                        presentFragment(new LanguageSelectActivity());
                    } else if (i == switchBackendButtonRow) {
                        if (getParentActivity() == null) {
                            return;
                        }
                        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                        builder.setMessage(LocaleController.getString("AreYouSure", br.com.uatizapi.messenger.R.string.AreYouSure));
                        builder.setTitle(LocaleController.getString("AppName", br.com.uatizapi.messenger.R.string.AppName));
                        builder.setPositiveButton(LocaleController.getString("OK", br.com.uatizapi.messenger.R.string.OK), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ConnectionsManager.getInstance().switchBackend();
                            }
                        });
                        builder.setNegativeButton(LocaleController.getString("Cancel", br.com.uatizapi.messenger.R.string.Cancel), null);
                        showAlertDialog(builder);
                    } else if (i == uatizapiFaqRow) {
                        try {
                            Intent pickIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(LocaleController.getString("UatizapiFaqUrl", R.string.UatizapiFaqUrl)));
                            getParentActivity().startActivity(pickIntent);
                        } catch (Exception e) {
                            FileLog.e("tmessages", e);
                        }
                    } else if (i == contactsReimportRow) {

                    } else if (i == contactsSortRow) {
                        if (getParentActivity() == null) {
                            return;
                        }
                        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                        builder.setTitle(LocaleController.getString("SortBy", br.com.uatizapi.messenger.R.string.SortBy));
                        builder.setItems(new CharSequence[] {
                                LocaleController.getString("Default", br.com.uatizapi.messenger.R.string.Default),
                                LocaleController.getString("SortFirstName", br.com.uatizapi.messenger.R.string.SortFirstName),
                                LocaleController.getString("SortLastName", br.com.uatizapi.messenger.R.string.SortLastName)
                        }, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                                SharedPreferences.Editor editor = preferences.edit();
                                editor.putInt("sortContactsBy", which);
                                editor.commit();
                                if (listView != null) {
                                    listView.invalidateViews();
                                }
                            }
                        });
                        builder.setNegativeButton(LocaleController.getString("Cancel", br.com.uatizapi.messenger.R.string.Cancel), null);
                        showAlertDialog(builder);
                    } else if (i == wifiDownloadRow || i == mobileDownloadRow || i == roamingDownloadRow) {
                        if (getParentActivity() == null) {
                            return;
                        }
                        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());

                        int mask = 0;
                        if (i == mobileDownloadRow) {
                            builder.setTitle(LocaleController.getString("WhenUsingMobileData", br.com.uatizapi.messenger.R.string.WhenUsingMobileData));
                            mask = MediaController.getInstance().mobileDataDownloadMask;
                        } else if (i == wifiDownloadRow) {
                            builder.setTitle(LocaleController.getString("WhenConnectedOnWiFi", br.com.uatizapi.messenger.R.string.WhenConnectedOnWiFi));
                            mask = MediaController.getInstance().wifiDownloadMask;
                        } else if (i == roamingDownloadRow) {
                            builder.setTitle(LocaleController.getString("WhenRoaming", br.com.uatizapi.messenger.R.string.WhenRoaming));
                            mask = MediaController.getInstance().roamingDownloadMask;
                        }
                        builder.setMultiChoiceItems(
                                new CharSequence[]{LocaleController.getString("AttachPhoto", br.com.uatizapi.messenger.R.string.AttachPhoto), LocaleController.getString("AttachAudio", br.com.uatizapi.messenger.R.string.AttachAudio), LocaleController.getString("AttachAudio", br.com.uatizapi.messenger.R.string.AttachVideo), LocaleController.getString("AttachAudio", br.com.uatizapi.messenger.R.string.AttachDocument)},
                                new boolean[]{(mask & MediaController.AUTODOWNLOAD_MASK_PHOTO) != 0, (mask & MediaController.AUTODOWNLOAD_MASK_AUDIO) != 0, (mask & MediaController.AUTODOWNLOAD_MASK_VIDEO) != 0, (mask & MediaController.AUTODOWNLOAD_MASK_DOCUMENT) != 0},
                                new DialogInterface.OnMultiChoiceClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                        int mask = 0;
                                        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                                        SharedPreferences.Editor editor = preferences.edit();
                                        if (i == mobileDownloadRow) {
                                            mask = MediaController.getInstance().mobileDataDownloadMask;
                                        } else if (i == wifiDownloadRow) {
                                            mask = MediaController.getInstance().wifiDownloadMask;
                                        } else if (i == roamingDownloadRow) {
                                            mask = MediaController.getInstance().roamingDownloadMask;
                                        }

                                        int maskDiff = 0;
                                        if (which == 0) {
                                            maskDiff = MediaController.AUTODOWNLOAD_MASK_PHOTO;
                                        } else if (which == 1) {
                                            maskDiff = MediaController.AUTODOWNLOAD_MASK_AUDIO;
                                        } else if (which == 2) {
                                            maskDiff = MediaController.AUTODOWNLOAD_MASK_VIDEO;
                                        } else if (which == 3) {
                                            maskDiff = MediaController.AUTODOWNLOAD_MASK_DOCUMENT;
                                        }

                                        if (isChecked) {
                                            mask |= maskDiff;
                                        } else {
                                            mask &= ~maskDiff;
                                        }

                                        if (i == mobileDownloadRow) {
                                            editor.putInt("mobileDataDownloadMask", mask);
                                            mask = MediaController.getInstance().mobileDataDownloadMask = mask;
                                        } else if (i == wifiDownloadRow) {
                                            editor.putInt("wifiDownloadMask", mask);
                                            MediaController.getInstance().wifiDownloadMask = mask;
                                        } else if (i == roamingDownloadRow) {
                                            editor.putInt("roamingDownloadMask", mask);
                                            MediaController.getInstance().roamingDownloadMask = mask;
                                        }
                                        editor.commit();
                                        if (listView != null) {
                                            listView.invalidateViews();
                                        }
                                    }
                                });
                        builder.setNegativeButton(LocaleController.getString("OK", br.com.uatizapi.messenger.R.string.OK), null);
                        showAlertDialog(builder);
                    } else if (i == usernameRow) {
                        presentFragment(new SettingsChangeUsernameActivity());
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
    protected void onDialogDismiss() {
        MediaController.getInstance().checkAutodownloadSettings();
    }

    @Override
    public PhotoViewer.PlaceProviderObject getPlaceForPhoto(MessageObject messageObject, TLRPC.FileLocation fileLocation, int index) {
        if (fileLocation == null) {
            return null;
        }
        TLRPC.User user = MessagesController.getInstance().getUser(UserConfig.getClientUserId());
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
                        object.user_id = UserConfig.getClientUserId();
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

    public void performAskAQuestion() {
        final SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
        int uid = preferences.getInt("support_id", 0);
        TLRPC.User supportUser = null;
        if (uid != 0) {
            supportUser = MessagesController.getInstance().getUser(uid);
            if (supportUser == null) {
                String userString = preferences.getString("support_user", null);
                if (userString != null) {
                    try {
                        byte[] datacentersBytes = Base64.decode(userString, Base64.DEFAULT);
                        if (datacentersBytes != null) {
                            SerializedData data = new SerializedData(datacentersBytes);
                            supportUser = (TLRPC.User)TLClassStore.Instance().TLdeserialize(data, data.readInt32());
                            if (supportUser != null && supportUser.id == 333000) {
                                supportUser = null;
                            }
                        }
                    } catch (Exception e) {
                        FileLog.e("tmessages", e);
                        supportUser = null;
                    }
                }
            }
        }
        if (supportUser == null) {
            final ProgressDialog progressDialog = new ProgressDialog(getParentActivity());
            progressDialog.setMessage(LocaleController.getString("Loading", br.com.uatizapi.messenger.R.string.Loading));
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.setCancelable(false);
            progressDialog.show();
            TLRPC.TL_help_getSupport req = new TLRPC.TL_help_getSupport();
            ConnectionsManager.getInstance().performRpc(req, new RPCRequest.RPCRequestDelegate() {
                @Override
                public void run(TLObject response, TLRPC.TL_error error) {
                    if (error == null) {

                        final TLRPC.TL_help_support res = (TLRPC.TL_help_support)response;
                        AndroidUtilities.RunOnUIThread(new Runnable() {
                            @Override
                            public void run() {
                                SharedPreferences.Editor editor = preferences.edit();
                                editor.putInt("support_id", res.user.id);
                                SerializedData data = new SerializedData();
                                res.user.serializeToStream(data);
                                editor.putString("support_user", Base64.encodeToString(data.toByteArray(), Base64.DEFAULT));
                                editor.commit();
                                try {
                                    progressDialog.dismiss();
                                } catch (Exception e) {
                                    FileLog.e("tmessages", e);
                                }
                                ArrayList<TLRPC.User> users = new ArrayList<TLRPC.User>();
                                users.add(res.user);
                                MessagesStorage.getInstance().putUsersAndChats(users, null, true, true);
                                MessagesController.getInstance().putUser(res.user, false);
                                Bundle args = new Bundle();
                                args.putInt("user_id", res.user.id);
                                presentFragment(new ChatActivity(args));
                            }
                        });
                    } else {
                        AndroidUtilities.RunOnUIThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    progressDialog.dismiss();
                                } catch (Exception e) {
                                    FileLog.e("tmessages", e);
                                }
                            }
                        });
                    }
                }
            });
        } else {
            MessagesController.getInstance().putUser(supportUser, true);
            Bundle args = new Bundle();
            args.putInt("user_id", supportUser.id);
            presentFragment(new ChatActivity(args));
        }
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        avatarUpdater.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void saveSelfArgs(Bundle args) {
        if (avatarUpdater != null && avatarUpdater.currentPicturePath != null) {
            args.putString("path", avatarUpdater.currentPicturePath);
        }
    }

    @Override
    public void restoreSelfArgs(Bundle args) {
        if (avatarUpdater != null) {
            avatarUpdater.currentPicturePath = args.getString("path");
        }
    }

    @Override
    public void didReceivedNotification(int id, Object... args) {
        if (id == NotificationCenter.updateInterfaces) {
            int mask = (Integer)args[0];
            if ((mask & MessagesController.UPDATE_MASK_AVATAR) != 0 || (mask & MessagesController.UPDATE_MASK_NAME) != 0) {
                if (listView != null) {
                    listView.invalidateViews();
                }
            }
        }
    }

    private void sendLogs() {
        try {
            ArrayList<Uri> uris = new ArrayList<Uri>();
            File sdCard = ApplicationLoader.applicationContext.getExternalFilesDir(null);
            File dir = new File (sdCard.getAbsolutePath() + "/logs");
            File[] files = dir.listFiles();
            for (File file : files) {
                uris.add(Uri.fromFile(file));
            }

            if (uris.isEmpty()) {
                return;
            }
            Intent i = new Intent(Intent.ACTION_SEND_MULTIPLE);
            i.setType("message/rfc822") ;
            i.putExtra(Intent.EXTRA_EMAIL, new String[]{BuildVars.SEND_LOGS_EMAIL});
            i.putExtra(Intent.EXTRA_SUBJECT, "last logs");
            i.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            getParentActivity().startActivity(Intent.createChooser(i, "Select email application."));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
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
            return i == textSizeRow || i == enableAnimationsRow || i == blockedRow || i == notificationRow || i == backgroundRow ||
                    i == askQuestionRow || i == sendLogsRow || i == sendByEnterRow || i == terminateSessionsRow || i == wifiDownloadRow ||
                    i == mobileDownloadRow || i == clearLogsRow || i == roamingDownloadRow || i == languageRow || i == usernameRow ||
                    i == switchBackendButtonRow || i == uatizapiFaqRow || i == contactsSortRow || i == contactsReimportRow || i == saveToGalleryRow;
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
                    LayoutInflater li = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    view = li.inflate(br.com.uatizapi.messenger.R.layout.settings_name_layout, viewGroup, false);

                    ImageButton button = (ImageButton)view.findViewById(br.com.uatizapi.messenger.R.id.settings_edit_name);
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            presentFragment(new SettingsChangeNameActivity());
                        }
                    });

                    final ImageButton button2 = (ImageButton)view.findViewById(br.com.uatizapi.messenger.R.id.settings_change_avatar_button);
                    button2.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (getParentActivity() == null) {
                                return;
                            }
                            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());

                            CharSequence[] items;

                            TLRPC.User user = MessagesController.getInstance().getUser(UserConfig.getClientUserId());
                            if (user == null) {
                                user = UserConfig.getCurrentUser();
                            }
                            if (user == null) {
                                return;
                            }
                            boolean fullMenu = false;
                            if (user.photo != null && user.photo.photo_big != null && !(user.photo instanceof TLRPC.TL_userProfilePhotoEmpty)) {
                                items = new CharSequence[] {LocaleController.getString("OpenPhoto", br.com.uatizapi.messenger.R.string.OpenPhoto), LocaleController.getString("FromCamera", br.com.uatizapi.messenger.R.string.FromCamera), LocaleController.getString("FromGalley", br.com.uatizapi.messenger.R.string.FromGalley), LocaleController.getString("DeletePhoto", br.com.uatizapi.messenger.R.string.DeletePhoto)};
                                fullMenu = true;
                            } else {
                                items = new CharSequence[] {LocaleController.getString("FromCamera", br.com.uatizapi.messenger.R.string.FromCamera), LocaleController.getString("FromGalley", br.com.uatizapi.messenger.R.string.FromGalley)};
                            }

                            final boolean full = fullMenu;
                            builder.setItems(items, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    if (i == 0 && full) {
                                        TLRPC.User user = MessagesController.getInstance().getUser(UserConfig.getClientUserId());
                                        if (user != null && user.photo != null && user.photo.photo_big != null) {
                                            PhotoViewer.getInstance().setParentActivity(getParentActivity());
                                            PhotoViewer.getInstance().openPhoto(user.photo.photo_big, SettingsActivity.this);
                                        }
                                    } else if (i == 0 && !full || i == 1 && full) {
                                        avatarUpdater.openCamera();
                                    } else if (i == 1 && !full || i == 2 && full) {
                                        avatarUpdater.openGallery();
                                    } else if (i == 3) {
                                        MessagesController.getInstance().deleteUserPhoto(null);
                                    }
                                }
                            });
                            showAlertDialog(builder);
                        }
                    });
                }
                TextView textView = (TextView)view.findViewById(br.com.uatizapi.messenger.R.id.settings_online);
                textView.setText(LocaleController.getString("Online", br.com.uatizapi.messenger.R.string.Online));

                textView = (TextView)view.findViewById(br.com.uatizapi.messenger.R.id.settings_name);
                Typeface typeface = AndroidUtilities.getTypeface("fonts/rmedium.ttf");
                textView.setTypeface(typeface);
                TLRPC.User user = MessagesController.getInstance().getUser(UserConfig.getClientUserId());
                if (user == null) {
                    user = UserConfig.getCurrentUser();
                }
                if (user != null) {
                    textView.setText(ContactsController.formatName(user.first_name, user.last_name));
                    BackupImageView avatarImage = (BackupImageView)view.findViewById(br.com.uatizapi.messenger.R.id.settings_avatar_image);
                    avatarImage.processDetach = false;
                    TLRPC.FileLocation photo = null;
                    TLRPC.FileLocation photoBig = null;
                    if (user.photo != null) {
                        photo = user.photo.photo_small;
                        photoBig = user.photo.photo_big;
                    }
                    avatarImage.setImage(photo, "50_50", AndroidUtilities.getUserAvatarForId(user.id));
                    avatarImage.imageReceiver.setVisible(!PhotoViewer.getInstance().isShowingImage(photoBig), false);
                }
                return view;
            } else if (type == 1) {
                if (view == null) {
                    view = new SettingsSectionLayout(mContext);
                }
                if (i == numberSectionRow) {
                    ((SettingsSectionLayout) view).setText(LocaleController.getString("Info", br.com.uatizapi.messenger.R.string.Info));
                } else if (i == settingsSectionRow) {
                    ((SettingsSectionLayout) view).setText(LocaleController.getString("SETTINGS", br.com.uatizapi.messenger.R.string.SETTINGS));
                } else if (i == supportSectionRow) {
                    ((SettingsSectionLayout) view).setText(LocaleController.getString("Support", br.com.uatizapi.messenger.R.string.Support));
                } else if (i == messagesSectionRow) {
                    ((SettingsSectionLayout) view).setText(LocaleController.getString("MessagesSettings", br.com.uatizapi.messenger.R.string.MessagesSettings));
                } else if (i == mediaDownloadSection) {
                    ((SettingsSectionLayout) view).setText(LocaleController.getString("AutomaticMediaDownload", br.com.uatizapi.messenger.R.string.AutomaticMediaDownload));
                } else if (i == contactsSectionRow) {
                    ((SettingsSectionLayout) view).setText(LocaleController.getString("Contacts", br.com.uatizapi.messenger.R.string.Contacts).toUpperCase());
                }
            } else if (type == 2) {
                if (view == null) {
                    LayoutInflater li = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    view = li.inflate(br.com.uatizapi.messenger.R.layout.settings_row_button_layout, viewGroup, false);
                }
                TextView textView = (TextView)view.findViewById(br.com.uatizapi.messenger.R.id.settings_row_text);
                View divider = view.findViewById(br.com.uatizapi.messenger.R.id.settings_row_divider);
                if (i == notificationRow) {
                    textView.setText(LocaleController.getString("NotificationsAndSounds", br.com.uatizapi.messenger.R.string.NotificationsAndSounds));
                    divider.setVisibility(View.VISIBLE);
                } else if (i == blockedRow) {
                    textView.setText(LocaleController.getString("BlockedUsers", br.com.uatizapi.messenger.R.string.BlockedUsers));
                    divider.setVisibility(backgroundRow != 0 ? View.VISIBLE : View.INVISIBLE);
                } else if (i == backgroundRow) {
                    textView.setText(LocaleController.getString("ChatBackground", br.com.uatizapi.messenger.R.string.ChatBackground));
                    divider.setVisibility(View.VISIBLE);
                } else if (i == sendLogsRow) {
                    textView.setText("Send Logs");
                    divider.setVisibility(View.VISIBLE);
                } else if (i == clearLogsRow) {
                    textView.setText("Clear Logs");
                    divider.setVisibility(View.VISIBLE);
                } else if (i == askQuestionRow) {
                    textView.setText(LocaleController.getString("AskAQuestion", br.com.uatizapi.messenger.R.string.AskAQuestion));
                    divider.setVisibility(View.INVISIBLE);
                } else if (i == terminateSessionsRow) {
                    textView.setText(LocaleController.getString("TerminateAllSessions", br.com.uatizapi.messenger.R.string.TerminateAllSessions));
                    divider.setVisibility(View.INVISIBLE);
                } else if (i == switchBackendButtonRow) {
                    textView.setText("Switch Backend");
                    divider.setVisibility(View.VISIBLE);
                } else if (i == uatizapiFaqRow) {
                    textView.setText(LocaleController.getString("UatizapiFAQ", br.com.uatizapi.messenger.R.string.UatizapiFaq));
                    divider.setVisibility(View.VISIBLE);
                } else if (i == contactsReimportRow) {
                    textView.setText(LocaleController.getString("ImportContacts", br.com.uatizapi.messenger.R.string.ImportContacts));
                    divider.setVisibility(View.INVISIBLE);
                }
            } else if (type == 3) {
                if (view == null) {
                    LayoutInflater li = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    view = li.inflate(br.com.uatizapi.messenger.R.layout.settings_row_check_layout, viewGroup, false);
                }
                TextView textView = (TextView)view.findViewById(br.com.uatizapi.messenger.R.id.settings_row_text);
                View divider = view.findViewById(br.com.uatizapi.messenger.R.id.settings_row_divider);
                ImageView checkButton = (ImageView)view.findViewById(br.com.uatizapi.messenger.R.id.settings_row_check_button);
                SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                if (i == enableAnimationsRow) {
                    textView.setText(LocaleController.getString("EnableAnimations", br.com.uatizapi.messenger.R.string.EnableAnimations));
                    divider.setVisibility(View.VISIBLE);
                    boolean enabled = preferences.getBoolean("view_animations", true);
                    if (enabled) {
                        checkButton.setImageResource(br.com.uatizapi.messenger.R.drawable.btn_check_on);
                    } else {
                        checkButton.setImageResource(br.com.uatizapi.messenger.R.drawable.btn_check_off);
                    }
                } else if (i == sendByEnterRow) {
                    textView.setText(LocaleController.getString("SendByEnter", br.com.uatizapi.messenger.R.string.SendByEnter));
                    divider.setVisibility(View.INVISIBLE);
                    boolean enabled = preferences.getBoolean("send_by_enter", false);
                    if (enabled) {
                        checkButton.setImageResource(br.com.uatizapi.messenger.R.drawable.btn_check_on);
                    } else {
                        checkButton.setImageResource(br.com.uatizapi.messenger.R.drawable.btn_check_off);
                    }
                } else if (i == saveToGalleryRow) {
                    textView.setText(LocaleController.getString("SaveToGallerySettings", br.com.uatizapi.messenger.R.string.SaveToGallerySettings));
                    divider.setVisibility(View.INVISIBLE);
                    if (MediaController.getInstance().canSaveToGallery()) {
                        checkButton.setImageResource(br.com.uatizapi.messenger.R.drawable.btn_check_on);
                    } else {
                        checkButton.setImageResource(br.com.uatizapi.messenger.R.drawable.btn_check_off);
                    }
                }
            } else if (type == 4) {
                if (view == null) {
                    LayoutInflater li = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    view = li.inflate(br.com.uatizapi.messenger.R.layout.settings_logout_button, viewGroup, false);
                    TextView textView = (TextView)view.findViewById(br.com.uatizapi.messenger.R.id.settings_row_text);
                    textView.setText(LocaleController.getString("LogOut", br.com.uatizapi.messenger.R.string.LogOut));
                    textView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (getParentActivity() == null) {
                                return;
                            }
                            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                            builder.setMessage(LocaleController.getString("AreYouSureLogout", br.com.uatizapi.messenger.R.string.AreYouSureLogout));
                            builder.setTitle(LocaleController.getString("AppName", br.com.uatizapi.messenger.R.string.AppName));
                            builder.setPositiveButton(LocaleController.getString("OK", br.com.uatizapi.messenger.R.string.OK), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                                    SharedPreferences.Editor editor = preferences.edit();
                                    editor.clear().commit();
                                    NotificationCenter.getInstance().postNotificationName(NotificationCenter.appDidLogout);
                                    MessagesController.getInstance().unregistedPush();
                                    MessagesController.getInstance().logOut();
                                    UserConfig.clearConfig();
                                    MessagesStorage.getInstance().cleanUp(false);
                                    MessagesController.getInstance().cleanUp();
                                    ContactsController.getInstance().deleteAllAppAccounts();
                                }
                            });
                            builder.setNegativeButton(LocaleController.getString("Cancel", br.com.uatizapi.messenger.R.string.Cancel), null);
                            showAlertDialog(builder);
                        }
                    });
                }
            } else if (type == 5) {
                if (view == null) {
                    LayoutInflater li = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    view = li.inflate(br.com.uatizapi.messenger.R.layout.user_profile_leftright_row_layout, viewGroup, false);
                }
                TextView textView = (TextView)view.findViewById(br.com.uatizapi.messenger.R.id.settings_row_text);
                TextView detailTextView = (TextView)view.findViewById(br.com.uatizapi.messenger.R.id.settings_row_text_detail);
                View divider = view.findViewById(br.com.uatizapi.messenger.R.id.settings_row_divider);
                if (i == numberRow) {
                    TLRPC.User user = UserConfig.getCurrentUser();
                    textView.setText(LocaleController.getString("Phone", br.com.uatizapi.messenger.R.string.Phone));
                    if (user != null && user.phone != null && user.phone.length() != 0) {
                        detailTextView.setText(PhoneFormat.getInstance().format("+" + user.phone));
                    } else {
                        detailTextView.setText(LocaleController.getString("NumberUnknown", br.com.uatizapi.messenger.R.string.NumberUnknown));
                    }
                    divider.setVisibility(View.VISIBLE);
                } else if (i == textSizeRow) {
                    SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                    int size = preferences.getInt("fons_size", AndroidUtilities.isTablet() ? 18 : 16);
                    detailTextView.setText(String.format("%d", size));
                    textView.setText(LocaleController.getString("TextSize", br.com.uatizapi.messenger.R.string.TextSize));
                    divider.setVisibility(View.VISIBLE);
                } else if (i == languageRow) {
                    detailTextView.setText(LocaleController.getCurrentLanguageName());
                    textView.setText(LocaleController.getString("Language", br.com.uatizapi.messenger.R.string.Language));
                    divider.setVisibility(View.VISIBLE);
                } else if (i == contactsSortRow) {
                    textView.setText(LocaleController.getString("SortBy", br.com.uatizapi.messenger.R.string.SortBy));
                    divider.setVisibility(View.VISIBLE);
                    SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                    int sort = preferences.getInt("sortContactsBy", 0);
                    if (sort == 0) {
                        detailTextView.setText(LocaleController.getString("Default", br.com.uatizapi.messenger.R.string.Default));
                    } else if (sort == 1) {
                        detailTextView.setText(LocaleController.getString("FirstName", br.com.uatizapi.messenger.R.string.SortFirstName));
                    } else if (sort == 2) {
                        detailTextView.setText(LocaleController.getString("LastName", br.com.uatizapi.messenger.R.string.SortLastName));
                    }
                } else if (i == usernameRow) {
                    TLRPC.User user = UserConfig.getCurrentUser();
                    textView.setText(LocaleController.getString("Username", br.com.uatizapi.messenger.R.string.Username));
                    if (user != null && user.username != null && user.username.length() != 0) {
                        detailTextView.setText("@" + user.username);
                    } else {
                        detailTextView.setText(LocaleController.getString("UsernameEmpty", br.com.uatizapi.messenger.R.string.UsernameEmpty));
                    }
                    divider.setVisibility(View.INVISIBLE);
                }
            } else if (type == 6) {
                if (view == null) {
                    LayoutInflater li = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    view = li.inflate(br.com.uatizapi.messenger.R.layout.settings_row_version, viewGroup, false);
                    TextView textView = (TextView)view.findViewById(br.com.uatizapi.messenger.R.id.settings_row_text);
                    try {
                        PackageInfo pInfo = ApplicationLoader.applicationContext.getPackageManager().getPackageInfo(ApplicationLoader.applicationContext.getPackageName(), 0);
                        textView.setText(String.format(Locale.US, "Uatizapi for Android v%s (%d)", pInfo.versionName, pInfo.versionCode));
                    } catch (Exception e) {
                        FileLog.e("tmessages", e);
                    }
                }
            } else if (type == 7) {
                if (view == null) {
                    LayoutInflater li = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    view = li.inflate(br.com.uatizapi.messenger.R.layout.settings_row_detail_layout, viewGroup, false);
                }
                TextView textView = (TextView)view.findViewById(br.com.uatizapi.messenger.R.id.settings_row_text);
                TextView textViewDetail = (TextView)view.findViewById(br.com.uatizapi.messenger.R.id.settings_row_text_detail);
                View divider = view.findViewById(br.com.uatizapi.messenger.R.id.settings_row_divider);

                SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                int mask = 0;
                if (i == mobileDownloadRow) {
                    textView.setText(LocaleController.getString("WhenUsingMobileData", br.com.uatizapi.messenger.R.string.WhenUsingMobileData));
                    divider.setVisibility(View.VISIBLE);
                    mask = MediaController.getInstance().mobileDataDownloadMask;
                } else if (i == wifiDownloadRow) {
                    textView.setText(LocaleController.getString("WhenConnectedOnWiFi", br.com.uatizapi.messenger.R.string.WhenConnectedOnWiFi));
                    divider.setVisibility(View.VISIBLE);
                    mask = MediaController.getInstance().wifiDownloadMask;
                } else if (i == roamingDownloadRow) {
                    textView.setText(LocaleController.getString("WhenRoaming", br.com.uatizapi.messenger.R.string.WhenRoaming));
                    divider.setVisibility(View.VISIBLE);
                    mask = MediaController.getInstance().roamingDownloadMask;
                }
                String text = "";
                if ((mask & MediaController.AUTODOWNLOAD_MASK_PHOTO) != 0) {
                    text += LocaleController.getString("AttachPhoto", br.com.uatizapi.messenger.R.string.AttachPhoto);
                }
                if ((mask & MediaController.AUTODOWNLOAD_MASK_AUDIO) != 0) {
                    if (text.length() != 0) {
                        text += ", ";
                    }
                    text += LocaleController.getString("AttachAudio", br.com.uatizapi.messenger.R.string.AttachAudio);
                }
                if ((mask & MediaController.AUTODOWNLOAD_MASK_VIDEO) != 0) {
                    if (text.length() != 0) {
                        text += ", ";
                    }
                    text += LocaleController.getString("AttachVideo", br.com.uatizapi.messenger.R.string.AttachVideo);
                }
                if ((mask & MediaController.AUTODOWNLOAD_MASK_DOCUMENT) != 0) {
                    if (text.length() != 0) {
                        text += ", ";
                    }
                    text += LocaleController.getString("AttachDocument", br.com.uatizapi.messenger.R.string.AttachDocument);
                }
                if (text.length() == 0) {
                    text = LocaleController.getString("NoMediaAutoDownload", br.com.uatizapi.messenger.R.string.NoMediaAutoDownload);
                }
                textViewDetail.setText(text);
            }
            return view;
        }

        @Override
        public int getItemViewType(int i) {
            if (i == profileRow) {
                return 0;
            } else if (i == numberSectionRow || i == settingsSectionRow || i == supportSectionRow || i == messagesSectionRow || i == mediaDownloadSection || i == contactsSectionRow) {
                return 1;
            } else if (i == textSizeRow || i == languageRow || i == contactsSortRow || i == numberRow || i == usernameRow) {
                return 5;
            } else if (i == enableAnimationsRow || i == sendByEnterRow || i == saveToGalleryRow) {
                return 3;
            } else if (i == notificationRow || i == blockedRow || i == backgroundRow || i == askQuestionRow || i == sendLogsRow || i == terminateSessionsRow || i == clearLogsRow || i == switchBackendButtonRow || i == uatizapiFaqRow || i == contactsReimportRow) {
                return 2;
            } else if (i == logoutRow) {
                return 4;
            } else if (i == versionRow) {
                return 6;
            } else if (i == wifiDownloadRow || i == mobileDownloadRow || i == roamingDownloadRow) {
                return 7;
            } else {
                return 2;
            }
        }

        @Override
        public int getViewTypeCount() {
            return 8;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }
    }
}
