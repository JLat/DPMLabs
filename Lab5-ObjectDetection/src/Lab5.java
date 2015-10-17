import lejos.hardware.Button;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.SensorModes;
import lejos.robotics.SampleProvider;

public class Lab5 {
	private static final EV3LargeRegulatedMotor leftMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("A"));
	private static final EV3LargeRegulatedMotor rightMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("D"));

	private static final int WOOD_BLOCK = 0;
	private static final int STYROFOAM_BLOCK = 1;

	public static void main(String[] args) {

		@SuppressWarnings("resource")
		SensorModes colorSensor = new EV3ColorSensor(LocalEV3.get().getPort("S2"));
		SampleProvider colorValue = colorSensor.getMode("Red");
		float[] colorData = new float[colorValue.sampleSize()];
		LightSensor lSensor = new LightSensor(colorValue, colorData, 0);
		SmoothUSSensor USS = new SmoothUSSensor(10, 3, 3, 50, 0);
		Odometer odo = new Odometer(leftMotor, rightMotor, 30, true);
		LCDInfo LCD = new LCDInfo(odo);
		int robotState = 1;
		/*
		 * Current state of the robot 0 = TASK COMPLETED 1 = SEARCHING FOR
		 * OBJECTS 2 = DETECT TYPE OF OBJECT 3 = GRABBING OBJECT
		 * 4 = NAVIGATE TO CORNER
		 */

		USS.start();
		lSensor.start();

		int buttonChoice = 0;
		do {
			LCD.setAddText("OBJECT DETECT:", "RIGHT");
			LCD.setAddText("SEARCH OBJECT", "LEFT");

			buttonChoice = Button.waitForAnyPress();
		} while (buttonChoice != Button.ID_LEFT && buttonChoice != Button.ID_RIGHT);
		LCD.clearAdditionalInfo();

		
		//Lab Part 1
		if (buttonChoice == Button.ID_RIGHT) {
			while (true) {
				//If object is place in front of the sensor
				if (USS.getProcessedDistance() < 10) {
					
					//Clear screen and print object detected
					LCD.clearAdditionalInfo();
					LCD.setAddText("", "Object Detected");
					
					//Determine if it is wood block or Styrofoam
					if (detectType(lSensor.getValue()) == WOOD_BLOCK)
						LCD.setAddText("", "Not Block");
					else
						LCD.setAddText("", "Block");
				}
				//Option to escape, waits 1 sec to escape
				if (Button.waitForAnyPress(1000) == Button.ID_ESCAPE){
					System.exit(0);
				}
			}
		}
		//Lab Part 2
		else {
			//LOCALIZE
			
			//TODO: Just really basic ideas, we can do all these steps with seperate classes/methods to keep this area simple
			
			//While not finished
			while (robotState == 0) {
				
				//Search for object
				if (robotState == 1) {
					//Navigate field searching for object
					//Change to state 2 if object found
				} 
				//Approach and Determine type of object
				else if (robotState == 2) {
					//Approach Object
					//Check color of object
					//Change to state 3 if stryofoam, other state 1
				}
				
				//Grab object
				else if (robotState == 3) {
					//Grab object
					//Change to state 4
				}
				
				//Navigate to corner
				else if(robotState ==4){
					//Navigate to top right corner
				}
			}
		}
	}

	// asuming RED Mode Color
	public static int detectType(double ColorReading) {
		// If Color reading relates to Wood Block
		if (ColorReading < 50)
			return WOOD_BLOCK;
		else
			return STYROFOAM_BLOCK;
	}

}