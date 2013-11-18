package com.qihoo.ilike.util;

import com.qihoo.ilike.manager.LoginManager;
import com.qihoo.ilike.vo.User;
import com.qihoo.ilike.vo.VolumeList;

public class Valuable {
    private static User user;
    private static VolumeList volumeList;

    public static void clear() {
        user = null;
        volumeList = null;
    }

    public static String getQid() {
        return LoginManager.getQid();
    }

    public static void setQid(String qid) {
        // TODO
    }

    public static VolumeList getVolumeList() {
        return Valuable.volumeList;
    }

    public static void setVolumeList(VolumeList volumeList) {
        Valuable.volumeList = volumeList;
    }

    public static User getUser() {
        return Valuable.user;
    }

    public static void setUser(User user) {
        Valuable.user = user;
    }
}
