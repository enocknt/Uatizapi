/*
 * This is the source code of Telegram for Android v. 1.3.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013.
 */

package br.com.uatizapi.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import br.com.uatizapi.android.AndroidUtilities;
import br.com.uatizapi.android.ImageLoader;
import br.com.uatizapi.messenger.FileLog;
import br.com.uatizapi.android.LocaleController;
import br.com.uatizapi.ui.Views.ActionBar.BaseFragment;

import java.io.File;

public class PhotoCropActivity extends BaseFragment {

    public interface PhotoCropActivityDelegate {
        public abstract void didFinishCrop(Bitmap bitmap);
    }

    private class PhotoCropView extends FrameLayout {

        Paint rectPaint = null;
        Paint circlePaint = null;
        Paint halfPaint = null;
        float rectSize = 600;
        float rectX = -1, rectY = -1;
        int draggingState = 0;
        float oldX = 0, oldY = 0;
        int bitmapWidth, bitmapHeight, bitmapX, bitmapY;
        int viewWidth, viewHeight;

        public PhotoCropView(Context context) {
            super(context);
            init();
        }

        public PhotoCropView(Context context, AttributeSet attrs) {
            super(context, attrs);
            init();
        }

        public PhotoCropView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
            init();
        }

        private void init() {
            rectPaint = new Paint();
            rectPaint.setColor(0xfffafafa);
            rectPaint.setStrokeWidth(AndroidUtilities.dp(2));
            rectPaint.setStyle(Paint.Style.STROKE);
            circlePaint = new Paint();
            circlePaint.setColor(0x7fffffff);
            halfPaint = new Paint();
            halfPaint.setColor(0x3f000000);
            setBackgroundColor(0xff000000);

            setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    float x = motionEvent.getX();
                    float y = motionEvent.getY();
                    int cornerSide = AndroidUtilities.dp(14);
                    if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                        if (rectX - cornerSide < x && rectX + cornerSide > x && rectY - cornerSide < y && rectY + cornerSide > y) {
                            draggingState = 1;
                        } else if (rectX - cornerSide + rectSize < x && rectX + cornerSide + rectSize > x && rectY - cornerSide < y && rectY + cornerSide > y) {
                            draggingState = 2;
                        } else if (rectX - cornerSide < x && rectX + cornerSide > x && rectY - cornerSide + rectSize < y && rectY + cornerSide + rectSize > y) {
                            draggingState = 3;
                        } else if (rectX - cornerSide + rectSize < x && rectX + cornerSide + rectSize > x && rectY - cornerSide + rectSize < y && rectY + cornerSide + rectSize > y) {
                            draggingState = 4;
                        } else if (rectX < x && rectX + rectSize > x && rectY < y && rectY + rectSize > y) {
                            draggingState = 5;
                        } else {
                            draggingState = 0;
                        }
                        if (draggingState != 0) {
                            PhotoCropView.this.requestDisallowInterceptTouchEvent(true);
                        }
                        oldX = x;
                        oldY = y;
                    } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                        draggingState = 0;
                    } else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE && draggingState != 0) {
                        float diffX = x - oldX;
                        float diffY = y - oldY;
                        if (draggingState == 5) {
                            rectX += diffX;
                            rectY += diffY;

                            if (rectX < bitmapX) {
                                rectX = bitmapX;
                            } else if (rectX + rectSize > bitmapX + bitmapWidth) {
                                rectX = bitmapX + bitmapWidth - rectSize;
                            }
                            if (rectY < bitmapY) {
                                rectY = bitmapY;
                            } else if (rectY + rectSize > bitmapY + bitmapHeight) {
                                rectY = bitmapY + bitmapHeight - rectSize;
                            }
                        } else if (draggingState == 1) {
                            if (rectSize - diffX < 160) {
                                diffX = rectSize - 160;
                            }
                            if (rectX + diffX < bitmapX) {
                                diffX = bitmapX - rectX;
                            }
                            if (rectY + diffX < bitmapY) {
                                diffX = bitmapY - rectY;
                            }
                            rectX += diffX;
                            rectY += diffX;
                            rectSize -= diffX;
                        } else if (draggingState == 2) {
                            if (rectSize + diffX < 160) {
                                diffX = -(rectSize - 160);
                            }
                            if (rectX + rectSize + diffX > bitmapX + bitmapWidth) {
                                diffX = bitmapX + bitmapWidth - rectX - rectSize;
                            }
                            if (rectY - diffX < bitmapY) {
                                diffX = rectY - bitmapY;
                            }
                            rectY -= diffX;
                            rectSize += diffX;
                        } else if (draggingState == 3) {
                            if (rectSize - diffX < 160) {
                                diffX = rectSize - 160;
                            }
                            if (rectX + diffX < bitmapX) {
                                diffX = bitmapX - rectX;
                            }
                            if (rectY + rectSize - diffX > bitmapY + bitmapHeight) {
                                diffX = rectY + rectSize - bitmapY - bitmapHeight;
                            }
                            rectX += diffX;
                            rectSize -= diffX;
                        } else if (draggingState == 4) {
                            if (rectX + rectSize + diffX > bitmapX + bitmapWidth) {
                                diffX = bitmapX + bitmapWidth - rectX - rectSize;
                            }
                            if (rectY + rectSize + diffX > bitmapY + bitmapHeight) {
                                diffX = bitmapY + bitmapHeight - rectY - rectSize;
                            }
                            rectSize += diffX;
                            if (rectSize < 160) {
                                rectSize = 160;
                            }
                        }

                        oldX = x;
                        oldY = y;
                        invalidate();
                    }
                    return true;
                }
            });
        }

        private void updateBitmapSize() {
            if (viewWidth == 0 || viewHeight == 0 || imageToCrop == null) {
                return;
            }
            float percX = (rectX - bitmapX) / bitmapWidth;
            float percY = (rectY - bitmapY) / bitmapHeight;
            float percSize = rectSize / bitmapWidth;
            float w = imageToCrop.getWidth();
            float h = imageToCrop.getHeight();
            float scaleX = viewWidth / w;
            float scaleY = viewHeight / h;
            if (scaleX > scaleY) {
                bitmapHeight = viewHeight;
                bitmapWidth = (int)Math.ceil(w * scaleY);
            } else {
                bitmapWidth = viewWidth;
                bitmapHeight = (int)Math.ceil(h * scaleX);
            }
            bitmapX = (viewWidth - bitmapWidth) / 2;
            bitmapY = (viewHeight - bitmapHeight) / 2;

            if (rectX == -1 && rectY == -1) {
                if (bitmapWidth > bitmapHeight) {
                    rectY = bitmapY;
                    rectX = (viewWidth - bitmapHeight) / 2;
                    rectSize = bitmapHeight;
                } else {
                    rectX = bitmapX;
                    rectY = (viewHeight - bitmapWidth) / 2;
                    rectSize = bitmapWidth;
                }
            } else {
                rectX = percX * bitmapWidth + bitmapX;
                rectY = percY * bitmapHeight + bitmapY;
                rectSize = percSize * bitmapWidth;
            }
            invalidate();
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            super.onLayout(changed, left, top, right, bottom);
            viewWidth = right - left;
            viewHeight = bottom - top;
            updateBitmapSize();
        }

        public Bitmap getBitmap() {
            float percX = (rectX - bitmapX) / bitmapWidth;
            float percY = (rectY - bitmapY) / bitmapHeight;
            float percSize = rectSize / bitmapWidth;
            int x = (int)(percX * imageToCrop.getWidth());
            int y = (int)(percY * imageToCrop.getHeight());
            int size = (int)(percSize * imageToCrop.getWidth());
            if (x + size > imageToCrop.getWidth()) {
                size = imageToCrop.getWidth() - x;
            }
            if (y + size > imageToCrop.getHeight()) {
                size = imageToCrop.getHeight() - y;
            }
            try {
                return Bitmap.createBitmap(imageToCrop, x, y, size, size);
            } catch (Exception e) {
                FileLog.e("tmessags", e);
                System.gc();
                try {
                    return Bitmap.createBitmap(imageToCrop, x, y, size, size);
                } catch (Exception e2) {
                    FileLog.e("tmessages", e2);
                }
            }
            return null;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (drawable != null) {
                drawable.setBounds(bitmapX, bitmapY, bitmapX + bitmapWidth, bitmapY + bitmapHeight);
                drawable.draw(canvas);
            }
            canvas.drawRect(bitmapX, bitmapY, bitmapX + bitmapWidth, rectY, halfPaint);
            canvas.drawRect(bitmapX, rectY, rectX, rectY + rectSize, halfPaint);
            canvas.drawRect(rectX + rectSize, rectY, bitmapX + bitmapWidth, rectY + rectSize, halfPaint);
            canvas.drawRect(bitmapX, rectY + rectSize, bitmapX + bitmapWidth, bitmapY + bitmapHeight, halfPaint);

            canvas.drawRect(rectX, rectY, rectX + rectSize, rectY + rectSize, rectPaint);

            int side = AndroidUtilities.dp(7);
            canvas.drawRect(rectX - side, rectY - side, rectX + side, rectY + side, circlePaint);
            canvas.drawRect(rectX + rectSize - side, rectY - side, rectX + rectSize + side, rectY + side, circlePaint);
            canvas.drawRect(rectX - side, rectY + rectSize - side, rectX + side, rectY + rectSize + side, circlePaint);
            canvas.drawRect(rectX + rectSize - side, rectY + rectSize - side, rectX + rectSize + side, rectY + rectSize + side, circlePaint);
        }
    }

    private Bitmap imageToCrop;
    private BitmapDrawable drawable;
    private PhotoCropActivityDelegate delegate = null;
    private PhotoCropView view;
    private boolean sameBitmap = false;
    private boolean doneButtonPressed = false;

    public PhotoCropActivity(Bundle args) {
        super(args);
    }

    @Override
    public boolean onFragmentCreate() {
        swipeBackEnabled = false;
        String photoPath = getArguments().getString("photoPath");
        Uri photoUri = getArguments().getParcelable("photoUri");
        if (photoPath == null && photoUri == null) {
            return false;
        }
        if (photoPath != null) {
            File f = new File(photoPath);
            if (!f.exists()) {
                return false;
            }
        }
        int size = 0;
        if (AndroidUtilities.isTablet()) {
            size = AndroidUtilities.dp(520);
        } else {
            size = Math.max(AndroidUtilities.displaySize.x, AndroidUtilities.displaySize.y);
        }
        imageToCrop = ImageLoader.loadBitmap(photoPath, photoUri, size, size);
        if (imageToCrop == null) {
            return false;
        }
        drawable = new BitmapDrawable(imageToCrop);
        super.onFragmentCreate();
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        drawable = null;
        if (imageToCrop != null && !sameBitmap) {
            imageToCrop.recycle();
            imageToCrop = null;
        }
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
            View doneButton = actionBarLayer.findViewById(br.com.uatizapi.messenger.R.id.done_button);
            doneButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (delegate != null && !doneButtonPressed) {
                        Bitmap bitmap = view.getBitmap();
                        if (bitmap == imageToCrop) {
                            sameBitmap = true;
                        }
                        delegate.didFinishCrop(bitmap);
                        doneButtonPressed = true;
                    }
                    finishFragment();
                }
            });

            cancelButton.setText(LocaleController.getString("Cancel", br.com.uatizapi.messenger.R.string.Cancel).toUpperCase());
            TextView textView = (TextView)doneButton.findViewById(br.com.uatizapi.messenger.R.id.done_button_text);
            textView.setText(LocaleController.getString("Done", br.com.uatizapi.messenger.R.string.Done).toUpperCase());

            fragmentView = view = new PhotoCropView(getParentActivity());
            fragmentView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        } else {
            ViewGroup parent = (ViewGroup)fragmentView.getParent();
            if (parent != null) {
                parent.removeView(fragmentView);
            }
        }
        return fragmentView;
    }

    public void setDelegate(PhotoCropActivityDelegate delegate) {
        this.delegate = delegate;
    }
}
