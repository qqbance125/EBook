package com.qihoo360.reader.ui.view;

import com.qihoo360.reader.R;
import com.qihoo360.reader.ui.channels.AddReaderLeftPaneAdapter;

import android.content.Context;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;

public class AddReaderLeftPane extends FrameLayout implements OnItemClickListener {

    private AddReaderLeftPaneAdapter mAdapter;
    private ListView mList;
    private Handler mHandler = new Handler();

    public AddReaderLeftPane(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public AddReaderLeftPane(Context context) {
        this(context, null);
    }

    private ImageView mView;

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.rd_add_reader_leftpane, this);
        mList = (ListView) findViewById(R.id.list1);
        mList.setOnItemClickListener(this);
        mView = (ImageView) findViewById(R.id.animation_view);
        mView.setVisibility(View.GONE);
    }

    public void setLeftPanAdapter(AddReaderLeftPaneAdapter adapter) {
        mAdapter = adapter;
        mList.setAdapter(mAdapter);
        mAdapter.setHighLightPosition(AddReaderLeftPaneAdapter.DEFAULT_HIGHLIGHT_POSITION);
        mList.setSelection(AddReaderLeftPaneAdapter.DEFAULT_HIGHLIGHT_POSITION);
    }

    private int mPrePosition = AddReaderLeftPaneAdapter.DEFAULT_HIGHLIGHT_POSITION;

    private int mPrePositionY = 0;

    @Override
    public void onItemClick(AdapterView<?> parent, final View view, final int position, long id) {

        if (mOnLeftItemSelectListener != null && position == mAdapter.getCount() - 1) {
            mOnLeftItemSelectListener.onItemSelected(position);
            return;
        }

        if (mPrePosition == position) {
            return;
        }
        mPrePosition = position;
        mAdapter.setHighLightPosition(position);
        View selectView = mAdapter.getSelectView();
        //        Log.e("info", "------------pre position:" + mAdapter.getPreHighLightPosition() + " now position:" + position);
        int start = 0;
        int target = 0;
        int location[] = new int[2];
        view.getLocationInWindow(location);
        Rect rect = new Rect();
        view.getWindowVisibleDisplayFrame(rect);
        target = location[1] - rect.top;
        if (mAdapter.getPreHighLightPosition() >= mList.getFirstVisiblePosition()
                        && mAdapter.getPreHighLightPosition() <= mList.getLastVisiblePosition()) {
            if (selectView != null) {
                selectView.findViewById(R.id.add_reader_root).setBackgroundResource(0);
                selectView.getLocationInWindow(location);
                start = location[1] - rect.top;
            }
        } else {
            if (mAdapter.getPreHighLightPosition() >= mList.getLastVisiblePosition()) {
                start = mList.getLastVisiblePosition() * view.getMeasuredHeight();
            } else if (mAdapter.getPreHighLightPosition() <= mList.getFirstVisiblePosition()) {
                start = 0;
            }
        }

        Animation animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0,
                        Animation.ABSOLUTE, start <= 0 ? mPrePositionY : start, Animation.ABSOLUTE, target <= 0 ? 0
                                        : target);
        //        Log.e("info", "view height:" + view.getMeasuredHeight() + "----------from:" + from + "  to:" + target);
        mPrePositionY = target;
        animation.setInterpolator(new DecelerateInterpolator(1.5f));
        animation.setDuration(200);
        mView.setVisibility(View.VISIBLE);
        mView.clearAnimation();
        mView.startAnimation(animation);
        animation.setAnimationListener(new AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mView.setVisibility(View.GONE);
                mAdapter.notifyDataSetChanged();
                if (mOnLeftItemSelectListener != null) {
                    mHandler.postDelayed(new Runnable() {

                        @Override
                        public void run() {

                            mOnLeftItemSelectListener.onItemSelected(position);
                        }
                    }, 80);
                }
            }
        });
    }

    public void setOnLeftItemSelectedListener(OnLeftItemSelectListener listener) {
        this.mOnLeftItemSelectListener = listener;
    }

    private OnLeftItemSelectListener mOnLeftItemSelectListener;

    public interface OnLeftItemSelectListener {
        public void onItemSelected(int position);
    }

    public int getSearchItemPosition() {
        return mAdapter.getCount() - 1;
    }

}
