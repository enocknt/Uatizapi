/*
 * This is the source code of Telegram for Android v. 1.3.2.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013.
 */

package br.com.uatizapi.ui;

import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import br.com.uatizapi.android.AndroidUtilities;
import br.com.uatizapi.android.LocaleController;
import br.com.uatizapi.messenger.TLRPC;
import br.com.uatizapi.android.MessagesController;
import br.com.uatizapi.messenger.R;
import br.com.uatizapi.ui.Views.ActionBar.ActionBarLayer;
import br.com.uatizapi.ui.Views.ActionBar.BaseFragment;
import br.com.uatizapi.ui.Views.IdenticonView;

public class IdenticonActivity extends BaseFragment {
    private int chat_id;

    public IdenticonActivity(Bundle args) {
        super(args);
    }

    @Override
    public boolean onFragmentCreate() {
        chat_id = getArguments().getInt("chat_id");
        return super.onFragmentCreate();
    }

    @Override
    public View createView(LayoutInflater inflater, ViewGroup container) {
        if (fragmentView == null) {
            actionBarLayer.setDisplayHomeAsUpEnabled(true, R.drawable.ic_ab_back);
            actionBarLayer.setBackOverlay(R.layout.updating_state_layout);
            actionBarLayer.setTitle(LocaleController.getString("EncryptionKey", R.string.EncryptionKey));
            actionBarLayer.setTitleIcon(R.drawable.ic_lock_white, AndroidUtilities.dp(4));

            actionBarLayer.setActionBarMenuOnItemClick(new ActionBarLayer.ActionBarMenuOnItemClick() {
                @Override
                public void onItemClick(int id) {
                    if (id == -1) {
                        finishFragment();
                    }
                }
            });

            fragmentView = inflater.inflate(R.layout.identicon_layout, container, false);
            IdenticonView identiconView = (IdenticonView) fragmentView.findViewById(R.id.identicon_view);
            TextView textView = (TextView)fragmentView.findViewById(R.id.identicon_text);
            TLRPC.EncryptedChat encryptedChat = MessagesController.getInstance().getEncryptedChat(chat_id);
            if (encryptedChat != null) {
                identiconView.setBytes(encryptedChat.auth_key);
                TLRPC.User user = MessagesController.getInstance().getUser(encryptedChat.user_id);
                textView.setText(Html.fromHtml(LocaleController.formatString("EncryptionKeyDescription", R.string.EncryptionKeyDescription, user.first_name, user.first_name)));
            }
        } else {
            ViewGroup parent = (ViewGroup)fragmentView.getParent();
            if (parent != null) {
                parent.removeView(fragmentView);
            }
        }
        return fragmentView;
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        fixLayout();
    }

    @Override
    public void onResume() {
        super.onResume();
        fixLayout();
    }

    private void fixLayout() {
        ViewTreeObserver obs = fragmentView.getViewTreeObserver();
        obs.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (fragmentView != null) {
                    fragmentView.getViewTreeObserver().removeOnPreDrawListener(this);
                }
                if (getParentActivity() == null || fragmentView == null) {
                    return true;
                }
                LinearLayout layout = (LinearLayout)fragmentView;
                WindowManager manager = (WindowManager)ApplicationLoader.applicationContext.getSystemService(Context.WINDOW_SERVICE);
                int rotation = manager.getDefaultDisplay().getRotation();

                if (rotation == Surface.ROTATION_270 || rotation == Surface.ROTATION_90) {
                    layout.setOrientation(LinearLayout.HORIZONTAL);
                } else {
                    layout.setOrientation(LinearLayout.VERTICAL);
                }

                fragmentView.setPadding(fragmentView.getPaddingLeft(), 0, fragmentView.getPaddingRight(), fragmentView.getPaddingBottom());
                return false;
            }
        });
    }
}
