package kr.hyosang.cybackup;

import android.util.Log;

/**
 * Created by Hyosang on 2015-12-23.
 */
public class ArticleVO {
    public String id;
    public long date;

    public ArticleVO(String id_date) {
        String [] vals = id_date.split("_");
        id = vals[0];
        try {
            date = Long.parseLong(vals[1]);
        }catch(NumberFormatException e) {
            Log.e("Parser", Log.getStackTraceString(e));
        }
    }
}
