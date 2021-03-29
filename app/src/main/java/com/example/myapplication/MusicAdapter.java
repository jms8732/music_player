package com.example.myapplication;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.databinding.ListRowBinding;

import java.util.List;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MusicHolder> {
    private static final String TAG = "jms8732";
    private List<Music> list;
    private MusicClickListener listener;

    public MusicAdapter(List<Music> list, MusicClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MusicHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ListRowBinding binding = ListRowBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new MusicHolder(binding);
    }

    public void removeMusic(int pos) {
        list.remove(pos);
        notifyItemRemoved(pos);
    }

    public void addMusic(int pos, Music music){
        list.add(pos,music);
        notifyItemInserted(pos);
    }

    public Music getMusic(int pos){
        return list.get(pos);
    }

    @Override
    public long getItemId(int position) {
        return Long.parseLong(list.get(position).getId());
    }

    @Override
    public void onBindViewHolder(@NonNull MusicHolder holder, int position) {
        holder.rowBinding.setMusic(list.get(position));
        holder.rowBinding.setListener(listener);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class MusicHolder extends RecyclerView.ViewHolder {
        public ListRowBinding rowBinding;

        public MusicHolder(@NonNull ListRowBinding binding) {
            super(binding.getRoot());
            rowBinding = binding;
        }
    }
}
