package com.mortensickel.measemulator;
// http://maps.google.com/maps?q=loc:59.948509,10.602627
import com.google.android.gms.common.api.*;
import android.content.Context;
import android.text.*;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.view.animation.*;
import android.app.*;
import android.os.*;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Handler.Callback;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.*;
import android.view.View.*;
import android.location.Location;
import android.util.Log;
import com.google.android.gms.location.*;
import com.google.android.gms.common.*;
import android.preference.*;
import android.view.*;
import android.content.*;


// Todo over a certain treshold, change calibration factor 
// TODO settable calibration factor
// TODO finish icons
// DONE location 
// TODO input background and source. Calculate activity from distance
// TODO Use distribution map 
// TODO add settings menu
// TODO generic skin
// TODO handle shutdown


public class MainActivity extends Activity 
implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,LocationListener
{
	/*
	AUTOMESS:
	0.44 pps = 0.1uSv/h
	ca 16. pulser pr nSv
	*/
	boolean poweron=false;
	boolean showdebug=false;
	long shutdowntime=0;
	long meastime;
	TextView tvTime,tvPulsedata, tvPause,tvAct, tvDoserate;
	long starttime = 0;
	public long pulses=0;
	Integer mode=0;
	final Integer MAXMODE=3;
	final int MODE_OFF=0;
	final int MODE_MOMENTANDOSE=1;
	final int MODE_DOSERATE=2;
	final int MODE_DOSE=3;
	public final String TAG="measem";
	double calibration=4.4;
	public Integer sourceact=1;
	protected long lastpulses=0;
	public boolean gpsenabled = true;
	public Context context;
	public Integer gpsinterval=2000;
	private GoogleApiClient gac;
	private Location here,there;
	protected LocationRequest loreq;
	private LinearLayout llDebuginfo;
	
	@Override
	public void onConnectionFailed(ConnectionResult p1)
	{
		// TODO: Implement this method
	}

	protected void createLocationRequest(){
		loreq = new LocationRequest();
		loreq.setInterval(gpsinterval);
		loreq.setFastestInterval(100);
		loreq.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
	}
	
	protected void startLocationUpdates(){
		if(loreq==null){
			createLocationRequest();
		}
		LocationServices.FusedLocationApi.requestLocationUpdates(gac,loreq,this);
	}
	
	public void ConnectionCallbacks(){
		
	}

	@Override
	public void onLocationChanged(Location p1)
	{
		here=p1;
		double distance=here.distanceTo(there);
		sourceact=(int)Math.round(2.0+1000.0/(distance*distance));
		tvAct.setText(String.valueOf(sourceact));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// TODO: Implement this method
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.mainmenu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		
		switch (item.getItemId()){
			case R.id.mnuSettings:
				Intent intent=new Intent();
				intent.setClass(MainActivity.this,SetPreferenceActivity.class);
				startActivityForResult(intent,0);
				return true;
			case R.id.mnuSaveLoc:
				saveLocation();
				return true;
			case R.id.mnuShowLoc:
				showLocation();
				return true;
		}
		
		// TODO: Implement this method
		return super.onOptionsItemSelected(item);
	}

	protected void showLocation(){
		String lat=String.valueOf(there.getLatitude());
		String lon=String.valueOf(there.getLongitude());
		Toast.makeText(getApplicationContext(),getString(R.string.SourceLocation)+lat+','+lon, Toast.LENGTH_LONG).show();
		
	}
	
	
	protected void saveLocation(){
		Location LastLocation = LocationServices.FusedLocationApi.getLastLocation(
			gac);
        if (LastLocation != null) {
            String lat=String.valueOf(LastLocation.getLatitude());
            String lon=String.valueOf(LastLocation.getLongitude());
			Toast.makeText(getApplicationContext(),getString(R.string.SourceLocation)+lat+','+lon, Toast.LENGTH_LONG).show();
		    there=LastLocation;
        }else{
			Toast.makeText(getApplicationContext(),getString(R.string.CouldNotGetLocation), Toast.LENGTH_LONG).show();	
		}
    
	}
	
	@Override
	public void onConnected(Bundle p1)
	{
		Location loc = LocationServices.FusedLocationApi.getLastLocation(gac);
		if(loc != null){
			here=loc;
		}
		// TODO: Implement this method
	}

	@Override
	public void onConnectionSuspended(int p1)
	{
		// TODO: Implement this method
	}

	protected void onStart(){
		gac.connect();
		super.onStart();
	}
	
	
	protected void onStop(){
		gac.disconnect();
		super.onStop();
	}
	
	
    //this  posts a message to the main thread from our timertask
    //and updates the textfield
	final Handler h = new Handler(new Callback() {
	
			@Override
			public boolean handleMessage(Message msg) {
				long millis = System.currentTimeMillis() - starttime;
				int seconds = (int) (millis / 1000);
				if(seconds>0){
					double display=0;
					if( mode==MODE_MOMENTANDOSE){
						if (lastpulses==0 || (lastpulses>pulses)){
							display=0;
						}else{
							display=((pulses-lastpulses)/calibration);
						}
						lastpulses=pulses;
					}
					if (mode==MODE_DOSERATE){
						display=(double)pulses/(double)seconds/calibration;
					}
					if (mode==MODE_DOSE){
						display=(double)pulses/calibration/3600;
					}
					tvDoserate.setText(String.format("%.2f",display));
				}
				if(showdebug){
					int minutes = seconds / 60;
					seconds     = seconds % 60;
					tvTime.setText(String.format("%d:%02d", minutes, seconds));			
				}
				return false;
			}
		});
	
		
		
