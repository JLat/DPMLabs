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

	public static void main (String [] args){

		@SuppressWarnings("resource")
		SensorModes colorSensor = new EV3ColorSensor(LocalEV3.get().getPort("S2"));
		SampleProvider colorValue = colorSensor.getMode("Red");
		float[] colorData = new float[colorValue.sampleSize()];
		LightSensor lSensor = new LightSensor(colorValue, colorData, 0);
		SmoothUSSensor USS = new SmoothUSSensor(10, 3, 3, 50, 0);
		Odometer odo = new Odometer(leftMotor, rightMotor, 30, true);
		LCDInfo LCD = new LCDInfo(odo);
		int robotState = 1;
		/* Current state of the robot
		 * 0 = TASK COMPLETED
		 * 1 = SEARCHING FOR OBJECTS
		 * 2 = DETECT TYPE OF OBJECT
		 * 3 = GRABBING OBJECT
		 */
		
	
		USS.start();
		lSensor.start(); 
		
		int buttonChoice = 0;
		do {
		LCD.setAddText("OBJECT DETECT:","RIGHT");
		LCD.setAddText("SEARCH OBJECT", "LEFT");

			buttonChoice = Button.waitForAnyPress();
		} while (buttonChoice != Button.ID_LEFT && buttonChoice != Button.ID_RIGHT);
		LCD.clearAdditionalInfo();
		
		if (buttonChoice == Button.ID_RIGHT){
			while (true){
				if (USS.getProcessedDistance() < 10){
				LCD.clearAdditionalInfo();
				LCD.setAddText("","Object Detected");
				if (detectType(lSensor.getValue()) == WOOD_BLOCK)
					LCD.setAddText("","Not Block");
				else
					LCD.setAddText("","Block");
				}
			}
		}
		else
		{
			
			while (robotState == 0){
				
			}
		}
	}

	// asuming RED Mode Color
	public static int detectType(double ColorReading) {
		if (ColorReading < .5)
			return WOOD_BLOCK;
		else
			return STYROFOAM_BLOCK;
	}

}