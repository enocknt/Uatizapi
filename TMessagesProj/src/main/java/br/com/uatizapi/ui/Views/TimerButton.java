/*
 * This is the source code of Telegram for Android v. 1.3.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2014.
 */

package br.com.uatizapi.ui.Views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import br.com.uatizapi.android.AndroidUtilities;
import br.com.uatizapi.messenger.FileLog;

public class TimerButton extends View {

    private static Drawable emptyTimerDrawable;
    private static Drawable timerDrawable;
    private static TextPaint timePaint;
    private StaticLayout timeLayout;
    private float timeWidth = 0;
    private int timeHeight = 0;
    private int time = 0;

    private void init() {
        if (emptyTimerDrawable == null) {
            emptyTimerDrawable = getResources().getDrawable(br.com.uatizapi.messenger.R.drawable.header_timer);
            timerDrawable = getResources().getDrawable(br.com.uatizapi.messenger.R.drawable.header_timer2);
            timePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            timePaint.setTextSize(AndroidUtilities.dp(10));
            timePaint.setColor(0xffd7e8f7);
            timePaint.setTypeface(Typeface.DEFAULT_BOLD);
        }

        setBackgroundResource(br.com.uatizapi.messenger.R.drawable.bar_selector);
    }

    public TimerButton(Context context) {
        super(context);
        init();
    }

    public TimerButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TimerButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setTime(int value) {
        time = value;

        String timeString = null;
        if (time >= 1 && time < 60) {
            timeString = "" + value;
            if (timeString.length() < 2) {
                timeString += "s";
            }
        } else if (time >= 60 && time < 60 * 60) {
            timeString = "" + value / 60;
            if (timeString.length() < 2) {
                timeString += "m";
            }
        } else if (time >= 60 * 60 && time < 60 * 60 * 24) {
            timeString = "" + value / 60 / 60;
            if (timeString.length() < 2) {
                timeString += "h";
            }
        } else if (time >= 60 * 60 * 24 && time < 60 * 60 * 24 * 7) {
            timeString = "" + value / 60 / 60 / 24;
            if (timeString.length() < 2) {
                timeString += "d";
            }
        } else {
            timeString = "" + value / 60 / 60 / 24 / 7;
            if (timeString.length() < 2) {
                timeString += "w";
            } else if (timeString.length() > 2) {
                timeString = "c";
            }
        }

        timeWidth = timePaint.measureText(timeString);
        try {
            timeLayout = new StaticLayout(timeString, timePaint, (int)Math.ceil(timeWidth), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
            timeHeight = timeLayout.getHeight();
        } catch (Exception e) {
            timeLayout = null;
            FileLog.e("tmessages", e);
        }

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        Drawable drawable = null;
        if (time == 0) {
            drawable = timerDrawable;
        } else {
            drawable = emptyTimerDrawable;
        }

        int x = (width - drawable.getIntrinsicWidth()) / 2;
        int y = (height - drawable.getIntrinsicHeight()) / 2;
        drawable.setBounds(x, y, x + drawable.getIntrinsicWidth(), y + drawable.getIntrinsicHeight());
        drawable.draw(canvas);

        if (time != 0 && timeLayout != null) {
            canvas.translate((int)(width / 2 - Math.ceil(timeWidth / 2)), (height - timeHeight) / 2 + AndroidUtilities.dpf2(1.5f));
            timeLayout.draw(canvas);
        }
    }
}
