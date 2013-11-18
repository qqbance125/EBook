package com.qihoo.ilike.ui;

import com.qihoo.ilike.subscription.IlikeArticle;
import com.qihoo.ilike.util.Constants;
import com.qihoo360.reader.R;
import com.qihoo360.reader.Settings;
import com.qihoo360.reader.image.BitmapFactoryBase;
import com.qihoo360.reader.image.BitmapHelper;
import com.qihoo360.reader.image.ImageUtils;
import com.qihoo360.reader.subscription.Article;
import com.qihoo360.reader.subscription.Channel;
import com.qihoo360.reader.support.ArithmeticUtils;
import com.qihoo360.reader.support.AsyncTaskEx;
import com.qihoo360.reader.support.FileUtils;
import com.qihoo360.reader.support.Utils;
import com.qihoo360.reader.ui.articles.ArticleDetailAdapter;
import com.qihoo360.reader.ui.articles.ArticleUtils;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.Handler;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class ILikeArticleDetailAdapter extends ArticleDetailAdapter {
    public static final String TAG = "ILikeArticleDetailAdapter";
    public HashMap<Long, Integer> mLikedCountCache = null;

    public ILikeArticleDetailAdapter(Context context, Cursor c) {
        super(context, c);
    }

    @Override
    public void reference(Channel channel, Handler handler, boolean isCollection) {
        if(channel.type == Channel.TYPE_ILIKE_MY_COLLECTION) {
            mLikedCountCache = new HashMap<Long, Integer>();
        }

        super.reference(channel, handler, isCollection);
    }

    @Override
    protected View inflateListHeaderView(Context context) {
        View header = LayoutInflater.from(context).inflate(R.layout.ilike_article_detail_list_header, null);
        if(Settings.isNightMode()) {
            Resources res = context.getResources();
            ((TextView) header.findViewById(R.id.ilike_author)).setTextColor(res.getColor(R.color.rd_night_text));
            ((TextView) header.findViewById(R.id.ilike_album)).setTextColor(res.getColor(R.color.rd_night_text));
        }
        return header;
    }

    @Override
    protected View inflateListFooterView(Context context) {
        return LayoutInflater.from(context).inflate(R.layout.ilike_article_detail_list_footer, null);
    }

    @Override
    protected void loadHeaderViewContext(View header, Article article, int pos) {
        if (article == null) {
            return;
        }

        IlikeArticle ia = (IlikeArticle) article;
        header.findViewById(R.id.rd_article_detail_title_container).setBackgroundColor(
                ArticleUtils.getRandomColor(mContext, pos, Settings.isNightMode()));
        final TextView titleView = (TextView) header.findViewById(R.id.rd_article_title);
        CharSequence title = Html.fromHtml(ia.title);
        if(title.length() > 150) {
            titleView.setTextSize(18);
        } else if(title.length() > 35) {
            titleView.setTextSize(22);
        } else {
            titleView.setTextSize(25);
        }
        titleView.setText(title);

        final ImageView photoView = (ImageView) header.findViewById(R.id.ilike_author_photo);
        final TextView authorView = (TextView) header.findViewById(R.id.ilike_author);
        final TextView albumView = (TextView) header.findViewById(R.id.ilike_album);
        if(article.isDownloaded) {
            photoView.setVisibility(View.VISIBLE);
            loadAuthorPhoto(photoView, ia.author_image_url);
            authorView.setText(ia.author);
            albumView.setText(ia.album_title);
        } else {
            photoView.setVisibility(View.INVISIBLE);
            authorView.setText("");
            albumView.setText("");
        }
    }

    @Override
    protected void loadFooterViewContent(ListView list, Article article, boolean newDownload) {
        IlikeArticle ia = (IlikeArticle) article;
        final TextView timeView = (TextView) list.findViewById(R.id.ilike_time);
        timeView.setText(ArticleUtils.formatTime(ia.pubdate * 1000));

        final TextView likeCountView = (TextView) list.findViewById(R.id.ilike_liked);

        final int count;
        if(mChannel.type == Channel.TYPE_ILIKE_MY_COLLECTION) {
            if(newDownload) {
                mLikedCountCache.put(ia.contentid, ia.like_count);
                count = ia.like_count;
            } else {
                count = mLikedCountCache.containsKey(ia.contentid) ? mLikedCountCache.get(ia.contentid)
                        : ia.like_count;
            }
        } else {
            count = ia.like_count;
        }
        likeCountView
                .setText(String.format(mContext.getString(R.string.like_amount), count));

        // header info needed to be set
        View headerView = list.findViewById(R.id.rd_article_list_header_container);
        final TextView authorView = (TextView) headerView.findViewById(R.id.ilike_author);
        if (!TextUtils.isEmpty(ia.author) && TextUtils.isEmpty(authorView.getText())) {
            authorView.setText(ia.author);

            ImageView photoView = (ImageView) headerView.findViewById(R.id.ilike_author_photo);
            photoView.setVisibility(View.VISIBLE);
            loadAuthorPhoto(photoView, ia.author_image_url);
        }

        final TextView albumView = (TextView) headerView.findViewById(R.id.ilike_album);
        if (!TextUtils.isEmpty(ia.album_title) && TextUtils.isEmpty(albumView.getText())) {
            albumView.setText(ia.album_title);
        }
    }


    private void loadAuthorPhoto(ImageView imageView, String url) {
        Bitmap bitmap = null;
        final String filePath = Constants.LOCAL_PATH_AUTHOR_PHOTO + ArithmeticUtils.getMD5(url) + ".jpg";
        File pf = new File(filePath);
        if (pf.exists()) {// 读取本地图片
            bitmap = BitmapHelper.decodeFile(filePath);
        }

        if(bitmap != null) {
            imageView.setImageBitmap(bitmap);
            imageView.setTag(BitmapFactoryBase.IMAGE_STATE_LOADED);
        } else {
            imageView.setTag(url);
            downloadAuthorPhoto(imageView, url, filePath);
        }
    }

    private void downloadAuthorPhoto(final ImageView imageView, final String url,
            final String storePath) {
        new AsyncTaskEx<Void, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Void... params) {
                HttpClient httpClient = new DefaultHttpClient();
                String fullUrl = url + "?f=ilike";
                HttpUriRequest httpRequest = ImageUtils.getHttpRequestForImageDownload(fullUrl);
                try {
                    HttpResponse response = httpClient.execute(httpRequest);
                    if (response.getStatusLine().getStatusCode() == 200) {
                        byte[] bytes = EntityUtils.toByteArray(response.getEntity());
                        if (bytes != null && bytes.length > 0) {
                            Bitmap downloadedBitmap = BitmapHelper.decodeByteArray(bytes, 0,
                                    bytes.length);

                            if (FileUtils.isExternalStorageAvail()) {
                                if (fullUrl.endsWith(".png")) {
                                    Bitmap newBitmap = ImageUtils.convertPngToJpeg(bytes);
                                    if (newBitmap != null) {
                                        downloadedBitmap.recycle();
                                        downloadedBitmap = newBitmap;

                                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                        newBitmap.compress(CompressFormat.JPEG, 60, baos);
                                        bytes = baos.toByteArray();
                                    }
                                }

                                ensureImageDir();
                                ImageUtils.writeImageDataToFile(mContext, storePath, bytes);
                            }

                            return downloadedBitmap;
                        }
                    }
                } catch (Exception e) {
                    Utils.error(getClass(), Utils.getStackTrace(e));
                } catch (OutOfMemoryError e) {
                    System.gc();
                    Utils.debug(TAG, "out of memory, causing gc...");
                }
                return null;
            }

            @Override
            protected void onPostExecute(final Bitmap result) {
                ((Activity)mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (result != null && url.equals(imageView.getTag())) {
                            imageView.setImageBitmap(result);
                            imageView.setTag(BitmapFactoryBase.IMAGE_STATE_LOADED);
                        }
                    }
                });
            }

        }.execute();
    }

    public static void ensureImageDir() {
        String path = Constants.LOCAL_PATH_AUTHOR_PHOTO;
        File dir = new File(path);
        if (dir.exists()) {
            return;
        }

        dir.mkdirs();
        try {
            FileUtils.createNoMediaFileIfPathExists(path);
        } catch (IOException e) {
            Utils.error(ILikeArticleDetailActivity.class, Utils.getStackTrace(e));
        }
    }

    public boolean needToUpdateLikeCount(long content_id) {
        return !mLikedCountCache.containsKey(content_id);
    }

    public void updateLikedCount(View container, int count, long content_id) {
        mLikedCountCache.put(content_id, count);

        ((TextView) container.findViewById(R.id.ilike_liked)).setText(String.format(
                mContext.getString(R.string.like_amount), count));
    }

    public void cacheNoChangedLikedCount(int count, long content_id) {
        // liked count does not change, put it into the cache
        mLikedCountCache.put(content_id, count);
    }
}
