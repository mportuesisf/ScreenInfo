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

import java.lang.reflect.Method;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

/**
 * Screen is a model object that summarizes information about the
 * device's display.
 * 
 * It unifies information from a few disparate Android APIs (Display,
 * DisplayMetrics, Configuration), and derives some additional device
 * statistics.  It also provides helpers to format data as strings
 * for display.
 * 
 */
public class Screen {
	
	public static final int UNSUPPORTED = -255;

	private Display mDisplay;
	private Configuration mConfig;
	
	private int mSizeClass;
	
	private int widthPx;
	private int heightPx;

	private int realWidthPx;
	private int realHeightPx;
	
	private int widthDp;
	private int heightDp;
	
	private int smallestDp;
	private int densityDpi;
	private float xdpi;
	private float ydpi;
	private double density;
	private float scaledDensity;

	private double physicalWidth;
	private double physicalHeight;
	private double diagonalSizeInches;
	private double diagonalSizeMillimeters;

    private int screenLayout;
    private int touchScreen;
    
    private int defaultOrientation;
    private String currentOrientation;
        
    private int pixelFormat;
    private float refreshRate;

	@SuppressLint("NewApi")
	public Screen( Context ctx ) {
		WindowManager wm = ((WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE));
		mDisplay = wm.getDefaultDisplay();
        mConfig = ctx.getResources().getConfiguration();
        
        // Screen Size classification
		mSizeClass = mConfig.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;

		// Usable Screen dimensions
		try {
			Method getSizeMethod = mDisplay.getClass().getMethod("getSize", Point.class);
			Point pt = new Point();
			getSizeMethod.invoke( mDisplay, pt );
			widthPx = pt.x;
			heightPx = pt.y;
		} catch (Exception ignore) {
			// Use older APIs
			widthPx = mDisplay.getWidth();
			heightPx = mDisplay.getHeight();
		}
		
		// Total (real) screen dimensions (as of Android 4.2, API 17)
		realWidthPx = UNSUPPORTED;
		realHeightPx = UNSUPPORTED;
		if ( Build.VERSION.SDK_INT >= 17 ) {
			DisplayMetrics metrics = new DisplayMetrics();
			wm.getDefaultDisplay().getRealMetrics(metrics);
			
			realWidthPx = metrics.widthPixels;
			realHeightPx = metrics.heightPixels;
		}

		// Screen sizes in device-independent pixels (dp) (as of API 13)
		DisplayMetrics metrics = new DisplayMetrics();
		mDisplay.getMetrics(metrics);
		heightDp = (int) (((double) ((realHeightPx == UNSUPPORTED) ? heightPx : realHeightPx) / metrics.density) + 0.5);

        widthDp = UNSUPPORTED;
        smallestDp = UNSUPPORTED;
        if ( Build.VERSION.SDK_INT >= 13 ) {
            widthDp = mConfig.screenWidthDp;
            smallestDp = mConfig.smallestScreenWidthDp;
        } else {
        	widthDp = (int) (((double) ((realWidthPx == UNSUPPORTED) ? widthPx : realWidthPx) / metrics.density) + 0.5);
        	smallestDp = ( widthDp <= heightDp ) ? widthDp : heightDp;
        }

		// Nominal DPI
		densityDpi = metrics.densityDpi;
		xdpi = metrics.xdpi;
		ydpi = metrics.ydpi;
		
		// Nominal screen density scaling factors
		density = metrics.density;
		scaledDensity = metrics.scaledDensity;
		
		// Normalize the xdpi/ydpi for the next set of calculations
		double xdpi = metrics.xdpi;
		if ( xdpi < 1.0 ) {
			// Guard against divide-by-zero. This is possible with lazy device manufacturers
			// who set these fields incorrectly. Set the density to our best guess.
			xdpi = metrics.densityDpi;
		}
		double ydpi = metrics.ydpi;
		if ( ydpi < 1.0 ) {
			ydpi =  metrics.densityDpi;
		}
		
		// Calculate physical screen width/height
		physicalWidth = ((double) metrics.widthPixels) / xdpi;
		physicalHeight = ((double) metrics.heightPixels) / ydpi;
		
		// Calculate diagonal screen size, in both U.S. and Metric units
		double rawDiagonalSizeInches = Math.sqrt(Math.pow(physicalWidth, 2) + Math.pow(physicalHeight, 2));
		diagonalSizeInches = Math.floor( rawDiagonalSizeInches * 10.0 + 0.5 ) / 10.0;
		diagonalSizeMillimeters = Math.floor( rawDiagonalSizeInches * 25.4 + 0.5 );
		
		// Long/wide
        screenLayout = mConfig.screenLayout & Configuration.SCREENLAYOUT_LONG_MASK;
        
        // Orientation
        defaultOrientation = mConfig.orientation;
        
        // Touchscreen type
        touchScreen = mConfig.touchscreen;

        // Current rotation
        determineCurrentRotation( ctx );
        
        // Pixel format (deprecated as of Android 4.2, API 17)
        pixelFormat = UNSUPPORTED;
        if ( Build.VERSION.SDK_INT < 17 ) {
        	pixelFormat = mDisplay.getPixelFormat();
        }
		
		// Refresh rate
        refreshRate = mDisplay.getRefreshRate();
	}
	
