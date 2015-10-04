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

		t.drawString("Press anything", 0, 0);
		while(buttonChoice == 0){
			buttonChoice = Button.waitForAnyPress();
		}
			odometer.start();
			odometryDisplay.start();
			navigator.start();
			
			double [] destX = {60,30,30,60};
			double [] destY = {30,30,60,0};
			
			for (int i = 0; i < destX.length; i ++){
				navigator.travelTo(destX[i],destY[i]);
				//Wait until we are done moving to location
				while(navigator.isNavigating()){}
			}
			
			
		
		while (Button.waitForAnyPress() != Button.ID_ESCAPE);
		System.exit(0);
	}
}