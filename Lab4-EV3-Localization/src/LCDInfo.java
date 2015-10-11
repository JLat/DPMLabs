import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.lcd.TextLCD;
import lejos.utility.Timer;
import lejos.utility.TimerListener;

public class LCDInfo implements TimerListener {
	public static final int LCD_REFRESH = 100;
	private Odometer odo;
	private Timer lcdTimer;
	private String additionalTextLabel = " ";
	private String additionalText = " ";
	private String source = " ";
	private TextLCD LCD = LocalEV3.get().getTextLCD();
	private SmoothUSSensor USS;
	private LightSensor lSensor;

	// arrays for displaying data
	private double[] pos;

	public LCDInfo(Odometer odo) {
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
		LCD.drawInt((int) (pos[0] * 10), 3, 0);
		LCD.drawInt((int) (pos[1] * 10), 3, 1);
		LCD.drawInt((int) pos[2], 3, 2);

		// added a customizable new text field for USS or color data.
		updateValue();
		LCD.drawString(additionalTextLabel, 0, 3);
		LCD.drawString(additionalText, 3, additionalTextLabel.length());
	}

	public void setSensor(SmoothUSSensor Uss) {
		this.USS = Uss;
		this.source = "USS";
	}

	public void setSensor(LightSensor lSensor) {
		this.lSensor = lSensor;
		this.source = "lSensor";
	}

	// update the additional info value using its source.
	public void updateValue() {
		if (this.source.equals("USS")) {
			setAddText("D: ", "" + USS.getProcessedDistance());
		} else if (this.source.equals("lSensor")) {
			setAddText("L: ", "" + lSensor.getValue());
		}else{
			setAddText(" "," ");
		}
	}

	public void setAddText(String label, String text) {
		this.additionalTextLabel = label;
		this.additionalText = text;

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

	public void setUSS(SmoothUSSensor Uss) {
		this.USS = Uss;
	}

}
