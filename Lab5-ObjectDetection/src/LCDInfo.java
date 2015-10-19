import java.util.ArrayList;

import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.lcd.TextLCD;
import lejos.utility.Timer;
import lejos.utility.TimerListener;

public class LCDInfo implements TimerListener {
	public static final int LCD_REFRESH = 100;
	private Odometer odo;
	private Timer lcdTimer;
	private TextLCD LCD = LocalEV3.get().getTextLCD();
	private SmoothUSSensor USS;
	private LightSensor lSensor;
	private String lastAdded = " ";

	// arrays for displaying data
	private double[] pos;
	private ArrayList<String> additionalInfo = new ArrayList<String>();

	public LCDInfo(Odometer odo, SmoothUSSensor USS, LightSensor lSensor) {
		this.USS = USS;
		this.lSensor = lSensor;
		this.odo = odo;
		this.lcdTimer = new Timer(LCD_REFRESH, this);

		// initialize the arrays for displaying data
		pos = new double[3];

		// start the timer
		lcdTimer.start();
	}

	public void timedOut() {
		odo.getPosition(pos);
		LCD.clear();
		LCD.drawString("X: ", 0, 0);
		LCD.drawString("Y: ", 0, 1);
		LCD.drawString("H: ", 0, 2);
		LCD.drawString(formattedDoubleToString((pos[0]), 2), 3, 0);
		LCD.drawString(formattedDoubleToString((pos[1]), 2), 3, 1);
		LCD.drawString(formattedDoubleToString((pos[2]), 2), 3, 2);

		
		int i = 3;

		// display all additional information.

		// in order to avoid ConcurrentModificationException, we iterate over a
		// copy of the object such that the iterator doesnt throw an exception.
		ArrayList<String> temp = new ArrayList<String>(additionalInfo);
		
		
		//TODO: Joel please take a look at this just so you're not confused when trying to add new info later on.
		for (String element : temp) {
			// if the element contains "D: ", then it represent the ultrasonic Distance value. we update the value.
			if(element.contains("D: ")){
				element = "D: "+(this.USS.getProcessedDistance());
				LCD.drawString(element,0, i);
			}else if(element.contains("R: ")){
				element = "R: "+formattedDoubleToString((this.lSensor.getRedValue()),2);
				LCD.drawString(element,0, i);
			}else if(element.contains("G: ")){
				element = "G: "+formattedDoubleToString((this.lSensor.getGreenValue()),2);
				LCD.drawString(element,0, i);
			}else if(element.contains("B: ")){
				element = "B: "+formattedDoubleToString((this.lSensor.getBlueValue()),2);
				LCD.drawString(element,0, i);
			}else{
				if(element.contains(",")){
					String[] parts = element.split(",");
					String label = parts[0];
					String data = parts[1];
					LCD.drawString(label, 0, i);
					LCD.drawString(data, label.length(), i);
				}else{
					LCD.drawString(element, 0, i);
				}
			}
			
			i++;
		}
	}
	
	//Add info to be displayed onto screen
	public void addInfo(String Label, double info) {
		String text = Label + "," + formattedDoubleToString(info, 2);
		this.additionalInfo.add(text);
		this.lastAdded=text;
	}
	
	public void addInfo(String info) {
		this.additionalInfo.add(info);
		this.lastAdded=info;
	}
	public void removeInfo(String info){
		for(String element: this.additionalInfo){
			if(element.toLowerCase().contains(info.toLowerCase())){
				this.additionalInfo.remove(element);
			}
		}
	}
	public void removeLastAddedInfo(){
		if(this.lastAdded!=" " && this.additionalInfo.contains(lastAdded)){
			this.additionalInfo.remove(lastAdded);
		}
	}

	//Clear additional information on lcd display
	public void clearAdditionalInfo() {
		this.additionalInfo.clear();
	}
	

	private static String formattedDoubleToString(double x, int places) {
		String result = "";
		String stack = "";
		long t;

		// put in a minus sign as needed
		if (x < 0.0)
			result += "-";

		// put in a leading 0
		if (-1.0 < x && x < 1.0)
			result += "0";
		else {
			t = (long) x;
			if (t < 0)
				t = -t;

			while (t > 0) {
				stack = Long.toString(t % 10) + stack;
				t /= 10;
			}

			result += stack;
		}

		// put the decimal, if needed
		if (places > 0) {
			result += ".";

			// put the appropriate number of decimals
			for (int i = 0; i < places; i++) {
				x = Math.abs(x);
				x = x - Math.floor(x);
				x *= 10.0;
				result += Long.toString((long) x);
			}
		}

		return result;
	}

}
