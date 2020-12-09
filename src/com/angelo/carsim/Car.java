package com.angelo.carsim;

import java.awt.Color;
import java.awt.DefaultKeyboardFocusManager;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.io.Console;
import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;

public class Car implements IRendering{
	
	public static final double AIR_TEMPERATURE = 298.15;
	public static final double GASOLINE_ENERGY_DENSITY = 34.2;
	
	public static final double CAR_TEXTURE_SCALE = 0.4;
	public static final double TIRE_TEXTURE_SCALE = 0.41;
	
	//Body Constants	
	public static final double CAR_MASS = 1588;
	public static final double DRAG_COEFFICIENT = 0.41;
	public static final double FRONTAL_AREA = 2.55;
	public static final double LENGTH = 4.52;
	public static final double WIDTH = 1.82;
	public static final double HEIGHT = 1.82;
	public static final double WHEEL_BASE = 2.62;
	public static final double COG_TO_FRONT_WHEEL = 1.127;
	public static final double COG_TO_REAR_WHEEL = 1.493;
	public static final double COG_HEIGHT = 0.84;
	public static final double FRONT_SUSPENSION_STIFFNESS = 88772.4;
	public static final double REAR_SUSPENSION_STIFFNESS = 67010.4;
	
	public static final double MAX_BRAKE_TORQUE = 12500;
	
	//Brake Rotors
	public static final double CAST_IRON_SPECIFIC_HEAT_CAPACITY = 460;
	public static final double BRAKE_ROTOR_VOLUME = 0.00371;
	public static final double CAST_IRON_DENSITY = 7300;
	public static final double CONVECTIVE_HEAT_TRANSFER_COEFFICIENT = -20;
	public static final double BRAKE_ROTOR_SURFACE_AREA = 0.145931754;
	
	public double throttlePosition = 0f;
	public double frontBrakePosition = 0f;
	public double rearBrakePosition = 0f;
	
	private Electronics electronics;
	
	private Drivetrain drivetrain;
	
	private Wheels frontWheels;
	private Wheels rearWheels;	
	
	private double vehicleVelocity = 40;
	
	//Wheels
	private double frontRotorTemp = 298.15;
	private double rearRotorTemp = 298.15;
	
	private double mpg;
	private double carAcceleration;
	private double weightF;
	private double weightR;
	private double frontWheelsDriveTorque;
	private double rearWheelsDriveTorque;
		
	private double zeroHundredStartDistance;
	private long zeroHundredStartTime;
	
	private double zeroHundredEndDistance;
	private long zeroHundredEndTime;
	
	private double zeroHundredDistance;
	private long zeroHundredTime;
	
	private double hundredZeroStartDistance;
	private long hundredZeroStartTime;
	
	private double hundredZeroEndDistance;
	private long hundredZeroEndTime;
	
	private double hundredZeroDistance;
	private long hundredZeroTime;
	
	public Image carTexture;
	public Image tireTexture;
	
	public AffineTransform carTransform; 
	public AffineTransform leftTireTransform;
	public AffineTransform rightTireTransform; 
	
	public double previousAngle = 0;
	
	public String tireSquealAudioFilePath = "tires_squal_loop.wav";
    public AudioPlayer player;

	boolean stopZeroHundredUpdateSpeedGoalValues = false;
	boolean stopHundredZeroUpdateSpeedGoalValues = false;
	
	private boolean holdingKey = false;
	private double keyTimer;
	
	public Car() {	
		carTexture = TextureLoader.getTexture("car3.png");
		tireTexture = TextureLoader.getTexture("tire.png");
		
		Main.pixelsPerMeter = carTexture.getWidth(null) * CAR_TEXTURE_SCALE / LENGTH;
		
		double x = Display.WIDTH / 2.0 - carTexture.getWidth(null) / 2.0 * CAR_TEXTURE_SCALE;
		double y = Display.HEIGHT / 2.0 - carTexture.getHeight(null) / 2.0 * CAR_TEXTURE_SCALE;
		
		carTransform = AffineTransform.getTranslateInstance(x,y);
		carTransform.scale(CAR_TEXTURE_SCALE, CAR_TEXTURE_SCALE);
		
		leftTireTransform = AffineTransform.getTranslateInstance(246,317);
		leftTireTransform.scale(TIRE_TEXTURE_SCALE, TIRE_TEXTURE_SCALE);
		
		rightTireTransform = AffineTransform.getTranslateInstance(499,317);
		rightTireTransform.scale(TIRE_TEXTURE_SCALE, TIRE_TEXTURE_SCALE);
		
		electronics = new Electronics();
		
		drivetrain = new Drivetrain(this);
		
		frontWheels = new Wheels();
		rearWheels = new Wheels();
		
		updateWeightDistribution(0);
		
		player = new AudioPlayer(tireSquealAudioFilePath);
	}
	
