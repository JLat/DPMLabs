package wallFollower;
import lejos.hardware.motor.*;

public class BangBangController implements UltrasonicController{
	private final int bandCenter, bandwidth;
	private final int motorLow, motorHigh;
	private int distance;
	private EV3LargeRegulatedMotor leftMotor, rightMotor;
	
	public BangBangController(EV3LargeRegulatedMotor leftMotor, EV3LargeRegulatedMotor rightMotor,
							  int bandCenter, int bandwidth, int motorLow, int motorHigh) {
		//Default Constructor
		this.bandCenter = bandCenter;
		this.bandwidth = bandwidth;
		this.motorLow = motorLow;
		this.motorHigh = motorHigh;
		this.leftMotor = leftMotor;
		this.rightMotor = rightMotor;
		leftMotor.setSpeed(20);				// Start robot moving forward
		rightMotor.setSpeed(20);
		leftMotor.forward();
		rightMotor.forward();
	}
	
	@Override
	public void processUSData(int distance) {
		this.distance = distance;
		// TODO: process a movement based on the us distance passed in (BANG-BANG style)
		
		
		if(distance<bandCenter){
			// too close, turn right
			
			if(distance <15){
				rightMotor.rotate(-50, false);
				rightMotor.flt();
			
			}else{
				leftMotor.setSpeed(motorHigh);
				rightMotor.setSpeed(motorLow);
				leftMotor.forward();
				rightMotor.forward();
			}
		}else if(distance>=(bandCenter) && distance < (bandCenter+bandwidth)){
			
			leftMotor.setSpeed(motorLow);
			rightMotor.setSpeed(motorHigh);
			leftMotor.forward();
			rightMotor.forward();
			
			
		}else{
			// too far, turn left
			
			
			leftMotor.setSpeed(motorLow);
			rightMotor.setSpeed(motorHigh);
			leftMotor.forward();
			rightMotor.forward();
			
		}
	}

	@Override
	public int readUSDistance() {
		return this.distance;
	}
}
