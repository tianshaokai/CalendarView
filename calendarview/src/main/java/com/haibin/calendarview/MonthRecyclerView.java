package com.haibin.calendarview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.ScaleAnimation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MonthRecyclerView extends RecyclerView implements IMonthView {

    private int mMonthCount;

    private CalendarViewDelegate mDelegate;

    private int mCurrentViewHeight;

    CalendarLayout mParentLayout;

    WeekViewPager weekViewPager;

    WeekBar weekBar;

    /**
     * 是否使用滚动到某一天
     */
    private boolean isUsingScrollToCalendar;
    private boolean f8704P0;

    private int mCurrentItem;
    private int mCurrentItemHeight;

    public LinearSnapHelper linearSnapHelper;

    private CalendarLayoutManger calendarLayoutManger;
    private MonthRecyclerViewAdapter monthRecyclerViewAdapter;

    /**
     * 是否在月视图滚动
     */
    private boolean isMonthListScrolling = false;


    public MonthRecyclerView(@NonNull Context context) {
        this(context, null);
    }

    public MonthRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        isUsingScrollToCalendar = false;
        f8704P0 = true;
    }

    @Override
    public void setup(CalendarViewDelegate delegate, boolean z) {
        this.mDelegate = delegate;
        if (delegate.isFullScreenCalendar()) {
            init();
            return;
        }

        updateMonthViewHeight(mDelegate.getCurrentDay().getYear(),
                mDelegate.getCurrentDay().getMonth());

        ViewGroup.LayoutParams params = getLayoutParams();
        params.height = mCurrentViewHeight;
        setLayoutParams(params);
        init();
    }

    private void init() {
        setClipToPadding(false);
        setPadding(0, 0, 0, mDelegate.getMonthHeaderHeight());
        mMonthCount = (((mDelegate.getMaxYear() - mDelegate.getMinYear()) * 12) - mDelegate.getMinYearMonth()) + 1 + mDelegate.getMaxYearMonth();
        CalendarLayoutManger calendarLayoutManger = new CalendarLayoutManger(getContext());
        this.calendarLayoutManger = calendarLayoutManger;
        setLayoutManager(calendarLayoutManger);
        setItemAnimator(null);

        linearSnapHelper = new MonthLinearSnapHelper();
        linearSnapHelper.attachToRecyclerView(this);

        monthRecyclerViewAdapter = new MonthRecyclerViewAdapter();
        setAdapter(monthRecyclerViewAdapter);

        addOnScrollListener(new MonthRecyclerViewScrollListener(this.linearSnapHelper, new MonthRecyclerViewScrollListener.ScrollListener() {
            @Override
            public void onScrollToPosition(int i) {
                if (!MonthRecyclerView.this.isMonthListScrolling) {
                    onPageSelected(i, false);
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int i, int i2) {

            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int i) {

            }
        }));
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        return mDelegate.isMonthViewScrollable() && super.onInterceptTouchEvent(e);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        return mDelegate.isMonthViewScrollable() && super.onTouchEvent(e);
    }



    @Override
    public void scrollToCalendar(int year, int month, int day, boolean smoothScroll, boolean invokeListener) {
        this.isUsingScrollToCalendar = true;
        Calendar calendar = new Calendar();
        calendar.setYear(year);
        calendar.setMonth(month);
        calendar.setDay(day);
        calendar.setCurrentDay(calendar.equals(this.mDelegate.getCurrentDay()));
        LunarCalendar.setupLunarCalendar(calendar);

        mDelegate.mIndexCalendar = calendar;
        mDelegate.mSelectedCalendar = calendar;
        mDelegate.updateSelectCalendarScheme();


        int y = calendar.getYear() - mDelegate.getMinYear();
        int position = 12 * y + calendar.getMonth() - mDelegate.getMinYearMonth();
        int curItem = getCurrentItem();
        if (curItem == position) {
            isUsingScrollToCalendar = false;
            setCurrentItem(year, smoothScroll);
        } else {
            setCurrentItem(year, smoothScroll);
            onPageSelected(year, true);
        }

        BaseMonthView view = findViewWithTag(position);
        if (view != null) {
            view.setSelectedCalendar(mDelegate.mIndexCalendar);
            view.invalidate();
            if (mParentLayout != null) {
                mParentLayout.updateSelectPosition(view.getSelectedIndex(mDelegate.mIndexCalendar));
            }
        }
        if (mParentLayout != null) {
            int week = CalendarUtil.getWeekFromDayInMonth(calendar, mDelegate.getWeekStart());
            mParentLayout.updateSelectWeek(week);
        }

        if (mDelegate.mCalendarSelectListener != null && invokeListener) {
            mDelegate.mCalendarSelectListener.onCalendarSelect(calendar, false);
        }
        if (mDelegate.mInnerListener != null) {
            mDelegate.mInnerListener.onMonthDateSelected(calendar, false);
        }

        updateSelected();
    }

    @Override
    public void onPageSelected(int position, boolean z) {

        this.mCurrentItem = position;
        this.monthRecyclerViewAdapter.notifyItemChanged(position);

        Calendar calendar = CalendarUtil.getFirstCalendarFromMonthViewPager(position, mDelegate);
        if (getVisibility() == VISIBLE) {
            if (!mDelegate.isShowYearSelectedLayout &&
                    mDelegate.mIndexCalendar != null &&
                    calendar.getYear() != mDelegate.mIndexCalendar.getYear() &&
                    mDelegate.mYearChangeListener != null) {
                mDelegate.mYearChangeListener.onYearChange(calendar.getYear());
            }
            mDelegate.mIndexCalendar = calendar;
        }
        //月份改变事件
        if (mDelegate.mMonthChangeListener != null) {
            mDelegate.mMonthChangeListener.onMonthChange(calendar.getYear(), calendar.getMonth());
        }

        //周视图显示的时候就需要动态改变月视图高度
        if (weekViewPager.getVisibility() == VISIBLE) {
            updateMonthViewHeight(calendar.getYear(), calendar.getMonth());
            return;
        }


        if (mDelegate.getSelectMode() == CalendarViewDelegate.SELECT_MODE_DEFAULT) {
            if (!calendar.isCurrentMonth()) {
                mDelegate.mSelectedCalendar = calendar;
            } else {
                mDelegate.mSelectedCalendar = CalendarUtil.getRangeEdgeCalendar(calendar, mDelegate);
            }
            mDelegate.mIndexCalendar = mDelegate.mSelectedCalendar;
        } else {
            if (mDelegate.mSelectedStartRangeCalendar != null &&
                    mDelegate.mSelectedStartRangeCalendar.isSameMonth(mDelegate.mIndexCalendar)) {
                mDelegate.mIndexCalendar = mDelegate.mSelectedStartRangeCalendar;
            } else {
                if (calendar.isSameMonth(mDelegate.mSelectedCalendar)) {
                    mDelegate.mIndexCalendar = mDelegate.mSelectedCalendar;
                }
            }
        }

        mDelegate.updateSelectCalendarScheme();
        if (!isUsingScrollToCalendar && mDelegate.getSelectMode() == CalendarViewDelegate.SELECT_MODE_DEFAULT) {
            weekBar.onDateSelected(mDelegate.mSelectedCalendar, mDelegate.getWeekStart(), false);
            if (mDelegate.mCalendarSelectListener != null) {
                mDelegate.mCalendarSelectListener.onCalendarSelect(mDelegate.mSelectedCalendar, false);
            }
        }

        BaseMonthView view = findViewWithTag(position);
        if (view != null) {
            int index = view.getSelectedIndex(mDelegate.mIndexCalendar);
            if (mDelegate.getSelectMode() == CalendarViewDelegate.SELECT_MODE_DEFAULT) {
                view.mCurrentItem = index;
            }
            if (index >= 0 && mParentLayout != null) {
                mParentLayout.updateSelectPosition(index);
            }
            view.invalidate();
        }
        weekViewPager.updateSelected(mDelegate.mIndexCalendar, false);
        updateMonthViewHeight(calendar.getYear(), calendar.getMonth());
        isUsingScrollToCalendar = false;
    }


    /**
     * 更新月视图的高度
     *
     * @param year  year
     * @param month month
     */
    private void updateMonthViewHeight(int year, int month) {
        if (mDelegate.getMonthViewShowMode() == CalendarViewDelegate.MODE_ALL_MONTH) {//非动态高度就不需要了
            mCurrentViewHeight = 6 * mDelegate.getCalendarItemHeight();
            ViewGroup.LayoutParams params = getLayoutParams();
            params.height = mCurrentViewHeight;
            return;
        }

        if (mParentLayout != null) {
            if (getVisibility() != VISIBLE) {//如果已经显示周视图，则需要动态改变月视图高度，否则显示就有bug
                ViewGroup.LayoutParams params = getLayoutParams();
                params.height = CalendarUtil.getMonthViewHeight(year, month,
                        mDelegate.getCalendarItemHeight(), mDelegate.getWeekStart(),
                        mDelegate.getMonthViewShowMode());
                setLayoutParams(params);
            }
            mParentLayout.updateContentViewTranslateY();
        }
        mCurrentViewHeight = CalendarUtil.getMonthViewHeight(year, month,
                mDelegate.getCalendarItemHeight(), mDelegate.getWeekStart(),
                mDelegate.getMonthViewShowMode());
    }

    @Override
    public void updateRange() {
        notifyDataSetChanged();
        if (getVisibility() == VISIBLE) {
            this.isUsingScrollToCalendar = false;
            Calendar calendar = mDelegate.mSelectedCalendar;
            int year = (((calendar.getYear() - mDelegate.getMinYear()) * 12) + calendar.getMonth()) - mDelegate.getMinYearMonth();
            setCurrentItem(year, false);
            BaseMonthView baseMonthView = findViewWithTag(Integer.valueOf(year));
            if (baseMonthView != null) {
                baseMonthView.setSelectedCalendar(mDelegate.mIndexCalendar);
                baseMonthView.invalidate();

                if (mParentLayout != null) {
                    mParentLayout.updateSelectPosition(baseMonthView.getSelectedIndex(mDelegate.mIndexCalendar));
                }
            }
            if (mParentLayout != null) {
                mParentLayout.updateSelectWeek(CalendarUtil.getWeekFromDayInMonth(calendar, mDelegate.getWeekStart()));
            }
            if (mDelegate.mInnerListener != null) {
                mDelegate.mInnerListener.onWeekDateSelected(calendar, false);
            }
            if (mDelegate.mCalendarSelectListener != null) {
                mDelegate.mCalendarSelectListener.onCalendarSelect(calendar, false);
            }
            updateSelected();
        }
    }

    @Override
    public void updateDefaultSelect() {
        BaseMonthView baseMonthView = findViewWithTag(Integer.valueOf(getCurrentItem()));
        if (baseMonthView != null) {
            int m = baseMonthView.getSelectedIndex(mDelegate.mSelectedCalendar);
            baseMonthView.mCurrentItem = m;
            if (m >= 0 && mParentLayout != null) {
                mParentLayout.updateSelectPosition(m);
            }
            baseMonthView.invalidate();
        }
    }

    @Override
    public void scrollToCurrent(boolean smoothScroll) {
        this.isUsingScrollToCalendar = true;
        int year = (((mDelegate.getCurrentDay().getYear() - mDelegate.getMinYear()) * 12) + mDelegate.getCurrentDay().getMonth()) - mDelegate.getMinYearMonth();
        if (getCurrentItem() == year) {
            this.isUsingScrollToCalendar = false;
            setCurrentItem(year, smoothScroll);
        } else {
            setCurrentItem(year, smoothScroll);
            onPageSelected(year, true);
        }
        BaseMonthView baseMonthView = findViewWithTag(Integer.valueOf(year));
        if (baseMonthView != null) {
            baseMonthView.setSelectedCalendar(mDelegate.getCurrentDay());
            baseMonthView.invalidate();
            CalendarLayout calendarLayout = this.mParentLayout;
            if (calendarLayout != null) {
                calendarLayout.updateSelectPosition(baseMonthView.getSelectedIndex(mDelegate.getCurrentDay()));
            }
        }
        if (mDelegate.mCalendarSelectListener != null && getVisibility() == VISIBLE) {
            mDelegate.mCalendarSelectListener.onCalendarSelect(mDelegate.mSelectedCalendar, false);
        }
    }

    @Override
    public void updateShowMode() {
        this.monthRecyclerViewAdapter.notifyDataSetChanged();
        if (mDelegate.getMonthViewShowMode() == 0) {
            this.mCurrentViewHeight = mDelegate.getCalendarItemHeight() * 6;
        } else {
//            updateMonthViewHeight(mDelegate.mSelectedCalendar.getYear(), mDelegate.mSelectedCalendar.getMonth(), false);
        }
        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        layoutParams.height = this.mCurrentViewHeight;
        setLayoutParams(layoutParams);
        if (mParentLayout != null) {
            mParentLayout.updateContentViewTranslateY();
        }
    }

    @Override
    public int getCurrentHeight() {
        return CalendarUtil.getMonthViewHeight((((this.mCurrentItem + mDelegate.getMinYearMonth()) - 1) / 12) + mDelegate.getMinYear(),
                (((this.mCurrentItem + mDelegate.getMinYearMonth()) - 1) % 12) + 1, mDelegate.getCalendarItemHeight(),
                mDelegate.getWeekStart(), mDelegate.getMonthViewShowMode());
    }

    public int getCurrentItem() {
        return mCurrentItem;
    }

    @Override
    public List<Calendar> getCurrentMonthCalendars() {
        BaseMonthView baseMonthView = findViewWithTag(Integer.valueOf(getCurrentItem()));
        if (baseMonthView == null) {
            return null;
        }
        return baseMonthView.mItems;
    }

    @Override
    public int getCurrentMonthItem() {
        return getCurrentItem();
    }


    public void mo9517g2(int i) {
//        CalendarLayout calendarLayout;
//        BaseMonthView baseMonthView = (BaseMonthView) findViewWithTag(Integer.valueOf(i));
//        if (baseMonthView != null) {
//            int m = baseMonthView.getSelectedIndex(this.calendarViewDelegate.mIndexCalendar);
//            if (this.calendarViewDelegate.getSelectMode() == 0) {
//                baseMonthView.mCurrentItem = m;
//            }
//            if (m >= 0 && (calendarLayout = this.mParentLayout) != null) {
//                calendarLayout.updateSelectPosition(m);
//            }
//            baseMonthView.invalidate();
//        }
    }


    @Override
    public boolean isVisible() {
        return getVisibility() == VISIBLE;
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void updateItemHeight() {
        monthRecyclerViewAdapter.notifyDataSetChanged();
        mCurrentViewHeight = CalendarUtil.getMonthViewHeight(mDelegate.mIndexCalendar.getYear(),
                mDelegate.mIndexCalendar.getMonth(), mDelegate.getCalendarItemHeight(), mDelegate.getWeekStart(),
                mDelegate.getMonthViewShowMode());
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void updateWeekStart() {
        monthRecyclerViewAdapter.notifyDataSetChanged();
//        updateMonthViewHeight(mDelegate.mSelectedCalendar.getYear(), mDelegate.mSelectedCalendar.getMonth(), false);
        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        layoutParams.height = this.mCurrentViewHeight;
        setLayoutParams(layoutParams);
        if (mParentLayout != null) {
            mParentLayout.updateSelectWeek(CalendarUtil.getWeekFromDayInMonth(mDelegate.mSelectedCalendar, mDelegate.getWeekStart()));
        }
        updateSelected();
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void updateMonthViewClass() {
        monthRecyclerViewAdapter.notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void notifyDataSetChanged() {
        mMonthCount = (((mDelegate.getMaxYear() - mDelegate.getMinYear()) * 12) - mDelegate.getMinYearMonth()) + 1 + mDelegate.getMaxYearMonth();
        monthRecyclerViewAdapter.notifyDataSetChanged();
    }

    @Override
    public void notifyItemChanged(int i) {
        monthRecyclerViewAdapter.notifyItemChanged(i);
    }

    @Override
    public void updateScheme() {
        for (int i = 0; i < getChildCount(); i++) {
            ((BaseMonthView) getChildAt(i)).update();
        }
    }

    @Override
    public boolean isMonthListScrolling() {
        return isMonthListScrolling;
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void clearSelect() {
        monthRecyclerViewAdapter.notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void clearSelectRange() {
        monthRecyclerViewAdapter.notifyDataSetChanged();
    }

    @Override
    public void showMoreMonthView() {
        if (mParentLayout != null) {
            if (!mParentLayout.isExpand()) {
                mParentLayout.expand();
                return;
            }
            if (mCurrentItemHeight == 0) {
                mCurrentItemHeight = mDelegate.getCalendarItemHeight();
            }
            int height = mCurrentItemHeight;

            ValueAnimator ofInt = ValueAnimator.ofInt(new int[]{height, height * 2});
            ofInt.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    int childCount = getChildCount();
                    if (childCount > 1) {
                        for (int i = 1; i < childCount; i++) {
                            View child = getChildAt(i);
                            if (child != null && child.getTranslationY() == 0) {
                                child.setVisibility(VISIBLE);
                                child.setTranslationY(child.getHeight() / 2);
                                child.animate().translationY(0).setInterpolator(new LinearInterpolator()).setDuration(i == 1 ? 250 : 160).start();
                            } else {
                                return;
                            }
                        }
                    }
                }
            });
            ofInt.setDuration(180);
            ofInt.start();
            ValueAnimator ofInt2 = ValueAnimator.ofInt(new int[]{getHeight(), this.mParentLayout.getHeight()});
            ofInt2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    int intValue = ((Integer) valueAnimator.getAnimatedValue()).intValue();
                    ViewGroup.LayoutParams layoutParams = getLayoutParams();
                    layoutParams.height = intValue;
                    setLayoutParams(layoutParams);
                    linearSnapHelper.attachToRecyclerView(null);
                }
            });
            ofInt2.setDuration(180);
            ofInt2.start();
            isMonthListScrolling = true;
        }
    }

    @Override
    public void showSingleMonthView() {
        int currentMonthTop = 0;
        int currentIndex = 0;
        int childCount = getChildCount();
        boolean z = true;

        if (childCount >= 1) {

            for (int i = 0; i < childCount; i++) {
                BaseMonthView baseMonthView = (BaseMonthView) getChildAt(i);
                if (baseMonthView.isMonthDayTop()) {
                    currentIndex = ((Integer) baseMonthView.getTag()).intValue();
                    currentMonthTop = baseMonthView.getTop();
                }
            }
        } else {
            currentMonthTop = 0;
            currentIndex = 0;
        }

        smoothScrollBy(0, mDelegate.getMonthHeaderHeight() + currentMonthTop);
        mCurrentItem = currentIndex;

        postDelayed(new Runnable() {
            @Override
            public void run() {

                int i = 0;
                ValueAnimator ofInt = ValueAnimator.ofInt(new int[]{mParentLayout.getHeight(), getCurrentHeight()});
                ofInt.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        int intValue = ((Integer) valueAnimator.getAnimatedValue()).intValue();
                        ViewGroup.LayoutParams layoutParams = getLayoutParams();
                        layoutParams.height = intValue;
                        setLayoutParams(layoutParams);
                    }
                });
                ofInt.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        postDelayedAttachToRecyclerView(1);
                        onPageSelected(mCurrentItem, false);
                    }
                });
                ofInt.setDuration(180);
                ofInt.start();
                isMonthListScrolling = false;
                int childCount = getChildCount();
                if (childCount > 1) {
                    while (i < childCount) {
                        final View childAt = getChildAt(i);
                        if (childAt != null) {
                            if (((Integer) childAt.getTag()).intValue() != mCurrentItem) {
                                childAt.animate()
                                        .translationY((float) childAt.getHeight())
                                        .setInterpolator(new LinearInterpolator())
                                        .setDuration(250)
                                        .setListener(new AnimatorListenerAdapter() {
                                            @Override
                                            public void onAnimationEnd(Animator animation) {
                                                super.onAnimationEnd(animation);
                                                childAt.setTranslationY(0);
                                            }
                                        })
                                        .start();
                            }
                            i++;
                        } else {
                            return;
                        }
                    }
                }
            }
        },  currentMonthTop == -mDelegate.getMonthHeaderHeight() ? 0 : 200);
    }

    public void setParentLayout(CalendarLayout calendarLayout) {
        this.mParentLayout = calendarLayout;
    }

    public void setWeekBar(WeekBar weekBar) {
        this.weekBar = weekBar;
    }

    public void setWeekViewPager(WeekViewPager weekViewPager) {
        this.weekViewPager = weekViewPager;
    }

    @Override
    public void hideMonthView() {
        setVisibility(INVISIBLE);
    }

    @Override
    public void showMonthView() {
        setVisibility(VISIBLE);
    }

    @Override
    public void goneMonthView() {
        setVisibility(GONE);
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void updateSelected() {
        for (int i = 0; i < getChildCount(); i++) {
            BaseMonthView baseMonthView = (BaseMonthView) getChildAt(i);
            baseMonthView.setSelectedCalendar(mDelegate.mSelectedCalendar);
            baseMonthView.invalidate();
        }
        if (isMonthListScrolling) {
            monthRecyclerViewAdapter.notifyDataSetChanged();
        }
    }



    @Override
    public void setCurrentItem(int i, boolean z) {

        if (!this.f8704P0) {
            this.calendarLayoutManger.scrollToPositionWithOffset(i, -mDelegate.getMonthHeaderHeight());
            this.mCurrentItem = i;
            return;
        }
        this.linearSnapHelper.attachToRecyclerView((RecyclerView) null);
        if (Math.abs(getCurrentItem() - i) > 1) {
            this.calendarLayoutManger.scrollToPositionWithOffset(i, -mDelegate.getMonthHeaderHeight());
        } else {
            smoothScrollToPosition(i);
        }
        postDelayedAttachToRecyclerView(80);
        this.f8704P0 = false;
        this.mCurrentItem = i;

    }


    public void postDelayedAttachToRecyclerView(int delayMillis) {
        postDelayed(new Runnable() {
            @Override
            public void run() {
                linearSnapHelper.attachToRecyclerView(MonthRecyclerView.this);
            }
        }, delayMillis);
    }

    @Override
    public void closeMonthViewAnimation(Animation.AnimationListener animationListener) {
        int month = mDelegate.mIndexCalendar.getMonth();
        int height = getHeight();
        int width = getWidth();
        int scalingOffset = 0;
        int animationHeight = 0;

        if (month == 1) {
            animationHeight = height / 8;
        } else if(month == 2) {
            scalingOffset = width / 2;
            animationHeight = height / 8;
        } else if (month == 3) {
            scalingOffset = width;
            animationHeight = height / 8;
        } else if (month == 4) {
            animationHeight = (height / 4) * 3;
        } else if (month == 5) {
            scalingOffset = width / 2;
            animationHeight = (height / 4) * 3;
        } else if (month == 6) {
            scalingOffset = width;
            animationHeight = (height / 4) * 3;
        } else if (month == 7) {
            animationHeight = (height / 5) * 4;
        } else if (month == 8) {
            scalingOffset = width / 2;
            animationHeight = (height / 5) * 4;
        } else if (month == 9) {
            scalingOffset = width;
            animationHeight = (height / 5) * 4;
        } else if (month == 10) {
            animationHeight = height;
        } else if (month == 11) {
            scalingOffset = width / 2;
            animationHeight = height;
        } else if (month == 12) {
            scalingOffset = width;
            animationHeight = height;
        }

        ScaleAnimation scaleAnimation = new ScaleAnimation(0.0f, 1.0f, 0.0f, 1.0f, 0, (float) scalingOffset, 0, (float) animationHeight);
        scaleAnimation.setDuration(320);
        scaleAnimation.setAnimationListener(animationListener);
        clearAnimation();
        startAnimation(scaleAnimation);
    }

    class MonthLinearSnapHelper extends LinearSnapHelper {

        @Override
        public int[] calculateDistanceToFinalSnap(@NonNull LayoutManager layoutManager, @NonNull View view) {

            int k;
            final int intValue = ((Integer) view.getTag()).intValue();
            int[] outArray = super.calculateDistanceToFinalSnap(layoutManager, view);
            if (intValue == mCurrentItem || isMonthListScrolling || outArray == null || outArray.length < 2 || (k = CalendarUtil.getMonthViewHeight((((mCurrentItem + mDelegate.getMinYearMonth()) - 1) / 12) + mDelegate.getMinYear(), (((mCurrentItem + mDelegate.getMinYearMonth()) - 1) % 12) + 1, mDelegate.getCalendarItemHeight(), mDelegate.getWeekStart(), mDelegate.getMonthViewShowMode())) == view.getHeight() - mDelegate.getMonthHeaderHeight()) {
                return outArray;
            }
            outArray[1] = outArray[1] + (((k - view.getHeight()) - mDelegate.getMonthHeaderHeight()) / 2) + mDelegate.getMonthHeaderHeight();
            ValueAnimator ofInt = ValueAnimator.ofInt(new int[]{k, view.getHeight() - mDelegate.getMonthHeaderHeight()});
            ofInt.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    mCurrentItem = intValue;
                    int intValue = ((Integer) valueAnimator.getAnimatedValue()).intValue();
                    ViewGroup.LayoutParams layoutParams = MonthRecyclerView.this.getLayoutParams();
                    layoutParams.height = intValue;
                    setLayoutParams(layoutParams);
                }
            });
            ofInt.setDuration(250);
            ofInt.start();
            return outArray;
        }
    }

    class MonthRecyclerViewAdapter extends Adapter<MonthRecyclerViewAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            BaseMonthView baseMonthView;
            try {
                baseMonthView = (BaseMonthView) mDelegate.getMonthViewClass().getConstructor(new Class[]{Context.class}).newInstance(new Object[]{MonthRecyclerView.this.getContext()});
            } catch (Exception e) {
                e.printStackTrace();
                baseMonthView = null;
            }
            if (baseMonthView != null) {
                return new ViewHolder(baseMonthView);
            }
            throw new IllegalArgumentException("MonthView class path not exist");
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            BaseMonthView baseMonthView = holder.baseMonthView;

            MonthRecyclerView monthRecyclerView = MonthRecyclerView.this;
            baseMonthView.iMonthView = monthRecyclerView;
            baseMonthView.mParentLayout = monthRecyclerView.mParentLayout;
            baseMonthView.setup(monthRecyclerView.mDelegate);
            baseMonthView.setTag(Integer.valueOf(position));
            baseMonthView.initMonthWithDate((((MonthRecyclerView.this.mDelegate.getMinYearMonth() + position) - 1) / 12)
                            + MonthRecyclerView.this.mDelegate.getMinYear(),
                    (((MonthRecyclerView.this.mDelegate.getMinYearMonth() + position) - 1) % 12) + 1);
            baseMonthView.setSelectedCalendar(MonthRecyclerView.this.mDelegate.mSelectedCalendar);
        }

        @Override
        public int getItemCount() {
            return mMonthCount;
        }

        public class ViewHolder extends RecyclerView.ViewHolder {

            public BaseMonthView baseMonthView;

            public ViewHolder(BaseMonthView baseMonthView) {
                super(baseMonthView);
                this.baseMonthView = baseMonthView;
            }
        }
    }


}
