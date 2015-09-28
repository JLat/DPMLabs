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
	private int xLine, yLine;
	private boolean check; // Check to prevent multiple detections of the line
	private double baseline;// Represents the color of the starting floor

	// constructor
	public OdometryCorrection(Odometer odometer) {
		this.odometer = odometer;
		sample[0] = 0;
		check = true;
		baseline = 0.;
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
			if (recent.size() <2) //If list isn't full add first 5 values
				recent.addLast(sample[0]*100);
			else{ // Next values to be added are the difference between the past value
				if (baseline ==0.)// If baseline has yet to be set do so
					baseline = getAverage(recent);
				recent.removeFirst();
				recent.addLast(sample[0]*100);
			}
			if (Math.abs(baseline - getAverage(recent)) < 2)
				check = true;
			
			// put your correction code here
			
			//TODO: determine when crossing line while moving (5 is arbitrary right now)
			
			if (Math.abs(baseline - getAverage(recent)) > 5 && baseline != 0. && check){ //Line detected
				LocalEV3.get().getAudio().systemSound(0); // Play system sound of beeping when line detected
				Double theta = odometer.getTheta() * 180 / Math.PI; // Current angle of robot in degrees
				check = false;
					if (theta % 180 < 35 || theta % 180 > 145){ //Facing X direction
						adjustX(theta);
					}
					else{// Facing X direction 
						adjustY(theta);
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
			odometer.setY(yLine*15 - 6);
		}
		else{
			yLine --;
			odometer.setY(yLine*15 - 6);
		}	
	}
	public void adjustX(double theta ){
		if (theta % 360 < 90 || theta % 360 > 270){ // facing in x direction (positive x)
			xLine ++;
			odometer.setX(xLine*15 - 6);
		}
		else{
			xLine --;
			odometer.setX(xLine*15 - 6);
		}
	}
	// get method to allow access of light sensor reading by other classes
	public double getLight(){
		return (double)sample[0] * 100;
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