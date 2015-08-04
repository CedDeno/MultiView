package io.apptik.widget.multiview.scalablerecyclerview;


import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import io.apptik.widget.multiview.layoutmanagers.ViewPagerLayoutManager;


public class ScalableRecyclerViewOld extends RecyclerView {

    private static String TAG = ScalableRecyclerViewOld.class.getName();

    public static final float DEFAULT_MAX_VIEWITEM_SCALE_FACTOR = 10f;
    public static final float DEFAULT_MAX_GRID_SCALE_FACTOR = 4f;
    public static final int ZOOM_ANIMATION_DURATION_MS = 200;

    private ScaleGestureDetector mScaleDetector;
    private GestureDetector mGestureDetector;
    private volatile float mOldScaleFactor = 1.f;
    private volatile float mScaleFactor = 1.f;
    private volatile boolean mWillNotScaleMore = false;

    private AnimatorSet mFlingAnimator;
    private AnimatorSet mPanAnimator;
    private ValueAnimator mZoomAnimator;


    //the point of the view that is touched for scaling
    //this one will be used to move to
    private volatile float mFromX;
    private volatile float mFromY;
    private volatile boolean mIsScaling = false;
    private volatile int mFromPos = -1;

    private GridLayoutManager mLayoutManagerGrid;
    private ViewPagerLayoutManager mLayoutManagerSingle;


    public ScalableRecyclerViewOld(Context context) {
        super(context);
        init(context);
    }

    public ScalableRecyclerViewOld(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ScalableRecyclerViewOld(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    @Override
    public void setLayoutManager(LayoutManager layout) {
        if (!GridLayoutManager.class.isAssignableFrom(layout.getClass())) {
            throw new RuntimeException("GridLayoutManager required.");
        }
        mLayoutManagerGrid = (GridLayoutManager) layout;
        if (super.getLayoutManager() != mLayoutManagerSingle) {
            //doScale(getItemPosition(-1, -1));
        }
    }

    private void init(Context context) {
        mLayoutManagerGrid = new GridLayoutManager(getContext(), 3);
        mLayoutManagerSingle = new ViewPagerLayoutManager(getContext()).withPageChangeListener(
                new ViewPagerLayoutManager.PageChangeListener() {
                    @Override
                    public void onPageChanging(int currPage, int newPage) {
                        Log.d(TAG, "page changing: " + currPage + ":" + newPage);
                        if (currPage > -1) {
                            View curentView = mLayoutManagerSingle.findViewByPosition(currPage);
                            resetItemScale(curentView);
                            mOldScaleFactor = mScaleFactor = getMaxGridScaleFactor() + 0.001f;
                            //doScale(newPage);
                        }
                    }

                    @Override
                    public void onPageChanged(int prevPage, int newPage) {

                    }
                }
        );

        super.setLayoutManager(mLayoutManagerGrid);
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());

        mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                View curentView = getChildAt(0);
                panAnimate(curentView, distanceX, distanceY);
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                //View curentView = getChildAt(0);
                //panAnimate(curentView, velocityX*20, velocityY*20);
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (mScaleFactor < 10f + getMaxGridScaleFactor()) {
                    mScaleFactor = 10f + getMaxGridScaleFactor();
                } else {
                    mScaleFactor = getMaxGridScaleFactor() + 0.001f;
                }

                mFromX = e.getX();
                mFromY = e.getY();
                doScale(getItemPosition(mFromX, mFromY));
                return true;
            }

            @Override
            public boolean onDoubleTapEvent(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {

            }

            @Override
            public void onShowPress(MotionEvent e) {

            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return true;
            }
        });


    }


    //TODO do we use     ScaleGestureDetector at all ?

    //todo
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // Let the ScaleGestureDetector inspect all events.
        final int action = MotionEventCompat.getActionMasked(ev);
        // Always handle the case of the touch gesture being complete.

        if (mIsScaling) {
            if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
                doScale(mFromPos);
                // Release the scale.
                mIsScaling = false;
                mFromPos = -1;
                if (mWillNotScaleMore) {
                    mWillNotScaleMore = false;
                }
            }
        }


        mScaleDetector.onTouchEvent(ev);
        if (mIsScaling) return true;
        if (isZoomedInSingle()) {
            //if (mGestureDetector.onTouchEvent(ev))
            return mGestureDetector.onTouchEvent(ev);
        }

        return super.onTouchEvent(ev);
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (!isInViewPagerMode() && mIsScaling) {
            float newScale = mScaleFactor - mOldScaleFactor + 1;
            canvas.scale(newScale, newScale, mFromX, mFromY);
        } else {
            canvas.scale(1, 1);
        }
        super.onDraw(canvas);
    }

    protected float getMaxViewItemScaleFactor() {
        return DEFAULT_MAX_VIEWITEM_SCALE_FACTOR;
    }

    protected float getMaxGridScaleFactor() {
        return DEFAULT_MAX_GRID_SCALE_FACTOR;
    }

    protected float getMinGridScaleFactor() {
        return 1 / DEFAULT_MAX_GRID_SCALE_FACTOR;
    }

    protected int getGridSpanCount(float scaleFactor) {
        if (scaleFactor < 1) {
            return 3 + Math.round(1 / scaleFactor);
        } else if (scaleFactor < 2) {
            return 3;
        } else if (scaleFactor < 3) {
            return 2;
        } else if (scaleFactor <= getMaxGridScaleFactor()) {
            return 1;
        } else {
            //not grid anymore
            return -1;
        }
    }

