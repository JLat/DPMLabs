import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.motor.EV3MediumRegulatedMotor;


public class Scanner{
	
	public LightSensor lSensor;
	public SmoothUSSensor USS;
	private EV3MediumRegulatedMotor motor = new EV3MediumRegulatedMotor(LocalEV3.get().getPort("B"));
	// ratio between motor tachometer angle and real angle of the sensor.
	public double pConstant = 0.50847;
	private int SPEED = 100;
	private boolean blockDetected, blueBlockDetected;
	
	
	public Scanner(LightSensor lSensor, SmoothUSSensor USS){
		this.lSensor = lSensor;
		this.USS=USS;
		this.motor.resetTachoCount();
		this.motor.flt();
				
	}
	public double getAngle(){
		return this.motor.getTachoCount()*pConstant;
	}
	
	public void turnTo(double angle, boolean detect){
		double currentAngle = this.motor.getTachoCount()*this.pConstant;
		double error = currentAngle - angle;
		while(Math.abs(error)>=1){
			currentAngle = this.motor.getTachoCount()*this.pConstant;
			error = currentAngle - angle;
			
			this.motor.setSpeed(SPEED);
			if(error<0){
				motor.forward();
			}else{
				motor.backward();
			}
			
			// if we need to detect blocks on the way there
			if(detect && this.USS.getProcessedDistance()<=10){
				this.blockDetected=true;
				if(this.lSensor.seesBlueBlock()){
					this.blueBlockDetected=true;
					this.motor.stop();
					LocalEV3.get().getAudio().systemSound(0);
					break;
				}else{
					this.blueBlockDetected=false;
					this.motor.stop();
					LocalEV3.get().getAudio().systemSound(1);
					break;
				}
			}

			
			
		}
		this.motor.stop();
	}
	
	public void setSpeed(int speed){
		this.SPEED = speed;
	}
	
	
	
	public void setMode(String mode){
		if(mode.toLowerCase().equals("scan")){
			
		}
	}
	
	public boolean seesObject(int objectDistanceThreshold){
		if(this.USS.getProcessedDistance()<=objectDistanceThreshold){
			return true;
		}else{
			return false;
		}
	}
	
	public void scan(){
		
		this.blockDetected=false;
		this.blueBlockDetected=false;
		turnTo(-80,false);
		turnTo(80,true);
	}
	
	public boolean blockDetected(){
		return this.blockDetected;
	}
	
	public boolean blueBlockDetected(){
		// blueBlockDetected is only set to true if blockDetected is also true.
		return this.blueBlockDetected;
	}
	
	public int getDistance(){
		return this.USS.getProcessedDistance();
	}
	
	
	
	
	
	
	
	
}