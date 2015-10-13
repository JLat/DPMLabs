import lejos.hardware.Button;
import lejos.hardware.ev3.LocalEV3;

public class USLocalizer {
	public enum LocalizationType {
		FALLING_EDGE, RISING_EDGE
	};

	public static float ROTATION_SPEED = 30;

	private Odometer odo;
	private LocalizationType locType;
	private Navigation nav;
	private SmoothUSSensor smoothUs;
	private LCDInfo lcd;
	private int distanceCap = 50;
	private double detectionRatio = 0.7;

	public USLocalizer(Odometer odo, LocalizationType locType, LCDInfo LCD) {
		this.odo = odo;
		this.locType = locType;
		// added for convenience.
		this.lcd = LCD;
		this.nav = new Navigation(odo);
		this.smoothUs = new SmoothUSSensor(10, 3, 3, distanceCap, 0);
		this.smoothUs.start();
	}

	public void doLocalization() {
		double[] pos = new double[3];
		double angleA, angleB, delta;
		lcd.setSensor(smoothUs);

		if (locType == LocalizationType.FALLING_EDGE) {

			// rotate the robot until it sees no wall
			rotateUntilOpen("right");

			//this.lcd.addTempMessage("Open Space!",3);
			rotateUntilWall("right");
			// a wall was observed.

			// stop the motors and play a sound.
			nav.setSpeeds(0, 0);
			LocalEV3.get().getAudio().systemSound(0);
			// latch the angle
			angleA = odo.getAng();

			// add the angle info on the screen.
			this.lcd.addInfo("angleA: ", angleA);

			// switch direction and rotate until it sees no wall
			rotateUntilOpen("left");

			// keep rotating until the robot sees a wall, then latch the angle
			rotateUntilWall("left");

			nav.setSpeeds(0, 0);
			LocalEV3.get().getAudio().systemSound(0);
			angleB = odo.getAng();

			// add the value of the angle to the LCD.
			this.lcd.addInfo("angleB: ", angleB);

			if (angleA < angleB) {

				delta = 45.0 - (angleB + angleA) / 2;
			} else {
				// (angleA>angleB)
				delta = 224.0 - (angleB + angleA) / 2;
			}

		} else {
			/*
			 * The robot should turn until it sees the wall, then look for the
			 * "rising edges:" the points where it no longer sees the wall. This
			 * is very similar to the FALLING_EDGE routine, but the robot will
			 * face toward the wall for most of it.
			 */

			// rotate the robot until it sees a wall
			rotateUntilWall("right");
			rotateUntilOpen("right");
			// a rising edge was detected.

			// stop the motors
			nav.setSpeeds(0, 0);
			// latch the angle
			LocalEV3.get().getAudio().systemSound(0);
			angleA = odo.getAng();

			this.lcd.addInfo("angleA: ", angleA);

			// switch direction and rotate until it doesnt see a wall anymore.
			rotateUntilWall("left");
			rotateUntilOpen("left");

			nav.setSpeeds(0, 0);
			LocalEV3.get().getAudio().systemSound(0);
			angleB = odo.getAng();

			// add the value of the angle to the LCD.
			this.lcd.addInfo("angleB: ", angleB);

			if (angleA > angleB) {

				delta = 40.0 - (angleB + angleA) / 2;
			} else {
				// (angleA<angleB)
				delta = 220.0 - (angleB + angleA) / 2;
			}

		}

		// current odometer heading - delta = real current heading
		// hence, by turning to theta-delta, we are actually turning to theta.

		nav.turnTo(180 - delta, true);
		// clear the recent list of the smoothUs sensor, and wait until the
		// list is full again to latch the distance towards the wall.
		smoothUs.clear();
		while (!smoothUs.isFull()) {
		}
		double distanceToLeftWall = smoothUs.getProcessedDistance();
		LocalEV3.get().getAudio().systemSound(0);
		this.lcd.addInfo("Dleft: ", distanceToLeftWall);

		// turn to face back wall.
		nav.turnTo(270 - delta, true);
		smoothUs.clear();
		while (!smoothUs.isFull()) {
		}
		double distanceToBackWall = smoothUs.getProcessedDistance();
		LocalEV3.get().getAudio().systemSound(0);
		this.lcd.addInfo("Dback: ", distanceToBackWall);

		// set the new position.
		pos[0] = (-26.5 + distanceToLeftWall);
		pos[1] = (-26.5 + distanceToBackWall);
		pos[2] = 270;

		// update the odometer position
		odo.setPosition(pos, new boolean[] { true, true, true });

		// turn towards 0 degrees (parallel to back wall)
		nav.turnTo(0, true);
		pause();
		this.lcd.clearAdditionalInfo();

	}

	private void rotateUntilWall(String direction) {
		int dir = 0;
		if (direction.equals("left")) {
			dir = -1;
		} else if (direction.equals("right")) {
			dir = 1;
		}
		//
		int distance = smoothUs.getProcessedDistance();
		while (distance < distanceCap) {
			nav.setSpeeds(dir * ROTATION_SPEED, -dir * ROTATION_SPEED);
			distance = smoothUs.getProcessedDistance();
		}
		
//		long counter = System.currentTimeMillis();
//		long delta = System.currentTimeMillis()-counter;
		
		while (distance >= detectionRatio * distanceCap// && delta < 500
				) {
			nav.setSpeeds(dir * ROTATION_SPEED, -dir * ROTATION_SPEED);
			distance = smoothUs.getProcessedDistance();
			//delta = System.currentTimeMillis()-counter;
		}
	}

	private void rotateUntilOpen(String direction) {
		int dir = 0;
		if (direction.equals("left")) {
			dir = -1;
		} else if (direction.equals("right")) {
			dir = 1;
		}

		// rotate the robot until it sees open space.

		// int distance = smoothUs.getProcessedDistance();
		int distance = smoothUs.getProcessedDistance();
		
		while (distance < distanceCap) {
			nav.setSpeeds(dir * ROTATION_SPEED, -dir * ROTATION_SPEED);
			distance = smoothUs.getProcessedDistance();
			//delta = System.currentTimeMillis()-counter;
		}		
	}

	public void pause() {
		while (Button.waitForAnyPress() != Button.ID_DOWN)
			System.exit(0);
	}

}
