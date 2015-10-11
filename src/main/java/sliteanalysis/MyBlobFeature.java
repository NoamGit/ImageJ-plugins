package sliteanalysis;

/**
 * Created by noambox on 8/26/2015.
 */

import ij.ImageStack;
import ij.blob.*;

import java.awt.geom.Point2D;
import java.util.ArrayList;

public class MyBlobFeature extends CustomBlobFeature {

    public double LocationFeature(Integer width, Integer height, Integer a, Integer b) {
        // Location based feature set to SLITE system's FOV  (Oval centered FOV)
        Point2D centerOfGrafity = new Point2D.Float();
        Blob thisblob = getBlob();
        int[] x =  thisblob.getOuterContour().xpoints;
        int[] y =  thisblob.getOuterContour().ypoints;
        int sumx = 0;
        int sumy = 0;
        int npoints = thisblob.getOuterContour().npoints;
        for(int i = 0; i < npoints - 1; ++i) {
            sumx += x[i] ;
            sumy += y[i] ;
        }
        centerOfGrafity.setLocation((double)sumx/npoints, (double)sumy/npoints);
        double px = ( ((double) width / 2 ) - centerOfGrafity.getX() )/a;
        double py = ( ((double) height / 2 ) - centerOfGrafity.getY() )/b;
        double feature = px * px + py * py;
        return feature;
    }

/*    public ArrayList<Double> DeltaFoverF_Blob(CalciumSignal ca_sig) {
        ArrayList<Double> feature = ca_sig.getSignalProcessed();
        return feature;
    }*/

}