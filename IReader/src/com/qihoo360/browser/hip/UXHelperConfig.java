package com.qihoo360.browser.hip;

import java.util.HashMap;

/**
 * Rule:
 * 模块控件动作 名称命名:
 * 实际存储到内存和数据库中的记录中, 动作名称和动作次数都是int类型, UXHelperConfig主要用于动作命名管理.
 * 命名规范:
 * int 模块名_控件或者子模块名_动作名 = 1aabbcc(都是以1开头);
 * aa 指模块编号
 * bb 指控件编号
 * cc 指动作编号
 *
 * aa bb cc取值都是自由的, 但是为了规范管理, 请将同一模块的命名添加在一起.
 *
 * 示例:
 * public static final UXKey Urlbar_button_refresh_OnClick = new UXKey(1020701, "刷新");
 * 在new UXKey的时候, 不能换行, 否则会对build脚本造成影响!!!!!!!!
 * 命名的KEY不能重复, 注释不能为空, 否则会在DEBUG模式下会导致程序崩溃, 当程序崩溃的时候, 可以通过查看LOG中的EXCEPTION信息来定位到底是哪一个KEY
 * 的定义出了问题.
 */
public class UXHelperConfig {

    public static HashMap<String, UXKey> mUxNavigationHelper = null;

    public static HashMap<String, UXKey> getNavUxHelper() {
        if (mUxNavigationHelper == null) {
            mUxNavigationHelper = new HashMap<String, UXKey>(7);
            mUxNavigationHelper.put("http://m.baidu.com/", UXHelperConfig.HomePage_default_site_baidu_OnClick);
            mUxNavigationHelper.put("http://3g.sina.com.cn/", UXHelperConfig.HomePage_default_site_sina_OnClick);
            mUxNavigationHelper.put("http://m.taobao.com/", UXHelperConfig.HomePage_default_site_taobao_OnClick);
            mUxNavigationHelper.put("http://3g.163.com/touch/", UXHelperConfig.HomePage_default_site_wangyi__OnClick);
            mUxNavigationHelper.put("http://i.ifeng.com/itouch?vt=5&mid=65Mpyc&ch=llq_360&vt=4&vt=5",
                            UXHelperConfig.HomePage_default_site_fenghuang_OnClick);
            mUxNavigationHelper.put("http://btouch.easou.com/?wver=t&cid=hqdb", UXHelperConfig.HomePage_default_site_easou_OnClick);
            mUxNavigationHelper.put("http://wap.sohu.com/?nid=1&v=2&fr=360sy",
                            UXHelperConfig.HomePage_default_site_sohu_OnClick);
            mUxNavigationHelper.put("http://weibo.cn/dpool/ttt/h5/index.php?wm=4391",
                            UXHelperConfig.HomePage_default_site_sina_weibo_OnClick);

            /*            mUxNavigationHelper.put("http://3g.youku.com/", UXHelperConfig.HomePage_default_site_youku_OnClick);
            */
        }
        return mUxNavigationHelper;
    }

    /**
     * Module: URLBAR
     */
/*
 *  //ouzhili: 暂无作用
    public static final UXKey Urlbar_button_show_fast_screen_OnClick = 1020101;
    //ouzhili: 暂无作用
    public static final UXKey Urlbar_button_full_screen_OnClick = 1020201;
    //ouzhili: 暂无作用
    public static final UXKey Urlbar_button_login_OnClick = 1020301;
    //ouzhili: 暂无作用
    public static final UXKey Urlbar_button_back_OnClick = 1020401;
    //ouzhili: 暂无作用
    public static final UXKey Urlbar_button_forward_OnClick = 1020501;
    //ouzhili: 暂无作用
    public static final UXKey Urlbar_txt_tab_title_OnClick = 1020601;
    */
    //wuke: 刷新
    public static final UXKey Urlbar_button_refresh_OnClick = new UXKey(1020701, "刷新");
    //wuke: 停止
    public static final UXKey Urlbar_button_stop_OnClick = new UXKey(1020801, "停止");

    //yy: 地址栏添加收藏
    public static final UXKey Urlbar_farvorite_OnClick = new UXKey(1020910, "地址栏收藏菜单");
    public static final UXKey Urlbar_farvorite_add_ilike_OnClick = new UXKey(1020912, "地址栏添加我喜欢");
    public static final UXKey Urlbar_farvorite_add_local_OnClick = new UXKey(1020913, "地址栏添加本地收藏");
    public static final UXKey Urlbar_farvorite_add_online_bookmark_OnClick = new UXKey(1020914, "地址栏添加网络收藏");
    public static final UXKey Urlbar_farvorite_add_read_later_OnClick = new UXKey(1020915, "地址栏添加稍后阅读");
    public static final UXKey Urlbar_farvorite_add_fast_link_OnClick = new UXKey(1020916, "地址栏添加主页快速链接");

    //xuesong: urlbar edit text click
    public static final UXKey Urlbar_edit_url_OnClick = new UXKey(1021001, "urlbar edit text click");
    public static final UXKey Urlbar_go_search_OnClick = new UXKey(1021004, "点击搜索按钮");
    public static final UXKey Urlbar_safe_center_OnClick = new UXKey(1021005, "点击安全中心");
    //wuke: urlbar edit text copy
    public static final UXKey Urlbar_edit_url_Copy = new UXKey(1021002, "urlbar edit text copy");
    //wuke: urlbar edit text post
    public static final UXKey Urlbar_edit_url_Post = new UXKey(1021003, "urlbar edit text post");

    //ouzhili: 暂无作用
    //public static final UXKey Urlbar_search_engine_icon_OnClick = new UXKey(1021101, "");
    //ouzhili: 地址栏里"清除输入"按钮
    public static final UXKey Urlbar_button_delete_OnClick = new UXKey(1021201, "地址栏里清除输入按钮");

    /*
    //ouzhili: 暂无作用
    public static final UXKey Urlbar_loginButton_OnClick = 1021301;
    //ouzhili: 暂无作用
    public static final UXKey Urlbar_cancelButton_OnClick = 1021401;
    //ouzhili: 暂无作用
    public static final UXKey Urlbar_registTxt_OnClick = 1021501;
    //ouzhili: 暂无作用
    public static final UXKey Urlbar_exit_ok_OnClick = 1021601;
    //ouzhili: 暂无作用
    public static final UXKey Urlbar_exit_cancel_OnClick = 1021701;
    //ouzhili: 暂无作用
    public static final UXKey Urlbar_exit_online_bookmark_menu_cancel_OnClick = 1021801;
    //ouzhili: 暂无作用
    public static final UXKey Urlbar_exit_online_bookmark_menu_exit_OnClick = 1021901;
    //ouzhili: 暂无作用
    public static final UXKey Urlbar_exit_online_bookmark_menu_setphoto_OnClick = 1022001;
    */
    /**
     * Module: MENU
     */

