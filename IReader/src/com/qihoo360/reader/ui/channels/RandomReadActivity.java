package com.qihoo360.reader.ui.channels;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.OvershootInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.qihoo360.browser.hip.UXHelper;
import com.qihoo360.browser.hip.UXHelperConfig;
import com.qihoo360.reader.R;
import com.qihoo360.reader.Settings;
import com.qihoo360.reader.image.ChannelBitmapFacotry;
import com.qihoo360.reader.subscription.Category;
import com.qihoo360.reader.subscription.Channel;
import com.qihoo360.reader.subscription.reader.RssChannel;
import com.qihoo360.reader.subscription.reader.RssManager;
import com.qihoo360.reader.subscription.reader.RssSortedChannel;
import com.qihoo360.reader.subscription.reader.RssSubscribedChannel;
import com.qihoo360.reader.support.Rotate3dAnimation;
import com.qihoo360.reader.support.ShakeDetector;
import com.qihoo360.reader.support.ShakeDetector.OnShakeListener;
import com.qihoo360.reader.support.UiUtils;
import com.qihoo360.reader.support.Utils;
import com.qihoo360.reader.ui.ActivityBase;
import com.qihoo360.reader.ui.CommonUtil;
import com.qihoo360.reader.ui.ReaderPlugin;
import com.qihoo360.reader.ui.articles.ArticleReadActivity;
import com.qihoo360.reader.ui.imagechannel.ImageChannelActivity;

public class RandomReadActivity extends ActivityBase implements OnShakeListener {
    public static final String TAG = "RandomReadActivity";
    ShakeDetector mDetector = null;
    ViewFlipper mFlipper = null;

    private Button mQuitButton;
    private Button mShakeAgainButton;

    Animation[] mMainInAnimations = null;
    Animation[] mMainOutAnimations = null;
    int mAnimationIndex = 0;
    int mOutAnimationDuration = 500;

    private int mCurrentIndex = Settings.getLastRandomId();

