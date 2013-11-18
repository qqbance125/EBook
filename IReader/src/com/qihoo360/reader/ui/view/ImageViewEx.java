
package com.qihoo360.reader.ui.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.widget.ImageView;

public class ImageViewEx extends ImageView {
    public static final String TAG = "ImageViewEx";

    private boolean isEmpty = false;
    Matrix mMatrix = new Matrix();
    Matrix mSavedMatrix = new Matrix();

    static final int NONE = 0;
    static final int DRAG = 1;
    static final int ZOOM = 2;
    int mMode = NONE;
    PointF mStartPt = new PointF();
    PointF mMidPt = new PointF();
    float mOldDist = 1f;

    private boolean mImageInBounds = true;

    private int mViewWidth = 0;
    private int mViewHeight = 0;
    private int mBitmapWidth = 0;
    private int mBitmapHeight = 0;

    private float mInitTranslateX = 0.0f;
    private float mInitTranslateY = 0.0f;
    private float mInitScaleFactor = 0.0f;

    private ImageTranlationAnimation mAnimation = null;
    private static final long mAnimationDuration = 300;

    private boolean mDoubleTapDetected = false;
    private GestureDetector mGestureDetector = new GestureDetector(
            new GestureDetector.SimpleOnGestureListener() {
                public boolean onDoubleTap(MotionEvent e) {
                    onDoubleTaped(e);
                    return true;
                };
            });

    public ImageViewEx(Context context) {
        super(context);
    }

