package com.citysmartgo.android.taxiguide;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.bugsense.trace.BugSenseHandler;
import com.citysmartgo.android.taxiguide.api.ApiResponse;
import com.citysmartgo.android.taxiguide.api.distancematrix.DistanceMatrixApiRequest;
import com.citysmartgo.android.taxiguide.api.distancematrix.DistanceMatrixApiResponse;
import com.citysmartgo.android.taxiguide.database.DatabaseHelper;
import com.citysmartgo.android.taxiguide.dialogs.DiscountDialog;
import com.citysmartgo.android.taxiguide.fragments.CompaniesFragment;
import com.citysmartgo.android.taxiguide.fragments.DiscountsFragment;
import com.citysmartgo.android.taxiguide.fragments.SearchFragment;
import com.citysmartgo.android.taxiguide.util.ApiConnectionUtility;
import com.citysmartgo.android.taxiguide.util.GeolocationUtility;
import com.citysmartgo.android.taxiguide.util.NetworkUtility;
import com.flurry.android.FlurryAgent;
import com.google.android.maps.GeoPoint;
import com.viewpagerindicator.TabPageIndicator;

/**
 * initial activity that appears up on the application startup
 * 
 * @author zikesjan, matyama
 * 
 */
public class MainActivity extends SherlockFragmentActivity implements ActionBar.OnNavigationListener{
	
	private ViewPager mViewPager;

	public TextView tabCenter;
	public TextView tabText;
	
	public static final int ORIGIN_REQUEST_CODE = 0;
	public static final int DESTINATION_REQUEST_CODE = 1;
	
	public static final int SEARCH_FRAGMENT_PAGE_NUMBER = 0;
	public static final int COMPANIES_FRAGMENT_PAGE_NUMBER = 1;
	public static final int DISCOUNTS_FRAGMENT_PAGE_NUMBER = 2;
	
	
	@Override
	public void onStart() {
	   super.onStart();
	   FlurryAgent.onStartSession(this, "WS35X95BVFK7B9M9CC4F");
	   String id = Secure.getString(getBaseContext().getContentResolver(), Secure.ANDROID_ID); 
	   FlurryAgent.setUserId(id);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		BugSenseHandler.setup(this, "b3cde333"); // XXX

		ProgressDialog dialog = ProgressDialog.show(this, "", getResources().getString(R.string.db_dialog_text), true);
		DatabaseHelper.getInstance(this); 
		dialog.cancel();
		
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(new CustomFragmentAdapter(getSupportFragmentManager()));
		
		TabPageIndicator indicator = (TabPageIndicator) findViewById(R.id.indicator);
		indicator.setViewPager(mViewPager);
		indicator.setCurrentItem(0);
		indicator.setOnPageChangeListener(new OnPageChangeListener() {

			@Override
			public void onPageSelected(int arg0) {
				if (arg0 == 1) {
					hideKeyboard();
				}
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
				// empty body
			}

			@Override
			public void onPageScrollStateChanged(int arg0) {
				// empty body
			}
		});
		
		Context context = getSupportActionBar().getThemedContext();
        ArrayAdapter<CharSequence> list = ArrayAdapter.createFromResource(context, R.array.cities, R.layout.sherlock_spinner_item);
        list.setDropDownViewResource(R.layout.sherlock_spinner_dropdown_item);

        getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        getSupportActionBar().setListNavigationCallbacks(list, this);
		
	}

	/**
	 * Method dealing with spinner choice
	 */
	@Override
	public boolean onNavigationItemSelected(int position, long itemId) {
		String city;
		if(position == 0){
			city=getResources().getString(R.string.Praha);
		}else if(position == 1){
			city=getResources().getString(R.string.Brno);
		}else{
			city=getResources().getString(R.string.Bratislava);
		}
		SharedPreferences sp = getSharedPreferences("city", Context.MODE_PRIVATE);
		Editor editor = sp.edit();
		editor.putString("city", city); 
		editor.commit();
		CompaniesFragment companies = (CompaniesFragment) getSupportFragmentManager().findFragmentByTag("android:switcher:"+ R.id.pager + ":" + COMPANIES_FRAGMENT_PAGE_NUMBER);
		DiscountsFragment discounts = (DiscountsFragment) getSupportFragmentManager().findFragmentByTag("android:switcher:"+ R.id.pager + ":" + DISCOUNTS_FRAGMENT_PAGE_NUMBER);
		if (companies != null ) {
			companies.recreateList(city);
		}
		if(discounts != null){
			discounts.recreateList(city);
		}
		return true;
	}
	
	
	/**
	 * method creating options menu
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.activity_main, menu);
		
		return super.onCreateOptionsMenu(menu);
	}

	/**
	 * method dealing with about particular item in menu selected
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		switch (item.getItemId()) {
		case R.id.rate:
			String myUrl ="https://play.google.com/store/apps/details?id=com.citysmartgo.android.taxiguide";
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(myUrl)));
			return true;
		case R.id.info:
			Intent intentInfo = new Intent(this, InfoActivity.class);
			startActivity(intentInfo);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	
	@Override
	public void onStop() {
	   super.onStop();
	   FlurryAgent.onEndSession(this);	//closing flurry onStop
	}
	
	/**
	 * sets origin location (and text) by calling map activity
	 */
	public void setOriginFromMap(View view) {
	    // Set the request code to any code you like, you can identify the callback via this code
		startActivityForResult(new Intent(this, MapActivity.class), ORIGIN_REQUEST_CODE);
	}
	
