/*
 * Odometer.java
 */

package ev3Odometer;

import lejos.hardware.motor.EV3LargeRegulatedMotor;

public class Odometer extends Thread {
	// robot position
	private double x, y, theta;
	private int lastTachoL, lastTachoR, nowTachoL, nowTachoR;
	private EV3LargeRegulatedMotor rightMotor, leftMotor;
	private double WR, WS;

	// odometer update period, in ms
	private static final long ODOMETER_PERIOD = 25;

	// lock object for mutual exclusion
	private Object lock;

	// default constructor

	public Odometer() {
		x = 0.0;
		y = 0.0;
		theta = 0.0;
		lastTachoL = 0;
		lastTachoR = 0;
		lock = new Object();
		rightMotor.resetTachoCount();
		leftMotor.resetTachoCount();
	}

	// Constructor with motors
	public Odometer(EV3LargeRegulatedMotor leftMotor, EV3LargeRegulatedMotor rightMotor, double WR, double WS) {
		this.leftMotor = leftMotor;
		this.rightMotor = rightMotor;
		this.WR = WR;
		this.WS = WS;
		x = 0.0;
		y = 0.0;
		theta = 0.0;
		lastTachoL = 0;
		lastTachoR = 0;
		lock = new Object();
		rightMotor.resetTachoCount();
		leftMotor.resetTachoCount();
		
		
	}

	// run method (required for Thread)
	public void run() {
		long updateStart, updateEnd;

		updateStart = System.currentTimeMillis();

		while (true) {
			updateStart = System.currentTimeMillis();

			double deltaL, deltaR, deltaD;

			// Current facing = theta = 0
			// get new tachometer readings
			nowTachoL = leftMotor.getTachoCount();
			nowTachoR = rightMotor.getTachoCount();
			// calculate distance traveled for left and right wheel
			deltaR = WR * (nowTachoR - lastTachoR) * Math.PI / 180;
			deltaL = WR * (nowTachoL - lastTachoL) * Math.PI / 180;
			deltaD = (deltaR + deltaL) / 2; // Calculate total distance moved
			lastTachoL = nowTachoL; // update holder for last tachometer
									// readings
			lastTachoR = nowTachoR;

			synchronized (lock) {
				// don't use the variables x, y, or theta anywhere but here!
				theta += (deltaL - deltaR) / WS; // update Theta, x and y
													// according to measured
													// changes
				x += deltaD * Math.sin(theta);
				y += deltaD * Math.cos(theta);
			}

			// this ensures that the odometer only runs once every period
			updateEnd = System.currentTimeMillis();
			if (updateEnd - updateStart < ODOMETER_PERIOD) {
				try {
					Thread.sleep(ODOMETER_PERIOD - (updateEnd - updateStart));
				} catch (InterruptedException e) {
					// there is nothing to be done here because it is not
					// expected that the odometer will be interrupted by
					// another thread
				}
			}
		}
	}

	// accessors
	public void getPosition(double[] position, boolean[] update) {
		// ensure that the values don't change while the odometer is running
		synchronized (lock) {
			if (update[0])
				position[0] = x;
			if (update[1])
				position[1] = y;
			if (update[2])
				position[2] = theta / Math.PI * 180;// Return the value in
													// degrees
		}
	}

	public double getX() {
		double result;

		synchronized (lock) {
			result = x;
		}

		return result;
	}

	public double getY() {
		double result;

		synchronized (lock) {
			result = y;
		}

		return result;
	}

	public double getTheta() {
		double result;

		synchronized (lock) {
			result = theta;
		}

		return result;
	}

	// mutators
	public void setPosition(double[] position, boolean[] update) {
		// ensure that the values don't change while the odometer is running
		synchronized (lock) {
			if (update[0])
				x = position[0];
			if (update[1])
				y = position[1];
			if (update[2])
				theta = position[2];
		}
	}

	public void setX(double x) {
		synchronized (lock) {
			this.x = x;
		}
	}

	public void setY(double y) {
		synchronized (lock) {
			this.y = y;
		}
	}

	public void setTheta(double theta) {
		synchronized (lock) {
			this.theta = theta;
		}
	}
}