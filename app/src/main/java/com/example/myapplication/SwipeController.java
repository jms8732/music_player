package com.example.myapplication;


import android.graphics.Canvas;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

public class SwipeController extends ItemTouchHelper.Callback {
    private static final String TAG = "jms8732";
    private MusicClickListener adapter;

    enum ButtonState {
        GONE, RIGHT_VISIBLE
    }

    private ButtonState buttonState = ButtonState.GONE;
    private static final float buttonWidth = 300;
    private boolean swiped = false;

    public SwipeController(MusicClickListener adapter) {
        this.adapter = adapter;
    }

    @Override
    public int convertToAbsoluteDirection(int flags, int layoutDirection) {
        if (swiped) {
            swiped = buttonState != ButtonState.GONE;
            return 0;
        }

        return super.convertToAbsoluteDirection(flags, layoutDirection);
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            if (buttonState != ButtonState.GONE) {
                dX = Math.min(dX, -buttonWidth);
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            } else {
                setTouchListener(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        }

        if (buttonState == ButtonState.GONE) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
    }

    private void setTouchListener(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        recyclerView.setOnTouchListener((v, event) -> {
            swiped = event.getAction() == MotionEvent.ACTION_CANCEL || event.getAction() == MotionEvent.ACTION_UP;
            if (swiped) {
                //스와이프를 한 경우
                if (dX < -buttonWidth) buttonState = ButtonState.RIGHT_VISIBLE;

                if (buttonState != ButtonState.GONE) {
                    setTouchDownListener(c, recyclerView, viewHolder, 0, dY, actionState, isCurrentlyActive);
                    setItemClickable(recyclerView, false);
                }
            }
            return false;
        });
    }

    private void setTouchDownListener(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        recyclerView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                setTouchUpListener(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
            return false;
        });
    }

    private void setTouchUpListener(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        recyclerView.setOnTouchListener((v, event) -> {
            SwipeController.super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            recyclerView.setOnTouchListener((v1, event1) -> false);

            recyclerView.performClick();
            setItemClickable(recyclerView, true);
            swiped = false;
            buttonState = ButtonState.GONE;

            return false;
        });
    }


    private void setItemClickable(RecyclerView recyclerView, boolean isClick) {
        for (int i = 0; i < recyclerView.getChildCount(); i++)
            recyclerView.getChildAt(i).setClickable(isClick);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        return makeMovementFlags(0, ItemTouchHelper.START | ItemTouchHelper.END);
    }
}