//    protected float getScaleFactorForSpanCount(int spanCount) {
//        if(spanCount>3) {
//            return 0.5f;
//        } else if(spanCount==3) {
//            return 1f;
//        } else if(spanCount==2) {
//            return 1.5f;
//        } else if(spanCount==1) {
//            return 2.5f;
//        } else {
//            return getMaxGridScaleFactor()+0.001f;
//        }
//    }

    private synchronized void doScale(int currPos) {
        Log.d(TAG, "doScale : " + currPos + " :: " + mOldScaleFactor + "/" + mScaleFactor);
        //resetItemScale(this);
        //we are in grid
        if (mScaleFactor <= getMaxGridScaleFactor()) {


            //check if we are coming from zoomed state
            if (mOldScaleFactor > getMaxGridScaleFactor() + 0.001f) {
                mWillNotScaleMore = true;
                mScaleFactor = getMaxGridScaleFactor() + 0.001f;
                zoomItem(currPos);
            } //check if we are coming form single item layout
            else {
                int newRowSpan = getGridSpanCount(mScaleFactor);
                if (mLayoutManagerGrid.getSpanCount() != newRowSpan) {
                    mLayoutManagerGrid.setSpanCount(newRowSpan);
                    mLayoutManagerGrid.requestLayout();
                }

                if (mOldScaleFactor > getMaxGridScaleFactor()) {
                    super.setLayoutManager(mLayoutManagerGrid);
                    getRecycledViewPool().clear();
                    scrollToPosition(currPos);
                }
            }
        } else {
            //check if we are coming from gridlayout
            if (mOldScaleFactor <= getMaxGridScaleFactor()) {
                super.setLayoutManager(mLayoutManagerSingle);
                getRecycledViewPool().clear();
                scrollToPosition(currPos);
                mScaleFactor = getMaxGridScaleFactor() + 0.001f;
                mWillNotScaleMore = true;
            }


            if (!mWillNotScaleMore && mScaleFactor >= (getMaxGridScaleFactor() + 0.001f)) {
                Log.d(TAG, "doScale in zoomed : ");
                zoomItem(currPos);
            }
        }


        if (mWillNotScaleMore) {
            mOldScaleFactor = mScaleFactor = getMaxGridScaleFactor() + 0.001f;
        } else if (isZoomedInSingle()) {
            mOldScaleFactor = mScaleFactor;
        } else if (isInViewPagerMode()) {
            mOldScaleFactor = getMaxGridScaleFactor() + 0.001f;
        } else {
            mOldScaleFactor = mScaleFactor;
            // mOldScaleFactor = getScaleFactorForSpanCount(layoutManagerGrid.getSpanCount());
        }
    }

    private void zoomItem(int currPos) {
        // zoom view with factor absolute mScaleFactor
        float viewScaleFactor = mScaleFactor + 1 - getMaxGridScaleFactor();
        float viewOldScaleFacotor = mOldScaleFactor + 1 - getMaxGridScaleFactor();
        if (viewOldScaleFacotor < 1) {
            viewOldScaleFacotor = 1f;
        }
        // View curentView = layoutManagerSingle.findViewByPosition(currPos);
        View curentView = getCenterItem();
        //View curentView = this.findViewHolderForAdapterPosition(currPos).itemView;
        Log.d(TAG, "scale view : " + viewScaleFactor + " /  " + curentView.getScaleX() + ":" + curentView.getScaleY());
        zoomAnimate(curentView, viewOldScaleFacotor, viewScaleFactor);
    }

    public View getCenterItem() {
        int middleX = (int) (this.getX() + (this.getWidth() * this.getScaleX()) / 2);
        int middleY = (int) (this.getY() + (this.getHeight() * this.getScaleY()) / 2);
        View v = this.findChildViewUnder(middleX, middleY);
        return v;
    }

    public boolean isInViewPagerMode() {
        return mScaleFactor > getMaxGridScaleFactor();
    }

    public boolean isZoomedInSingle() {
        return mScaleFactor > (getMaxGridScaleFactor() + 0.001f);
    }

    private void panAnimate(final View currentView, float distanceX, float distanceY) {
        if (mPanAnimator != null) {
            mPanAnimator.end();
        }
        ValueAnimator panAnimatorX = ValueAnimator.ofFloat(currentView.getTranslationX(), currentView.getTranslationX() - distanceX);
        ValueAnimator panAnimatorY = ValueAnimator.ofFloat(currentView.getTranslationY(), currentView.getTranslationY() - distanceY);
        panAnimatorX.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float translation = (Float) animation.getAnimatedValue();
                currentView.setTranslationX(translation);
                //  currentView.setScrollX((int) translation);

            }
        });
        panAnimatorY.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float translation = (Float) animation.getAnimatedValue();
                currentView.setTranslationY(translation);
                //currentView.setScrollX((int) translation);
            }
        });

        mPanAnimator = new AnimatorSet();
        mPanAnimator.play(panAnimatorX).with(panAnimatorY);
        mPanAnimator.setDuration(ZOOM_ANIMATION_DURATION_MS);
        mPanAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mPanAnimator = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mPanAnimator = null;

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });

        mPanAnimator.start();
    }

    private void zoomAnimate(final View currentView, float fromScaleFactor, float toScaleFactor) {
        if (mZoomAnimator != null) {
            mZoomAnimator.end();
        }
        mZoomAnimator = new ValueAnimator();
        mZoomAnimator.setFloatValues(fromScaleFactor, toScaleFactor);
        mZoomAnimator.setDuration(ZOOM_ANIMATION_DURATION_MS);
        mZoomAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mZoomAnimator = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mZoomAnimator = null;

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        mZoomAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float newScaleFactor = (Float) animation.getAnimatedValue();
                currentView.setPivotX(mFromX + currentView.getTranslationX());
                currentView.setPivotY(mFromY + currentView.getTranslationY());
                currentView.setScaleX(newScaleFactor);
                currentView.setScaleY(newScaleFactor);

            }
        });
        mZoomAnimator.start();
    }

    void resetItemScale(View view) {
        if (view == null) return;
        view.setScaleX(1f);
        view.setScaleY(1f);
        view.setTranslationX(0f);
        view.setTranslationY(0f);
    }

    private class ScaleListener
            extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return super.onScaleBegin(detector);
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            super.onScaleEnd(detector);
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            Log.d(TAG, "scale factor: " + mScaleFactor + ", detector: " + detector.getScaleFactor());
            if (!mIsScaling) {
                mIsScaling = true;
                mFromX = detector.getFocusX();
                mFromY = detector.getFocusY();
                mFromPos = getItemPosition(mFromX, mFromY);
            }
            if (!mWillNotScaleMore) {
                mScaleFactor *= detector.getScaleFactor();
                Log.d(TAG, "scale factor after: " + mScaleFactor);
                // Don't let the object get too small or too large.
                mScaleFactor = Math.max(getMinGridScaleFactor(), Math.min(mScaleFactor, getMaxGridScaleFactor() + getMaxViewItemScaleFactor()));
                Log.d(TAG, "scale factor end: " + mScaleFactor);

                if (isInViewPagerMode()) {
                    // doScale(getItemPosition(mFromX, mFromY));
                    doScale(mFromPos);
                } else {
                    if (mLayoutManagerGrid.getSpanCount() != getGridSpanCount(mScaleFactor)) {
                        doScale(mFromPos);
                    } else {
                        invalidate();
                        //zoomAnimate(ScalableRecyclerView.this, mOldScaleFactor, mScaleFactor);
                    }
                }
            }

            return true;
        }
    }

    private int getItemPosition(float x, float y) {
        return getChildAdapterPosition(findChildViewUnder(x, y));
        //return ViewUtils.getItemPositionAtPoint(this, x, y);
    }
}
