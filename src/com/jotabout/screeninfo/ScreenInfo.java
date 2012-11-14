package com.jotabout.screeninfo;

/**
 * ScreenInfo
 * 
 * Display the screen configuration parameters for an Android device.
 * 
 * Copyright (c) 2011 Michael J. Portuesi (http://www.jotabout.com)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * Main activity class.  Displays information to user.
 */
public class ScreenInfo extends Activity {
	
	//////////////////////////////////////////////////////////////////////////
	// Constants
	//////////////////////////////////////////////////////////////////////////
	
	private final static int ABOUT_DIALOG = 1;
	private final static int MENU_ABOUT = Menu.FIRST;
	private final static int MENU_SHARE = Menu.FIRST + 1;
	
	//////////////////////////////////////////////////////////////////////////
	// State
	//////////////////////////////////////////////////////////////////////////

	Dialog mAbout;
	Screen mScreen;
	
	//////////////////////////////////////////////////////////////////////////
	// Activity Lifecycle
	//////////////////////////////////////////////////////////////////////////
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mScreen = new Screen(this);
    }
    
    @Override
	protected void onResume() {
		super.onResume();
		
        showDeviceInfo();
        showScreenMetrics();
    	showScreenDiagonalSize();
    	showScreenLongWide();
        showDefaultOrientation();
        showCurrentOrientation();
        showTouchScreen();
        showPixelFormat();
        showRefreshRate();
    }
    
	//////////////////////////////////////////////////////////////////////////
	// Info Display
	//////////////////////////////////////////////////////////////////////////

	/**
     * Show basic information about the device.
     */
    public void showDeviceInfo() {
        ((TextView) findViewById(R.id.device_name)).setText( mScreen.deviceModel() );
        ((TextView) findViewById(R.id.os_version)) .setText( mScreen.androidVersion() );
    }
    
    /**
     * Show the screen metrics (pixel dimensions, density, dpi, etc) for the device.
     */
    public void showScreenMetrics() {
        ((TextView) findViewById(R.id.screen_class))		.setText( mScreen.sizeClassificationText(this) );
        ((TextView) findViewById(R.id.density_class))		.setText( mScreen.densityDpiText(this) );
        ((TextView) findViewById(R.id.total_width_pixels))	.setText( mScreen.realWidthPxText(this) );
        ((TextView) findViewById(R.id.total_height_pixels))	.setText( mScreen.realHeightPxText(this) );
        ((TextView) findViewById(R.id.width_pixels))		.setText( Integer.toString( mScreen.widthPx()) );
        ((TextView) findViewById(R.id.height_pixels))		.setText( Integer.toString( mScreen.heightPx()) );
        ((TextView) findViewById(R.id.width_dp))			.setText( Integer.toString( mScreen.widthDp()) );
        ((TextView) findViewById(R.id.height_dp))			.setText( Integer.toString( mScreen.heightDp()) );
        ((TextView) findViewById(R.id.smallest_dp))			.setText( Integer.toString( mScreen.smallestDp()) );
        ((TextView) findViewById(R.id.screen_dpi))			.setText( Integer.toString( mScreen.densityDpi()) );
        ((TextView) findViewById(R.id.actual_xdpi))			.setText( Float.toString  ( mScreen.xdpi()) );
        ((TextView) findViewById(R.id.actual_ydpi))			.setText( Float.toString  ( mScreen.ydpi()) );
        ((TextView) findViewById(R.id.logical_density))		.setText( Double.toString ( mScreen.density()) );
        ((TextView) findViewById(R.id.font_scale_density))	.setText( Float.toString  ( mScreen.scaledDensity()) );
    }

    /**
     * Calculate and display the physical diagonal size of the screen.
     * The size is calculated in inches, rounded to one place after decimal (e.g. '3.7', '10.1')
     * The size is also calculated in millimeters
     * 
     * @param metrics
     */
	private void showScreenDiagonalSize() {
        ((TextView) findViewById(R.id.computed_diagonal_size_inches))
        		.setText( Double.toString(mScreen.diagonalSizeInches()) );
        ((TextView) findViewById(R.id.computed_diagonal_size_mm))
        		.setText( Double.toString(mScreen.diagonalSizeMillimeters()) );
	}
	
	/**
	 * Display whether or not the device has a display that is longer or wider than normal.
	 */
	private void showScreenLongWide() {
        TextView longWideText = ((TextView) findViewById(R.id.long_wide));
        longWideText.setText( mScreen.screenLayoutText(this) );
	}

	/**
	 * Display the "natural" screen orientation of the device.
	 */
	private void showDefaultOrientation() {
        TextView orientationText = ((TextView) findViewById(R.id.natural_orientation));
        orientationText.setText( mScreen.defaultOrientationText(this) );
	}

	/**
	 * Display the current screen orientation of the device, with respect to natural orientation.
	 */
	private void showCurrentOrientation() {
		TextView orientationText = ((TextView) findViewById(R.id.current_orientation));
		orientationText.setText( mScreen.currentOrientationText( ) );
	}
	
	/**
	 * Display touchscreen properties
	 */
	private void showTouchScreen() {
        TextView touchScreenText = ((TextView) findViewById(R.id.touchscreen));
        touchScreenText.setText( mScreen.touchScreenText(this) );
	}
	
	/**
	 * Display pixel format
	 */
	private void showPixelFormat() {
        TextView pixelFormatText = ((TextView) findViewById(R.id.pixel_format));
        pixelFormatText.setText( mScreen.pixelFormatText(this) );
	}
	
	/**
	 * Display refresh rate
	 */
	private void showRefreshRate() {
        TextView refreshRateText = ((TextView) findViewById(R.id.refresh_rate));
		refreshRateText.setText(Float.toString(mScreen.refreshRate()));
	}
	
	//////////////////////////////////////////////////////////////////////////
	// About Dialog
	//////////////////////////////////////////////////////////////////////////
	
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

		switch (id) {
		case ABOUT_DIALOG:
			mAbout = new Dialog( this );
			mAbout.setContentView( R.layout.about_dialog );
			mAbout.setTitle( R.string.about_title );
			( (TextView) mAbout.findViewById( R.id.about_version ) )
					.setText( appVersion() );
			( (Button) mAbout.findViewById( R.id.about_dismiss ) )
					.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							mAbout.dismiss();
						}
					} );
			break;
		}

		return mAbout;
	}
	
	//////////////////////////////////////////////////////////////////////////
	// Share
	//////////////////////////////////////////////////////////////////////////

	/**
	 * Share summary report via email or whatever
	 */
	private void share() {
		String summaryString = mScreen.summaryText( this );
		final Intent shareIntent = new Intent( android.content.Intent.ACTION_SEND );
		shareIntent.setType( "text/plain" );
		shareIntent.putExtra( android.content.Intent.EXTRA_SUBJECT, 
				appendVersionToSubject( R.string.share_summary_subject ) );
		shareIntent.putExtra( android.content.Intent.EXTRA_TEXT, summaryString );

		startActivity( Intent.createChooser( shareIntent, getString( R.string.share_title ) ) );
	}

	/**
	 * Append the version number of the app to the Subject: string.
	 * 
	 * @param subjectResId
	 * @return
	 */
	private String appendVersionToSubject( int subjectResId ) {
		StringBuilder subjectLine = new StringBuilder();
		subjectLine.append( getString( subjectResId ) );
		subjectLine.append( " (" );
		subjectLine.append( appVersion() );
		subjectLine.append( ")" );
		return subjectLine.toString();
	}
	
	//////////////////////////////////////////////////////////////////////////
	// Menu
	//////////////////////////////////////////////////////////////////////////
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add( 0, MENU_ABOUT, 0, R.string.about_menu )
			.setIcon( android.R.drawable.ic_menu_info_details );
		menu.add( 0, MENU_SHARE, 0, R.string.share_menu )
			.setIcon( android.R.drawable.ic_menu_share );
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch( item.getItemId() ) {
		case MENU_ABOUT:
			showDialog(ABOUT_DIALOG);
			return true;
		case MENU_SHARE:
			share();
			return true;
		}
		
		return false;
	}
}