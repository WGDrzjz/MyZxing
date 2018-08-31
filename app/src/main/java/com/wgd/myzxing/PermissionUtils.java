package com.wgd.myzxing;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wangguodong on 2017/10/10.
 */

public class PermissionUtils {

    public static final int SDK_PERMISSION_REQUEST = 9901 ;
    private Activity mActivity ;
    private List<String> mList ;
    private Fragment fragment ;

    private OnResultPermissions mOnResultPermissions ;

    public static interface OnResultPermissions{
        abstract void OnAllAgree();
        abstract void OnAllAgree2();
        abstract void OnUnAllAgree2();
        abstract void OnUnAgree(List<String> permissionListUn);
    }

    public PermissionUtils(){

    }
    public PermissionUtils(Fragment fragment, OnResultPermissions onResultPermissions){
        this.fragment = fragment ;
        mActivity = fragment.getActivity() ;
        mOnResultPermissions = onResultPermissions ;
    }
    public PermissionUtils(Activity activity, OnResultPermissions onResultPermissions){
        mActivity = activity ;
        mOnResultPermissions = onResultPermissions ;
    }
    public void setPermissionUtils(Fragment fragment, OnResultPermissions onResultPermissions){
        this.fragment = fragment ;
        mActivity = fragment.getActivity() ;
        mOnResultPermissions = onResultPermissions ;
    }
    public void setPermissionUtils(Activity activity, OnResultPermissions onResultPermissions){
        mActivity = activity ;
        mOnResultPermissions = onResultPermissions ;
    }

