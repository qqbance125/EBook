package com.qihoo.ilike.http.support;

import com.qihoo.ilike.http.core.AbstractHttpReceiver;
import com.qihoo.ilike.http.core.AbstractHttpRequest;

import org.apache.http.HttpEntity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
/**
 * 文件下载的类
 */
public abstract class FileHttpRequest extends AbstractHttpRequest<File> {
	@Override
	public AbstractHttpReceiver<File> getHttpResponseReceiver() {
		return new AbstractHttpReceiver<File>() {
			@Override
			public void onReceive(HttpEntity entity) throws IOException {
				int readCount;
				OutputStream os = null;
				File tempFile;
				try {
					tempFile = getFile(getTempAbsolutePath());
					os = new FileOutputStream(tempFile);
					byte[] buffer = new byte[1024];
					InputStream in = entity.getContent();
					while ((readCount = in.read(buffer)) != -1) {
						os.write(buffer, 0, readCount);
					}
				} finally {
					if (os != null) {
						try {
							os.close();
						} catch (Exception e) {
						}
					}
				}
				File file = getFile(getFileAbsolutePath());
				if (tempFile != null && tempFile.renameTo(file))
					response = file;
			}
		};
	}

	private File getFile(String fileName) {
		File file = new File(fileName);
		if (file.exists())
		{
			file.delete();
		}
		else
		{
			makeDirs(fileName);
		}
		return file;
	}

	private void makeDirs(String fileName) {
		int idx = fileName.lastIndexOf("/");
		if (idx > 0) {
			File file = new File(fileName.substring(0, idx));
			if (!file.exists()) {
				synchronized (getClass())
				{
					if (!file.exists())
						file.mkdirs();
				}
			}
		}
	}

	public abstract String getTempAbsolutePath();

	public abstract String getFileAbsolutePath();
}
