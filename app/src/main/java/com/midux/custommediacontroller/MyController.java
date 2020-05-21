package com.midux.custommediacontroller;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Locale;

/**
 * Created by yu_midux on 2018/3/21.
 */

public class MyController extends FrameLayout implements MediaControllerInterface {

    private LinearLayout ll_top;
    private ImageView iv_collect;
    private ImageView iv_center_play;
    private LinearLayout ll_bottom;
    private ImageView iv_play;
    private TextView tv_watched;
    private SeekBar sb_progress;
    private TextView tv_total;
    private ImageView iv_fullscreen;
    private MediaControllerInterface.MediaControl mPlayer;
    private static final int FADE_OUT = 1;
    private static final int SHOW_PROGRESS = 2;
    private static int sDefaultTimeout = 3000;
    private boolean mShowing = false;
    private long mDuration;
    private Context mContext;
    private ViewGroup mAnchorVGroup;
    private long SEEKBAR_MAX = 1000L;
    private View controllerView;

    public MyController(@NonNull Context context) {
        this(context, null);
    }

    public MyController(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MyController(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
    }

    //初始化
    private View initView() {
        View view = LayoutInflater.from(mContext).inflate(R.layout.view_controller, null);

        ll_top = (LinearLayout) view.findViewById(R.id.ll_top);
        iv_collect = (ImageView) view.findViewById(R.id.iv_collect);
        iv_center_play = (ImageView) view.findViewById(R.id.iv_center_play);
        ll_bottom = (LinearLayout) view.findViewById(R.id.ll_bottom);
        iv_play = (ImageView) view.findViewById(R.id.iv_play);
        tv_watched = (TextView) view.findViewById(R.id.tv_watched);
        sb_progress = (SeekBar) view.findViewById(R.id.sb_progress);
        tv_total = (TextView) view.findViewById(R.id.tv_total);
        iv_fullscreen = (ImageView) view.findViewById(R.id.iv_fullscreen);

        iv_collect.setOnClickListener(mCollectListener);//收藏监听
        iv_center_play.setOnClickListener(mPlayListener);//播放按钮的监听
        iv_play.setOnClickListener(mPlayListener);//播放按钮的监听
        iv_fullscreen.setOnClickListener(mFullListener);//全屏按钮的监听

        sb_progress.setMax((int) SEEKBAR_MAX);
        //进度条拖动的监听
        sb_progress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mPlayer.seekTo(mDuration * seekBar.getProgress() / SEEKBAR_MAX);
            }
        });
        return view;
    }

    //当触摸事件处于控制器上的时候，显示控制器，不让其消失
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        show(sDefaultTimeout);
        return false;
    }

    //当触摸事件处于控制器上的时候，显示控制器，不让其消失
    @Override
    public boolean onTrackballEvent(MotionEvent ev) {
        show(sDefaultTimeout);
        return false;
    }

    //当触摸事件处于控制器上的时候，显示控制器，不让其消失
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (event.getRepeatCount() == 0
                && (keyCode == KeyEvent.KEYCODE_HEADSETHOOK
                || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keyCode == KeyEvent.KEYCODE_SPACE)) {
            show(sDefaultTimeout);
            if (iv_play != null)
                iv_play.requestFocus();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP) {
            if (mPlayer.isPlaying()) {
                mPlayer.VideoPause();
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_BACK
                || keyCode == KeyEvent.KEYCODE_MENU) {
            hide();
            return true;
        } else {
            show(sDefaultTimeout);
        }
        return super.dispatchKeyEvent(event);
    }

    //收藏的监听
    private OnClickListener mCollectListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            mPlayer.toCollect();
        }
    };

    //全屏按钮监听
    private OnClickListener mFullListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            mPlayer.actionForFullScreen();
        }
    };

    //播放按钮的监听
    private OnClickListener mPlayListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            if (mPlayer.isPlaying()) {//正在播放
                iv_play.setImageResource(R.mipmap.k_play);
                showCneter();
                mPlayer.VideoPause();
            } else {
                iv_play.setImageResource(R.mipmap.k_stop);
                hideCenter();//隐藏中心
                mPlayer.VideoStart();
            }
        }
    };

    //添加控制绑定
    public void setControl(MediaControllerInterface.MediaControl mPlayer) {
        this.mPlayer = mPlayer;
    }

    public void setAnchorView(ViewGroup view) {
        mAnchorVGroup = view;
        FrameLayout.LayoutParams frameParams = new FrameLayout.LayoutParams(
                mAnchorVGroup.getWidth(),
                mAnchorVGroup.getHeight()
        );
        removeAllViews();
        frameParams.gravity=Gravity.BOTTOM;
        controllerView = initView();
        mAnchorVGroup.addView(controllerView, frameParams);
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            long progress;
            switch (msg.what) {
                case FADE_OUT:
                    hide();
                    break;
                case SHOW_PROGRESS:
                    if (mPlayer.isPlaying()) {
                        progress = setProgress();
                    } else {
                        return;
                    }

                    if (mShowing && mPlayer.isPlaying()) {
                        msg = obtainMessage(SHOW_PROGRESS);
                        sendMessageDelayed(msg, SEEKBAR_MAX - (progress % SEEKBAR_MAX));
                    }
                    break;
            }
        }
    };

    private long setProgress() {
        if (mPlayer == null) {
            return 0;
        }

        long position = mPlayer.getCurrentPosition();
        long duration = mPlayer.getDuration();
        if (sb_progress != null) {
            if (duration > 0) {
                long pos = SEEKBAR_MAX * position / duration;
                sb_progress.setProgress((int) pos);
            }
//            int percent = mPlayer.getBufferPercentage();//得到当前缓存进度
//            sb_progress.setSecondaryProgress(percent * 10);//设置缓存进度
        }

        mDuration = duration;

        if (tv_total != null)
            tv_total.setText(generateTime(mDuration));
        if (tv_watched != null)
            tv_watched.setText(generateTime(position));

        return position;
    }

    //long数据格式化
    private static String generateTime(long position) {
        int totalSeconds = (int) (position / 1000);

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        if (hours > 0) {
            return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes,
                    seconds).toString();
        } else {
            return String.format(Locale.US, "%02d:%02d", minutes, seconds)
                    .toString();
        }
    }

    @Override
    public void show() {
        show(sDefaultTimeout);
    }

    //显示控制器
    @Override
    public void show(int time) {
        if (!mShowing && mAnchorVGroup != null) {
            if (mPlayer.isFullScreen()) {//是全屏，显示上下
                showTopAndBottom();
            } else {//显示底部
                showBottom();
            }
        }

        if (mPlayer.isPlaying()) {
            iv_play.setImageResource(R.mipmap.k_stop);
        } else {
            iv_play.setImageResource(R.mipmap.k_play);
        }

        mHandler.sendEmptyMessage(SHOW_PROGRESS);

        mShowing = true;
        if (time != 0) {
            mHandler.removeMessages(FADE_OUT);
            mHandler.sendMessageDelayed(mHandler.obtainMessage(FADE_OUT),
                    time);
        }
    }

    //隐藏控制器
    @Override
    public void hide() {
        if (mAnchorVGroup == null) {
            return;
        }
        try {
            if (mPlayer.isPausing()) {//正处于暂停状态，隐藏上下，中间按钮不隐藏
                hideAll();
            } else {
                hideTopAndBottom();
            }
            if (mHandler != null) {
                mHandler.removeMessages(SHOW_PROGRESS);
            }
        } catch (IllegalArgumentException ex) {
            Log.w("MediaController", "already removed");
        }
        mShowing = false;
    }

    //控制器是否正显示
    @Override
    public boolean isShowing() {
        return ll_bottom.getVisibility() == VISIBLE ? true : false;
    }

    @Override
    public void setSeekBarEnabled(boolean enabled) {
        sb_progress.setEnabled(enabled);
    }

    @Override
    public void isCompleted() {
        long dutaction = mPlayer.getDuration();
        tv_watched.setText(generateTime(dutaction));
        sb_progress.setProgress((int) SEEKBAR_MAX);
        tv_total.setText(generateTime(dutaction));
    }

    @Override
    public void To_change_screen(int w, int h) {
        FrameLayout.LayoutParams frameParams = new FrameLayout.LayoutParams(
                w,
                h
        );
        controllerView.setLayoutParams(frameParams);
    }

    @Override
    public void setCollected(boolean isCollected) {
        if (isCollected) {
            iv_collect.setImageResource(R.mipmap.video_detail_collect);
        } else {
            iv_collect.setImageResource(R.mipmap.video_detail_collected);

        }
    }

    //显示底部
    private void showBottom() {
        ll_bottom.setVisibility(VISIBLE);
    }

    //显示上下部分
    private void showTopAndBottom() {
        ll_top.setVisibility(VISIBLE);
        ll_bottom.setVisibility(VISIBLE);
    }

    //显示中间
    private void showCneter() {
        iv_center_play.setVisibility(VISIBLE);
    }

    //全部隐藏
    private void hideAll() {
        ll_top.setVisibility(GONE);
        ll_bottom.setVisibility(GONE);
        iv_center_play.setVisibility(GONE);
        mShowing = false;
    }

    //隐藏上下
    private void hideTopAndBottom() {
        if (ll_top.getVisibility() == VISIBLE) {
            ll_top.setVisibility(GONE);
        }
        ll_bottom.setVisibility(GONE);
    }

    //隐藏中心
    private void hideCenter() {
        iv_center_play.setVisibility(GONE);
    }

    //清空
    public void release() {
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
    }
}
