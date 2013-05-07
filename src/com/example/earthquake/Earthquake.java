package com.example.earthquake;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;

import javax.xml.parsers.*;

import org.apache.http.ParseException;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import android.location.Location;
import android.os.*;
import android.annotation.*;
import android.app.*;
import android.content.*;
import android.content.res.Resources;
import android.database.Cursor;
import android.view.*;
import android.widget.*;
import android.widget.AdapterView.*;

@SuppressLint("NewApi")
public class Earthquake extends Activity {
	
	ListView earthquakeListView;
	ArrayAdapter<Quake> aa;
	
	ArrayList<Quake> earthquakes = new ArrayList<Quake>();
	
	static final private int MENU_UPDATE = Menu.FIRST;
	static final private int QUAKE_DIALOG = 1;
	Quake selectedQuake;
	
	static final private int MENU_PREFERENCES = Menu.FIRST+1;
	
	private static final int SHOW_PREFERENCES = 1;
	
	int minimumMagnitude = 0;
	boolean autoUpdate = false;
	int updateFreq = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);
		
		earthquakeListView=(ListView)this.findViewById(R.id.earthquakeListView);
		
		// Add Listener to ListView
		
		earthquakeListView.setOnItemClickListener(new OnItemClickListener(){
			
			public void onItemClick(AdapterView _av, View _v, int _index, long arg3){
				selectedQuake = earthquakes.get(_index);
				showDialog(QUAKE_DIALOG);
			}
		});
		
		int layoutID = android.R.layout.simple_list_item_1;
		aa = new ArrayAdapter<Quake>(this,layoutID,earthquakes);
		earthquakeListView.setAdapter(aa);	
		
		//loadQuakeFromProvider(); Chapter 6
		
		updateFromPreferences();
		refreshEarthquakes();
	}	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		menu.add(0,MENU_UPDATE,Menu.NONE,R.string.menu_update);				
		menu.add(0, MENU_PREFERENCES, Menu.NONE, R.string.menu_preferences);

		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		super.onOptionsItemSelected(item);
		
		switch(item.getItemId()){
		case(MENU_UPDATE) :{
			updateFromPreferences();
			refreshEarthquakes();
			return true;
		}
		case(MENU_PREFERENCES) :{
			Intent i = new Intent(this, Preferences.class);
			startActivityForResult(i,SHOW_PREFERENCES);
			return true;
		}
		}		
		return false;
	}
	
	@Override
	public Dialog onCreateDialog(int id){
		switch(id){
		case(QUAKE_DIALOG):
			LayoutInflater li = LayoutInflater.from(this);
			View quakeDetailsView = li.inflate(R.layout.quake_details, null);
			
			AlertDialog.Builder quakeDialog = new AlertDialog.Builder(this);
			quakeDialog.setTitle("Quake Time");
			quakeDialog.setView(quakeDetailsView);
			return quakeDialog.create();
		}
		return null;
	}

	@Override
	public void onPrepareDialog(int id, Dialog dialog){
		switch(id){
		case(QUAKE_DIALOG):
			SimpleDateFormat sdf;
		sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		String dateString = sdf.format(selectedQuake.getDate());
		String quakeText = "Mangitude " + selectedQuake.getMagnitude() + "\n" + selectedQuake.getDetails() + "\n" + selectedQuake.getLink();
		
		AlertDialog quakeDialog = (AlertDialog)dialog;
		quakeDialog.setTitle(dateString);
		TextView tv = (TextView)quakeDialog.findViewById(R.id.quakeDetailsTextView);
		tv.setText(quakeText);
		
		break;
		
		}
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data){
		super.onActivityResult(requestCode, resultCode, data);
		
		if (requestCode == SHOW_PREFERENCES){
			if (resultCode == Activity.RESULT_OK){
				updateFromPreferences();
				refreshEarthquakes();
			}
		}
	}
	
	
	
	private void addQuakeToArray (Quake _quake){
		if (_quake.getMagnitude() > minimumMagnitude){
			// Add the new quake to our list of earthquakes
			earthquakes.add(_quake);
		}
		
		// Notify the array adapter of a change
		aa.notifyDataSetChanged();
	}
	
	private void updateFromPreferences(){
		SharedPreferences prefs = getSharedPreferences(Preferences.USER_PREFERENCE,Activity.MODE_PRIVATE);
		
		int minMagIndex = prefs.getInt(Preferences.PREF_MIN_MAG, 0);
		if (minMagIndex < 0)
		{
			minMagIndex = 0;
		}
		
		int freqIndex = prefs.getInt(Preferences.PREF_UPDATE_FREQ, 0);
		if (freqIndex < 0)
		{
			freqIndex = 0;
		}
		
		autoUpdate = prefs.getBoolean(Preferences.PREF_AUTO_UPDATE, false);
		
		Resources r = getResources();
		
		// Get the option values from the arrays
		int[] minMagValues = r.getIntArray(R.array.magnitude);
		int[] freqValues = r.getIntArray(R.array.update_freq_values);
		
		// Convert the values to ints
		minimumMagnitude = minMagValues[minMagIndex];
		updateFreq = freqValues[freqIndex];
	}
	
	private void loadQuakeFromProvider(){
		// Clear the existing earthquake array
		earthquakes.clear();
		
		ContentResolver cr = getContentResolver();
		
		// Return all the saved earthquakes
		Cursor c = cr.query(EarthquakeProvider.CONTENT_URI, null, null, null, null);
		
		if (c.moveToFirst())
		{
			do{
				//Extract the quake details
				Long datems = c.getLong(EarthquakeProvider.DATE_COLUMN);
				String details;
				details = c.getString(EarthquakeProvider.DETAILS_COLUMN);
				Float lat = c.getFloat(EarthquakeProvider.LATITUDE_COLUMN);
				Float lng = c.getFloat(EarthquakeProvider.LONGITUDE_COLUMN);
				Double mag = c.getDouble(EarthquakeProvider.MAGNITUDE_COLUMN);
				String link = c.getString(EarthquakeProvider.LINK_COLUMN);
				
				Location location = new Location("dummy");
				location.setLatitude(lat);
				location.setLongitude(lng);
				
				Date date = new Date(datems);
				
				Quake q = new Quake(date,details,location,mag,link);
				addQuakeToArray(q);
			}
			while(c.moveToNext());
		}
		c.close();
	}
	
	private void refreshEarthquakes(){
		startService(new Intent(this,EarthquakeService.class));
	}
	
	public class EarthquakeReceiver extends BroadcastReceiver{
		
		
		
		@Override
		public void onReceive(Context context, Intent intent) {
			loadQuakeFromProvider();
		}
		
		

	}
	
	EarthquakeReceiver receiver;
	
	@Override
	public void onResume(){
		IntentFilter filter;
		filter = new IntentFilter(EarthquakeService.NEW_EARTHQUAKE_FOUND);
		receiver = new EarthquakeReceiver();
		registerReceiver(receiver,filter);
		
		loadQuakeFromProvider();
		super.onResume();
	}
	
	@Override
	public void onPause(){
		unregisterReceiver(receiver);
		super.onPause();
	}
	
}