	/**
	 * sets destination location (and text) by calling map activity
	 */
	public void setDestinationFromMap(View view) {
	    // Set the request code to any code you like, you can identify the callback via this code
		startActivityForResult(new Intent(this, MapActivity.class), DESTINATION_REQUEST_CODE);
	}
	
	/**
	 * method dealing with the map "subactivity" result
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK && data.hasExtra("address text")) {
			switch (requestCode) {
				case ORIGIN_REQUEST_CODE:
					// set new origin text
					EditText originEditText = (EditText) findViewById(R.id.origin_autocomplete);
					originEditText.setText(data.getExtras().getString("address text"));
					break;
				case DESTINATION_REQUEST_CODE:
					// set new destination text
					EditText destinationEditText = (EditText) findViewById(R.id.destination_autocomplete);
					destinationEditText.setText(data.getExtras().getString("address text"));
					break;
				default:
					break;
			}
		  }
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	/**
	 * method for deleting origin autocomplete
	 * @param view
	 */
	public void deleteOriginText(View view) {
		EditText originEditText = (EditText) findViewById(R.id.origin_autocomplete);
		originEditText.setText("");
	}

	/**
	 * method for deleting destination autocomplete
	 * @param view
	 */
	public void deleteDestinationText(View view) {
		EditText destinationEditText = (EditText) findViewById(R.id.destination_autocomplete);
		destinationEditText.setText("");
	}
	
	/**
	 * dealing with the search button click
	 */
	public void search(View view) {
		if (NetworkUtility.isOnline(getBaseContext())) {	//checking the Internet connection
			EditText originEditText = (EditText) findViewById(R.id.origin_autocomplete);
			EditText destinationEditText = (EditText) findViewById(R.id.destination_autocomplete);
			
			String originText = originEditText.getText().toString();
			String destinationText = destinationEditText.getText().toString();
			
			if (!"".equals(destinationText)) {	//dealing with possible wrong inputs
				if ("".equals(originText)) {
					originText = originEditText.getHint() != null ? originEditText.getHint().toString() : "";
				}
				callApiAndMoveToSearchResult(originText, destinationText);
				logSearchInputs(originText, destinationText);
			}else{
				destinationUndefinedToast();
			}
			 
		} else {
			hideKeyboard();
			Toast.makeText(
					this,
					getApplicationContext().getResources().getString(
							R.string.internet_connection_problem),
					Toast.LENGTH_SHORT).show();
		}

	}

	/**
	 * method dealing with add discount button click
	 */
	public void addDiscount(View view) {
		DiscountDialog.newInstance().show(getSupportFragmentManager(), "discount");
	}

	private void logSearchInputs(String originText, String destinationText) {
		if (originText != null && originText != "" && destinationText != null && destinationText != "") {
			Map<String, String> map = new HashMap<String, String>();
			map.put("origin", originText);
			map.put("destination", destinationText);
			FlurryAgent.logEvent("Search from Main", map);
		}
	}
	
	/**
	 * calling distance matrix API and moving to the result 
	 * @param originText
	 * @param destinationText
	 */
	private void callApiAndMoveToSearchResult(String originText, String destinationText) {
		AsyncTask<String, Void, DistanceMatrixApiResponse> task = new ApiAndSearchResultCallAsyncTask();
		task.execute(originText, destinationText);
	}
	
	/**
	 * method for moving to search result activity
	 */
	private void moveToSearchResult(String originText, String destinationText, double distance, String distanceText, long time, String timeText) {
		if (originText == null || originText == "") {
			hideKeyboard();
			originUndefinedToast();
			return;
		}
		if (destinationText == null || destinationText == "") {
			hideKeyboard();
			destinationUndefinedToast();
			return;
		}
		if (distanceText != null && distanceText != "" && timeText != null && timeText != "") {
			Intent intent = new Intent(this, SearchResultActivity.class);
			intent.putExtra("originText", originText);
			intent.putExtra("destinationText", destinationText);
			intent.putExtra("distance", distance);
			intent.putExtra("distanceText", distanceText);
			intent.putExtra("time", time);
			intent.putExtra("timeText", timeText);
			startActivity(intent);
		}
	}

	/**
	 * 
	 * method for hiding the keyboard
	 */
	public void hideKeyboard() {
		try {
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(findViewById(R.id.origin_autocomplete)
					.getWindowToken(), 0);
		} catch (Exception e) {
			e.printStackTrace();  
		}
	}

	/**
	 * method for hiding keyboard from view
	 */
	public void hideKeyboard(View view) { 
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(findViewById(R.id.origin_autocomplete).getWindowToken(), 0);
	}
	