	/**
	 * Model name of device.
	 * @return
	 */
	public String deviceModel() {
		return Build.MODEL;
	}
	
	/**
	 * Version of Android (e.g. "2.3.5").
	 * 
	 * @return
	 */
	public String androidVersion() {
		return Build.VERSION.RELEASE;
	}
	
	/**
	 * Screen Size classification
	 */
	public int sizeClassification() {
		return mSizeClass;
	}
	
	/**
	 * Size classification, as displayable text
	 * 
	 * @param ctx
	 * @return
	 */
	public String sizeClassificationText( Context ctx ) {
		switch ( mSizeClass ) {
		case Configuration.SCREENLAYOUT_SIZE_SMALL:
			return "small";
		case Configuration.SCREENLAYOUT_SIZE_NORMAL:
			return "normal";
		case Configuration.SCREENLAYOUT_SIZE_LARGE:
			return "large";
		case Configuration.SCREENLAYOUT_SIZE_XLARGE:
			return "xlarge";
		case Configuration.SCREENLAYOUT_SIZE_UNDEFINED:
			return ctx.getString(R.string.undefined);
		}
		
		return ctx.getString(R.string.unknown);
	}
	
	/**
	 * Usable (application-accessible) Width of screen, in pixels
	 * 
	 * @return
	 */
	public int widthPx() {
		return widthPx;
	}

	/**
	 * Usable (application-accessible) Height of screen, in pixels
	 * 
	 * @return
	 */
	public int heightPx() {
		return heightPx;
	}
	
	/**
	 * Real width of screen, in pixels (all usable space, including system-reserved space).
	 * Returns UNSUPPORTED on systems with < API 17.
	 */
	public int realWidthPx() {
		return this.realWidthPx;
	}
	
	public String realWidthPxText( Context ctx ) {
		return this.realWidthPx == UNSUPPORTED ?
				ctx.getString( R.string.unsupported ) :
					Integer.toString( realWidthPx );
	}
	
	/**
	 * Real height of screen, in pixels (all usable space, including system-reserved space)
	 * Returns UNSUPPORTED on systems with < API 17.
	 */
	public int realHeightPx() {
		return this.realHeightPx;
	}
	
	public String realHeightPxText( Context ctx ) {
		return this.realHeightPx == UNSUPPORTED ?
				ctx.getString( R.string.unsupported ) :
					Integer.toString( realHeightPx );
	}

	/**
	 * Width of screen, in dp
	 * 
	 * @return
	 */
	public int widthDp() {
		return widthDp;
	}
	
	/**
	 * Height of screen, in dp
	 * 
	 * @return
	 */
	public int heightDp() {
		return heightDp;
	}
	
