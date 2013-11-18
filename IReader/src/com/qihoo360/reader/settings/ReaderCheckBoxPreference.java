package com.qihoo360.reader.settings;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.qihoo360.reader.R;
import com.qihoo360.reader.Settings;
import com.qihoo360.reader.support.Utils;

/***
 * checkbox 设置
 * @author tangzhihui
 *
 */
public class ReaderCheckBoxPreference extends LinearLayout implements OnClickListener {

    private TextView mTitle, mSummary;
    private CheckBox mCheckBox;
    private String mKey;
    private Context mContext;

    public ReaderCheckBoxPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ReaderCheckBoxPreference(Context context) {
        this(context, null);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.rd_custom_checkbox_preference, this);
        mContext = context;
        mTitle = (TextView) findViewById(R.id.title);
        mSummary = (TextView) findViewById(R.id.summary);
        mCheckBox = (CheckBox) findViewById(R.id.checkbox);
        this.setOnClickListener(this);
    }

    public void setTitle(String title) {
        if (TextUtils.isEmpty(title) == false) {
            mSummary.setText(title);
        }
    }

    public void setTitle(int title) {
        mTitle.setText(mContext.getResources().getString(title));
    }

    public void setSummary(String summary) {
        if (TextUtils.isEmpty(summary) == false) {
            mSummary.setVisibility(View.VISIBLE);
            mSummary.setText(summary);
        }
    }

    public void setSummary(int summary) {
        mSummary.setVisibility(View.VISIBLE);
        mSummary.setText(mContext.getResources().getString(summary));
    }

    /***
     *设置checkbox的初始状态
     * @param b
     */
    public void setOriginalChecked(boolean b) {
        mCheckBox.setChecked(b);
    }

    /***
     * 设置当前checkbox的key
     * @param key
     */
    public void setKey(String key) {
        mKey = key;
    }

    @Override
    public void onClick(View v) {
        boolean b = !mCheckBox.isChecked();
        mCheckBox.setChecked(b);
        if (mKey != null) {
            Settings.setBoolean(mKey, b);
        } else {
            try {
                new IllegalAccessError("key should be setted");
            } catch (Exception e) {
                Utils.error(getClass(), Utils.getStackTrace(e));
            }
        }
    }

}
