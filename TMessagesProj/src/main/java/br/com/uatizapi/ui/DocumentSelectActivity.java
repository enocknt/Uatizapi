/*
 * This is the source code of Telegram for Android v. 1.3.2.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013.
 */

package br.com.uatizapi.ui;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import br.com.uatizapi.messenger.FileLog;
import br.com.uatizapi.android.LocaleController;
import br.com.uatizapi.messenger.Utilities;
import br.com.uatizapi.ui.Adapters.BaseFragmentAdapter;
import br.com.uatizapi.ui.Views.ActionBar.ActionBarLayer;
import br.com.uatizapi.ui.Views.ActionBar.ActionBarMenu;
import br.com.uatizapi.ui.Views.ActionBar.ActionBarMenuItem;
import br.com.uatizapi.ui.Views.BackupImageView;
import br.com.uatizapi.ui.Views.ActionBar.BaseFragment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

public class DocumentSelectActivity extends BaseFragment {

    public static abstract interface DocumentSelectActivityDelegate {
        public void didSelectFile(DocumentSelectActivity activity, String path);
        public void startDocumentSelectActivity();
    }

    private ListView listView;
    private ListAdapter listAdapter;
    private File currentDir;
    private TextView emptyView;
    private ArrayList<ListItem> items = new ArrayList<ListItem>();
    private boolean receiverRegistered = false;
    private ArrayList<HistoryEntry> history = new ArrayList<HistoryEntry>();
    private long sizeLimit = 1024 * 1024 * 1024;
    private DocumentSelectActivityDelegate delegate;

    private class ListItem {
        int icon;
        String title;
        String subtitle = "";
        String ext = "";
        String thumb;
        File file;
    }

