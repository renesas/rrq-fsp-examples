/*
 * Copyright 2015-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.renesas.wifi.awsiot.log;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Insets;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.widget.Button;
import android.widget.SimpleAdapter;
import android.widget.SimpleAdapter.ViewBinder;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListVersionsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.S3VersionSummary;
import com.amazonaws.services.s3.model.VersionListing;
import com.renesas.wifi.R;
import com.renesas.wifi.util.CustomToast;
import com.renesas.wifi.util.MyLog;
import com.renesas.wifi.util.StaticDataSave;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;


/**
 * DownloadSelectionActivity displays a list of files in the bucket. Users can
 * select a file to download.
 */
public class LogActivity extends ListActivity {

    static final int READ_LOG_COUNT = 30;  //

    static final String DOOR_OPEN_APP = "Door is opened by App.";
    static final String DOOR_OPEN_MCU = "Door is opened by MCU";
    static final String DOOR_CLOSE_APP = "Door is closed by App";
    static final String DOOR_CLOSE_TIMER = "Door is closed by MCU";

    TextView tv_noLog;
    TextView tv_noThing;
    Button btn_refresh;
    Button btn_clearLog;
    public static Context logContext;
    // The S3 client used for getting the list of objects in the bucket
    private AmazonS3Client s3Client = null;

    // An adapter to show the objects
    private SimpleAdapter simpleAdapter;
    private ArrayList<HashMap<String, Object>> transferRecordMaps;

    S3Util S3util;

    private ProgressDialog getFileDialog;