	public void update(double delta) {
		pollInput(delta);
		
		updateBrakeRotorsTemperatures(delta);
		updateVehicleAcceleration(delta);	
		electronics.updateElectronicSystems(delta, this);
		updateSpeedGoalValues(delta);
		
		double tDiameter = tireTexture.getWidth(null);	

		leftTireTransform.rotate(rearWheels.getAngularVelocity() * delta, tDiameter / 2.0, tDiameter / 2.0);
		rightTireTransform.rotate(frontWheels.getAngularVelocity() * delta, tDiameter / 2.0, tDiameter / 2.0);
		
		//System.out.println(leftTireTransform.getTranslateX()+", "+leftTireTransform.getTranslateY());
		
		double kmh = 0;
		
		if(drivetrain.awdEnabled) {
			kmh = (frontWheels.getLinearVelocity() + rearWheels.getLinearVelocity()) / 2 * 3.6;
		}
		else {
			kmh = frontWheels.getLinearVelocity() * 3.6;
		}
		
		Display.kmh.setValue(kmh);
		Display.rpm.setValue(drivetrain.getRPM() / 1000.0);
	}
	
	private void updateSpeedGoalValues(double delta) {
		//0-100
		
		if(vehicleVelocity <= 0.1) {
			zeroHundredStartTime = System.nanoTime();
			zeroHundredStartDistance = Road.roadTransform.getTranslateX();
			stopZeroHundredUpdateSpeedGoalValues = false;
		}
		if(!stopZeroHundredUpdateSpeedGoalValues) {
			if (vehicleVelocity >= (100.0 / 3.6)){
				zeroHundredEndTime = System.nanoTime();
				zeroHundredEndDistance = Road.roadTransform.getTranslateX();
				stopZeroHundredUpdateSpeedGoalValues = true;
				
				zeroHundredDistance = Math.abs((zeroHundredEndDistance - zeroHundredStartDistance) / Main.pixelsPerMeter);
				zeroHundredTime = zeroHundredEndTime - zeroHundredStartTime;
			}
		}
		
		//100 - 0
		
		if(vehicleVelocity >= 27.7778) {
			hundredZeroStartTime = System.nanoTime();
			hundredZeroStartDistance = Road.roadTransform.getTranslateX();
			stopHundredZeroUpdateSpeedGoalValues = false;
		}
		if(!stopHundredZeroUpdateSpeedGoalValues) {
			if (vehicleVelocity <= 0.1){
				hundredZeroEndTime = System.nanoTime();
				hundredZeroEndDistance = Road.roadTransform.getTranslateX();
				stopHundredZeroUpdateSpeedGoalValues = true;
				
				hundredZeroDistance = Math.abs((hundredZeroEndDistance - hundredZeroStartDistance) / Main.pixelsPerMeter);
				hundredZeroTime = hundredZeroEndTime - hundredZeroStartTime;
			}
		}
	}
	
	private void updateMPG(double torque, double rpm, double delta) {
		double power = torque * rpm / 9.5488;
		double energy = power * delta / 1_000_000;
		double gallonsUsed = energy / GASOLINE_ENERGY_DENSITY / 3.785;	
		double distance = vehicleVelocity * delta / 1609.344;
		
		mpg = distance / gallonsUsed;
		
	}

	private void updateBrakeRotorsTemperatures(double delta) {
		//T = Ti*e^(A*h*t)/(c*p*V) - Ta*e^(A*h*t)/(c*p*V) + Ta
		
		double x = (BRAKE_ROTOR_SURFACE_AREA * CONVECTIVE_HEAT_TRANSFER_COEFFICIENT * delta) / (CAST_IRON_SPECIFIC_HEAT_CAPACITY * CAST_IRON_DENSITY * BRAKE_ROTOR_VOLUME);
			
		double frontFinalTemp = frontRotorTemp * Math.pow(Math.E, x) - AIR_TEMPERATURE * Math.pow(Math.E, x) + AIR_TEMPERATURE;
		double rearFinalTemp = rearRotorTemp * Math.pow(Math.E, x) - AIR_TEMPERATURE * Math.pow(Math.E, x) + AIR_TEMPERATURE;
		
		//System.out.println((frontRotorTemp - frontFinalTemp) > 0 ? "Cooling":"Heating or constant");
		
		frontRotorTemp = frontFinalTemp;
		rearRotorTemp = rearFinalTemp;	
	}
		
