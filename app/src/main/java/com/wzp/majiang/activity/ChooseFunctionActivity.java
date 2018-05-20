package com.wzp.majiang.activity;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.wzp.majiang.R;
import com.wzp.majiang.activity.base.BluetoothBaseActivity;
import com.wzp.majiang.constant.BluetoothState;
import com.wzp.majiang.constant.ProjectConstants;
import com.wzp.majiang.util.BluetoothClientHelper;
import com.wzp.majiang.widget.MyApplication;

import java.util.List;

public class ChooseFunctionActivity extends BluetoothBaseActivity {

	private TextView tvBtState;
	private ImageButton ibtnBack;
	private ImageButton ibtnSearch;

	private TextView tvLocation;
	private Button btnDesignPlayMethod;
	private Button btnShowCard;
	private Button btnStudyTest;

	private static final int REQUEST_ENABLE_BT = 0x00; // 请求打开蓝牙
	private static final int REQUEST_CONNECT_DEVICE_SECURE = 0x01; // 请求安全连接蓝牙设备

	private BluetoothAdapter bluetoothAdapter;

	private String macAddr; // 蓝牙设备的mac地址

	private String[] needPermissions = {
			Manifest.permission.ACCESS_COARSE_LOCATION,
			Manifest.permission.ACCESS_FINE_LOCATION,
			Manifest.permission.WRITE_EXTERNAL_STORAGE,
			Manifest.permission.READ_EXTERNAL_STORAGE
	};

	private AMapLocationClient locationClient;
	private AMapLocationClientOption locationOption;

	private String districtCode; // 区域码

	private View.OnClickListener listener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
				case R.id.ibtn_back:
					onBackPressed();
					break;

				case R.id.ibtn_search:
					Intent searchIntent = new Intent(ChooseFunctionActivity.this, DeviceListActivity.class);
					startActivityForResult(searchIntent, REQUEST_CONNECT_DEVICE_SECURE);
					break;

				case R.id.btn_designPlayMethod:
					ShowPlayMethodActivity.myStartActivity(ChooseFunctionActivity.this, districtCode);
					break;

				case R.id.btn_showCard:
					MainActivity.myStartActivity(ChooseFunctionActivity.this);
					break;

				case R.id.btn_studyTest:
					StudyTestActivity.myStartActivity(ChooseFunctionActivity.this);
					break;