    CustomToast customToast = null;
    Handler mHandler_toast;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.awsiot_activity_log);
        logContext = this;
        customToast = new CustomToast(getApplicationContext());
        mHandler_toast = new Handler();
        S3util = new S3Util();
        initData();
        initUI();
    }

    @Override
    protected void onResume() {
        super.onResume();

        StaticDataSave.saveData = getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
        StaticDataSave.thingName = StaticDataSave.saveData.getString(StaticDataSave.thingNameKey, null);

        if (StaticDataSave.thingName != null) {
            tv_noThing.setVisibility(View.INVISIBLE);
            new GetFileListTask().execute();
        } else {
            tv_noThing.setVisibility(View.VISIBLE);
            tv_noLog.setVisibility(View.INVISIBLE);
        }
    }

    private void initData() {
        // Gets the default S3 client.
        s3Client = S3util.getS3Client(LogActivity.this);

        transferRecordMaps = new ArrayList<HashMap<String, Object>>();
    }

    private void initUI() {

        tv_noThing = findViewById(R.id.tv_noThing);
        tv_noLog = findViewById(R.id.tv_noLog);

        btn_refresh = findViewById(R.id.btn_refresh);
        btn_refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (StaticDataSave.thingName != null) {
                    tv_noThing.setVisibility(View.INVISIBLE);
                    new GetFileListTask().execute();
                } else {
                    tv_noThing.setVisibility(View.VISIBLE);
                    tv_noLog.setVisibility(View.INVISIBLE);
                }
            }
        });

        btn_clearLog = findViewById(R.id.btn_clearLog);
        btn_clearLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (StaticDataSave.thingName != null) {
                    tv_noThing.setVisibility(View.INVISIBLE);
                    new DeleteObjectTask().execute();
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    tv_noLog.setVisibility(View.VISIBLE);
                } else {
                    tv_noThing.setVisibility(View.VISIBLE);
                    tv_noLog.setVisibility(View.INVISIBLE);
                }
            }
        });

        simpleAdapter = new SimpleAdapter(getApplicationContext(), transferRecordMaps,
                R.layout.awsiot_bucket_item, new String[]{"key"}, new int[]{R.id.key}) {

        };

        simpleAdapter.setViewBinder(new ViewBinder() {
            @Override
            public boolean setViewValue(View view, Object data, String textRepresentation) {
                if (view.getId() == R.id.key) {
                    TextView fileName = (TextView) view.findViewById(R.id.key);

                    String keyName;
                    String centerLog;
                    String fieldLog;

                    String data_split[] = ((String) data).split("/");

                    if (data_split[0] != null) {
                        tv_noLog.setVisibility(View.INVISIBLE);
                        MyLog.i("data_split[0] = " + data_split[0]);
                        long batch_date = Long.parseLong(data_split[0]);
                        Date dt = new Date(batch_date);
                        SimpleDateFormat sfd = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss aaa");
                        sfd.setTimeZone(TimeZone.getDefault());
                        keyName = data_split[0];
                        centerLog = data_split[1];
                        MyLog.i("key_name = " + keyName + ", centerLog = " + centerLog);

                        switch (ReadJson(centerLog)) {
                            case 1: {
                                fieldLog = DOOR_OPEN_APP;
                            }
                            break;
                            case 2: {
                                fieldLog = DOOR_OPEN_MCU;
                            }
                            break;
                            case 3: {
                                fieldLog = DOOR_CLOSE_APP;
                            }
                            break;
                            case 4: {
                                fieldLog = DOOR_CLOSE_TIMER;
                            }
                            break;
                            case 0:
                            default: {
                                fieldLog = "ERROR ";
                            }
                            break;
                        }

                        MyLog.i("Content = " + fieldLog + ", Time = " + sfd.format(dt));
                        fileName.setText(sfd.format(dt) + "\n" + fieldLog);
                    } else {
                        tv_noLog.setVisibility(View.VISIBLE);
                    }

                    return true;
                }
                return false;
            }
        });

        setListAdapter(simpleAdapter);

    }

    /**
     * This async task queries S3 for all files in the given bucket so that they
     * can be displayed on the screen
     */
    private class GetFileListTask extends AsyncTask<Void, Void, Void> {
        // The list of objects we find in the S3 bucket
        private List<S3ObjectSummary> s3ObjList;
        // A dialog to let the user know we are retrieving the files

        @Override
        protected void onPreExecute() {
            MyLog.i("=== GetFileListTask : onPreExecute ===");
            getFileDialog = new ProgressDialog(LogActivity.this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
            getFileDialog.setTitle(getString(R.string.loading));
            getFileDialog.setMessage(getString(R.string.please_wait));
            getFileDialog.show();

            int displayWidth = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowMetrics windowMetrics = getWindowManager().getCurrentWindowMetrics();
                Insets insets = windowMetrics.getWindowInsets()
                        .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars());
                displayWidth = windowMetrics.getBounds().width() - insets.left - insets.right;
            } else {
                DisplayMetrics displayMetrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                displayWidth = displayMetrics.widthPixels;
            }

            WindowManager.LayoutParams params = getFileDialog.getWindow().getAttributes();
            int dialogWindowWidth = (int) (displayWidth * 0.8f);
            params.width = dialogWindowWidth;
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            getFileDialog.getWindow().setAttributes((WindowManager.LayoutParams) params);

        }


        @Override
        protected Void doInBackground(Void... inputs) {
            // Queries files in the bucket from S3.
            MyLog.i("=== GetFileListTask : doInBackground ===");
            try {

                int i = 0;
                int keySize = 0;
                List<S3ObjectSummary> reqS3ObjList = null;
                StaticDataSave.saveData = getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
                StaticDataSave.bucketName = StaticDataSave.saveData.getString(StaticDataSave.bucketNameKey, null);
                MyLog.i("StaticDataSave.bucketName = " + StaticDataSave.bucketName);
                s3ObjList = s3Client.listObjects(StaticDataSave.bucketName).getObjectSummaries();

                keySize = s3ObjList.size();

                if (READ_LOG_COUNT < keySize) {
                    reqS3ObjList = s3ObjList.subList(keySize - READ_LOG_COUNT, keySize);
                } else {
                    reqS3ObjList = s3ObjList.subList(0, keySize);
                }
                MyLog.i("KEY total size = " + keySize);
                s3ObjList.size();
                transferRecordMaps.clear();

                for (S3ObjectSummary summary : reqS3ObjList) {
                    S3Object tempObject = null;
                    String log = "";
                    HashMap<String, Object> map = new HashMap<String, Object>();

                    tempObject = s3Client.getObject(new GetObjectRequest(StaticDataSave.bucketName, summary.getKey()));
                    log = textInputStream(tempObject.getObjectContent());

                    map.put("header", summary.getKey());
                    map.put("key", summary.getKey() + "/" + log);

                    MyLog.i("GetFileListTask count = " + i + ", key = " + summary.getKey() + ", log = " + log);
                    transferRecordMaps.add(map);
                    i++;
                }

                Collections.sort(transferRecordMaps, new Comparator<Map<String, Object>>() {
                    @Override
                    public int compare(Map<String, Object> o1, Map<String, Object> o2) {
                        if (o1.get("key") != null && o2.get("key") != null) {
                            return o2.get("key").toString().compareTo(o1.get("key").toString());
                        } else if (o1.get("key") != null) {
                            return 1;
                        } else {
                            return -1;
                        }
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            MyLog.i("=== GetFileListTask : onPostExecute ===");
            getFileDialog.dismiss();
            simpleAdapter.notifyDataSetChanged();
        }
    }

    public int ReadJson(String jsonStr) {
        MyLog.i("=== ReadJson() ===" + jsonStr);
        String doorState = "";
        String openMethod = "";

        try {
            JSONObject jsonObj = new JSONObject(jsonStr);
            JSONObject state_obj = jsonObj.getJSONObject("state");
            JSONObject reported_obj = state_obj.getJSONObject("reported");
            doorState = reported_obj.getString("doorState");
            openMethod = reported_obj.getString("openMethod");
            if (doorState.equals("true")) {
                if (openMethod.equals("app")) {
                    return 1;
                } else if (openMethod.equals("mcu")) {
                    return 2;
                } else {
                    MyLog.e("=== error ReadJson() ===" + jsonStr);
                }
            } else if (doorState.equals("false")) {
                if (openMethod.equals("app")) {
                    return 3;
                } else if (openMethod.equals("mcu")) {
                    return 4;
                } else {
                    MyLog.e("=== error ReadJson() ===" + jsonStr);
                }
            } else {
                return 0;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }


    private static String textInputStream(InputStream input) throws IOException {
        int i;
        StringBuffer buffer = new StringBuffer();
        byte[] b = new byte[4096];
        while ((i = input.read(b)) != -1) {
            buffer.append(new String(b, 0, i));
        }
        String str = buffer.toString();
        return str;
    }

    private class ToastRunnable implements Runnable {
        String mText;

        public ToastRunnable(String text) {
            mText = text;
        }

        @Override
        public void run() {
            if (customToast != null) {
                customToast.showToast(getApplicationContext(), mText, Toast.LENGTH_SHORT);
            }
        }
    }

    private void clearBucket(final String bucketName) {
        MyLog.i("clearBucket : " + bucketName);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (s3Client == null) {
                        s3Client = S3util.getS3Client(LogActivity.this);
                    }
                    ObjectListing objectListing = s3Client.listObjects(bucketName);
                    while (true) {
                        Iterator<S3ObjectSummary> objIter = objectListing.getObjectSummaries().iterator();
                        while (objIter.hasNext()) {
                            s3Client.deleteObject(bucketName, objIter.next().getKey());
                        }

                        // If the bucket contains many objects, the listObjects() call
                        // might not return all of the objects in the first listing. Check to
                        // see whether the listing was truncated. If so, retrieve the next page of objects
                        // and delete them.
                        if (objectListing.isTruncated()) {
                            objectListing = s3Client.listNextBatchOfObjects(objectListing);
                        } else {
                            MyLog.i("objectListing.isTruncated() = false");
                            break;
                        }
                    }

                    // Delete all object versions (required for versioned buckets).
                    VersionListing versionList = s3Client.listVersions(new ListVersionsRequest().withBucketName(bucketName));
                    while (true) {
                        Iterator<S3VersionSummary> versionIter = versionList.getVersionSummaries().iterator();
                        while (versionIter.hasNext()) {
                            S3VersionSummary vs = versionIter.next();
                            s3Client.deleteVersion(bucketName, vs.getKey(), vs.getVersionId());
                        }

                        if (versionList.isTruncated()) {
                            versionList = s3Client.listNextBatchOfVersions(versionList);
                        } else {
                            MyLog.i("versionList.isTruncated() = false");
                            break;
                        }
                    }

                    // After all objects and object versions are deleted, delete the bucket.
                    s3Client.deleteBucket(bucketName);
                } catch (AmazonServiceException e) {
                    // The call was transmitted successfully, but Amazon S3 couldn't process
                    // it, so it returned an error response.
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void deleteObject(final String bucketName) {
        MyLog.i("deleteObject in " + bucketName);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int i = 0;
                    int keySize = 0;
                    List<S3ObjectSummary> s3ObjList;
                    List<S3ObjectSummary> reqS3ObjList = null;

                    if (s3Client == null) {
                        s3Client = S3util.getS3Client(LogActivity.this);
                    }

                    s3ObjList = s3Client.listObjects(StaticDataSave.bucketName).getObjectSummaries();

                    keySize = s3ObjList.size();

                    if (READ_LOG_COUNT < keySize) {
                        reqS3ObjList = s3ObjList.subList(keySize - READ_LOG_COUNT, keySize);
                    } else {
                        reqS3ObjList = s3ObjList.subList(0, keySize);
                    }
                    MyLog.i("KEY total size = " + keySize);
                    s3ObjList.size();
                    transferRecordMaps.clear();

                    for (S3ObjectSummary summary : reqS3ObjList) {
                        s3Client.deleteObject(new DeleteObjectRequest(bucketName, summary.getKey()));
                        i++;
                    }

                } catch (AmazonServiceException e) {
                    // The call was transmitted successfully, but Amazon S3 couldn't process
                    // it, so it returned an error response.
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private class DeleteObjectTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            MyLog.i("=== DeleteObjectTask : onPreExecute ===");
            getFileDialog = new ProgressDialog(LogActivity.this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
            getFileDialog.setTitle(getString(R.string.loading));
            getFileDialog.setMessage(getString(R.string.please_wait));
            getFileDialog.show();

            int displayWidth = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowMetrics windowMetrics = getWindowManager().getCurrentWindowMetrics();
                Insets insets = windowMetrics.getWindowInsets()
                        .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars());
                displayWidth = windowMetrics.getBounds().width() - insets.left - insets.right;
            } else {
                DisplayMetrics displayMetrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                displayWidth = displayMetrics.widthPixels;
            }

            WindowManager.LayoutParams params = getFileDialog.getWindow().getAttributes();
            int dialogWindowWidth = (int) (displayWidth * 0.8f);
            params.width = dialogWindowWidth;
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            getFileDialog.getWindow().setAttributes((WindowManager.LayoutParams) params);

        }

        @Override
        protected Void doInBackground(Void... inputs) {
            // Queries files in the bucket from S3.
            MyLog.i("=== DeleteObjectTask : doInBackground ===");
            try {
                int i = 0;
                int keySize = 0;
                List<S3ObjectSummary> s3ObjList;
                List<S3ObjectSummary> reqS3ObjList = null;

                if (s3Client == null) {
                    s3Client = S3util.getS3Client(LogActivity.this);
                }

                s3ObjList = s3Client.listObjects(StaticDataSave.bucketName).getObjectSummaries();

                keySize = s3ObjList.size();

                if (READ_LOG_COUNT < keySize) {
                    reqS3ObjList = s3ObjList.subList(keySize - READ_LOG_COUNT, keySize);
                } else {
                    reqS3ObjList = s3ObjList.subList(0, keySize);
                }
                MyLog.i("KEY total size = " + keySize);
                s3ObjList.size();
                transferRecordMaps.clear();

                for (S3ObjectSummary summary : reqS3ObjList) {
                    s3Client.deleteObject(new DeleteObjectRequest(StaticDataSave.bucketName, summary.getKey()));
                    i++;
                }
            } catch (AmazonServiceException e) {
                // The call was transmitted successfully, but Amazon S3 couldn't process
                // it, so it returned an error response.
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            MyLog.i("=== DeleteObjectTask : onPostExecute ===");
            getFileDialog.dismiss();
            simpleAdapter.notifyDataSetChanged();
        }
    }

}
