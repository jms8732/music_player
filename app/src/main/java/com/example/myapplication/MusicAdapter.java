package com.example.myapplication;

import android.graphics.Color;
import android.os.AsyncTask;
import android.text.BoringLayout;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.myapplication.databinding.ListRowBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MusicHolder> implements Filterable {
    private static final String TAG = "jms8732";
    private List<Music> origin,filtered;
    private MusicClickListener listener;
    private MusicFilter filter;

    public MusicAdapter(List<Music> list, MusicClickListener listener) {
        this.origin = list;
        this.filtered =new ArrayList<>(origin);
        this.listener = listener;
        this.filter = new MusicFilter();
    }

    @NonNull
    @Override
    public MusicHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ListRowBinding binding = ListRowBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new MusicHolder(binding);
    }

    public void setOrigin(List<Music> list){
        this.origin = list;
    }

    public void removeMusic(int pos) {
        origin.remove(pos);

        filtered.clear();
        filtered.addAll(origin);
        notifyItemRemoved(pos);
    }

    public void addMusic(int pos, Music music){
        origin.add(pos,music);

        filtered.clear();
        filtered.addAll(origin);
        notifyItemInserted(pos);
    }

    //음악 목록을 새로 고침하는 메소드
    public void refreshMusicList(final List<Music> newList, final SwipeRefreshLayout swipeRefreshLayout, final RecyclerView recyclerView){

        new AsyncTask<Void, Void, DiffUtil.DiffResult>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected void onPostExecute(DiffUtil.DiffResult result) {
                origin.clear();
                origin.addAll(newList);

                filtered.clear();
                filtered.addAll(origin);

                result.dispatchUpdatesTo(MusicAdapter.this);
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            protected DiffUtil.DiffResult doInBackground(Void... voids) {
                final MusicDiff diff = new MusicDiff(origin, newList);
                final DiffUtil.DiffResult result = DiffUtil.calculateDiff(diff);
                return result;
            }
        }.execute();

    }

    public Music getMusic(int pos){
        return filtered.get(pos);
    }

    @Override
    public Filter getFilter() {
        return filter;
    }

    @Override
    public long getItemId(int position) {
        return Long.parseLong(filtered.get(position).getId());
    }

    @Override
    public void onBindViewHolder(@NonNull MusicHolder holder, int position) {
        holder.rowBinding.setMusic(filtered.get(position));
        holder.rowBinding.setListener(listener);
    }

    @Override
    public int getItemCount() {
        return filtered.size();
    }

    public static class MusicHolder extends RecyclerView.ViewHolder {
        public ListRowBinding rowBinding;

        public MusicHolder(@NonNull ListRowBinding binding) {
            super(binding.getRoot());
            rowBinding = binding;
        }
    }

    //음악 필터링
    private class MusicFilter extends Filter{
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<Music> ret = new ArrayList<>();

            String s = constraint.toString().toLowerCase().trim();

            if(s == null && s.isEmpty()){
                ret.addAll(origin);
            }else{
                List<Music> collect = origin.stream()
                        .filter(m -> m.getTitle().toLowerCase().contains(s) || m.getArtist().toLowerCase().contains(s))
                        .distinct()
                        .collect(Collectors.toList());
                ret.addAll(collect);
            }

            FilterResults results = new FilterResults();
            results.values = ret;

            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            List<Music> temp = (List<Music>)results.values;
            filtered.clear();
            filtered.addAll(temp);
            notifyDataSetChanged();
        }
    }
}
