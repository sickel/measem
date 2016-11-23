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
	
	TextView text, text3,tvAct;
    long starttime = 0;
    //this  posts a message to the main thread from our timertask
    //and updates the textfield
	final Handler h = new Handler(new Callback() {

			@Override
			public boolean handleMessage(Message msg) {
				long millis = System.currentTimeMillis() - starttime;
				int seconds = (int) (millis / 1000);
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
			Random rng=new Random();
			Double pause=rng.nextGaussian();
			String act=tvAct.getText().toString();
			if(act.equals("")){
				act="1";
			}
			Integer interval=1000/Integer.parseInt(act);
			Integer sd=interval/5;
			pause=pause*sd+interval;
			if(pause<0){pause=0.0;}
			text3.setText(String.format("%d",pause.intValue()));
			LinearLayout myText = (LinearLayout) findViewById(R.id.llLed );
			Animation anim = new AlphaAnimation(0.0f, 1.0f);
			anim.setDuration(50); //You can manage the time of the blink with this parameter
			anim.setStartOffset(20);
			anim.setRepeatMode(Animation.REVERSE);
			anim.setRepeatCount(0);
			myText.startAnimation(anim);
			h2.postDelayed(this,pause.intValue());
			//Handler blkh=new Handler();
        }
    };

	//tells handler to send a message
	class firstTask extends TimerTask {

        @Override
        public void run() {
            h.sendEmptyMessage(0);
        }
	};

	//tells activity to run on ui thread
	/*class secondTask extends TimerTask {

        @Override
        public void run() {
            main.this.runOnUiThread(new Runnable() {

					@Override
					public void run() {
						long millis = System.currentTimeMillis() - starttime;
						int seconds = (int) (millis / 1000);
						int minutes = seconds / 60;
						seconds     = seconds % 60;

						text2.setText(String.format("%d:%02d", minutes, seconds));
					}
				});
        }
	}; 
	
	*/


	Timer timer = new Timer();
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        text = (TextView)findViewById(R.id.text);
      //  text2 = (TextView)findViewById(R.id.text2);
        text3 = (TextView)findViewById(R.id.text3);
		tvAct=(TextView)findViewById(R.id.activity);
        Button b = (Button)findViewById(R.id.button);
        b.setText("start");
        b.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					Button b = (Button)v;
					if(b.getText().equals("stop")){
						timer.cancel();
						timer.purge();
						h2.removeCallbacks(run);
						b.setText("start");
					}else{
						starttime = System.currentTimeMillis();
						timer = new Timer();
						timer.schedule(new firstTask(), 0,500);
						//timer.schedule(new secondTask(),  0,500);
						h2.postDelayed(run, 0);
						b.setText("stop");
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
