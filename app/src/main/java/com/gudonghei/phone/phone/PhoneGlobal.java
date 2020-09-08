package com.gudonghei.phone.phone;

import android.content.Context;



/**
 * Created by Jianghua on 2018/8/28.
 */

public final class PhoneGlobal {

    private static LinphoneMiniManager miniManager;


    public static LinphoneMiniManager getMiniManager(Context context) {
        if (miniManager == null) {
                miniManager = new LinphoneMiniManager(context);
        }
        return miniManager;
    }

}
