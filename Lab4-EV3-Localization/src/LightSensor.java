import lejos.robotics.SampleProvider;

public class LightSensor extends Thread {
	// simple class that takes care of continuously fetching samples from the
	// light sensor.

	private float[] data;
	private SampleProvider sensor;
	private boolean calibrated;
	private double woodValue, currentValue, lineDifference;
	private int calibrationCounter;

	public LightSensor(SampleProvider sensor, float[] data, int lineThreshold) {
		this.sensor = sensor;
		this.data = data;
		this.lineDifference = lineThreshold;
	}

	public void run() {
		while (true) {

			if (!calibrated)
				calibrate();

			sensor.fetchSample(data, 0);
			currentValue = 100 * data[0];

			try {
				Thread.sleep(50);
			} catch (Exception e) {
			} // Poor man's timed sampling
		}
	}

	public double getValue() {
		return this.currentValue;
	}

	// calibrates the sensor (set the woodValue to the average of the 10 first
	// values);
	public void calibrate() {
		calibrationCounter = 0;
		double temp = 0;
		while (calibrationCounter < 10) {
			sensor.fetchSample(data, 0);
			temp += 100 * data[0];
		}
		this.woodValue = temp / 10;
		this.calibrated = true;
	}

	public boolean seesLine() {
		if (calibrated) {
			return woodValue - currentValue > lineDifference;
		}
		return false;
	}

}
