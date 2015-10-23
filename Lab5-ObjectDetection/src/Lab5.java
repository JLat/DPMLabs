import lejos.hardware.Button;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.motor.EV3LargeRegulatedMotor;

public class Lab5 {
	private static final EV3LargeRegulatedMotor leftMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("A"));
	private static final EV3LargeRegulatedMotor rightMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("D"));

	private static LCDInfo LCD;

	public static void main(String[] args) {

		try {

			Odometer odo = new Odometer(leftMotor, rightMotor, 30, true);

			// creating the sensors.
			LightSensor lSensor = new LightSensor("S4", "RGB");
			lSensor.start();

			SmoothUSSensor USS = new SmoothUSSensor(10, 10, 15, 50, 0);
			USS.start();

			Scanner scanner = new Scanner(lSensor, USS);
			LCD = new LCDInfo(odo, USS, lSensor);

			Navigation nav = new Navigation(odo, scanner, LCD);

			USLocalizer2 localizer = new USLocalizer2(nav, odo, USS, LCD);

			int buttonChoice;
			LCD.clearAdditionalInfo();
			do {
				LCD.addInfo("OBJ DETECT: LEFT");
				LCD.addInfo("SEARCH OBJ: RIGHT");

				buttonChoice = Button.waitForAnyPress();
			} while (buttonChoice != Button.ID_LEFT && buttonChoice != Button.ID_RIGHT);
			LCD.clearAdditionalInfo();

			if (buttonChoice == Button.ID_LEFT) {
				USS.setParameters(10, 10, 15, 50, 0);
				part1(USS, lSensor, scanner);
			} else {
				USS.setParameters(10, 10, 15, 50, 0);
				localizer.doLocalization(30);
				USS.setParameters(10, 15, 15, 90, 0);
				nav.part2();
			}
			// To avoid US Sensor error to crash brick
		} catch (IllegalArgumentException e) {
			System.exit(0);
		}

	}

	public static void pause() {
		LCD.addInfo("Esc to exit, Right to cont.");

		while (true) {
			int choice = Button.waitForAnyPress();
			if (choice == Button.ID_ESCAPE) {
				System.exit(0);
			} else if (choice == Button.ID_RIGHT) {
				LCD.removeLastAddedInfo();
				break;
			} else {

			}
		}

	}

	public static void part1(SmoothUSSensor USS, LightSensor lSensor, Scanner scanner) {
		LCD.addInfo("D: ");

		USS.setParameters(5, 15, 15, 50, 0);
		//Wait until open space
		while (USS.getProcessedDistance() < 30)
			;

		// 5 tests
		for (int i = 0; i < 5; i++) {
			LCD.clearAdditionalInfo();
			LCD.addInfo("D: ");

			scanner.turnTo(0, false);
			while (USS.getProcessedDistance() >= 10)
				;

			scanner.scan();
			//Scan for block
			if (scanner.blockDetected()) {
				LCD.addInfo("Block Detected");
				if (scanner.blueBlockDetected()) {
					LCD.addInfo("Blue Block");
				} else {
					LCD.addInfo("Wood Block");
				}
			} else {
				LCD.addInfo("No Block");
			}
			//Wait until block is removed
			while (USS.getProcessedDistance() < 20)
				;
		}
		scanner.turnTo(0, false);
		pause();
	}
}