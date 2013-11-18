package com.qihoo360.reader.ui;

import com.qihoo360.browser.hip.UXHelper;
import com.qihoo360.browser.hip.UXHelperConfig;
import com.qihoo360.reader.R;
import com.qihoo360.reader.Settings;
import com.qihoo360.reader.listener.CommonListener;
import com.qihoo360.reader.listener.OnVisibilityChangeListener;
import com.qihoo360.reader.settings.ReaderPreferencePage;
import com.qihoo360.reader.subscription.reader.RssManager;
import com.qihoo360.reader.support.AnimationHelper;
import com.qihoo360.reader.ui.articles.ArticleDetailActivity;
import com.qihoo360.reader.ui.articles.ArticleReadActivity;
import com.qihoo360.reader.ui.channels.ArticleCollectionActivity;
import com.qihoo360.reader.ui.imagechannel.ImageChannelActivity;
import com.qihoo360.reader.ui.offline.OfflineActivity;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ReaderMenu extends LinearLayout implements OnClickListener {

    private TextView mMenuCollectBtn, mNightModeBtn, mNoPicModeBtn, mFontBtn;

    public ReaderMenu(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ReaderMenu(Context context) {
        this(context, null);
    }

    private void init(final Context context) {
        LayoutInflater.from(context).inflate(R.layout.rd_reader_menu_detail, this);
        this.setBackgroundResource(R.drawable.rd_menu_content_bg);
        this.setOrientation(VERTICAL);
        mMenuCollectBtn = (TextView) findViewById(R.id.menu_add_bookmark);
        mNightModeBtn = (TextView) findViewById(R.id.menu_night_mode);
        mNoPicModeBtn = (TextView) findViewById(R.id.menu_photo_gone);
        mFontBtn = (TextView) findViewById(R.id.menu_text_size);
        //        mOfflineLoadingBtn = (ImageView) findViewById(R.id.menu_offline_loading);

        updateState();

        //        mOfflineLoadingBtn.setOnClickListener(mCommonListener);
        mFontBtn.setOnClickListener(this);
        findViewById(R.id.menu_add_bookmark).setOnClickListener(this);
        mNightModeBtn.setOnClickListener(mCommonListener);
        mNoPicModeBtn.setOnClickListener(mCommonListener);
        findViewById(R.id.menu_my_favour).setOnClickListener(mCommonListener);
        findViewById(R.id.menu_settings).setOnClickListener(mCommonListener);
        findViewById(R.id.menu_clear).setOnClickListener(mCommonListener);
        //        findViewById(R.id.menu_quit).setOnClickListener(mCommonListener);
        //        findViewById(R.id.menu_feedback).setOnClickListener(mCommonListener);
        findViewById(R.id.menu_offline).setOnClickListener(mCommonListener);
        findViewById(R.id.menu_my_favour).setEnabled(false);

    }

    private CommonListener mListener;

    public void setMenuClickListener(CommonListener listener) {
        this.mListener = listener;
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

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (mVisibilityChangeListener != null) {
            mVisibilityChangeListener.onVisibilityChange(visibility);
        }
        updateMenuItemView();
    }

    private OnClickListener mCommonListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            final Context context = v.getContext();
            int id = v.getId();
            if (id == R.id.menu_night_mode) {
                UXHelper.getInstance().addActionRecord(UXHelperConfig.Reader_Menu_Nightly_Mode, 1);
                if (context instanceof Activity && context instanceof Nightable) {
                    Settings.setNightMode((Activity) context, !Settings.isNightMode());
                    updateMenuItemView();
                }
            } else if (id == R.id.menu_my_favour) {
                UXHelper.getInstance().addActionRecord(UXHelperConfig.Reader_Menu_MyArticle_OnClick, 1);
                Intent intent = new Intent(context, ArticleCollectionActivity.class);
                intent.putExtra("back2browser", false);
                context.startActivity(intent);
            }/* else if (id == R.id.menu_feedback) {
                ReaderPlugin.openLinkWithBrowser(context, CommonUtil.getFeedbackUrlString(context));
             }*/else if (id == R.id.menu_offline) {
                UXHelper.getInstance().addActionRecord(UXHelperConfig.Reader_Menu_Offline_DownList_OnClick, 1);
                OfflineActivity.startActivity(getContext(), false);
            } else if (id == R.id.menu_photo_gone) {
                if (context instanceof Activity) {
                    boolean currentMode = Settings.isNoPicMode();
                    Settings.setNoPicMode((Activity) context, !currentMode);
                    updateMenuItemView();
                    UXHelper.getInstance().addActionRecord(
                                    currentMode == true ? UXHelperConfig.Reader_Menu_No_Image
                                                    : UXHelperConfig.Reader_Menu_Show_Image, 1);
                }
            } else if (id == R.id.menu_clear) {
                UXHelper.getInstance().addActionRecord(UXHelperConfig.Reader_Menu_Clear_History, 1);
                final CustomDialog dialog = new CustomDialog(getContext());
                dialog.setTitle(R.string.rd_clear_all_tips);
                dialog.setMessage(R.string.rd_should_clear_all_tips);
                dialog.setPositiveButton(R.string.rd_dialog_ok, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        RssManager.clearCache(getContext());
                        if (getContext() instanceof ArticleDetailActivity
                                        || getContext() instanceof ArticleReadActivity
                                        || getContext() instanceof ImageChannelActivity) {
                            ActivityBase.closeAll();
                        }
                        dialog.dismiss();
                    }
                });
                dialog.setNegativeButton(R.string.rd_dialog_cancel, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        dialog.dismiss();
                    }
                });
                dialog.show();
            } else if (id == R.id.menu_settings) {
                UXHelper.getInstance().addActionRecord(UXHelperConfig.Reader_Menu_Reader_Setting, 1);
                context.startActivity(new Intent(context, ReaderPreferencePage.class));
            } /*else if (id == R.id.menu_quit) {
                UXHelper.getInstance().addActionRecord(UXHelperConfig.Reader_Menu_Exit_Reader, 1);
                ActivityBase.closeAll();
                ((Activity) context).finish();
              }*/
            if (mMenuClickListener != null) {
                mMenuClickListener.onMenuClick();
            }
        }
    };

    public void updateState() {
        mNightModeBtn.setCompoundDrawablesWithIntrinsicBounds(0,
                        Settings.isNightMode() == false ? R.drawable.rd_menu_item_our_nightmode_icon
                                        : R.drawable.rd_menu_item_our_daymode_icon, 0, 0);
        mNightModeBtn.setText(Settings.isNightMode() == false ? R.string.rd_night_mode : R.string.rd_day_mode);
        mNoPicModeBtn.setText(Settings.isNoPicMode() == false ? R.string.rd_no_image_mode : R.string.rd_image_mode);

        mNoPicModeBtn.setCompoundDrawablesWithIntrinsicBounds(0,
                        Settings.isNoPicMode() == false ? R.drawable.rd_menu_item_our_noimage_mode_icon
                                        : R.drawable.rd_menu_item_our_image_mode_icon, 0, 0);
        mMenuCollectBtn.setEnabled((Activity) getContext() instanceof ArticleDetailActivity);
        mFontBtn.setEnabled((Activity) getContext() instanceof ArticleDetailActivity);
        findViewById(R.id.menu_my_favour).setEnabled(
                        (Activity) getContext() instanceof ArticleCollectionActivity ? false : true);
    }

    private OnVisibilityChangeListener mVisibilityChangeListener;

    public void setOnVisibilityChangeListener(OnVisibilityChangeListener listener) {
        this.mVisibilityChangeListener = listener;
    }

    @Override
    public void onClick(View v) {
        if (mListener != null) {
            mListener.actionPerform(v.getId());
        }
    }

    private void updateMenuItemView() {
        mNightModeBtn.setCompoundDrawablesWithIntrinsicBounds(0,
                        Settings.isNightMode() == false ? R.drawable.rd_menu_item_our_nightmode_icon
                                        : R.drawable.rd_menu_item_our_daymode_icon, 0, 0);
        mNightModeBtn.setText(Settings.isNightMode() == false ? "夜间模式" : "日间模式");
        mNoPicModeBtn.setText(Settings.isNoPicMode() == false ? "无图模式" : "有图模式");

        mNoPicModeBtn.setCompoundDrawablesWithIntrinsicBounds(0,
                        Settings.isNoPicMode() == false ? R.drawable.rd_menu_item_our_noimage_mode_icon
                                        : R.drawable.rd_menu_item_our_image_mode_icon, 0, 0);
    }

    private OnMenuClickListener mMenuClickListener;

    public interface OnMenuClickListener {
        public void onMenuClick();
    }

    public void setOnMenuClickListener(OnMenuClickListener listener) {
        mMenuClickListener = listener;
    }

}
