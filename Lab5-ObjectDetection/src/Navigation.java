
/*
 * File: Navigation.java
 * Written by: Sean Lawlor
 * ECSE 211 - Design Principles and Methods, Head TA
 * Fall 2011
 * Ported to EV3 by: Francois Ouellet Delorme
 * Fall 2015
 * 
 * Movement control class (turnTo, travelTo, flt, localize)
 */
import java.util.ArrayList;

import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.lcd.LCD;
import lejos.hardware.motor.EV3LargeRegulatedMotor;

public class Navigation {
	final static int FAST = 100, SLOW = 60, ACCELERATION = 2000;
	//TODO: the USS_SENSOR_OFFSET value is closer to 12 in real life.
	final static double DEG_ERR = 3, CM_ERR = 1.0, USS_SENSOR_OFFSET = 8.8;
	private Odometer odometer;
	private Scanner scanner;
	private LCDInfo LCD;
	private EV3LargeRegulatedMotor leftMotor, rightMotor;
	public Claw claw = new Claw();
	private ArrayList<Integer> searched= new ArrayList<Integer>();

	public Navigation(Odometer odo) {
		this.odometer = odo;
		EV3LargeRegulatedMotor[] motors = this.odometer.getMotors();
		this.leftMotor = motors[0];
		this.rightMotor = motors[1];

		// set acceleration
		this.leftMotor.setAcceleration(ACCELERATION);
		this.rightMotor.setAcceleration(ACCELERATION);
	}

	public Navigation(Odometer odo, Scanner scan, LCDInfo lcd) {
		this.odometer = odo;
		this.scanner = scan;
		this.LCD = lcd;
		EV3LargeRegulatedMotor[] motors = this.odometer.getMotors();
		this.leftMotor = motors[0];
		this.rightMotor = motors[1];

		// set acceleration
		this.leftMotor.setAcceleration(ACCELERATION);
		this.rightMotor.setAcceleration(ACCELERATION);
	}

	/*
	 * Functions to set the motor speeds jointly
	 */
	public void setSpeeds(float lSpd, float rSpd) {
		this.leftMotor.setSpeed(lSpd);
		this.rightMotor.setSpeed(rSpd);
		if (lSpd < 0)
			this.leftMotor.backward();
		else
			this.leftMotor.forward();
		if (rSpd < 0)
			this.rightMotor.backward();
		else
			this.rightMotor.forward();
	}

	public void setSpeeds(int lSpd, int rSpd) {
		this.leftMotor.setSpeed(lSpd);
		this.rightMotor.setSpeed(rSpd);
		if (lSpd < 0)
			this.leftMotor.backward();
		else
			this.leftMotor.forward();
		if (rSpd < 0)
			this.rightMotor.backward();
		else
			this.rightMotor.forward();
	}

	/*
	 * Float the two motors jointly
	 */
	public void setFloat() {
		this.leftMotor.stop();
		this.rightMotor.stop();
		this.leftMotor.flt(true);
		this.rightMotor.flt(true);
	}

	/*
	 * TravelTo function which takes as arguments the x and y position in cm
	 * Will travel to designated position, while constantly updating it's
	 * heading
	 */
	public void travelTo(double x, double y) {
		double minAng;
		// while (Math.abs(x - odometer.getX()) > CM_ERR || Math.abs(y -
		// odometer.getY()) > CM_ERR) {
		while (distanceBetween(odometer.getX(), odometer.getY(), x, y) >= CM_ERR) {
			minAng = (Math.atan2(y - odometer.getY(), x - odometer.getX())) * (180.0 / Math.PI);
			if (minAng < 0)
				minAng += 360.0;

			this.turnTo(minAng, false);

			// set the speeds slower if the robot is close to objective.
			if (distanceBetween(odometer.getX(), odometer.getY(), x, y) < 5) {
				this.setSpeeds(FAST / 2, FAST / 2);
			} else {
				this.setSpeeds(FAST, FAST);
			}

		}
		this.setSpeeds(0, 0);
	}

	/*
	 * TurnTo function which takes an angle and boolean as arguments The boolean
	 * controls whether or not to stop the motors when the turn is completed
	 */
	public void turnTo(double angle, boolean stop) {

		// double error = angle - this.odometer.getAng();
		double error = Odometer.fixDegAngle(Odometer.minimumAngleFromTo(this.odometer.getAng(), angle));

		while (Math.abs(error) > DEG_ERR) {

			// error = angle - this.odometer.getAng();
			error = Odometer.fixDegAngle(Odometer.minimumAngleFromTo(this.odometer.getAng(), angle));
			if (error < -180.0) {
				this.setSpeeds(-SLOW, SLOW);
			} else if (error < 0.0) {
				this.setSpeeds(SLOW, -SLOW);
			} else if (error > 180.0) {
				this.setSpeeds(SLOW, -SLOW);
			} else {
				this.setSpeeds(-SLOW, SLOW);
			}
		}

		if (stop) {
			this.setSpeeds(0, 0);
		}
	}
	public void part2() {
		// Navigate board and located locate styrofoam block
		boolean check = false;
		
		//TODO: Implement multiple checks until blue block is found
		check = searchForObject();
		scanner.turnTo(0, false);
		
		
		// Block not found
		if (!check) {
			LocalEV3.get().getAudio().systemSound(2);
			System.exit(0);
		}

		//TODO: Implement grabbing and taking block to finish
		// Grab object
		//grabBlock();

		// Travel to top right corner
		//travelTo(75, 75);

		// Drop off block
		//dropBlock();
		Lab5.pause();
	}
	