    /**
     * Module: BOOKMARK
     */

    /**
     * Module: SEARCH
     */
    //ouzhili: 从地址栏里选择google作为搜索引擎
    public static final UXKey SearchEngine_null_doSearchGoogle = new UXKey(1050001, "从地址栏里选择google作为搜索引擎");
    //ouzhili: 从地址栏里选择有道作为搜索引擎
    public static final UXKey SearchEngine_null_doSearchYoudao = new UXKey(1050002, "从地址栏里选择有道作为搜索引擎");
    //ouzhili: 从地址栏里选择奇虎问答作为搜索引擎
    public static final UXKey SearchEngine_null_doSearchQihoo = new UXKey(1050003, "从地址栏里选择奇虎问答作为搜索引擎");
    //ouzhili: 从地址栏里选择百度作为搜索引擎
    public static final UXKey SearchEngine_null_doSearchBaidu = new UXKey(1050004, "从地址栏里选择百度作为搜索引擎");
    //xuesong: 淘宝搜索
    public static final UXKey SearchEngine_null_doSearchTaobao = new UXKey(1050005, "淘宝搜索");
    //xuesong: 京东搜索
    public static final UXKey SearchEngine_null_doSearchJingdong = new UXKey(1050006, "京东搜索");
    //xuesong: 当当搜索
    public static final UXKey SearchEngine_null_doSearchDangdang = new UXKey(1050007, "当当搜索");

    public static final UXKey SearchEngine_By_Search = new UXKey(1050011, "输入网址，使用搜索结果");
    public static final UXKey SearchEngine_By_Url = new UXKey(10500012, "输入网址，使用URL结果");
    /**
     * Module: UPDATER
     */
    public static final UXKey Updater_CheckUpdate_TryToCheckUpdate = new UXKey(1010101, "Try to checkUpdate");

    /**
     * Module: DOWNLOADER
     */
    //xuesong: 正在下载的列表头, 点击
    public static final UXKey Downloader_DownloadingGroup_OnClick = new UXKey(1060101, "正在下载的列表头, 点击");
    //xuesong: 已经下载完毕的列表头, 点击
    public static final UXKey Downloader_DownloadCompleteGroup_OnClick = new UXKey(1060201, "已经下载完毕的列表头, 点击");
    //xuesong: 下载任务(包括完成的和未完成的以及出错的), 点击
    public static final UXKey Downloader_DownloadChild_OnClick = new UXKey(1060301, "下载任务(包括完成的和未完成的以及出错的), 点击");
    //xuesong: 清除所有下载任务, 点击
    public static final UXKey Downloader_ClearAllButton_OnClick = new UXKey(1060401, "清除所有下载任务, 点击");
    //xuesong: 界面, 返回按钮, 点击
    public static final UXKey Downloader_BackButton_OnClick = new UXKey(1060501, "界面, 返回按钮, 点击");
    //xuesong: 长按下载任务后弹出的菜单中的打开文件选项, 点击
    public static final UXKey Downloader_ChildMenu_FileOpen_OnClick = new UXKey(1060601, "长按下载任务后弹出的菜单中的打开文件选项, 点击");
    //xuesong: 长按下载任务后弹出的菜单中的删除选项, 点击
    public static final UXKey Downloader_ChildMenu_DownloadDelete_OnClick = new UXKey(1060701, "长按下载任务后弹出的菜单中的删除选项, 点击");
    //xuesong: 长按下载任务后弹出的菜单中的文件信息选项, 点击
    public static final UXKey Downloader_ChildMenu_DownloadInfo_OnClick = new UXKey(1060801, "长按下载任务后弹出的菜单中的文件信息选项, 点击");
    //xuesong: 长按下载任务后弹出的菜单中的取消下载选项, 点击(正在下载中)
    public static final UXKey Downloader_ChildMenu_DownloadCancel_OnClick = new UXKey(1060901, "长按下载任务后弹出的菜单中的取消下载选项, 点击(正在下载中)");
    //xuesong: 长按下载任务后弹出的菜单中的清除下载选项, 点击(下载出错)
    public static final UXKey Downloader_ChildMenu_DownloadClear_OnClick = new UXKey(1061001, "长按下载任务后弹出的菜单中的清除下载选项, 点击(下载出错)");
    //xuesong: 长按下载任务后弹出的菜单中的暂停下载选项, 点击(正在下载中)
    public static final UXKey Downloader_ChildMenu_DownloadPause_OnClick = new UXKey(1061101, "长按下载任务后弹出的菜单中的暂停下载选项, 点击(正在下载中)");
    //xuesong: 长按一个下载任务
    public static final UXKey Downloader_DownloadChild_OnLongClick = new UXKey(1061201, "长按一个下载任务");
    //xuesong: 长按下载任务后弹出的菜单中的恢复下载选项, 点击(下载暂停时)
    public static final UXKey Downloader_ChildMenu_DownloadResume_OnClick = new UXKey(1061301, "长按下载任务后弹出的菜单中的恢复下载选项, 点击(下载暂停时)");
    //xuesong: 长按下载任务后弹出的菜单中的重新下载选项, 点击(下载完成后, 包括出错时)
    public static final UXKey Downloader_ChildMenu_DownloadRestart_OnClick = new UXKey(1061401, "长按下载任务后弹出的菜单中的重新下载选项, 点击(下载完成后, 包括出错时)");

