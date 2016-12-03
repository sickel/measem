package com.mortensickel.measemulator;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceScreen;


public class SetPreferenceActivity extends Activity
{
	
	@Override
	public void onCreate(Bundle SavedInstanceState){
		super.onCreate(SavedInstanceState);
		getFragmentManager().beginTransaction().replace(android.R.id.content,new SettingsFragment()).commit();
	}
}

