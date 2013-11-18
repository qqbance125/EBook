package com.qihoo360.reader.support;

import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;

public class AnimationHelper {

    public static final int BOTTOM_MENU_ANIMATION_DURATION = 200;

    private static TranslateAnimation mTranslateRightInAnimation;
    private static TranslateAnimation mTranslateRightOutAnimation;
    private static TranslateAnimation mTranslateInAnimation;
    private static TranslateAnimation mTranslateOutAnimation;
    private static TranslateAnimation mTranslateLeftInAnimation;
    private static TranslateAnimation mTranslateLeftOutAnimation;
    private static TranslateAnimation mTranslateTopOutAnimation;
    private static TranslateAnimation mTranslateTopInAnimation;
    private static AlphaAnimation mAlphaFadeInAnimation;
    private static AlphaAnimation mAlphaFadeOutAnimation;
    private static TranslateAnimation mShowMoreTransAnimation;
    private static TranslateAnimation mHideMoreTransAnimation;
    private static RotateAnimation mRotateAnimationLeftVisible;
    private static RotateAnimation mRotateAnimationLeftHide;
    private static RotateAnimation mRotateAnimationRightVisible;
    private static RotateAnimation mRotateAnimationRightHide;
    private static ScaleAnimation mScaleAnimationHide;

    private static AccelerateDecelerateInterpolator mInterpolator;

    static {
        mTranslateTopOutAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                        Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, -1f, Animation.RELATIVE_TO_SELF,
                        0f);

        mTranslateTopInAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
                        0.0f, Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, -1f);

        mTranslateRightInAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                        Animation.RELATIVE_TO_SELF, 1.0f, Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF,
                        0f);
        mTranslateRightOutAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 1.0f,
                        Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF,
                        0f);
        mTranslateRightInAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                        Animation.RELATIVE_TO_SELF, 1.0f, Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF,
                        0f);
        mTranslateRightOutAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 1.0f,
                        Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF,
                        0f);

        mTranslateLeftInAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                        Animation.RELATIVE_TO_SELF, -1.0f, Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF,
                        0f);

        mTranslateLeftOutAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, -1.0f,
                        Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF,
                        0f);

        mShowMoreTransAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
                        0.0f, Animation.RELATIVE_TO_SELF, 0.33f, Animation.RELATIVE_TO_SELF, 0f);

        mHideMoreTransAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
                        0.0f, Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 1f);

        mTranslateInAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
                        0.0f, Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 1f);
        mTranslateOutAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
                        0.0f, Animation.RELATIVE_TO_SELF, 1f, Animation.RELATIVE_TO_SELF, 0f);

        mRotateAnimationLeftHide = new RotateAnimation(-90, 0, Animation.RELATIVE_TO_SELF, 0,
                        Animation.RELATIVE_TO_SELF, 1);
        mRotateAnimationLeftVisible = new RotateAnimation(0, -90, Animation.RELATIVE_TO_SELF, 0,
                        Animation.RELATIVE_TO_SELF, 1);
        mRotateAnimationLeftVisible.setDuration(BOTTOM_MENU_ANIMATION_DURATION);
        mRotateAnimationLeftHide.setDuration(BOTTOM_MENU_ANIMATION_DURATION);

        mRotateAnimationRightHide = new RotateAnimation(0, 90, Animation.RELATIVE_TO_SELF, 1,
                        Animation.RELATIVE_TO_SELF, 1);
        mRotateAnimationRightVisible = new RotateAnimation(90, 0, Animation.RELATIVE_TO_SELF, 1,
                        Animation.RELATIVE_TO_SELF, 1);
        mRotateAnimationRightHide.setDuration(BOTTOM_MENU_ANIMATION_DURATION);
        mRotateAnimationRightVisible.setDuration(BOTTOM_MENU_ANIMATION_DURATION);
        //        mScaleAnimationOut = new ScaleAnimation(0.0f, 1.0f, 0.0f, 1.0f,
        //                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
        //                1f);
        //        mScaleAnimationIn = new ScaleAnimation(1.0f, 0f, 1.0f, 0f,
        //                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
        //                1f);
        mAlphaFadeInAnimation = new AlphaAnimation(1f, 0f);
        mAlphaFadeOutAnimation = new AlphaAnimation(0f, 1f);
        mInterpolator = new AccelerateDecelerateInterpolator();
        mAlphaFadeInAnimation.setDuration(BOTTOM_MENU_ANIMATION_DURATION);
        mAlphaFadeOutAnimation.setDuration(BOTTOM_MENU_ANIMATION_DURATION);

        mShowMoreTransAnimation.setDuration(BOTTOM_MENU_ANIMATION_DURATION);
        mHideMoreTransAnimation.setDuration(BOTTOM_MENU_ANIMATION_DURATION);

        mScaleAnimationHide = new ScaleAnimation(1.0f, 0f, 1.0f, 0f, Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF, 0.5f);
        mScaleAnimationHide.setDuration(200);

    }

    public static Animation animationComeOutOrBackHorizontal(boolean isAppear, boolean isLeft) {
        AnimationSet set = new AnimationSet(true);
        if (!isAppear) {
            if (isLeft)
                set.addAnimation(mTranslateLeftInAnimation);
            else
                set.addAnimation(mTranslateRightInAnimation);
            set.addAnimation(mAlphaFadeInAnimation);
        } else {
            if (isLeft)
                set.addAnimation(mTranslateLeftOutAnimation);
            else
                set.addAnimation(mTranslateRightOutAnimation);
            set.addAnimation(mAlphaFadeOutAnimation);
        }
        set.setDuration(BOTTOM_MENU_ANIMATION_DURATION);
        set.setInterpolator(mInterpolator);
        return set;
    }
    /**
     * 进 或 出的动画
     */
    public static Animation animationComeOutOrIn(boolean isAppear) {
        AnimationSet set = new AnimationSet(true);
        if (!isAppear) {
            set.addAnimation(mTranslateInAnimation);
            set.addAnimation(mAlphaFadeInAnimation);
        } else {
            set.addAnimation(mTranslateOutAnimation);
            set.addAnimation(mAlphaFadeOutAnimation);
        }
        set.setDuration(BOTTOM_MENU_ANIMATION_DURATION);
        set.setInterpolator(mInterpolator);
        return set;
    }

    public static Animation animationComeOutOrInFromTop(boolean isAppear) {
        AnimationSet set = new AnimationSet(true);
        if (!isAppear) {
            set.addAnimation(mTranslateTopInAnimation);
        } else {
            set.addAnimation(mTranslateTopOutAnimation);
        }
        set.setDuration(BOTTOM_MENU_ANIMATION_DURATION);
        set.setInterpolator(mInterpolator);
        return set;
    }

    public static Animation animationAlphaFade(boolean isAppear) {
        if (!isAppear) {
            return mAlphaFadeInAnimation;
        } else {
            return mAlphaFadeOutAnimation;
        }
    }

    public static Animation animationMoreMenu(boolean isAppear) {
        if (isAppear) {
            return mShowMoreTransAnimation;
        } else {
            return mHideMoreTransAnimation;
        }
    }

    public static Animation animationRotationLeft(boolean isAppear) {
        if (isAppear) {
            return mRotateAnimationLeftHide;
        } else {
            return mRotateAnimationLeftVisible;
        }
    }

    public static Animation animationRotationRight(boolean isAppear) {
        if (isAppear) {
            return mRotateAnimationRightVisible;
        } else {
            return mRotateAnimationRightHide;
        }
    }

    public static Animation animationScaleHide() {
        return mScaleAnimationHide;
    }

}
