package com.angelo.carsim;

import java.text.DecimalFormat;

public class Main {
	
	public static final double GRAVITY = 9.81;
	
	//Updates per Second
	private static final int UPS = 1000;
	private static final int FPS = 144;
	
	public static Car car;
	public static Road road;	
	public static Display display;
	
	public static DecimalFormat df;
	
	public static double pixelsPerMeter;
	
	private static long lastFrameTime;
	private static long lastTime;
	private static double delta = 0;
	private static double uDelta = 0;

	private static void init(){
		df = new DecimalFormat();
		df.setMaximumFractionDigits(2);
		
		car = new Car();
		road = new Road();		
		
		display = new Display();
		
		lastFrameTime = System.nanoTime();
		lastTime = System.nanoTime();		
	}
	
	private static void update(){
		double dT = getDeltaTime();
		
		//dT *= 0.5;
		
		car.update(dT);
		road.update(dT);

		display.requestFocus();
	}
	
	private static void render(){
		display.updateDisplay();
	}
	
	private static double getDeltaTime(){
		long currentFrameTime = System.nanoTime();
		double delta = (currentFrameTime - lastFrameTime) / 1000000000.0;
		lastFrameTime = currentFrameTime;	
		
		return delta;
	}
	
	public static void main(String[] args) throws InterruptedException {
		init();
		
		while(true){
			long now = System.nanoTime();
			delta += (now - lastTime) / (1000_000_000.0 / FPS);
			uDelta += (now - lastTime) / (1000_000_000.0 / UPS);
			lastTime = now;
			
			while(uDelta >= 1) {
				uDelta--;
				
				update();
			}
			
			while (delta >= 1) {
				delta--;
				
				render();			
			}			
		}
	}
}
