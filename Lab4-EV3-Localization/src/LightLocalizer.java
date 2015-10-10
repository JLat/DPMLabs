import lejos.robotics.SampleProvider;

public class LightLocalizer {
	private Odometer odo;
	private SampleProvider colorSensor;
	private float[] colorData;	
	private Navigation nav;
	public static float ROTATION_SPEED = 30;
	
	// TODO: Change this to find proper distance
	public static int lightSensorDistance = 10; 
	
	public LightLocalizer(Odometer odo, SampleProvider colorSensor, float[] colorData, Navigation nav) {
		this.odo = odo;
		this.colorSensor = colorSensor;
		this.colorData = colorData;
		this.nav = nav;
	}
	
	public void doLocalization() {
		// drive to location listed in tutorial
		// start rotating and clock all 4 gridlines
		// do trig to compute (0,0) and 0 degrees
		// when done travel to (0,0) and turn to 0 degrees
		
		double thetaX1, thetaX2, thetaY1, thetaY2, newX, newY, deltaTheta, deltaThetaX,deltaThetaY;
		
		//Set initial position without adjustment
		nav.travelTo(-5,-5);
		nav.turnTo(0,true);
		
		//Assuming starting with robot facing 0 degrees (not sure if this is correct)
		int i = 0;
		for (i = 0; i < 4; i ++){ // Rotate,detect 4 lines and collect the data neeeded
			while (){ //TODO: detect line
				nav.setSpeeds(+ROTATION_SPEED, -ROTATION_SPEED);
			}
			nav.setSpeeds(0, 0); //Line has been found, stop rotating
			if (i % 2 == 1){ // Odd therefore lines on y axis (vertical)
				if (thetaY1 == 0){ //This is the first line on Y axis
					thetaY1 = odo.getAng();
				}
				else{ // Second line on Y axis
					if (odo.getAng() < thetaY1){ // If wraparound occured (360 -> 0) then change original theta to -360 to accomodate
						thetaY1 -= 360;
					}
					thetaY2 = odo.getAng() - thetaY1;
					 newX = -lightSensorDistance * Math.cos(thetaY2 / 2);
				}
				
			}
			else{ //Even therefore lines on x axis (horizontal)
				if (thetaX1 == 0){ //This is the first line on Y axis
					thetaX1 = odo.getAng();
				}
				else{ // Second line on x axis
					if (odo.getAng() < thetaX1){ // If wraparound occured (360 -> 0) then change original theta to -360 to accomodate
						thetaX1 -= 360;
					}
					thetaX2 = odo.getAng() - thetaX1;
					 newY = -lightSensorDistance * Math.cos(thetaX2 / 2);
				}
			}
		}
		deltaThetaY = 90  - (thetaY1 - 180) + thetaY2/2;
		deltaThetaX = 90  - (thetaX1 - 180) + thetaX2/2;
		deltaTheta = (deltaThetaY + deltaThetaX) / 2;
		
		odo.setPosition(new double[] {newX, newY, odo.getAng() + deltaTheta}, new boolean [] {true,true,true});
		nav.travelTo(0,0);
		nav.turnTo(0,true);
	}

}
