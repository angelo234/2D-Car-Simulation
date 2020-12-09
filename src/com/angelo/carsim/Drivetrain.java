package com.angelo.carsim;

public class Drivetrain {

	public static final double GEAR_RATIOS[] = {2.786, 1.614, 1.082, 0.773, 0.566};
	
	public static final double DIFF_RATIO = 4.5;
	public static final double TRANSMISSION_EFFICIENCY = 1.0;	

	//Engine Constants
	public static final double IDLE_RPM = 1200;
	public static final double REDLINE_RPM = 6700;
	
	private Car car;
	
	//When AWD disabled, car becomes FWD
	public boolean awdEnabled = false;
	
	public int currentGear = 0;
	
	private double rpm;
	
	
	public Drivetrain(Car car) {
		this.car = car;
	}
	
	public void updateEngineRPM() {
		double centerDiffVelocity = 0;
		
		if(awdEnabled) {
			centerDiffVelocity = (car.getFrontWheels().getLinearVelocity() + car.getRearWheels().getLinearVelocity()) / 2;
		}
		else {
			centerDiffVelocity = car.getFrontWheels().getLinearVelocity();
		}
		
		double wheelRotationRate = (double) (centerDiffVelocity * 30 / (Math.PI * Wheels.RADIUS));
		
		rpm = wheelRotationRate * getGearRatio(currentGear) * DIFF_RATIO;
		
		rpm = Math.max(rpm, IDLE_RPM);
	}

	private double getGearRatio(int gear){
		return GEAR_RATIOS[gear];
	}
	
	public double getEngineTorqueFromRPM(double rpm){
		//-0.00000846x2+0.0749x+22.6		
		double torque = (double) (-0.00000846 * Math.pow(rpm, 2) + 0.0749 * rpm + 22.6);
		
		if(rpm >= REDLINE_RPM) {
			torque = 0;
		}
		
		return torque;
	}
	
	public double getOutputTorque(double delta) {
		updateEngineRPM();
		
		double engineTorque = getEngineTorqueFromRPM(rpm) * car.throttlePosition;
		double driveTorque = engineTorque * getGearRatio(currentGear) * DIFF_RATIO * TRANSMISSION_EFFICIENCY;
		//System.out.println("RPM: "+rpm+", Torque: "+engineTorque);

		//updateMPG(engineTorque, rpm, delta);
		
		return driveTorque;
	}
	
	public double getRPM() {
		return rpm;
	}
}
