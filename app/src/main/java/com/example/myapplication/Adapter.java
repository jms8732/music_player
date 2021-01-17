package com.example.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.databinding.ItemListRowBinding;

import java.util.ArrayList;
import java.util.List;

public class Adapter extends RecyclerView.Adapter<Adapter.MyHolder> {
    private List<Music> music;
    private Context context;
    private HandleListener handleListener;
    private MusicViewModel musicViewModel;

    public Adapter(Context context, HandleListener handler, MusicViewModel musicViewModel){
        this.context =context;
        this.handleListener = handler;
        this.musicViewModel = musicViewModel;
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
        Music m = music.get(position);
        holder.binding.setMusic(m);
        holder.binding.setHandler(handleListener);
        holder.binding.setHolder(holder);
        holder.binding.setMusicView(musicViewModel);
        holder.binding.executePendingBindings();
    }

    public void setMusic(List<Music> music){
        this.music = music;
    }

    @Override
    public int getItemCount() {
        return music.size();
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemListRowBinding binding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()),R.layout.item_list_row,parent,false);
        MyHolder myHolder = new MyHolder(binding);
        return myHolder;
    }


    public static class MyHolder extends RecyclerView.ViewHolder{
        ItemListRowBinding binding;

        public MyHolder(@NonNull ItemListRowBinding itemView) {
            super(itemView.getRoot());
            binding = itemView;
        }

    }


}
