/*
 * OdometryDisplay.java
 */

package ev3Odometer;

import lejos.hardware.lcd.TextLCD;

public class OdometryDisplay extends Thread {
	private static final long DISPLAY_PERIOD = 250;
	private Odometer odometer;
	private OdometryCorrection OC;
	private TextLCD t;

	// constructor
	public OdometryDisplay(Odometer odometer, TextLCD t, OdometryCorrection OC) {
		this.odometer = odometer;
		this.t = t;
		this.OC = OC;
	}

	// run method (required for Thread)
	public void run() {
		long displayStart, displayEnd;
		double[] position = new double[3];

		// clear the display once
		t.clear();

		while (true) {
			displayStart = System.currentTimeMillis();

			// clear the lines for displaying odometry information
			t.drawString("X:       ", 0, 0);
			t.drawString("Y:       ", 0, 1);
			t.drawString("T:       ", 0, 2);
			
			//Added light sensor to the display
			t.drawString("Light:          ", 0, 3);
			t.drawString(formattedDoubleToString(OC.getLight() ,2), 7, 3);
			
			t.drawString("baseline:    ", 0, 4);
			t.drawString(formattedDoubleToString(OC.getBaseline(), 2), 7, 4);
			
			t.drawString(OC.getXline()+"", 10, 0);
			t.drawString(OC.getYline()+"", 10, 1);
			
			
			
			// get the odometry information
			odometer.getPosition(position, new boolean[] { true, true, true });

			// display odometry information
			for (int i = 0; i < 3; i++) {
				t.drawString(formattedDoubleToString(position[i], 2), 3, i);
			}

			// throttle the OdometryDisplay
			displayEnd = System.currentTimeMillis();
			if (displayEnd - displayStart < DISPLAY_PERIOD) {
				try {
					Thread.sleep(DISPLAY_PERIOD - (displayEnd - displayStart));
				} catch (InterruptedException e) {
					// there is nothing to be done here because it is not
					// expected that OdometryDisplay will be interrupted
					// by another thread
				}
			}
		}
	}
	
	private static String formattedDoubleToString(double x, int places) {
		String result = "";
		String stack = "";
		long t;
		
		// put in a minus sign as needed
		if (x < 0.0)
			result += "-";
		
		// put in a leading 0
		if (-1.0 < x && x < 1.0)
			result += "0";
		else {
			t = (long)x;
			if (t < 0)
				t = -t;
			
			while (t > 0) {
				stack = Long.toString(t % 10) + stack;
				t /= 10;
			}
			
			result += stack;
		}
		
		// put the decimal, if needed
		if (places > 0) {
			result += ".";
		
			// put the appropriate number of decimals
			for (int i = 0; i < places; i++) {
				x = Math.abs(x);
				x = x - Math.floor(x);
				x *= 10.0;
				result += Long.toString((long)x);
			}
		}
		
		return result;
	}

}
