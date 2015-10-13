import lejos.hardware.Button;
import lejos.hardware.ev3.LocalEV3;
import lejos.robotics.SampleProvider;

public class LightLocalizer {
	private Odometer odo;
	private LightSensor lSensor;
	private Navigation nav;
	public static float ROTATION_SPEED = 60;
	private int lineDifference = 30;
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
				deltaThetaY, deltaX=0, deltaY=0;

		while (!this.lSensor.isCalibrated()) {
			// do nothing.
		}
		// this.lcd.addInfo("Calib: ", this.lSensor.getWoodValue());

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

			if (i % 2 == 1) {
				// Even therefore lines on x axis (horizontal)
				if (thetaX1 == 0) {
					// This is the first line on X axis
					thetaX1 = odo.getAng();
					this.lcd.addInfo("thetax1: ", thetaX1);
				} else {
					// Second line on x axis

					// if (odo.getAng() > thetaX1) {
					// // If wraparound occured (360 -> 0) then change original
					// // theta to +360 to accomodate
					// thetaX1 += 360;
					// }
					//
					// thetaX2 = Math.abs(thetaX1 - odo.getAng());

					thetaX2 = odo.getAng();

					deltaX = AngleTraveledFromTo(thetaX1, odo.getAng());
					this.lcd.addInfo("thetaX2: ", thetaX2);

					newY = -lightSensorDistance * Math.cos((Math.toRadians(deltaX)) / 2);
				}
			} else {
				// Odd therefore lines on y axis (vertical)
				if (thetaY1 == 0) {
					// This is the first line on Y axis
					thetaY1 = odo.getAng();
					this.lcd.addInfo("thetaY1: ", thetaY1);
				} else {
					// Second line on Y axis
					thetaY2 = odo.getAng();
					deltaY= AngleTraveledFromTo(thetaY1, odo.getAng());
					
					this.lcd.addInfo("thetaY2: ", thetaY2);
					newX = -lightSensorDistance * Math.cos(Math.toRadians(thetaY2) / 2);
				}

			}
			while (lSensor.seesLine()) {
				nav.setSpeeds(+ROTATION_SPEED, -ROTATION_SPEED);
			}

		}
		nav.setSpeeds(0, 0);

		deltaThetaY = 180 - thetaY2 - deltaY / 2;
		deltaThetaX = 270 - thetaX2 - deltaX / 2;
		deltaTheta = (deltaThetaY + deltaThetaX) / 2;

		odo.setPosition(new double[] { newX, newY, odo.getAng() + deltaTheta }, new boolean[] { true, true, true });
		pause();

		nav.travelTo(0, 0);
		nav.turnTo(0, true);
		pause();
	}

	public void pause() {
		while (Button.waitForAnyPress() != Button.ID_DOWN)
			System.exit(0);
	}

	public double AngleTraveledFromTo(double startingAngle, double endAngle) {
		// assumption made that the angles are decreasing while the robot is
		// moving, i.e. the robot is turning right.
		if(startingAngle<endAngle){
			return 360-(endAngle-startingAngle);
		}else{
			return startingAngle-endAngle;
		}
	}

}
