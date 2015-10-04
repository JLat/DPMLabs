package ev3Navigation;

import lejos.hardware.motor.EV3LargeRegulatedMotor;

public class Navigator extends Thread {

	private boolean navigating, obstacle;
	private Odometer odometer;
	private EV3LargeRegulatedMotor rightMotor, leftMotor;
	private double wheelRadius, wheelTrack;
	private double destX, destY, destTheta,
	// temporary X and Y values
			tempX = 0, tempY = 0;

	// ---------------SETTINGS TO TWEAK---------------------------

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
			 * (in degrees) over which On-Point turning (without forward
			 * velocity) takes charge of correcting the robot's heading instead
			 * of a smooth proportional turning.
			 */
			smoothTurningThreshold = 30;
	private double
	// this constant dictates how much of an impact the angle difference between
	// the current heading and the destination heading will have on the motor
	// speeds.
	angularCorrectionConstant = 5.0;

	// -----------------------------------------------------------

	// what values are supposed to go in your smoothsensor?

	// TODO: Fab- the values are pretty well explained in the declarations on
	// the top of SmoothUSSensor class, basicly the list size, the plus and
	// minus from the average that are tolerable, and the absolute bounds on
	// values that we accept (0-200 for example).
	private SmoothUSSensor USS = new SmoothUSSensor(10, 5, 20, 200, 0);

	// Default Constructor
	public Navigator(Odometer OD, EV3LargeRegulatedMotor leftMotor,
			EV3LargeRegulatedMotor rightMotor, double WR, double WS) {
		odometer = OD;
		this.leftMotor = leftMotor;
		this.rightMotor = rightMotor;
		wheelRadius = WR;
		wheelTrack = WS;

		this.USS.start();

	}

	public void run() {
		if (navigating) {

			// If we have reached our destination stop the motors and stop
			// navigating
			if (Math.abs(odometer.getX() - destX) < 1
					&& Math.abs(odometer.getY() - destY) < 1) {
				rightMotor.flt();
				leftMotor.flt();
				navigating = false;
			}

			// Obstacle in the way , start avoiding it.
			else if (USS.getProcessedDistance() < obstacleThreshold
					&& !obstacle) {
				obstacle = true;
			}

			// Currently avoiding an obstacle
			if (obstacle) {

				// method avoidObstacle() uses the USS data and sets the motors
				// to the right speeds, returns true if the obstacle is now
				// avoided.
				obstacle = avoidObstacle();

			}

			// The obstacle was avoided or the heading drifted past a the
			// threshold, turn On-Point to face destination
			else if (Math.abs(destTheta - odometer.getTheta()) > smoothTurningThreshold) {

				rightMotor.flt();
				leftMotor.flt();
				travelTo(destX, destY);
			}

			// Heading towards destination, proceed forward
			// Adjust motor speed depending on the linear and angular errors.
			else {

				// Calculate current distance to destination
				double distanceTo = distanceBetween(odometer.getX(),
						odometer.getY(), destX, destY);

				// TODO: Check if the angular and linear errors effectively
				// smooth out the correction of the robot's linear speed and
				// heading.
				int linearError = (int) (Math.min(FORWARD_SPEED, FORWARD_SPEED
						/ 2 + (distanceTo * 5)));

				int angularError = (int) (getMinimalAngleBetween(
						odometer.getTheta(), destTheta));

				// set the speeds of the motor according to the linear and
				// angular corrections.
				rightMotor
						.setSpeed((int) (linearError + angularCorrectionConstant
								* angularError));
				leftMotor
						.setSpeed((int) (linearError - angularCorrectionConstant
								* angularError));

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
		 * great again (or simply that the obstacle is cleared) Note: Odometer
		 * is active while this happens.
		 * 
		 * 3) Use current X and Y to reposition itself to target and begin
		 * moving required distance.
		 */
	}

	// Rotate robot to heading theta
	public void turnTo(double theta) {
		navigating = true;

		Double currentHeading = odometer.getTheta() % 360;

		rotate(getMinimalAngleBetween(currentHeading, theta));
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
		// Set Destination location
		Double dx, dy;
		destX = x;
		destY = y;

		// tell other classes it is navigating
		navigating = true;

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
			destTheta = (Math.atan(dy / dx)) * 180 / Math.PI;
		} else if (dx < 0 && dy > 0) {
			destTheta = (Math.atan(dy / dx) + Math.PI) * 180 / Math.PI;
		} else if (dx < 0 && dy < 0) {
			destTheta = (Math.atan(dy / dx) - Math.PI) * 180 / Math.PI;
		}
		turnTo(destTheta);
		// Start moving forward
		leftMotor.forward();
		rightMotor.forward();
	}

