package kr.hyosang.cybackup;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Hyosang on 2015-12-18.
 */
public class CyData {
    private static CyData mInstance = new CyData();

    private CyData() {
        //block default constructor

    }

    public static CyData getInstance() {
        return mInstance;
    }

    public String mTid = "";
}
