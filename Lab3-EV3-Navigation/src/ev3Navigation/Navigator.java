package ev3Navigation;

import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.motor.EV3LargeRegulatedMotor;

public class Navigator extends Thread {

	private boolean
	// boolean variable, used to tell other classes if the robot is currently
	// moving.
	navigating,
			// represents if the robot is currently facing/avoiding an obstacle.
			obstacle,
			// represents wether or not the robot should avoid obstacles (take
			// the USS readings into account).
			avoid;
	private Odometer odometer;
	private EV3LargeRegulatedMotor rightMotor, leftMotor;
	private double wheelRadius, wheelTrack;
	// current destination of the robot.
	private double destX, destY, destTheta,

	// temporary X and Y values
			tempX = 0, tempY = 0;
	private static final long NAVIGATOR_PERIOD = 25;

	// ---------------SETTINGS---------------------------

	private static final int FORWARD_SPEED = 250;
	private static final int ROTATE_SPEED = 150;
	private int
	// useful thresholds for robot correction.
	// Distance value below which the robot considers it is facing an obstacle.
	obstacleThreshold = 40,
			// distance below which the robot enters an emergency turn.
			emergencyThreshold = 15,
			/*
			 * smoothTurningThreshold is a constant representing an angle error
			 * (in degrees) over which On-Point turning occurs.
			 */
			smoothTurningThreshold = 5,
			// used in the P-type obstacle avoider.
			ratio = 7;

	// -----------------------------------------------------------
	// wrapper for a USS sensor, more info in the SmoothUSSensor.java file.
	private SmoothUSSensor USS;

	// Constructor for the Navigator.
	public Navigator(Odometer OD, EV3LargeRegulatedMotor leftMotor, EV3LargeRegulatedMotor rightMotor, double WR,
			double WS) {
		odometer = OD;
		this.leftMotor = leftMotor;
		this.rightMotor = rightMotor;
		wheelRadius = WR;
		wheelTrack = WS;
		this.USS = new SmoothUSSensor(5, 5, 10, 50, 1);
		this.USS.start();

	}

	// main process of the program.
	public void run() {

		long updateStart, updateEnd;

		while (true) {
			updateStart = System.currentTimeMillis();

			/*
			 * Algorithm idea:
			 * 
			 * 
			 * 1) head to the target, move forward the required distance.
			 * 
			 * if at any time the robot detects that it is close to a wall:
			 * 
			 * 2)- begin a P-type wall-following method until the distance
			 * becomes great again (or simply that the obstacle is cleared)
			 * Note: Odometer is active while this happens.
			 * 
			 * 3) Use current X and Y to reposition itself to target and begin
			 * moving required distance.
			 */

			// if the robot is currently moving or has a command to move,
			if (navigating) {

				// If the robot is set to avoid obstacles, and that it detects
				// one, then proceed to avoid it.
				if (avoid && USS.getProcessedDistance() < obstacleThreshold && !obstacle) {
					obstacle = true;
				}

				// if there is an obstacle to avoid,
				if (obstacle) {

					obstacle = avoidObstacle();
					// method avoidObstacle() uses the USS data and sets the
					// motors to the right speeds, returns false if the obstacle
					// is now avoided.

				}

				// The obstacle was avoided or the heading drifted past a the
				// threshold, turn On-Point to face destination
				else if ((Math.abs(getMinimalAngleBetween(destTheta, odometer.getTheta())) > smoothTurningThreshold)) {

					rightMotor.flt();
					leftMotor.flt();

					// head to destination and start motors.
					travelTo(destX, destY, avoid);

				}

				// currently Heading towards destination, proceed forward
				// Adjust motor speed depending on the linear error.
				else {

					// Calculate current distance to destination
					double distanceTo = distanceBetween(odometer.getX(), odometer.getY(), destX, destY);

					// this linear error correction slows down the robot once it
					// gets close to its objective.
					int linearError = (int) (FORWARD_SPEED / 2 + (distanceTo * 5));

					// constrainWithin sets bounds on the linearError value.
					linearError = constrainWithin(FORWARD_SPEED / 2, linearError, FORWARD_SPEED);

					// set the speeds of the motor according to the linear
					// error.
					rightMotor.setSpeed(linearError);
					leftMotor.setSpeed(linearError);

					rightMotor.forward();
					leftMotor.forward();
				}

				// If we have reached our destination stop the motors and stop
				// navigating (proceed to next destination).
				if (distanceBetween(odometer.getX(), odometer.getY(), destX, destY) < 2) {

					navigating = false;
					// stop both motors at once.
					rightMotor.stop(true);
					leftMotor.stop(false);
					LocalEV3.get().getAudio().systemSound(0);

				}

				updateEnd = System.currentTimeMillis();
				if (updateEnd - updateStart < NAVIGATOR_PERIOD) {
					try {
						Thread.sleep(NAVIGATOR_PERIOD - (updateEnd - updateStart));
					} catch (InterruptedException e) {
						// there is nothing to be done here because it is not
						// expected that the Navigator will be interrupted by
						// another thread
					}
				}
			}
		}

	}

