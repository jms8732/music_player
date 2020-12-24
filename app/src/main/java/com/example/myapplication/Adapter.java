package com.example.myapplication;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.databinding.ItemListRowBinding;

import java.util.List;
import java.util.logging.Handler;

public class Adapter extends RecyclerView.Adapter<Adapter.MyHolder> {
    private List<Music> music;
    private Context context;
    private HandleListener handleListener;
    private MainViewModel viewModel;

    public Adapter(Context context, List<Music> list, HandleListener handler, MainViewModel viewModel){
        this.context =context;
        this.music = list;
        this.handleListener = handler;
        this.viewModel =viewModel;
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
        Music m = music.get(position);
        holder.binding.setMusic(m);
        holder.binding.setHandler(handleListener);
        holder.binding.setHolder(holder);
        holder.binding.setViewModel(viewModel);
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
