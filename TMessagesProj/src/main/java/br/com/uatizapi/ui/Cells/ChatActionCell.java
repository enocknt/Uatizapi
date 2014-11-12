/*
 * This is the source code of Telegram for Android v. 1.7.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2014.
 */

package br.com.uatizapi.ui.Cells;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.Spannable;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.style.URLSpan;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;

import br.com.uatizapi.android.AndroidUtilities;
import br.com.uatizapi.android.ImageReceiver;
import br.com.uatizapi.android.MessageObject;
import br.com.uatizapi.android.MessagesController;
import br.com.uatizapi.android.PhotoObject;
import br.com.uatizapi.messenger.FileLog;
import br.com.uatizapi.messenger.TLRPC;
import br.com.uatizapi.messenger.UserConfig;
import br.com.uatizapi.ui.PhotoViewer;

public class ChatActionCell extends BaseCell {

    public static interface ChatActionCellDelegate {
        public abstract void didClickedImage(ChatActionCell cell);
        public abstract void didLongPressed(ChatActionCell cell);
        public abstract void needOpenUserProfile(int uid);
    }

    private static Drawable backgroundBlack;
    private static Drawable backgroundBlue;
    private static TextPaint textPaint;

    private URLSpan pressedLink;

    private ImageReceiver imageReceiver;
    private StaticLayout textLayout;
    private int textWidth = 0;
    private int textHeight = 0;
    private int textX = 0;
    private int textY = 0;
    private int textXLeft = 0;
    private boolean useBlackBackground = false;
    private int previousWidth = 0;
    private boolean imagePressed = false;

    private MessageObject currentMessageObject;

    private ChatActionCellDelegate delegate;

    public ChatActionCell(Context context) {
        super(context);
        if (backgroundBlack == null) {
            backgroundBlack = getResources().getDrawable(br.com.uatizapi.messenger.R.drawable.system_black);
            backgroundBlue = getResources().getDrawable(br.com.uatizapi.messenger.R.drawable.system_blue);

            textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setColor(0xffffffff);
            textPaint.linkColor = 0xffffffff;
        }
        imageReceiver = new ImageReceiver(this);
        textPaint.setTextSize(AndroidUtilities.dp(MessagesController.getInstance().fontSize));
    }

    public void setDelegate(ChatActionCellDelegate delegate) {
        this.delegate = delegate;
    }

    public void setMessageObject(MessageObject messageObject) {
        if (currentMessageObject == messageObject) {
            return;
        }
        currentMessageObject = messageObject;
        previousWidth = 0;
        if (currentMessageObject.type == 11) {
            int id = 0;
            if (messageObject.messageOwner.to_id != null) {
                if (messageObject.messageOwner.to_id.chat_id != 0) {
                    id = messageObject.messageOwner.to_id.chat_id;
                } else {
                    id = messageObject.messageOwner.to_id.user_id;
                    if (id == UserConfig.getClientUserId()) {
                        id = messageObject.messageOwner.from_id;
                    }
                }
            }
            if (currentMessageObject.messageOwner.action instanceof TLRPC.TL_messageActionUserUpdatedPhoto) {
                imageReceiver.setImage(currentMessageObject.messageOwner.action.newUserPhoto.photo_small, "50_50", getResources().getDrawable(AndroidUtilities.getUserAvatarForId(id)), false);
            } else {
                PhotoObject photo = PhotoObject.getClosestImageWithSize(currentMessageObject.photoThumbs, AndroidUtilities.dp(64));
                if (photo != null) {
                    if (photo.image != null) {
                        imageReceiver.setImageBitmap(photo.image);
                    } else {
                        imageReceiver.setImage(photo.photoOwner.location, "50_50", getResources().getDrawable(AndroidUtilities.getGroupAvatarForId(id)), false);
                    }
                } else {
                    imageReceiver.setImageBitmap(getResources().getDrawable(AndroidUtilities.getGroupAvatarForId(id)));
                }
            }
            imageReceiver.setVisible(!PhotoViewer.getInstance().isShowingImage(currentMessageObject), false);
        } else {
            imageReceiver.setImageBitmap((Bitmap)null);
        }
        requestLayout();
    }

    public void setUseBlackBackground(boolean value) {
        useBlackBackground = value;
    }

    public MessageObject getMessageObject() {
        return currentMessageObject;
    }

    public ImageReceiver getPhotoImage() {
        return imageReceiver;
    }

