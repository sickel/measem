package com.mortensickel.measemulator;

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

/* Todo over a certain treshold, change calibration factor */

public class MainActivity extends Activity 
{
	/*
	0.44 pps = 0.1uSv/h
	ca 16. pulser pr nSv
	*/
	boolean poweron=false;
	boolean showdebug=false;
	long shutdowntime=0;
	long meastime;
	TextView text,text2, text3,tvAct, tvDoserate;
	long starttime = 0;
	public long pulses=0;
	Integer mode=0;
	final Integer MAXMODE=2;
	final int MODE_OFF=0;
	final int MODE_DOSERATE=1;
	final int MODE_DOSE=2;
	double calibration=4.4;
	public Integer sourceact=1;
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
					text.setText(String.format("%d:%02d", minutes, seconds));			
				}
				return false;
			}
		});
		
	//runs without timer be reposting self after a random interval
	Handler h2 = new Handler();
	Runnable run = new Runnable() {

        @Override
        public void run() {
			long pause=pause(getInterval());
			h2.postDelayed(run,pause);
			if(showdebug){
				text3.setText(String.format("%d",pause));
			}
			//h2.postDelayed(this,pause.intValue());
			//text3.setText(String.format("%d",pause.intValue()));
			receivepulse();
			//Handler blkh=new Handler();
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
		anim.setDuration(50); //You can manage the time of the blink with this parameter
		anim.setStartOffset(20);
		anim.setRepeatMode(Animation.REVERSE);
		anim.setRepeatCount(0);
		myText.startAnimation(anim);
		pulses++;
		Double sdev=Math.sqrt(pulses);
		if(showdebug){
		text2.setText(String.format("%d - %.1f - %.0f %%",pulses,sdev,sdev/pulses*100));
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
		
        text = (TextView)findViewById(R.id.text);
        text2 = (TextView)findViewById(R.id.text2);
        text3 = (TextView)findViewById(R.id.text3);
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
					// TODO Auto-generated method stub
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
						//timer.schedule(new secondTask(),  0,500);
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
		Button b = (Button)v;
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
			//timer.schedule(new secondTask(),  0,500);
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
