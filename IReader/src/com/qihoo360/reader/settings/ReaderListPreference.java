package com.qihoo360.reader.settings;

import com.qihoo360.reader.R;
import com.qihoo360.reader.Settings;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class ReaderListPreference extends LinearLayout implements OnClickListener {

    private TextView mTitle, mSummary;
    private String mKey;
    private LayoutInflater mInflater;
    private String[] mEntries;
    private String[] mEntryValues;
    private int mClickedDialogEntryIndex;
    private Context mContext;

    public ReaderListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);

    }

    public ReaderListPreference(Context context) {
        this(context, null);
    }

    private void init(Context context) {
        mContext = context;
        mInflater = LayoutInflater.from(context);
        mInflater.inflate(R.layout.rd_custom_list_preference, this);
        mTitle = (TextView) findViewById(R.id.title);
        mSummary = (TextView) findViewById(R.id.summary);
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
     * 设置弹出dialog entries
     * @param entries
     */
    public void setEntries(String[] entries) {
        if (entries != null) {
            this.mEntries = entries;
        }
    }

    /***
     * 设置dialog entries values
     * @param values
     */
    public void setValues(int values) {
        this.mEntryValues = mContext.getResources().getStringArray(values);
    }

    /***
     * 设置弹出dialog entries
     * @param entries
     */
    public void setEntries(int entries) {
        this.mEntries = mContext.getResources().getStringArray(entries);
    }

    /***
     * 设置dialog entries values
     * @param values
     */
    public void setValues(String[] values) {
        if (values != null) {
            this.mEntryValues = values;
        }
    }

    /***
     * 设置当前preference的key
     * @param key
     */
    public void setKey(String key) {
        mKey = key;
    }

    /***
     * 设置上次选择的项
     * @param value
     * preference里保存的值
     */
    public void setSelectItem(String value) {
        if (mEntryValues != null) {
            for (int i = 0; i < mEntryValues.length; i++) {
                if (value.equalsIgnoreCase(mEntryValues[i])) {
                    mClickedDialogEntryIndex = i;
                    setSummary(mEntries[i]);
                    return;
                }
            }
        }
    }

    private BaseAdapter mDialogAdapter = new BaseAdapter() {
        @Override
        public int getCount() {
            return (null != mEntries) ? mEntries.length : 0;
        }

        @Override
        public Object getItem(int position) {
            return (null != mEntries) ? mEntries[position] : null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            convertView = mInflater.inflate(R.layout.rd_pref_entry_radio, null);
            CheckedTextView view = (CheckedTextView) convertView.findViewById(R.id.checkedtext);
            view.setChecked(mClickedDialogEntryIndex == position);
            ((TextView) convertView.findViewById(R.id.txt)).setText(mEntries[position]);
            return convertView;
        }
    };

    private Dialog mDialog;

    public Dialog getDialog() {
        if (mDialog == null) {
            mDialog = new Dialog(this.getContext(), R.style.dialog);
            mDialog.setTitle(mTitle.getText());
            mDialog.setContentView(R.layout.rd_custom_context_dialog);
        }
        return mDialog;
    };

    @Override
    public void onClick(View v) {
        // bind entries
        final Dialog mDialog = getDialog();
        ((TextView) mDialog.findViewById(R.id.title)).setText(mTitle.getText());
        ListView list = (ListView) mDialog.findViewById(R.id.list);
        list.setAdapter(mDialogAdapter);
        list.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mClickedDialogEntryIndex = position;
                mDialogAdapter.notifyDataSetChanged();

                if (mKey != null) {
                    Settings.setString(mKey, mEntryValues[position]);
                    setSummary(mEntries[position]);
                }

                mDialog.dismiss();
            }
        });
        mDialog.show();
        mDialog.setOnDismissListener(new OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
                if (mListener != null) {
                    mListener.onDismiss();
                }
            }
        });
    }

    public interface OnListPreferenceDialogDismissListener {
        public void onDismiss();
    }

    private OnListPreferenceDialogDismissListener mListener = null;

    public void setListPreferenceDialogDismissListener(OnListPreferenceDialogDismissListener listener) {
        this.mListener = listener;
    }
}
