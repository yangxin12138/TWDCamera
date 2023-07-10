package com.twd.twdcamera.utils;

import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.ViewGroup;

import androidx.camera.view.PreviewView;

public class ScreenUtils {
    TextureView mTextureView;//获取相机预览界面的对象
    int screenWidth;//当前屏幕的宽度
    int screenHeight;//当前屏幕的高度
    ViewGroup.LayoutParams layoutParams;

    public ScreenUtils(TextureView mTextureView,int screenWidth,int screenHeight) {
        this.mTextureView = mTextureView;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }



    public  void updateSize(String item){
         layoutParams = mTextureView.getLayoutParams();//绑定预览页面的布局参数对象
        if ("1280*720".equals(item)) {
            float aspectRatio4x3 = 4f / 3f;//设置画面比例 4:3
            int targetWidth4x3 = screenWidth;
            int targetHeight4x3 = (int) (targetWidth4x3 / aspectRatio4x3);
            if (targetHeight4x3 > screenHeight) { //判断计算出的高是否溢出屏幕不可见
                targetHeight4x3 = screenHeight;
                targetWidth4x3 = (int) (targetHeight4x3 * aspectRatio4x3);
            }
            layoutParams.width = targetWidth4x3;
            layoutParams.height = targetHeight4x3;
            mTextureView.setLayoutParams(layoutParams);
            Log.i("yang", "------首页切换到720------" + "width:" + layoutParams.width + ",height:" + layoutParams.height);
        } else if ("1920*1080".equals(item) || "自动".equals(item)) {
            float aspectRatio16x9 = 16f / 9f;
            int targetWidth16x9 = screenWidth;
            int targetHeight16x9 = (int) (targetWidth16x9 / aspectRatio16x9);
            if (targetHeight16x9 > screenHeight) {
                targetHeight16x9 = screenHeight;
                targetWidth16x9 = (int) (targetHeight16x9 * aspectRatio16x9);
            }
            layoutParams.width = targetWidth16x9;
            layoutParams.height = targetHeight16x9;
            mTextureView.setLayoutParams(layoutParams);
            Log.i("yang", "------首页切换到1080------" + "width:" + layoutParams.width + ",height:" + layoutParams.height);
        }
    }

    public ViewGroup.LayoutParams getLayoutParams() {
        return layoutParams;
    }
}