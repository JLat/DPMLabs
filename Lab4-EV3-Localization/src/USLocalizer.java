import lejos.robotics.SampleProvider;

public class USLocalizer {
	public enum LocalizationType { FALLING_EDGE, RISING_EDGE };
	public static float ROTATION_SPEED = 30;

	private Odometer odo;
	private SampleProvider usSensor;
	private float[] usData;
	private LocalizationType locType;
	private Navigation nav;
	private SmoothUSSensor smoothUs;
	private int distanceCap = 80;
	
	public USLocalizer(Odometer odo,  SampleProvider usSensor, float[] usData, LocalizationType locType) {
		this.odo = odo;
		this.usSensor = usSensor;
		this.usData = usData;
		this.locType = locType;
		//added for convenience. 
		this.nav = new Navigation(odo);
		this.smoothUs = new SmoothUSSensor(10,5,5,0,distanceCap);
		this.smoothUs.start();
	}
	
	public void doLocalization() {
		double [] pos = new double [3];
		double angleA, angleB;
		
		if (locType == LocalizationType.FALLING_EDGE) {
			
			
			
			// rotate the robot until it sees no wall
			while(smoothUs.getProcessedDistance()!=distanceCap){
				nav.setSpeeds(+ROTATION_SPEED, -ROTATION_SPEED);
			}
			
			while(smoothUs.getProcessedDistance()==distanceCap){
				nav.setSpeeds(+ROTATION_SPEED, -ROTATION_SPEED);
			}
			
			// the distance has gone down from the cap, therefore a wall was observed.
			
			// stop the motors
			nav.setSpeeds(0, 0);
			// latch the angle
			angleA = wrapAngle(odo.getAng());
			
			// switch direction and wait rotate until it sees no wall
			while(smoothUs.getProcessedDistance()!=distanceCap){
				nav.setSpeeds(-ROTATION_SPEED, +ROTATION_SPEED);
			}
			
			
			

			// keep rotating until the robot sees a wall, then latch the angle
			while(smoothUs.getProcessedDistance()==distanceCap){
				nav.setSpeeds(-ROTATION_SPEED, +ROTATION_SPEED);
			}
			nav.setSpeeds(0, 0);
			angleB = wrapAngle(odo.getAng());
			
			
			
			
			// angleA is clockwise from angleB, so assume the average of the
			// angles to the right of angleB is 45 degrees past 'north'
			
			double delta;
			if(angleA<angleB){
				delta = 45 - (angleB+angleA)/2;
			}else{
				//(angleA>angleB)
				delta = 225 - (angleB+angleA)/2;
			}
			
			// current heading - delta = "real current heading"
			
			// by turning to 180-delta, we are actually turning to face the wall to the left of the robot.
			nav.turnTo(180-delta, true);
			
			smoothUs.clear();
			while(!smoothUs.isFull()){
				//do nothing!
			}
			double distanceToLeftWall = smoothUs.getProcessedDistance();
			
			nav.turnTo(270-delta, true);
			
			smoothUs.clear();
			while(!smoothUs.isFull()){
				//do nothing!
			}
			double distanceToBackWall = smoothUs.getProcessedDistance();
			
			double X = 0 -(15- distanceToLeftWall);
			double Y = 0 -(15- distanceToBackWall);
			
			
			// update the odometer position (example to follow:)
			odo.setPosition(new double [] {X, Y, 270-delta}, new boolean [] {true, true, true});
			
			//turn towards 90 degrees (parallel to left wall)
			nav.turnTo(90, true);
			
			
		} else {
			/*
			 * The robot should turn until it sees the wall, then look for the
			 * "rising edges:" the points where it no longer sees the wall.
			 * This is very similar to the FALLING_EDGE routine, but the robot
			 * will face toward the wall for most of it.
			 */
			
			//
			// FILL THIS IN
			//
		}
	}
	
	private float getFilteredData() {
		usSensor.fetchSample(usData, 0);
		float distance = usData[0];
				
		return distance;
	}
	
	private double wrapAngle(double angle){
		if(angle>=360){
			angle -=360;
		}else if(angle <0){
			angle +=360;
		}
		return angle;
	}

}
