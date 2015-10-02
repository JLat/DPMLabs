package ev3Navigation;

import lejos.hardware.motor.EV3LargeRegulatedMotor;

public class Navigator extends Thread {

	private boolean navigating;
	private Odometer odometer;
	private EV3LargeRegulatedMotor rightMotor, leftMotor;
	private double wheelRadius, wheelTrack;

	// Default Constructor
	public Navigator(Odometer OD, EV3LargeRegulatedMotor leftMotor, EV3LargeRegulatedMotor rightMotor, double WR,
			double WS) {
		odometer = OD;
		this.leftMotor = leftMotor;
		this.rightMotor = rightMotor;
		wheelRadius = WR;
		wheelTrack = WS;

	}

	public void run() {
		
	}

	// Rotate robot to heading theta
	public void turnTo(double theta) {

		navigating = true;
		Double currentHeading = odometer.getTheta() % 360;

		// if the absolute value of the difference between our heading and theta
		// is less than 180, make that turn.
		if (Math.abs(theta - currentHeading) < 180)

			rotate(theta - currentHeading);
		else
			// if not, then turn the other way.
			rotate((theta-currentHeading) - 360);
	}

	// Rotate robot by angle theta
	public void rotate(double theta) {
		leftMotor.rotate(convertAngle(wheelRadius, wheelTrack, theta), true);
		rightMotor.rotate(-convertAngle(wheelRadius, wheelTrack, theta), false);
	}

	// Move robot forward by set distance
	public void move(double distance) {
		leftMotor.rotate(convertDistance(wheelRadius, distance), true);
		rightMotor.rotate(convertDistance(wheelRadius, distance), false);
	}

	// Move robot to location (x,y)
	public void travelTo(double x, double y) {
		Double dx, dy;
		navigating = true;
		dx = x - odometer.getX();
		dy = y - odometer.getY();
		// TODO: Fab- corrected this to dy/dx for tan (correct this if I'm wrong.
		turnTo(Math.tan(dy / dx));
		move(Math.sqrt((Math.pow(dx, 2) + Math.pow(dy, 2))));
	}

	public boolean isNavigating() {
		return navigating;
	}

	// When obstacle is detected move to avoid it
	public void avoidObstacle() {

	}

	// From Lab2 - Square Driver
	private static int convertDistance(double radius, double distance) {
		return (int) ((180.0 * distance) / (Math.PI * radius));
	}

	// TODO: Fab- What is this ?
	private static int convertAngle(double radius, double width, double angle) {
		
		return convertDistance(radius, Math.PI * width * angle / 360.0);
	}

}