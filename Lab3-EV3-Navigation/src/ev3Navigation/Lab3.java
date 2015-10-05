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
		int buttonChoice = 0;

		rightMotor.setAcceleration(500);
		leftMotor.setAcceleration(500);
		
		final TextLCD t = LocalEV3.get().getTextLCD();
		Odometer odometer = new Odometer(leftMotor,rightMotor, WHEEL_RADIUS, TRACK);
		OdometryDisplay odometryDisplay = new OdometryDisplay(odometer,t);
		Navigator navigator = new Navigator(odometer,leftMotor,rightMotor,WHEEL_RADIUS,TRACK);	
		
		do {
			// clear the display
			t.clear();

			// ask the user whether the motors should drive in a square or float
			t.drawString("< Left | Right >", 0, 0);
			t.drawString("       |        ", 0, 1);
			t.drawString(" Part  | Part   ", 0, 2);
			t.drawString("   1   |   2    ", 0, 3);
			t.drawString("       |        ", 0, 4);

			buttonChoice = Button.waitForAnyPress();
		} while (buttonChoice != Button.ID_LEFT
				&& buttonChoice != Button.ID_RIGHT);
		if (buttonChoice == Button.ID_RIGHT) {
			odometer.start();
			odometryDisplay.start();
			navigator.start();
			
			double [] destX = {0,60};
			double [] destY = {60,0};
			
			for (int i = 0; i < destX.length; i ++){
				navigator.travelTo(destX[i],destY[i], true);
				//Wait until we are done moving to location
				while(navigator.isNavigating()){}
			}
		}
		else{
			odometer.start();
			odometryDisplay.start();
			navigator.start();
			
			double [] destX = {60,30,30,60};
			double [] destY = {30,30,60,0};
			for (int i = 0; i < destX.length; i ++){
				navigator.travelTo(destX[i],destY[i], false);
				//Wait until we are done moving to location
				while(navigator.isNavigating()){}
			}
		}
			
		
		while (Button.waitForAnyPress() != Button.ID_ESCAPE);
		System.exit(0);
	}
}