package moe.shizuku.bridge.utils;

import android.os.UserManager;

/**
 * Created by Rikka on 2017/4/14.
 */

public class UserManagerUtils {

    public static boolean isUsingWorkProfile(UserManager userManager) {
        return userManager.getUserProfiles().size() > 1;
    }
}
