package com.qihoo360.reader.ui;

import com.qihoo360.reader.R;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.TextView;

public class ReaderListPreference extends android.preference.ListPreference {
    private LayoutInflater mInflater;
    private CharSequence[] mEntries;
    private CharSequence[] mEntryValues;
    private int mClickedDialogEntryIndex;

    public ReaderListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            convertView = mInflater.inflate(R.layout.rd_pref_entry_radio, null);
            CheckedTextView view = (CheckedTextView) convertView.findViewById(R.id.checkedtext);
            view.setChecked(mClickedDialogEntryIndex == position);
            ((TextView) convertView.findViewById(R.id.txt)).setText(mEntries[position]);
            //          view.setBackgroundResource(R.drawable.c_dialog_text_1);
            return convertView;
        }
    };

    private DialogInterface.OnClickListener mOnClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                if (callChangeListener((String) mEntryValues[mClickedDialogEntryIndex])) {
                    ReaderListPreference.this.setValueIndex(mClickedDialogEntryIndex);
                }
            }
            dialog.dismiss();
        }
    };

    private Dialog mDialog;

    public Dialog getDialog() {
        if (mDialog == null) {
            mDialog = new Dialog(this.getContext(), R.style.dialog);
            mDialog.setTitle(getDialogTitle());
            mDialog.setContentView(R.layout.rd_custom_context_dialog);
        }
        return mDialog;
    };

    @Override
    protected void onClick() {
        // bind entries
        mEntries = getEntries();
        mEntryValues = getEntryValues();
        mClickedDialogEntryIndex = findIndexOfValue(getValue());
        final Dialog mDialog = getDialog();
        ((TextView) mDialog.findViewById(R.id.title)).setText(getDialogTitle());
        ListView list = (ListView) mDialog.findViewById(R.id.list);
        list.setAdapter(mDialogAdapter);
        list.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mClickedDialogEntryIndex = position;
                mDialogAdapter.notifyDataSetChanged();
                if (callChangeListener((String) mEntryValues[mClickedDialogEntryIndex])) {
                    ReaderListPreference.this.setValueIndex(mClickedDialogEntryIndex);
                }
                mDialog.dismiss();
            }
        });
        mDialog.show();
    }
}
