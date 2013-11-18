
package com.qihoo360.reader.support;

import com.qihoo360.reader.support.LinkifyUtil.OnLinkClickListener;
import com.qihoo360.reader.support.LinkifyUtil.Type;
import com.qihoo360.reader.ui.ContextDialog;
import com.qihoo360.reader.ui.ReaderPlugin;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class BaseOnLinkClickListener implements OnLinkClickListener {
    Context mContext;

    public BaseOnLinkClickListener(Context context) {
        mContext = context;
    }

    @Override
    public void OnLinkClick(final String text, Type type) {
        if (ContextDialog.isShowingDialog()) {
            return;
        }

        if (Type.phone == type) {
            // not supported for now
        } else if (Type.web == type) {
            ReaderPlugin.openLinkWithBrowser(mContext, text);
        } else if (Type.email == type) {
            Uri uri = Uri.parse("mailto:" + text);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            try {
                mContext.startActivity(intent);
            } catch (Exception e) {
                Utils.error(getClass(), Utils.getStackTrace(e));
            }
        }
    }
}