    ArrayList<RssChannel> mFullChannelList = new ArrayList<RssChannel>();
    ArrayList<RssChannel> mChannelPool = new ArrayList<RssChannel>();
    Channel mNextChannel = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rd_random_read);

        initViews();
        initAnimations();
        prepareChannelData();

        // prepareSentences();

        mDetector = new ShakeDetector(this);
        mDetector.setOnShakeListener(this);

        ReaderPlugin.mIsRunning = true;
    }

    @Override
    public void finish() {
        ReaderPlugin.mIsRunning = false;
        if (!ReaderPlugin.getBrowserActivityRunning()) {
            ReaderPlugin.bringBrowserForeground(this);
        }
        UXHelper.getInstance().addActionRecord(
                UXHelperConfig.Reader_Shake_Shaked_And_Back_Times, 1);
        super.finish();
    }

    private void initAnimations() {
        mMainInAnimations = new Animation[9];
        /*
         * AnimationSet animSet = new AnimationSet(true);
         * animSet.addAnimation(new RotateAnimation(-180, 0,
         * Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f));
         * animSet.addAnimation(new ScaleAnimation(0f, 1f, 0f, 1f,
         * Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f));
         */
        mMainInAnimations[0] = new RotateAnimation(-180, 0,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        mMainInAnimations[1] = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, -1f, Animation.RELATIVE_TO_SELF, 0f);
        mMainInAnimations[2] = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 1f, Animation.RELATIVE_TO_SELF, 0f);
        mMainInAnimations[3] = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 1f, Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f);
        mMainInAnimations[4] = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, -1f, Animation.RELATIVE_TO_SELF,
                0f, Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF,
                0f);
        mMainInAnimations[5] = new Rotate3dAnimation(-90, 0, 0, 300, 0, false);
        mMainInAnimations[6] = new Rotate3dAnimation(90, 0, 0, 300, 0, false);
        mMainInAnimations[7] = new ScaleAnimation(0f, 1f, 0f, 1f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        mMainInAnimations[8] = new ScaleAnimation(3f, 1f, 3f, 1f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);

        mMainOutAnimations = new Animation[9];
        AnimationSet animSet = new AnimationSet(true);
        animSet.addAnimation(new TranslateAnimation(Animation.RELATIVE_TO_SELF,
                0f, Animation.RELATIVE_TO_SELF, -1.5f,
                Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF,
                1.5f));
        animSet.addAnimation(new ScaleAnimation(1f, 0f, 1f, 0f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f));
        mMainOutAnimations[0] = animSet;
        mMainOutAnimations[1] = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 1f);
        mMainOutAnimations[2] = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, -1f);
        mMainOutAnimations[3] = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF,
                -1f, Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 0f);
        mMainOutAnimations[4] = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 1f,
                Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f);
        mMainOutAnimations[5] = new Rotate3dAnimation(0, 90, 0, 300, 0, false);
        mMainOutAnimations[6] = new Rotate3dAnimation(0, -90, 0, 300, 0, false);
        mMainOutAnimations[7] = new ScaleAnimation(1f, 3f, 1f, 3f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        mMainOutAnimations[8] = new ScaleAnimation(1f, 0f, 1f, 0f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
    }

    private boolean prepareChannelData() {
        List<Category> categories = RssManager.getIndex().getCategories();
        if (categories == null) {
            return false;
        }

        List<String> subscribedChannels = RssSubscribedChannel
                .getChannelsForNameOnly(getContentResolver());
        RssSortedChannel sortedChannel = RssSortedChannel.get();
        if (sortedChannel != null && sortedChannel.getList() != null) {
            for (String channelName : sortedChannel.getList()) {
                // 已经订阅过的频道？
                if (subscribedChannels.contains(channelName)) {
                    continue;
                }

                // 没有订阅，但它不存在或者被禁用？
                RssChannel addedChannel = RssChannel.get(channelName);
                if (addedChannel != null && !addedChannel.disabled) {
                    mFullChannelList.add(addedChannel);
                }
            }
        }

        Utils.debug(TAG, mFullChannelList.size() + " channels were found.");

        mChannelPool.addAll(mFullChannelList);

        return true;
    }

    private void startShaking() {
        prepareNextChannel();

        Animation animShake = createShakeAnimation();
        animShake.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                loadNewChannel();
                startOut();
            }
        });

        View currentView = mFlipper.getCurrentView();
        currentView.startAnimation(animShake);

    }

    private void loadNewChannel() {
        if (mNextChannel == null) {
            return;
        }

        int count = mFlipper.getChildCount();
        int currentIdx = mFlipper.getDisplayedChild();

        View nextView = null;
        if (currentIdx < count - 1) {
            nextView = mFlipper.getChildAt(++currentIdx);
        } else {
            nextView = mFlipper.getChildAt(0);
        }

        nextView.findViewById(R.id.shelf).setVisibility(View.VISIBLE);
        nextView.findViewById(R.id.default_img).setVisibility(View.GONE);

        ImageView iv = (ImageView) nextView.findViewById(R.id.image);
        loadChannelCoverFromFile(mNextChannel, iv);

        TextView tv_name = (TextView) nextView.findViewById(R.id.name);
        TextView tv_desc = (TextView) nextView.findViewById(R.id.desc);
        if (!TextUtils.isEmpty(mNextChannel.title)) {
            tv_name.setText(mNextChannel.title);
            tv_name.setVisibility(View.VISIBLE);

            tv_desc.setText(mNextChannel.desc);
            tv_desc.setVisibility(View.VISIBLE);
        } else {
            tv_name.setVisibility(View.INVISIBLE);
            tv_desc.setVisibility(View.INVISIBLE);
        }

        nextView.findViewById(R.id.shelf).setTag(mNextChannel.channel);
        mShakeAgainButton.setText(R.string.rd_random_read_shake_again);
    }

    private void loadChannelCoverFromFile(Channel channel, ImageView view) {
        view.setImageBitmap(null);

        int imageVersion = 0;
        if (!TextUtils.isEmpty(channel.imageversion)) {
            try {
                imageVersion = Integer.parseInt(channel.imageversion);
            } catch (Exception e) {
                Utils.error(getClass(), Utils.getStackTrace(e));
            }
        }

        ChannelBitmapFacotry.getInstance(this).setRawChannelCover(this,
                channel.image, imageVersion, channel.channel, view);
    }

    private void startOut() {
        mAnimationIndex = (int) (Math.random() * mMainInAnimations.length);
        
        Animation animIn = createInAnimation();

        Animation animOut = createOutAnimation();

        mFlipper.setInAnimation(animIn);
        mFlipper.setOutAnimation(animOut);
        mFlipper.showNext();
    }

    private void initViews() {
        mFlipper = (ViewFlipper) findViewById(R.id.flipper);
        int childCount = mFlipper.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = mFlipper.getChildAt(i);
            child.findViewById(R.id.shelf).setOnClickListener(
                    new OnClickListener() {
                        @Override
                        public void onClick(View paramView) {
                            String channel = (String) paramView.getTag();
                            if (!TextUtils.isEmpty(channel)) {
                                Intent intent = null;
                                Channel channelObj = RssChannel.get(channel);
                                if (channelObj.type == 3) {
                                    intent = new Intent(
                                            RandomReadActivity.this,
                                            ImageChannelActivity.class);
                                } else {
                                    intent = new Intent(
                                            RandomReadActivity.this,
                                            ArticleReadActivity.class);
                                }
                                intent.putExtra("channel", channel);
                                intent.putExtra("random_read", true);
                                startActivity(intent);
                                UXHelper.getInstance()
                                        .addActionRecord(
                                                UXHelperConfig.Reader_Random_Read_Channel_Select,
                                                1);
                            }
                        }
                    });
        }

        mQuitButton = (Button) findViewById(R.id.rd_quit);
        mQuitButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mShakeAgainButton = (Button) findViewById(R.id.rd_shake_again);
        mShakeAgainButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                onShake();
            }
        });
    }

    @Override
    public void onShake() {
        if (CommonUtil.isInLock()) {
            return;
        }
        startShaking();
        UXHelper.getInstance().addActionRecord(
                UXHelperConfig.Reader_Random_Read_Shake, 1);
        vibrate();
        playSound();
    }

    @Override
    public void onBackPressed() {
        UXHelper.getInstance().addActionRecord(
                UXHelperConfig.Reader_Random_Read_Back_Key_OnClick, 1);
        super.onBackPressed();
    }

    private void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(1000);
    }

    private void playSound() {
        MediaPlayer m = new MediaPlayer();
        try {
            AssetFileDescriptor descriptor = getResources().openRawResourceFd(
                    R.raw.shake_sound);
            m.setDataSource(descriptor.getFileDescriptor(),
                    descriptor.getStartOffset(), descriptor.getLength());
            descriptor.close();
            m.prepare();
            m.start();
        } catch (Exception e) {
            Utils.error(getClass(), Utils.getStackTrace(e));
        }
    }

    private void prepareNextChannel() {
        int poolSize = mChannelPool.size();
        if (poolSize == 0) {
            mChannelPool = mFullChannelList;
            poolSize = mFullChannelList.size();
        }
        if (poolSize == 0) {
            // 解决23574 -
            // [v_2.0.0Beta_Build075_REL][阅读]进入摇一摇页面后点击Back键，浏览器crash（见log）问题
            return;
        }

        if (mCurrentIndex >= poolSize) {
            // 到头了怎么办？
            mCurrentIndex = 0;
            prepareChannelData();
            prepareNextChannel();
            return;
        }
        mNextChannel = mChannelPool.get(mCurrentIndex++);

    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        mFlipper.showNext();
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onDestroy()
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                Settings.setLastRandomId(mCurrentIndex);
                return null;
            }

        }.execute();

    }

    @Override
    public void onResume() {
        super.onResume();

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mDetector.resume();
    }

    @Override
    public void onPause() {
        mDetector.pause();
        super.onPause();
    }

    private Animation createShakeAnimation() {
        Animation anim = AnimationUtils.loadAnimation(this, R.anim.rd_shake);
        return anim;
    }

    private Animation createInAnimation() {
        AnimationSet animSet = new AnimationSet(true);
        Log.i("Animation", mAnimationIndex + " = mAnimationIndex");
        animSet.addAnimation(mMainInAnimations[mAnimationIndex]);
        animSet.addAnimation(new AlphaAnimation(0f, 1f));
        animSet.setDuration(mOutAnimationDuration);
        // animSet.setInterpolator(new DecelerateInterpolator(1.5f));
        animSet.setInterpolator(new OvershootInterpolator(0.8f));
        return animSet;
    }

    private Animation createOutAnimation() {
        AnimationSet animSet = new AnimationSet(true);
        Log.i("Animation", mAnimationIndex + " = mAnimationIndex");
        animSet.addAnimation(mMainOutAnimations[mAnimationIndex]);
        animSet.addAnimation(new AlphaAnimation(1f, 0f));

        animSet.setDuration(mOutAnimationDuration);
        animSet.setInterpolator(new AccelerateInterpolator(1.5f));

        return animSet;
    }

    @Override
    public void onUpdateNightMode() {
        super.onUpdateNightMode();
        setBackground(R.id.container);
    }
}