				default:
					break;
			}
		}
	};

	/**
	 * 定位监听
	 */
	private AMapLocationListener locationListener = new AMapLocationListener() {
		@Override
		public void onLocationChanged(AMapLocation location) {
			if (null != location) {
				//errCode等于0代表定位成功，其他的为定位失败，具体的可以参照官网定位错误码说明
				if(location.getErrorCode() == 0){
					districtCode = location.getAdCode();
					tvLocation.setText(location.getAddress() + "---" + districtCode);
					tvLocation.setVisibility(View.VISIBLE);
				} else {
					//定位失败
					StringBuffer sb = new StringBuffer();
					sb.append("定位失败" + "\n");
					sb.append("错误码:" + location.getErrorCode() + "\n");
					sb.append("错误信息:" + location.getErrorInfo() + "\n");
					sb.append("错误描述:" + location.getLocationDetail() + "\n");
					Log.e(LOG_TAG, sb.toString());

					tvLocation.setText("定位失败！错误描述：" + location.getLocationDetail()
							+ "。错误码：" + location.getErrorCode());
					tvLocation.setVisibility(View.VISIBLE);
					districtCode = null;
				}
			} else {
				Log.e(LOG_TAG, "定位失败，location is null");
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_choose_function);

		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (bluetoothAdapter == null) {
			Toast.makeText(this, "当前设备不具备蓝牙功能！",
					Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		if (!bluetoothAdapter.isEnabled()) {
			// 蓝牙尚未打开
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		} else {
			// 蓝牙已经打开
			initParam();
			initWidget();
		}
	}

	@Override
	protected void onStart() {
		super.onStart();

		List<String> deniedPermissionList = checkPermissions(needPermissions);
		if (deniedPermissionList == null || deniedPermissionList.size() == 0) {
			// 启动定位
			locationClient.startLocation();
			Log.d(LOG_TAG, "startLocation");
		}
	}

	@Override
	protected void onStop() {
		// 停止定位
		locationClient.stopLocation();
		super.onStop();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case REQUEST_ENABLE_BT:
				if (resultCode == Activity.RESULT_OK) {
					initParam();
					initWidget();
				} else {
					Log.d(LOG_TAG, "蓝牙尚未开启");
					Toast.makeText(this, "蓝牙尚未开启，无法使用应用程序", Toast.LENGTH_SHORT).show();
					finish();
				}
				break;

			case REQUEST_CONNECT_DEVICE_SECURE:
				if (resultCode == Activity.RESULT_OK) {
					connectDevice(data);
				}
				break;

			default:
				break;
		}
	}

	private void initParam() {
        if (MyApplication.btClientHelper == null) {
            MyApplication.btClientHelper = new BluetoothClientHelper();
        }
        MyApplication.btClientHelper.setBluetoothConnect(new IBluetoothConnect() {
            @Override
            public void showToast(String info, int duration) {
                ChooseFunctionActivity.this.showToast(info, duration);
            }

            @Override
            public void showToast(String info) {
                ChooseFunctionActivity.this.showToast(info);
            }

            @Override
            public void showBtState(final int state) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        switch (state) {
                            case BluetoothState.STATE_NONE:
                                tvBtState.setText("未连接");
                                break;

                            case BluetoothState.STATE_CONNECTING:
                                tvBtState.setText("连接中...");
                                break;

                            case BluetoothState.STATE_CONNECTED:
                                tvBtState.setText("已连接：" + MyApplication
                                        .btClientHelper.getRemoteDevName());

                                if (readDataThread == null && isFront) {
                                    readDataThread = new ReadDataThread();
                                    readDataThread.start();
                                }

								// 保存mac地址至SharedPreferences中
								if (!TextUtils.isEmpty(macAddr)) {
									MyApplication.getSpSetting().commitString("macAddress", macAddr);
								}
                                break;

                            default:
                                break;
                        }
                    }
                });
            }
        });

        // 初始化定位功能
		initLocation();
	}

	private void initWidget() {
		tvBtState = (TextView) findViewById(R.id.tv_btState);
		ibtnBack = (ImageButton) findViewById(R.id.ibtn_back);
		ibtnSearch = (ImageButton) findViewById(R.id.ibtn_search);
		tvLocation = (TextView) findViewById(R.id.tv_location);
		btnDesignPlayMethod = (Button) findViewById(R.id.btn_designPlayMethod);
		btnShowCard = (Button) findViewById(R.id.btn_showCard);
		btnStudyTest = (Button) findViewById(R.id.btn_studyTest);

		ibtnBack.setOnClickListener(listener);
		ibtnSearch.setOnClickListener(listener);
		btnDesignPlayMethod.setOnClickListener(listener);
		btnShowCard.setOnClickListener(listener);
		btnStudyTest.setOnClickListener(listener);

		if (MyApplication.btClientHelper != null && MyApplication.btClientHelper.isBluetoothConnected()) {
			tvBtState.setText("已连接: " + MyApplication.btClientHelper.getRemoteDevName());
		} else {
			tvBtState.setText("未连接");
		}
	}

	/**
	 * 连接蓝牙设备
	 *
	 * @param data
	 */
	private void connectDevice(Intent data) {
		macAddr = data.getExtras().getString(ProjectConstants.EXTRA_DEVICE_ADDRESS);
		BluetoothDevice device = bluetoothAdapter.getRemoteDevice(macAddr);
		MyApplication.btClientHelper.connect(device);
	}

	public static void myStartActivity(Context context) {
		Intent intent = new Intent(context, ChooseFunctionActivity.class);
		context.startActivity(intent);
	}

	@Override
	protected void onBluetoothDataReceived(byte[] recvData) {}

	@TargetApi(23)
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] paramArrayOfInt) {
		super.onRequestPermissionsResult(requestCode, permissions, paramArrayOfInt);
		if (requestCode == PERMISSON_REQUEST_CODE) {
			if (verifyPermissions(paramArrayOfInt)) {
				// 启动定位
				locationClient.startLocation();
			}
		}
	}

	/**
	 * 初始化定位
	 *
	 * @since 2.8.0
	 * @author hongming.wang
	 *
	 */
	private void initLocation(){
		//初始化client
		locationClient = new AMapLocationClient(this.getApplicationContext());
		locationOption = getDefaultOption();
		//设置定位参数
		locationClient.setLocationOption(locationOption);
		// 设置定位监听
		locationClient.setLocationListener(locationListener);
	}
	/**
	 * 默认的定位参数
	 * @since 2.8.0
	 * @author hongming.wang
	 *
	 */
	private AMapLocationClientOption getDefaultOption(){
		AMapLocationClientOption mOption = new AMapLocationClientOption();
		mOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);//可选，设置定位模式，可选的模式有高精度、仅设备、仅网络。默认为高精度模式
		mOption.setGpsFirst(false);//可选，设置是否gps优先，只在高精度模式下有效。默认关闭
		mOption.setHttpTimeOut(30000);//可选，设置网络请求超时时间。默认为30秒。在仅设备模式下无效
		mOption.setInterval(5000);//可选，设置定位间隔。默认为2秒
		mOption.setNeedAddress(true);//可选，设置是否返回逆地理地址信息。默认是true
		mOption.setOnceLocation(false);//可选，设置是否单次定位。默认是false
		mOption.setOnceLocationLatest(false);//可选，设置是否等待wifi刷新，默认为false.如果设置为true,会自动变为单次定位，持续定位时不要使用
		AMapLocationClientOption.setLocationProtocol(AMapLocationClientOption.AMapLocationProtocol.HTTP);//可选， 设置网络请求的协议。可选HTTP或者HTTPS。默认为HTTP
		mOption.setSensorEnable(false);//可选，设置是否使用传感器。默认是false
		mOption.setWifiScan(true); //可选，设置是否开启wifi扫描。默认为true，如果设置为false会同时停止主动刷新，停止以后完全依赖于系统刷新，定位位置可能存在误差
		mOption.setLocationCacheEnable(false); //可选，设置是否使用缓存定位，默认为true
		mOption.setGeoLanguage(AMapLocationClientOption.GeoLanguage.DEFAULT);//可选，设置逆地理信息的语言，默认值为默认语言（根据所在地区选择语言）
		return mOption;
	}
}