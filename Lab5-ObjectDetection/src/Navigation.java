
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.motor.EV3LargeRegulatedMotor;

public class Navigation {
	final static int FAST = 100, SLOW = 60, ACCELERATION = 2000;
	// TODO: the USS_SENSOR_OFFSET value is closer to 12 in real life.
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

	// Part 2 of the lab, finding, obtaining and delivering blue block
	public void part2() {
		
		// Navigate board and located locate styrofoam block
		boolean check = searchForObject();


		LCD.addInfo(check ? "BLUE BLOCK" : "NO BLOCK");
		Lab5.pause();
		scanner.turnTo(0, false);
		
		// Block not found
		if (!check) {
			LocalEV3.get().getAudio().systemSound(2);
			System.exit(0);
		}
		
		// Grab object
		grabBlock();

		// Travel to top right corner
		travelTo(75, 75);
		turnTo(225,true);

		// Drop off block
		dropBlock();

		LCD.addInfo("Complete");
		Lab5.pause();
	}

	// Search for block in field, return true if block is found, otherwise false
	public boolean searchForObject() {
		double woodblock = -1;
		// Travel to and orient to starting position
		travelTo(20, 20);
		turnTo(310, true);
		LCD.addInfo("D: ");

		// Rotate robot will searching for block
		while (odometer.getAng() < 155 || odometer.getAng() > 200) {
			setSpeeds(-SLOW / 2, SLOW / 2);

			// If object is seen inside threshold then block is in front of the
			// robot
			int distanceThreshold = getDistanceThreshold(odometer.getAng());
			if (scanner.seesObject(distanceThreshold)) {
				setSpeeds(0, 0);
				turnTo(odometer.getAng() + 15, true);
				double tempangle = odometer.getAng();

				// If block is a blue block exit method and return true
				if (approachAndCheck()) {
					return true;
				}
				// Block was a wood block, rotate until no longer facing block
				// then continue
				else {
					woodblock = tempangle;
					travelTo(20, 20);
					scanner.turnTo(0, false);
					turnTo(tempangle, true);

					// To make sure robot is no longer facing object check 3
					// times that no object is viewable before continuing

					while (scanner.seesObject(distanceThreshold)
							&& (odometer.getAng() < 155 || odometer.getAng() > 250)) {
						while (scanner.seesObject(distanceThreshold)
								&& (odometer.getAng() < 155 || odometer.getAng() > 250)) {
							while (scanner.seesObject(distanceThreshold)
									&& (odometer.getAng() < 155 || odometer.getAng() > 250)) {
								setSpeeds(-SLOW / 2, SLOW / 2);
							}
							turnTo(odometer.getAng() + 20, true);
						}
						turnTo(odometer.getAng() + 10, true);
					}
				}

				LCD.clearAdditionalInfo();
				LCD.addInfo("D: ");
			}
		}
		setSpeeds(0, 0);
		return behindWood(woodblock);
	}

	// If only wood block was found check behind the wooden blocj
	public boolean behindWood(double angle) {
		LCD.addInfo("Behind");
		if (angle == -1) { // NO BLOCK WAS FOUND
			return false;
		} else if (angle < 45 || angle > 315) {
			travelTo(20, 50);
			turnTo(0, true);
			
			//Captures case where blue block was beside wooden block 
			if (scanner.getDistance() <= 30)
				return approachAndCheck();
			travelTo(70, 70);
			turnTo(270, true);
			return approachAndCheck();
		} else
		{
			travelTo(50, 20);
			travelTo(70, 70);
			turnTo(180, true);
			return approachAndCheck();
		}
	}


	// Return the desired value for the distance threshold to avoid the
	// detection of walls
	public int getDistanceThreshold(double robotAngle) {
		int output = 1000;
		// Facing back wall, to avoid detecting wall tailor distance threshold
		// by angle of robot
		if (robotAngle > 280 && robotAngle < 355) {
			output = (int) (25 / Math.cos(Math.toRadians(360 - robotAngle - 30)));
		}

		// Facing left wall
		else if (robotAngle < 170 && robotAngle > 105) {
			output = (int) (25 / Math.cos(Math.toRadians(180 - robotAngle + 30)));
		}

		// Not facing wall, return default distance threshold
		return Math.min(70, output);
	}

	// Approach located object and check type
	// Return true if object is block
	// Return false if not block
	public boolean approachAndCheck() {
		double distanceToBlock = scanner.getDistance();

		// Move towards the block 
		goForward(distanceToBlock / 2);

		// Scan for block, move forward after each scan if no block is found
		scanner.USS.setParameters(10, 10, 15, 50, 0);
		scanner.scan();
		while (!scanner.blockDetected()) {
			goForward(3);
			scanner.scan();
		}
		scanner.USS.setParameters(10, 15, 15, 90, 0);
		
		// Check if block is blue, return true if it is
		if (scanner.blueBlockDetected()) {
			// Turn robot to face object detected based on angle of scanner
			turnTo(odometer.getAng() + ((Math.abs(scanner.getAngle()) > 20) ? scanner.getAngle() : 0), true);
			LCD.addInfo("Blue Block Found");
			return true;
		}

		// Not styrofoam block
		LCD.addInfo("Wood Block Found");
		return false;
	}

	
	  //Go foward a set distance in cm
	public void goForward(double distance) {
		this.travelTo(Math.cos(Math.toRadians(this.odometer.getAng())) * distance + odometer.getX(),
				Math.sin(Math.toRadians(this.odometer.getAng())) * distance + odometer.getY());
	}

	// FROM SQUARE DRIVER (LAB 2)
	//Go backwards a set distance in cm (used to grab block)
	public void goBackward(double distance) {
		leftMotor.setSpeed(250);
		rightMotor.setSpeed(250);
		leftMotor.rotate(-convertDistance(2.093, distance), true);
		rightMotor.rotate(-convertDistance(2.093, distance), false);
	}

	// FROM SQUARE DRIVER (LAB 2)
	private static int convertDistance(double radius, double distance) {
		return (int) ((180.0 * distance) / (Math.PI * radius));
	}

	public static double distanceBetween(double x1, double y1, double x2, double y2) {
		return Math.sqrt(Math.pow((y2 - y1), 2) + Math.pow((x2 - x1), 2));
	}

	public void grabBlock() {
		turnTo(this.odometer.getAng() + 180, true);
		claw.partialClose();
		goBackward(10);
		this.claw.grab();
	}

	public void dropBlock() {
		turnTo(this.odometer.getAng() + 180, true);
		this.claw.open();

	}
}
