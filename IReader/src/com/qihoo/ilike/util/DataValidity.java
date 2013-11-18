package com.qihoo.ilike.util;

import com.qihoo.ilike.vo.User;
import com.qihoo.ilike.vo.VolumeList;

public class DataValidity {
	public static boolean checkUserValidityForShareTo(User user) {
		boolean ret = false;
		if ((user != null)) {
			ret = true;
		}
		return ret;
	}

	public static boolean checkVolumeListValidity(VolumeList volumeList) {
		boolean ret = false;
		if ((volumeList != null) && (volumeList.volumeList != null)
				&& (volumeList.volumeList.length > 0)) {
			ret = true;
		}
		return ret;
	}
}