	private void pollInput(double delta) {
		//System.out.println(brakePosition);
			
		if(keyTimer >= 0.01) {
			if(Keyboard.keys[KeyEvent.VK_W] ) {		
				Display.throttleSlider.setValue((int)(Display.throttleSlider.getValue() + 1));		
			}	
			else if(Keyboard.keys[KeyEvent.VK_S]) {
				Display.throttleSlider.setValue((int)(Display.throttleSlider.getValue() - 1));
			}		
			if(Keyboard.keys[KeyEvent.VK_Q]) {
				Display.brakeSlider.setValue((int)(Display.brakeSlider.getValue() + 1));
			}			
			else if(Keyboard.keys[KeyEvent.VK_A]) {
				Display.brakeSlider.setValue((int)(Display.brakeSlider.getValue() - 1));
			}
			
			if(Keyboard.keys[KeyEvent.VK_1]) {
				Display.gearSlider.setValue(1);
			}
			else if(Keyboard.keys[KeyEvent.VK_2]) {
				Display.gearSlider.setValue(2);
			}
			else if(Keyboard.keys[KeyEvent.VK_3]) {
				Display.gearSlider.setValue(3);
			}
			else if(Keyboard.keys[KeyEvent.VK_4]) {
				Display.gearSlider.setValue(4);
			}
			else if(Keyboard.keys[KeyEvent.VK_5]) {
				Display.gearSlider.setValue(5);
			}
			
			
			keyTimer = 0;
		}
		
		keyTimer += delta;
		
		if(!electronics.isFrontABSUse()) {
			frontBrakePosition = Display.brakeSlider.getValue() / 100.0;
		}
		
		if(!electronics.isRearABSUse()) {
			rearBrakePosition = Display.brakeSlider.getValue() / 100.0;
		}
		
		if(!electronics.isTCSActive()) {
			throttlePosition = Display.throttleSlider.getValue() / 100.0;
		}
		
		drivetrain.currentGear = Display.gearSlider.getValue() - 1;

	}
	
	private void updateVehicleAcceleration(double delta) {	
		frontWheelsDriveTorque = getDriveTorqueForFrontWheels(delta, true);
		rearWheelsDriveTorque = getDriveTorqueForRearWheels(drivetrain.awdEnabled, delta, true);

		if(vehicleVelocity <= 0 && frontWheelsDriveTorque < 0) {
			frontWheelsDriveTorque = 0;		
		}
		
		if(vehicleVelocity <= 0 && rearWheelsDriveTorque < 0) {
			rearWheelsDriveTorque = 0;		
		}

		frontWheels.applyTorque(frontWheelsDriveTorque);
		rearWheels.applyTorque(rearWheelsDriveTorque);
		
		frontWheels.update(delta, weightF, vehicleVelocity);
		rearWheels.update(delta, weightR, vehicleVelocity);
		
		double frontWheelTractionTorque = frontWheels.getTractionTorque(weightF, vehicleVelocity);
		double rearWheelTractionTorque = rearWheels.getTractionTorque(weightR, vehicleVelocity);
		
		double netForce = (frontWheelTractionTorque + rearWheelTractionTorque) / Wheels.RADIUS;
		
		netForce -= (getDragForce() + getRRForce());
		
		updateWeightDistribution(netForce);
		
		
		carAcceleration = netForce / CAR_MASS;
		
		vehicleVelocity += carAcceleration * delta;
		
		if(vehicleVelocity < 0) {
			vehicleVelocity = 0;
		}
	}
	
	private void updateWeightDistribution(double driveForce) {
		//Weight on Front Axle: Wf = (c/L)*W - (h/L)*M*a 
	    //Weight on Rear Axle: Wr = (b/L)*W + (h/L)*M*a
		
		weightF = (COG_TO_REAR_WHEEL / WHEEL_BASE) * CAR_MASS * Main.GRAVITY - (COG_HEIGHT / WHEEL_BASE) * driveForce;
		weightR = (COG_TO_FRONT_WHEEL / WHEEL_BASE) * CAR_MASS * Main.GRAVITY + (COG_HEIGHT / WHEEL_BASE) * driveForce;
		
		weightF = Math.min(weightF, CAR_MASS * Main.GRAVITY);
		weightR = Math.min(weightR, CAR_MASS * Main.GRAVITY);
		
		weightF = Math.max(weightF, 0);
		weightR = Math.max(weightR, 0);
		
		//System.out.println(weightF + "_" + weightR);
		
		
		double frontHeight = weightF / -FRONT_SUSPENSION_STIFFNESS;
		double rearHeight = weightR / -REAR_SUSPENSION_STIFFNESS;
		
		//System.out.println("Front Height: "+frontHeight);
		//System.out.println("Rear Height: "+rearHeight);
		
		double angle = -Math.atan2(frontHeight - rearHeight, WHEEL_BASE);
		
		//System.out.println("Angle: "+angle);
		
		carTransform.rotate(angle - previousAngle, carTexture.getWidth(null) / 2.0, carTexture.getHeight(null) / 2.0);
		
		previousAngle = angle;
	}
	