	boolean avoidObstacle() {

		// the return value of this function represents if the robot has cleared
		// the obstacle.
		boolean obstacleAvoided = false;

		// the temporary values tempX and tempY represent position of the end of
		// the obstacle. they equal 0 when the position has not been set yet.

		int distance = USS.getProcessedDistance();

		// the ratio variable is used to slow down one wheel proportionally
		// to how close the robot is to the obstacle (P-Type style).
		// ratio should remain fractional and between 0 and 1;
		double ratio;

		// TODO: Fab- prehaps it would better to use a constant multiplier of
		// some kind in order to get a more severe response when the robot gets
		// close to an obstacle.. I dont know for now, physical experimentation
		// will
		// tell us it this is sufficient.

		// emergency turn if robot somehow gets right next to the obstacle
		if (distance <= emergencyThreshold) {

			turnTo(odometer.getTheta() + 20);
			tempX = 0;
			tempY = 0;

		} else if (distance < obstacleThreshold) {
			// if the robot is at a safe distance from an obstacle, begin
			// avoiding it with a P-Type controller.

			ratio = (distance / 40.0);
			rightMotor.setSpeed((int) (FORWARD_SPEED * ratio));
			leftMotor.setSpeed(FORWARD_SPEED);

			tempX = 0;
			tempY = 0;

		} else {

			// the distance is now bigger than 40.
			// The obstacle was cleared. the robot should still move forward for
			// a little while before setting the obstacleAvoided value to true
			// and repositioning itself towards the destination.

			// if the tempX and tempY values have not been set yet, set them so
			// they are not reset every time this method is called.
			if (tempX == 0 && tempY == 0) {
				tempX = odometer.getX();
				tempY = odometer.getY();
			}

			// move 10 cm away from the current position.
			if (distanceBetween(odometer.getX(), odometer.getY(), tempX, tempY) <= 10) {
				rightMotor.setSpeed(FORWARD_SPEED);
				leftMotor.setSpeed(FORWARD_SPEED);

			} else {
				// we have now moved 10 cm away from the end of the obstacle,
				// reset the tempX and tempY values and return true so the robot
				// can reposition itself.
				tempX = 0;
				tempY = 0;
				obstacleAvoided = true;
			}
		}

		return obstacleAvoided;

	}

	public boolean isNavigating() {
		return navigating;
	}

	public double getMinimalAngleBetween(double currentTheta,
			double DestinationTheta) {

		if (Math.abs(DestinationTheta - currentTheta) <= 180) {
			return (DestinationTheta - currentTheta);
		} else if ((DestinationTheta - currentTheta) < -180) {
			return (DestinationTheta - currentTheta) + 360;
		} else if ((DestinationTheta - currentTheta) > 180) {
			return (DestinationTheta - currentTheta) - 360;
		} else {
			// all cases have been supposedly taken care of. If something goes
			// wrong, then print a message.
			System.out.println("Invalid angle in getMinimalAngleBetween("
					+ currentTheta + "," + DestinationTheta + ")");
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

	public double distanceBetween(double x1, double y1, double x2, double y2) {
		return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
	}

}