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
	
	//Transmission/Drivetrain Constants
	public static final double GEAR_1 = 2.786;
	public static final double GEAR_2 = 1.614;
	public static final double GEAR_3 = 1.082;
	public static final double GEAR_4 = 0.773;
	public static final double GEAR_5 = 0.566;
	public static final double DIFF_RATIO = 4.5;
	public static final double TRANSMISSION_EFFICIENCY = 0.85;		
	
	//Engine Constants
	public static final double IDLE_RPM = 800;
	public static final double REDLINE_RPM = 6700;
	
	//Body Constants	
	public static final double CAR_MASS = 1588;
	public static final double DRAG_COEFFICIENT = 0.41;
	public static final double FRONTAL_AREA = 2.54;
	public static final double LENGTH = 4.52;
	public static final double WIDTH = 1.82;
	public static final double HEIGHT = 1.82;
	public static final double WHEEL_BASE = 2.62;
	public static final double COG_TO_FRONT_WHEEL = 1.127;
	public static final double COG_TO_REAR_WHEEL = 1.493;
	public static final double COG_HEIGHT = 0.84;
	public static final double FRONT_SUSPENSION_STIFFNESS = 88772.4;
	public static final double REAR_SUSPENSION_STIFFNESS = 67010.4;
	
	public static final double MAX_BRAKE_TORQUE = 7000;
	
	//Brake Rotors
	public static final double CAST_IRON_SPECIFIC_HEAT_CAPACITY = 460;
	public static final double BRAKE_ROTOR_VOLUME = 0.00371;
	public static final double CAST_IRON_DENSITY = 7300;
	public static final double CONVECTIVE_HEAT_TRANSFER_COEFFICIENT = -20;
	public static final double BRAKE_ROTOR_SURFACE_AREA = 0.145931754;
	
	public Wheels frontWheels;
	public Wheels rearWheels;
	
	
	//When AWD disabled, car becomes FWD
	public boolean awdEnabled = false;
	public boolean electronicPowerDistributionEnabled = false;
	public boolean electronicBrakeDistributionEnabled = true;
	public boolean absEnabled = true;
	public boolean tcsEnabled = false;
	
	public double vehicleVelocity = 40;
	
	//Wheels
	public double frontRotorTemp = 298.15;
	public double rearRotorTemp = 298.15;
	
	//ABS System
	public boolean ABSLightOn = false;
	public double absTimer;
	
	public boolean frontABSUse = false;
	public boolean rearABSUse = false;
	
	public static final double MAX_SLIP_RATIO = 0.22;
	public static final double MIN_SLIP_RATIO = 0.18;

	public boolean tcsActive;
	public boolean tcsLightOn;
	public double tcsTimer;
	
	public boolean prevFrontSlipping = false;
	public boolean prevRearSlipping = false;
	
	public double mpg;
	public double rpm;
	public double carAcceleration;
	public double weightF;
	public double weightR;
	public double carDriveForce;
	public double frontWheelsDriveTorque;
	public double rearWheelsDriveTorque;
		
	public double throttlePosition = 0f;
	public double frontBrakePosition = 0f;
	public double rearBrakePosition = 0f;
	public int currentGear = 0;
	
	public double zeroHundredStartDistance;
	public long zeroHundredStartTime;
	
	public double zeroHundredEndDistance;
	public long zeroHundredEndTime;
	
	public double zeroHundredDistance;
	public long zeroHundredTime;
	
	public double hundredZeroStartDistance;
	public long hundredZeroStartTime;
	
	public double hundredZeroEndDistance;
	public long hundredZeroEndTime;
	
	public double hundredZeroDistance;
	public long hundredZeroTime;
	
	public Image carTexture;
	public Image tireTexture;
	
	public AffineTransform carTransform; 
	public AffineTransform leftTireTransform;
	public AffineTransform rightTireTransform; 
	
	public double previousAngle = 0;
	
	public String tireSquealAudioFilePath = "tires_squal_loop.wav";
    public AudioPlayer player;
    
	public DecimalFormat df;
	
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
		
		frontWheels = new Wheels();
		rearWheels = new Wheels();
		
		updateWeightDistribution(0);
		
		player = new AudioPlayer(tireSquealAudioFilePath);
		
		df = new DecimalFormat();
		df.setMaximumFractionDigits(2);
	}
	
	public void update(double delta) {
		pollInput(delta);
		
		updateBrakeRotorsTemperatures(delta);
		updateVehicleAcceleration(delta);	
		updateElectronicSystems(delta);
		updateSpeedGoalValues(delta);
		
		double tDiameter = tireTexture.getWidth(null);	

		leftTireTransform.rotate(rearWheels.getAngularVelocity() * delta, tDiameter / 2.0, tDiameter / 2.0);
		rightTireTransform.rotate(frontWheels.getAngularVelocity() * delta, tDiameter / 2.0, tDiameter / 2.0);
		
		//System.out.println(leftTireTransform.getTranslateX()+", "+leftTireTransform.getTranslateY());
		
		double kmh = 0;
		
		if(awdEnabled) {
			kmh = (frontWheels.getLinearVelocity() + rearWheels.getLinearVelocity()) / 2 * 3.6;
		}
		else {
			kmh = frontWheels.getLinearVelocity() * 3.6;
		}
		
		Display.kmh.setValue(kmh);
		Display.rpm.setValue(rpm / 1000.0);
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

	private void updateElectronicSystems(double delta) {
		
		//ABS
		if(absEnabled) {
			//Update 15 times per second
			
			if(absTimer >= 1.0/15.0) {
				frontABSUse = false;
				rearABSUse = false;
				
				double frontSlipRatio = Math.abs(frontWheels.getSlipRatio(vehicleVelocity));
				
				if(vehicleVelocity > 0.1 && (frontSlipRatio > MAX_SLIP_RATIO || Display.brakeSlider.getValue() / 100.0 > frontBrakePosition) && Display.brakeSlider.getValue() > 0) {
					if(frontSlipRatio < MIN_SLIP_RATIO) {
						frontABSUse = true;
						frontBrakePosition += 0.15;
					}
					else if(frontSlipRatio > MAX_SLIP_RATIO) {
						frontABSUse = true;
						frontBrakePosition -= 0.15;
					}
					
					
					if(frontBrakePosition > 1) {
						frontBrakePosition = 1;
					}
					else if(frontBrakePosition < 0) {
						frontBrakePosition = 0;
					}

					System.out.println("ABS! Front brake pos: "+df.format(frontBrakePosition) + ", velocity: "+df.format(frontWheels.getLinearVelocity()));	
				}
				
				double rearSlipRatio = Math.abs(rearWheels.getSlipRatio(vehicleVelocity));
				
				if(vehicleVelocity > 0.1 && (rearSlipRatio > MAX_SLIP_RATIO || Display.brakeSlider.getValue() / 100.0 > rearBrakePosition) && Display.brakeSlider.getValue() > 0) {
					if(rearSlipRatio < MIN_SLIP_RATIO) {
						rearABSUse = true;
						rearBrakePosition += 0.15;
					}
					else if(rearSlipRatio > MAX_SLIP_RATIO) {
						rearABSUse = true;
						rearBrakePosition -= 0.15;
					}				
					
					if(rearBrakePosition > 1) {
						rearBrakePosition = 1;
					}
					else if(rearBrakePosition < 0) {
						rearBrakePosition = 0;
					}

					System.out.println("ABS! Rear brake pos: "+df.format(rearBrakePosition) + ", velocity: "+df.format(rearWheels.getLinearVelocity()));		
				}

				//absActive = absRunning;

				absTimer = 0;
			}	
			absTimer += delta;	
			
			if(frontABSUse || rearABSUse) {
				Display.absTimer += delta;
			}
		}
		
		
		// TCS
		if (tcsEnabled) {
			// Update 15 times per second

			if (tcsTimer >= 1.0 / 15.0) {
				boolean tcsRunning = false;

				if(vehicleVelocity > 0.1 
						&& (frontWheels.slipping || rearWheels.slipping || Display.throttleSlider.getValue() / 100.0 > throttlePosition)
						&& Display.throttleSlider.getValue() != 0){
						
					tcsRunning = true;
					tcsActive = true;

					if (prevFrontSlipping || prevRearSlipping) {
						throttlePosition -= 0.1;
					}
					else {
						throttlePosition += 0.05;
					}

					if (throttlePosition < 0) {
						throttlePosition = 0;
					} else if (throttlePosition > 1) {
						throttlePosition = 1;
					}

					prevFrontSlipping = frontWheels.slipping;
					prevRearSlipping = rearWheels.slipping;

					//System.out.println("TCS! Throttle pos: "+ df.format(throttlePosition) );
				}
	
				tcsActive = tcsRunning;

				tcsTimer = 0;
			}
			tcsTimer += delta;

			if (tcsActive) {
				Display.tcsTimer += delta;
			}
		}
		
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
		
		if(!frontABSUse) {
			frontBrakePosition = Display.brakeSlider.getValue() / 100.0;
		}
		
		if(!rearABSUse) {
			rearBrakePosition = Display.brakeSlider.getValue() / 100.0;
			//System.out.println("ay lmao");
		}
		
		if(!tcsActive) {
			throttlePosition = Display.throttleSlider.getValue() / 100.0;
		}
		
		currentGear = Display.gearSlider.getValue() - 1;

	}
	
	double maxFrontTorque = 0;
	
	private void updateVehicleAcceleration(double delta) {	
		frontWheelsDriveTorque = getDriveTorqueForFrontWheels(delta, true);
		rearWheelsDriveTorque = getDriveTorqueForRearWheels(awdEnabled, delta, true);

		if(vehicleVelocity <= 0 && frontWheelsDriveTorque < 0) {
			frontWheelsDriveTorque = 0;		
		}
		
		if(vehicleVelocity <= 0 && rearWheelsDriveTorque < 0) {
			rearWheelsDriveTorque = 0;		
		}

		frontWheels.update(delta);
		rearWheels.update(delta);
		
		
		double frontWheelTorque = frontWheels.getTractionTorque(weightF, vehicleVelocity);
		double rearWheelTorque = rearWheels.getTractionTorque(weightR, vehicleVelocity);
		
		if(maxFrontTorque < frontWheelTorque) {
			maxFrontTorque = frontWheelTorque;
			//System.out.println(maxFrontTorque);
		}
		
		//System.out.println(frontWheelTorque);
		
		double totalFrontTorque = frontWheelsDriveTorque - frontWheelTorque;
		double totalRearTorque = rearWheelsDriveTorque - rearWheelTorque;		
		
		frontWheels.applyTorque(totalFrontTorque);
		rearWheels.applyTorque(totalRearTorque);
		
		double netForce = (frontWheelTorque + rearWheelTorque) / Wheels.RADIUS;
		
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

	private double getOutputShaftTorque(double delta) {
		updateEngineRPM();
		
		double engineTorque = getEngineTorqueFromRPM(rpm) * throttlePosition;
		double driveTorque = engineTorque * getGearRatio(currentGear) * DIFF_RATIO * TRANSMISSION_EFFICIENCY;
		//System.out.println("RPM: "+rpm+", Torque: "+engineTorque);

		updateMPG(engineTorque, rpm, delta);
		
		return driveTorque;
	}
	
	private double getDriveTorqueForFrontWheels(double delta, boolean updateBrakeTemperature) {
		double brakeTorque = getFrontBrakeTorque();
		
		if(electronicBrakeDistributionEnabled) {
			brakeTorque *= Math.min(weightF / Main.GRAVITY / CAR_MASS, 1);
		}
		else {
			brakeTorque /= 2;
		}	
		
		if(updateBrakeTemperature) {
			frontRotorTemp += (brakeTorque / Wheels.RADIUS * frontWheels.getLinearVelocity() * delta) / (10 * CAST_IRON_SPECIFIC_HEAT_CAPACITY);
		}	
		
		double driveTorque = getOutputShaftTorque(delta);
		
		if(awdEnabled) {
			if(electronicPowerDistributionEnabled) {
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
		
		if(electronicBrakeDistributionEnabled) {
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
		
		
		double driveTorque = getOutputShaftTorque(delta);
		
		if(isPowered) {
			if(electronicPowerDistributionEnabled) {
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
		return (double) (0.5f * DRAG_COEFFICIENT * FRONTAL_AREA * 1.29f * Math.pow(vehicleVelocity, 2));
	}
	
	private double getRRForce(){
		return 0.015f * vehicleVelocity;
	}

	public void updateEngineRPM() {
		double centerDiffVelocity = 0;
		
		if(awdEnabled) {
			centerDiffVelocity = (frontWheels.getLinearVelocity() + rearWheels.getLinearVelocity()) / 2;
		}
		else {
			centerDiffVelocity = frontWheels.getLinearVelocity();
		}
		
		double wheelRotationRate = (double) (centerDiffVelocity * 30 / (Math.PI * Wheels.RADIUS));
		
		rpm = wheelRotationRate * getGearRatio(currentGear) * DIFF_RATIO;
		
		rpm = Math.max(rpm, IDLE_RPM);
	}

	private double getGearRatio(int gear){
		switch (gear) {
		case 0:{
			return GEAR_1;
		}
		case 1:{
			return GEAR_2;
		}
		case 2:{
			return GEAR_3;
		}
		case 3:{
			return GEAR_4;
		}
		case 4:{
			return GEAR_5;
		}
		}
		
		return 0;
	}
	
	public double getEngineTorqueFromRPM(double rpm){
		//-0.00000846x2+0.0749x+22.6		
		double torque = (double) (-0.00000846 * Math.pow(rpm, 2) + 0.0749 * rpm + 222.6);
		
		if(rpm >= REDLINE_RPM) {
			torque = 0;
		}
		
		return torque;
	}

	@Override
	public void render(Graphics2D g2) {		
		g2.drawString("Velocity: "+df.format(vehicleVelocity * 3.6) +" km/h", 150, 25);
		g2.drawString("Acceleration: "+df.format(carAcceleration * 0.101971621) +" g", 295, 25);
		
		String strZeroHundredTime = df.format(TimeUnit.NANOSECONDS.toMillis(zeroHundredTime) / 1000f);
		String zeroHundredGForce = df.format(zeroHundredDistance / (TimeUnit.NANOSECONDS.toMillis(zeroHundredTime) / 1000f * TimeUnit.NANOSECONDS.toMillis(zeroHundredTime) / 1000f * 0.5f) / Main.GRAVITY); 
		String hundredZeroGForce = df.format(hundredZeroDistance / (TimeUnit.NANOSECONDS.toMillis(hundredZeroTime) / 1000f * TimeUnit.NANOSECONDS.toMillis(hundredZeroTime) / 1000f * 0.5f) / Main.GRAVITY); 
		
		String strHundredZeroTime = df.format(TimeUnit.NANOSECONDS.toMillis(hundredZeroTime) / 1000f);
		
		g2.drawString("0-100 (Time/Distance/Average Gs): "+ strZeroHundredTime + "s / "+df.format(zeroHundredDistance) + "m / "+ zeroHundredGForce + " g", 450, 25);
		g2.drawString("100-0 (Time/Distance/Average Gs): "+ strHundredZeroTime + "s / "+df.format(hundredZeroDistance) + "m / "+ hundredZeroGForce + " g", 450, 50);
		
		g2.drawString("Front/Rear Wheels Velocity: "+df.format(frontWheels.getLinearVelocity() * 3.6) +" km/h / "+df.format(rearWheels.getLinearVelocity() * 3.6) +" km/h", 150, 50);
		
		g2.drawString("Weight Distribution (F/R): "+df.format(weightF / Main.GRAVITY / CAR_MASS * 100) +"% / "+df.format(weightR / Main.GRAVITY / CAR_MASS * 100)+"%", 150, 75);
		
		//g2.drawString("Wanted Drive Force: "+df.format(wantedDriveForce) +" N", 200, 100);
		
		if((frontWheels.locking || rearWheels.locking || frontWheels.slipping || rearWheels.slipping) && vehicleVelocity > 0) {
			g2.setColor(Color.RED);
			
			if(frontWheels.slipping || frontWheels.locking) {
				g2.drawString("Front Wheels Slipping!", 470, 75);
			}
			if(rearWheels.slipping || rearWheels.locking) {
				g2.drawString("Rear Wheels Slipping!", 470, 100);
			}
			
			if(Road.staticCoefficientOfFriction > 0.35) {
				double frontDiff = Math.abs(frontWheels.getLinearVelocity() - vehicleVelocity);
				double rearDiff = Math.abs(rearWheels.getLinearVelocity() - vehicleVelocity);
				
				if(frontDiff > 3) {
					player.play();
					
					player.setVolume((float)frontDiff * 0.1f);
				}
				if(rearDiff > 3) {
					player.play();

					player.setVolume((float)rearDiff * 0.1f);
				}	
			}
			
		}
		
		else {
			player.stop();
		}
		
		
		g2.setColor(Color.BLACK);
		
		g2.drawString("Wheel Torque (F/R): "+df.format(frontWheelsDriveTorque) +" Nm / "+df.format(rearWheelsDriveTorque)+" Nm" , 150, 125);	
		
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
		
		
		g2.drawString("Avaliable Drive Torque (F/R): "+df.format(frontMaxTorque)+" Nm / "+df.format(rearMaxTorque)+" Nm", 150, 150);
		*/
		g2.drawString("Brake Rotor Temperatures (F/R): "+df.format(frontRotorTemp - 273.15)+" C / "+df.format(rearRotorTemp - 273.15)+" C", 150, 175);
		
		double mpg = 0;
		
		if(!Double.isNaN(this.mpg)) {
			mpg = this.mpg;
		}
		
		g2.drawString("Fuel Economy: "+df.format(mpg)+" MPG", 150, 200);
		
		g2.drawImage(carTexture, carTransform, null);	
		g2.drawImage(tireTexture, leftTireTransform, null);	
		g2.drawImage(tireTexture, rightTireTransform, null);
	}
}
