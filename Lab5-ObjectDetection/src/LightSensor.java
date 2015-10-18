import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.port.Port;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.SensorModes;
import lejos.robotics.SampleProvider;

public class LightSensor extends Thread {

	private SensorModes colorSensor;
	private SampleProvider sensor;
	private float[] colorData;

	private double rValue, gValue, bValue;

	public LightSensor(String port, String mode) {
		this.colorSensor = new EV3ColorSensor(LocalEV3.get().getPort(port));
		this.sensor = colorSensor.getMode(mode);
		this.colorData = new float[sensor.sampleSize()];
	}

	public void run() {
		while (true) {

			sensor.fetchSample(colorData, 0);

			this.rValue = colorData[0] * 1024;
			this.gValue = colorData[1] * 1024;
			this.bValue = colorData[2] * 1024;

			

			try {
				Thread.sleep(50);
			} catch (Exception e) {

			}
		}
	}

	public double getRedValue() {
		return this.rValue;
	}

	public double getGreenValue() {
		return this.gValue;
	}

	public double getBlueValue() {
		return this.bValue;
	}
	
	// method waits until a certain amount of data has been properly connected, then, if it sees a block, then it 
	public void seesBlueBlock(){
		
		
		
		
		
		
	}
	
	
	
	

}
