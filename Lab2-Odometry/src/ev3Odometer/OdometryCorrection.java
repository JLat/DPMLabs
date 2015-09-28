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
	SampleProvider colorRGBSensor = colorSensor.getRedMode();
	int sampleSize = colorRGBSensor.sampleSize();
	float[] sample = new float[sampleSize];
	private LinkedList<Float> recent = new LinkedList<Float>();

	private static final long CORRECTION_PERIOD = 10;
	private Odometer odometer;
	private int xLine, yLine;

	// constructor
	public OdometryCorrection(Odometer odometer) {
		this.odometer = odometer;
		sample[0] = 0;
	}

	// run method (required for Thread)

	/*

	 * Assumption made that:
	 * 		  ^ x 
	 * 		  |
	 * 		  |
	 * -y <----* 
	 *  is positive
	 */
	public void run() {
		long correctionStart, correctionEnd;

		while (true) {

			correctionStart = System.currentTimeMillis();
			
			
			colorRGBSensor.fetchSample(sample, 0);// Get value from light sensor
			if (recent.size() <3) //If list isn't full add first 3 values
				recent.addLast(sample[0]*100);
			else{ // Next values to be added are the difference between the past value
				recent.removeFirst();
				recent.addLast(recent.getLast() - sample[0]*100);
			}
			// We construct recent list of size 5.
				// TODO: Fab:
				// Why are we adding the sum of three values? why not smooth out
				// the values instead ?
				// we add to the recent list the sum of three consecutive values

			// put your correction code here
			if (getAverage(recent)>5){ //Line detected
				LocalEV3.get().getAudio().systemSound(0); // Play system sound of beeping when line detected
				
				
				// Check which line is closer from current position ****FIX***** - Probably should use angle to determine which variable to change
				if (Math.abs(odometer.getX() % 15) < Math.abs(odometer.getY()%15)){ //Closer to line in X
					adjustX(odometer.getTheta()/Math.PI*180);
				}
				else{// Closer to line in Y
					adjustY(odometer.getTheta()/Math.PI*180);
				}
			}
			// TODO: Fab:
			// How about we use the average of the first X readings (eg. the
			// first second of movement) to set the "brown wood" light value,
			// then use an offset constant to get the "black line" reading ?
			// Also I am uncertain that using the sum of three values would be
			// good in this case, given the fact that the "null value" in this
			// case is 0.01

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

	public void adjustY(double theta ){
		if (theta % 360 < 180 ){ // facing in y direction (positive y)
			yLine ++;
			odometer.setY(yLine*15);
		}
		else{
			yLine --;
			odometer.setY(yLine*15);
		}	
	}
	public void adjustX(double theta ){
		if (theta % 360 < 90 || theta % 360 > 270){ // facing in x direction (positive x)
			xLine ++;
			odometer.setX(xLine*15);
		}
		else{
			xLine --;
			odometer.setX(xLine*15);
		}
	}
	// get method to allow access of light sensor reading by other classes
		public double getLight(){
			return (double)sample[0];
		}

		// Calculate average of entries of the list
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