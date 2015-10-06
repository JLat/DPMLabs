// Lab3.java - Navigation

// Fabrice Normandine
// Joel Lat

package ev3Navigation;

import lejos.hardware.Button;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.lcd.TextLCD;
import lejos.hardware.motor.EV3LargeRegulatedMotor;

public class Lab3 {

	private static final EV3LargeRegulatedMotor leftMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("A"));
	private static final EV3LargeRegulatedMotor rightMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("D"));

	// Constants
	public static final double WHEEL_RADIUS = 2.1;
	public static final double TRACK = 14.84;

	public static void main(String[] args) {
		// used to exit the loop.
		int buttonChoice = 0;

		// this allows smooth operation of the robot, effectively reducing
		// slipping.
		rightMotor.setAcceleration(500);
		leftMotor.setAcceleration(500);

		// instantiate main classes.
		final TextLCD t = LocalEV3.get().getTextLCD();
		Odometer odometer = new Odometer(leftMotor, rightMotor, WHEEL_RADIUS, TRACK);
		Navigator navigator = new Navigator(odometer, leftMotor, rightMotor, WHEEL_RADIUS, TRACK);
		OdometryDisplay odometryDisplay = new OdometryDisplay(odometer, t, navigator);

		do {
			// clear the display
			t.clear();

			// ask the user whether the robot should follow waypoints or avoid
			// an obstacle.
			t.drawString("< Left | Right >", 0, 0);
			t.drawString("       |        ", 0, 1);
			t.drawString(" Part  | Part   ", 0, 2);
			t.drawString("   1   |   2    ", 0, 3);
			t.drawString("       |        ", 0, 4);

			buttonChoice = Button.waitForAnyPress();
		} while (buttonChoice != Button.ID_LEFT && buttonChoice != Button.ID_RIGHT);
		if (buttonChoice == Button.ID_RIGHT) {

			// start the threads for obstacle avoiding.
			odometer.start();
			odometryDisplay.start();
			navigator.start();

			// waypoints, in array form for easy traversal.
			double[] destX = { 0, 60 };
			double[] destY = { 60, 0 };

			// for every waypoint,
			for (int i = 0; i < destX.length; i++) {
				// travel to the waypoint, set the avoid boolean to true such
				// that the Navigator knows to avoid obstacles in the way.
				navigator.travelTo(destX[i], destY[i], true);
				// Wait until we are done moving to location
				while (navigator.isNavigating()) {
					int button = Button.waitForAnyPress(500);
					if (button != Button.ID_ESCAPE && button != 0)
						System.exit(0);
				}
			}
		} else {
			// start the threads used in waypoint traversal.
			odometer.start();
			odometryDisplay.start();
			navigator.start();

			double[] destX = { 60, 30, 30, 60 };
			double[] destY = { 30, 30, 60, 0 };

			for (int i = 0; i < destX.length; i++) {
				// travel to waypoints, using a value of false for the avoid
				// variable.
				navigator.travelTo(destX[i], destY[i], false);
				// Wait until we are done moving to location
				while (navigator.isNavigating()) {
					int button = Button.waitForAnyPress(500);
					if (button != Button.ID_ESCAPE && button != 0)
						System.exit(0);
				}

			}
		}

		while (Button.waitForAnyPress() != Button.ID_ESCAPE)
			;
		System.exit(0);
	}
}