package com.example.musicplayer;

import android.app.LauncherActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.databinding.ListRowBinding;

import java.util.List;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MusicHolder> {
    private List<Music> musicList;
    private MacroListener listener;
    private MusicViewModel model;
    private ItemTouchHelper helper;

    public MusicAdapter(MacroListener listener, MusicViewModel model, ItemTouchHelper helper) {
        this.listener = listener;
        this.model = model;
        this.helper =helper;
    }

    public void setMusicList(List<Music> list){
        this.musicList = list;
    }

    @NonNull
    @Override
    public MusicHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ListRowBinding binding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()),R.layout.list_row,parent,false);
        return new MusicHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MusicHolder holder, int position) {
        holder.listRowBinding.setMusic(musicList.get(position));
        holder.listRowBinding.setListener(listener);
        holder.listRowBinding.setHolder(holder);
        holder.listRowBinding.setHelper(helper);
        holder.listRowBinding.setModel(model);
        holder.listRowBinding.executePendingBindings();
    }

    @Override
    public int getItemCount() {
        return musicList.size();
    }

    public class MusicHolder extends RecyclerView.ViewHolder{
        public ListRowBinding listRowBinding;

        public MusicHolder(@NonNull ListRowBinding binding) {
            super(binding.getRoot());
            listRowBinding = binding;
        }
    }
}