    public ImageViewEx(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ImageViewEx(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setImageResource(int resId) {
        super.setImageResource(resId);

        mBitmapWidth = 0;
        mBitmapHeight = 0;
        setScaleType(ScaleType.CENTER_INSIDE);
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);

        setScaleType(ScaleType.MATRIX);
        if (bm != null) {
            isEmpty = false;
            mBitmapWidth = bm.getWidth();
            mBitmapHeight = bm.getHeight();
            applyInitMatrix(mBitmapWidth, mBitmapHeight);
        } else {
            isEmpty = true;
            mBitmapWidth = 0;
            mBitmapHeight = 0;
        }
    }

    private void applyInitMatrix(int imageWidth, int imageHeight) {
        int viewWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        int viewHeight = getHeight() - getPaddingTop() - getPaddingBottom();

        if (viewWidth <= 0 || viewHeight <= 0) {
            return;
        }

        mViewWidth = viewWidth;
        mViewHeight = viewHeight;

        float scale;
        int dx;
        int dy;
        if (imageWidth <= viewWidth && imageHeight <= viewHeight) {
            scale = 1.0f;
        } else {
            scale = Math.min((float) viewWidth / (float) imageWidth,
                    (float) viewHeight / (float) imageHeight);
        }

        dx = (int) ((viewWidth - imageWidth * scale) * 0.5f + 0.5f);
        dy = (int) ((viewHeight - imageHeight * scale) * 0.5f + 0.5f);

        Matrix matrix = new Matrix();
        matrix.setScale(scale, scale);
        matrix.postTranslate(dx, dy);
        setImageMatrix(matrix);

        mInitTranslateX = dx;
        mInitTranslateY = dy;
        mInitScaleFactor = scale;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (w > 0 && h > 0 && (w != oldw || h != oldh)
                && mBitmapWidth > 0 && mBitmapHeight > 0) {
            applyInitMatrix(mBitmapWidth, mBitmapHeight);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                mMatrix.set(getImageMatrix());
                mSavedMatrix.set(mMatrix);

                if (!mImageInBounds) {
                    mStartPt.set(event.getX(), event.getY());
                    mMode = DRAG;
                }

                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                if (!mDoubleTapDetected) {
                    mOldDist = spacing(event);
                    if (mOldDist > 10f) {
                        mSavedMatrix.set(mMatrix);
                        midPoint(mMidPt, event);
                        mMode = ZOOM;

                        requestDisallowInterceptTouchEvent(true);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (!mDoubleTapDetected) {
                    float[] values = getCurrentMatrixValues();
                    if (values[Matrix.MSCALE_X] <= mInitScaleFactor) {
                        bounceBack();
                        requestDisallowInterceptTouchEvent(false);
                    } else {
                        alignBorders(values);
                    }
                } else {
                    mDoubleTapDetected = false;
                }
            case MotionEvent.ACTION_POINTER_UP:
                mMode = NONE;
                break;
            case MotionEvent.ACTION_MOVE:
                if (mMode == DRAG) {
                    mMatrix.set(mSavedMatrix);
                    mMatrix.postTranslate(event.getX() - mStartPt.x, event.getY()
                                            - mStartPt.y);
                } else if (mMode == ZOOM) {
                    float newDist = spacing(event);
                    if (newDist > 10f) {
                        mMatrix.set(mSavedMatrix);
                        float scale = newDist / mOldDist;
                        mMatrix.postScale(scale, scale, mMidPt.x, mMidPt.y);
                    }
                }
                break;
        }

        if (mGestureDetector.onTouchEvent(event)) {
            mDoubleTapDetected = true;
            mMode = NONE;
        }

        if (mMode != NONE) {
            setImageMatrix(mMatrix);
        }
        return true;
    }

    public boolean isBrowsing() {
        return !isEmpty && (mMode != NONE || mDoubleTapDetected || !mImageInBounds);
    }

    private float[] getCurrentMatrixValues() {
        float[] values = new float[9];
        getImageMatrix().getValues(values);
        return values;
    }

    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return FloatMath.sqrt(x * x + y * y);
    }

    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    private void bounceBack() {
        ensureAnimation();
        float[] startValues = getCurrentMatrixValues();
        mAnimation.prepareAnimation(startValues[Matrix.MTRANS_X], startValues[Matrix.MTRANS_Y],
                mInitTranslateX, mInitTranslateY, startValues[Matrix.MSCALE_X], mInitScaleFactor,
                0, 0);

        startAnimation(mAnimation);
    }

    protected void onDoubleTaped(MotionEvent e) {
        ensureAnimation();

        float[] values = getCurrentMatrixValues();
        if (values[Matrix.MSCALE_X] != mInitScaleFactor) {
            bounceBack();
            requestDisallowInterceptTouchEvent(false);
        } else {
            mAnimation.prepareAnimation(mInitScaleFactor, Math.max(mInitScaleFactor * 3, 1.0f),
                    (int) e.getX(), (int) e.getY());
            startAnimation(mAnimation);
            requestDisallowInterceptTouchEvent(true);
        }
    }

    private void alignBorders(float[] values) {
        PointF p = new PointF();
        calculateAlign(values, p);

        if (p.x != values[Matrix.MTRANS_X] || p.y != values[Matrix.MTRANS_Y]) {
            ensureAnimation();

            float scaleFactor = values[Matrix.MSCALE_X];
            mAnimation.prepareAnimation(values[Matrix.MTRANS_X], values[Matrix.MTRANS_Y],
                    p.x, p.y, scaleFactor, scaleFactor, 0, 0);

            startAnimation(mAnimation);
        }
    }

    private void calculateAlign(float[] values, PointF p) {
        int viewWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        int viewHeight = getHeight() - getPaddingTop() - getPaddingBottom();

        if (viewWidth <= 0 || viewHeight <= 0) {
            return;
        }

        float dx = values[Matrix.MTRANS_X];
        float dy = values[Matrix.MTRANS_Y];
        float scaleFactor = values[Matrix.MSCALE_X];

        int imageWidth = (int) (mBitmapWidth * scaleFactor + 0.5f);
        int imageHeight = (int) (mBitmapHeight * scaleFactor + 0.5f);

        if (imageWidth < viewWidth) {
            dx = (int) ((viewWidth - imageWidth) * 0.5f + 0.5f);
        } else {
            float translateX = values[Matrix.MTRANS_X];
            float rightMargin = imageWidth + translateX - viewWidth;
            if (translateX > 0) {
                dx += -Math.min(translateX, rightMargin);
            } else if (rightMargin < 0) {
                dx += Math.abs(Math.max(translateX, rightMargin));
            }
        }

        if (imageHeight < viewHeight) {
            dy = (int) ((viewHeight - imageHeight) * 0.5f + 0.5f);
        } else {
            float translateY = values[Matrix.MTRANS_Y];
            float bottomMargin = imageHeight + translateY - viewHeight;
            if (translateY > 0) {
                dy += -Math.min(translateY, bottomMargin);
            } else if (bottomMargin < 0) {
                dy += Math.abs(Math.max(translateY, bottomMargin));
            }
        }

        p.x = dx;
        p.y = dy;
    }

    private void ensureAnimation() {
        if (mAnimation == null) {
            mAnimation = new ImageTranlationAnimation();
            mAnimation.setInterpolator(new DecelerateInterpolator());
            mAnimation.setDuration(mAnimationDuration);
        }
    }

    private class ImageTranlationAnimation extends Animation {
        private static final int MODE_SCALE = 1;
        private static final int MODE_BOUNCE_BACK = 2;

        private int mMode = 0;
        private float mStartTranslateX = 0;
        private float mStartTranslateY = 0;
        private float mTargetTranslateX = 0;
        private float mTargetTranslateY = 0;

        private float mStartScaleFactor = 0.0f;
        private float mTargetScaleFactor = 0.0f;
        private int mScalePivotX = 0;
        private int mScalePivotY = 0;

        private Matrix mSavedMatrixForScale = new Matrix();

        public void prepareAnimation(float startX, float startY, float targetX, float targetY,
                float startScaleFactor, float targetScaleFactor, int pivotX, int pivotY) {
            mMode = MODE_BOUNCE_BACK;
            mStartTranslateX = startX;
            mStartTranslateY = startY;
            mTargetTranslateX = targetX;
            mTargetTranslateY = targetY;

            mStartScaleFactor = startScaleFactor;
            mTargetScaleFactor = targetScaleFactor;
            mScalePivotX = pivotX;
            mScalePivotY = pivotY;
        }

        public void prepareAnimation(float startScaleFactor, float targetScaleFactor, int pivotX,
                int pivotY) {
            mMode = MODE_SCALE;
            mSavedMatrixForScale.set(getImageMatrix());
            mStartScaleFactor = startScaleFactor;
            mTargetScaleFactor = targetScaleFactor;
            mScalePivotX = pivotX;
            mScalePivotY = pivotY;
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation transformation) {
            Matrix matrix = new Matrix();
            if (mMode == MODE_BOUNCE_BACK) {
                float scaleFactor = mTargetScaleFactor + (mStartScaleFactor - mTargetScaleFactor)
                            * ((float) (1 - interpolatedTime));
                matrix.setScale(scaleFactor, scaleFactor, mScalePivotX, mScalePivotY);

                int dx = (int) (mTargetTranslateX + (mStartTranslateX - mTargetTranslateX)
                            * ((float) (1 - interpolatedTime)));
                int dy = (int) (mTargetTranslateY + (mStartTranslateY - mTargetTranslateY)
                            * ((float) (1 - interpolatedTime)));
                matrix.postTranslate(dx, dy);
            } else if (mMode == MODE_SCALE) {
                matrix.set(mSavedMatrixForScale);
                float scaleFactor = 1 + (mTargetScaleFactor / mStartScaleFactor - 1)
                        * interpolatedTime;
                matrix.postScale(scaleFactor, scaleFactor, mScalePivotX, mScalePivotY);

                float[] values = new float[9];
                matrix.getValues(values);
                PointF p = new PointF();
                calculateAlign(values, p);
                matrix.postTranslate(p.x - values[Matrix.MTRANS_X], p.y - values[Matrix.MTRANS_Y]);
            }

            setImageMatrix(matrix);
        }
    }

    private void requestDisallowInterceptTouchEvent(boolean value) {
        ViewParent parent = getParent();

        if (parent != null) {
            ((ViewGroup) parent).requestDisallowInterceptTouchEvent(value);
        }
    }

    @Override
    public void setImageMatrix(Matrix matrix) {
        super.setImageMatrix(matrix);

        float[] values = new float[9];
        matrix.getValues(values);
        float scaleFactor = values[Matrix.MSCALE_X];

        mImageInBounds = (int) (mBitmapWidth * scaleFactor) <= mViewWidth
                && (int) (mBitmapHeight * scaleFactor) <= mViewHeight;
    }
}
