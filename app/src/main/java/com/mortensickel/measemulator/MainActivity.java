package com.mortensickel.measemulator;


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



public class MainActivity extends Activity 
{
	/*
	0.44 pps = 0.1uSv/h
	16. pulser pr nSv
	*/
	boolean poweron=false;
	long meastime;
	TextView text,text2, text3,tvAct, tvDoserate;
    long starttime = 0;
	public long pulses=0;
    //this  posts a message to the main thread from our timertask
    //and updates the textfield
	final Handler h = new Handler(new Callback() {

			@Override
			public boolean handleMessage(Message msg) {
				long millis = System.currentTimeMillis() - starttime;
				int seconds = (int) (millis / 1000);
				if(seconds>0){
					double doserate=(double)pulses/(double)seconds/4.4;
					tvDoserate.setText(String.format("%.2f",doserate));
				}
				int minutes = seconds / 60;
				seconds     = seconds % 60;
				text.setText(String.format("%d:%02d", minutes, seconds));			
				return false;
			}
		});
	//runs without timer be reposting self
	Handler h2 = new Handler();
	Runnable run = new Runnable() {

        @Override
        public void run() {
			
			Integer act=Integer.parseInt(tvAct.getText().toString());
			if(act==0){
				act=1;
			}
			Integer interval=5000/act;
			if (interval==0){
				interval=1;
			}
			
			long pause=interval;
			if(interval > 5){
				Random rng=new Random();
				pause=(long)rng.nextGaussian();
			
				Integer sd=interval/4;
				pause=pause*sd+interval;
				if(pause<0){pause=0;}
			}
			h2.postDelayed(run,pause);
			text3.setText(String.format("%d",pause));
			
			//h2.postDelayed(this,pause.intValue());
			//text3.setText(String.format("%d",pause.intValue()));
			receivepulse();
			//Handler blkh=new Handler();
        }
    };
    
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
		text2.setText(String.format("%d - %.1f - %.0f %%",pulses,sdev,sdev/pulses*100));
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
		tvAct=(TextView)findViewById(R.id.activity);
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
						h2.postDelayed(run, 0);
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
			timer.cancel();
			timer.purge();
			h2.removeCallbacks(run);
			pulses=0;
			poweron=false;
		}else{
			starttime = System.currentTimeMillis();
			timer = new Timer();
			timer.schedule(new firstTask(), 0,500);
			//timer.schedule(new secondTask(),  0,500);
			h2.postDelayed(run, 0);
			poweron=true;
		}
	}
	});
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
	
}
