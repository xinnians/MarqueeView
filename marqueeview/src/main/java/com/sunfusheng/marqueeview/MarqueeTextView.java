package com.sunfusheng.marqueeview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewDebug;

public class MarqueeTextView extends android.support.v7.widget.AppCompatTextView implements Runnable {


    private static final int MARQUEE_DELAY = 1200;
    private static final int MARQUEE_RESTART_DELAY = 10;

    private int currentScrollX;
    private boolean isStop = false;
    private int textWidth;
    private boolean isMeasure = false;
    private int mMarqueeRepeatLimit = 1;
    private int mMarqueeVelocity = 3;

    private OnMarqueeCompleteListener marqueeCompleteListener;

    public MarqueeTextView(Context context) {
        this(context, null);
        init();
    }

    public MarqueeTextView(Context context, AttributeSet attrs) {
        this(context, attrs,0);
        init();
    }

    public MarqueeTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public void init(){
        setEllipsize(TextUtils.TruncateAt.MARQUEE);
        setSingleLine(true);
        setSelected(true);
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    public interface OnMarqueeCompleteListener {
        void onMarqueeComplete(int position);
        void notMarquee(int position);
    }

    public OnMarqueeCompleteListener getMarqueeCompleteListener() {
        return marqueeCompleteListener;
    }

    public void setOnMarqueeCompleteListener(
            OnMarqueeCompleteListener marqueeCompleteListener) {
        this.marqueeCompleteListener = marqueeCompleteListener;
    }

    /**
     * 獲取文字滾動的速度，每秒移動的像素
     * @return
     */
    public int getMarqueeVelocity() {
        return mMarqueeVelocity;
    }

    /**
     * 設置文字的滾動的速度
     * @param velocity 每秒移動的像素
     */
    public void setMarqueeVelocity(int velocity) {
        this.mMarqueeVelocity = velocity ;
    }

    @Override
    @ViewDebug.ExportedProperty(category = "focus")
    public boolean isFocused() {
        return false;
    }

    @Override
    @ViewDebug.ExportedProperty
    public boolean isSelected() {
        return false;
    }

    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);
        startStopMarquee(selected);
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction,
                                  Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        startStopMarquee(focused);
    }

    public void startStopMarquee(boolean bool){
        if(bool){
            startScroll();
        }else{
            stopScroll();
        }
    }

//    public void setText(String text) {
//        setText(text);
//    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!isMeasure) {
            textWidth = getTextWidth();
            isMeasure = true;
        }
    }

    /**
     * 獲取文字寬度
     */
    private int getTextWidth() {
        Paint paint = this.getPaint();
        String str = this.getText().toString();
        return (int) paint.measureText(str);
    }

    @Override
    public void run() {
        if(textWidth <= getWidth()){
            if(null != marqueeCompleteListener){
                marqueeCompleteListener.notMarquee((Integer) getTag());
            }
            return;
        }

        if (isStop) {
            currentScrollX = 0;
            scrollTo(currentScrollX, 0);
            return;
        }

        currentScrollX += mMarqueeVelocity;
        scrollTo(currentScrollX, 0);


        if (textWidth != 0  && getScrollX() >= textWidth) {
            mMarqueeRepeatLimit--;
            if(mMarqueeRepeatLimit <= 0){
                Log.i("jack", "滾到底了");
                if(null != marqueeCompleteListener){
                    marqueeCompleteListener.onMarqueeComplete((Integer) getTag());
                }
                return;
            }
            currentScrollX = -getWidth();
        }

        postDelayed(this, MARQUEE_RESTART_DELAY);
    }

    /**
     *  開始滾動
     */
    private void startScroll() {
        isStop = false;
        this.removeCallbacks(this);
        this.invalidate();
        currentScrollX = 0;
        postDelayed(this, MARQUEE_DELAY);
    }

    /**
     *  停止滾動
     */
    private void stopScroll() {
        isStop = true;
    }

    /**
     *  從頭開始滾動
     */
    public void startFor0() {
        currentScrollX = 0;
        startScroll();
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
//        Log.e("Ian","[onVisibilityChanged] position:"+getTag()+", visibility:"+visibility);
        if(visibility == View.VISIBLE){
            startFor0();
        }
    }
}

