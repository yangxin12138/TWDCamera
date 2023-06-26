package com.twd.twdcamera;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

public class ScreenFragment extends Fragment implements View.OnFocusChangeListener {

    private LinearLayout LLScreen;
    private TextView screen_text;

    List<String> screenSizes = new ArrayList<>();
    int currentIndex = 0;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_screen,container,false);
        LLScreen = view.findViewById(R.id.LL_screen);
        screen_text = view.findViewById(R.id.screen_text);
        screenSizes.add("自动");
        screenSizes.add("1280*720");
        screenSizes.add("1920*1080");
        LLScreen.setOnFocusChangeListener(this);

        //TODO:初始化读取text
        SharedPreferences sharedPreferences = getActivity().getApplication().getSharedPreferences("ScreenSizePreferences",Context.MODE_PRIVATE);
        int index = sharedPreferences.getInt("index",0);
        screen_text.setText(screenSizes.get(index));
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

    private void updateLeft(){
        currentIndex --;
        if (currentIndex < 0){
            currentIndex = screenSizes.size()-1;
        }
        Log.i("ScreenFragment","currentIndex = "+currentIndex);
        String item = screenSizes.get(currentIndex);
        screen_text.setText(item);
        //TODO:底层代码控制切换分辨率
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
    }
}
