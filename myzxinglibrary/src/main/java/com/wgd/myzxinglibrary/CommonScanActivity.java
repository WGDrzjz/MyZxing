/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wgd.myzxinglibrary;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.Result;
import com.wgd.myzxinglibrary.zxing.ScanListener;
import com.wgd.myzxinglibrary.zxing.ScanManager;
import com.wgd.myzxinglibrary.zxing.decode.DecodeThread;
import com.wgd.myzxinglibrary.zxing.decode.Utils;

import java.util.ArrayList;
import java.util.List;


/**
 * 二维码扫描使用
 *
 */
public final class CommonScanActivity extends Activity implements ScanListener, View.OnClickListener {
    static final String TAG = CommonScanActivity.class.getSimpleName();
    SurfaceView scanPreview = null;
    View scanContainer;
    View scanCropView;
    ImageView scanLine;
    ScanManager scanManager;
    TextView iv_light;
    TextView qrcode_g_gallery;
    TextView qrcode_ic_back;
    final int PHOTOREQUESTCODE = 1111;

    Button rescan;
    ImageView scan_image;
    ImageView authorize_return;
    private int scanMode;//扫描模型（条形，二维码，全部）

    TextView title;
    TextView scan_hint;
    TextView tv_scan_result;

    PermissionUtils mPermissionUtils ;

    public static void start(Activity activity){
        Intent intent = new Intent(activity, CommonScanActivity.class);
        activity.startActivityForResult(intent, Constant.REQUEST_SCAN_START);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_scan_code);
        bindView();
        scanMode = getIntent().getIntExtra(Constant.REQUEST_SCAN_MODE, Constant.REQUEST_SCAN_MODE_ALL_MODE);
        initView();
    }

    private void initPermission(){
        mPermissionUtils = new PermissionUtils();
        mPermissionUtils.setPermissionUtils(CommonScanActivity.this, new PermissionUtils.OnResultPermissions() {
            @Override
            public void OnAllAgree() {
//                scanPreview = (SurfaceView) findViewById(R.id.capture_preview);
                //构造出扫描管理器
                onCreate(null);
            }

            @Override
            public void OnAllAgree2() {
                OnAllAgree();
            }

            @Override
            public void OnUnAllAgree2() {
                Toast.makeText(CommonScanActivity.this, "权限被禁止，功能不可正常使用！", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void OnUnAgree(List<String> permissionListUn) {
                Toast.makeText(CommonScanActivity.this, "权限被禁止，功能不可正常使用！", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (null!=mPermissionUtils)mPermissionUtils.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void bindView(){
        rescan = findViewById(R.id.service_register_rescan);
        scan_image = findViewById(R.id.scan_image);
        authorize_return = findViewById(R.id.authorize_return);
        title = findViewById(R.id.common_title_TV_center);
        scan_hint = findViewById(R.id.scan_hint);
        tv_scan_result = findViewById(R.id.tv_scan_result);
    }

    void initView() {
        switch (scanMode) {
            case DecodeThread.BARCODE_MODE:
                title.setText(R.string.scan_barcode_title);
                scan_hint.setText(R.string.scan_barcode_hint);
                break;
            case DecodeThread.QRCODE_MODE:
                title.setText(R.string.scan_qrcode_title);
                scan_hint.setText(R.string.scan_qrcode_hint);
                break;
            case DecodeThread.ALL_MODE:
                title.setText(R.string.scan_allcode_title);
                scan_hint.setText(R.string.scan_allcode_hint);
                break;
        }
        scanPreview = (SurfaceView) findViewById(R.id.capture_preview);
        scanContainer = findViewById(R.id.capture_container);
        scanCropView = findViewById(R.id.capture_crop_view);
        scanLine = (ImageView) findViewById(R.id.capture_scan_line);
        qrcode_g_gallery = (TextView) findViewById(R.id.qrcode_g_gallery);
        qrcode_g_gallery.setOnClickListener(this);
        qrcode_ic_back = (TextView) findViewById(R.id.qrcode_ic_back);
        qrcode_ic_back.setOnClickListener(this);
        iv_light = (TextView) findViewById(R.id.iv_light);
        iv_light.setOnClickListener(this);
        rescan.setOnClickListener(this);
        authorize_return.setOnClickListener(this);
        initPermission();
        scanManager = new ScanManager(CommonScanActivity.this, scanPreview, scanContainer, scanCropView, scanLine, scanMode, CommonScanActivity.this);

    }

    @Override
    public void onResume() {
        super.onResume();
        if (null!=scanManager)scanManager.onResume();
        rescan.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (null!=scanManager)scanManager.onPause();
    }

    /**
     *
     */
    public void scanResult(Result rawResult, Bundle bundle) {
        //扫描成功后，扫描器不会再连续扫描，如需连续扫描，调用reScan()方法。
        if (!scanManager.isScanning()) { //如果当前不是在扫描状态
            //设置再次扫描按钮出现
            rescan.setVisibility(View.VISIBLE);
            Bitmap barcode = null;
            byte[] compressedBitmap = bundle.getByteArray(DecodeThread.BARCODE_BITMAP);
            if (compressedBitmap != null) {
                barcode = BitmapFactory.decodeByteArray(compressedBitmap, 0, compressedBitmap.length, null);
                barcode = barcode.copy(Bitmap.Config.ARGB_8888, true);
            }
            scan_image.setImageBitmap(barcode);
        }
        rescan.setVisibility(View.VISIBLE);
        scan_image.setVisibility(View.VISIBLE);
        if (true) {
            Intent intent = new Intent();
            intent.putExtra("result",rawResult.getText());
            setResult(RESULT_OK, intent);
            finish();
        } else {
            tv_scan_result.setText(getResources().getString(R.string.result) + rawResult.getText() + getResources().getString(R.string.inconformity_demand));
        }

    }

    void startScan() {
        if (rescan.getVisibility() == View.VISIBLE) {
            rescan.setVisibility(View.INVISIBLE);
            scan_image.setVisibility(View.GONE);
            scanManager.reScan();
        }
    }

    @Override
    public void scanError(Exception e) {
//        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        //相机扫描出错时
//        if (e.getMessage() != null && e.getMessage().startsWith(getString(R.string.camera))) {
//            scanPreview.setVisibility(View.INVISIBLE);
//        }

        List<String> permissionList = new ArrayList<>();
        permissionList.add(Manifest.permission.CAMERA);
        permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        mPermissionUtils.getPermissions(permissionList);

    }

    public void showPictures(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, requestCode);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        String photo_path;
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case PHOTOREQUESTCODE:
                    String[] proj = {MediaStore.Images.Media.DATA};
                    Cursor cursor = this.getContentResolver().query(data.getData(), proj, null, null, null);
                    if (cursor.moveToFirst()) {
                        int colum_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                        photo_path = cursor.getString(colum_index);
                        if (photo_path == null) {
                            photo_path = Utils.getPath(getApplicationContext(), data.getData());
                        }
                        scanManager.scanningImage(photo_path);
                    }
            }
        }
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.qrcode_g_gallery) {
            showPictures(PHOTOREQUESTCODE);

        } else if (i == R.id.iv_light) {
            scanManager.switchLight();

        } else if (i == R.id.qrcode_ic_back) {
            finish();

        } else if (i == R.id.service_register_rescan) {
            startScan();

        } else if (i == R.id.authorize_return) {
            finish();

        } else {
        }
    }

}