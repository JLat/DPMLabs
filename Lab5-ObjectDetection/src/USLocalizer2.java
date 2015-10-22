import lejos.hardware.Button;
import lejos.hardware.ev3.LocalEV3;

public class USLocalizer2 {
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

	public USLocalizer2(Navigation nav, Odometer odo, SmoothUSSensor uss, LCDInfo LCD) {
		this.odo = odo;
		// added for convenience.
		this.lcd = LCD;
		this.nav = nav;
		this.uss = uss;
	}

	public void doLocalization(int Cap) {
		distanceCap = Cap;
		double[] pos = new double[3];
		double angleA, angleB, delta;

			// rotate the robot until it sees no wall
			rotateUntilOpen("right");

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
				delta = 40.0 - (angleB + angleA) / 2;
			} else {
				// (angleA>angleB)
				delta = 224.0 - (angleB + angleA) / 2;
			}


		// current odometer heading - delta = real current heading
		// hence, by turning to theta-delta, we are actually turning to theta.

		nav.turnTo(180 - delta, true);
		// clear the recent list of the uss sensor, and wait until the
		// list is full again to latch the distance towards the wall.
		uss.clear();
		while (!uss.isFull()) {
		}
		double distanceToLeftWall = uss.getProcessedDistance();
		LocalEV3.get().getAudio().systemSound(0);
		this.lcd.addInfo("Dleft: ", distanceToLeftWall);

		// turn to face back wall.
		nav.turnTo(270 - delta, true);
		uss.clear();
		while (!uss.isFull()) {
		}
		double distanceToBackWall = uss.getProcessedDistance();
		LocalEV3.get().getAudio().systemSound(0);
		this.lcd.addInfo("Dback: ", distanceToBackWall);

		// set the new position.
		pos[0] = (-26.5 + distanceToLeftWall);
		pos[1] = (-26.5 + distanceToBackWall);
		pos[2] = 270 - 7;

		// update the odometer position
		odo.setPosition(pos, new boolean[] {false,false, true });

		// turn towards 0 degrees (parallel to back wall)
		nav.turnTo(0, true);
		Lab5.pause();
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
		int distance = uss.getProcessedDistance();
		while (distance < distanceCap) {
			nav.setSpeeds(dir * ROTATION_SPEED, -dir * ROTATION_SPEED);
			distance = uss.getProcessedDistance();
		}

		while (distance >= detectionRatio * distanceCap) {
			nav.setSpeeds(dir * ROTATION_SPEED, -dir * ROTATION_SPEED);
			distance = uss.getProcessedDistance();
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

		int distance = uss.getProcessedDistance();

		while (distance < distanceCap) {
			nav.setSpeeds(dir * ROTATION_SPEED, -dir * ROTATION_SPEED);
			distance = uss.getProcessedDistance();
		}
	}


}
