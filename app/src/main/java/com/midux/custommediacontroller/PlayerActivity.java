package com.midux.custommediacontroller;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;

public class PlayerActivity extends TVBasePlayerAcivity {
    static final String TAG="PlayerActivity";
     KeyEvent.Callback customKeyCallback;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String url = getUrlData(getIntent());
        if (!TextUtils.isEmpty(url)) {
            setVideoUrl(url);
        } else {
//            setVideoUrl("http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4");
            setVideoUrl(STREAM_URL_MP4_VOD_SHORT);
        }
    }

    public String getUrlData(Intent intent) {
        return intent.getStringExtra("video_url");
    }

    public static void intentTo(Context ctx, String url) {
        Intent intent = new Intent(ctx, PlayerActivity.class);
        intent.putExtra("video_url", url);
        ctx.startActivity(intent);

    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.e("jbl","－－－－－向上－－－－－");
        getController().show();
        switch (keyCode){
            case KeyEvent.KEYCODE_DPAD_UP://向上
                Log.e("jbl","－－－－－向上－－－－－");
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN://向下
                if(isPlaying()){
                    Log.e("jbl","－－－－－暂停视频－－－－－");
                   getController().videoPause();
                }else{
                    Log.e("jbl","－－－－－开始视频－－－－－");
                    getController().videoResume();
                }
                Log.e("jbl","－－－－－向下－－－－－");
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT://向左
                Log.e("jbl","－－－－－向左－－－－－");
                Exit_full_screen();
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT://向右
                To_full_screen();
                Log.e("jbl","－－－－－向右－－－－－");
                break;
            case KeyEvent.KEYCODE_ENTER://确定

                Log.e("jbl","－－－－－确定－－－－－");
                break;
            case KeyEvent.KEYCODE_BACK://返回
                Log.e("jbl","－－－－－返回－－－－－");
                break;
            case KeyEvent.KEYCODE_HOME://房子
                Log.e("jbl","－－－－－房子－－－－－");
                break;
            case KeyEvent.KEYCODE_MENU://菜单
                Log.e("jbl","－－－－－菜单－－－－－");
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
}