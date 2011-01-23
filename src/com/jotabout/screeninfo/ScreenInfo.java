package com.jotabout.screeninfo;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
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
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        showDeviceInfo();
        showScreenInfo();
    }
    
    public void showDeviceInfo() {
        ((TextView) findViewById(R.id.device_name)).setText( Build.MODEL );
        ((TextView) findViewById(R.id.os_version)).setText( Build.VERSION.RELEASE );
    }
    
    public void showScreenInfo() {
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
        
    	// Screen physical diagonal size in inches, rounded to one place after decimal (e.g. '3.7', '10.1')
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
		
		double physicalWidth = ((double) metrics.widthPixels) / xdpi;
		double physicalHeight = ((double) metrics.heightPixels) / ydpi;
		
		double rawDiagonalSizeInches = Math.sqrt(Math.pow(physicalWidth, 2) + Math.pow(physicalHeight, 2));
		double diagonalSizeInches = Math.floor( rawDiagonalSizeInches * 10.0 + 0.5 ) / 10.0;
		double diagonalSizeMillimeters = Math.floor( rawDiagonalSizeInches * 25.4 + 0.5 );
        ((TextView) findViewById(R.id.computed_diagonal_size_inches)).setText( Double.toString(diagonalSizeInches) );
        ((TextView) findViewById(R.id.computed_diagonal_size_mm)).setText( Double.toString(diagonalSizeMillimeters) );
    }

	@Override
	protected Dialog onCreateDialog(int id) {
		mAbout = null;

		switch( id ) {
		case ABOUT_DIALOG:
	        mAbout = new Dialog(this);
	        mAbout.setContentView(R.layout.about_dialog);
	        mAbout.setTitle(R.string.about_title);
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