    /**
     * Module: BOTTOMBAR
     */
    /*
    //zhihui:底部按钮，收回/弹出底部栏
    public static final UXKey Bottombar_corner_btn_OnClick = 1070101;
    //zhihui:弹出/收回标签中心
    public static final UXKey Bottombar_bottom_menu_switch_screen_btn_OnClick = 1070301;
    //zhihui:回退到上一页
    public static final UXKey Bottombar_bottom_menu_back_btn_OnClick = 1070501;
    //zhihui:暂无作用
    public static final UXKey Bottombar_bottom_backupcontact_btn_OnClick = 1071001;
    //zhihui:前进，下一页
    public static final UXKey Bottombar_bottom_forward_btn_OnClick = 1071101;
    */
    //zhihui:显示左边栏历史收藏
    public static final UXKey Bottombar_bottom_menu_fav_pane_btn_OnClick = new UXKey(1070601, "显示左边栏历史收藏");
    //zhihui:显示主页
    public static final UXKey Bottombar_bottom_menu_homepage_btn_OnClick = new UXKey(1070201, "显示主页");;
    //zhihui:停止加载
    public static final UXKey Bottombar_bottom_menu_stoploading_btn_OnClick = new UXKey(1070401, "停止加载");
    public static final UXKey Bottombar_bottom_feedback_btn_OnClick = new UXKey(1070700, "意见反馈");
    public static final UXKey Bottombar_bottom_reload_btn_OnClick = new UXKey(1070701, "刷新");
    //zhihui:添加收藏
    public static final UXKey Bottombar_bottom_add2bookmark_btn_OnClick = new UXKey(1070801, "添加收藏");
    //zhihui:进入下载管理界面
    public static final UXKey Bottombar_bottom_download_btn_OnClick = new UXKey(1070901, "进入下载管理界面");

    public static final UXKey Bottombar_bottom_clear_btn_OnClick = new UXKey(1071300, "清除痕迹");
    //zhihui:浏览器退出
    public static final UXKey Bottombar_bottom_exit_btn_OnClick = new UXKey(1071301, "浏览器退出");
    //zhihui:进入设置界面
    public static final UXKey Bottombar_bottom_preference_btn_OnClick = new UXKey(1071401, "进入设置界面");


    //zhihui:标签中心添加Tab
    public static final UXKey Bottombar_add_new_tab_OnClick = new UXKey(1071501, "标签中心添加Tab");
    //zhihui:标签中心关闭所有tab
    public static final UXKey Bottombar_close_all_tabs_OnClick = new UXKey(1071601, "标签中心关闭所有tab");
    //zhihui:标签中心关闭tab
    public static final UXKey Bottombar_tab_close_btn_OnClick = new UXKey(1071701, "标签中心关闭tab");
    //wuke:标签中心切换标签
    public static final UXKey Bottombar_tab_switch_btn_OnClick = new UXKey(1071709, "标签中心切换标签");





/*
    //zhihui:是否显示图片
    public static final UXKey Bottombar_bottom_show_image_btn_OnClick = 1071801;
    //xuesong: 显示收藏中心
    public static final UXKey Bottombar_bottom_show_bookmarkcenter = 1071901;
    //xuesong: more 更多按钮
    public static final UXKey Bottombar_bottom_show_more = 1072101;
    //xuesong: 屏幕旋转
    public static final UXKey Bottombar_bottom_show_screen_direction_option = 1072201;
    //wuke: 滑动到左侧收藏夹
    public static final UXKey Bottombar_bottom_fav_panel = 1072500;
    //xuesong: checkupdate
    public static final UXKey Bottombar_bottom_checkupdate = 1072901;
    //wuke: 点击无痕浏览
    public static final UXKey Bottombar_bottom_traceless_click = 1073000;
    //wuke: 菜单中 设置默认浏览器
    public static final UXKey Bottombar_bottom_set_default_browser = 1073001;
    //wuke: 翻页模式
    public static final UXKey Bottombar_bottom_fast_page_option = 1073002;
    */

    //xuesong: 切换到夜间模式
    public static final UXKey Bottombar_bottom_show_night_mode = new UXKey(1072001, "切换到夜间模式");
    //xuesong: 切换到日间模式
    public static final UXKey Bottombar_bottom_show_day_mode = new UXKey(1072002, "切换到日间模式");
    //xuesong: 痕迹清理
    public static final UXKey Bottombar_bottom_show_clear_trace = new UXKey(1072301, "痕迹清理");
    //xuesong: 快捷控制: 展开
    public static final UXKey Bottombar_bottom_quickcontrol_show = new UXKey(1072401, "快捷控制: 展开");
    //xuesong: 快捷控制: 收起
    public static final UXKey Bottombar_bottom_quickcontrol_hide = new UXKey(1072402, "快捷控制: 收起");
    //xuesong: 快捷控制 向前
    public static final UXKey Bottombar_bottom_quickcontrol_forward = new UXKey(1072501, "快捷控制 向前");
    //xuesong: 快捷控制 向后
    public static final UXKey Bottombar_bottom_quickcontrol_back = new UXKey(1072601, "快捷控制 向后");
    //xuesong: 快捷控制: 显示底部tab
    public static final UXKey Bottombar_bottom_quickcontrol_show_bottom_tabs = new UXKey(1072701, "快捷控制: 显示底部tab");
    //xuesong: 快捷控制: 显示menu
    public static final UXKey Bottombar_bottom_quickcontrol_show_bottom_menu = new UXKey(1072801, "显示menu");
    //xuesong: 进入全屏
    public static final UXKey Bottombar_bottom_fullscreen_enter = new UXKey(1072901, "进入全屏");
    //xuesong: 离开全屏
    public static final UXKey Bottombar_bottom_fullscreen_leave = new UXKey(1072902, "离开全屏");

    //wuke: 点击向下翻页按钮
    public static final UXKey button_fast_page_down = new UXKey(1073003, "点击向下翻页按钮");
    //wuke: 点击向上翻页按钮
    public static final UXKey button_fast_page_up = new UXKey(1073004, "点击向上翻页按钮");

/*
    //wuke: 菜单条 常用按钮
    public static final UXKey Bottombar_bottom_commom = 1073005;
    //wuke: 菜单条 设置按钮
    public static final UXKey Bottombar_bottom_setting = 1073006;
    //wuke: 浏览模式按钮
    public static final UXKey Bottombar_bottom_browser_mode = 1073007;
    */



    //wuke: 极速模式
    public static final UXKey Bottombar_bottom_speed_mode = new UXKey(1073008, "极速模式");

    //wuke: 直连模式
    public static final UXKey Bottombar_bottom_direct_mode = new UXKey(1073009, "直连模式");

    /**
     * Module:LEFTPANE
     */

    //tengfei:查看网络收藏夹
    public static final UXKey LeftPane_button_online_bookmark_OnClick = new UXKey(1080101, "查看网络收藏夹");

