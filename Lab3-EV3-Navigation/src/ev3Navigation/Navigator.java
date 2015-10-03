package ev3Navigation;

import lejos.hardware.motor.EV3LargeRegulatedMotor;

public class Navigator extends Thread {

	private boolean navigating, obstacle;
	private Odometer odometer;
	private EV3LargeRegulatedMotor rightMotor, leftMotor;
	private double wheelRadius, wheelTrack;
	private double destX, destY, destTheta;
	private static final int FORWARD_SPEED = 250;
	private static final int ROTATE_SPEED = 150;
	
	// TODO: what values are supposed to go in your smoothsensor?
	private SmoothUSSensor USS = new SmoothUSSensor(recentListSize, PlusOffset, MinusOffset, UpperBound, LowerBound);

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
		if (navigating){
			
			
			//If we have reached our destination stop and stop navigating
			if (Math.abs(odometer.getX()-destX) < 1 && Math.abs(odometer.getY() -destY)< 1){
				rightMotor.flt();
				leftMotor.flt();
				navigating = false;
			}
			//Obstacle in the way stop and avoid obstacle
			else if (USS.getProcessedDistance() < 15 && !obstacle){
				rightMotor.flt();
				leftMotor.flt();
				obstacle = true;
			}
			
			
			
			//Currently avoiding an obstacle
			if (obstacle){
				//TODO: call class that will move around obstacle
				//use USS.getproccessedDistance() as the readings
				//Also call turnTo(destX,destY) when obstacle is "avoided"
			}
			
			//Drifted from the wrong heading, stop and readjust
			else if (Math.abs(destTheta - odometer.getTheta()) > 10){
				rightMotor.flt();
				leftMotor.flt();
				travelTo(destX,destY);
			}
			
			//Heading towards destination, proceed forward
			//Slow down as we approach destination
			else{
				//Calculate current distance to destination
				double distanceTo = Math.sqrt(Math.pow(destX - odometer.getX(), 2) +Math.pow(destY - odometer.getY(), 2));
					
				//Scale speed based on how close you are to destination (travel at reduced speed if less then 25cm from destination)
				rightMotor.setSpeed((int)(Math.min(FORWARD_SPEED,FORWARD_SPEED/2 + (distanceTo * 5))));
				leftMotor.setSpeed((int)(Math.min(FORWARD_SPEED,FORWARD_SPEED/2 + (distanceTo * 5))));
				rightMotor.forward();
				leftMotor.forward();	
			}
			
		}
		
		
		/*
		 * Algorithm idea: 
		 * 
		 * 
		 * 1) head to the target, move forward the required distance.
		 * 
		 * if at any time the robot detects that it is close to a wall:
		 * 
		 * 2)- begin a P-type wall-following method until the distance becomes
		 * great again (or simply that the obstacle is cleared)
		 * Note: Odometer is active while this happens.
		 * 
		 * 3) Use current X and Y to reposition itself to target and begin moving required distance.
		 * 
		 *  TODO: Fab- Tell me what you think of this approach.
		 * 
		 */
	}

	// Rotate robot to heading theta
	public void turnTo(double theta) {
		navigating = true;
		
		destTheta = theta;
		Double currentHeading = odometer.getTheta() % 360;
		
		// if the absolute value of the difference between our heading and theta
		// is less than 180, make that turn.
		if (Math.abs(theta - currentHeading) < 180)
			rotate(theta - currentHeading);
		// if not, then turn the other way.
		else
			rotate((theta - currentHeading) - 360);
	}

	// Rotate robot by angle theta
	public void rotate(double theta) {
		leftMotor.setSpeed(ROTATE_SPEED);
		rightMotor.setSpeed(ROTATE_SPEED);
		leftMotor.rotate(convertAngle(wheelRadius, wheelTrack, theta), true);
		rightMotor.rotate(-convertAngle(wheelRadius, wheelTrack, theta), false);
	}

	// Move robot to location (x,y)
	public void travelTo(double x, double y) {
		//Set Destination location
		Double dx, dy;
		destX = x;
		destY = y;
		
		//Start looking if arrived at destination, if obstacles approaches and tell other classes it is navigating
		navigating = true;
		
		//Change of location
		dx = x - odometer.getX();
		dy = y - odometer.getY();
		
		
		//Calculate angle to rotate to get to destination
		if (dx == 0 && dy > 0)
			turnTo(0);
		else if (dx == 0 && dy < 0)
			turnTo(180);
		else if (dy == 0 && dx > 0)
			turnTo(90);
		else if (dy == 0 && dx > 0)
			turnTo(270);
		else
			turnTo(Math.atan(dy / dx)*180/Math.PI);
		
		//Start moving forward
		leftMotor.forward();
		rightMotor.forward();
	}

	public boolean isNavigating() {
		return navigating;
	}

	// From Lab2 - Square Driver
	private static int convertDistance(double radius, double distance) {
		return (int) ((180.0 * distance) / (Math.PI * radius));
	}

	// TODO: Fab- What is this ? ANS: calculates how much to rotate to change theta by "angle"
	//From Lab2 - SquareDriver
	private static int convertAngle(double radius, double width, double angle) {
		return convertDistance(radius, Math.PI * width * angle / 360.0);
	}

}