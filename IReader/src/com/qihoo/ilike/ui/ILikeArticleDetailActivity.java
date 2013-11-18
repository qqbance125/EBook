package com.qihoo.ilike.ui;

import com.qihoo.ilike.subscription.IlikeArticle;
import com.qihoo.ilike.util.Utils;
import com.qihoo360.browser.hip.UXHelperConfig;
import com.qihoo360.browser.hip.UXKey;
import com.qihoo360.reader.R;
import com.qihoo360.reader.listener.OnMarkStarResultListener;
import com.qihoo360.reader.listener.OnRefreshLikeCountResultListener;
import com.qihoo360.reader.subscription.Article;
import com.qihoo360.reader.subscription.Channel;
import com.qihoo360.reader.ui.articles.ArticleDetailActivity;

import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

public class ILikeArticleDetailActivity extends ArticleDetailActivity {
    @Override
    protected UXKey getPvHipId() {
        return UXHelperConfig.WoXiHuan_PV_Times;
    }

    @Override
    protected void changeFavMenuState(boolean isStared) {
    }

    @Override
    protected boolean isStaredArticle(Article article) {
        return false;
    }

    @Override
    protected boolean isArticleUrlLiked(Article article) {
        return article.star == Article.STAR_APPEARANCE;
    }

    @Override
    protected void handleShareBtnClicked() {
        IlikeArticle article = (IlikeArticle) getCurrentArticle();
        String link = "http://www.woxihuan.com/" + article.author_qid + "/"
                + article.contentid + ".shtml";
        Utils.shareILikeArticle(this, article.title, link);
    }

    @Override
    protected void handleLikedBtnClicked() {
        Article article = getCurrentArticle();
        if (!isArticleUrlLiked(article)) {
            if (!Utils.accountConfigured(ILikeArticleDetailActivity.this)) {
                startWaitingForAccountConfiguration(article);
                return;
            }

            likeArticle(article);
        }
    }

    @Override
    protected void likeArticle(Article article) {
        if (!Utils
                .checkNetworkStatusBeforeLike(ILikeArticleDetailActivity.this)) {
            return;
        }

        ((IlikeArticle) article).markStar(getContentResolver(),
                new OnMarkStarResultListener() {
                    @Override
                    public void onComplete() {
                        Toast.makeText(ILikeArticleDetailActivity.this,
                                R.string.ilike_collect_successful, Toast.LENGTH_SHORT)
                                .show();
                    }

                    @Override
                    public void onFailure() {
                        Toast.makeText(ILikeArticleDetailActivity.this,
                                R.string.ilike_collect_failure, Toast.LENGTH_LONG)
                                .show();
                        handleLikeFail();
                    }
                });
        changeLikedBtnStatus();
        updateCursor();
    }

    @Override
    protected void toggleMenuView() {
    }

    @Override
    protected void updateLikeBtnStatues(Article article) {
        super.updateLikeBtnStatues(article);

        // check if need to update the liked count for this article
        if (mChannel.type == Channel.TYPE_ILIKE_MY_COLLECTION) {
            updateLikedCount(article);
        }
    }

    private final void updateLikedCount(Article article) {
        final long content_id = article.contentid;
        if (((ILikeArticleDetailAdapter) mDetailAdapter)
                .needToUpdateLikeCount(content_id)) {
            ((IlikeArticle) article).refreshLikeCount(getContentResolver(),
                    new OnRefreshLikeCountResultListener() {
                        @Override
                        public void onFailure(int error) {
                        }

                        @Override
                        public void onCompletion(int likeCount,
                                boolean isChanged) {
                            if (isChanged) {
                                try {
                                    for (int i = 0; i < mDetailReadView
                                            .getChildCount(); i++) {
                                        View child = mDetailReadView
                                                .getChildAt(i);
                                        ListView contentList = (ListView) child
                                                .findViewById(R.id.rd_article_detail_content);
                                        if (contentList != null
                                                && ((Long) contentList.getTag()) == content_id) {
                                            ((ILikeArticleDetailAdapter) mDetailAdapter)
                                                    .updateLikedCount(
                                                            contentList,
                                                            likeCount,
                                                            content_id);
                                            break;
                                        }
                                    }
                                } catch (Exception e) {
                                    com.qihoo360.reader.support.Utils.error(
                                            getClass(),
                                            com.qihoo360.reader.support.Utils
                                                    .getStackTrace(e));
                                }
                            } else {
                                ((ILikeArticleDetailAdapter) mDetailAdapter)
                                        .cacheNoChangedLikedCount(likeCount,
                                                content_id);
                            }
                        }
                    });
        }
    }
}