    //tengfei：查看书签
    public static final UXKey LeftPane_button_bookmark_OnClick = new UXKey(1080201, "查看书签");

    //tengfei:查看历史
    public static final UXKey LeftPane_button_history_OnClick = new UXKey(1080301, "查看历史");

    //tengfei:查看最常访问
    public static final UXKey LeftPane_button_most_visited_OnClick = new UXKey(1080401, "查看最常访问");

    //jiangyang: 查看Ilike
    public static final UXKey LeftPane_button_Ilike_OnClick = new UXKey(1080302, "查看Ilike");

    //jiangyang: 查看Read it later
    public static final UXKey LeftPane_button_ReadItLater_OnClick = new UXKey(1080303, "查看ReadItLater");


    //tengfei:新建书签文件夹
    public static final UXKey LeftPane_button_new_folder_OnClick = new UXKey(1080501, "新建书签文件夹");

    //tengfei:添加书签
    public static final UXKey LeftPane_button_add_bookmark_OnClick = new UXKey(1080601, "添加书签");

    public static final UXKey LeftPane_button_backup_bookmark_OnClick = new UXKey(1080602, "一键备份");

    //清除历史
    public static final UXKey LeftPane_button_clear_history_OnClick = new UXKey(1080701, "清除历史");

    //退出网络收藏夹
    public static final UXKey LeftPane_button_exit_login_OnClick = new UXKey(1080801, "退出网络收藏夹");

    //同步网络收藏夹
    public static final UXKey LeftPane_button_sync_immediately_OnClick = new UXKey(1080901, "同步网络收藏夹");

    //清除输入的用户名
    public static final UXKey LeftPane_button_delete_username_OnClick = new UXKey(1081001, "清除输入的用户名");

    //清除输入的密码
    public static final UXKey LeftPane_button_delete_password_OnClick = new UXKey(1081101, "清除输入的密码");

    //登录网络收藏夹
    public static final UXKey LeftPane_button_login_OnClick = new UXKey(1081201, "登录网络收藏夹");

    //验证码刷新
    public static final UXKey LeftPane_button_refresh_code_OnClick = new UXKey(1081301, "验证码刷新");
/*
    //取消注册
    public static final UXKey LeftPane_button_cancel_register_OnClick = new UXKey(1081501, "取消注册");
*/
    public static final UXKey LeftPane_button_exit_online_bookmark_ok_OnClick = new UXKey(1081401, "退出网络收藏夹");

    //取消退出网络收藏夹
    public static final UXKey LeftPane_button_exit_online_bookmark_cancel_OnClick = new UXKey(1081501, "取消退出网络收藏夹");

    //注册网络收藏夹账号。
    public static final UXKey leftPane_button_register_tab_OnClick = new UXKey(1081601, "注册网络收藏夹账号。");

    //点击头像进行登录注册
    public static final UXKey leftPane_click_avtar_to_login_or_register_OnClick = new UXKey(1081701, "点击头像进行登录注册");

    //点击底部返回按钮
    public static final UXKey leftPane_button_back_OnClick = new UXKey(1081801, "点击底部返回按钮");

    //长按书签，历史记录，最常访问出来的对话框， 点击打开
    public static final UXKey leftPane_item_long_click_open_OnClick = new UXKey(1081901, "长按书签，历史记录，最常访问出来的对话框， 点击打开");

    //长按书签，历史记录，最常访问出来的对话框， 点击在新窗口中打开
    public static final UXKey leftPane_item_long_click_open_in_new_window_OnClick = new UXKey(1082001, "长按书签，历史记录，最常访问出来的对话框， 点击在新窗口中打开");

    //长按书签，历史记录，最常访问出来的对话框， 点击分享链接
    public static final UXKey leftPane_item_long_click_share_link_OnClick = new UXKey(1082101, "长按书签，历史记录，最常访问出来的对话框， 点击分享链接");

    //长按书签，历史记录，最常访问出来的对话框， 点击复制链接
    public static final UXKey leftPane_item_long_click_copy_link_OnClick = new UXKey(1082201, "长按书签，历史记录，最常访问出来的对话框， 点击复制链接");

    //长按书签出来的对话框， 点击编辑书签
    public static final UXKey leftPane_item_long_click_edit_bookmark_OnClick = new UXKey(1082301, "长按书签出来的对话框， 点击编辑书签");

    //长按书签出来的对话框， 点击删除书签
    public static final UXKey leftPane_item_long_click_delete_bookmark_OnClick = new UXKey(1082401, "长按书签出来的对话框， 点击删除书签");

    //长按历史记录或最常访问出来的对话框， 点击添加到本地收藏夹
    public static final UXKey leftPane_item_long_click_add_to_bookmark_OnClick = new UXKey(1082501, "长按历史记录或最常访问出来的对话框， 点击添加到本地收藏夹");

    //长按历史记录或最常访问出来的对话框， 点击删除记录
    public static final UXKey leftPane_item_long_click_delete_history_OnClick = new UXKey(1082601, "长按历史记录或最常访问出来的对话框， 点击删除记录");

    /**
     * Module: HOMEPAGE
     */
/*
 *  //kangyonggen: 点击地址栏
    public static final UXKey HomePage_urlbar_edit_OnClick = new UXKey(, "")1090001;
    */

/*    //kangyonggen: 展开/收起 分类导航
    public static final UXKey HomePage_moblie_navigation_OnClick = new UXKey(1090201, "展开/收起 分类导航");
    //kangyonggen: 手机酷站链接
    public static final UXKey HomePage_moblie_navigation_link_OnClick = new UXKey(, "")1090301;
    //kangyonggen: 暂无作用
    public static final UXKey HomePage_internet_navigation_OnClik = new UXKey(, "")1090401;
    //kangyonggen: 暂无作用
    public static final UXKey HomePage_internet_navigation_link_OnClick = new UXKey(, "")1090501;
    //kangyonggen: 展开/收起实用查询
    public static final UXKey HomePage_inquires_navigation_OnClick = new UXKey(, "")1090601;
    //kangyonggen: 点击分类导航链接
    public static final UXKey HomePage_inquires_navigation_link_OnClick = new UXKey(, "")1090701;
    //kangyonggen: 展开/收起访问最多
    public static final UXKey HomePage_frequently_navigation_OnClick = new UXKey(, "")1090801;
    //kangyonggen: 点击最多问链接
    public static final UXKey HomePage_frequently_navigation_link_OnClick = new UXKey(, "")1090901;
    //kangyonggen: 展开/收起Android专区
    public static final UXKey HomePage_android_navigation_OnClick = new UXKey(, "")1091001;
    //kangyonggen: Android 专区链接
    public static final UXKey HomePage_android_navigation_link_OnClick = new UXKey(, "")1091101;
    */



