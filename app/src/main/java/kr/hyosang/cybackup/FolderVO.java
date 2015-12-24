package kr.hyosang.cybackup;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Hyosang on 2015-12-22.
 */
public class FolderVO {
    public String fid;
    public String name;
    public int totalCount = 0;
    public List<ArticleVO> articles = new ArrayList<ArticleVO>();

    public FolderVO(String id, String nm) {
        fid = id;
        name = nm;
    }
}
