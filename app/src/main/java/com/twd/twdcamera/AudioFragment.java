package com.twd.twdcamera;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class AudioFragment extends Fragment implements View.OnFocusChangeListener, SeekBar.OnSeekBarChangeListener {

    private SeekBar seekBar;
    private AudioManager audioManager;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_volume,container,false);
        seekBar = view.findViewById(R.id.seekbar_volume);
        seekBar.setOnSeekBarChangeListener(this);
        seekBar.setOnFocusChangeListener(this);

        audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);

        //获取当前系统音量
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        seekBar.setMax(maxVolume);
        seekBar.setProgress(currentVolume);
        return view;
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus){
            Log.i("yang","---------focus-fragment-------");
            v.setBackgroundColor(getResources().getColor(R.color.gray));
        } else {
            Log.i("yang","----------noFocus--------");
            v.setBackgroundColor(getResources().getColor(R.color.white));
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        //当seekbar值变化时调用
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,progress,0);
        Log.i("AudioFragment","当前音量："+audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        //当触摸SeekBar时调用
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        //当停止触摸SeekBar时调用
    }
}
