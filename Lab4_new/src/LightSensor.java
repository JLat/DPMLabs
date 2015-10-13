import java.util.LinkedList;

import lejos.robotics.SampleProvider;

public class LightSensor extends Thread {
	// simple class that takes care of continuously fetching samples from the
	// light sensor.

	private float[] data;
	private SampleProvider sensor;
	private boolean calibrated;
	private double woodValue, currentValue, lineDifference;
	private int calibrationCounter;
	private LinkedList<Double> recent = new LinkedList<Double>();

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
			
			smoothValue();

			try {
				Thread.sleep(50);
			} catch (Exception e) {
			} // Poor man's timed sampling
		}
	}
	
	public void smoothValue(){
		this.recent.addLast(this.currentValue);
		if(this.recent.size()>3){
			this.recent.removeFirst();
		}
		this.currentValue = getAverage(recent);
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
			calibrationCounter++;
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
	
	
	public double getAverage(LinkedList<Double> list){
		if(list.isEmpty()){
			return 0;
		}else{
			double sum=0;
			for(Double d: list){
				sum +=d;
			}
			return sum/list.size();
		}
	}
	
	public double getWoodValue(){
		return this.woodValue;
	}
	
	public boolean isCalibrated(){
		return this.calibrated;
	}
}
