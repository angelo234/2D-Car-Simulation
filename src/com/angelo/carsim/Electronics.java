package com.angelo.carsim;

public class Electronics {

	public static final double ABS_ACTIVATION_SLIP_RATIO = -0.2;
	
	public boolean electronicPowerDistributionEnabled = true;
	public boolean electronicBrakeDistributionEnabled = true;
	public boolean absEnabled = true;
	public boolean tcsEnabled = true;
	
	//ABS
	private boolean absLightOn = false;
	private double absTimer;
		
	private boolean frontABSUse = false;
	private boolean rearABSUse = false;
	
	//TCS
	private boolean tcsActive;
	private boolean tcsLightOn;
	private double tcsTimer;
	
	private boolean prevFrontSlipping = false;
	private boolean prevRearSlipping = false;
	
	private boolean applyFrontBrakes = false;
	private boolean applyRearBrakes = false;
	
	private boolean releaseFrontBrakes = false;
	private boolean releaseRearBrakes = false;
	
	
	public void updateElectronicSystems(double delta, Car car) {

		//ABS
		if(absEnabled) {
			//Update 15 times per second
			
			double frontSlipRatio = car.getFrontWheels().getSlipRatio(car.getVehicleVelocity());
			double rearSlipRatio = car.getRearWheels().getSlipRatio(car.getVehicleVelocity());
			
			if(absTimer >= 1.0/15.0) {
				frontABSUse = false;
				rearABSUse = false;			
				
				if(car.getVehicleVelocity() > 0.1 
						&& (frontSlipRatio < ABS_ACTIVATION_SLIP_RATIO || Display.brakeSlider.getValue() / 100.0 > car.frontBrakePosition) 
						&& Display.brakeSlider.getValue() > 0) {
					if(frontSlipRatio > ABS_ACTIVATION_SLIP_RATIO) {
						frontABSUse = true;
						applyFrontBrakes = true;
						releaseFrontBrakes = false;		
					}
					else {
						frontABSUse = true;
						releaseFrontBrakes = true;
						applyFrontBrakes = false;				
					}
					
					//System.out.println("ABS! Front Brake Pos: "+Main.df.format(car.frontBrakePosition) + ", Slip Ratio: "+Main.df.format(frontSlipRatio));	
					System.out.println(Main.df.format(car.frontBrakePosition) + "," + Main.df.format(car.getFrontWheels().getLinearVelocity()) + "," + Main.df.format(-frontSlipRatio));	
				}
				else {
					applyFrontBrakes = false;
					releaseFrontBrakes = false;
				}

				if(car.getVehicleVelocity() > 0.1 
						&& (rearSlipRatio < ABS_ACTIVATION_SLIP_RATIO || Display.brakeSlider.getValue() / 100.0 > car.rearBrakePosition) 
						&& Display.brakeSlider.getValue() > 0) {
					if(rearSlipRatio > ABS_ACTIVATION_SLIP_RATIO) {
						rearABSUse = true;
						applyRearBrakes = true;
						releaseRearBrakes = false;		
					}
					else {
						rearABSUse = true;
						releaseRearBrakes = true;
						applyRearBrakes = false;				
					}

					//System.out.println("ABS! Rear brake pos: "+Main.df.format(rearBrakePosition) + ", velocity: "+Main.df.format(getRearWheels().getLinearVelocity()));		
				}
				else {
					applyRearBrakes = false;
					releaseRearBrakes = false;
				}

				absTimer = 0;
	
			}
	
			absTimer += delta;	
	
			if(frontABSUse || rearABSUse) {
				Display.absTimer += delta;
			}
			
			double frontSlipABSActivationSlipDiff = frontSlipRatio - ABS_ACTIVATION_SLIP_RATIO;
			double rearSlipABSActivationSlipDiff = rearSlipRatio - ABS_ACTIVATION_SLIP_RATIO;

			double factor = 3;
			
			//Braking rate depends on the difference between slip ratio and ABS activation slip ratio
			double frontBrakeApplyRate = factor * Math.abs(frontSlipABSActivationSlipDiff);
			double rearBrakeApplyRate = factor * Math.abs(rearSlipABSActivationSlipDiff);
			
			double frontBrakeReleaseRate = factor * Math.abs(frontSlipABSActivationSlipDiff);
			double rearBrakeReleaseRate = factor * Math.abs(rearSlipABSActivationSlipDiff);
			
			if(frontSlipABSActivationSlipDiff >= 0) {
				frontBrakeReleaseRate = 0;		
			}
			if(rearSlipABSActivationSlipDiff >= 0) {
				rearBrakeReleaseRate = 0;
			}
			
			if(applyFrontBrakes) {
				double newFrontBrakePosition = Math.min(car.frontBrakePosition + frontBrakeApplyRate * delta, 1);
				
				car.frontBrakePosition = newFrontBrakePosition;
			}
			else if(releaseFrontBrakes) {
				double newFrontBrakePosition = Math.max(car.frontBrakePosition - frontBrakeReleaseRate * delta, 0);
				
				car.frontBrakePosition = newFrontBrakePosition;
			}
			
			if(applyRearBrakes) {
				double newRearBrakePosition = Math.min(car.rearBrakePosition + rearBrakeApplyRate  * delta, 1);
				
				car.rearBrakePosition = newRearBrakePosition;
			}
			else if(releaseRearBrakes) {
				double newRearBrakePosition = Math.max(car.rearBrakePosition - rearBrakeReleaseRate * delta, 0);
				
				car.rearBrakePosition = newRearBrakePosition;
			}
			
		}	
		
		// TCS
		if (tcsEnabled) {
			// Update 15 times per second

			if (tcsTimer >= 1.0 / 15.0) {
				boolean tcsRunning = false;

				if(car.getVehicleVelocity() > 0.1 
						&& (car.getFrontWheels().slipping  || car.getRearWheels().slipping || Display.throttleSlider.getValue() / 100.0 > car.throttlePosition)
						&& Display.throttleSlider.getValue() != 0){
						
					tcsRunning = true;
					tcsActive = true;

					if (prevFrontSlipping || prevRearSlipping) {
						car.throttlePosition -= 0.1;
					}
					else {
						car.throttlePosition += 0.05;
					}

					if (car.throttlePosition < 0) {
						car.throttlePosition = 0;
					} else if (car.throttlePosition > 1) {
						car.throttlePosition = 1;
					}

					prevFrontSlipping = car.getFrontWheels().slipping;
					prevRearSlipping = car.getRearWheels().slipping;

					//System.out.println("TCS! Throttle pos: "+ Main.df.format(throttlePosition) );
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


	public boolean isABSLightOn() {
		return absLightOn;
	}


	public boolean isFrontABSUse() {
		return frontABSUse;
	}


	public boolean isRearABSUse() {
		return rearABSUse;
	}


	public boolean isTCSActive() {
		return tcsActive;
	}


	public boolean isTCSLightOn() {
		return tcsLightOn;
	}


	public boolean isPrevFrontSlipping() {
		return prevFrontSlipping;
	}


	public boolean isPrevRearSlipping() {
		return prevRearSlipping;
	}
	
}
