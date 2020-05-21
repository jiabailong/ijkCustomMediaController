package com.midux.custommediacontroller;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

/**
 * 视频播放界面
 */
public class MainAcivity extends AppCompatActivity {
    private static final String TAG = "MainAcivity";

    private SurfaceView mSurfaceView;
     IjkMediaPlayer mMediaPlayer;

    private int mSurfaceWidth = 0;
    private int mSurfaceHeight = 0;

    private String mVideoPath = null;

    private long mLastUpdateStatTime = 0;
    private String is_over = "0";
    private int sv_height;//记录非全屏状态时，surfaceView的高度，以便退出全屏时，设置回来
    boolean firstRendering = true;//视频第一次渲染，在OnInfoListener 中改变值，保证恢复到先前播放的进度
    private MyController controller;
    private FrameLayout fl_surfaceview_parent;
    private boolean seekbarDrag = true;
    public static final String STREAM_URL_MP4_VOD_SHORT = "http://vfx.mtime.cn/Video/2019/03/19/mp4/190319212559089721.mp4";
    private boolean onComplet = false;//是否已经播放完一个视频
    //视频播放完成监听
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.module_activity_video_detail);
        super.onCreate(savedInstanceState);
        String CPU_ABI = android.os.Build.CPU_ABI;
Log.e("jbl",CPU_ABI);
        boolean isLiveStreaming = getIntent().getIntExtra("liveStreaming", 0) == 1;

        //播放地址
        mVideoPath = STREAM_URL_MP4_VOD_SHORT;

        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        fl_surfaceview_parent = (FrameLayout) findViewById(R.id.fl_surfaceview_parent);
        mSurfaceView.getHolder().addCallback(mCallback);
        mSurfaceWidth = getResources().getDisplayMetrics().widthPixels;
        mSurfaceHeight = getResources().getDisplayMetrics().heightPixels;


        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        controller = new MyController(this);

        ViewTreeObserver vto2 = mSurfaceView.getViewTreeObserver();
        vto2.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mSurfaceView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                sv_height = mSurfaceView.getHeight();
                mSurfaceView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, sv_height));
            }
        });

        mSurfaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                controller.show();
            }
        });
        findViewById(R.id.play).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                controller.show();
            }
        });
    }

    private boolean isPausing = false;//是否是暂停状态
    private boolean isFullScreen = false;//是否是全屏状态
    private MediaControllerInterface.MediaControl mPlayerControl = new MediaControllerInterface.MediaControl() {
        @Override
        public void VideoStart() {
            mMediaPlayer.start();
        }

        @Override
        public void VideoResume() {
            mMediaPlayer.start();
        }

        @Override
        public void VideoPause() {
            mMediaPlayer.pause();
        }

        @Override
        public void VideoStop() {
            mMediaPlayer.stop();
        }

        @Override
        public long getDuration() {
            return mMediaPlayer.getDuration();
        }

        @Override
        public long getCurrentPosition() {
            return mMediaPlayer.getCurrentPosition();
        }

        @Override
        public void seekTo(long var1) {
            mMediaPlayer.seekTo(var1);
        }

        @Override
        public boolean isPlaying() {
            return mMediaPlayer.isPlaying();
        }

        @Override
        public boolean isPausing() {
            return isPausing;
        }

        @Override
        public boolean isFullScreen() {
            return isFullScreen;
        }

        @Override
        public void toCollect() {
        }

        @Override
        public void actionForFullScreen() {
            if (isFullScreen) {
                //退出全屏
                Exit_full_screen();
            } else {
                To_full_screen();
            }
        }
    };

    //进入全屏的操作
    public void To_full_screen() {
        isFullScreen = true;
        fl_surfaceview_parent.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
        mSurfaceView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);//设置activity横屏
        getWindow().getDecorView().setSystemUiVisibility(View.INVISIBLE);//隐藏状态栏
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        Log.i("tag", "1--" + fl_surfaceview_parent.getMeasuredWidth() + "---" + fl_surfaceview_parent.getMeasuredHeight());
        controller.To_change_screen(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
    }

    //退出全屏的操作
    public void Exit_full_screen() {
        isFullScreen = false;
        fl_surfaceview_parent.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                sv_height));
        mSurfaceView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, sv_height));
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);//设置activity竖屏
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);//显示状态栏
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        Log.i("tag", "2--" + fl_surfaceview_parent.getWidth() + "---" + fl_surfaceview_parent.getHeight());
        controller.To_change_screen(FrameLayout.LayoutParams.MATCH_PARENT, sv_height);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        release();
        controller.release();
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.abandonAudioFocus(null);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }


    public void releaseWithoutStop() {
        if (mMediaPlayer != null) {
            mMediaPlayer.setDisplay(null);
        }
    }

    public void release() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    //播放准备
    private void prepare() {
        if (mMediaPlayer != null) {
            mMediaPlayer.setDisplay(mSurfaceView.getHolder());
            return;
        }

        try {
            if (mMediaPlayer == null) {
                mMediaPlayer = new IjkMediaPlayer();
            }
            addListener();
            mMediaPlayer.native_setLogLevel(IjkMediaPlayer.IJK_LOG_DEBUG);

//开启硬解码
            mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1);

            mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            mMediaPlayer.setDataSource(mVideoPath);
            mMediaPlayer.setDisplay(mSurfaceView.getHolder());
            mMediaPlayer.prepareAsync();
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void addListener(){
        if(mMediaPlayer!=null){
           mMediaPlayer.setOnBufferingUpdateListener(new IMediaPlayer.OnBufferingUpdateListener() {
               @Override
               public void onBufferingUpdate(IMediaPlayer iMediaPlayer, int i) {
                   long current = System.currentTimeMillis();
                   if (current - mLastUpdateStatTime > 3000) {
                       mLastUpdateStatTime = current;
                   }
               }
           });
           mMediaPlayer.setOnPreparedListener(new IMediaPlayer.OnPreparedListener() {
               @Override
               public void onPrepared(IMediaPlayer iMediaPlayer) {
                   controller.setControl(mPlayerControl);
                   controller.setAnchorView(fl_surfaceview_parent);
                   controller.setSeekBarEnabled(false);
                   mMediaPlayer.start();
               }
           });
            mMediaPlayer.setOnVideoSizeChangedListener(new IMediaPlayer.OnVideoSizeChangedListener() {
                @Override
                public void onVideoSizeChanged(IMediaPlayer iMediaPlayer, int width, int height, int i2, int i3) {
                    if (width != 0 && height != 0) {
                        float ratioW = (float) width / (float) mSurfaceWidth;
                        float ratioH = (float) height / (float) mSurfaceHeight;
                        float ratio = Math.max(ratioW, ratioH);
                        width = (int) Math.ceil((float) width / ratio);
                        height = (int) Math.ceil((float) height / ratio);
                        FrameLayout.LayoutParams layout = new FrameLayout.LayoutParams(width, height);
                        layout.gravity = Gravity.CENTER;
                        mSurfaceView.setLayoutParams(layout);

                    }
                }});

            mMediaPlayer.setOnControlMessageListener(new IjkMediaPlayer.OnControlMessageListener() {
                @Override
                public String onControlResolveSegmentUrl(int i) {
                    return null;
                }
            });

            mMediaPlayer.setOnCompletionListener(new IMediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(IMediaPlayer iMediaPlayer) {
                    controller.isCompleted();
                }
            });
            mMediaPlayer.setOnErrorListener(new IMediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(IMediaPlayer iMediaPlayer, int errorCode, int i1) {
                    switch (errorCode) {
                        case IjkMediaPlayer.MEDIA_ERROR_IO://网络异常
                            /**
                             * SDK will do reconnecting automatically
                             */
                            //如果网络异常播放失败，尝试重新连接
                            reload();
                            return false;
                        default://未知错误
                            break;
                    }
                    return true;
                }
            });

        }
    }

    //surfaceview回调函数
    private SurfaceHolder.Callback mCallback = new SurfaceHolder.Callback() {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            prepare();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            // release();
            releaseWithoutStop();
        }
    };






    /**
     * 转换播放时间
     *
     * @param milliseconds 传入毫秒值
     * @return 返回 hh:mm:ss或mm:ss格式的数据
     */
    @SuppressLint("SimpleDateFormat")
    public String getShowTime(long milliseconds) {
        // 获取日历函数
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliseconds);
        SimpleDateFormat dateFormat = null;
        // 判断是否大于60分钟，如果大于就显示小时。设置日期格式
        if (milliseconds / 60000 > 60) {
            dateFormat = new SimpleDateFormat("hh:mm:ss");
        } else {
            dateFormat = new SimpleDateFormat("mm:ss");
        }
        return dateFormat.format(calendar.getTime());
    }

    //视频播放状态监听









    //如果网络异常播放失败，尝试重新连接
    public void reload() {
        release();//先关闭先前的播放器
        //判断网络连接
        prepare();
    }
}