	private double getDriveTorqueForFrontWheels(double delta, boolean updateBrakeTemperature) {
		double brakeTorque = getFrontBrakeTorque();
		
		if(electronics.electronicBrakeDistributionEnabled) {
			brakeTorque *= Math.min(weightF / Main.GRAVITY / CAR_MASS, 1);
		}
		else {
			brakeTorque /= 2;
		}	
		
		if(updateBrakeTemperature) {
			frontRotorTemp += (brakeTorque / Wheels.RADIUS * frontWheels.getLinearVelocity() * delta) / (10 * CAST_IRON_SPECIFIC_HEAT_CAPACITY);
		}	
		
		double driveTorque = drivetrain.getOutputTorque(delta);
		
		if(drivetrain.awdEnabled) {
			if(electronics.electronicPowerDistributionEnabled) {
				driveTorque *= (weightF / Main.GRAVITY / CAR_MASS);
			}
			else {
				driveTorque /= 2;
			}		
		}
		
		return (driveTorque - brakeTorque);
	}
	
	private double getDriveTorqueForRearWheels(boolean isPowered, double delta, boolean updateBrakeTemperature) {
		double brakeTorque = getRearBrakeTorque();
		
		if(electronics.electronicBrakeDistributionEnabled) {
			brakeTorque *= Math.min(weightR / Main.GRAVITY / CAR_MASS, 1);
		}
		else {
			brakeTorque /= 2;
		}	
		
		//T = (Ff * d) / mc 
		
		//v = s/t 
		
		if(updateBrakeTemperature) {
			rearRotorTemp += (brakeTorque / Wheels.RADIUS * rearWheels.getLinearVelocity() * delta) / (10 * CAST_IRON_SPECIFIC_HEAT_CAPACITY);
		}
		
		
		double driveTorque = drivetrain.getOutputTorque(delta);
		
		if(isPowered) {
			if(electronics.electronicPowerDistributionEnabled) {
				driveTorque *= (weightR / Main.GRAVITY / CAR_MASS);
			}
			else {
				driveTorque /= 2;
			}		
		}
		else {
			driveTorque = 0;
		}
		
		
		return (driveTorque - brakeTorque);
	}
	
	private double getFrontBrakeTorque() {
		return MAX_BRAKE_TORQUE * frontBrakePosition;
	}
	
	private double getRearBrakeTorque() {
		return MAX_BRAKE_TORQUE * rearBrakePosition;
	}
	
	private double getDragForce(){
		return (double) (0.5f * DRAG_COEFFICIENT * FRONTAL_AREA * 1.18f * Math.pow(vehicleVelocity, 2));
	}
	
	private double getRRForce(){
		//Bars
		double tirePressure = 2;
		
		return 0.005 + (1.0 / tirePressure) * (0.01 + 0.0095 * Math.pow(vehicleVelocity * 3.6 / 100.0, 2));
	}