    public static final UXKey HomePage_default_site_baidu_OnClick = new UXKey(1091201, "导航页点击,百度链接");
    public static final UXKey HomePage_default_site_sina_OnClick = new UXKey(1091301, "导航页点击,新浪链接");
    public static final UXKey HomePage_default_site_wangyi__OnClick = new UXKey(1091401, "导航页点击,网易链接");
    public static final UXKey HomePage_default_site_taobao_OnClick = new UXKey(1091601, "导航页点击,淘宝链接");
    public static final UXKey HomePage_default_site_fenghuang_OnClick = new UXKey(1091903, "导航页点击,凤凰链接");
    public static final UXKey HomePage_default_site_easou_OnClick = new UXKey(1091907, "导航页点击,easou链接");
    public static final UXKey HomePage_default_site_sohu_OnClick = new UXKey(1091902, "导航页点击,sohu链接");
    public static final UXKey HomePage_default_site_sina_weibo_OnClick = new UXKey(1091901, "导航页点击,sina微博链接");


/*    public static final UXKey HomePage_default_site_yingyonghui_OnClick = new UXKey(, "")1091701;
    public static final UXKey HomePage_default_site_youku_OnClick = new UXKey(1091904, "导航页点击,优酷链接");
    public static final UXKey HomePage_default_site_tudou_OnClick = new UXKey(, "")1091801;*/


    public static final UXKey Homepage_quicklink_longclick_delete = new UXKey(1092301, "长按导航页图标后, 点击删除");
    public static final UXKey Homepage_quicklink_longclick = new UXKey(1092302, "长按导航页图标");


    //xuesong: 长按快捷链接, 选择编辑
    public static final UXKey Homepage_quicklink_longclick_shownoption_edit = new UXKey(1092401, "360安全导航,长按快捷链接, 选择编辑");
    //xuesong: 长按快捷链接, 选择删除
    public static final UXKey Homepage_quicklink_longclick_shownoption_delete = new UXKey(1092402, "360安全导航,长按快捷链接, 选择删除");
    //xuesong: 点击, 添加新的快链
    public static final UXKey Homepage_quicklink_click_to_add = new UXKey(1092501, "点击, 添加新的快链");

/*    //wuke: 点击给360提意见横幅
    public static final int HomePage_url_feedback_OnClick = new UXKey(, "")1092600;
    //wuke: 点击设置默认浏览器横幅 yes
    public static final int HomePage_url_set_default_browser_yes = new UXKey(, "")1092601;
    //wuke: 点击设置默认浏览器横幅 no
    public static final int HomePage_url_set_default_browser_no = new UXKey(, "")1092602;*/

/*
    //wuke: 点击网址导航滚动按钮
    public static final int HomePage_navigation_web = new UXKey(, "")1092604;
    //wuke: 点击报刊杂志滚动按钮
    public static final int HomePage_navigation_reader = new UXKey(, "")1092605;
    */
    //wuke: homePage scroll 记录
    public static final UXKey HomePage_scroll = new UXKey(1099000, "homePage scroll 记录") ;

    /**
     * Upper Tab
     */
    //xuesong: 点x关闭tab
    public static final UXKey UpperTab_OnClose_OneClick = new UXKey(1100101, "点x关闭tab");
    //xuesong: 双击关闭tab
    public static final UXKey UpperTab_OnClose_DoubleClick = new UXKey(1100102, "双击关闭tab");
    //xuesong: 切换tab
    public static final UXKey UpperTab_SwitchTab_OnClick = new UXKey(1100201, "切换tab");
    //xuesong: 新开一个tab
    public static final UXKey UpperTab_NewTab_OnClick = new UXKey(1100301, "新开一个tab");
    //xuesong: 长按一个tab
    public static final UXKey UpperTab_OnLongClick_Toshow_Option = new UXKey(1100401, "长按一个tab");
    //xuesong: 长按一个tab, 选择关闭当前
    public static final UXKey UpperTab_OnLongClick_SelectOption_CloseCurrent_OnClick = new UXKey(1100501, "长按一个tab, 选择关闭当前");
    //xuesong: 长按一个tab, 选择关闭其他
    public static final UXKey UpperTab_OnLongClick_SelectOption_CloseOthers_OnClick = new UXKey(1100502, "长按一个tab, 选择关闭其他");
    //xuesong: 长按一个tab, 选择关闭所有
    public static final UXKey UpperTab_OnLongClick_SelectOption_CloseAll_OnClick = new UXKey(1100503, "长按一个tab, 选择关闭所有");

    /**
     * Settings singleton
     */
    //xuesong: 多标签浏览设置, 默认
    public static final UXKey Settings_Singleton_newtab_default = new UXKey(1110101, "多标签浏览设置, 默认");
    //xuesong: 多标签浏览设置, 在当前页打开
    public static final UXKey Settings_Singleton_newtab_current = new UXKey(1110102, "多标签浏览设置, 在当前页打开");
    //xuesong: 多标签浏览设置, 在前台打开
    public static final UXKey Settings_Singleton_newtab_foreground = new UXKey(1110103, "多标签浏览设置, 在前台打开");
    //xuesong: 多标签浏览设置, 在后台打开
    public static final UXKey Settings_Singleton_newtab_background = new UXKey(1110104, "多标签浏览设置, 在后台打开");
    //xuesong: 标签栏显示 显示
    public static final UXKey Settings_Singleton_uppertab_visible = new UXKey(1110201, "标签栏显示 显示");
    //xuesong: 标签栏显示 隐藏
    public static final UXKey Settings_Singleton_uppertab_unvisible = new UXKey(1110202, "标签栏显示 隐藏");
    //xuesong: 屏幕方向 跟随系统
    public static final UXKey Settings_Singleton_screendirection_system = new UXKey(1110301, "屏幕方向 跟随系统");
    //xuesong: 屏幕方向 竖屏
    public static final UXKey Settings_Singleton_screendirection_portrait = new UXKey(1110302, "屏幕方向 竖屏");
    //xuesong: 屏幕方向 横屏
    public static final UXKey Settings_Singleton_screendirection_landscape = new UXKey(1110303, "屏幕方向 横屏");
    //jiangyang: 网络收藏夹保持登录
    public static final UXKey Settings_Singleton_onlinebookmark_autologin = new UXKey(1110304, "网络收藏夹保持登录");
    //jiangyang: 非网络收藏夹保持登录
    public static final UXKey Settings_Singleton_onlinebookmark_notautologin = new UXKey(1110305, "非网络收藏夹保持登录");
    //xuesong: 浏览模式:急速
    public static final UXKey Settings_Singleton_webframe_mode_fast = new UXKey(1110306, "浏览模式:急速");
    //xuesong: 浏览模式:正常
    public static final UXKey Settings_Singleton_webframe_mode_normal = new UXKey(1110307, "浏览模式:正常");
    //xuesong: 预读开关: 开
    public static final UXKey Settings_Singleton_web_pre_read_open = new UXKey(1110308, "预读开关: 开");
    //xuesong: 预读开关: 关
    public static final UXKey Settings_Singleton_web_pre_read_close = new UXKey(1110309, "预读开关: 关");
    //xuesong: 无痕模式: 开
    public static final UXKey Settings_Singleton_traceless_open = new UXKey(1110310, "无痕模式: 开");
    //xuesong: 无痕模式: 关
    public static final UXKey Settings_Singleton_traceless_close = new UXKey(1110311, "无痕模式: 关");