	/**
	 * Smallest screen dimension in dp, (smallestWidthDp used in layout classification)
	 * (see: http://android-developers.blogspot.com/2011/07/new-tools-for-managing-screen-sizes.html)
	 * 
	 * @return
	 */
	public int smallestDp() {
		return smallestDp;
	}
	
	/**
	 * Logical dpi, used for device classification
	 * 
	 * @return
	 */
	public int densityDpi() {
		return densityDpi;
	}
	
	/**
	 * Density classification, as text
	 */
	public String densityDpiText( Context ctx ) {
		switch ( densityDpi ) {
		case DisplayMetrics.DENSITY_TV:
			return "tvdpi";
		case DisplayMetrics.DENSITY_LOW:
			return "ldpi";
		case DisplayMetrics.DENSITY_MEDIUM:
			return "mdpi";
		case DisplayMetrics.DENSITY_HIGH:
			return "hdpi";
		case DisplayMetrics.DENSITY_XHIGH:
			return "xhdpi";
		case DisplayMetrics.DENSITY_XXHIGH:
			return "xxhdpi";
		case DisplayMetrics.DENSITY_XXXHIGH:
			return "xxxhdpi";
		}
		
		return ctx.getString(R.string.unknown);
	}
	
	/**
	 * Physical x dpi
	 * @return
	 */
	public float xdpi() {
		return xdpi;
	}
	
	/**
	 * Physical y dpi
	 * @return
	 */
	public float ydpi() {
		return ydpi;
	}

    /**
     * Density factor for px/dp conversions
     */
	public double density() {
		return density;
	}
	
	/**
	 * Scaling factor for fonts used on the display (DisplayMetrics.scaledDensity)
	 * @return
	 */
	public float scaledDensity() {
		return scaledDensity;
	}

    /**
     * Calculated physical width of the screen, in inches
     */
	public double physicalWidth() {
		return physicalWidth;
	}

    /**
     * Calculated physical height of the screen, in inches
     */
	public double physicalHeight() {
		return physicalHeight;
	}

    /**
     * Calculated diagonal size of the screen, in inches
     */
	public double diagonalSizeInches() {
		return diagonalSizeInches;
	}

    /**
     * Calculated diagonal size of the screen, in millimeters
     */
	public double diagonalSizeMillimeters() {
		return diagonalSizeMillimeters;
	}
	
	/**
	 * Screen layout, as integer
	 */
	public int screenLayout() {
		return screenLayout;
	}
	
	/**
	 * Screen layout, as text
	 */
	public String screenLayoutText( Context ctx ) {
		switch (screenLayout) {
        case Configuration.SCREENLAYOUT_LONG_YES:
        	return ctx.getString(R.string.yes);
        case Configuration.SCREENLAYOUT_LONG_NO:
        	return ctx.getString(R.string.no);
        case Configuration.SCREENLAYOUT_LONG_UNDEFINED:
        	return ctx.getString(R.string.undefined);
        }

		return ctx.getString(R.string.undefined);
	}
	
	/**
	 * Default, or "natural" screen orientation of the device.
	 */
	public int defaultOrientation() {
		return defaultOrientation;
	}
	
	/**
	 * Default orientation as text
	 */
	public String defaultOrientationText( Context ctx ) {
		return orientationText( ctx, defaultOrientation );
	}
	
	/**
	 * Current orientation as text
	 */
	public String currentOrientationText( ) {
		return currentOrientation;
	}
	
	/**
	 * Touchscreen properties
	 */
	public int touchScreen() {
		return touchScreen;
	}
	
	/**
	 * Touchscreen properties as text
	 */
	public String touchScreenText( Context ctx ) {
		switch ( touchScreen ) {
        case Configuration.TOUCHSCREEN_FINGER:
        	return ctx.getString(R.string.touchscreen_finger);
        case Configuration.TOUCHSCREEN_STYLUS:
        	return ctx.getString(R.string.touchscreen_stylus);        	
        case Configuration.TOUCHSCREEN_NOTOUCH:
        	return ctx.getString(R.string.touchscreen_none);
        case Configuration.TOUCHSCREEN_UNDEFINED:
        	return ctx.getString(R.string.undefined);
        }

    	return ctx.getString(R.string.undefined);
	}
	
