package com.example.myapplication;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

public class ItemDecoration extends RecyclerView.ItemDecoration{
    private static final int [] ATTRS = new int[]{android.R.attr.listDivider};
    private Drawable divider;

    public ItemDecoration(Context context){
        final TypedArray style = context.obtainStyledAttributes(ATTRS);
        divider = style.getDrawable(0);
        style.recycle();
    }

    public ItemDecoration(Context context, int resId){
        divider = ContextCompat.getDrawable(context,resId);
    }

    @Override
    public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        int left = parent.getPaddingLeft();
        int right = parent.getWidth() - parent.getPaddingRight();

        int childCount = parent.getChildCount();

        for(int i =0 ; i < childCount ; i++){
            View child = parent.getChildAt(i);

            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)child.getLayoutParams();

            int top = child.getBottom() + params.bottomMargin;
            int bottom = top + divider.getIntrinsicHeight();

            divider.setBounds(left,top,right,bottom);
            divider.draw(c);
        }
    }
}
