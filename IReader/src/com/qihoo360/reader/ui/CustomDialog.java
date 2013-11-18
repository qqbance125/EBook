package com.qihoo360.reader.ui;

import com.qihoo360.reader.R;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

public class CustomDialog extends Dialog {

    private TextView mTitle;
    private FrameLayout mCustom;
    private Button mPositiveButton;
    private DialogInterface.OnClickListener mPositiveButtonListener;
    private Button mNegativeButton;
    private DialogInterface.OnClickListener mNegativeButtonListener;
    private Button mNeutralButton;

    // Runtime env
    protected TextView mMessage;

    public CustomDialog(Context context) {
        super(context);
        init();
    }

    private final void init() {
        getContext().setTheme(R.style.dialog_theme);
        setContentView(R.layout.rd_common_custom_dialog);

        View root = findViewById(R.id.root);
        Display display = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int width = Math.min(display.getWidth(), display.getHeight());
        root.getLayoutParams().width = width;

        mTitle = (TextView) this.findViewById(R.id.title);
        mCustom = (FrameLayout) this.findViewById(R.id.custom);
        mPositiveButton = (Button) this.findViewById(R.id.button1);
        mPositiveButton.setOnClickListener(mButtonOnClickListener);
        mPositiveButton.setVisibility(View.GONE);
        mNegativeButton = (Button) this.findViewById(R.id.button2);
        mNegativeButton.setOnClickListener(mButtonOnClickListener);
        mNegativeButton.setVisibility(View.GONE);
        mNeutralButton = (Button) this.findViewById(R.id.button3);
        mNeutralButton.setOnClickListener(mButtonOnClickListener);
        mNeutralButton.setVisibility(View.GONE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    // Title
    public void setTitle(int resid) {
        mTitle.setText(resid);
    }

    public void setTitle(CharSequence text) {
        mTitle.setText(text);
    }

    /**
     * Handy method if you just want to display a message as the content of the dialog
     * @param resid String resource id with parameters
     * @param formatArgs parameter values
     */
    public void setMessage(int resid, Object... formatArgs) {
        String msg = this.getContext().getResources().getString(resid, formatArgs);
        setMessage(msg);
    }

    /**
     * Handy method if you just want to display a message as the content of the dialog
     * @param resid String resource id
     */
    public void setMessage(int resid) {
        String msg = this.getContext().getResources().getString(resid);
        setMessage(msg);
    }

    /**
     * Handy method if you just want to display a message as the content of the dialog
     * @param message String to be displayed
     */
    public void setMessage(CharSequence message) {
        if (null == mMessage) {
            mMessage = new TextView(this.getContext());
            mMessage.setTextColor(Color.WHITE);
            mMessage.setTextSize(20);
            //mMessage.setBackgroundResource(R.drawable.c_dialog_text);
            mMessage.setPadding(20, 40, 10, 40);
            mCustom.addView(mMessage);
        }
        mMessage.setText(message);
    }

    private View.OnClickListener mButtonOnClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (v.getId() == R.id.button1 && mPositiveButtonListener != null) {
                mPositiveButtonListener.onClick(CustomDialog.this, DialogInterface.BUTTON_POSITIVE);
            }
            if (v.getId() == R.id.button2 && mNegativeButtonListener != null) {
                mNegativeButtonListener.onClick(CustomDialog.this, DialogInterface.BUTTON_NEGATIVE);
            }
        }
    };

    /**
     * When positive button is clicked, the "which" value passed on to the listener is DialogInterface.BUTTON_POSITIVE
     * @param resid The string to display on the positive button
     * @param listener
     */
    public void setPositiveButton(int resid, OnClickListener listener) {
        mPositiveButton.setText(resid);
        mPositiveButton.setVisibility(View.VISIBLE);
        mPositiveButtonListener = listener;
    }

    /**
     * When negative button is clicked, the "which" value passed on to the listener is DialogInterface.BUTTON_NEGATIVE
     * @param resid The string to display on the negative button
     * @param listener
     */
    public void setNegativeButton(int resid, OnClickListener listener) {
        mNegativeButton.setText(resid);
        mNegativeButton.setVisibility(View.VISIBLE);
        mNegativeButtonListener = listener;
    }

    public void setNeutralButton(int resid, OnClickListener listener) {
        mNegativeButton.setText(resid);
        mNegativeButton.setVisibility(View.VISIBLE);
        mNegativeButtonListener = listener;
    }

    public void addContentView(int resid) {
        getLayoutInflater().inflate(resid, mCustom);
    }

    public void addContentView(View view) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        addContentView(view, lp);
    }

    public void addContentView(View view, FrameLayout.LayoutParams lp) {
        mCustom.addView(view, lp);
    }

    /**
     * Set the minmum width or/and height of the dialog, pass -1 to ignore either dimension.
     * If minHeight is set, extra space will be given to content area.
     * @param minWidth
     * @param minHeight
     */
    public final void setMinSize(int minWidth, int minHeight) {
        View view = findViewById(R.id.root);
        if (minWidth != -1) {
            view.setMinimumWidth(minWidth);
        }
        if (minHeight != -1) {
            view.setMinimumHeight(minHeight);
        }
    }

    public void showDilemiterLineAboveButton(boolean show) {
        findViewById(R.id.delimiter_line_above_button).setVisibility(View.GONE);
    }
}
