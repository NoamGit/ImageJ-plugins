/**
 * Created by noambox on 8/26/2015.
 */

import ij.blob.*;

import java.awt.geom.Point2D;

public class MyBlobFeature extends CustomBlobFeature {

    public double LocationFeature(Integer width, Integer height, Integer a, Integer b) {
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

    public int mySecondFancyFeature(Integer a, Double b) {
        int feature = (int) (b * getBlob().getAreaToPerimeterRatio() * a);
        return feature;
    }

}