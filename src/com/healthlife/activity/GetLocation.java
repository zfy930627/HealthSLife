package com.healthlife.activity;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.LocationClientOption.LocationMode;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.DistanceUtil;
import com.healthlife.R;
import com.healthlife.entity.Position;
import com.healthlife.util.MyOrientationListener;
import com.healthlife.util.MyOrientationListener.OnOrientationListener;
/**
 * Title: GetLocation.java
 * @author Jusitn Yin
 * 2015-5-7
 */
public class GetLocation extends Activity {
	
	MapView mMapView = null;
	BaiduMap mBaiduMap = null;
	private LocationClient mLocationClient;
	public MyLocationListener mMyLocationListener;
	private MyOrientationListener myOrientationListener;
	private MyLocationConfiguration.LocationMode mLocationConfiguration = 
			MyLocationConfiguration.LocationMode.NORMAL;
	
	private volatile boolean isFirstCompare = true;
	private boolean judge = true;
	private Chronometer chronometer;
	private Double maxLatitude;
	private Double minLatitude;
	private Double maxLongitude;
	private Double minLongitude;
	private Double centerLatitude;
	private Double centerLongitude;
	private Double mCurrentLatitude;  
    private Double mCurrentLongitude;
    private Button finishBtn;
    private TextView disText;
    private TextView paceText;
	private List<Position> points;
    private List<LatLng> pts;
	private double subLat;
	private double subLon;
    private float mCurrentX;
    private double distance;
    private long recordTime;
    private String date;
    private String duration;
    private static final String IOS = "Test";
    
