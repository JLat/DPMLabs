import lejos.hardware.Button;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.motor.EV3LargeRegulatedMotor;


public class Lab5 {
	private static final EV3LargeRegulatedMotor leftMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("A"));
	private static final EV3LargeRegulatedMotor rightMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("D"));

	
	private static LCDInfo LCD;

	public static void main(String[] args) {
		
		
		
		
		try{
			
		Odometer odo = new Odometer(leftMotor, rightMotor, 30, true);
		

		
		//creating the sensors.
		LightSensor lSensor = new LightSensor("S4", "RGB");
		lSensor.start();
		
		SmoothUSSensor USS = new SmoothUSSensor(10, 10, 15, 50, 0);
		USS.start();

		Scanner scanner = new Scanner(lSensor, USS);
		LCD = new LCDInfo(odo, USS, lSensor);
		
		Navigation nav = new Navigation(odo,scanner, LCD);
		
		
		USLocalizer localizer = new USLocalizer(nav, odo, USS, LCD);
		localizer.doLocalization(40);
		pause();
		
		
		USS.setParameters(10, 10, 15, 50, 0);
		
		//part1(USS,lSensor,scanner);
		nav.part2();
		
		int buttonChoice;
		do {
			LCD.addInfo("OBJECT DETECT: RIGHT");
			LCD.addInfo("SEARCH OBJECT: LEFT");

			buttonChoice = Button.waitForAnyPress();
		} while (buttonChoice != Button.ID_LEFT && buttonChoice != Button.ID_RIGHT);
		LCD.clearAdditionalInfo();
		
		
		}catch(IllegalArgumentException e){
			System.exit(0);
		}
		
	}
	public static void pause(){
		LCD.addInfo("Esc to exit, Right to cont.");
		
		while(true){
			int choice = Button.waitForAnyPress();
			if(choice==Button.ID_ESCAPE){
				System.exit(0);
			}else if(choice == Button.ID_RIGHT){
				LCD.removeInfo("Esc to exit, Right to cont.");
				break;
			}else{
				
			}
		}
		
	}

	
	
	public static void part1(SmoothUSSensor USS, LightSensor lSensor, Scanner scanner){
		LCD.addInfo("D: ");
		
		
		USS.setParameters(5,15,15,50,0);
		while(USS.getProcessedDistance()<30);
		
		for(int i=0; i<2; i++){
			LCD.clearAdditionalInfo();
			LCD.addInfo("D: ");
			
			scanner.turnTo(0,false);
			while(USS.getProcessedDistance()>=10);
			
			scanner.scan();
			if(scanner.blockDetected()){
				LCD.addInfo("Block Detected");
				if(scanner.blueBlockDetected()){
					LCD.addInfo("Blue Block");
				}else{
					LCD.addInfo("Wood Block");
				}
			}else{
				LCD.addInfo("No Block");
			}
			while(USS.getProcessedDistance()<20);
		}
		scanner.turnTo(0, false);
		pause();
	}
}