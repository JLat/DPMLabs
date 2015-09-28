/* 
 * OdometryCorrection.java
 */
package ev3Odometer;

import java.util.LinkedList;

import lejos.hardware.Audio;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.robotics.SampleProvider;

public class OdometryCorrection extends Thread {

	EV3ColorSensor colorSensor = new EV3ColorSensor(LocalEV3.get().getPort("S1"));
	SampleProvider colorRGBSensor = colorSensor.getRGBMode();
	int sampleSize = colorRGBSensor.sampleSize();
	float[] sample = new float[sampleSize];
	private LinkedList<Float> recent = new LinkedList<Float>();

	private static final long CORRECTION_PERIOD = 10;
	private Odometer odometer;
	private int xLine, yLine;

	// constructor
	public OdometryCorrection(Odometer odometer) {
		this.odometer = odometer;
	}

	// run method (required for Thread)

	/*
	 * Assumption made that: ^ y | | x <----* is positive
	 */
	public void run() {
		long correctionStart, correctionEnd;

		while (true) {

			correctionStart = System.currentTimeMillis();

			colorRGBSensor.fetchSample(sample, 0);

			// We construct recent list of size 5.
			if (recent.size() < 5)
				// TODO: Fab:
				// Why are we adding the sum of three values? why not smooth out
				// the values instead ?

				// we add to the recent list the sum of three consecutive values
				recent.addLast(sample[0] + sample[1] + sample[2]);

			else {

				recent.removeFirst();

				recent.addLast(sample[0] + sample[1] + sample[2]);
			}

			// TODO: Fab:
			// How about we use the average of the first X readings (eg. the
			// first second of movement) to set the "brown wood" light value,
			// then use an offset constant to get the "black line" reading ?
			// Also I am uncertain that using the sum of three values would be
			// good in this case, given the fact that the "null value" in this
			// case is 0.01

			// Line detected
			if (getAverage(recent) < 30) {

				LocalEV3.get().getAudio().systemSound(0);
				// Closer to line in X
				if (Math.abs(odometer.getX() % 15) < Math.abs(odometer.getY() % 15)) {
					adjustX(odometer.getTheta() / Math.PI * 180);
				} else {
					adjustY(odometer.getTheta() / Math.PI * 180);
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

	public void adjustX(double theta) {
		// facing in x direction
		if (theta % 360 > 180) {
			xLine++;
			odometer.setX(xLine * 15);
		} else {
			xLine--;
			odometer.setX(xLine * 15);
		}

	}

	public void adjustY(double theta) {
		// facing in y direction
		if (theta % 360 < 90 || theta % 360 > 270) {
			yLine++;
			odometer.setX(yLine * 15);
		} else {
			yLine--;
			odometer.setX(yLine * 15);
		}
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