package ev3Navigation;

import java.util.LinkedList;

import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.port.Port;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import lejos.hardware.sensor.SensorModes;
import lejos.robotics.SampleProvider;
/*
 * Copyright:
 * This class makes use of our own code from assignments 1 and 2 as well as parts of some demo code provided in these assignments.
 * 
 * TODO: Fab- Perhaps we could add methods in here to use the distance values and process the turning, but i'm not sure about that.
 * 
 * 
 * 
 */

public class SmoothUSSensor extends Thread {

	private static final Port usPort = LocalEV3.get().getPort("S1");
	private SampleProvider us;
	private float[] usData;

	// this class is to be a wrapper for the US sensor, effectively smoothing
	// out the values and allowing control of the smoothing from an exterior
	// class, as well as a mean of getting the "smooth" values

	// list containing the recent processed distance values.
	private LinkedList<Integer> recent;
	private int
		// original distance
		rawDistance,
		// altered distance
		processedDistance,
		// list size, greater list size improves smoothness but reduces
		// robot responsiveness.
		recentListSize,
		// previous average of the list before insertion of new data.
		previousAverage,
		// absolute bounds on the distance value, (set to 0-255) by default.
		upperBound = 255, lowerBound =0,
		//offset bound values from previousAverage to the next accept
		plusOffset, minusOffset;

	public SmoothUSSensor(int recentListSize, int PlusOffset, int MinusOffset, int UpperBound, int LowerBound) {

		this.recent = new LinkedList<Integer>();

		this.plusOffset = PlusOffset;
		this.minusOffset = MinusOffset;
		this.upperBound = UpperBound;
		this.lowerBound = LowerBound;

		@SuppressWarnings("resource")

		// usSensor is the instance
		SensorModes usSensor = new EV3UltrasonicSensor(usPort);

		SampleProvider usDistance = usSensor.getMode("Distance");
		// usDistance provides samples from this instance
		this.usData = new float[usDistance.sampleSize()];
		// usData is the buffer in which data are returned

	}

	public void run() {
		while (true) {
			// acquire data
			us.fetchSample(usData, 0);

			// extract from buffer, cast to int
			this.rawDistance = (int) (usData[0] * 100.0);
			
			
			processDistance();
			

			try {
				Thread.sleep(50);
			} catch (Exception e) {
			} // Poor man's timed sampling
		}
	}
	
	public void processDistance(){
		// the distance is constrained as to remove unpleasant values.
					// note: we use the rawDistance in the first call to min() in order
					// to use the rawDistance value, but not change it.
					processedDistance = Math.min(upperBound, rawDistance);
					processedDistance = Math.max(lowerBound, processedDistance);

					// use a linked list of size recentListSize to store recent US
					// readings. Every time a new reading is received, it is added to
					// the list, and the oldest reading is removed.

					previousAverage = getAverage(recent);

					// conserving the "original" distance since we will tinker with the
					// distance value.

					/*
					 * The US sensor seems unable to detect short distances when placed
					 * at an angle, causing many unwanted 255cm readings when right next
					 * to the wall. The distance value is limited to the current average
					 * +- an offset. This feature allows for smoothing out the irregular
					 * and "extreme" values while still allowing a change in the average
					 * over consistent readings.
					 * 
					 * note: the constant value added or removed to the currentAverage
					 * is based on experimentation, and might be greater for lower US
					 * values to allow faster response to walls than to open space.
					 * 
					 */
					processedDistance = Math.min(previousAverage + plusOffset, processedDistance);
					processedDistance = Math.max(processedDistance, previousAverage - minusOffset);

					// we add the processed distance to the recent values list.
					this.recent.addLast(processedDistance);

					// the size of the list is controlled.
					if (recent.size() > recentListSize) {
						recent.removeFirst();
					}
					if (recent.size() == recentListSize) {
						// if the list is full, we set the distance to be equal to the
						// average of the values in the list.
						// (the getAverage function is defined at the end of this page.)
						this.processedDistance = getAverage(recent);
					} else {
						// the list is not yet full, the distance value remains the
						// processed value.

					}
	}

	// simple method to get the average of a Linked list of integers.
	public int getAverage(LinkedList<Integer> list) {
		int result = 0;
		if (list.isEmpty()) {
			return 0;
		}
		for (Integer i : list) {
			result += i;
		}
		return result / list.size();
	}

	public int getProcessedDistance() {
		return this.processedDistance;
	}

	public int getRawDistance() {
		return this.rawDistance;
	}

}
