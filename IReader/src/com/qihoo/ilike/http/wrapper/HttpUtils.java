package com.qihoo.ilike.http.wrapper;

import java.net.URLEncoder;
import java.util.Map;

public class HttpUtils 
{
	public static String getCookie(Map<String, String> cookies)
	{
		String value = null;
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, String> entry : cookies.entrySet()) 
		{
			sb.append(entry.getKey()).append("=");
			try 
			{
				value = URLEncoder.encode(entry.getValue(), "UTF-8");
			} 
			catch (Exception e) 
			{
				value = "";
			}
			sb.append(value).append(";");
		}
		return sb.toString();
	}
}
