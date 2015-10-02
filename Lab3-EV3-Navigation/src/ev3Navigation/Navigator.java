package ev3Navigation;

import lejos.hardware.motor.EV3LargeRegulatedMotor;

public class Navigator extends Thread {

	private boolean navigating;
	private Odometer odometer;
	private EV3LargeRegulatedMotor rightMotor, leftMotor;
	private double wheelRadius, wheelTrack;


//Default Constructor
public Navigator (Odometer OD,EV3LargeRegulatedMotor leftMotor, EV3LargeRegulatedMotor rightMotor, double WR, double WS){
	odometer = OD;
	this.leftMotor = leftMotor;
	this.rightMotor = rightMotor;
	wheelRadius = WR;
	wheelTrack = WS;
	
}
	public void run(){
		// Not really sure what should be recurring but they said to implement thread so somethings needs to be done
	}

	// Rotate robot to heading theta
	public void turnTo(double theta) {
		navigating = true;
		Double currentHeading = odometer.getTheta() % 360;
		if (Math.abs(currentHeading - theta) < 180)
			rotate(theta - currentHeading);
		else
			rotate(currentHeading - (theta + 360));
	}

	// Rotate robot by angle theta
	public void rotate(double theta) {
		leftMotor.rotate(convertAngle(wheelRadius, wheelTrack, theta), true);
		rightMotor.rotate(-convertAngle(wheelRadius, wheelTrack, theta), false);
	}

	// Move robot forward by set distance
	public void move(double distance) {
		leftMotor.rotate(convertDistance(wheelRadius, distance), true);
		rightMotor.rotate(convertDistance(wheelRadius, distance), true);
	}

	// Move robot to location (x,y)
	public void travelTo(double x, double y) {
		Double dx, dy;
		navigating = true;
		dx = x - odometer.getX();
		dy = y - odometer.getY();
		turnTo(Math.tan(dx / dy));
		move(Math.sqrt((Math.pow(dx,2)+ Math.pow(dy,2))));
	}

	public boolean isNavigating() {
		return navigating;
	}

	// When obstacle is detected move to avoid it
	public void avoidObstacle() {

	}
	
	//From Lab2 - Square Driver
	private static int convertDistance(double radius, double distance) {
		return (int) ((180.0 * distance) / (Math.PI * radius));
	}

	private static int convertAngle(double radius, double width, double angle) {
		return convertDistance(radius, Math.PI * width * angle / 360.0);
	}

}