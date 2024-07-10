package com.haibin.calendarview;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

public class MonthRecyclerViewScrollListener extends RecyclerView.OnScrollListener {

    private final SnapHelper mSnapHelper;

    private final ScrollListener mScrollListener;

    private int mLastScrollPosition = -1;

    public interface ScrollListener {
        void onScrollToPosition(int i);

        void onScrolled(RecyclerView recyclerView, int i, int i2);

        void onScrollStateChanged(RecyclerView recyclerView, int i);
    }

    public MonthRecyclerViewScrollListener(SnapHelper snapHelper, ScrollListener scrollListener) {
        this.mSnapHelper = snapHelper;
        this.mScrollListener = scrollListener;
    }

    public void onScrollStateChanged(RecyclerView recyclerView, int i) {
        super.onScrollStateChanged(recyclerView, i);
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (layoutManager != null) {
            View snapView = mSnapHelper.findSnapView(layoutManager);
            int position = snapView != null ? layoutManager.getPosition(snapView) : 0;
            if (mScrollListener != null) {
                mScrollListener.onScrollStateChanged(recyclerView, i);
                if (i == 0 && this.mLastScrollPosition != position) {
                    this.mLastScrollPosition = position;
                    this.mScrollListener.onScrollToPosition(position);
                }
            }
        }
    }

    public void onScrolled(RecyclerView recyclerView, int i, int i2) {
        super.onScrolled(recyclerView, i, i2);
        if (mScrollListener != null) {
            mScrollListener.onScrolled(recyclerView, i, i2);
        }
    }
}