    /**
     * 使用这个必须在Activity中手动调用onRequestPermissionsResult方法
     * @param list
     */
    @TargetApi(23)
    public void getPermissions( List<String> list ) {
        mList = list ;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ArrayList<String> permissions = new ArrayList<String>();
            if (null!=mList && mList.size() > 0){
                int pos = 0 ;
                for (int i = 0; i < mList.size(); i++) {
                    if (addPermission(permissions, mList.get(i))) {
                        pos ++ ;
                    }
                }

                if (pos >= mList.size()){
                    //全部同意了，
                    if (null!=mOnResultPermissions)mOnResultPermissions.OnAllAgree();
                }else {
                    if (permissions.size() > 0) {
                        try {
//                                                            if (null!=mActivity)mActivity.requestPermissions(permissions.toArray(new String[permissions.size()]), SDK_PERMISSION_REQUEST);
                            if (null!=fragment){
                                fragment.requestPermissions(permissions.toArray(new String[permissions.size()]), SDK_PERMISSION_REQUEST);
                            }else
                            if (null!=mActivity) ActivityCompat.requestPermissions(mActivity, permissions.toArray(new String[permissions.size()]), SDK_PERMISSION_REQUEST);
                        }catch (Exception e){
                            e.printStackTrace();
                            Toast.makeText(mActivity, "申请失败，可能程序在Manifest中没有申请此权限！", Toast.LENGTH_SHORT).show();
                        }
                    }
                }

            }else if (null!=mOnResultPermissions)mOnResultPermissions.OnAllAgree();
        }else {//这里6.0以下直接不用代码申请权限
            if (null!=mOnResultPermissions)mOnResultPermissions.OnAllAgree();
        }
    }

    @TargetApi(23)
    private boolean addPermission(ArrayList<String> permissionsList, String permission) {
        if (null!=mActivity && mActivity.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) { // 如果应用没有获得对应权限,则添加到列表中,准备批量申请
//            if (null!=mActivity &&mActivity.shouldShowRequestPermissionRationale(permission)) {
            /**
             * shouldShowRequestPermissionRationale方法解析
             * 如果应用之前请求过此权限但用户拒绝了请求，此方法将返回 true
             * 如果用户在过去拒绝了权限请求，并在权限请求系统对话框中选择了 Don’t ask again 选项，此方法将返回 false；如果设备规范禁止应用具有该权限，此方法也会返回 false；
             * 注意：：：：当应用没有请求过权限时，即第一次请求时，此方法返回false！！！！！
             */
            if (null!=mActivity && ActivityCompat.shouldShowRequestPermissionRationale(mActivity, permission)) {
                //拒绝了也是可以强行提示的，不过如果再也不提示的情况还没处理
                permissionsList.add(permission);
                return false;
            } else {
                permissionsList.add(permission);
                return false;
            }
        } else {
//            PermissionPreferenceSaves.saveString(permission, 0);
            return true;
        }
    }

    private void showDialog(){
        try {
            AlertDialog dialog = new AlertDialog.Builder(mActivity).setTitle("权限被禁止!")
                    .setMessage("权限被禁止，功能将不能正常使用，是否前去开启？")
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            getAppDetailSettingIntent();
                        }
                    }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            //这里点击取消时，给一个回调，测试用
                            if (null!=mOnResultPermissions)mOnResultPermissions.OnUnAllAgree2();
                        }
                    }).show();
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    @TargetApi(23)
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case SDK_PERMISSION_REQUEST:
                if (null!=permissions && null!=grantResults && permissions.length == grantResults.length && grantResults.length>0){
                    List<String> unAgreePerm = new ArrayList<>();
                    for (int i = 0; i < permissions.length; i++){
                        if (-1 == grantResults[i]){
                            unAgreePerm.add(permissions[i]);
                            if (null!=mOnResultPermissions)mOnResultPermissions.OnUnAgree(unAgreePerm);
                            return;
                        }
                    }
                    if (null!=mOnResultPermissions)
                        mOnResultPermissions.OnAllAgree2();
                }
                break;
            default:
        }
    }

    /**
     * 跳转到权限设置界面
     */
    private void getAppDetailSettingIntent(){

        // vivo 点击设置图标>加速白名单>我的app
        //      点击软件管理>软件管理权限>软件>我的app>信任该软件
        Intent appIntent = mActivity.getPackageManager().getLaunchIntentForPackage("com.iqoo.secure");
        if(appIntent != null){
            mActivity.startActivity(appIntent);
            return;
        }

        // oppo 点击设置图标>应用权限管理>按应用程序管理>我的app>我信任该应用
        //      点击权限隐私>自启动管理>我的app
        appIntent = mActivity.getPackageManager().getLaunchIntentForPackage("com.oppo.safe");
        if(appIntent != null){
            mActivity.startActivity(appIntent);
            return;
        }

        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if(Build.VERSION.SDK_INT >= 9){
            intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
            intent.setData(Uri.fromParts("package", mActivity.getPackageName(), null));
        } else if(Build.VERSION.SDK_INT <= 8){
            intent.setAction(Intent.ACTION_VIEW);
            intent.setClassName("com.android.settings","com.android.settings.InstalledAppDetails");
            intent.putExtra("com.android.settings.ApplicationPkgName", mActivity.getPackageName());
        }
        mActivity.startActivity(intent);
    }

    /**
     *  这里为了解决shouldShowRequestPermissionRationale方法中，在第一次请求时返回false造成的问题
     */
/*    static class PermissionPreferenceSaves{

        public static void saveString(String key, int value) {
            if (null== AppUtils.getAppContext())return;
            SharedPreferences.Editor editor = getSharedPreferences().edit();
//            editor.putString(key, value);
            editor.putInt(key, value);
            editor.apply();
        }

        public static int getString(String key) {
            if (null==AppUtils.getAppContext())return 0;
            return getSharedPreferences().getInt(key, 0);
        }

        public static void cleanString(String key) {
            if (null==AppUtils.getAppContext())return;
            SharedPreferences.Editor edit = getSharedPreferences().edit();
            edit.remove(key);
            edit.apply();
        }

        private static SharedPreferences getSharedPreferences() {
            return AppUtils.getAppContext().getSharedPreferences("RPPermissionSave", Context.MODE_PRIVATE);
        }

    }*/




}
