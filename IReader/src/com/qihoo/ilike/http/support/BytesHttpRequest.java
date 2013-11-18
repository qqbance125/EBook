
package com.qihoo.ilike.http.support;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.http.HttpEntity;

import android.util.Log;

import com.qihoo.ilike.http.core.AbstractHttpReceiver;
import com.qihoo.ilike.http.core.AbstractHttpRequest;

/**
 * 处理字节的接受广播
 */
public abstract class BytesHttpRequest extends AbstractHttpRequest<byte[]> {
    @Override
    public AbstractHttpReceiver<byte[]> getHttpResponseReceiver() {
        return new AbstractHttpReceiver<byte[]>() {
            @Override
            public void onReceive(HttpEntity entity) throws IOException {
                InputStream is = entity.getContent();
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int readLength = 0;
                while ((readLength = is.read(buffer)) != -1) {
                    bos.write(buffer, 0, readLength);
                }
                response = bos.toByteArray();
            }
        };
    }

   

}
