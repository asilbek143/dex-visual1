package com.dexvisual.config;

public class Waypoint {
    public String name;
    public double x, y, z;
    public String dim;
    public int color = 0x6C8CFF;

    public Waypoint() {}

    public Waypoint(String name, double x, double y, double z, String dim) {
        this.name = name; this.x = x; this.y = y; this.z = z; this.dim = dim;
    }
}
