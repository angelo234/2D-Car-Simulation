package com.angelo.carsim;

public enum SurfaceType {
	DRY_TARMAC(0), WET_TARMAC(1), SNOW(2), ICE(3);
	
	// constructor
    private SurfaceType(final int surface) {
        this.surface = surface;
    }
 
    // internal state
    private int surface;
 
    public int getSurfaceIndex() {
        return surface;
    }
}