	/**
	 * Pixel format
	 */
	public int pixelFormat() {
		return pixelFormat;
	}
	
	/**
	 * Pixel format as text
	 */
	public String pixelFormatText( Context ctx ) {
		switch ( pixelFormat ) {
		case PixelFormat.A_8:
			return "A_8";
		case ImageFormat.JPEG:
			return "JPEG";
		case PixelFormat.L_8:
			return "L_8";
		case PixelFormat.LA_88:
			return "LA_88";
		case PixelFormat.OPAQUE:
			return "OPAQUE";
		case PixelFormat.RGB_332:
			return "RGB_332";
		case PixelFormat.RGB_565:
			return "RGB_565";
		case PixelFormat.RGB_888:
			return "RGB_888";
		case PixelFormat.RGBA_4444:
			return "RGBA_4444";
		case PixelFormat.RGBA_5551:
			return "RGBA_5551";
		case PixelFormat.RGBA_8888:
			return "RGBA_8888";
		case PixelFormat.RGBX_8888:
			return "RGBX_8888";
		case 5:
			// Credit to Lawrence D'Oliveiro (https://github.com/ldo/screeninfo_android/)
			return "BGRA_8888"; /* see platform/system/core/include/system/graphics.h */
		case PixelFormat.TRANSLUCENT:
			return "TRANSLUCENT";
		case PixelFormat.TRANSPARENT:
			return "TRANSPARENT";
		case PixelFormat.UNKNOWN:
			return "UNKNOWN";
		case ImageFormat.NV21:
			return "NV21";
		case ImageFormat.YUY2:
			return "YUY2";
		case ImageFormat.NV16:
			return "NV16";
		case ImageFormat.YUV_420_888:
			return "YUY_420_888";
		case ImageFormat.RAW10:
			return "RAW10";
		case ImageFormat.YV12:
			return "YV12";
		case ImageFormat.RAW_SENSOR:
			return "RAW_SENSOR";
		case UNSUPPORTED:
			return ctx.getString( R.string.unsupported );
		default:
			return ctx.getString( R.string.unknown );
		}
	}
	
	/**
	 * Refresh rate
	 */
	public float refreshRate() {
		return refreshRate;
	}

	/**
	 * Return a string containing a text-based summary, suitable
	 * to share, email, save to SD card, etc.
	 * 
	 * @param ctx
	 * @return
	 */
	public String summaryText( Context ctx ) {
		SummaryTextBuilder sb = new SummaryTextBuilder(ctx);

		sb.addLine(R.string.device_label, 					deviceModel() )
		  .addLine( R.string.os_version_label,					androidVersion() )
		  .addLine( R.string.screen_class_label, 					sizeClassificationText(ctx) )
		  .addLine( R.string.density_class_label, 					densityDpiText(ctx) )
		  .addLine( R.string.total_width_pixels_label, 				realWidthPxText(ctx) )
		  .addLine( R.string.total_height_pixels_label, 				realHeightPxText(ctx) )
		  .addLine( R.string.width_pixels_label, 			widthPx() )
		  .addLine( R.string.height_pixels_label, 			heightPx() )
		  .addLine( R.string.width_dp_label, 						widthDp() )
		  .addLine( R.string.height_dp_label, 					heightDp() )
		  .addLine( R.string.smallest_dp_label, 					smallestDp() )
		  .addLine( R.string.long_wide_label, 					screenLayoutText(ctx) )
		  .addLine( R.string.natural_orientation_label, 			defaultOrientationText(ctx) )
		  .addLine( R.string.current_orientation_label, 			currentOrientationText() )
		  .addLine( R.string.touchscreen_label, 					touchScreenText(ctx) )
		  .addLine( R.string.screen_dpi_label, 						densityDpi() )
		  .addLine( R.string.actual_xdpi_label, 							xdpi() )
		  .addLine( R.string.actual_ydpi_label, 							ydpi() )
		  .addLine( R.string.logical_density_label, 				density() )
		  .addLine( R.string.font_scale_density_label, 				scaledDensity() )
		  .addLine( R.string.computed_diagonal_size_inches_label, 	diagonalSizeInches() )
		  .addLine( R.string.computed_diagonal_size_mm_label, 		diagonalSizeMillimeters() )
		  .addLine( R.string.pixel_format_label, 					pixelFormatText(ctx) )
		  .addLine( R.string.refresh_rate_label, 				refreshRate() )
		  .addNewLine()
		  .addLine( R.string.play_store_link );
		
		return sb.toString();
	}
	
