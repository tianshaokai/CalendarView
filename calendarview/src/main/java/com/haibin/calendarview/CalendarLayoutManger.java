package com.haibin.calendarview;

import android.content.Context;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

public class CalendarLayoutManger extends LinearLayoutManager {

    class CalendarLinearSmoothScroller extends LinearSmoothScroller {
        CalendarLinearSmoothScroller(CalendarLayoutManger calendarLayoutManger, Context context) {
            super(context);
        }

        /* access modifiers changed from: protected */
        /* renamed from: B */
        public int getVerticalSnapPreference() {
            return -1;
        }

        /* access modifiers changed from: protected */
        /* renamed from: x */
        public int calculateTimeForScrolling(int i) {
            if (i > 100) {
                i = 100;
            }
            return super.calculateTimeForScrolling(i);
        }
    }

    public CalendarLayoutManger(Context context) {
        super(context);
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
        CalendarLinearSmoothScroller calendarLinearSmoothScroller = new CalendarLinearSmoothScroller(this, recyclerView.getContext());
        calendarLinearSmoothScroller.setTargetPosition(position);
        startSmoothScroll(calendarLinearSmoothScroller);
    }
}
