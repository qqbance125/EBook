package com.qihoo360.reader.ui;

import com.qihoo.ilike.ui.ILikeArticleDetailActivity;
import com.qihoo.ilike.ui.ILikeImageChannelActivity;
import com.qihoo360.reader.R;
import com.qihoo360.reader.ReaderMainPageActivity;
import com.qihoo360.reader.listener.CommonListener;
import com.qihoo360.reader.listener.OnVisibilityChangeListener;
import com.qihoo360.reader.support.AnimationHelper;
import com.qihoo360.reader.ui.articles.ArticleDetailActivity;
import com.qihoo360.reader.ui.articles.ArticleReadActivity;
import com.qihoo360.reader.ui.channels.AddReaderActivity;
import com.qihoo360.reader.ui.channels.ArticleCollectionActivity;
import com.qihoo360.reader.ui.channels.SubscribedActivity;
import com.qihoo360.reader.ui.imagechannel.ImageChannelActivity;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.LinearLayout;

public class ReaderBottomMenubar extends LinearLayout implements OnClickListener {

    public ReaderBottomMenubar(Context context, AttributeSet attrs) {
        super(context, attrs);

        init(context);
    }

    public ReaderBottomMenubar(Context context) {
        this(context, null);
    }

    public void init(Context context) {
        if (context instanceof AddReaderActivity) {
            LayoutInflater.from(context).inflate(R.layout.rd_reader_bottom_bar2, this);
        } else if (context instanceof ArticleReadActivity) {
            LayoutInflater.from(context).inflate(R.layout.rd_reader_bottom_bar3, this);
        } else if (context instanceof ILikeArticleDetailActivity) {
            LayoutInflater.from(context).inflate(R.layout.ilike_bottom_bar_article_detail, this);
        } else if (context instanceof ArticleDetailActivity) {
            LayoutInflater.from(context).inflate(R.layout.rd_reader_bottom_bar4, this);
        } else if (context instanceof ArticleCollectionActivity) {
            LayoutInflater.from(context).inflate(R.layout.rd_reader_bottom_bar5, this);
        } else if (context instanceof ReaderMainPageActivity) {
            LayoutInflater.from(context).inflate(R.layout.rd_reader_bottom_bar, this);
        } else if (context instanceof SubscribedActivity) {
            LayoutInflater.from(context).inflate(R.layout.rd_reader_bottom_bar2, this);
        } else if (context instanceof ILikeImageChannelActivity) {
            LayoutInflater.from(context).inflate(R.layout.ilike_bottom_bar_image_channel, this);
        } else if (context instanceof ImageChannelActivity) {
            LayoutInflater.from(context).inflate(R.layout.rd_reader_bottom_bar6, this);
        }
        for (int i = 0; i < this.getChildCount(); i++) {
            this.getChildAt(i).setOnClickListener(this);
        }
        this.setBackgroundResource(R.drawable.rd_bg_menu_level_1);
        this.setGravity(Gravity.CENTER);
        findViewById(R.id.menu_full_screen).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                ReaderBottomMenubar.this.setVisibility(View.GONE, true);
            }
        });
        this.setClickable(true);
    }

    @Override
    public void onClick(View v) {
        if (mListener != null) {
            mListener.actionPerform(v.getId());
        }
    }

    private CommonListener mListener;

    public void setClickListener(CommonListener onClickListener) {
        this.mListener = onClickListener;
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (mVisibilityChangeListener != null) {
            mVisibilityChangeListener.onVisibilityChange(visibility);
        }
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
            }
        }
    }

    private OnVisibilityChangeListener mVisibilityChangeListener;

    public void setOnFullScreenClickListener(OnVisibilityChangeListener listener) {
        this.mVisibilityChangeListener = listener;
    }

}