	//Search for block in field, return true if block is found, otherwise false
	public boolean searchForObject(){
		//Travel to and orient to starting position
		travelTo(20,20);
		turnTo(300,true);
		LCD.addInfo("D: ");
		
		//Rotate robot will searching for block
		while(odometer.getAng() < 150 || odometer.getAng() > 250){
			setSpeeds(-SLOW/2, SLOW/2);
			
			//If object is seen inside threshold then block is found
			int distanceThreshold = getDistanceThreshold(odometer.getAng());
			if (scanner.seesObject(distanceThreshold)){
				setSpeeds(0,0);
				//If block is a blue block exit method and return true
				if(approachAndCheck(odometer.getAng())){
					return true;
				}
				//Block was a wood block, rotate until no longer facing block then continue
				else{
					while(scanner.seesObject( distanceThreshold)){setSpeeds(-SLOW, SLOW);}
				}
				LCD.clearAdditionalInfo();
			}
		}
		return false;
	}
	//TODO: This is how i accounted for the walls being in the way, However it is currently unable to get the far corners near the wall
	
	//Return the desired value for the distance threshold to avoid the detection of walls
	public int getDistanceThreshold(double robotAngle){
		int output = 1000;
		//Facing back wall, to avoid detecting wall tailor distance threshold by angle of robot
		if (robotAngle > 280 && robotAngle < 355 ) {
			output =  (int)(25 / Math.cos(Math.toRadians(360 - robotAngle - 30)));
		}
		
		//Facing left wall
		else if(robotAngle < 170 && robotAngle > 105){
			output = (int)(25 / Math.cos(Math.toRadians(180 - robotAngle + 30)));
		}
		
		//Not facing wall, return default distance threshold
		return Math.min(80,output);
	}
	
	// Approach located object and check type
	// Return true if object is block
	// Return false if not block and return to original position
	public boolean approachAndCheck(double angle) {
		double distanceToBlock = scanner.getDistance();
		
		//Move 15 cm from block
		goForward(Math.max(15, distanceToBlock -15));
		Lab5.pause();
		
		//Scan for block, move forward after each scan if no block is found
		scanner.scan();
		while(!scanner.blockDetected()){
			goForward(5);
			scanner.scan();
		}
		
		// Check if block is blue, return true if it is
		if (scanner.blueBlockDetected()) {
			// Turn robot to face object detected based on angle of scanner
			double deltaTheta = convertThetaToRobot(scanner.getAngle());
			
			//Just using the scanner angle works, and is a lot less complicated
			turnTo(odometer.getAng() + //deltaTheta, true);
					scanner.getAngle(),true);
			LCD.addInfo("Blue Block Found");
			return true;
		}

		// Not styrofoam block return to previous location
		LCD.addInfo("Wood Block Found");
		scanner.turnTo(0,false);
		
		travelTo(20,20);
		turnTo(angle + 30, true);
		Lab5.pause();
		return false;
	}
	
	//Convert heading of light sensor to angle in relation to robot
	public double convertThetaToRobot(double angle){
		//X component of displacement
		double offsetX = Math.sin(Math.toRadians(angle));
		//Y component of displacement
		double offsetY = Math.cos(Math.toRadians(angle)) + USS_SENSOR_OFFSET;
		//Angle offset between sensor and robot
		return Math.atan2(offsetX,offsetY);
	}
	
	/*
	 * Go foward a set distance in cm
	 */
	public void goForward(double distance) {
		this.travelTo(Math.cos(Math.toRadians(this.odometer.getAng())) * distance + odometer.getX(),
				Math.sin(Math.toRadians(this.odometer.getAng())) * distance + odometer.getY());
	}
	
	//TODO: Implement going backwards to speed up return and to grab block
	//FROM SQUARE DRIVER (LAB 2)
	public void goBackward(double distance){
		leftMotor.rotate(-convertDistance(2.093, 60.96), true);
		rightMotor.rotate(-convertDistance(2.093, 60.96), false);
		
	}
	//FROM SQUARE DRIVER (LAB 2)
	private static int convertDistance(double radius, double distance) {
		return (int) ((180.0 * distance) / (Math.PI * radius));
	}

	public static double distanceBetween(double x1, double y1, double x2, double y2) {
		return Math.sqrt(Math.pow((y2 - y1), 2) + Math.pow((x2 - x1), 2));
	}

	public void grabBlock() {
		turnTo(this.odometer.getAng() + 180, true);
		// goBackward(15);
		this.claw.grab();
		goForward(15);
		turnTo(this.odometer.getAng() + 180, true);
	}

	public void dropBlock() {
		turnTo(this.odometer.getAng() + 180, true);
		this.claw.open();
		goForward(15);
		turnTo(this.odometer.getAng() + 180, true);

	}
}
