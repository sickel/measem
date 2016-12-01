package com.mortensickel.measemulator;
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
import android.location.*;
import android.util.Log;


// Todo over a certain treshold, change calibration factor 
// TODO settable calibration factor
// TODO finish icons
// TODO location
// TODO input bacground and source. Calculate activity from distance
// TODO Use distribution map 
// TODO add settings menu
// TODO generic skin
// TODO handle shutdown


public class MainActivity extends Activity 
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
	final Integer MAXMODE=2;
	final int MODE_OFF=0;
	final int MODE_DOSERATE=1;
	final int MODE_DOSE=2;
	public final String TAG="measem";
	double calibration=4.4;
	public Integer sourceact=1;
	public boolean gpsenabled = true;
	private LocationManager locationManager=null;
	private LocationListener locationListener=null; 
	public Context context;
	public Integer gpsinterval=5000;
	
	
	
	
    //this  posts a message to the main thread from our timertask
    //and updates the textfield
	final Handler h = new Handler(new Callback() {
	
			@Override
			public boolean handleMessage(Message msg) {
				long millis = System.currentTimeMillis() - starttime;
				int seconds = (int) (millis / 1000);
				if(seconds>0){
					double display=0;
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
    
	Handler gpsHandler = new Handler();
	Runnable gpsRun=new Runnable(){
		@Override
		public void run(){
			locationListener = new MyLocationListener();

			locationManager.requestLocationUpdates(LocationManager
												   .GPS_PROVIDER, 5000, 10,locationListener);
			
		//	Toast.makeText(context,"Location testing2",Toast.LENGTH_SHORT).show();
			gpsHandler.postDelayed(gpsRun,gpsinterval);
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
				gpsHandler.removeCallbacks(gpsRun);
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
			gpsHandler.postDelayed(gpsRun,gpsinterval);
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
		locationManager = (LocationManager) 
		getSystemService(Context.LOCATION_SERVICE);
		
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
	
	// from http://rdcworld-android.blogspot.no/2012/01/get-current-location-coordinates-city.html?m=1
	/*----------Listener class to get coordinates ------------- */
	private class MyLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location loc) {

            
            Toast.makeText(getBaseContext(),"Location changed : Lat: " +
						   loc.getLatitude()+ " Lng: " + loc.getLongitude(),
						   Toast.LENGTH_SHORT).show();
            String longitude = "Longitude: " +loc.getLongitude();  
			Log.v(TAG, longitude);
			String latitude = "Latitude: " +loc.getLatitude();
			Log.v(TAG, latitude);

			       }

        @Override
        public void onProviderDisabled(String provider) {
            // TODO Auto-generated method stub         
        }

        @Override
        public void onProviderEnabled(String provider) {
            // TODO Auto-generated method stub         
        }

        @Override
        public void onStatusChanged(String provider, 
									int status, Bundle extras) {
            // TODO Auto-generated method stub         
        }
    
	}
	
	
}