	/**
	 * Helper class used to generate summary text report.
	 */
	private class SummaryTextBuilder {
		private StringBuilder sb;
		private Context ctx;
		
		public SummaryTextBuilder( Context ctx ) {
			sb = new StringBuilder();
			this.ctx = ctx;
		}
		
		public SummaryTextBuilder addLine( int resId ) {
			sb.append( ctx.getString( resId ) ).append( "\n" );
			return this;
		}
		
		public SummaryTextBuilder addLine(int resId, String value ) {
			sb.append( ctx.getString( resId ) ).append( " " ).append( value ).append( "\n" );
			return this;
		}
		
		public SummaryTextBuilder addLine( int resId, int value ) {
			return addLine( resId, Integer.toString(value) );
		}
		
		public SummaryTextBuilder addLine( int resId, float value ) {
			return addLine( resId, Float.toString(value) );
		}
		
		public SummaryTextBuilder addLine( int resId, double value ) {
			return addLine( resId, Double.toString(value) );
		}
		
		public SummaryTextBuilder addNewLine() {
			sb.append("\n");
			return this;
		}
		
		@Override
		public String toString() {
			return sb.toString();
		}
	}
	
	//////////////////////////////////////////////////////////////////////////
	// Private
	//////////////////////////////////////////////////////////////////////////
	
	/**
	 * Do the best job we can to find out which way the screen is currently rotated.
	 */
	private void determineCurrentRotation( Context ctx ) {
		// First, try the Display#getRotation() call, which was introduced in Froyo.
		// Reference: http://android-developers.blogspot.com/2010/09/one-screen-turn-deserves-another.html
		try {
			Method getRotationMethod = mDisplay.getClass().getMethod("getRotation");
			int rotation = (Integer) getRotationMethod.invoke(mDisplay);
			switch (rotation) {
			case Surface.ROTATION_0:
				currentOrientation = "0";
				break;
			case Surface.ROTATION_90:
				currentOrientation = "90";
				break;
			case Surface.ROTATION_180:
				currentOrientation = "180";
				break;
			case Surface.ROTATION_270:
				currentOrientation = "270";
				break;
			}
			return;
		}
		catch (Exception ignore) {
			;
		}
		
		// Fall back on the deprecated Display#getOrientation method from earlier releases of Android.
		int orientation = mDisplay.getOrientation();
		currentOrientation = orientationText( ctx, orientation );
	}
	
	/**
	 * Returns orientation text as string
	 */
	private String orientationText( Context ctx, int orientation ) {
		switch ( orientation ) {
        case Configuration.ORIENTATION_LANDSCAPE:
        	return ctx.getString(R.string.orientation_landscape);
        case Configuration.ORIENTATION_PORTRAIT:
        	return ctx.getString(R.string.orientation_portrait);
        case Configuration.ORIENTATION_SQUARE:
        	return ctx.getString(R.string.orientation_square);
        case Configuration.ORIENTATION_UNDEFINED:
        	return ctx.getString(R.string.undefined);
        }
		
    	return ctx.getString(R.string.undefined);
	}
	
}