    @Override
    protected void onLongPress() {
        if (delegate != null) {
            delegate.didLongPressed(this);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        boolean result = false;
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (delegate != null) {
                if (currentMessageObject.type == 11 && imageReceiver.isInsideImage(x, y)) {
                    imagePressed = true;
                    result = true;
                }
                if (result) {
                    startCheckLongPress();
                }
            }
        } else {
            if (event.getAction() != MotionEvent.ACTION_MOVE) {
                cancelCheckLongPress();
            }
            if (imagePressed) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    imagePressed = false;
                    if (delegate != null) {
                        delegate.didClickedImage(this);
                        playSoundEffect(SoundEffectConstants.CLICK);
                    }
                } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                    imagePressed = false;
                } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    if (!imageReceiver.isInsideImage(x, y)) {
                        imagePressed = false;
                    }
                }
            }
        }
        if (!result) {
            if (event.getAction() == MotionEvent.ACTION_DOWN || pressedLink != null && event.getAction() == MotionEvent.ACTION_UP) {
                if (x >= textX && y >= textY && x <= textX + textWidth && y <= textY + textHeight) {
                    y -= textY;
                    x -= textXLeft;

                    final int line = textLayout.getLineForVertical((int)y);
                    final int off = textLayout.getOffsetForHorizontal(line, x);
                    final float left = textLayout.getLineLeft(line);
                    if (left <= x && left + textLayout.getLineWidth(line) >= x) {
                        Spannable buffer = (Spannable)currentMessageObject.messageText;
                        URLSpan[] link = buffer.getSpans(off, off, URLSpan.class);

                        if (link.length != 0) {
                            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                                pressedLink = link[0];
                                result = true;
                            } else {
                                if (link[0] == pressedLink) {
                                    if (delegate != null) {
                                        delegate.needOpenUserProfile(Integer.parseInt(link[0].getURL()));
                                    }
                                    result = true;
                                }
                            }
                        } else {
                            pressedLink = null;
                        }
                    } else {
                        pressedLink = null;
                    }
                } else {
                    pressedLink = null;
                }
            }
        }

        if (!result) {
            result = super.onTouchEvent(event);
        }

        return result;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (currentMessageObject == null) {
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), textHeight + AndroidUtilities.dp(14));
            return;
        }
        int width = MeasureSpec.getSize(widthMeasureSpec);
        if (width != previousWidth) {
            previousWidth = width;

            textLayout = new StaticLayout(currentMessageObject.messageText, textPaint, width - AndroidUtilities.dp(30), Layout.Alignment.ALIGN_CENTER, 1.0f, 0.0f, false);
            textHeight = 0;
            textWidth = 0;
            try {
                int linesCount = textLayout.getLineCount();
                for (int a = 0; a < linesCount; a++) {
                    float lineWidth = 0;
                    float lineLeft = 0;
                    try {
                        lineWidth = textLayout.getLineWidth(a);
                        textHeight = (int)Math.max(textHeight, Math.ceil(textLayout.getLineBottom(a)));
                    } catch (Exception e) {
                        FileLog.e("tmessages", e);
                        return;
                    }
                    textWidth = (int)Math.max(textWidth, Math.ceil(lineWidth));
                }
            } catch (Exception e) {
                FileLog.e("tmessages", e);
            }

            textX = (width - textWidth) / 2;
            textY = AndroidUtilities.dp(7);
            textXLeft = (width - textLayout.getWidth()) / 2;

            if (currentMessageObject.type == 11) {
                imageReceiver.setImageCoords((width - AndroidUtilities.dp(64)) / 2, textHeight + AndroidUtilities.dp(15), AndroidUtilities.dp(64), AndroidUtilities.dp(64));
            }
        }
        setMeasuredDimension(width, textHeight + AndroidUtilities.dp(14 + (currentMessageObject.type == 11 ? 70 : 0)));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (currentMessageObject == null) {
            return;
        }

        Drawable backgroundDrawable = null;
        if (useBlackBackground) {
            backgroundDrawable = backgroundBlack;
        } else {
            backgroundDrawable = backgroundBlue;
        }
        backgroundDrawable.setBounds(textX - AndroidUtilities.dp(5), AndroidUtilities.dp(5), textX + textWidth + AndroidUtilities.dp(5), AndroidUtilities.dp(9) + textHeight);
        backgroundDrawable.draw(canvas);

        if (currentMessageObject.type == 11) {
            imageReceiver.draw(canvas);
        }

        if (textLayout != null) {
            canvas.save();
            canvas.translate(textXLeft, textY);
            textLayout.draw(canvas);
            canvas.restore();
        }
    }
}
