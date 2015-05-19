/*
    JPC: An x86 PC Hardware Emulator for a pure Java Virtual Machine
    Release Version 2.4

    A project from the Physics Dept, The University of Oxford

    Copyright (C) 2007-2010 The University of Oxford

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License version 2 as published by
    the Free Software Foundation.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 
    Details (including contact information) can be found at: 

    jpc.sourceforge.net
    or the developer website
    sourceforge.net/projects/jpc/

    Conceived and Developed by:
    Rhys Newman, Ian Preston, Chris Dennis

    End of licence header
*/

package org.jpc.j2se;

import org.jpc.interop.Dimension;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.*;

import javax.swing.JScrollPane;

import org.jpc.emulator.*;
import org.jpc.emulator.pci.peripheral.*;
import org.jpc.emulator.peripheral.*;

/**
 * 
 * @author Rhys Newman
 */
public class PCMonitor extends KeyHandlingPanel implements org.jpc.interop.IPCMonitor {
    private Keyboard keyboard;
    private DefaultVGACard vgaCard;
    private Updater updater;
    private Component frame = null;
    private PC pc;
    private double scaleX = 1.0;
    private double scaleY = 1.0;
    private boolean ignoreResize = false;
    
    private volatile boolean clearBackground;

    public PCMonitor(PC pc) 
    {
        this(null, pc);
    }

    public PCMonitor(LayoutManager mgr, PC pc) 
    {
        super(mgr);
        this.pc = pc;

        clearBackground = true;
        setDoubleBuffered(false);
        requestFocusInWindow();

        vgaCard = (DefaultVGACard) pc.getComponent(VGACard.class);
        vgaCard.setMonitor(this);
        vgaCard.resizeDisplay(720, 480);
        keyboard = (Keyboard) pc.getComponent(Keyboard.class);
        setInputMap(WHEN_FOCUSED, null);
    }

    protected PC getPC()
    {
        return pc;
    }

    @Override
    public void saveState(OutputStream out) throws IOException
    {
        int[] rawImageData = vgaCard.getDisplayBuffer();
        byte[] dummy = new byte[rawImageData.length * 4];
        for (int i = 0, j = 0; i < rawImageData.length; i++) {
            int val = rawImageData[i];
            dummy[j++] = (byte) (val >> 24);
            dummy[j++] = (byte) (val >> 16);
            dummy[j++] = (byte) (val >> 8);
            dummy[j++] = (byte) (val);
        }

        DataOutputStream output = new DataOutputStream(out);
        output.writeInt(rawImageData.length);
        out.write(dummy);
        out.flush();
    }

    @Override
    public void loadState(InputStream in) throws IOException
    {
        DataInputStream input = new DataInputStream(in);
        int len = input.readInt();
        int[] rawImageData = vgaCard.getDisplayBuffer();
        if (len != rawImageData.length) {
            throw new IOException("Image size not consistent with saved image state");
        }
        byte[] dummy = new byte[len * 4];
        input.readFully(dummy);
        for (int i = 0, j = 0; i < len; i++) {
            int val = 0;
            val |= (0xff & dummy[j++]) << 24;
            val |= (0xff & dummy[j++]) << 16;
            val |= (0xff & dummy[j++]) << 8;
            val |= 0xff & dummy[j++];

            rawImageData[i] = val;
        }
    }

    public void setFrame(Component f)
    {
        this.frame = f;
    }

    @Override
    public void mouseExited(MouseEvent e) {
        super.mouseExited(e);
    }

    @Override
    public void keyPressed(int keyCode)
    {
        keyboard.keyPressed(KeyMapping.getScancode(Integer.valueOf(keyCode)));
    }

    @Override
    public void keyReleased(int keyCode)
    {
        keyboard.keyReleased(KeyMapping.getScancode(Integer.valueOf(keyCode)));
    }

    @Override
    public void mouseEventReceived(int dx, int dy, int dz, int buttons)
    {
        keyboard.putMouseEvent(dx, dy, dz, buttons);
    }

    @Override
    public synchronized void startUpdateThread()
    {
        stopUpdateThread();
        updater = new Updater();
        updater.start();
    }

    @Override
    public synchronized void stopUpdateThread()
    {
        if (updater != null)
            updater.halt();
    }

    @Override
    public synchronized boolean isRunning()
    {
        if (updater == null)
            return false;
        return updater.running;
    }

    class Updater extends Thread 
    {
        private volatile boolean running = !Option.noScreen.isSet();

        public Updater() 
        {
            super("PC Monitor Updater Task");
        }

        public void run() 
        {
            while (running) 
            {
                try
                {
                    Thread.sleep(20);
                }
                catch (InterruptedException e) {}

                vgaCard.prepareUpdate();
                vgaCard.updateDisplay();
                /*
                int xmin = (int)((vgaCard.getXMin() -1 ) * scaleX);
                int xmax = (int)(vgaCard.getXMax() * scaleX);
                int ymin = (int)((vgaCard.getYMin() - 1) * scaleY);
                int ymax = (int)(vgaCard.getYMax() * scaleY);
                
                repaint(xmin, ymin, xmax - xmin + 1, ymax - ymin + 1);
                */
                repaint();
            }
        }

        public void halt() 
        {
            try 
            {
                running = false;
                interrupt();
            } 
            catch (SecurityException e) {}
        }
    }

    @Override
    public void resizeDisplay(int width, int height)
    {
		resizeDisplayCommon((int)(width * scaleX), (int)(height * scaleY));
    }
    private void resizeDisplayCommon(int width, int height) 
    {
    	//System.out.println("resized X="+width+" Y="+height);
    	//System.out.println("resized scaleX="+scaleX+" scaleY="+scaleY);
        setPreferredSize(new java.awt.Dimension(width, height));
        setMaximumSize(new java.awt.Dimension(width, height));
        setMinimumSize(new java.awt.Dimension(width, height));

        clearBackground = true;
        revalidate();
        repaint();
    }

    @Override
    public void scaleDisplay(int width, int height)
    {
        
    	Dimension display = vgaCard.getDisplaySize();
    	double displayWidth=display.width;
    	double displayHeight=display.height;
    	if(width > displayWidth) {
    		scaleX = width / displayWidth;
    	}else{
    		scaleX = 1.0;
    	}
    	if(height > displayHeight) {
    		scaleY = height / displayHeight;
    	}else{
    		scaleY = 1.0;
    	}
    	//System.out.println("scale display scaleX="+scaleX+" scaleY="+scaleY + "actual x="+displayWidth+" y="+displayHeight);
    }

    @Override
    public File saveScreenshot() {
        throw new RuntimeException("when running on j2se, the method in DefaultVGACard shouldnt call PCMonitors screenshot method");
    }

    @Override
    public void update(Graphics g)
    {
        paint(g);
    }

    @Override
    public void paint(Graphics g)
    {
        if (clearBackground)
        {
            g.setColor(Color.white);
            java.awt.Dimension s1 = getSize();
            Dimension s2 = vgaCard.getDisplaySize();

            if (s1.width > s2.width)
                g.fillRect(s2.width, 0, s1.width - s2.width, s1.height);
            if (s1.height > s2.height)
                g.fillRect(0, s2.height, s1.width, s1.height - s2.height);
            clearBackground = false;
        }
        vgaCard.paintPCMonitor((Graphics2D)g, this);
    }
}
