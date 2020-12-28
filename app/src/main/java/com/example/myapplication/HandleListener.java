package com.example.myapplication;

import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.databinding.BindingAdapter;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.RecyclerView;

import com.skydoves.transformationlayout.TransformationLayout;

import org.jetbrains.annotations.NotNull;

public class HandleListener{
    private static final String TAG = "jms8732";
    private HandleAdpater mAdapter;

    public HandleListener(HandleAdpater adapter) {
        this.mAdapter =adapter;
    }

    public void onClick(Music music){
        mAdapter.judgeAction(music,-1);
    }

    public void onForwardRewindClick(int status){
        if(status == Status.FORWARD){
            mAdapter.forwardMusic();
        }else{
            mAdapter.rewindMusic();
        }
    }

    public void onRecycleLayoutClick(Music music, Adapter.MyHolder holder){
        mAdapter.judgeAction(music,holder.getAdapterPosition());
    }

    public void onTransform(View view){
        TransformationLayout transformationLayout = (TransformationLayout)view;
        if(transformationLayout.isTransformed())
            transformationLayout.finishTransform();
        else
            transformationLayout.startTransform();
    }

}
