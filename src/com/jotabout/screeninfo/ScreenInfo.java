package com.jotabout.screeninfo;

/**
 * ScreenInfo
 * 
 * A simple app to display the screen configuration parameters for an
 * Android device.
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
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
    	showScreenDiagonalSize();
    	showScreenLongWide();
        showDefaultOrientation();
        showCurrentOrientation();
        showTouchScreen();
        showPixelFormat();
        showRefreshRate();
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
		
		int widthPx;
		int heightPx;
		try {
			// Try to get size without the Status bar, if we can (API level 13)
			Method getSizeMethod = display.getClass().getMethod("getSize", Point.class);
			Point pt = new Point();
			getSizeMethod.invoke( display, pt );
			widthPx = pt.x;
			heightPx = pt.y;
		} catch (Exception ignore) {
			// Use older APIs
			widthPx = display.getWidth();
			heightPx = display.getHeight();
		}
    	
        ((TextView) findViewById(R.id.width_pixels)).setText( Integer.toString(widthPx) );
        ((TextView) findViewById(R.id.height_pixels)).setText( Integer.toString(heightPx) );
        
		DisplayMetrics metrics = new DisplayMetrics();
		display.getMetrics(metrics);
		
		// Calculate screen sizes in device-independent pixels (dp)
		int widthDp = (int) (((double) widthPx / metrics.density) + 0.5);
		int heightDp = (int) (((double) heightPx / metrics.density) + 0.5);
		int smallestDp = widthDp > heightDp ? heightDp : widthDp;

        ((TextView) findViewById(R.id.width_dp)).setText( Integer.toString(widthDp) );
        ((TextView) findViewById(R.id.height_dp)).setText( Integer.toString(heightDp) );
        ((TextView) findViewById(R.id.smallest_dp)).setText( Integer.toString(smallestDp) );

        ((TextView) findViewById(R.id.screen_dpi)).setText( Integer.toString(metrics.densityDpi) );
        ((TextView) findViewById(R.id.actual_xdpi)).setText( Float.toString(metrics.xdpi) );
        ((TextView) findViewById(R.id.actual_ydpi)).setText( Float.toString(metrics.ydpi) );
        ((TextView) findViewById(R.id.logical_density)).setText( Double.toString(metrics.density) );
        ((TextView) findViewById(R.id.font_scale_density)).setText( Float.toString(metrics.scaledDensity) );
    }

    /**
     * Calculate and display the physical diagonal size of the screen.
     * The size is calculated in inches, rounded to one place after decimal (e.g. '3.7', '10.1')
     * The size is also calculated in millimeters
     * 
     * @param metrics
     */
	private void showScreenDiagonalSize() {
		WindowManager wm = ((WindowManager) getSystemService(Context.WINDOW_SERVICE));
		Display display = wm.getDefaultDisplay();
 		DisplayMetrics metrics = new DisplayMetrics();
		display.getMetrics(metrics);
		
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
	 * Display whether or not the device has a display that is longer or wider than normal.
	 */
	private void showScreenLongWide() {
        TextView longWideText = ((TextView) findViewById(R.id.long_wide));
        Configuration config = getResources().getConfiguration();
        
        int screenLayout = config.screenLayout & Configuration.SCREENLAYOUT_LONG_MASK;
        switch (screenLayout) {
        case Configuration.SCREENLAYOUT_LONG_YES:
        	longWideText.setText(R.string.yes);
        	break;
        case Configuration.SCREENLAYOUT_LONG_NO:
        	longWideText.setText(R.string.no);
        	break;
        case Configuration.SCREENLAYOUT_LONG_UNDEFINED:
        	longWideText.setText(R.string.undefined);
        	break;
        }
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
		catch (Exception ignore) {;}
		
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
        	orientationText.setText(R.string.undefined);
        	break;
        }
	}
	
	/**
	 * Display touchscreen properties
	 */
	private void showTouchScreen() {
        TextView touchScreenText = ((TextView) findViewById(R.id.touchscreen));
        Configuration config = getResources().getConfiguration();
        
        switch (config.touchscreen ) {
        case Configuration.TOUCHSCREEN_FINGER:
        	touchScreenText.setText(R.string.touchscreen_finger);
        	break;
        case Configuration.TOUCHSCREEN_STYLUS:
        	touchScreenText.setText(R.string.touchscreen_stylus);        	
        	break;
        case Configuration.TOUCHSCREEN_NOTOUCH:
        	touchScreenText.setText(R.string.touchscreen_none);
        	break;
        case Configuration.TOUCHSCREEN_UNDEFINED:
        	touchScreenText.setText(R.string.undefined);
        	break;
        }
	}
	
	/**
	 * Display pixel format
	 */
	private void showPixelFormat() {
        TextView pixelFormatText = ((TextView) findViewById(R.id.pixel_format));
		WindowManager wm = ((WindowManager) getSystemService(Context.WINDOW_SERVICE));
		Display display = wm.getDefaultDisplay();
		
		int format = display.getPixelFormat();
		
		switch ( format ) {
		case PixelFormat.A_8:
			pixelFormatText.setText("A_8");
			break;
		case ImageFormat.JPEG:
			pixelFormatText.setText("JPEG");
			break;
		case PixelFormat.L_8:
			pixelFormatText.setText("L_8");
			break;			
		case PixelFormat.LA_88:
			pixelFormatText.setText("LA_88");
			break;			
		case PixelFormat.OPAQUE:
			pixelFormatText.setText("OPAQUE");
			break;			
		case PixelFormat.RGB_332:
			pixelFormatText.setText("RGB_332");
			break;			
		case PixelFormat.RGB_565:
			pixelFormatText.setText("RGB_565");
			break;			
		case PixelFormat.RGB_888:
			pixelFormatText.setText("RGB_888");
			break;			
		case PixelFormat.RGBA_4444:
			pixelFormatText.setText("RGBA_4444");
			break;			
		case PixelFormat.RGBA_5551:
			pixelFormatText.setText("RGBA_5551");
			break;
		case PixelFormat.RGBA_8888:
			pixelFormatText.setText("RGBA_8888");
			break;			
		case PixelFormat.RGBX_8888:
			pixelFormatText.setText("RGBX_8888");
			break;			
		case PixelFormat.TRANSLUCENT:
			pixelFormatText.setText("TRANSLUCENT");
			break;			
		case PixelFormat.TRANSPARENT:
			pixelFormatText.setText("TRANSPARENT");
			break;			
		case PixelFormat.UNKNOWN:
			pixelFormatText.setText("UNKNOWN");
			break;			
		case ImageFormat.NV21:
			pixelFormatText.setText("NV21");
			break;			
		case ImageFormat.YUY2:
			pixelFormatText.setText("YUY2");
			break;			
		case ImageFormat.NV16:
			pixelFormatText.setText("NV16");
			break;
		default:
			pixelFormatText.setText(R.string.unknown);
		}
	}
	
	/**
	 * Display refresh rate
	 */
	private void showRefreshRate() {
        TextView refreshRateText = ((TextView) findViewById(R.id.refresh_rate));
		WindowManager wm = ((WindowManager) getSystemService(Context.WINDOW_SERVICE));
		Display display = wm.getDefaultDisplay();
		
		refreshRateText.setText(Float.toString(display.getRefreshRate()));
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