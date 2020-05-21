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
public class TVBasePlayerAcivity extends AppCompatActivity {
    private static final String TAG = "TVBasePlayerAcivity";

    private SurfaceView mSurfaceView;
    IjkMediaPlayer mMediaPlayer;

    private int mSurfaceWidth = 0;
    private int mSurfaceHeight = 0;

    private String mVideoPath = null;

    private long mLastUpdateStatTime = 0;
    private String is_over = "0";
    private int sv_height=400;//记录非全屏状态时，surfaceView的高度，以便退出全屏时，设置回来
    boolean firstRendering = true;//视频第一次渲染，在OnInfoListener 中改变值，保证恢复到先前播放的进度
    private VideoController controller;
    private AspectFrameLayout fl_surfaceview_parent;
    private boolean seekbarDrag = true;
    public static final String STREAM_URL_MP4_VOD_SHORT = "http://vfx.mtime.cn/Video/2019/03/19/mp4/190319212559089721.mp4";
    private boolean onComplet = false;//是否已经播放完一个视频

    //视频播放完成监听
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.module_activity_video_detail);
        super.onCreate(savedInstanceState);
        String CPU_ABI = android.os.Build.CPU_ABI;
        boolean isLiveStreaming = getIntent().getIntExtra("liveStreaming", 0) == 1;
        //播放地址
        mVideoPath = "";

        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        fl_surfaceview_parent = (AspectFrameLayout) findViewById(R.id.fl_surfaceview_parent);
        mSurfaceView.getHolder().addCallback(mCallback);
        mSurfaceWidth = getResources().getDisplayMetrics().widthPixels;
        mSurfaceHeight = getResources().getDisplayMetrics().heightPixels;


        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        controller = new VideoController(this);

        ViewTreeObserver vto2 = mSurfaceView.getViewTreeObserver();
        vto2.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mSurfaceView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                mSurfaceView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, sv_height));
            }
        });

        mSurfaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                controller.show();
            }
        });

    }

    public void setVideoUrl(String url) {
        mVideoPath = url;
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
        FrameLayout.LayoutParams temp=new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        temp.gravity=Gravity.CENTER;
        fl_surfaceview_parent.setLayoutParams(temp);
        mSurfaceView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);//设置activity横屏
        getWindow().getDecorView().setSystemUiVisibility(View.INVISIBLE);//隐藏状态栏
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        Log.i("tag", "1--" + fl_surfaceview_parent.getMeasuredWidth() + "---" + fl_surfaceview_parent.getMeasuredHeight());
        controller.To_change_screen(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
    }

    //退出全屏的操作
    public void Exit_full_screen() {
        isFullScreen = false;
       FrameLayout.LayoutParams temp= new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                sv_height);
       temp.gravity=Gravity.TOP;
        fl_surfaceview_parent.setLayoutParams(temp);

        mSurfaceView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, sv_height));
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

    public void addListener() {
        if (mMediaPlayer != null) {
            mMediaPlayer.setOnBufferingUpdateListener(new IMediaPlayer.OnBufferingUpdateListener() {
                @Override
                public void onBufferingUpdate(IMediaPlayer iMediaPlayer, int i) {
                    Log.e("jbl", "onBufferingUpdate==" + i);
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
                        int videoWidth = iMediaPlayer.getVideoWidth();
                        int videoHeight = iMediaPlayer.getVideoHeight();
                        float ratio = (float)videoWidth/(float)videoHeight;
                        Log.e("jbl==","w="+videoWidth+",h="+videoHeight+",i2="+i2+",i3="+i3);
                        fl_surfaceview_parent.setAspectRatio(ratio);
                        To_full_screen();

                    }
                }
            });

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
                    Log.e("jbl", "视频播放错误" + errorCode);
                    // reload();
                    return true;
                }
            });
            mMediaPlayer.setOnInfoListener(new IMediaPlayer.OnInfoListener() {
                @Override
                public boolean onInfo(IMediaPlayer iMediaPlayer, int i, int i1) {
                    switch (i) {
                        case IjkMediaPlayer.MEDIA_INFO_BUFFERING_START:
                            Log.e("jbl", "缓存开始");
                            break;
                        case IjkMediaPlayer.MEDIA_INFO_BUFFERING_END:
                            Log.e("jbl", "缓存结束");

                            break;
                        case IjkMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
                            Log.e("jbl", "视频开始渲染");

                            break;
                    }
                    return false;
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


    public boolean isPlaying() {

        return mMediaPlayer.isPlaying();
    }

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


    //如果网络异常播放失败，尝试重新连接
    public void reload() {
        release();//先关闭先前的播放器
        //判断网络连接
        prepare();
    }

    public VideoController getController() {
        return controller;
    }
}
