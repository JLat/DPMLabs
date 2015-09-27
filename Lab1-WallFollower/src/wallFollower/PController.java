package wallFollower;

import java.util.ArrayList;
import java.util.LinkedList;

import lejos.hardware.motor.EV3LargeRegulatedMotor;

public class PController implements UltrasonicController {

	private final int bandCenter, bandwidth;
	private final int motorStraight = 200, FILTER_OUT = 20;
	private EV3LargeRegulatedMotor leftMotor, rightMotor;
	private int distance;
	private int filterControl;
	private LinkedList<Integer> recent = new LinkedList<Integer>();

	public PController(EV3LargeRegulatedMotor leftMotor, EV3LargeRegulatedMotor rightMotor, int bandCenter,
			int bandwidth) {
		// Default Constructor
		this.bandCenter = bandCenter;
		this.bandwidth = bandwidth;
		this.leftMotor = leftMotor;
		this.rightMotor = rightMotor;
		leftMotor.setSpeed(10); // Initalize motor rolling forward
		rightMotor.setSpeed(10);
		leftMotor.forward();
		rightMotor.forward();
		filterControl = 0;
	}

	@Override
	public void processUSData(int distance) {

		// rudimentary filter - toss out invalid samples corresponding to null
		// signal.
		// (n.b. this was not included in the Bang-bang controller, but easily
		// could have).
		//
		if (distance >= 255 && filterControl < FILTER_OUT) {
			// bad value, do not set the distance var, however do increment the
			// filter value
			filterControl++;
			// continue with past instruction if value is outside standard range
			
			return;
		} else if (distance >= 255) {
			// true 255, therefore set distance to 255
			this.distance = 255;
		} else {
			// distance went below 255, therefore reset everything.
			filterControl = 0;
			this.distance = distance;
		}

		/*
		 * Algorithm logic:
		 * 
		 * (processed distance) - (objective) = (delta)
		 * 
		 * wheel speed = base value - R * delta (R constant)
		 * 
		 */

		// the distance is constrained as to remove unpleasant values.
		distance = Math.min(100, distance);
		distance = Math.max(0, distance);

		// use a linked list of size recentListSize to store recent US readings.
		// Every time a new reading is received, it is added to the list, and
		// the oldest reading is removed.

		// list size, greater list size improves smoothness but reduces robot
		// responsiveness.
		int recentListSize = 10;

		int currentAverage = getAverage(recent);

		// conserving the "original" distance since we will tinker with the
		// distance value.
		int immediateDistance = distance;

		/*
		 * The US sensor seems unable to detect short distances when placed at
		 * an angle, causing many unwanted 255cm readings when right next to the
		 * wall. The distance value is limited to the current average +- an
		 * offset. This feature allows for smoothing out the irregular and
		 * "extreme" values while still allowing a change in the average over
		 * consistent readings.
		 * 
		 * note: the constant value added or removed to the currentAverage is
		 * based on experimentation, and might be greater for lower US values to
		 * allow faster response to walls than to open space.
		 * 
		 */
		distance = Math.min(currentAverage + 20, distance);
		distance = Math.max(distance, currentAverage - 20);

		// we add the processed distance to the recent values list.x
		this.recent.addLast(distance);

		// the size of the list is controlled.
		if (recent.size() > recentListSize) {
			recent.removeFirst();
		}
		if (recent.size() == recentListSize) {
			// if the list is full, we set the distance to be equal to the
			// average of the values in the list.
			// (the getAverage function is defined at the end of this page.)
			distance = getAverage(recent);
		} else {
			// the list is not yet full, the distance value remains the
			// processed value.
		}

		// we establish the delta variable to be the difference between the
		// distance value and the target value;
		int delta = distance - bandCenter;

		// setting the arbitrary ratio value;
		int ratio = 7;

		if (Math.abs(delta) <= bandwidth) {

			// the difference is within the acceptable margin, run straight

			rightMotor.setSpeed(motorStraight);
			leftMotor.setSpeed(motorStraight);

			rightMotor.forward();
			leftMotor.forward();

		} else if (delta >= 0) {
			// too far, turn left!

			// the left Motor is slowed proportionally to the difference between
			// the target distance and current average distance, with a minimum
			// value for its speed.
			rightMotor.setSpeed(motorStraight);
			leftMotor.setSpeed(Math.max(100, motorStraight - ratio * delta));

			rightMotor.forward();
			leftMotor.forward();

		} else {
			// delta < 0;

			// This multiplication allows the robot to remain proportional-type,
			// but to have a sharper response when too close to the wall
			// compared to
			// when it is too far.
			ratio *= 2;
			// too close, turn right!

			if (immediateDistance < 10) {
				// emmergency safety check, if the robot failed to remain at a
				// safe distance from the wall, move back.
				rightMotor.rotate(-50, false);
				rightMotor.flt();
				recent.clear();
			} else {
				// the robot is too close, slow one wheel down proportionally to
				// the delta value;
				rightMotor.setSpeed(Math.max(0, motorStraight - Math.abs(ratio * delta)));
				leftMotor.setSpeed(motorStraight);
				rightMotor.forward();
				leftMotor.forward();
			}

		}

	}

	@Override
	public int readUSDistance() {
		return this.distance;
	}

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
}
