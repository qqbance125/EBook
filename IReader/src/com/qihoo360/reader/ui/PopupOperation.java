/**
 *
 */

package com.qihoo360.reader.ui;

import com.qihoo360.reader.R;
import com.qihoo360.reader.Settings;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.PopupWindow;
import android.widget.TextView;

/**
 * 当文章左上角的按钮时，弹出的窗口
 *
 * @author Jiongxuan Zhang
 */
public class PopupOperation {

    private Activity mActivity;
    private View mParentView;
    private PopupWindow mPopupOperation;
    private TextView mNoPictureView;
    private TextView mNeedPictureView;


    /**章左上角的按钮时，弹出的窗口
     * 初始化
     *
     * @param activity
     */
    public PopupOperation(Activity activity, View parentView) {
        mActivity = activity;
        mParentView = parentView;
        mParentView.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (isShowing()) {
                    dismiss();
                } else {
                    show();
                }
            }
        });
    }

    /**
     * 立即显示它
     */
    public void show() {
        if (mPopupOperation == null) {
            initPopupOperation();
        }

        dismiss();
        udpateRadioView();
        mPopupOperation.showAsDropDown(mParentView, -11, 0);
    }

    /**
     * 立即让它消失，返回false表示弹窗并没有弹出
     */
    public boolean dismiss() {
        if (mPopupOperation != null && mPopupOperation.isShowing()) {
            udpateRadioView();
            mPopupOperation.dismiss();
            return true;
        }

        return false;
    }

    /**
     * 看弹窗是否已经显示
     *
     * @return
     */
    public boolean isShowing() {
        return mPopupOperation != null && mPopupOperation.isShowing();
    }

    private void initPopupOperation() {
        View pop_view = LayoutInflater.from(mActivity).inflate(R.layout.rd_article_popup_operation, null, false);
        mPopupOperation = new PopupWindow(pop_view, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        mPopupOperation.setOutsideTouchable(true);
        mPopupOperation.setFocusable(false);

        mNoPictureView = (TextView) pop_view.findViewById(R.id.no_picture);
        mNoPictureView.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if ((mPopupOperation != null) && mPopupOperation.isShowing()) {
                    mPopupOperation.dismiss();
                }
                Settings.setNoPicMode(mActivity, true);
            }
        });

        mNeedPictureView = (TextView) pop_view.findViewById(R.id.need_picture);
        mNeedPictureView.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if ((mPopupOperation != null) && mPopupOperation.isShowing()) {
                    mPopupOperation.dismiss();
                }
                Settings.setNoPicMode(mActivity, false);
            }
        });
    }

    private void udpateRadioView() {
        if (Settings.isNoPicMode()) {
            mNoPictureView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.rd_popup_radio_yes, 0, 0, 0);
            mNeedPictureView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.rd_popup_radio_no, 0, 0, 0);
        } else {
            mNoPictureView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.rd_popup_radio_no, 0, 0, 0);
            mNeedPictureView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.rd_popup_radio_yes, 0, 0, 0);
        }
    }
}
