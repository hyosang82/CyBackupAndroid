package kr.hyosang.cybackup;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import kr.hyosang.common.HttpUtil;

/**
 * Created by Hyosang on 2015-12-18.
 */
public class DownloadActivity extends Activity {
    private static final String TAG = "DownloadActivity";

    private static int MSG_APPEND_TEXT = 0x01;
    private static int MSG_SHOW_IMAGE = 0x02;

    private static final String SAVE_ROOT = Environment.getExternalStorageDirectory().getAbsolutePath() + "/CyBackup";

    private TextView mInfoText1;
    private TextView mInfoText2;
    private ImageView mPreview;
    private CheckBox mChkNomedia;
    private CheckBox mChkMakezip;

    private List<FolderVO> mFolderList = new ArrayList<FolderVO>();
    private int mFolderIdx = 0;
    private int mArticleIdx = 0;
    private int mSerial = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_download);

        mInfoText1 = (TextView) findViewById(R.id.txt_info_1);
        mInfoText2 = (TextView) findViewById(R.id.txt_info_2);
        mPreview = (ImageView) findViewById(R.id.preview);
        mChkNomedia = (CheckBox) findViewById(R.id.chk_nomedia);
        mChkMakezip = (CheckBox) findViewById(R.id.chk_makezip);

        findViewById(R.id.btn_start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.setEnabled(false);
                mChkNomedia.setEnabled(false);
                mChkMakezip.setEnabled(false);

                mFolderIdx = 0;
                startFolder();
            }
        });

        requestFolders();

        ViewGroup.LayoutParams lp = mPreview.getLayoutParams();
        lp.height = lp.width;
        mPreview.setLayoutParams(lp);
    }

    private void requestFolders() {
        String url = "http://cy.cyworld.com/home/" + CyData.getInstance().mTid + "/menu?type=folder&_=" + System.currentTimeMillis();
        HttpUtil.HttpData folderList = HttpUtil.HttpData.createGetRequest(url);
        folderList.mListener = new HttpUtil.HttpListener() {
            @Override
            public void onCompleted(HttpUtil.HttpData data) {
                Pattern p = Pattern.compile("<span><em>사진첩<\\/em><\\/span>(.*?)<\\/ul>", Pattern.DOTALL);
                Matcher m = p.matcher(data.responseBody);

                if(m.find()) {
                    parsingFolderList(m.group(1));
                }else {
                    Log.d(TAG, "Not found");
                }
            }
        };

        HttpUtil.getInstance().add(folderList);
    }

    private void parsingFolderList(String html) {
        Pattern p = Pattern.compile("value=\\\"([0-9A-F]+)\\\".*?<span><em>([^<]+)<\\/em>");
        Matcher m = p.matcher(html);

        StringBuffer sb = new StringBuffer();

        while(m.find()) {
            FolderVO f = new FolderVO(m.group(1), m.group(2));

            mFolderList.add(f);

            sb.append(f.fid).append(" : ").append(f.name).append("\n");
        }

        mInfoText1.setText("검색된 사진첩 폴더\n" + sb.toString());
    }

    private void startFolder() {
        FolderVO folder = mFolderList.get(mFolderIdx);

        updateLog("폴더 진입 : " + folder.name);

        String url = "http://cy.cyworld.com/home/" + CyData.getInstance().mTid + "/postlist?startdate=&enddate=&folderid=" + folder.fid + "&tagname=&_=" + System.currentTimeMillis();
        HttpUtil.HttpData list = HttpUtil.HttpData.createGetRequest(url);
        list.mListener = new HttpUtil.HttpListener() {
            @Override
            public void onCompleted(HttpUtil.HttpData data) {
                FolderVO folder = mFolderList.get(mFolderIdx);

                Pattern p = Pattern.compile("<article .*? id=\\\"([0-9A-F_]+)\\\"");
                Matcher m = p.matcher(data.responseBody);

                ArticleVO article = null;

                while(m.find()) {
                    article = new ArticleVO(m.group(1));

                    folder.articles.add(article);
                }

                Log.d(TAG, folder.name + " 첫 페이지 " + folder.articles.size());

                //다음페이지 여부 체크
                boolean bMore = false;
                p = Pattern.compile("<input .*? id=\\\"postTotCnt\\\".*value=\\\"([0-9]+)\\\"");
                m = p.matcher(data.responseBody);
                if(m.find()) {
                    try {
                        int totalCnt = Integer.parseInt(m.group(1), 10);
                        if(totalCnt > folder.articles.size()) {
                            bMore = true;
                        }
                        folder.totalCount = totalCnt;
                    }catch(NumberFormatException e) {
                        Log.e(TAG, Log.getStackTraceString(e));
                    }
                }

                if(bMore && (article != null)) {
                    //다음페이지 진행
                    Log.d(TAG, "다음페이지 있음 : " + folder.name);
                    folderMore(article.id, article.date);
                }else {
                    //사진 작성글 다운로드 시작
                    nextFolder();
                }
            }
        };

        HttpUtil.getInstance().add(list);
    }

    private void folderMore(String lastid, long lastdate) {
        FolderVO folder = mFolderList.get(mFolderIdx);

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(lastdate);
        String yyyymm = "" + c.get(Calendar.YEAR);
        int m = c.get(Calendar.MONTH) + 1;
        yyyymm += (m < 10) ? ("0" + m) : m;

        String url = "http://cy.cyworld.com/home/" + CyData.getInstance().mTid + "/postmore?lastid=" + lastid + "&lastdate=" + lastdate + "&startdate=&enddate=&folderid=" + folder.fid + "&tagname=&lastyymm=" + yyyymm + "&listsize=24&_=" + System.currentTimeMillis();
        HttpUtil.HttpData list = HttpUtil.HttpData.createGetRequest(url);
        list.mListener = new HttpUtil.HttpListener() {
            @Override
            public void onCompleted(HttpUtil.HttpData data) {
                FolderVO folder = mFolderList.get(mFolderIdx);

                Pattern p = Pattern.compile("<article .*? id=\\\"([0-9A-F_]+)\\\"");
                Matcher m = p.matcher(data.responseBody);

                ArticleVO article = null;
                while(m.find()) {
                    article = new ArticleVO(m.group(1));
                    folder.articles.add(article);
                }

                Log.d(TAG, folder.name + " 글 목록 구성중 " + folder.articles.size() + "/" + folder.totalCount);
                updateLog("[" + folder.name + "] 목록 구성중 : " + folder.articles.size() + "/" + folder.totalCount);

                if((folder.articles.size() < folder.totalCount) && (article != null)) {
                    folderMore(article.id, article.date);
                }else {
                    //다음 폴더 진행
                    nextFolder();
                }
            }
        };

        HttpUtil.getInstance().add(list);
    }

    private void nextFolder() {
        mFolderIdx++;
        if(mFolderIdx < mFolderList.size()) {
            startFolder();
        }else {
            //완료
            Log.d(TAG, "목록구성 완료");
            updateLog("목록구성 완료");

            startDownload();
        }
    }

    private void startDownload() {
        mFolderIdx = 0;

        if(mChkNomedia.isChecked()) {
            //nomedia 파일 생성
            try {
                File f = new File(SAVE_ROOT + "/.nomedia");
                f.getParentFile().mkdirs();
                f.createNewFile();
            }catch(IOException e) {
                Log.e("TEST", Log.getStackTraceString(e));
            }
        }

        startFolderDownload();
    }

    private void startFolderDownload() {
        if(mFolderIdx < mFolderList.size()) {
            mArticleIdx = mFolderList.get(mFolderIdx).articles.size() - 1;
            mSerial = 1;

            nextDownload();
        }else {
            updateLog("다운로드 완료");

            if(mChkMakezip.isChecked()) {
                //압축파일 생성
                (new Thread() {
                    @Override
                    public void run() {
                        startZip();
                    }
                }).start();
            }
        }

    }

    private void nextDownload() {
        if(mArticleIdx >= 0) {
            ArticleVO article = mFolderList.get(mFolderIdx).articles.get(mArticleIdx);

            String url = "http://cy.cyworld.com/home/" + CyData.getInstance().mTid + "/post/" + article.id;
            HttpUtil.HttpData down = HttpUtil.HttpData.createGetRequest(url);
            down.mListener = new HttpUtil.HttpListener() {
                @Override
                public void onCompleted(HttpUtil.HttpData data) {
                    String title = "";
                    String fileurl = null;

                    Pattern p = Pattern.compile("<h2 .*?id=\\\"cyco-post-title\\\".*?>(.*?)<\\/h2>", Pattern.DOTALL);
                    Matcher m = p.matcher(data.responseBody);

                    if(m.find()) {
                        title = m.group(1);
                    }else {
                        Log.d(TAG, "글 제목 찾기 실패 : " + data.url);
                    }

                    //파일 다운로드 주소 찾기
                    p = Pattern.compile("<figure>.*?srctext=\\\"([^\\\"]+)\\\".*?<\\/figure>", Pattern.DOTALL);
                    m = p.matcher(data.responseBody);

                    if(m.find()) {
                        fileurl = m.group(1);
                    }else {
                        //플래시 파일인 경우
                        p = Pattern.compile("<object.*?data=\\\"([^\\\"]+)\\\"");
                        m = p.matcher(data.responseBody);

                        if(m.find()) {
                            fileurl = m.group(1);
                        }
                    }

                    if(fileurl == null) {
                        Log.d(TAG, "주소찾기 실패 : " + data.url);
                    }else {
                        //파일 다운로드
                        Log.d(TAG, title + " / File = " + fileurl);

                        addDownloadFile(data.url, title, fileurl);

                        updateLog("파일 다운로드중(" + mSerial + "/" + mFolderList.get(mFolderIdx).totalCount + ") : " + title);
                    }

                    mArticleIdx--;
                    mSerial++;

                    nextDownload();
                }
            };
            HttpUtil.getInstance().add(down);
        }else {
            //refresh MediaStore
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(SAVE_ROOT))));

            mFolderIdx++;
            startFolderDownload();
        }
    }

    private void addDownloadFile(String referer, String title, String fileurl) {
        //원본 경로에서 확장자 추출
        String filename;
        int idx = fileurl.lastIndexOf('.');
        if(idx > 0) {
            String ext = fileurl.substring(idx + 1);

            filename = String.format("%05d_%s.%s", mSerial, title, ext);
        }else {
            filename = String.format("%05d_%s", mSerial, title);
        }

        String folderName = mFolderList.get(mFolderIdx).name.replaceAll("[\\\\\\/:\\*\\?\\\"<>\\|]", "");
        filename = filename.replaceAll("[\\\\\\/:\\*\\?\\\"<>\\|]", "");

        //저장할 경로
        String savepath = String.format("%s/%s/%s", SAVE_ROOT, folderName, filename);

        HttpUtil.HttpData filedown = HttpUtil.HttpData.createGetRequest(fileurl);
        filedown.requestHeaders.put("Referer", referer);
        filedown.saveTo = savepath;
        filedown.mListener = new HttpUtil.HttpListener() {
            @Override
            public void onCompleted(HttpUtil.HttpData data) {
                mHandler.obtainMessage(MSG_SHOW_IMAGE, data.saveTo).sendToTarget();

            }
        };


        HttpUtil.getInstance().add(filedown);
    }

    private void startZip() {
        ZipOutputStream zos = null;

        try {
            zos = new ZipOutputStream(new FileOutputStream(SAVE_ROOT + "/CyBackup.zip"));
            File root = new File(SAVE_ROOT);
            File[] dirs = root.listFiles();

            for (File f : dirs) {
                if (f.isDirectory()) {
                    zipFolder(zos, f);
                }
            }
        }catch(IOException e) {
            Log.e("TEST", Log.getStackTraceString(e));
        }finally {
            if (zos != null) {
                try { zos.close(); } catch(IOException e) { }
            }
        }

        updateLog("압축파일 생성 완료");
    }

    private void zipFolder(ZipOutputStream zos, File dir) {
        int cutLen = SAVE_ROOT.length() + 1;
        File [] files = dir.listFiles();
        byte [] buf = new byte[50 * 1024];  //50K
        int nRead;

        for(File f : files) {
            if(f.isDirectory()) {
                zipFolder(zos, f);
            }else {
                try {
                    String relPath = f.getAbsolutePath().substring(cutLen);
                    updateLog("압축중: " + relPath);
                    Log.d("TEST", "Zip = " + relPath);

                    FileInputStream fis = new FileInputStream(f);
                    ZipEntry entry = new ZipEntry(relPath);
                    zos.putNextEntry(entry);
                    while( (nRead = fis.read(buf)) > 0) {
                        zos.write(buf, 0, nRead);
                    }
                    zos.closeEntry();
                }catch(IOException e) {
                    Log.e("TEST", Log.getStackTraceString(e));
                }
            }
        }

    }

    private void updateLog(String str) {
        mHandler.obtainMessage(MSG_APPEND_TEXT, str).sendToTarget();
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if(msg.what == MSG_APPEND_TEXT) {
                mInfoText2.setText((String) msg.obj);
            }else if(msg.what == MSG_SHOW_IMAGE) {
                mPreview.setImageURI(Uri.fromFile(new File((String) msg.obj)));
            }
        }
    };
}
