package com.jotabout.screeninfo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class ScreenInfo extends Activity {
	
	private final static int ABOUT_DIALOG = 1;
	private final static int MENU_ABOUT = Menu.FIRST;
	Dialog mAbout;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }
    
    @Override
	protected void onResume() {
		super.onResume();
		
        showDeviceInfo();
        showScreenMetrics();
        showDefaultOrientation();
        showCurrentOrientation();
	}

	/**
     * Show basic information about the device.
     */
    public void showDeviceInfo() {
        ((TextView) findViewById(R.id.device_name)).setText( Build.MODEL );
        ((TextView) findViewById(R.id.os_version)).setText( Build.VERSION.RELEASE );
    }
    
    /**
     * Show the screen metrics (pixel dimensions, density, dpi, etc) for the device.
     */
    public void showScreenMetrics() {
		WindowManager wm = ((WindowManager) getSystemService(Context.WINDOW_SERVICE));
		Display display = wm.getDefaultDisplay();
    	
        ((TextView) findViewById(R.id.width_pixels)).setText( Integer.toString(display.getWidth()) );
        ((TextView) findViewById(R.id.height_pixels)).setText( Integer.toString(display.getHeight()) );
        
		DisplayMetrics metrics = new DisplayMetrics();
		display.getMetrics(metrics);

        ((TextView) findViewById(R.id.screen_dpi)).setText( Integer.toString(metrics.densityDpi) );
        ((TextView) findViewById(R.id.actual_xdpi)).setText( Float.toString(metrics.xdpi) );
        ((TextView) findViewById(R.id.actual_ydpi)).setText( Float.toString(metrics.ydpi) );
        ((TextView) findViewById(R.id.logical_density)).setText( Double.toString(metrics.density) );
        ((TextView) findViewById(R.id.font_scale_density)).setText( Float.toString(metrics.scaledDensity) );
        
    	showScreenDiagonalSize(metrics);
    }

    /**
     * Calculate and display the physical diagonal size of the screen.
     * The size is calculated in inches, rounded to one place after decimal (e.g. '3.7', '10.1')
     * The size is also calculated in millimeters
     * 
     * @param metrics
     */
	private void showScreenDiagonalSize(DisplayMetrics metrics) {
		double xdpi = metrics.xdpi;
		if ( xdpi < 1.0 ) {
			// Guard against divide-by-zero, possible with lazy device manufacturers who set these fields incorrectly
			// Set the density to our best guess.
			xdpi = metrics.densityDpi;
		}
		double ydpi = metrics.ydpi;
		if ( ydpi < 1.0 ) {
			ydpi =  metrics.densityDpi;
		}
		
		// Calculate physical screen width/height
		double physicalWidth = ((double) metrics.widthPixels) / xdpi;
		double physicalHeight = ((double) metrics.heightPixels) / ydpi;
		
		// Calculate diagonal screen size, in both U.S. and Metric units
		double rawDiagonalSizeInches = Math.sqrt(Math.pow(physicalWidth, 2) + Math.pow(physicalHeight, 2));
		double diagonalSizeInches = Math.floor( rawDiagonalSizeInches * 10.0 + 0.5 ) / 10.0;
		double diagonalSizeMillimeters = Math.floor( rawDiagonalSizeInches * 25.4 + 0.5 );
        ((TextView) findViewById(R.id.computed_diagonal_size_inches)).setText( Double.toString(diagonalSizeInches) );
        ((TextView) findViewById(R.id.computed_diagonal_size_mm)).setText( Double.toString(diagonalSizeMillimeters) );
	}

	/**
	 * Display the "natural" screen orientation of the device.
	 */
	private void showDefaultOrientation() {
		// Screen default orientation
        TextView orientationText = ((TextView) findViewById(R.id.natural_orientation));
        Configuration config = getResources().getConfiguration();
        setOrientationText(orientationText, config.orientation);
	}

	/**
	 * Display the current screen orientation of the device, with respect to natural orientation.
	 */
	private void showCurrentOrientation() {
		WindowManager wm = ((WindowManager) getSystemService(Context.WINDOW_SERVICE));
		Display display = wm.getDefaultDisplay();
        TextView orientationText = ((TextView) findViewById(R.id.current_orientation));
		
		// First, try the Display#getRotation() call, which was introduced in Froyo.
		// Reference: http://android-developers.blogspot.com/2010/09/one-screen-turn-deserves-another.html
		try {
			Method getRotationMethod = display.getClass().getMethod("getRotation");
			int rotation = (Integer) getRotationMethod.invoke(display);
			switch (rotation) {
			case Surface.ROTATION_0:
				orientationText.setText("0");
				break;
			case Surface.ROTATION_90:
				orientationText.setText("90");
				break;
			case Surface.ROTATION_180:
				orientationText.setText("180");
				break;
			case Surface.ROTATION_270:
				orientationText.setText("270");
				break;
			}
			
			return;
		}
		catch (SecurityException e) {;}
		catch (NoSuchMethodException e) {;} 
		catch (IllegalArgumentException e) {;}
		catch (IllegalAccessException e) {;}
		catch (InvocationTargetException e) {;}
		
		// Fall back on the deprecated Display#getOrientation method from earlier releases of Android.
		int orientation = display.getOrientation();
		setOrientationText( orientationText, orientation );
	}
	
	/**
	 * Helper sets an orientation string in the given text widget.
	 * 
	 * @param orientationText
	 * @param orientation
	 */
	private void setOrientationText(TextView orientationText, int orientation) {
		switch ( orientation ) {
        case Configuration.ORIENTATION_LANDSCAPE:
        	orientationText.setText(R.string.orientation_landscape);
        	break;
        case Configuration.ORIENTATION_PORTRAIT:
        	orientationText.setText(R.string.orientation_portrait);
        	break;
        case Configuration.ORIENTATION_SQUARE:
        	orientationText.setText(R.string.orientation_square);
        	break;
        case Configuration.ORIENTATION_UNDEFINED:
        	orientationText.setText(R.string.orientation_undefined);
        	break;
        }
	}
	
	/**
	 * Helper returns a string containing version number from the package manifest.
	 */
	private String appVersion() {
		String version = "";
		PackageInfo info;
		try {
			info = getPackageManager().getPackageInfo(getPackageName(), 0);
			version = this.getString(R.string.version) + " " + info.versionName;
		} catch (NameNotFoundException ignore) {;}

		return version;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		mAbout = null;

		switch( id ) {
		case ABOUT_DIALOG:
	        mAbout = new Dialog(this);
	        mAbout.setContentView(R.layout.about_dialog);
	        mAbout.setTitle(R.string.about_title);
	        ((TextView) mAbout.findViewById(R.id.about_version)).setText(appVersion());
	        ((Button) mAbout.findViewById(R.id.about_dismiss)).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					mAbout.dismiss();
				}
	        });
		}

		return mAbout;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add( 0, MENU_ABOUT, 0, R.string.about_menu );
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch( item.getItemId() ) {
		case MENU_ABOUT:
			showDialog(ABOUT_DIALOG);
			return true;
		}
		
		return false;
	}
}