	// Rotate robot to desired angle theta (with respect to the Y axis
	// representing 0 degrees, growing clockwise.)
	public void turnTo(double theta) {

		Double currentHeading = odometer.getTheta() % 360;

		rotate(getMinimalAngleBetween(currentHeading, theta));
		// tell other classes or threads that the robot is moving.
		this.navigating = true;
	}

	// Rotate robot by angle theta
	public void rotate(double theta) {
		// code from Lab2.
		leftMotor.setSpeed(ROTATE_SPEED);
		rightMotor.setSpeed(ROTATE_SPEED);
		leftMotor.rotate(convertAngle(wheelRadius, wheelTrack, theta), true);
		rightMotor.rotate(-convertAngle(wheelRadius, wheelTrack, theta), false);

	}

	// Move robot to location (x,y), if avoid==true, avoid obstacles in the way.
	public void travelTo(double x, double y, boolean avoid) {
		// tell other classes it is navigating
		this.navigating = true;
		// Set Destination location
		Double dx, dy;
		this.destX = x;
		this.destY = y;

		// Determine whether to search for obstacles or not
		this.avoid = avoid;

		// calculate the positional error.
		dx = x - odometer.getX();
		dy = y - odometer.getY();

		// Calculate angle to rotate to get to destination
		// Note: every possible combination is taken care of by conditional
		// statements, assuring correct results from the atan function.

		if (dx == 0 && dy > 0) {
			destTheta = 0;
		} else if (dx == 0 && dy < 0)
			destTheta = 180;
		else if (dy == 0 && dx > 0)
			destTheta = 90;
		else if (dy == 0 && dx < 0)
			destTheta = 270;
		else if (dx > 0) {
			destTheta = (Math.PI / 2 - Math.atan(dy / dx)) * 180 / Math.PI;
		} else if (dx < 0 && dy > 0) {
			destTheta = (-Math.PI / 2 - Math.atan(dy / dx)) * 180 / Math.PI;
		} else if (dx < 0 && dy < 0) {
			destTheta = (3 * Math.PI / 2 - Math.atan(dy / dx)) * 180 / Math.PI;
		}

		// limit the destTheta value within -180 and 180;
		destTheta = getMinimalAngleBetween(0, destTheta);

		turnTo(destTheta);

	}

