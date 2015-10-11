import lejos.hardware.ev3.LocalEV3;
import lejos.robotics.SampleProvider;

public class USLocalizer {
	public enum LocalizationType {
		FALLING_EDGE, RISING_EDGE
	};

	public static float ROTATION_SPEED = 30;

	private Odometer odo;
	private SampleProvider usSensor;
	private float[] usData;
	private LocalizationType locType;
	private Navigation nav;
	private SmoothUSSensor smoothUs;
	private LCDInfo lcd;
	private int distanceCap = 40;
	private int wallThreshold = 30;

	public USLocalizer(Odometer odo, SampleProvider usSensor, float[] usData, LocalizationType locType, LCDInfo LCD) {
		this.odo = odo;
		this.usSensor = usSensor;
		this.usData = usData;
		this.locType = locType;
		// added for convenience.
		this.lcd = LCD;
		this.nav = new Navigation(odo);
		this.smoothUs = new SmoothUSSensor(10, 5, 5, 0, distanceCap);
		this.smoothUs.start();
	}

	public void doLocalization() {
		double[] pos = new double[3];
		double angleA, angleB;

		if (locType == LocalizationType.FALLING_EDGE) {
			lcd.setSensor(smoothUs);

			// rotate the robot until it sees no wall
			while (smoothUs.getProcessedDistance() != distanceCap) {
				nav.setSpeeds(+ROTATION_SPEED, -ROTATION_SPEED);
			}
			while (smoothUs.getProcessedDistance() == distanceCap) {
				nav.setSpeeds(+ROTATION_SPEED, -ROTATION_SPEED);
			}

			// when the above loop exits, the distance has gone down from the
			// cap, therefore a wall was observed.

			// stop the motors
			nav.setSpeeds(0, 0);
			// latch the angle
			LocalEV3.get().getAudio().systemSound(0);
			angleA = wrapAngle(odo.getAng());

			// switch direction and rotate until it sees no wall
			while (smoothUs.getProcessedDistance() != distanceCap) {
				nav.setSpeeds(-ROTATION_SPEED, +ROTATION_SPEED);
			}

			// keep rotating until the robot sees a wall, then latch the angle
			while (smoothUs.getProcessedDistance() == distanceCap) {
				nav.setSpeeds(-ROTATION_SPEED, +ROTATION_SPEED);
			}
			nav.setSpeeds(0, 0);
			LocalEV3.get().getAudio().systemSound(0);
			angleB = wrapAngle(odo.getAng());

			double delta;
			if (angleA < angleB) {
				// TODO: validate those values;
				delta = 45.0 - (angleB + angleA) / 2;
			} else {
				// (angleA>angleB)
				delta = 225.0 - (angleB + angleA) / 2;
			}

			// current heading - delta = "real current heading"
			// hence, by turning to 180-delta, we are actually turning to face
			// the left wall
			nav.turnTo(180 - delta, true);

			// clear the recent list of the smoothUs sensor, and wait until the
			// list is full again to latch the distance towards the wall.
			smoothUs.clear();
			while (!smoothUs.isFull()) {
			}
			double distanceToLeftWall = smoothUs.getProcessedDistance();
			LocalEV3.get().getAudio().systemSound(0);

			// turn to face back wall.
			nav.turnTo(270 - delta, true);
			smoothUs.clear();
			while (!smoothUs.isFull()) {
			}
			double distanceToBackWall = smoothUs.getProcessedDistance();
			LocalEV3.get().getAudio().systemSound(0);

			pos[0] = -15 + distanceToLeftWall;
			pos[1] = -15 + distanceToBackWall;
			pos[2] = 270 - delta;

			// update the odometer position
			odo.setPosition(pos, new boolean[] { true, true, true });

			// turn towards 90 degrees (parallel to left wall)
			nav.turnTo(90, true);

		} else {
			/*
			 * The robot should turn until it sees the wall, then look for the
			 * "rising edges:" the points where it no longer sees the wall. This
			 * is very similar to the FALLING_EDGE routine, but the robot will
			 * face toward the wall for most of it.
			 */
			lcd.setSensor(smoothUs);
			lcd.setAddText("PD: ", "" + smoothUs.getProcessedDistance());

			// rotate the robot until it sees a wall
			while (smoothUs.getProcessedDistance() == distanceCap) {
				nav.setSpeeds(+ROTATION_SPEED, -ROTATION_SPEED);
			}
			while (smoothUs.getProcessedDistance() != distanceCap) {
				nav.setSpeeds(+ROTATION_SPEED, -ROTATION_SPEED);
			}
			// when the above loop exits, the distance has gone back up to the cap, therefore it no longer sees the wall.

			// stop the motors
			nav.setSpeeds(0, 0);
			LocalEV3.get().getAudio().systemSound(0);
			// latch the angle
			angleA = wrapAngle(odo.getAng());

			// switch direction and rotate until it sees no wall again
			while (smoothUs.getProcessedDistance() == distanceCap) {
				nav.setSpeeds(-ROTATION_SPEED, +ROTATION_SPEED);
			}
			// keep rotating until the robot no longer sees a wall, then latch the angle
			while (smoothUs.getProcessedDistance() != distanceCap) {
				nav.setSpeeds(-ROTATION_SPEED, +ROTATION_SPEED);
			}
			
			
			nav.setSpeeds(0, 0);
			LocalEV3.get().getAudio().systemSound(0);
			angleB = wrapAngle(odo.getAng());

			double delta;
			if (angleA < angleB) {
				// TODO: validate those values;
				delta = 45.0 - (angleB + angleA) / 2;
			} else {
				// (angleA>angleB)
				delta = 225.0 - (angleB + angleA) / 2;
			}

			// current heading - delta = "real current heading"
			// hence, by turning to 180-delta, we are actually turning to face
			// the left wall
			nav.turnTo(180 - delta, true);

			// clear the recent list of the smoothUs sensor, and wait until the
			// list is full again to latch the distance towards the wall.
			smoothUs.clear();
			while (!smoothUs.isFull()) {
			}
			double distanceToLeftWall = smoothUs.getProcessedDistance();
			LocalEV3.get().getAudio().systemSound(0);

			// turn to face back wall.
			nav.turnTo(270 - delta, true);
			smoothUs.clear();
			while (!smoothUs.isFull()) {
			}
			double distanceToBackWall = smoothUs.getProcessedDistance();
			LocalEV3.get().getAudio().systemSound(0);

			pos[0] = -15 + distanceToLeftWall;
			pos[1] = -15 + distanceToBackWall;
			pos[2] = 270 - delta;

			// update the odometer position
			odo.setPosition(pos, new boolean[] { true, true, true });

			// turn towards 90 degrees (parallel to left wall)
			nav.turnTo(90, true);
		}
	}

//	private float getFilteredData() {
//		usSensor.fetchSample(usData, 0);
//		float distance = usData[0];
//
//		return distance;
//	}

	private double wrapAngle(double angle) {
		if (angle >= 360) {
			angle -= 360;
		} else if (angle < 0) {
			angle += 360;
		}
		return angle;
	}

}
