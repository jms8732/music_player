package com.example.myapplication;

import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.databinding.BindingAdapter;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.RecyclerView;

import com.skydoves.transformationlayout.TransformationLayout;

import org.jetbrains.annotations.NotNull;

public class HandleListener implements Status{
    private static final String TAG = "jms8732";
    private HandleAdpater mAdapter;
    private String id;

    public HandleListener(HandleAdpater adapter) {
        this.mAdapter =adapter;
    }

    public void onClick(MutableLiveData<Boolean> play){
        if(!play.getValue()){
            mAdapter.restartMusic();
        }else{
            mAdapter.pauseMusic();
        }
    }

    public void onRecycleLayoutClick(Music music, Adapter.MyHolder holder, MutableLiveData<Boolean> play){
        if(id == null || !id.equals(music.getId())){
            //음악이 처음 실행되는 경우
            mAdapter.startMusic(music,START,holder.getAdapterPosition());
            id = music.getId();
        }else{
            if(!play.getValue()){
                mAdapter.restartMusic();
            }else
                mAdapter.pauseMusic();
        }

    }

    public void onTransform(View view){
        TransformationLayout transformationLayout = (TransformationLayout)view;
        if(transformationLayout.isTransformed())
            transformationLayout.finishTransform();
        else
            transformationLayout.startTransform();
    }

}
