package com.qihoo360.reader.ui;

import com.qihoo360.reader.R;
import com.qihoo360.reader.Settings;
import com.qihoo360.reader.listener.CommonListener;
import com.qihoo360.reader.listener.OnVisibilityChangeListener;
import com.qihoo360.reader.support.AnimationHelper;
import com.qihoo360.reader.ui.ReaderMenu.OnMenuClickListener;
import com.qihoo360.reader.ui.articles.ArticleDetailActivity;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;

public class ReaderMenuContainer extends FrameLayout implements OnClickListener {

    private ReaderMenu mMenuContainer;
    private ReaderBottomMenubar mBottomMenubar;
    private FrameLayout mWrapper;
    private boolean mInitFullScreenState;
    private View mStandaloneBtnContainer = null;

    public ReaderMenuContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ReaderMenuContainer(Context context) {
        this(context, null);
    }

    public void init(Context context) {
        if (context instanceof ArticleDetailActivity) {
            LayoutInflater.from(context).inflate(R.layout.rd_reader_menu_layout4, this);
        } else {
            LayoutInflater.from(context).inflate(R.layout.rd_reader_menu_layout, this);
        }

        mMenuContainer = (ReaderMenu) findViewById(R.id.reader_menu);

        mBottomMenubar = (ReaderBottomMenubar) findViewById(R.id.bottom_bar);
        mWrapper = (FrameLayout) findViewById(R.id.wrapper);

        /*if (context instanceof ArticleDetailActivity) {
            if (Settings.getArticleDetailBarVisibilityState()) {
                mBottomMenubar.setVisibility(View.VISIBLE);
            } else {
                mBottomMenubar.setVisibility(View.GONE);
            }
        } else {*/
        mInitFullScreenState = Settings.isFullScreen();
        if (mInitFullScreenState) {
            mBottomMenubar.setVisibility(View.GONE);
            mBottomBarVisible = false;
        } else {
            mBottomMenubar.setVisibility(View.VISIBLE);
            mBottomBarVisible = true;
        }
        //        }

        mMenuContainer.setOnVisibilityChangeListener(new OnVisibilityChangeListener() {

            @Override
            public void onVisibilityChange(int visibility) {
                mWrapper.setVisibility(visibility);
            }
        });

        mStandaloneBtnContainer = findViewById(R.id.standalone_btn_container);
        findViewById(R.id.menu_controller).setOnClickListener(this);
        findViewById(R.id.standalone_btn_back).setOnClickListener(this);
        mStandaloneBtnContainer.setVisibility(mBottomBarVisible ? View.GONE : View.VISIBLE);

        mWrapper.setOnClickListener(this);

        mBottomMenubar.setOnFullScreenClickListener(new OnVisibilityChangeListener() {

            @Override
            public void onVisibilityChange(int visibility) {
                if (visibility == View.VISIBLE) {
                    //                    mBottomBarVisible = true;
                    mStandaloneBtnContainer.setVisibility(View.GONE);
                } else {
                    if (mMenuContainer.getVisibility() == View.VISIBLE) {
                        mMenuContainer.setVisibility(View.GONE);
                    }
                    mBottomBarVisible = false;
                    mStandaloneBtnContainer.setVisibility(View.VISIBLE);
                    mStandaloneBtnContainer.startAnimation(AnimationHelper.animationAlphaFade(true));
                    if (mControllBtnClickListener != null) {
                        mControllBtnClickListener.onControllClick(View.GONE);
                    }
                }
            }
        });

        mMenuContainer.setOnMenuClickListener(new OnMenuClickListener() {

            @Override
            public void onMenuClick() {
                hideMenuOrBottomBar();
            }
        });
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.menu_controller) {
            //            onControllerBtnClick();
            mBottomMenubar.setVisibility(View.VISIBLE, true);
            mBottomBarVisible = true;
            if (mControllBtnClickListener != null) {
                mControllBtnClickListener.onControllClick(View.VISIBLE);
            }
        } else if (v.getId() == R.id.standalone_btn_back && getContext() instanceof Activity) {
            ((Activity)getContext()).finish();
        } else if (v.getId() == R.id.wrapper) {
            setMenuGoneWithState();
        }
    }

    public void onMenuClick() {
        if (mMenuContainer.getVisibility() == View.GONE) {
            //            if (getContext() instanceof ImageChannelActivity == false) {
            if (mBottomMenubar.getVisibility() == View.GONE) {
                setBottomBarVisibility(View.VISIBLE);
            }
            //            }
            mMenuContainer.setVisibility(View.VISIBLE, true);
        } else if (mMenuContainer.getVisibility() == View.VISIBLE) {
            setMenuGoneWithState();
        }
    }

    private void setMenuGoneWithState() {
        mMenuContainer.setVisibility(View.GONE, true);
        restoreBottomBarState();
    }

    public void restoreBottomBarState() {
        if (!mBottomBarVisible) {
            mBottomMenubar.setVisibility(View.GONE, true);
        }
    }

    public boolean onBackBtnPressed() {
        if (mMenuContainer.getVisibility() == View.VISIBLE) {
            setMenuGoneWithState();
            return true;
        }
        return false;
    }

    private boolean mBottomBarVisible;

    private void onControllerBtnClick() {
        if (mBottomMenubar.getVisibility() == View.VISIBLE) {
            if (mMenuContainer.getVisibility() == View.VISIBLE) {
                mMenuContainer.setVisibility(View.GONE, true);
                mBottomMenubar.setVisibility(View.GONE, true);
            } else {
                mBottomMenubar.setVisibility(View.GONE, true);
            }

            if (mControllBtnClickListener != null) {
                mControllBtnClickListener.onControllClick(View.GONE);
            }

            mBottomBarVisible = false;

        } else if (mBottomMenubar.getVisibility() == View.GONE) {
            mBottomMenubar.setVisibility(View.VISIBLE, true);
            mBottomBarVisible = true;
            if (mControllBtnClickListener != null) {
                mControllBtnClickListener.onControllClick(View.VISIBLE);
            }
        }

    }

    public void hideMenuOrBottomBar() {
        if (mMenuContainer.getVisibility() == View.VISIBLE) {
            mMenuContainer.setVisibility(View.GONE);
            if (mBottomBarVisible == false) {
                mBottomMenubar.setVisibility(View.GONE);
            }
        }
    }

    public void hideMenu() {
        if (mMenuContainer.getVisibility() == View.VISIBLE) {
            mMenuContainer.setVisibility(View.GONE);
        }
    }

    public void setBottomBarListener(CommonListener commonListener) {
        if (commonListener != null) {
            mBottomMenubar.setClickListener(commonListener);
        }
    }

    public void setMenuClickListener(CommonListener commonListener) {
        if (commonListener != null) {
            mMenuContainer.setMenuClickListener(commonListener);
        }
    }

    public void updateMenuStatus() {
        mMenuContainer.updateState();
    }

    private OnControllBtnClickListener mControllBtnClickListener;

    public void setOnControllBtnClick(OnControllBtnClickListener listener) {
        this.mControllBtnClickListener = listener;
    }

    public interface OnControllBtnClickListener {
        public void onControllClick(int visibility);
    }

    public void saveBottomBarState() {
        boolean b = mBottomMenubar.getVisibility() == View.VISIBLE ? false : true;
        /*if (getContext() instanceof ArticleDetailActivity) {
            Settings.setArticleDetailBarVisibilityState(b);
        } else {*/
        if (b != mInitFullScreenState) {
            mInitFullScreenState = b;
            Settings.setFullScreen(b, true);
        }
        //        }
    }

    public void onResume() {
        mMenuContainer.updateState();
        if (Settings.isFullScreen()) {
            setBottomBarVisibility(View.GONE);
        } else {
            setBottomBarVisibility(View.VISIBLE);
        }
    }

    /***
     * 获取menu按钮点击之前的bottomBar状态
     * @return
     */
    public void setBottomBarVisibility() {
        if (!mBottomBarVisible) {
            setBottomBarVisibility(View.GONE);
        }
    }

    public void setBottomBarVisibility(int visibility) {
        mBottomMenubar.setVisibility(visibility);
    }

    public int getBottomBarVisibility() {
        return mBottomMenubar.getVisibility();
    }
}
