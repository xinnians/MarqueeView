package com.sunfusheng.marqueeview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.os.Build;
import android.support.annotation.AnimRes;
import android.support.annotation.FontRes;
import android.support.annotation.NonNull;
import android.support.v4.content.res.ResourcesCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.ViewFlipper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sunfusheng on 16/5/31.
 */
public class MarqueeView<T> extends ViewFlipper {

    private int interval = 3000;
    private boolean hasSetAnimDuration = false;
    private int animDuration = 1000;
    private int textSize = 14;
    private int textColor = 0xff000000;
    private boolean singleLine = false;

    private int gravity = Gravity.LEFT | Gravity.CENTER_VERTICAL;
    private static final int GRAVITY_LEFT = 0;
    private static final int GRAVITY_CENTER = 1;
    private static final int GRAVITY_RIGHT = 2;

    private int direction = DIRECTION_BOTTOM_TO_TOP;
    private static final int DIRECTION_BOTTOM_TO_TOP = 0;
    private static final int DIRECTION_TOP_TO_BOTTOM = 1;
    private static final int DIRECTION_RIGHT_TO_LEFT = 2;
    private static final int DIRECTION_LEFT_TO_RIGHT = 3;

    private Typeface typeface;

    @AnimRes
    private int inAnimResId = R.anim.anim_bottom_in;
    @AnimRes
    private int outAnimResId = R.anim.anim_top_out;

    private int mPosition = 0;
    private List<T> messages = new ArrayList<>();
    private OnItemClickListener onItemClickListener;

    private boolean isNeedStop = false;

    public MarqueeView(Context context) {
        this(context, null);
    }

    public MarqueeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray typedArray =
                context.obtainStyledAttributes(attrs, R.styleable.MarqueeViewStyle, defStyleAttr, 0);

        interval = typedArray.getInteger(R.styleable.MarqueeViewStyle_mvInterval, interval);
        hasSetAnimDuration = typedArray.hasValue(R.styleable.MarqueeViewStyle_mvAnimDuration);
        animDuration = typedArray.getInteger(R.styleable.MarqueeViewStyle_mvAnimDuration, animDuration);
        singleLine = typedArray.getBoolean(R.styleable.MarqueeViewStyle_mvSingleLine, false);
        if (typedArray.hasValue(R.styleable.MarqueeViewStyle_mvTextSize)) {
            textSize = (int) typedArray.getDimension(R.styleable.MarqueeViewStyle_mvTextSize, textSize);
            textSize = Utils.px2sp(context, textSize);
        }
        textColor = typedArray.getColor(R.styleable.MarqueeViewStyle_mvTextColor, textColor);
        @FontRes int fontRes = typedArray.getResourceId(R.styleable.MarqueeViewStyle_mvFont, 0);
        if (fontRes != 0) {
            typeface = ResourcesCompat.getFont(context, fontRes);
        }
        int gravityType = typedArray.getInt(R.styleable.MarqueeViewStyle_mvGravity, GRAVITY_LEFT);
        switch (gravityType) {
            case GRAVITY_LEFT:
                gravity = Gravity.LEFT | Gravity.CENTER_VERTICAL;
                break;
            case GRAVITY_CENTER:
                gravity = Gravity.CENTER;
                break;
            case GRAVITY_RIGHT:
                gravity = Gravity.RIGHT | Gravity.CENTER_VERTICAL;
                break;
        }

        if (typedArray.hasValue(R.styleable.MarqueeViewStyle_mvDirection)) {
            direction = typedArray.getInt(R.styleable.MarqueeViewStyle_mvDirection, direction);
            switch (direction) {
                case DIRECTION_BOTTOM_TO_TOP:
                    inAnimResId = R.anim.anim_bottom_in;
                    outAnimResId = R.anim.anim_top_out;
                    break;
                case DIRECTION_TOP_TO_BOTTOM:
                    inAnimResId = R.anim.anim_top_in;
                    outAnimResId = R.anim.anim_bottom_out;
                    break;
                case DIRECTION_RIGHT_TO_LEFT:
                    inAnimResId = R.anim.anim_right_in;
                    outAnimResId = R.anim.anim_left_out;
                    break;
                case DIRECTION_LEFT_TO_RIGHT:
                    inAnimResId = R.anim.anim_left_in;
                    outAnimResId = R.anim.anim_right_out;
                    break;
            }
        } else {
            inAnimResId = R.anim.anim_bottom_in;
            outAnimResId = R.anim.anim_top_out;
        }

