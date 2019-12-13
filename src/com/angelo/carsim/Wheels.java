package com.angelo.carsim;

public class Wheels {

	public static final double RADIUS = 0.362;
	public static final double MASS = 10 * 2;
	public static final double INERTIA = 0.5 * MASS * RADIUS * RADIUS;
	
	/* Dry Tarmac = 0
	 * Wet Tarmac = 1
	 * Snow       = 2
	 * Ice        = 3
	 */
	private static final double COEFFICIENT_LIST_BY_SURFACE [][] = {
			{10, 1.9, 1, 0.97},
			{12, 2.3, 0.82, 1},
			{5, 2, 0.3, 1},
			{4, 2, 0.1, 1}			
	};
	
	public boolean locking = false;
	public boolean slipping = false;
	
	private double linearVelocity = 0;
	private double appliedTorque = 0;

	public void update(double delta, double load, double carVelocity) {
		linearVelocity += (appliedTorque - getTractionTorque(load, carVelocity)) / INERTIA * RADIUS * delta;
		
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
		
		if(k > 0.18) {
			slipping = true;
		}
		else if(k < -0.18) {
			locking = true;
		}
		else {
			slipping = false;
			locking = false;
		}
		
		double B = COEFFICIENT_LIST_BY_SURFACE[Road.SURFACE_TYPE.getSurfaceIndex()][0];
		double C = COEFFICIENT_LIST_BY_SURFACE[Road.SURFACE_TYPE.getSurfaceIndex()][1];
		double D = COEFFICIENT_LIST_BY_SURFACE[Road.SURFACE_TYPE.getSurfaceIndex()][2];
		double E = COEFFICIENT_LIST_BY_SURFACE[Road.SURFACE_TYPE.getSurfaceIndex()][3];
		
		double Bk = B * k;
		
		double force = load * D * Math.sin(C * Math.atan(Bk - E * (Bk - Math.atan(Bk))));
		
			
		return force * RADIUS;
	}
	
}
