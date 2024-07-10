package com.haibin.calendarview;

import android.view.animation.Animation;

import java.util.List;

public interface IMonthView {
    void setTranslationY(float f);

    void goneMonthView();

    void scrollToCalendar(int year, int month, int day, boolean smoothScroll, boolean invokeListener);

    int getHeight();

//    void clear();

    void updateRange();

    void updateDefaultSelect();

    void scrollToCurrent(boolean smoothScroll);

    void updateShowMode();

    int getCurrentHeight();

    List<Calendar> getCurrentMonthCalendars();

    int getCurrentMonthItem();

    void notifyItemChanged(int i);

    boolean isVisible();

    void onPageSelected(int position, boolean z);

    void updateItemHeight();

    boolean isMonthListScrolling();

    void updateWeekStart();

    void clearSelect();

    void closeMonthViewAnimation(Animation.AnimationListener animationListener);

    void updateMonthViewClass();

    void notifyDataSetChanged();

    void updateScheme();

    void setParentLayout(CalendarLayout calendarLayout);

    void setWeekBar(WeekBar weekBar);

    void setWeekViewPager(WeekViewPager weekViewPager);

    void clearSelectRange();

    void hideMonthView();

    void showMonthView();

    void updateSelected();

    void setup(CalendarViewDelegate cVar, boolean z);

    void setCurrentItem(int i, boolean z);

    void showMoreMonthView();

    void showSingleMonthView();

}