        typedArray.recycle();
        setFlipInterval(interval);
    }

    /**
     * 根据字符串，启动翻页公告
     *
     * @param message 字符串
     */
    public void startWithText(String message) {
        startWithText(message, inAnimResId, outAnimResId);
    }

    /**
     * 根据字符串，启动翻页公告
     *
     * @param message      字符串
     * @param inAnimResId  进入动画的resID
     * @param outAnimResID 离开动画的resID
     */
    public void startWithText(final String message, final @AnimRes int inAnimResId,
                              final @AnimRes int outAnimResID) {
        if (TextUtils.isEmpty(message)) return;
        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
                startWithFixedWidth(message, inAnimResId, outAnimResID);
            }
        });
    }

    /**
     * 根据字符串和宽度，启动翻页公告
     *
     * @param message 字符串
     */
    private void startWithFixedWidth(String message, @AnimRes int inAnimResId,
                                     @AnimRes int outAnimResID) {
        int messageLength = message.length();
        int width = Utils.px2dip(getContext(), getWidth());
        if (width == 0) {
            throw new RuntimeException("Please set the width of MarqueeView !");
        }
        int limit = width / textSize;
        List list = new ArrayList();

        if (messageLength <= limit) {
            list.add(message);
        } else {
            int size = messageLength / limit + (messageLength % limit != 0 ? 1 : 0);
            for (int i = 0; i < size; i++) {
                int startIndex = i * limit;
                int endIndex = ((i + 1) * limit >= messageLength ? messageLength : (i + 1) * limit);
                list.add(message.substring(startIndex, endIndex));
            }
        }

        if (messages == null) {
            messages = new ArrayList<>();
        }
        messages.clear();
        messages.addAll(list);
        postStart(inAnimResId, outAnimResID);
    }

    /**
     * 根据字符串列表，启动翻页公告
     *
     * @param messages 字符串列表
     */
    public void startWithList(List<T> messages) {
        startWithList(messages, inAnimResId, outAnimResId);
    }

    /**
     * 根据字符串列表，启动翻页公告
     *
     * @param messages     字符串列表
     * @param inAnimResId  进入动画的resID
     * @param outAnimResID 离开动画的resID
     */
    public void startWithList(List<T> messages, @AnimRes int inAnimResId, @AnimRes int outAnimResID) {
        if (Utils.isEmpty(messages)) return;
        setMessages(messages);
        postStart(inAnimResId, outAnimResID);
    }

    private void postStart(final @AnimRes int inAnimResId, final @AnimRes int outAnimResID) {
        post(new Runnable() {
            @Override
            public void run() {
                start(inAnimResId, outAnimResID);
            }
        });
    }

    private boolean isAnimStart = false;

    private void start(final @AnimRes int inAnimResId, final @AnimRes int outAnimResID) {
        removeAllViews();
        clearAnimation();
        // 检测数据源
        if (messages == null || messages.isEmpty()) {
            throw new RuntimeException("The messages cannot be empty!");
        }
        int index = 0;
        addView(createTextView(messages.get(index), index));

        if (messages.size() > 1) {
            setInAndOutAnimation(inAnimResId, outAnimResID);
//            startFlipping();
            for (int i = 1; i < messages.size(); i++) {
                index++;
                View view = createTextView(messages.get(index), index);
                if (view.getParent() == null) {
//                    Log.e("Ian", "view.getParent() == null i:" + i + "messages.size():" + messages.size());
                    addView(view);
                }
            }
        }

//        if (getInAnimation() != null) {
//            getInAnimation().setAnimationListener(new Animation.AnimationListener() {
//                @Override
//                public void onAnimationStart(Animation animation) {
//                    Log.e("Ian","[onAnimationStart] call.");
//                    if (isAnimStart) {
//                        animation.cancel();
//                    }
//                    isAnimStart = true;
//                }
//
//                @Override
//                public void onAnimationEnd(Animation animation) {
//                    position++;
//                    if (position >= messages.size()) {
//                        position = 0;
//                    }
//                    Log.e("Ian","[onAnimationEnd] call. position:"+position+", messages.size():"+messages.size());
//                    View view = createTextView(messages.get(position));
//                    if (view.getParent() == null) {
//                        Log.e("Ian","view.getParent() == null");
//                        addView(view);
//                    }
//                    isAnimStart = false;
//                }
//
//                @Override
//                public void onAnimationRepeat(Animation animation) {
//                }
//            });
//        }
    }

    private MarqueeTextView createTextView(T marqueeItem, int index) {
        MarqueeTextView textView = (MarqueeTextView) getChildAt(index);
        if (textView == null) {
            textView = new MarqueeTextView(getContext());
            textView.setGravity(gravity | Gravity.CENTER_VERTICAL);
            textView.setTextColor(textColor);
            textView.setTextSize(textSize);
            if (typeface != null) {
                textView.setTypeface(typeface);
            }
            textView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onItemClickListener != null) {
                        onItemClickListener.onItemClick(getPosition(), (TextView) v);
                    }
                }
            });
            textView.setOnMarqueeCompleteListener(new MarqueeTextView.OnMarqueeCompleteListener() {
                @Override
                public void onMarqueeComplete(int position) {
//                    Log.e("Ian", "[onMarqueeComplete] call. position:" + position);
                    if(mPosition == position){
                        showNext();
                    }
                }

                @Override
                public void notMarquee(int position) {
//                    Log.e("Ian", "[notMarquee] position:" + position);
                    if(mPosition == position){
                        showNext();
                    }
                }
            });
        }
        CharSequence message = "";
        if (marqueeItem instanceof CharSequence) {
            message = (CharSequence) marqueeItem;
        } else if (marqueeItem instanceof IMarqueeItem) {
            message = ((IMarqueeItem) marqueeItem).marqueeMessage();
        }
        textView.setText(message);
        textView.setTag(index);
        return textView;
    }

    public int getPosition() {
        return (int) getCurrentView().getTag();
    }

    public List<T> getMessages() {
        return messages;
    }

    public void setMessages(List<T> messages) {
        this.messages = messages;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public interface OnItemClickListener {
        void onItemClick(int position, TextView textView);
    }

    /**
     * 设置进入动画和离开动画
     *
     * @param inAnimResId  进入动画的resID
     * @param outAnimResID 离开动画的resID
     */
    private void setInAndOutAnimation(@AnimRes int inAnimResId, @AnimRes int outAnimResID) {
        Animation inAnim = AnimationUtils.loadAnimation(getContext(), inAnimResId);
        if (hasSetAnimDuration) inAnim.setDuration(animDuration);
        setInAnimation(inAnim);

        Animation outAnim = AnimationUtils.loadAnimation(getContext(), outAnimResID);
        if (hasSetAnimDuration) outAnim.setDuration(animDuration);
        setOutAnimation(outAnim);
    }

    public void setTypeface(Typeface typeface) {
        this.typeface = typeface;
    }

    public void showNext() {
        if(!isNeedStop){
            mPosition++;
            if (mPosition >= messages.size()) {
                mPosition = 0;
            }
            super.showNext();
        }
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
//        Log.e("Ian","[onVisibilityChanged] visibility:"+visibility);
        if(visibility == View.VISIBLE){
            isNeedStop = false;
        }else{
            isNeedStop = true;
        }
    }
}
