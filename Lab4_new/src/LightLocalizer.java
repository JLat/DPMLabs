import lejos.hardware.Button;
import lejos.hardware.ev3.LocalEV3;
import lejos.robotics.SampleProvider;

public class LightLocalizer {
	private Odometer odo;
	private LightSensor lSensor;
	private Navigation nav;
	public static float ROTATION_SPEED = 30;
	private int lineDifference = 20;
	private LCDInfo lcd;

	// TODO: Change this to find proper distance
	public static double lightSensorDistance = 12.5;

	public LightLocalizer(Odometer odo, SampleProvider colorSensor, float[] colorData, Navigation nav, LCDInfo LCD) {
		this.odo = odo;
		this.lSensor = new LightSensor(colorSensor, colorData, lineDifference);
		this.nav = nav;
		this.lcd = LCD;
	}

	public void doLocalization() {
		this.lSensor.start();
		this.lcd.setSensor(lSensor);
		// initiate required variables.
		double thetaX1 = 0, thetaX2 = 0, thetaY1 = 0, thetaY2 = 0, newX = 0, newY = 0, deltaTheta, deltaThetaX,
				deltaThetaY;
		
		while(!this.lSensor.isCalibrated()){
			// do nothing.
		}
		this.lcd.addInfo("Calib: ", this.lSensor.getWoodValue());
		
		
		pause();

		// Assuming starting with robot facing 0 degrees (not sure if this is
		// correct)
		
		// Rotate,detect 4 lines and collect the data needed
		for (int i = 1; i <= 4; i++) {

			while (!lSensor.seesLine()) {
				nav.setSpeeds(+ROTATION_SPEED, -ROTATION_SPEED);
			}
			// Line has been found, stop rotating
			nav.setSpeeds(0, 0);
			LocalEV3.get().getAudio().systemSound(0);

			if (i % 2 == 0) {
				// Even therefore lines on x axis (horizontal)
				if (thetaX1 == 0) {
					// This is the first line on X axis
					thetaX1 = odo.getAng();
					//this.lcd.addInfo("thetax1: ", thetaX1);
				} else {
					// Second line on x axis

					if (odo.getAng() < thetaX1) {
						// If wraparound occured (360 -> 0) then change original
						// theta to -360 to accomodate
						thetaX1 -= 360;
					}

					thetaX2 = odo.getAng() - thetaX1;
					this.lcd.addInfo("thetaX2: ", thetaX2);
					newY = -lightSensorDistance * Math.cos((thetaX2) / 2);
				}
			} else {
				// Odd therefore lines on y axis (vertical)
				if (thetaY1 == 0) {
					// This is the first line on Y axis
					thetaY1 = odo.getAng();
					//this.lcd.addInfo("thetaY1: ", thetaY1);
				} else {
					// Second line on Y axis

					if (odo.getAng() < thetaY1) {
						// If wraparound occured (360 -> 0) then change original
						// theta to -360 to accomodate
						thetaY1 -= 360;
					}

					thetaY2 = odo.getAng() - thetaY1;
					this.lcd.addInfo("thetaY2: ", thetaY2);
					newX = -lightSensorDistance * Math.cos(thetaY2 / 2);
				}

			}
			while (lSensor.seesLine()) {
				nav.setSpeeds(+ROTATION_SPEED, -ROTATION_SPEED);
			}

		}

		deltaThetaY = 90 - (thetaY1 - 180) + thetaY2 / 2;
		deltaThetaX = 90 - (thetaX1 - 180) + thetaX2 / 2;
		deltaTheta = (deltaThetaY + deltaThetaX) / 2;

		odo.setPosition(new double[] { newX, newY, odo.getAng() + deltaTheta }, new boolean[] { true, true, true });
		pause();
		
		
		nav.travelTo(0, 0);
		nav.turnTo(0, true);
	}

	public void pause() {
		while (Button.waitForAnyPress() != Button.ID_DOWN)
			System.exit(0);
	}

}