	/**
	 * just creating certain type of the toast
	 */
	private void originUndefinedToast() {
		Toast.makeText(
				this,
				getApplicationContext().getResources().getString(
						R.string.cant_find_your_origin),
				Toast.LENGTH_SHORT).show();
	}
	
	/**
	 * just creating certain type of the toast
	 */
	private void destinationUndefinedToast() {
		Toast.makeText(
				this,
				getApplicationContext().getResources().getString(
						R.string.cant_find_your_destination),
				Toast.LENGTH_SHORT).show();
	}

	/**
	 * AsyncTask that transforms origin and destination texts to GeoPoints, calls distance matrix API and finally redirects user to search result activity
	 * execute method accepts exactly 2 params (origin and destination)
	 * @author matyama, zikesjan
	 *
	 */
	private class ApiAndSearchResultCallAsyncTask extends AsyncTask<String, Void, DistanceMatrixApiResponse> {

		// private GeoPoint origin;
		
		// private GeoPoint destination;
		
		private ProgressDialog dialog;
		
		private String originText = "";
		
		private String destinationText = "";
		
		private GeoPoint origin;
		
		private GeoPoint destination;
		
		@Override
		protected void onPreExecute() {
			// hide keyboard if shown
			hideKeyboard();
			
			// show progress dialog
			dialog = ProgressDialog.show(MainActivity.this, "",
					getResources().getString(R.string.search_dialog_text), true);
			dialog.setCancelable(true);
			
			super.onPreExecute();
		}
		
		@Override
		protected DistanceMatrixApiResponse doInBackground(String... locations) {
			// check for invalid number of arguments
			if (locations.length != 2) {
				return new DistanceMatrixApiResponse(ApiResponse.Status.ERROR);
			}
			
			originText = locations[0];
			destinationText = locations[1];					
			
			// check for invalid input
			if (originText == null || originText == "" || destinationText == null || destinationText == "") {
				return new DistanceMatrixApiResponse(ApiResponse.Status.ERROR);
			}
			
			Locale locale = Locale.getDefault();
			
			// get origin and destination points from texts
			origin = GeolocationUtility.locationStringToPoint(locations[0], MainActivity.this, locale);
			destination = GeolocationUtility.locationStringToPoint(locations[1], MainActivity.this, locale);
			
			if(origin == null || destination == null){
				return null;
			}
			// call distance matrix API for distance and time
			ApiResponse response = ApiConnectionUtility.getFromApi(new DistanceMatrixApiRequest(origin, destination, null, locale, true));	//FIXME
			
			// return result of appropriate instance
			return response.getStatus() != ApiResponse.Status.OK ? new DistanceMatrixApiResponse(response.getStatus()) : (DistanceMatrixApiResponse) response;	//FIXME
		}

		@Override
		protected void onPostExecute(DistanceMatrixApiResponse result) {
			if (originText == null || originText == "" || origin == null) {
				originUndefinedToast();
				dialog.cancel();
				super.onPostExecute(result);
				return;
			}
			if (destinationText == null || destinationText == "" || destination == null) {
				destinationUndefinedToast();
				dialog.cancel();
				super.onPostExecute(result);
				return;
			}
			if (result.getStatus() == ApiResponse.Status.OK) {
				if (dialog != null) {
					dialog.cancel();
				}
				moveToSearchResult(originText, destinationText, result.getDistance(), result.getDistanceText(), result.getDuration(), result.getDurationText());
			} else if (result.getStatus() == ApiResponse.Status.API_CALL_ERROR) {
				Toast.makeText(
						MainActivity.this,
						getApplicationContext().getResources().getString(R.string.internet_connection_problem),Toast.LENGTH_SHORT).show();
				dialog.cancel();	
			} else {	
				Toast.makeText(
						MainActivity.this,
						getApplicationContext().getResources().getString(
								R.string.inconnected_route), Toast.LENGTH_SHORT).show();
				dialog.cancel();
			}
			super.onPostExecute(result);
		}

	}

	/**
	 * class that handles tab sweeping and changing
	 * @author zikesjan
	 * 
	 */
	public class CustomFragmentAdapter extends FragmentPagerAdapter {

		private final String[] TITLES = new String[] {
				getResources().getString(R.string.search),
				getResources().getString(R.string.companies),
				getResources().getString(R.string.discounts)
		};

		public final int NUM_TITLES = TITLES.length;

		public CustomFragmentAdapter(FragmentManager fm) {
			super(fm);

		}

		@Override
		public Fragment getItem(int position) {
			switch (position) {
			case SEARCH_FRAGMENT_PAGE_NUMBER:
				return SearchFragment.newInstance();
			case COMPANIES_FRAGMENT_PAGE_NUMBER:
				return CompaniesFragment.newInstance();
			case DISCOUNTS_FRAGMENT_PAGE_NUMBER:
				return DiscountsFragment.newInstance();
			}
			return null;
		}

		@Override
		public int getCount() {
			return NUM_TITLES;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return TITLES[position % NUM_TITLES].toUpperCase();
		}
		
		/**
		 * method that prevents FragmentPagerAdapter from recreating fragments every swipe.
		 * It is important to not have super in it.
		 */
		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			// empty body
		}
	}

	

}
