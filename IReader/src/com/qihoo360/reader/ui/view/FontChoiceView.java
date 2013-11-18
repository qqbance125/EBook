package com.qihoo360.reader.ui.view;

import com.qihoo360.reader.R;
import com.qihoo360.reader.support.AnimationHelper;
import com.qihoo360.reader.ui.articles.ArticleDetailAdapter;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.LinearLayout;

public class FontChoiceView extends LinearLayout {

    public FontChoiceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public FontChoiceView(Context context) {
        this(context, null);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.rd_font_choice, this);
    }

    public void setVisibility(final int visibility, final boolean shouldAnimation) {
        if (shouldAnimation == false) {
            super.setVisibility(visibility);
        } else {
            Animation animation = null;
            if (visibility == View.GONE) {
                animation = AnimationHelper.animationComeOutOrIn(false);
                startAnimation(animation);
                animation.setAnimationListener(new AnimationListener() {

                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        setVisibility(visibility);
                    }
                });
            } else if (visibility == View.VISIBLE) {
                setVisibility(visibility);
                animation = AnimationHelper.animationComeOutOrIn(true);
                startAnimation(animation);
            } else {
                setVisibility(visibility);
            }

        }
    }

    public void changFontSelection(int textSize) {
        findViewById(R.id.small).setBackgroundResource(R.drawable.rd_font_toselect);
        findViewById(R.id.middle).setBackgroundResource(R.drawable.rd_font_toselect);
        findViewById(R.id.large).setBackgroundResource(R.drawable.rd_font_toselect);
        if (textSize == ArticleDetailAdapter.TEXT_SIZE_LARGE) {
            findViewById(R.id.large).setBackgroundResource(R.drawable.rd_font_selected);
        } else if (textSize == ArticleDetailAdapter.TEXT_SIZE_MIDDLE) {
            findViewById(R.id.middle).setBackgroundResource(R.drawable.rd_font_selected);
        } else if (textSize == ArticleDetailAdapter.TEXT_SIZE_SMALL) {
            findViewById(R.id.small).setBackgroundResource(R.drawable.rd_font_selected);
        }

    }
}