    public static final UXKey Settings_Singleton_UA_mobile_mode = new UXKey(1110313, "手机模式UA");
    public static final UXKey Settings_Singleton_UA_desktop_mode = new UXKey(1110314, "电脑模式UA");
    public static final UXKey Settings_Singleton_slide_to_leftPane_on = new UXKey(1110315, "支持滑出左侧栏：开启");
    public static final UXKey Settings_Singleton_slide_to_leftPane_off = new UXKey(1110316, "支持滑出左侧栏：关闭");
    public static final UXKey Settings_Singleton_slide_to_ilike_on = new UXKey(1110317, "支持滑出我喜欢：开启");
    public static final UXKey Settings_Singleton_slide_to_ilike_off = new UXKey(1110318, "支持滑出我喜欢：关闭");
    public static final UXKey Settings_Singleton_display_360SafeSites_on = new UXKey(1110319, "启动显示360安全网址：开启");
    public static final UXKey Settings_Singleton_display_360SafeSites_off = new UXKey(1110320, "启动显示360安全网址：关闭");
    public static final UXKey Settings_Singleton_display_most_viste_on = new UXKey(1110321, "新建标签显示最常访问：开启");
    public static final UXKey Settings_Singleton_display_most_viste_off = new UXKey(1110322, "新建标签显示最常访问：关闭");

    /**
     * browser launch
     */
    //wuke: 冷/热启动次数
    public static final UXKey Launch_browser = new UXKey(1111000, "冷/热启动次数");

    /**
     * 网络收藏夹
     */
    //wuke: 网络收藏夹用户每天使用次数
    public static final UXKey Online_Bookmark_Daily = new UXKey(1111001, "网络收藏夹用户每天使用次数 ");

    /**
     * Reader Home
     */
    // kangyonggen
    // 点击随便看看
    public static final UXKey Reader_Home_Random_Read_OnClick = new UXKey(1120101, "点击随便看看");
    // 点击我的收藏
    public static final UXKey Reader_Home_My_Collection_OnClick = new UXKey(1120102, "点击我的收藏");
    // 点击添加订阅
    public static final UXKey Reader_Home_Add_New_Channel_OnClick = new UXKey(1120103, "点击添加订阅");
    // 长按频道
    public static final UXKey Reader_Home_Icon_Long_OnClick = new UXKey(1120104, "长按频道");
    // 删除频道
    public static final UXKey Reader_Home_Icon_Deleted = new UXKey(1120105, "删除频道");
    // 物理返回键取消频道删除状态
    public static final UXKey Reader_Home_Icon_Delete_State_Cancel_With_Back = new UXKey(1120106, "物理返回键取消频道删除状态");
    // 点击空白处取消频道删除状态
    public static final UXKey Reader_Home_Icon_Delete_State_Cancel_With_Click_Space = new UXKey(1120107, "点击空白处取消频道删除状态");
    // 其他频道点击
    public static final UXKey Reader_Home_SubScribe_OnClick = new UXKey(1120108, "其他频道点击");
    // 阅读首页所有图标日总点击数
    public static final UXKey Reader_Daily_Icon_OnClick_Times = new UXKey(1120109, "阅读首页所有图标日总点击数");
    // 阅读总PV - by Jiongxuan Zhang
    public static final UXKey Reader_PV_Times = new UXKey(1120110, "阅读总PV");
    // 我喜欢用户数 - by Jiongxuan Zhang
    public static final UXKey Reader_Woxihuan_User = new UXKey(1120111, "我喜欢用户数");
    // 我喜欢总PV - by Jiongxuan Zhang
    public static final UXKey WoXiHuan_PV_Times = new UXKey(1120112, "我喜欢总PV");

    /**
     * Reader random read
     */
    // kangyonggen
    // 用户摇动手机
    public static final UXKey Reader_Random_Read_Shake = new UXKey(1130201, "用户摇动手机");
    // 点击随机频道
    public static final UXKey Reader_Random_Read_Channel_Select = new UXKey(1130202, "点击随机频道");
    // 订阅随机频道
    public static final UXKey Reader_Random_Read_Add_Channel = new UXKey(1130203, "订阅随机频道");
    // 物理键返回
    public static final UXKey Reader_Random_Read_Back_Key_OnClick = new UXKey(1130204, "物理键返回");
    // 虚拟回退键
    public static final UXKey Reader_Shake_Shaked_And_Back_Times = new UXKey(1200903, "虚拟回退键");

    /**
     * Reader collection
     */
    // kangyonggen
    // 一键清除收藏
    public static final UXKey Reader_Collection_Clear_All_Collection = new UXKey(1140301, "一键清除收藏");
    // 长按取消收藏
    public static final UXKey Reader_Collection_Delete_Collection_With_Long_Click = new UXKey(1140302, "长按取消收藏");
    // 物理键返回
    public static final UXKey Reader_Collection_Back_Key_OnClick = new UXKey(1140303, "物理键返回");
    // 工具条返回
    public static final UXKey Reader_Collection_Back_with_Action_Bar = new UXKey(1140304, "工具条返回");

