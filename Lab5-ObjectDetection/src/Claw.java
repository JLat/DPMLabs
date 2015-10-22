
import lejos.hardware.Button;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.motor.EV3LargeRegulatedMotor;

public class Claw {

	private EV3LargeRegulatedMotor motor;
	private boolean isOpen;

	public Claw() {
		// assumes the claw is open at the start of the program.
		this.motor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("C"));
		this.isOpen = true;
	}

	public void grab() {
		motor.resetTachoCount();
		while (!motor.isStalled()) {
			motor.setSpeed(300);
			motor.backward();
		}
		motor.stop();
		isOpen = false;
	}

	public void partialOpen(){
		this.motor.rotate(-250);
	}
	
	public void open() {
		this.motor.resetTachoCount();
		while (!motor.isStalled()) {
			motor.setSpeed(300);
			motor.forward();
		}
		motor.stop();
		isOpen = true;
	}

	public boolean isOpen() {
		return this.isOpen;
	}

	public EV3LargeRegulatedMotor getMotor() {
		return this.motor;
	}

}
