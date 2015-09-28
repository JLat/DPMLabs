/* 
 * OdometryCorrection.java
 */
package ev3Odometer;

import java.util.LinkedList;

import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.robotics.SampleProvider;

public class OdometryCorrection extends Thread {

	EV3ColorSensor colorSensor = new EV3ColorSensor(LocalEV3.get().getPort("S1"));
	SampleProvider colorRGBSensor = colorSensor.getRedMode();
	int sampleSize = colorRGBSensor.sampleSize();
	float[] sample = new float[sampleSize];
	private LinkedList<Float> recent = new LinkedList<Float>();

	private static final long CORRECTION_PERIOD = 10;
	private Odometer odometer;
	private int xLine, yLine, calibrationCounter;

	// Check to prevent multiple detections of the line, assert if the light
	// sensor is calibrated to the floor value.
	private boolean check, calibrated;

	// Represents the calibrated color value of the starting floor
	private double baseline, calibrationTemp;//

	// constructor
	public OdometryCorrection(Odometer odometer) {
		this.odometer = odometer;
		sample[0] = 0;
		check = true;
		baseline = 0.;
		calibrationCounter = 0;
		calibrated = false;
	}

	// run method (required for Thread)
	
	/*
	 * Assumption made that:
	 *^ x 
	 *|
	 *|
	 **------> Y
	 *  are the positive axes directions.
	 */

	public void run() {
		long correctionStart, correctionEnd;

		while (true) {

			correctionStart = System.currentTimeMillis();

			// Get value from light sensor
			colorRGBSensor.fetchSample(sample, 0);

			// If the baseline value has not yet been calibrated, do so.
			if (!calibrated) {
				if (calibrationCounter < 20) {
					// 20 data values for the floor have not yet been collected.
					// Add the recent readings to the temp value;
					calibrationTemp += sample[0] * 100;
					calibrationCounter++;
				} else {
					// we have successfully collected 20 samples of the wood
					// floor, the baseline value can now be set to the average
					// of those values, and the calibrated boolean is set to
					// true in order to avoid calibrating again.
					baseline = calibrationTemp / 20;
					calibrated = true;
				}
			}

			// If list isn't full add first 2 values
			if (recent.size() < 2)
				recent.addLast(sample[0] * 100);
			else {
				// the list is full, add an new reading to it and remove the
				// oldest reading.
				recent.removeFirst();
				recent.addLast(sample[0] * 100);
			}
			// TODO: -Fab:
			// I don't understand the purpose of this line, if we are 'in' the
			// line, it would allow check to be equal to true, since the recent
			// values would be close together.
			if (Math.abs(baseline - getAverage(recent)) < 5)
				check = true;

			// TODO: determine when crossing line while moving (5 is arbitrary
			// right now)

			// line detection condition
			if (Math.abs(baseline - getAverage(recent)) > 10 && baseline != 0. && check) {

				// Play system sound of beeping when line is detected
				LocalEV3.get().getAudio().systemSound(0);
				// Current angle of robot in degrees
				Double theta = odometer.getTheta() * 180 / Math.PI;
				check = false;
				// Facing X direction
				if (theta % 180 < 35 || theta % 180 > 145) {
					adjustX(theta);
				} else {
					// Facing Y direction
					adjustY(theta);
				}
			}

			// this ensure the odometry correction occurs only once every period
			correctionEnd = System.currentTimeMillis();
			if (correctionEnd - correctionStart < CORRECTION_PERIOD) {
				try {
					Thread.sleep(CORRECTION_PERIOD - (correctionEnd - correctionStart));
				} catch (InterruptedException e) {
					// there is nothing to be done here because it is not
					// expected that the odometry correction will be
					// interrupted by another thread
				}
			}
		}
	}

	public void adjustY(double theta) {
		// facing in y direction (positive y)
		if (theta % 360 < 180) { 
			yLine++;
			odometer.setY(yLine * 15 - 6);
		} else {
			yLine--;
			odometer.setY(yLine * 15 - 6);
		}
	}

	public void adjustX(double theta) {
		// facing in x direction (positive x)
		if (theta % 360 < 90 || theta % 360 > 270) { 
			xLine++;
			
			// TODO: -Fab: Why are you taking 6 off from X ? please provide a comment when it's not oubvious.
			
			
			odometer.setX(xLine * 15 - 6);
		} else {
			xLine--;
			odometer.setX(xLine * 15 - 6);
		}
	}

	// get method to allow access of light sensor reading by other classes
	public double getLight() {
		return (double) sample[0] * 100;
	}
	
	public double getBaseline(){
		return this.baseline;
	}

	public double getAverage(LinkedList<Float> list) {
		double result = 0;
		if (list.isEmpty()) {
			return 0;
		}
		for (Float i : list) {
			result += i;
		}
		return result / list.size();
	}
}