	@Override
	public void render(Graphics2D g2) {		
		g2.drawString("Velocity: "+Main.df.format(vehicleVelocity * 3.6) +" km/h", 150, 25);
		g2.drawString("Acceleration: "+Main.df.format(carAcceleration * 0.101971621) +" g", 295, 25);
		
		String strZeroHundredTime = Main.df.format(TimeUnit.NANOSECONDS.toMillis(zeroHundredTime) / 1000f);
		String zeroHundredGForce = Main.df.format(zeroHundredDistance / (TimeUnit.NANOSECONDS.toMillis(zeroHundredTime) / 1000f * TimeUnit.NANOSECONDS.toMillis(zeroHundredTime) / 1000f * 0.5f) / Main.GRAVITY); 
		String hundredZeroGForce = Main.df.format(hundredZeroDistance / (TimeUnit.NANOSECONDS.toMillis(hundredZeroTime) / 1000f * TimeUnit.NANOSECONDS.toMillis(hundredZeroTime) / 1000f * 0.5f) / Main.GRAVITY); 
		
		String strHundredZeroTime = Main.df.format(TimeUnit.NANOSECONDS.toMillis(hundredZeroTime) / 1000f);
		
		g2.drawString("0-100 (Time/Distance/Average Gs): "+ strZeroHundredTime + "s / "+Main.df.format(zeroHundredDistance) + "m / "+ zeroHundredGForce + " g", 450, 25);
		g2.drawString("100-0 (Time/Distance/Average Gs): "+ strHundredZeroTime + "s / "+Main.df.format(hundredZeroDistance) + "m / "+ hundredZeroGForce + " g", 450, 50);
		
		g2.drawString("Front/Rear Wheels Velocity: "+Main.df.format(frontWheels.getLinearVelocity() * 3.6) +" km/h / "+Main.df.format(rearWheels.getLinearVelocity() * 3.6) +" km/h", 150, 50);
		
		g2.drawString("Weight Distribution (F/R): "+Main.df.format(weightF / Main.GRAVITY / CAR_MASS * 100) +"% / "+Main.df.format(weightR / Main.GRAVITY / CAR_MASS * 100)+"%", 150, 75);
		
		//g2.drawString("Wanted Drive Force: "+Main.df.format(wantedDriveForce) +" N", 200, 100);
		
		if((frontWheels.locking || rearWheels.locking || frontWheels.slipping || rearWheels.slipping) && vehicleVelocity > 0) {
			g2.setColor(Color.RED);
			
			if(frontWheels.slipping || frontWheels.locking) {
				g2.drawString("Front Wheels Slipping!", 470, 75);
			}
			if(rearWheels.slipping || rearWheels.locking) {
				g2.drawString("Rear Wheels Slipping!", 470, 100);
			}
			
			if(Road.SURFACE_TYPE == SurfaceType.DRY_TARMAC) {
				double frontSlipRatio = Math.abs(frontWheels.getSlipRatio(vehicleVelocity));
				double rearSlipRatio = Math.abs(rearWheels.getSlipRatio(vehicleVelocity));
				
				if(frontSlipRatio > 0.9) {
					player.play();

					player.setVolume((float)frontSlipRatio * 0.5f);
				}
				if(rearSlipRatio > 0.9) {
					player.play();

					player.setVolume((float)rearSlipRatio * 0.5f);
				}	
			}
			
		}
		
		else {
			player.stop();
		}
		
		
		g2.setColor(Color.BLACK);
		
		g2.drawString("Wheel Torque (F/R): "+Main.df.format(frontWheelsDriveTorque) +" Nm / "+Main.df.format(rearWheelsDriveTorque)+" Nm" , 150, 125);	
		
		double frontMaxTorque;
		double rearMaxTorque;
		
		/*
		if(frontSlipping || frontLocking) {
			frontMaxTorque = getFrontWheelsMaxDriveTorqueKinetic();
		}
		else {
			frontMaxTorque = getFrontWheelsMaxDriveTorqueStatic();
		}
		
		if(rearSlipping || rearLocking) {
			rearMaxTorque = getRearWheelsMaxDriveTorqueKinetic();
		}
		else {
			rearMaxTorque = getRearWheelsMaxDriveTorqueStatic();
		}
		
		
		g2.drawString("Avaliable Drive Torque (F/R): "+Main.df.format(frontMaxTorque)+" Nm / "+Main.df.format(rearMaxTorque)+" Nm", 150, 150);
		*/
		g2.drawString("Brake Rotor Temperatures (F/R): "+Main.df.format(frontRotorTemp - 273.15)+" C / "+Main.df.format(rearRotorTemp - 273.15)+" C", 150, 175);
		
		double mpg = 0;
		
		if(!Double.isNaN(this.mpg)) {
			mpg = this.mpg;
		}
		
		g2.drawString("Fuel Economy: "+Main.df.format(mpg)+" MPG", 150, 200);
		
		g2.drawImage(carTexture, carTransform, null);	
		g2.drawImage(tireTexture, leftTireTransform, null);	
		g2.drawImage(tireTexture, rightTireTransform, null);
	}

	public Wheels getFrontWheels() {
		return frontWheels;
	}

	public Wheels getRearWheels() {
		return rearWheels;
	}

	public double getVehicleVelocity() {
		return vehicleVelocity;
	}

	public Electronics getElectronics() {
		return electronics;
	}
	
}
