package com.twd.twdcamera;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.view.PreviewView;
import androidx.fragment.app.Fragment;

import com.twd.twdcamera.utils.ScreenUtils;

import java.util.ArrayList;
import java.util.List;

public class ScreenFragment extends Fragment implements View.OnFocusChangeListener {

    private LinearLayout LLScreen;
    private TextView screen_text;

    private PreviewView previewView;

    List<String> screenSizes = new ArrayList<>();
    int currentIndex = 0;

    ViewGroup.LayoutParams layoutParams;
    private ScreenUtils utils;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_screen,container,false);
        previewView = getActivity().findViewById(R.id.cameraView);
        LLScreen = view.findViewById(R.id.LL_screen);
        screen_text = view.findViewById(R.id.screen_text);
        screenSizes.add("自动");
        screenSizes.add("1280*720");
        screenSizes.add("1920*1080");
        LLScreen.setOnFocusChangeListener(this);

        layoutParams = previewView.getLayoutParams();

        //TODO:初始化读取text
        SharedPreferences sharedPreferences = getActivity().getApplication().getSharedPreferences("ScreenSizePreferences",Context.MODE_PRIVATE);
        int index = sharedPreferences.getInt("index",0);
        screen_text.setText(screenSizes.get(index));
        Log.i("yang","初始化text："+screen_text.getText().toString());

        //获取屏幕的宽度和高度
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;
        utils = new ScreenUtils(previewView,screenWidth,screenHeight);
        utils.updateSize(screen_text.getText().toString());
        //updateSize(screen_text.getText().toString());
        return view;
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus){
            Log.i("yang","---------focus-fragment-------");
            v.setBackgroundColor(getResources().getColor(R.color.gray));
            if (v.getId() == R.id.LL_screen){
                v.setOnKeyListener(new View.OnKeyListener() {
                    @Override
                    public boolean onKey(View v, int keyCode, KeyEvent event) {
                        //TODO:判断左右方向键做textview更新
                        int action = event.getAction();
                        if (action == KeyEvent.ACTION_DOWN){
                            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT){
                                updateLeft();
                                saveUpdate(currentIndex);
                            } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                                updateRight();
                                saveUpdate(currentIndex);
                            }
                        }
                        return false;
                    }
                });
            }
        } else {
            Log.i("yang","----------noFocus--------");
            v.setBackgroundColor(getResources().getColor(R.color.white));
        }
    }


    //TODO：保存currentIndex信息
    private void saveUpdate(int currentIndex){
        Log.i("saveUpdate","保存的坐标:"+currentIndex);
        SharedPreferences.Editor editor = getActivity().getApplication().getSharedPreferences("ScreenSizePreferences", Context.MODE_PRIVATE).edit();
        editor.putInt("index",currentIndex);
        editor.apply();
    }

    private void updateSize(String item){

        //获取屏幕的宽度和高度
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;

        switch (item){
            case "1280*720":
                /*layoutParams.width = 960;
                layoutParams.height = 720;
                previewView.setLayoutParams(layoutParams);*/
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
                Log.i("yang","------切换到720------"+"width:"+layoutParams.width+",height:"+layoutParams.height);
                break;
            case "1920*1080":
                /*layoutParams.width = 960;
                layoutParams.height = 540;*/
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
                Log.i("yang","------切换到1080------"+"width:"+layoutParams.width+",height:"+layoutParams.height);
                break;
            case "自动":
                layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
                previewView.setLayoutParams(layoutParams);
                Log.i("yang","------切换到Auto------"+"width:"+layoutParams.width+",height:"+layoutParams.height);
                break;
        }
    }

    private void updateLeft(){
        currentIndex --;
        if (currentIndex < 0){
            currentIndex = screenSizes.size()-1;
        }
        Log.i("ScreenFragment","currentIndex = "+currentIndex);
        String item = screenSizes.get(currentIndex);
        screen_text.setText(item);
        //TODO:底层代码控制切换分辨率
        //updateSize(item);
        utils.updateSize(item);
    }

    private void updateRight(){
        currentIndex++;
        if (currentIndex >= screenSizes.size()){
            currentIndex = 0;
        }
        Log.i("ScreenFragment","currentIndex = "+currentIndex);
        String item = screenSizes.get(currentIndex);
        screen_text.setText(item);
        //TODO:底层代码控制切换分辨率
//        updateSize(item);
        utils.updateSize(item);
    }
}