	boolean avoidObstacle() {

		// the return value of this function represents if the robot has an
		// obstacle to avoid.
		boolean avoidingObstacle = true;

		// the temporary values tempX and tempY represent position of the end of
		// the obstacle. they equal 0 when the position has not been set yet.

		// retreive a "smooth" distance reading from the USS.
		int distance = USS.getProcessedDistance();

		// emergency turn if robot gets right next to the obstacle
		if (distance <= emergencyThreshold) {
			// rotate away from the obstacle.
			rotate(35);
			tempX = 0;
			tempY = 0;

		} else if (distance < obstacleThreshold) {
			// if the robot is at a safe distance from an obstacle, begin
			// avoiding it with a P-Type controller.

			int delta = obstacleThreshold - distance;
			// the speed of the motor is constrained between FORWARD_SPEED/2 and
			// FORWARD_SPEED
			rightMotor
					.setSpeed((int) constrainWithin(FORWARD_SPEED / 2, (FORWARD_SPEED - ratio * delta), FORWARD_SPEED));
			leftMotor.setSpeed(FORWARD_SPEED);
			rightMotor.forward();
			leftMotor.forward();

			tempX = 0;
			tempY = 0;

		} else {

			// the distance is now bigger than the obstacleThreshold.
			// The obstacle was cleared. the robot should still move forward for
			// a little while before setting the avoidingObstacle value to false
			// and repositioning itself towards the destination.

			// if the tempX and tempY values have not been set yet, set them so
			// they are not reset every time this method is called.
			if (tempX == 0 && tempY == 0) {
				tempX = odometer.getX();
				tempY = odometer.getY();
			}

			// move 25cm away from the current position.
			if (distanceBetween(odometer.getX(), odometer.getY(), tempX, tempY) <= 25) {
				rightMotor.setSpeed(FORWARD_SPEED);
				leftMotor.setSpeed(FORWARD_SPEED);
				rightMotor.forward();
				leftMotor.forward();

			} else {
				// we have now moved 25 cm away from the end of the obstacle,
				// reset the tempX and tempY values and return false so the
				// robot
				// can reposition itself.

				tempX = 0;
				tempY = 0;
				avoidingObstacle = false;
				// stop both motors at once.
				leftMotor.stop(true);
				rightMotor.stop(false);

				// head to target and start moving forward.
				travelTo(destX, destY, avoid);

			}
		}
		// return true if obstacle remains, false if it was cleared.
		return avoidingObstacle;

	}

	public boolean isNavigating() {
		return navigating;
	}

	public double getMinimalAngleBetween(double currentTheta, double DestinationTheta) {

		// this method returns the minimal angle between currentTheta and
		// DestinationTheta (in degrees)
		currentTheta %= 360;
		DestinationTheta %= 360;

		if (Math.abs(DestinationTheta - currentTheta) <= 180) {
			return (DestinationTheta - currentTheta);
		} else if ((DestinationTheta - currentTheta) < -180) {
			return (DestinationTheta - currentTheta) + 360;
		} else if ((DestinationTheta - currentTheta) > 180) {
			return (DestinationTheta - currentTheta) - 360;
		} else {
			// all cases have been supposedly taken care of. If something goes
			// wrong, then print a message.
			System.out
					.println("Invalid angle in getMinimalAngleBetween(" + currentTheta + "," + DestinationTheta + ")");
			return 0.0;
		}

	}

	// From Lab2 - Square Driver
	private static int convertDistance(double radius, double distance) {
		return (int) ((180.0 * distance) / (Math.PI * radius));
	}

	// From Lab2 - SquareDriver
	private static int convertAngle(double radius, double width, double angle) {
		return convertDistance(radius, Math.PI * width * angle / 360.0);
	}

	// returns the distance between two points.
	public double distanceBetween(double x1, double y1, double x2, double y2) {
		return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
	}

	// simple function used to access relevant information in the OdometryDisplay class.
	public double[] getDestination() {
		double[] destination = { destX, destY, destTheta, USS.getProcessedDistance() };
		return destination;
	}

	// limits the value within [lowerBound, upperBound]
	public int constrainWithin(int lowerBound, int value, int upperBound) {
		return Math.min(upperBound, Math.max(lowerBound, value));
	}
	// same, used for angle
	public double constrainWithin(double lowerBound, double value, double upperBound) {
		return Math.min(upperBound, Math.max(lowerBound, value));
	}
	

}