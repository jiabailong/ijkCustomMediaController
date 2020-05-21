package com.midux.custommediacontroller;

/**
 * Created by yu_midux on 2018/3/21.
 */

public interface MediaControllerInterface {

    void show();//显示

    void show(int time);//显示

    void hide();//隐藏

    boolean isShowing();//是否显示

    void setCollected(boolean isCollected);//是否被收藏

    void setSeekBarEnabled(boolean var1);

    void isCompleted();//设置已经播放完成了

    void To_change_screen(int w,int h);//改变宽高


    interface MediaControl {
        void VideoStart();//开始播放

        void VideoResume();//继续播放

        void VideoPause();//暂停

        void VideoStop();//停止

        long getDuration();//视频时长

        long getCurrentPosition();//当前播放进度

        void seekTo(long var1);//拖动进度

        boolean isPlaying();//是否正在播放

        boolean isPausing();//是否是暂停状态

        boolean isFullScreen();//判断是否是全屏状态

        void toCollect();//收藏按钮

        void actionForFullScreen();//收藏按钮
    }
}
