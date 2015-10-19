
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
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.lcd.LCD;
import lejos.hardware.motor.EV3LargeRegulatedMotor;

public class Navigation {
	final static int FAST = 100, SLOW = 60, ACCELERATION = 2000;
	final static double DEG_ERR = 3, CM_ERR = 1.0, USS_SENSOR_OFFSET = 8.8;
	private Odometer odometer;
	private Scanner scanner;
	private LCDInfo LCD;
	private EV3LargeRegulatedMotor leftMotor, rightMotor;
	public Claw claw = new Claw();

	
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
		//while (Math.abs(x - odometer.getX()) > CM_ERR || Math.abs(y - odometer.getY()) > CM_ERR) {
		while (distanceBetween(odometer.getX(), odometer.getY(), x,y)>=CM_ERR){
			minAng = (Math.atan2(y - odometer.getY(), x - odometer.getX())) * (180.0 / Math.PI);
			if (minAng < 0)
				minAng += 360.0;
			
			this.turnTo(minAng, false);
			
			// set the speeds slower if the robot is close to objective.
			if(distanceBetween(odometer.getX(), odometer.getY(), x, y)<5){
				this.setSpeeds(FAST/2, FAST/2);
			}else{
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
	

	// This is the method that will search for objects
	public void part2() {
		// Navigate board and located locate styrofoam block
		boolean check = false;
		
		/*
		
		//TODO: Change to i< 2 to try larger attempt
		for (int i = 0; i < 2; i++) {
			check = searchForObject(i);
			if (check) {
				break;
			}
		}*/
		
		check = searchForObject(0);
		scanner.turnTo(0, false);
		// Block not found
		if (!check) {
			LocalEV3.get().getAudio().systemSound(2);
			System.exit(0);
		}

		// Grab object
		//grabBlock();

		// Travel to top right corner
		//travelTo(75, 75);

		// Drop off block
		//dropBlock();

	}

	// Navigate board searching for object, return true if block has been found
	public boolean searchForObject(int searchSize) {
		// Travel to back wall, face 90 deg and turn sensor to -90 (0 deg)
		travelTo(20 + searchSize * 30, -15);
		turnTo(90, true);
		scanner.turnTo(-90,false);
		Lab5.pause();
		LCD.addInfo("LEG - 1");
		// Move forwards searching for object until we reach top of the square
		while (odometer.getY() < 20 + searchSize * 30) {
			
			setSpeeds(FAST, FAST);
			// if object stop and approach to detect type of object
			if (scanner.seesObject()) {
				setSpeeds(0, 0);
				LCD.addInfo("Saw Object");
				Lab5.pause();
				if (approachAndCheck(odometer.getX(), odometer.getY(), odometer.getAng())) {
					// If block exit method
					return true;
				}
			}
		}
		setSpeeds(0, 0);
		Lab5.pause();
		
		LCD.clearAdditionalInfo();
		LCD.addInfo("LEG - 2");
		// Slowly rotate searching for object until facing 180 degrees
		while (odometer.getAng() < 180) {
			setSpeeds(-SLOW, SLOW);
			
			// if object stop and approach to detect type of object
			if (scanner.seesObject()) {
				setSpeeds(0, 0);
				LCD.addInfo("Saw Object");
				Lab5.pause();
				if (approachAndCheck(odometer.getX(), odometer.getY(), odometer.getAng())) {
					// If block exit method
					return true;
				}
			}
			
		}
		setSpeeds(0, 0);
		Lab5.pause();
		
		LCD.clearAdditionalInfo();
		LCD.addInfo("LEG - 3");
		// Travel to left wall searching for object
		while (odometer.getX() > -15) {
			setSpeeds(FAST, FAST);
			if (scanner.seesObject()) {
				// if object stop and approach to detect type of object
				setSpeeds(0, 0);
				LCD.addInfo("Saw Object");
				Lab5.pause();
				if (approachAndCheck(odometer.getX(), odometer.getY(), odometer.getAng())) {
					// If block exit method
					return true;
				}
			}
			
		}
		//No object viewed
		return false;
	}

	// Approach located object and check type
	// Return true if object is block
	// Return false if not block and return to original position
	public boolean approachAndCheck(double x, double y, double angle) {

		// Turn robot to face object detected
		double deltaTheta = Math.atan2(USS_SENSOR_OFFSET, scanner.getDistance());
		turnTo(odometer.getAng() - deltaTheta, true);

		// Reset scanner position
		scanner.turnTo(0,false);

		// Slowly move towards object
		while (scanner.getDistance() > 10) {
			setSpeeds(SLOW, SLOW);
		}
		setSpeeds(0, 0);

		// Check if block is styrofoam, return true if it is
		if (scanner.scan()) {
			return true;
		}

		// Not styrofoam block return to previous location
		scanner.turnTo(-90, false);
		travelTo(x, y);
		turnTo(angle, true);
		return false;
	}
	
	

	/*
	 * Go foward a set distance in cm
	 */
	public void goForward(double distance) {
		this.travelTo(Math.cos(Math.toRadians(this.odometer.getAng())) * distance,
				Math.sin(Math.toRadians(this.odometer.getAng())) * distance);

	}

	public static double distanceBetween(double x1, double y1, double x2, double y2) {
		return Math.sqrt(Math.pow((y2 - y1), 2) + Math.pow((x2 - x1), 2));
	}
	
	public void grabBlock(){
		turnTo(this.odometer.getAng()+180,true);
		goForward(15);
		this.claw.grab();
		goForward(15);
		turnTo(this.odometer.getAng()+180,true);
	}
	
	public void dropBlock(){
		turnTo(this.odometer.getAng()+180,true);
		this.claw.open();
		goForward(15);
		turnTo(this.odometer.getAng()+180,true);
		
	}
}
