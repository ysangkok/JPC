package org.jpc.interop;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;


/**
 * Created by janus on 4/21/15.
 */
public interface IPCMonitor extends Serializable {
    void saveState(OutputStream out) throws IOException;

    void loadState(InputStream in) throws IOException;

    void keyPressed(int keyCode);

    void keyReleased(int keyCode);

    void mouseEventReceived(int dx, int dy, int dz, int buttons);

    void startUpdateThread();

    void stopUpdateThread();

    boolean isRunning();

    void resizeDisplay(int width, int height);

    void scaleDisplay(int width, int height);

    File saveScreenshot();
}
