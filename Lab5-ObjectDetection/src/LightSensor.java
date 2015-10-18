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
			sensor.fetchSample(colorData, 1);
			sensor.fetchSample(colorData, 2);

			this.rValue = colorData[0] * 1024;
			this.gValue = colorData[1] * 1024;
			this.bValue = colorData[2] * 1024;

			// TODO: Ok here I'll explain the way someone else showed me to do
			// it, and it seems to work flawlessly for him.
			/*
			 * Basicly when you hit a Blue block, the ratio of blue compared to
			 * the other modes is a lot higher. I'm super tired I'm gonna head
			 * home soon.
			 * 
			 * I'll build a function that automaticly sets some boolean for what
			 * kind of block it is currently seeing.
			 * 
			 * 
			 * 
			 */

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

}
