/**
 * Created by noambox on 8/26/2015.
 */

import ij.blob.*;

public class MyBlobFeature extends CustomBlobFeature {

    public double LocationFeature(Integer width, Integer height) {
        double feature = getBlob().getCenterOfGravity().distance((double)width,(double)height);
        return feature;
    }

    public int mySecondFancyFeature(Integer a, Double b) {
        int feature = (int) (b * getBlob().getAreaToPerimeterRatio() * a);
        return feature;
    }

}