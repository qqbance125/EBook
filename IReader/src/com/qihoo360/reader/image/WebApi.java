package com.qihoo360.reader.image;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;

import android.accounts.NetworkErrorException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class WebApi{
   
    public static InputStream getInputStreamFromUrl(String url) throws NetworkErrorException, ClientProtocolException, IOException {
		InputStream content = null;
		int statusCode=0 ;
		HttpClient httpclient = new DefaultHttpClient();
		httpclient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 20000);
		HttpResponse response = httpclient.execute(new HttpGet(url));
		statusCode=response.getStatusLine().getStatusCode();
		content = response.getEntity().getContent();
		if(statusCode!=200){
			return null;
		}
		return content;
	}
    
    public static byte[] getByteEntityFromUrl(String url) throws NetworkErrorException, ClientProtocolException, IOException {
    	InputStream content = null;
    	HttpClient httpclient = new DefaultHttpClient();
    	httpclient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 20000);
		HttpResponse response = httpclient.execute(new HttpGet(url));
		content = response.getEntity().getContent();
		byte[] b = getBytes(content);
		content.close();
		return b;
	}
    
    public static String getStringFromUrl(String url) throws NetworkErrorException, ClientProtocolException, IOException {
    	InputStream content = null;
    	HttpClient httpclient = new DefaultHttpClient();
    	httpclient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 20000);
		HttpResponse response = httpclient.execute(new HttpGet(url));
		content = response.getEntity().getContent();
		String  str = new String(getBytes(content));
		content.close();
		return str;
	}
    
    public static byte[] getBytes(InputStream is) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] b = new byte[1024];
		int len = 0;
		while ((len = is.read(b, 0, 1024)) != -1) {
			baos.write(b, 0, len);
			baos.flush();
		}
		baos.close();
		is.close();
		byte[] bytes = baos.toByteArray();
		return bytes;
	}
}