    private class HistoryEntry {
        int scrollItem, scrollOffset;
        File dir;
        String title;
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context arg0, Intent intent) {
            Runnable r = new Runnable() {
                public void run() {
                    try {
                        if (currentDir == null) {
                            listRoots();
                        } else {
                            listFiles(currentDir);
                        }
                    } catch (Exception e) {
                        FileLog.e("tmessages", e);
                    }
                }
            };
            if (Intent.ACTION_MEDIA_UNMOUNTED.equals(intent.getAction())) {
                listView.postDelayed(r, 1000);
            } else {
                r.run();
            }
        }
    };

    @Override
    public void onFragmentDestroy() {
        try {
            if (receiverRegistered) {
                getParentActivity().unregisterReceiver(receiver);
            }
        } catch (Exception e) {
            FileLog.e("tmessages", e);
        }
        super.onFragmentDestroy();
    }

    @Override
    public View createView(LayoutInflater inflater, ViewGroup container) {
        if (!receiverRegistered) {
            receiverRegistered = true;
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
            filter.addAction(Intent.ACTION_MEDIA_CHECKING);
            filter.addAction(Intent.ACTION_MEDIA_EJECT);
            filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
            filter.addAction(Intent.ACTION_MEDIA_NOFS);
            filter.addAction(Intent.ACTION_MEDIA_REMOVED);
            filter.addAction(Intent.ACTION_MEDIA_SHARED);
            filter.addAction(Intent.ACTION_MEDIA_UNMOUNTABLE);
            filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
            filter.addDataScheme("file");
            getParentActivity().registerReceiver(receiver, filter);
        }

        if (fragmentView == null) {
            actionBarLayer.setDisplayHomeAsUpEnabled(true, br.com.uatizapi.messenger.R.drawable.ic_ab_back);
            actionBarLayer.setBackOverlay(br.com.uatizapi.messenger.R.layout.updating_state_layout);
            actionBarLayer.setTitle(LocaleController.getString("SelectFile", br.com.uatizapi.messenger.R.string.SelectFile));
            actionBarLayer.setActionBarMenuOnItemClick(new ActionBarLayer.ActionBarMenuOnItemClick() {
                @Override
                public void onItemClick(int id) {
                    if (id == -1) {
                        finishFragment();
                    } else if (id == 1) {
                        if (delegate != null) {
                            delegate.startDocumentSelectActivity();
                        }
                        finishFragment(false);
                    }
                }
            });
            ActionBarMenu menu = actionBarLayer.createMenu();
            ActionBarMenuItem item = menu.addItem(1, br.com.uatizapi.messenger.R.drawable.ic_ab_other);

            fragmentView = inflater.inflate(br.com.uatizapi.messenger.R.layout.document_select_layout, container, false);
            listAdapter = new ListAdapter(getParentActivity());
            emptyView = (TextView)fragmentView.findViewById(br.com.uatizapi.messenger.R.id.searchEmptyView);
            emptyView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return true;
                }
            });
            listView = (ListView)fragmentView.findViewById(br.com.uatizapi.messenger.R.id.listView);
            listView.setEmptyView(emptyView);
            listView.setAdapter(listAdapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    ListItem item = items.get(i);
                    File file = item.file;
                    if (file == null) {
                        HistoryEntry he = history.remove(history.size() - 1);
                        actionBarLayer.setTitle(he.title);
                        if (he.dir != null) {
                            listFiles(he.dir);
                        } else {
                            listRoots();
                        }
                        listView.setSelectionFromTop(he.scrollItem, he.scrollOffset);
                    } else if (file.isDirectory()) {
                        HistoryEntry he = new HistoryEntry();
                        he.scrollItem = listView.getFirstVisiblePosition();
                        he.scrollOffset = listView.getChildAt(0).getTop();
                        he.dir = currentDir;
                        he.title = actionBarLayer.getTitle().toString();
                        if (!listFiles(file)) {
                            return;
                        }
                        history.add(he);
                        actionBarLayer.setTitle(item.title);
                        listView.setSelection(0);
                    } else {
                        if (!file.canRead()) {
                            showErrorBox(LocaleController.getString("AccessError", br.com.uatizapi.messenger.R.string.AccessError));
                            return;
                        }
                        if (sizeLimit != 0) {
                            if (file.length() > sizeLimit) {
                                showErrorBox(LocaleController.formatString("FileUploadLimit", br.com.uatizapi.messenger.R.string.FileUploadLimit, Utilities.formatFileSize(sizeLimit)));
                                return;
                            }
                        }
                        if (file.length() == 0) {
                            return;
                        }
                        if (delegate != null) {
                            delegate.didSelectFile(DocumentSelectActivity.this, file.getAbsolutePath());
                        }
                    }
                }
            });

            listRoots();
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
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public boolean onBackPressed() {
        if (history.size() > 0) {
            HistoryEntry he = history.remove(history.size() - 1);
            actionBarLayer.setTitle(he.title);
            if (he.dir != null) {
                listFiles(he.dir);
            } else {
                listRoots();
            }
            listView.setSelectionFromTop(he.scrollItem, he.scrollOffset);
            return false;
        }
        return super.onBackPressed();
    }

    public void setDelegate(DocumentSelectActivityDelegate delegate) {
        this.delegate = delegate;
    }

    private boolean listFiles(File dir) {
        if (!dir.canRead()) {
            if (dir.getAbsolutePath().startsWith(Environment.getExternalStorageDirectory().toString())
                    || dir.getAbsolutePath().startsWith("/sdcard")
                    || dir.getAbsolutePath().startsWith("/mnt/sdcard")) {
                if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)
                        && !Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
                    currentDir = dir;
                    items.clear();
                    String state = Environment.getExternalStorageState();
                    if (Environment.MEDIA_SHARED.equals(state)) {
                        emptyView.setText(LocaleController.getString("UsbActive", br.com.uatizapi.messenger.R.string.UsbActive));
                    } else {
                        emptyView.setText(LocaleController.getString("NotMounted", br.com.uatizapi.messenger.R.string.NotMounted));
                    }
                    listAdapter.notifyDataSetChanged();
                    return true;
                }
            }
            showErrorBox(LocaleController.getString("AccessError", br.com.uatizapi.messenger.R.string.AccessError));
            return false;
        }
        emptyView.setText(LocaleController.getString("NoFiles", br.com.uatizapi.messenger.R.string.NoFiles));
        File[] files = null;
        try {
            files = dir.listFiles();
        } catch(Exception e) {
            showErrorBox(e.getLocalizedMessage());
            return false;
        }
        if (files == null) {
            showErrorBox(LocaleController.getString("UnknownError", br.com.uatizapi.messenger.R.string.UnknownError));
            return false;
        }
        currentDir = dir;
        items.clear();
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File lhs, File rhs) {
                if (lhs.isDirectory() != rhs.isDirectory()) {
                    return lhs.isDirectory() ? -1 : 1;
                }
                return lhs.getName().compareToIgnoreCase(rhs.getName());
                /*long lm = lhs.lastModified();
                long rm = lhs.lastModified();
                if (lm == rm) {
                    return 0;
                } else if (lm > rm) {
                    return -1;
                } else {
                    return 1;
                }*/
            }
        });
        for (File file : files) {
            if (file.getName().startsWith(".")) {
                continue;
            }
            ListItem item = new ListItem();
            item.title = file.getName();
            item.file = file;
            if (file.isDirectory()) {
                item.icon = br.com.uatizapi.messenger.R.drawable.ic_directory;
            } else {
                String fname = file.getName();
                String[] sp = fname.split("\\.");
                item.ext = sp.length > 1 ? sp[sp.length - 1] : "?";
                item.subtitle = Utilities.formatFileSize(file.length());
                fname = fname.toLowerCase();
                if (fname.endsWith(".jpg") || fname.endsWith(".png") || fname.endsWith(".gif") || fname.endsWith(".jpeg")) {
                    item.thumb = file.getAbsolutePath();
                }
            }
            items.add(item);
        }
        ListItem item = new ListItem();
        item.title = "..";
        item.subtitle = "";
        item.icon = br.com.uatizapi.messenger.R.drawable.ic_directory;
        item.file = null;
        items.add(0, item);
        listAdapter.notifyDataSetChanged();
        return true;
    }

    private void showErrorBox(String error) {
        if (getParentActivity() == null) {
            return;
        }
        new AlertDialog.Builder(getParentActivity())
                .setTitle(LocaleController.getString("AppName", br.com.uatizapi.messenger.R.string.AppName))
                .setMessage(error)
                .setPositiveButton(br.com.uatizapi.messenger.R.string.OK, null)
                .show();
    }

    private void listRoots() {
        currentDir = null;
        items.clear();
        String extStorage = Environment.getExternalStorageDirectory().getAbsolutePath();
        ListItem ext = new ListItem();
        if (Build.VERSION.SDK_INT < 9 || Environment.isExternalStorageRemovable()) {
            ext.title = LocaleController.getString("SdCard", br.com.uatizapi.messenger.R.string.SdCard);
        } else {
            ext.title = LocaleController.getString("InternalStorage", br.com.uatizapi.messenger.R.string.InternalStorage);
        }
        ext.icon = Build.VERSION.SDK_INT < 9 || Environment.isExternalStorageRemovable() ? br.com.uatizapi.messenger.R.drawable.ic_external_storage : br.com.uatizapi.messenger.R.drawable.ic_storage;
        ext.subtitle = getRootSubtitle(extStorage);
        ext.file = Environment.getExternalStorageDirectory();
        items.add(ext);
        try {
            BufferedReader reader = new BufferedReader(new FileReader("/proc/mounts"));
            String line;
            HashMap<String, ArrayList<String>> aliases = new HashMap<String, ArrayList<String>>();
            ArrayList<String> result = new ArrayList<String>();
            String extDevice = null;
            while ((line = reader.readLine()) != null) {
                if ((!line.contains("/mnt") && !line.contains("/storage") && !line.contains("/sdcard")) || line.contains("asec") || line.contains("tmpfs") || line.contains("none")) {
                    continue;
                }
                String[] info = line.split(" ");
                if (!aliases.containsKey(info[0])) {
                    aliases.put(info[0], new ArrayList<String>());
                }
                aliases.get(info[0]).add(info[1]);
                if (info[1].equals(extStorage)) {
                    extDevice=info[0];
                }
                result.add(info[1]);
            }
            reader.close();
            if (extDevice != null) {
                result.removeAll(aliases.get(extDevice));
                for (String path : result) {
                    try {
                        ListItem item = new ListItem();
                        if (path.toLowerCase().contains("sd")) {
                            ext.title = LocaleController.getString("SdCard", br.com.uatizapi.messenger.R.string.SdCard);
                        } else {
                            ext.title = LocaleController.getString("ExternalStorage", br.com.uatizapi.messenger.R.string.ExternalStorage);
                        }
                        item.icon = br.com.uatizapi.messenger.R.drawable.ic_external_storage;
                        item.subtitle = getRootSubtitle(path);
                        item.file = new File(path);
                        items.add(item);
                    } catch (Exception e) {
                        FileLog.e("tmessages", e);
                    }
                }
            }
        } catch (Exception e) {
            FileLog.e("tmessages", e);
        }
        ListItem fs = new ListItem();
        fs.title = "/";
        fs.subtitle = LocaleController.getString("SystemRoot", br.com.uatizapi.messenger.R.string.SystemRoot);
        fs.icon = br.com.uatizapi.messenger.R.drawable.ic_directory;
        fs.file = new File("/");
        items.add(fs);

        try {
            File telegramPath = new File(Environment.getExternalStorageDirectory(), "Telegram");
            if (telegramPath.exists()) {
                fs = new ListItem();
                fs.title = "Telegram";
                fs.subtitle = telegramPath.toString();
                fs.icon = br.com.uatizapi.messenger.R.drawable.ic_directory;
                fs.file = telegramPath;
                items.add(fs);
            }
        } catch (Exception e) {
            FileLog.e("tmessages", e);
        }

        listAdapter.notifyDataSetChanged();
    }

    private String getRootSubtitle(String path) {
        StatFs stat = new StatFs(path);
        long total = (long)stat.getBlockCount() * (long)stat.getBlockSize();
        long free = (long)stat.getAvailableBlocks() * (long)stat.getBlockSize();
        if (total == 0) {
            return "";
        }
        return LocaleController.formatString("FreeOfTotal", br.com.uatizapi.messenger.R.string.FreeOfTotal, Utilities.formatFileSize(free), Utilities.formatFileSize(total));
    }

    private class ListAdapter extends BaseFragmentAdapter {
        private Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        public int getViewTypeCount() {
            return 2;
        }

        public int getItemViewType(int pos) {
            return items.get(pos).subtitle.length() > 0 ? 0 : 1;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            ListItem item = items.get(position);
            if (v == null) {
                v = View.inflate(mContext, br.com.uatizapi.messenger.R.layout.document_item, null);
                if (item.subtitle.length() == 0) {
                    v.findViewById(br.com.uatizapi.messenger.R.id.docs_item_info).setVisibility(View.GONE);
                }
            }
            TextView typeTextView = (TextView)v.findViewById(br.com.uatizapi.messenger.R.id.docs_item_type);
            ((TextView)v.findViewById(br.com.uatizapi.messenger.R.id.docs_item_title)).setText(item.title);

            ((TextView)v.findViewById(br.com.uatizapi.messenger.R.id.docs_item_info)).setText(item.subtitle);
            BackupImageView imageView = (BackupImageView)v.findViewById(br.com.uatizapi.messenger.R.id.docs_item_thumb);
            if (item.thumb != null) {
                imageView.setImageBitmap(null);
                typeTextView.setText(item.ext.toUpperCase().substring(0, Math.min(item.ext.length(), 4)));
                imageView.setImage(item.thumb, "55_42", 0);
                imageView.setVisibility(View.VISIBLE);
                typeTextView.setVisibility(View.VISIBLE);
            } else if (item.icon != 0) {
                imageView.setImageResource(item.icon);
                imageView.setVisibility(View.VISIBLE);
                typeTextView.setVisibility(View.GONE);
            } else {
                typeTextView.setText(item.ext.toUpperCase().substring(0, Math.min(item.ext.length(), 4)));
                imageView.setVisibility(View.GONE);
                typeTextView.setVisibility(View.VISIBLE);
            }
            return v;
        }
    }
}