    //��ʱ���ñ���
    private MotionReceiver motionReceiver;
    private IntentFilter intentFilterforStep;
    private ServiceConnection connection;
    private int steps;

    
    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.location_main);
		disText = (TextView)findViewById(R.id.location_main_distance);
		paceText = (TextView)findViewById(R.id.location_main_pace);
		finishBtn = (Button)findViewById(R.id.location_main_finishbtn);
		
		//mMapView.removeViewAt(1);
		
		maxLatitude = 0.0;
		minLatitude = 0.0;
		maxLongitude = 0.0;
		minLongitude = 0.0;
		centerLatitude =40.058359;
		centerLongitude = 116.307629;
		subLat = 0.0;
		subLon = 0.0;
		distance = 0.0;
		recordTime = 0;
		points = new ArrayList<Position>();
		
		SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.getDefault());       
		date = sDateFormat.format(new java.util.Date());
		
		initMap();
		initMyLocation();
		initTime();
		
		//��ʼ��ʼ���Ʋ���
		//********************************************************************************************************************
		intentFilterforStep = new IntentFilter();
		intentFilterforStep.addAction("com.healthlife.activity.WalkActivity.MotionAdd");
		
		motionReceiver = new MotionReceiver();
		
		registerReceiver(motionReceiver,intentFilterforStep);
		
		connection = new ServiceConnection(){

			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
			}

			@Override
			public void onServiceDisconnected(ComponentName arg0) {
				
				
			}
		};
		
		Intent intntBind = new Intent(this,com.healthlife.util.StepService.class);
		bindService(intntBind,connection,BIND_AUTO_CREATE);
		//******************************************************************************************************************************
		//�Ʋ�����ʼ������
		
		//�����˶���ť��Ӧ
		finishBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				//����󶨼Ʋ�������
				unbindService(connection);
				
				if(points.size() > 1){
					chronometer.stop();
					recordTime = (SystemClock.elapsedRealtime() - chronometer.getBase())/1000;
					
					String sh = "";
					String sm = "";
					String ss = "";
					long hour = recordTime / 3600;
					long minute = (recordTime - hour * 60 * 60) / 60;
					long second = recordTime - hour * 60 * 60 - minute * 60;
					if(hour < 10)
						sh = "0" + String.valueOf(hour);
					else
						sh = String.valueOf(hour);
					if(minute < 10)
						sm = "0" + String.valueOf(minute);
					else
						sm = String.valueOf(minute);
					if(second < 10)
						ss = "0" + String.valueOf(second);
					else
						ss = String.valueOf(second);
					duration = sh + ":" + sm + ":" + ss;
					Log.i("Test", "Intent֮ǰ��");
					Intent intent = new Intent();
					intent.setClass(GetLocation.this, LocationResult.class);
					intent.putExtra("locinfo", (Serializable) points);
					intent.putExtra("cenlat", centerLatitude);
					intent.putExtra("cenlon", centerLongitude);
					intent.putExtra("distance", distance);
					intent.putExtra("duration", duration);
					intent.putExtra("rectime", recordTime);
					intent.putExtra("date", date);
					startActivity(intent);
					finish();
					Log.i("Test", "Intent֮��");
				}else {
					Toast.makeText(GetLocation.this, "�˶�����Ϊ0��", Toast.LENGTH_SHORT).show();
				}
			}
		});
	}
	
	private void initMyLocation()
	{
		// ��λ��ʼ��
		mLocationClient = new LocationClient(this);
		mMyLocationListener = new MyLocationListener();
		mLocationClient.registerLocationListener(mMyLocationListener);
		
		LocationClientOption option = new LocationClientOption();
		option.setLocationMode(LocationMode.Hight_Accuracy);// ���ö�λģʽ
		option.setOpenGps(true);// ��gps
		option.setCoorType("bd09ll"); // ������������
		option.setScanSpan(6000);
		mLocationClient.setLocOption(option);
		
		//���򴫸��������¼�
		myOrientationListener = new MyOrientationListener(getApplicationContext());
		myOrientationListener.setOnOrientationListener(new OnOrientationListener() {
			
			@Override
			public void onOrientationChanged(float x) {
				mCurrentX = x;
			}
		});
	}
	
	//��ͼ��ʼ��
	private void initMap(){
		mMapView = (MapView)findViewById(R.id.location_main_bmapView);
		mBaiduMap = mMapView.getMap();
		LatLng cenpt = new LatLng(30.513236,114.419936);
		MapStatus mMapStatus = new MapStatus.Builder().target(cenpt).zoom(17).build();
		MapStatusUpdate mMapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mMapStatus);
		mBaiduMap.setMapStatus(mMapStatusUpdate);
	}
	
	//��ʱ����ʼ��
	private void initTime() {
		chronometer = (Chronometer)findViewById(R.id.location_main_chronotime);
		chronometer.setBase(SystemClock.elapsedRealtime());
		chronometer.start();//��ʱ��ʼ 
	}
	
	/**
	 * ʵ��ʵλ�ص�����
	 */
	public class MyLocationListener implements BDLocationListener
	{
		@Override
		public void onReceiveLocation(BDLocation location)
		{
			MyLocationData locData = new MyLocationData.Builder()  
			    .accuracy(location.getRadius())  
			    .direction(mCurrentX)
			    .latitude(location.getLatitude())  
			    .longitude(location.getLongitude()).build();  
			// ���ö�λ����  
			mBaiduMap.setMyLocationData(locData);
			mCurrentLatitude = location.getLatitude();  
            mCurrentLongitude = location.getLongitude();
            
//          SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.getDefault());       
//    		String date = sDateFormat.format(new java.util.Date());
            long potTime = SystemClock.elapsedRealtime();
            
    		if (isFirstCompare)
			{
            	isFirstCompare = false;
            	returnMyLoc();
            	maxLatitude = mCurrentLatitude;
            	minLatitude = maxLatitude;
            	maxLongitude = mCurrentLongitude;
            	minLongitude = maxLongitude;
            	centerLatitude = mCurrentLatitude;
            	centerLongitude = mCurrentLongitude;
        		addPoints(potTime, mCurrentLatitude, mCurrentLongitude);
			}else {
				subLat = Math.abs(points.get(points.size()-1).getLatitude() - mCurrentLatitude);
	    		subLon = Math.abs(points.get(points.size()-1).getLongitude() - mCurrentLongitude);
	    		
	    		LatLng pot = new LatLng(points.get(points.size()-1).getLatitude(),
	    				points.get(points.size()-1).getLongitude());
	    		LatLng pot1 = new LatLng(mCurrentLatitude, mCurrentLongitude);
	    		distance = distance + DistanceUtil.getDistance(pot, pot1);
				Log.i("Test", "����ȥ֮ǰ�ľ���Ϊ��" + String.format("%.2f", distance));
				
			}
    		disText.setText("·��Ϊ��\n" + String.format("%.1f", distance) + "��");
    		
    		//�ж϶�λ���Ƿ���Ч��ȡ0.0003Ϊ���
    		Log.i("Test", "γ�Ȳ" + String.valueOf(subLat));
    		Log.i("Test", "���Ȳ" + String.valueOf(subLon));
    		finishBtn.setText("γ�Ȳ" + String.valueOf(subLat) + "���Ȳ" + String.valueOf(subLon));
    		
    		if (subLat <0.0003 && subLon < 0.0003){
    			if(!(subLat == 0.0 && subLon == 0.0)){
    				maxLatitude = maxLatitude(maxLatitude,mCurrentLatitude);
		            minLatitude = minLatitude(minLatitude,mCurrentLatitude);
		            maxLongitude = maxLongitude(maxLongitude,mCurrentLongitude);
		            minLongitude = minLongitude(minLongitude,mCurrentLongitude);
		            
		            centerLatitude = (maxLatitude + minLatitude)/2.0;
		            centerLongitude = (maxLongitude + minLongitude)/2.0;
		            
		            addPoints(potTime, mCurrentLatitude, mCurrentLongitude);
		            Log.i("Test", "�з�ӳ��");
    			}
    		}
    		
    		Log.i("Test", "�з�ӳô��");
    		ActionBar actionBar = getActionBar();
    		actionBar.setTitle(String.valueOf(points.size()));
            
    		Log.i("CenLat", "����γ�ȣ�" + centerLatitude.toString());
    		Log.i("CenLon", "���ľ���" + centerLongitude.toString());
            Log.i("Lat", "γ�ȣ�" + mCurrentLatitude.toString());
            Log.i("Lon", "����" + mCurrentLongitude.toString());
            
            // ���ö�λͼ������ã���λģʽ���Ƿ���������Ϣ���û��Զ��嶨λͼ�꣩  
//			BitmapDescriptor mCurrentMarker = BitmapDescriptorFactory  
//			    .fromResource(R.drawable.locimg);
			MyLocationConfiguration config = new MyLocationConfiguration(mLocationConfiguration, true, null);
			mBaiduMap.setMyLocationConfigeration(config); 
			
		}
		
	}
	
	//�����С��γ�ȱȽ�
	private double maxLatitude(double lat1, double lat2) {
		if(lat1 >= lat2){
			return lat1;
		}else {
			return lat2;
		}
	}
	private double minLatitude(double lat3, double lat4) {
		if(lat3 <= lat4){
			return lat3;
		}else {
			return lat4;
		}
	}
	private double maxLongitude(double lon1, double lon2) {
		if(lon1 >= lon2){
			return lon1;
		}else {
			return lon2;
		}
	}
	private double minLongitude(double lon3, double lon4) {
		if(lon3 <= lon4){
			return lon3;
		}else {
			return lon4;
		}
	}
	
	//������ҵ�λ�á�
	private void returnMyLoc(){
		mLocationClient.requestLocation();
		LatLng ll = new LatLng(mCurrentLatitude, mCurrentLongitude);  
        MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(ll);  
        mBaiduMap.animateMapStatus(u);
	}
	
	private void addPoints(long time, Double mCurrentLat, Double mCurrentLon){
		Position p = new Position();
		p.setTime(time);
		p.setLatitude(mCurrentLat);
		p.setLongitude(mCurrentLon);
		points.add(p);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		// ����ͼ�㶨λ  
        mBaiduMap.setMyLocationEnabled(true);  
        if (!mLocationClient.isStarted())  
        {  
            mLocationClient.start();  
        }
        //�������򴫸���
        myOrientationListener.start();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mMapView.onDestroy(); 
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		mMapView.onPause(); 
	}

	@Override
	protected void onResume() {
		super.onResume();
		mMapView.onResume(); 
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.get_location, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_locate) {
			try{
				returnMyLoc();
			}catch(Exception e){
				Toast.makeText(this, "û�м�⵽λ��", Toast.LENGTH_SHORT).show();
			}
			return true;
		}
		if (id == R.id.action_switch){
			if(judge)
			{
				item.setTitle("�л�����ͨͼ");
				judge = false;
				mBaiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);  //��������ͼ
			}else{
				item.setTitle("�л�������ͼ");
				judge = true;
				mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
			}
			return true;
		}
		if (id == R.id.action_showpoint){
			if(points!=null){
				for(int i=0;i<points.size();i++)
				{
					LatLng singlePoint = new LatLng(points.get(i).getLatitude(),points.get(i).getLongitude());  
					BitmapDescriptor bitmap = BitmapDescriptorFactory  
							.fromResource(R.drawable.locate);  
					//����MarkerOption�������ڵ�ͼ�����Marker  
					OverlayOptions option = new MarkerOptions()  
				    	.position(singlePoint)  
				    	.icon(bitmap); 
					//�ڵ�ͼ�����Marker������ʾ  
					mBaiduMap.addOverlay(option);
				}
			}
			return true;
		}
		if (id == R.id.action_drawroute){
			pts = new ArrayList<LatLng>();
			if(points != null){
				for(int j = 0;j<points.size();j++)
				{
					LatLng pt = new LatLng(points.get(j).getLatitude(), points.get(j).getLongitude());
					pts.add(pt);
				}
			}
			Log.i("Test",String.valueOf(distance));
			//�����������ߵ�Option����  
			OverlayOptions polylineOption = new PolylineOptions()  
			    .points(pts)
			    .width(9)
			    .color(0xAAFF4F4F);
			mBaiduMap.addOverlay(polylineOption);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	//�Ʋ����㲥������
	class MotionReceiver extends BroadcastReceiver{
		@Override
		public void onReceive(Context context, Intent intent) {
			steps = intent.getIntExtra("motionNum",-1);
			paceText.setText(String.valueOf(steps));	
		}
		
	}
}