    /**
     * Reader add channel
     */
    // kangyonggen
    // 切换显示模式
    public static final UXKey Reader_Classify_ControlBar_SwitcherStyle_OnClick = new UXKey(1150404, "切换显示模式");
    // 刷新频道列表
    public static final UXKey Reader_Classify_ControlBar_Refresh_List = new UXKey(1150405, "刷新频道列表");
    // 物理键返回
    public static final UXKey Reader_Classify_ControlBar_Back_Key_OnClick = new UXKey(1150406, "物理键返回");
    // 工具条返回
    public static final UXKey Reader_Classify_ControlBar_Back_With_Action_Bar = new UXKey(1150407, "工具条返回");
    // 搜索键
    public static final UXKey Reader_Classify_ControlBar_Search_OnClick = new UXKey(1150408, "搜索键");
    // 切换网格模式
    public static final UXKey Reader_Classify_Grid_Style_State = new UXKey(1150409, "切换网格模式");
    // 切换列表模式
    public static final UXKey Reader_Classify_List_Style_State = new UXKey(1150410, "切换列表模式");

    /**
     * Reader channel article list
     */
    // kangyonggen
    // 下拉刷新
    public static final UXKey Reader_Article_List_Pull_To_Refresh = new UXKey(1160501, "下拉刷新");
    // 滑动加载更多
    public static final UXKey Reader_Article_List_Fling_To_Load_More = new UXKey(1160502, "滑动加载更多");
    // 分享
    public static final UXKey Reader_Article_List_Share_OnClick = new UXKey(1160503, "分享");
    // 工具条刷新
    public static final UXKey Reader_Article_List_Refresh_Width_Action_Bar = new UXKey(1160504, "工具条刷新");
    // 取消订阅
    public static final UXKey Reader_Article_List_Cancel_SubScribe = new UXKey(1160506, "取消订阅");
    // 添加订阅
    public static final UXKey Reader_Article_List_Add_SubScribe = new UXKey(1160516, "添加订阅");
    // 物理键返回
    public static final UXKey Reader_Article_List_Back_Key_OnClick = new UXKey(1160507, "物理键返回");
    // 工具条返回
    public static final UXKey Reader_Article_List_Back_With_Action_Bar = new UXKey(1160508, "工具条返回");
    // 展开Tool bar
    public static final UXKey Reader_Article_List_ControlBar_Opening = new UXKey(1160509, "展开Tool bar");
    // 收起Tool bar
    public static final UXKey Reader_Article_List_ControlBar_Closing = new UXKey(1160510, "收起Tool bar");

    /**
     * Reader article detail read
     */
    // kangyonggen
    // 分享文章
    public static final UXKey Reader_Article_Detail_Share_Article = new UXKey(1170601, "分享文章");
    // 收藏文章
    public static final UXKey Reader_Article_Detail_Collection_Article = new UXKey(1170602, "收藏文章");
    // 取消收藏
    public static final UXKey Reader_Article_Detail_Cancel_Collection = new UXKey(1170603, "取消收藏");
    // 物理键返回
    public static final UXKey Reader_Article_Detail_Back_Key_OnClick = new UXKey(1170604, "物理键返回");
    // 工具条返回
    public static final UXKey Reader_Article_Detial_Back_With_Action_Bar = new UXKey(1170605, "工具条返回");
    // 查看图片
    public static final UXKey Reader_Article_Detail_Image_OnClick = new UXKey(1170606, "查看图片");
    // 复制文本
    public static final UXKey Reader_Article_Detail_Long_Copy_Article = new UXKey(1170607, "复制文本");
    // 分享图片
    public static final UXKey Reader_Article_Detail_Share_Image = new UXKey(1170608, "分享图片");
    // 保存图片
    public static final UXKey Reader_Article_Detail_Save_Image = new UXKey(1170609, "保存图片");
    // 展开Tool bar
    public static final UXKey Reader_Article_Detail__ControlBar_Opening = new UXKey(1170610, "展开Tool bar");
    // 收起Tool bar
    public static final UXKey Reader_Article_Detail__ControlBar_Closing = new UXKey(1170611, "收起Tool bar");
    // 回退键点击
    public static final UXKey Reader_Article_Detail_ControlBar_Back_OnClick = new UXKey(1170612, "回退键点击");

    /**
     * Reader menu
     */
    // kangyonggen
    // 添加收藏
    public static final UXKey Reader_Menu_Add_Collection = new UXKey(1180701, "添加收藏");
    // 取消收藏
    public static final UXKey Reader_Menu_Cancel_Collection = new UXKey(1180702, "取消收藏");
    // 刷新
    public static final UXKey Reader_Menu_Refresh = new UXKey(1180703, "阅读刷新");
    // 夜间模式
    public static final UXKey Reader_Menu_Nightly_Mode = new UXKey(1180704, "夜间模式");
    // 选择小字体
    public static final UXKey Reader_Menu_Font_Small_Selected = new UXKey(1180705, "选择小字体");
    // 选择中字体
    public static final UXKey Reader_Menu_Font_Middle_Selected = new UXKey(1180706, "选择中字体");
    // 选择大字体
    public static final UXKey Reader_Menu_Font_Large_Selected = new UXKey(1180707, "选择大字体");
    // 阅读设置
    public static final UXKey Reader_Menu_Reader_Setting = new UXKey(1180708, "阅读设置");
    // 显示图片
    public static final UXKey Reader_Menu_Show_Image = new UXKey(1180709, "显示图片");
    // 无图模式
    public static final UXKey Reader_Menu_No_Image = new UXKey(1180710, "无图模式");
    // 一键清除
    public static final UXKey Reader_Menu_Clear_History = new UXKey(1180711, "一键清除");
    // 退出阅读
    public static final UXKey Reader_Menu_Exit_Reader = new UXKey(1180712, "退出阅读");
    // 离线下载
    public static final UXKey Reader_Menu_Offline_DownList_OnClick = new UXKey(1180713, "离线下载");
    // 我的文章
    public static final UXKey Reader_Menu_MyArticle_OnClick = new UXKey(1180714, "我的文章");

