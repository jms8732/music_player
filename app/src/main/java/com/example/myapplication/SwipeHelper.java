package com.example.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

public class SwipeHelper extends ItemTouchHelper.Callback {
    private static final String TAG = "jms8732";
    private SwipeAdapter adapter;
    private Context context;
    private Paint mClearPaint;
    private ColorDrawable mBackground;
    private int backgroundColor;
    private Drawable deleteDrawable;
    private int intrinsicWidth, intrinsicHeight;


    public SwipeHelper(Context context, SwipeAdapter adapter) {
        this.context = context;
        this.adapter = adapter;
        this.mBackground = new ColorDrawable();
        this.backgroundColor = Color.parseColor("#b80f0a");
        this.mClearPaint = new Paint();
        this.mClearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        this.deleteDrawable = ContextCompat.getDrawable(context,R.drawable.delete);
        intrinsicWidth = deleteDrawable.getIntrinsicWidth();
        intrinsicHeight = deleteDrawable.getIntrinsicHeight();
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

        View view = viewHolder.itemView;
        int itemHeight = view.getHeight();

        boolean isCancelled = dX == 0 && !isCurrentlyActive;

        if(isCancelled){
            clearCanvas(c,view.getRight()+ dX, (float)view.getTop(), view.getRight(),view.getBottom());
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            return;
        }
        mBackground.setColor(backgroundColor);
        mBackground.setBounds(view.getRight() + (int) dX, view.getTop(), view.getRight(), view.getBottom());
        mBackground.draw(c);

        int deleteIconTop = view.getTop()  + (itemHeight - intrinsicHeight)  / 2;
        int deleteIconMargin = (itemHeight - intrinsicHeight) /2;
        int deleteIconLeft = view.getRight() - deleteIconMargin - intrinsicWidth;
        int deleteIconRight = view.getRight() - deleteIconMargin;
        int deleteIconBottom = deleteIconTop + intrinsicHeight;

        deleteDrawable.setBounds(deleteIconLeft, deleteIconTop, deleteIconRight, deleteIconBottom);
        deleteDrawable.draw(c);

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    private void clearCanvas(Canvas c, float left , float top ,float right, float bottom){
        c.drawRect(left,top,right,bottom,mClearPaint);
    }

    @Override
    public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
        return 0.7F;
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        return makeMovementFlags(0,ItemTouchHelper.LEFT);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        adapter.swipeDelete(viewHolder,direction);
    }
}