	//runs without timer - reposting itself after a random interval
	Handler h2 = new Handler();
	Runnable run = new Runnable() {
        @Override
        public void run() {
			long pause=pause(getInterval());
			h2.postDelayed(run,pause);
			if(showdebug){
				tvPause.setText(String.format("%d",pause));
			}
			receivepulse();
        }
    };
    
	
	
	public Integer getInterval(){
		Integer act=sourceact;
		if(act==0){
			act=1;
		}
		Integer interval=5000/act;
		if (interval==0){
			interval=1;
		}
		return(interval);
	}
	
	public long pause(Integer interval){
		double pause=interval;
		if(interval > 5){
			Random rng=new Random();
			pause=rng.nextGaussian();

			Integer sd=interval/4;
			pause=pause*sd+interval;
			if(pause<0){pause=0;}
		}
		return((long)pause);
	}
	
	
	public void receivepulse(){
		LinearLayout myText = (LinearLayout) findViewById(R.id.llLed );
		Animation anim = new AlphaAnimation(0.0f, 1.0f);
		anim.setDuration(20); //You can manage the time of the blink with this parameter
		anim.setStartOffset(20);
		anim.setRepeatMode(Animation.REVERSE);
		anim.setRepeatCount(0);
		myText.startAnimation(anim);
		pulses++;
		Double sdev=Math.sqrt(pulses);
		if(showdebug){
		tvPulsedata.setText(String.format("%d - %.1f - %.0f %%",pulses,sdev,sdev/pulses*100));
		}
	}
	
	//tells handler to send a message
	class firstTask extends TimerTask {

        @Override
        public void run() {
            h.sendEmptyMessage(0);
        }
	};
	
	
	
	Timer timer = new Timer();
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
		context=this;
		there = new Location("dummyprovider");
		there.setLatitude(59.948509);
		there.setLongitude(10.602627);
		llDebuginfo=(LinearLayout)findViewById(R.id.llDebuginfo);
		llDebuginfo.setVisibility(View.GONE);
		gac=new GoogleApiClient.Builder(this).addConnectionCallbacks(this).addOnConnectionFailedListener(this).addApi(LocationServices.API).build();
        tvTime = (TextView)findViewById(R.id.tvTime);
        tvPulsedata = (TextView)findViewById(R.id.tvPulsedata);
        tvPause = (TextView)findViewById(R.id.tvPause);
		tvDoserate = (TextView)findViewById(R.id.etDoserate);
		tvAct=(EditText)findViewById(R.id.activity);
		tvAct.addTextChangedListener(new TextWatcher() {
				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
					// TODO Auto-generated method stub
				}

				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {
					// TODO Auto-generated method stub
				}

				@Override
				public void afterTextChanged(Editable s) {
					String act=tvAct.getText().toString();
					if(act.equals("")){act="1";}
					sourceact=Integer.parseInt(act);
					// TODO better errorchecking.
					// TODO disable if using geolocation
				}
			});
        switchMode(mode);
		Button b = (Button)findViewById(R.id.button);
        b.setText("start");
        b.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Button b = (Button)v;
				if(poweron){
					timer.cancel();
					timer.purge();
					h2.removeCallbacks(run);
					pulses=0;
					b.setText("start");
					poweron=false;
				}else{
					starttime = System.currentTimeMillis();
					timer = new Timer();
					timer.schedule(new firstTask(), 0,500);
					h2.postDelayed(run, pause(getInterval()));
					b.setText("stop");
					poweron=true;
				}
			}
		});
    

    b = (Button)findViewById(R.id.btPower);
	b.setOnClickListener(new View.OnClickListener() {
	@Override
	public void onClick(View v) {
		//Button b = (Button)v;
		if(poweron){
			long now=System.currentTimeMillis();
			if(now> shutdowntime && now < shutdowntime+500){
				timer.cancel();
				timer.purge();
				h2.removeCallbacks(run);
				pulses=0;
				poweron=false;
				mode=MODE_OFF;
				switchMode(mode);
			}
			shutdowntime = System.currentTimeMillis()+500;
			
		}else{
			shutdowntime=0;
			starttime = System.currentTimeMillis();
			timer = new Timer();
			timer.schedule(new firstTask(), 0,500); 
			startLocationUpdates();
			h2.postDelayed(run, pause(getInterval()));
			mode=1;
			switchMode(mode);
			poweron=true;
		}
	}
	});
	b=(Button)findViewById(R.id.btMode);
	b.setOnClickListener(new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			modechange(v);
	}});
	b=(Button)findViewById(R.id.btLight);
	b.setOnClickListener(new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			showdebug=!showdebug;
		}});	
	
		
	}
  	@Override
    public void onPause() {
        super.onPause();
        timer.cancel();
        timer.purge();
        h2.removeCallbacks(run);
        Button b = (Button)findViewById(R.id.button);
        b.setText("start");
    }
	
	
	public void modechange(View v){
		if(mode > 0){
		mode++;
		if (mode > MAXMODE){
			mode=1;
		}
		switchMode(mode);}
	}

	public void switchMode(int mode){
		int unit=0;
		switch(mode){
			case MODE_MOMENTANDOSE:
			case MODE_DOSERATE:
				unit = R.string.ugyh;
				break;
			case MODE_DOSE:
				unit = R.string.ugy;
				break;
			case MODE_OFF:
				unit= R.string.blank;
				tvDoserate.setText("");
				break;
		}
		TextView tv=(TextView)findViewById(R.id.etUnit);
		tv.setText(unit);
	}
	
	
}
