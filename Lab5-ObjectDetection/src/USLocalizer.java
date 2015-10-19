import lejos.hardware.Button;
import lejos.hardware.ev3.LocalEV3;

public class USLocalizer {
	public enum LocalizationType {
		FALLING_EDGE, RISING_EDGE
	};

	public static float ROTATION_SPEED = 60;

	private Odometer odo;
	private LocalizationType locType;
	private Navigation nav;
	private SmoothUSSensor uss;
	private LCDInfo lcd;
	private int distanceCap = 30;
	private double detectionRatio = 0.7;

	public USLocalizer(Navigation nav, Odometer odo, SmoothUSSensor uss, LCDInfo LCD) {
		this.odo = odo;
		// added for convenience.
		this.lcd = LCD;
		this.nav = nav;
		this.uss = uss;
	}

	public void doLocalization(int distanceCap) {
		
//		//used to calibrate the Odometer.
//		for(int i=0; i<1; i++){
//			this.nav.travelTo(0, 0);
//			this.nav.travelTo(0, 60);
//			this.nav.travelTo(60, 60);
//			this.nav.travelTo(60, 0);
//			this.nav.travelTo(0, 0);
//			this.nav.turnTo(90, true);
//		}
//		pause();
		
		
		
		
		
		this.distanceCap = distanceCap;
		double[] pos = new double[3];
		double angleA, angleB, delta;
		this.lcd.addInfo("D: ");

		uss.setParameters(10, 10, 10, 50, 0);

		nav.setSpeeds(ROTATION_SPEED, -ROTATION_SPEED);

		// dont consider latching the A angle before the robot has successfully
		// faced open space.
		while (uss.getProcessedDistance() < 45)
			;

		while (uss.getProcessedDistance() >= distanceCap)
			;

		// theta A was found.
		LocalEV3.get().getAudio().systemSound(0);
		angleA = odo.getAng();
		

		while (true) {
			
			double difference = differenceBetweenAngles(angleA, odo.getAng());
			if (uss.getProcessedDistance() >= distanceCap) {

				
				//TODO: double check with me that this method correctly calculates a minimum 100 degrees between A and B.
				if (difference < 100) {
					// the first angle A was just a block, set the new angleA.
					
//					LocalEV3.get().getAudio().systemSound(0);
					angleA = odo.getAng();
					
				} else {
					// if the angles were spaced out enough, this is the true B.
					LocalEV3.get().getAudio().systemSound(0);
					angleB = odo.getAng();
					this.lcd.addInfo("Angle_a: " + angleA);
					this.lcd.addInfo("Angle_b: " + angleB);
					this.lcd.addInfo("diff: " + difference);
					break;
				}
			}
			
		}

		// current odometer heading - delta = real current heading
		// hence, by turning to theta-delta, we are actually turning to theta.

		if (angleA > angleB) {
			delta = 211 - (angleA + angleB) / 2;
		} else {
			delta = 33 - (angleA + angleB) / 2;
		}

		lcd.addInfo("Delta: " + delta);
		nav.turnTo(90 + delta, true);
		// set the new position.
		pos[0] = (0);
		pos[1] = (0);
		pos[2] = 0;

		// update the odometer position
		odo.setPosition(pos, new boolean[] { true, true, true });

		// turn towards 0 degrees (parallel to back wall)
//		nav.turnTo(0, true);
		pause();
		this.lcd.clearAdditionalInfo();

	}

	public void pause() {
		Lab5.pause();
	}

	public double differenceBetweenAngles(double angle1, double angle2) {

		if (angle1 > angle2) {
			return (angle1 - angle2);
		} else if (angle1 < angle2) {
			return 360 - (angle2 - angle1);
		} else {
			return 0;
		}
	}

}
