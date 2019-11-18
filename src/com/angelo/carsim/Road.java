package com.angelo.carsim;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public class Road implements IRendering{

	private static final double LENGTH = 10000;
	
	public static double staticCoefficientOfFriction = 1.0;
	public static double kineticCoefficientOfFriction = 0.9;
	
	public static AffineTransform roadTransform; 
	
	private List<Point2D.Double> tireMarks = new ArrayList<Point2D.Double>();
	
	public Road() {
		roadTransform = AffineTransform.getTranslateInstance(0,300);
	}

	public void update(double delta) {
		roadTransform.translate(-Main.car.vehicleVelocity * Main.pixelsPerMeter * delta, 0);
		
		/*
		if(Main.car.vehicleVelocity > 2 && staticCoefficientOfFriction > 0.35) {
			if(Math.abs(Main.car.frontWheelVelocity - Main.car.vehicleVelocity) > 5) {			
				tireMarks.add(new Point2D.Double((499-10+Main.car.tireTexture.getWidth(null) * Car.TIRE_TEXTURE_SCALE / 2 - roadTransform.getTranslateX()),(317-5+Main.car.tireTexture.getHeight(null) * Car.TIRE_TEXTURE_SCALE - 15) - 300));
				//System.out.println((int)(246+Main.car.tireTexture.getWidth(null) * Car.TIRE_SCALE / 2 - roadTransform.getTranslateX()));
			}
			
			if(Math.abs(Main.car.rearWheelVelocity - Main.car.vehicleVelocity) > 5) {
				tireMarks.add(new Point2D.Double((246-10+Main.car.tireTexture.getWidth(null) * Car.TIRE_TEXTURE_SCALE / 2 - roadTransform.getTranslateX()),(317-5+Main.car.tireTexture.getHeight(null) * Car.TIRE_TEXTURE_SCALE - 15) - 300));
			}
		}
		*/
	}
	
	@Override
	public void render(Graphics2D g2) {	
		
		g2.setTransform(roadTransform);
		
		if(staticCoefficientOfFriction > 0.35) {
			g2.setColor(Color.DARK_GRAY);
		}
		else {
			g2.setColor(Color.WHITE);
		}
		
		g2.fillRect(0, 0, (int)(LENGTH * Main.pixelsPerMeter), 500);
		
		g2.setColor(Color.YELLOW);
		for(int x = 0; x < (int)(LENGTH * Main.pixelsPerMeter); x+= (int)(10 * Main.pixelsPerMeter)) {
			g2.fillRect(x, 150, (int)(5 * Main.pixelsPerMeter), (int)(0.1 * Main.pixelsPerMeter));
		}

		//g2.fillRect((int)(10 * Main.pixelsPerMeter), 150, (int)(5 * Main.pixelsPerMeter), (int)(0.1 * Main.pixelsPerMeter));
	
		g2.setColor(Color.BLACK);
		
		//System.out.println(tireMarks.size());
		for(Point2D.Double point : new ArrayList<Point2D.Double>(tireMarks)) {		
			g2.fillRect((int)point.x, (int)point.y, 20, 20);
		}
	}

}
