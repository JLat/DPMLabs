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

		
		final TextLCD t = LocalEV3.get().getTextLCD();
		Odometer odometer = new Odometer(leftMotor,rightMotor, WHEEL_RADIUS, TRACK);
		OdometryCorrection odometryCorrection = new OdometryCorrection(odometer);
		OdometryDisplay odometryDisplay = new OdometryDisplay(odometer,t,odometryCorrection);
		Navigator navigator = new Navigator(odometer,leftMotor,rightMotor,WHEEL_RADIUS,TRACK);


		while(buttonChoice == 0){
			buttonChoice = Button.waitForAnyPress();
		}
			odometer.start();
			odometryDisplay.start();
			odometryCorrection.start();
			navigator.start();
			
		
		while (Button.waitForAnyPress() != Button.ID_ESCAPE);
		System.exit(0);
	}
}