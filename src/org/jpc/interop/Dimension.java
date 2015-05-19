package org.jpc.interop;

public class Dimension
{
    public int height;
    public int width;
    
    public Dimension(final int width, final int height) {
        this.width = width;
        this.height = height;
    }
    
    public int getHeight() {
        return this.height;
    }
    
    public int getWidth() {
        return this.width;
    }
    
    public void setSize(final int width, final int height) {
        this.width = width;
        this.height = height;
    }
}
