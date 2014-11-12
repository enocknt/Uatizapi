/*
 * This is the source code of Telegram for Android v. 1.3.2.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013.
 */

package br.com.uatizapi.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import br.com.uatizapi.android.AndroidUtilities;
import br.com.uatizapi.android.LocaleController;
import br.com.uatizapi.messenger.Utilities;

public class IntroActivity extends Activity {
    private ViewPager viewPager;
    private ImageView topImage1;
    private ImageView topImage2;
    private ViewGroup bottomPages;
    private int lastPage = 0;
    private boolean justCreated = false;
    private boolean startPressed = false;
    private int[] icons;
    private int[] titles;
    private int[] messages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(br.com.uatizapi.messenger.R.style.Theme_TMessages);
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        if (AndroidUtilities.isTablet()) {
            setContentView(br.com.uatizapi.messenger.R.layout.intro_layout_tablet);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            setContentView(br.com.uatizapi.messenger.R.layout.intro_layout);
        }

        if (LocaleController.isRTL) {
            icons = new int[] {
                    br.com.uatizapi.messenger.R.drawable.intro7,
                    br.com.uatizapi.messenger.R.drawable.intro6,
                    br.com.uatizapi.messenger.R.drawable.intro5,
                    br.com.uatizapi.messenger.R.drawable.intro4,
                    br.com.uatizapi.messenger.R.drawable.intro3,
                    br.com.uatizapi.messenger.R.drawable.intro2,
                    br.com.uatizapi.messenger.R.drawable.intro1
            };
            titles = new int[] {
                    br.com.uatizapi.messenger.R.string.Page7Title,
                    br.com.uatizapi.messenger.R.string.Page6Title,
                    br.com.uatizapi.messenger.R.string.Page5Title,
                    br.com.uatizapi.messenger.R.string.Page4Title,
                    br.com.uatizapi.messenger.R.string.Page3Title,
                    br.com.uatizapi.messenger.R.string.Page2Title,
                    br.com.uatizapi.messenger.R.string.Page1Title
            };
            messages = new int[] {
                    br.com.uatizapi.messenger.R.string.Page7Message,
                    br.com.uatizapi.messenger.R.string.Page6Message,
                    br.com.uatizapi.messenger.R.string.Page5Message,
                    br.com.uatizapi.messenger.R.string.Page4Message,
                    br.com.uatizapi.messenger.R.string.Page3Message,
                    br.com.uatizapi.messenger.R.string.Page2Message,
                    br.com.uatizapi.messenger.R.string.Page1Message
            };
        } else {
            icons = new int[] {
                    br.com.uatizapi.messenger.R.drawable.intro1,
                    br.com.uatizapi.messenger.R.drawable.intro2,
                    br.com.uatizapi.messenger.R.drawable.intro3,
                    br.com.uatizapi.messenger.R.drawable.intro4,
                    br.com.uatizapi.messenger.R.drawable.intro5,
                    br.com.uatizapi.messenger.R.drawable.intro6,
                    br.com.uatizapi.messenger.R.drawable.intro7
            };
            titles = new int[] {
                    br.com.uatizapi.messenger.R.string.Page1Title,
                    br.com.uatizapi.messenger.R.string.Page2Title,
                    br.com.uatizapi.messenger.R.string.Page3Title,
                    br.com.uatizapi.messenger.R.string.Page4Title,
                    br.com.uatizapi.messenger.R.string.Page5Title,
                    br.com.uatizapi.messenger.R.string.Page6Title,
                    br.com.uatizapi.messenger.R.string.Page7Title
            };
            messages = new int[] {
                    br.com.uatizapi.messenger.R.string.Page1Message,
                    br.com.uatizapi.messenger.R.string.Page2Message,
                    br.com.uatizapi.messenger.R.string.Page3Message,
                    br.com.uatizapi.messenger.R.string.Page4Message,
                    br.com.uatizapi.messenger.R.string.Page5Message,
                    br.com.uatizapi.messenger.R.string.Page6Message,
                    br.com.uatizapi.messenger.R.string.Page7Message
            };
        }
        viewPager = (ViewPager)findViewById(br.com.uatizapi.messenger.R.id.intro_view_pager);
        TextView startMessagingButton = (TextView) findViewById(br.com.uatizapi.messenger.R.id.start_messaging_button);
        startMessagingButton.setText(LocaleController.getString("StartMessaging", br.com.uatizapi.messenger.R.string.StartMessaging));
        topImage1 = (ImageView)findViewById(br.com.uatizapi.messenger.R.id.icon_image1);
        topImage2 = (ImageView)findViewById(br.com.uatizapi.messenger.R.id.icon_image2);
        bottomPages = (ViewGroup)findViewById(br.com.uatizapi.messenger.R.id.bottom_pages);
        topImage2.setVisibility(View.GONE);
        viewPager.setAdapter(new IntroAdapter());
        viewPager.setPageMargin(0);
        viewPager.setOffscreenPageLimit(1);
        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int i) {

            }

            @Override
            public void onPageScrollStateChanged(int i) {
                if (i == ViewPager.SCROLL_STATE_IDLE || i == ViewPager.SCROLL_STATE_SETTLING) {
                    if (lastPage != viewPager.getCurrentItem()) {
                        lastPage = viewPager.getCurrentItem();

                        final ImageView fadeoutImage;
                        final ImageView fadeinImage;
                        if (topImage1.getVisibility() == View.VISIBLE) {
                            fadeoutImage = topImage1;
                            fadeinImage = topImage2;

                        } else {
                            fadeoutImage = topImage2;
                            fadeinImage = topImage1;
                        }

                        fadeinImage.bringToFront();
                        fadeinImage.setImageResource(icons[lastPage]);
                        fadeinImage.clearAnimation();
                        fadeoutImage.clearAnimation();


                        Animation outAnimation = AnimationUtils.loadAnimation(IntroActivity.this, br.com.uatizapi.messenger.R.anim.icon_anim_fade_out);
                        outAnimation.setAnimationListener(new Animation.AnimationListener() {
                            @Override
                            public void onAnimationStart(Animation animation) {
                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {
                                fadeoutImage.setVisibility(View.GONE);
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {

                            }
                        });

                        Animation inAnimation = AnimationUtils.loadAnimation(IntroActivity.this, br.com.uatizapi.messenger.R.anim.icon_anim_fade_in);
                        inAnimation.setAnimationListener(new Animation.AnimationListener() {
                            @Override
                            public void onAnimationStart(Animation animation) {
                                fadeinImage.setVisibility(View.VISIBLE);
                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {

                            }
                        });


                        fadeoutImage.startAnimation(outAnimation);
                        fadeinImage.startAnimation(inAnimation);
                    }
                }
            }
        });

        startMessagingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (startPressed) {
                    return;
                }
                startPressed = true;
                Intent intent2 = new Intent(IntroActivity.this, LaunchActivity.class);
                intent2.putExtra("fromIntro", true);
                startActivity(intent2);
                finish();
            }
        });

        justCreated = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (justCreated) {
            if (LocaleController.isRTL) {
                viewPager.setCurrentItem(6);
                lastPage = 6;
            } else {
                viewPager.setCurrentItem(0);
                lastPage = 0;
            }
            justCreated = false;
        }
        Utilities.checkForCrashes(this);
        Utilities.checkForUpdates(this);
    }

    private class IntroAdapter extends PagerAdapter {
        @Override
        public int getCount() {
            return 7;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View view = View.inflate(container.getContext(), br.com.uatizapi.messenger.R.layout.intro_view_layout, null);
            TextView headerTextView = (TextView)view.findViewById(br.com.uatizapi.messenger.R.id.header_text);
            TextView messageTextView = (TextView)view.findViewById(br.com.uatizapi.messenger.R.id.message_text);
            container.addView(view, 0);

            headerTextView.setText(getString(titles[position]));
            messageTextView.setText(Html.fromHtml(getString(messages[position])));

            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
            int count = bottomPages.getChildCount();
            for (int a = 0; a < count; a++) {
                View child = bottomPages.getChildAt(a);
                if (a == position) {
                    child.setBackgroundColor(0xff2ca5e0);
                } else {
                    child.setBackgroundColor(0xffbbbbbb);
                }
            }
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view.equals(object);
        }

        @Override
        public void finishUpdate(View arg0) {
        }

        @Override
        public void restoreState(Parcelable arg0, ClassLoader arg1) {
        }

        @Override
        public Parcelable saveState() {
            return null;
        }

        @Override
        public void startUpdate(View arg0) {
        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {
            if (observer != null) {
                super.unregisterDataSetObserver(observer);
            }
        }
    }
}