    /**
     * 图片缩略页
     */
    // kangyonggen
    // 后退键
    public static final UXKey Reader_ImageTabloid_ControlBar_Back_OnClick = new UXKey(1190801, "后退键");
    // 刷新键
    public static final UXKey Reader_ImageTabloid_ControlBar_Refresh_OnClick = new UXKey(1190802, "刷新键");
    // 分享键
    public static final UXKey Reader_ImageTabloid_ControlBar_Share_OnClick = new UXKey(1190803, "分享键");
    // 订阅键
    public static final UXKey Reader_ImageTabloid_ControlBar_SubScribe_OnClick = new UXKey(1190804, "订阅键");

    /**
     * Url loading
     */
    //xuesong: 浏览器每天执行打开页面的操作次数. wap1.x算为直连
    public static final UXKey WebKit_OnLoadUrl = new UXKey(1201001, "浏览器每天执行打开页面的操作次数. wap1.x算为直连");
    //私有协议
    public static final UXKey WebKit_Private_OnLoadUrl = new UXKey(1201002, "私有协议");

    /**
     * Active Time
     */
     public static final UXKey Browser_Active_Time = new UXKey(1301001,"浏览器活动时间");

     /*
      * Read Later: 稍后阅读
      */
     // 浏览器主菜单打开稍后阅读
     public static final UXKey Reade_Later_Browser_Menu_Open_OnClick = new UXKey(1310101, "主菜单—稍后阅读");
     // 浏览器网页长按菜单添加到稍后阅读
     public static final UXKey Reade_Later_Browser_ContextMenu_Add_OnClick = new UXKey(1310201, "网页长按菜单添加到稍后阅读");
     // 在稍后阅读页面点击刷新按钮
     public static final UXKey Reade_Later_List_Activity_Refresh_OnClick = new UXKey(1310301, "刷新按钮");
     // 在稍后阅读页面点击添加页面按钮
     public static final UXKey Reade_Later_List_Activity_Add_OnClick = new UXKey(1310302, "添加页面按钮");
     // 在稍后阅读页面点击一条未读记录
     public static final UXKey Reade_Later_List_Activity_Unread_Item_OnClick = new UXKey(1310303, "点击一条未读记录，打开页面");
     // 在稍后阅读页面点击一条已读记录
     public static final UXKey Reade_Later_List_Activity_Read_Item_OnClick = new UXKey(1310304, "点击一条已读记录，打开页面");
     // 在稍后阅读页面删除一条未读记录
     public static final UXKey Reade_Later_List_Activity_Item_LongOnClick_Delete = new UXKey(1310305, "删除一条记录");

     /**
      * 壁纸管理key
      */
     //用户使用本地壁纸（从相册中选取的)
     public static final UXKey Wallpaper_Local = new UXKey(1410000, "使用本地壁纸");
     //用户使用非本地壁纸（包括网络壁纸和程序自带的壁纸）
     //如单独统计每张图片使用次数，用该id加图片id作为key
     public static final UXKey Wallpaper_Net = new UXKey(1420000, "使用网络壁纸");

     /**
      * 网络收藏夹打点
      */
     //使用添加网络收藏夹次数 - ?? Urlbar_farvorite_add_online_bookmark_OnClick
     //public static final UXKey Network_Bookmark_Add = new UXKey(1430101, "使用添加网络收藏夹次数");
     //点击网络收藏夹次数 - ?? LeftPane_button_online_bookmark_OnClick
     //public static final UXKey Network_Bookmark_Click_Show = new UXKey(1430102, "点击网络收藏夹次数");
     //点击网络收藏夹里所有链接总和次数
     public static final UXKey Network_Bookmark_Click_Counter = new UXKey(1430103, "点击网络收藏夹里所有链接总和次数");
     //点击同步按钮次数 - ?? LeftPane_button_sync_immediately_OnClick
     //public static final UXKey Network_Bookmark_Sync = new UXKey(1430104, "点击同步按钮次数");
     //点击查看历史备份次数
     public static final UXKey Network_Bookmark_Show_History = new UXKey(1430105, "点击查看历史备份次数");
     //恢复备份历史次数
     public static final UXKey Network_Bookmark_Recover_History_Counter = new UXKey(1430106, "恢复备份历史次数");

    public static final UXKey NetWork_10086 = new UXKey(4600000, "中国移动");
    public static final UXKey NetWork_10010 = new UXKey(4600100, "中国联通");
    public static final UXKey NetWork_10000 = new UXKey(4600300, "中国电信");

    public static final UXKey NetWork_TYPE_MOBILE = new UXKey(4699990, "TYPE_MOBILE");
    public static final UXKey NetWork_TYPE_WIFI = new UXKey(4699991, "TYPE_WIFI");
    public static final UXKey NetWork_TYPE_MOBILE_MMS = new UXKey(4699992, "TYPE_MOBILE_MMS");
    public static final UXKey NetWork_TYPE_MOBILE_SUPL = new UXKey(4699993, "TYPE_MOBILE_SUPL");
    public static final UXKey NetWork_TYPE_MOBILE_DUN = new UXKey(4699994, "TYPE_MOBILE_DUN");
    public static final UXKey NetWork_TYPE_MOBILE_HIPRI = new UXKey(4699995, "TYPE_MOBILE_HIPRI");
    public static final UXKey NetWork_TYPE_WIMAX = new UXKey(4699996, "TYPE_WIMAX");

    public static final UXKey START_APP_TYPE_BROWSER = new UXKey(4700000, "从浏览器图标进入");
    public static final UXKey START_APP_TYPE_READER = new UXKey(4700001, "从阅读图标进入");
    
    public static final UXKey HomePage_competitive_link_OnClick = new UXKey(2000000, "点击导航页所有格子次数");
    public static final UXKey HomePage_navPager_link_OnClick = new UXKey(2000010, "点击360安全网址");
    public static final UXKey HomePage_mostVisit_link_OnClick = new UXKey(2000020, "点击最长访问");
    public static final UXKey HomePage_url_ilike_OnClick = new UXKey(2000030, "点击启动我喜欢");    
    public static final UXKey HomePage_url_baohe_OnClick = new UXKey(2000040, "点击启动插件宝盒");
    public static final UXKey Navigation_url_OnClick = new UXKey(2000050, " 点击360安全网址二级页面中链接");    
    public static final UXKey Mostvisit_url_OnClick = new UXKey(2000051, " 点击最长访问二级页面中链接");    
    public static final UXKey Global_PV = new UXKey(2000060, " 点击所有网页PV");    
    
    
}
