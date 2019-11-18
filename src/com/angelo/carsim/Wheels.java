package com.angelo.carsim;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Wheels {

	public static final double RADIUS = 0.362;
	public static final double MASS = 20 * 2;
	public static final double INERTIA = 0.5 * MASS * RADIUS * RADIUS;
	
	private static final double B = 10;
	private static final double C = 1.9;
	private static final double D = 1;
	private static final double E = 0.97;

	private double linearVelocity = 0;
	private double appliedTorque = 0;

	public void update(double delta) {
		linearVelocity += appliedTorque / INERTIA * RADIUS * delta;
		
		appliedTorque = 0;
		
		if(linearVelocity < 0) {
			linearVelocity = 0;			
		}
	}
	
	public void applyTorque(double torque) {
		appliedTorque += torque;
	}
	
	public double getAngularVelocity() {
		return linearVelocity / RADIUS;
	}
	
	public double getLinearVelocity() {
		return linearVelocity;
	}
	
	public void setLinearVelocity(double linearVelocity) {
		this.linearVelocity = linearVelocity;
	}
	
	public double getSlipRatio(double carVelocity) {
		double maxK = 1;
		double error = 5;
		 		
		//Wheel slip ratio		
		double k = 0;
		
		
		if(linearVelocity < 5) {
			k = (linearVelocity - (carVelocity)) / (linearVelocity + error);
		}
		else {
			k = (linearVelocity - carVelocity) / linearVelocity;
		}	
	
		k = Math.min(k, maxK);
		k = Math.max(k, -maxK);
		
		return k;
	}
	
	public double getTractionTorque(double load, double carVelocity) {
		
		double k = getSlipRatio(carVelocity);
		//System.out.println(k);
		
		
		double Bk = B * k;
		
		double force = load * D * Math.sin(C * Math.atan(Bk - E * (Bk - Math.atan(Bk))));
		
			
		return force * RADIUS;
	}
	
}
