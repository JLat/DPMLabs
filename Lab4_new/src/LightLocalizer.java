import lejos.hardware.Button;
import lejos.hardware.ev3.LocalEV3;
import lejos.robotics.SampleProvider;

public class LightLocalizer {
	// Creatings objects of the other classes
	private Odometer odo;
	private LightSensor lSensor;
	private Navigation nav;
	private LCDInfo lcd;

	// Speed that robot will rotate with
	public static float ROTATION_SPEED = 60;
	// Difference of color reading that will indicate a line
	private int lineDifference = 30;
	// Distance of light sensor from centre of robot (in cm)
	public static double lightSensorDistance = 12.5;

	// Main constructor
	public LightLocalizer(Odometer odo, SampleProvider colorSensor, float[] colorData, Navigation nav, LCDInfo LCD) {
		this.odo = odo;
		this.lSensor = new LightSensor(colorSensor, colorData, lineDifference);
		this.nav = nav;
		this.lcd = LCD;
	}

	// Perform light localizations
	public void doLocalization() {

		// Send the light sensor to LCD class and update status
		this.lSensor.start();
		this.lcd.setSensor(lSensor);

		// initiate required variables.
		double thetaX1 = 0, thetaX2 = 0, thetaY1 = 0, thetaY2 = 0, newX = 0, newY = 0, deltaTheta, deltaThetaX,
				deltaThetaY, deltaX = 0, deltaY = 0;

		// Wait until light sensor is calibrated
		while (!this.lSensor.isCalibrated()) {
		}

		// Pause to wait to take readings (Press down to continue, other button
		// to close)
		pause();

		// Assuming starting with robot facing approximately 0 degrees (-x is
		// first line crossed)

		// Rotate,detect 4 lines and collect the data needed
		for (int i = 1; i <= 4; i++) {

			// while no line is detected, rotate.
			while (!lSensor.seesLine()) {
				nav.setSpeeds(+ROTATION_SPEED, -ROTATION_SPEED);
			}

			// Play sound when line is found
			LocalEV3.get().getAudio().systemSound(0);

			if (i % 2 == 1) {
				// i is the line number. Our axes are defined such that the
				// first line it detects is the negative X axis value, then the
				// positive Y value, and so on.

				// if thetaX1 is not yet set,
				if (thetaX1 == 0) {
					// This is the first line on X axis
					thetaX1 = odo.getAng();
					this.lcd.addInfo("thetax1: ", thetaX1);

				} else {
					// Second line on x axis
					thetaX2 = odo.getAng();
					this.lcd.addInfo("thetaX2: ", thetaX2);

					// set the difference between the two angles.
					deltaX = AngleTraveledFromTo(thetaX1, odo.getAng());

					// set the new Y coordinate.
					newY = -lightSensorDistance * Math.cos((Math.toRadians(deltaX)) / 2);
				}

			} else {

				// Odd therefore lines on y axis (vertical)

				// if thetaY1 has not been set yet
				if (thetaY1 == 0) {
					// This is the first line on Y axis

					thetaY1 = odo.getAng();
					this.lcd.addInfo("thetaY1: ", thetaY1);

				} else {
					// Second line on Y axis
					thetaY2 = odo.getAng();
					deltaY = AngleTraveledFromTo(thetaY1, odo.getAng());

					this.lcd.addInfo("thetaY2: ", thetaY2);

					// set new X coordinate.
					newX = -lightSensorDistance * Math.cos(Math.toRadians(deltaY) / 2);
				}

			}

			// this loop rotates the robot off a line so that it doesnt detect
			// it twice.
			while (lSensor.seesLine()) {
				nav.setSpeeds(+ROTATION_SPEED, -ROTATION_SPEED);
			}

		}

		// stop the motors when all lines have been detected.
		nav.setSpeeds(0, 0);

		// calculate the deltaTheta values with respect to our set of
		// coordinates.
		deltaThetaY = 180 - thetaY2 - deltaY / 2;
		deltaThetaX = 270 - thetaX2 - deltaX / 2;
		deltaTheta = (deltaThetaY + deltaThetaX) / 2;

		// Update odometer with new position and pause to take measurements
		odo.setPosition(new double[] { newX, newY, odo.getAng() + deltaTheta }, new boolean[] { true, true, true });
		pause();

		// Travel to 0,0 w/ 0 deg heading and pause again to wait for
		// measurements
		nav.travelTo(0, 0);
		nav.turnTo(0, true);
		pause();
	}

	// Method to stop localization until user continues
	public void pause() {
		while (Button.waitForAnyPress() != Button.ID_DOWN)
			System.exit(0);
	}

	// Finds angle rotated between two points
	public double AngleTraveledFromTo(double startingAngle, double endAngle) {
		// assumption made that the angles are decreasing while the robot is
		// moving, i.e. the robot is turning right.
		if (startingAngle < endAngle) {
			return 360 - (endAngle - startingAngle);
		} else {
			return startingAngle - endAngle;
		}
	}

}
