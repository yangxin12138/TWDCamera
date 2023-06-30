package com.twd.twdcamera.utils;

import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ViewGroup;

import androidx.camera.view.PreviewView;

public class ScreenUtils {
    PreviewView previewView;
    int screenWidth;
    int screenHeight;

    public ScreenUtils(PreviewView previewView,int screenWidth,int screenHeight) {
        this.previewView = previewView;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }



    public  void updateSize(String item){
        ViewGroup.LayoutParams layoutParams = previewView.getLayoutParams();

        if ("1280*720".equals(item)) {
            float aspectRatio4x3 = 4f / 3f;
            int targetWidth4x3 = screenWidth;
            int targetHeight4x3 = (int) (targetWidth4x3 / aspectRatio4x3);
            if (targetHeight4x3 > screenHeight) {
                targetHeight4x3 = screenHeight;
                targetWidth4x3 = (int) (targetHeight4x3 * aspectRatio4x3);
            }
            layoutParams.width = targetWidth4x3;
            layoutParams.height = targetHeight4x3;
            previewView.setLayoutParams(layoutParams);
            Log.i("yang", "------首页切换到720------" + "width:" + layoutParams.width + ",height:" + layoutParams.height);
        } else if ("1920*1080".equals(item)) {
            float aspectRatio16x9 = 16f / 9f;
            int targetWidth16x9 = screenWidth;
            int targetHeight16x9 = (int) (targetWidth16x9 / aspectRatio16x9);
            if (targetHeight16x9 > screenHeight) {
                targetHeight16x9 = screenHeight;
                targetWidth16x9 = (int) (targetHeight16x9 * aspectRatio16x9);
            }
            layoutParams.width = targetWidth16x9;
            layoutParams.height = targetHeight16x9;
            previewView.setLayoutParams(layoutParams);
            Log.i("yang", "------首页切换到1080------" + "width:" + layoutParams.width + ",height:" + layoutParams.height);
        } else if ("自动".equals(item)) {
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
            previewView.setLayoutParams(layoutParams);
            Log.i("yang", "------首页切换到Auto------" + "width:" + layoutParams.width + ",height:" + layoutParams.height);
        }
    }
}