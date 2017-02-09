import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ErrorMsg
import ij.IJ
import ij.ImagePlus
import ij.gui.Overlay
import ij.gui.PointRoi
import ij.gui.PolygonRoi
import ij.gui.Roi
import ij.plugin.filter.MaximumFinder
import ij.plugin.frame.RoiManager
import mpicbg.models.Affine2D
import mpicbg.models.AffineModel2D
import mpicbg.models.Model
import mpicbg.models.Point
import mpicbg.models.PointMatch
import sliteanalysis.AAP_woStimulus
import sliteanalysis.Stimulus_API

import java.awt.Polygon
import java.awt.geom.AffineTransform
import java.awt.geom.Point2D
import java.awt.image.AffineTransformOp

/**
 * Created by noambox on 6/2/2016.
 */

// define functions
int[] findCentroid(Roi roi){
    roi_poly = roi.getPolygon();
    double x_cent =0 ,y_cent = 0;
    x_cent_size = roi_poly.xpoints.size();
    for (int j = 0; j < x_cent_size; j++) {
        x_cent += roi_poly.xpoints[j];
        y_cent += roi_poly.ypoints[j];
    }
    int[] out = [(x_cent/x_cent_size).intValue(), (y_cent/x_cent_size).intValue()];
    return out;
}

public static Point2D[] int2PointArray(int[] x, int[] y){
    if(x.length != y.length) {
        new ErrorMsg("inputs don't have same length");
        return null;
    }
    Point2D[] out = new Point2D[x.length];
    for(int i = 0;i<x.length;i++) {
        out[i] = new Point2D.Double();
        out[i].setLocation(x[i], y[i]);
    }
    return out;
}

// Load environment
String inputdir = "C:\\Users\\noambox\\Pictures\\test roi align\\rotat\\";
ImagePlus imp = IJ.openImage(inputdir + "image_after.tif");
ArrayList<PolygonRoi> rois = AAP_woStimulus.loadROIZip(inputdir + "roi.zip")
ImagePlus imp_source = IJ.openImage(inputdir + "image_before.tif");

// find maxima in image as data set (ds)
tolarance = 5;
MaximumFinder mf = new MaximumFinder();
Polygon imp_mp = mf.getMaxima(imp.getProcessor(),tolarance, true); // imp max points
int max_points = imp_mp.xpoints.size() > 15 ? 15 : imp_mp.xpoints.size();
PointRoi point_roi = new PointRoi(imp_mp.xpoints, imp_mp.ypoints, max_points)

// Visualize
RoiManager roi_man = new RoiManager();
roi_man.open(inputdir + "roi.zip")
point_roi.setPointType(0);
point_roi.setSize(3)
point_roi.drawPixels(imp.getProcessor())
imp.show()
//point_roi.draw(imp);
IJ.run("In [+]", "");
IJ.run("In [+]", "");
roi_man.addRoi((Roi)point_roi)
roi_man.show()
roi_man.close()

// compare ds to existing centers and extract best correspondences
ArrayList<Point2D> corresp_point = new ArrayList();
double dist = 0;
int closest = 0;
for (int i = 0; i <point_roi.getXCoordinates().size(); i++) {
    for (int j = 0; j < rois.size(); j++) {
        int[] roi_centroid = findCentroid(rois.get(j));
        dist = Point2D.distance(imp_mp.xpoints[i], imp_mp.ypoints[i], roi_centroid[0],roi_centroid[1]);
        if(j == 0) {
            closest = j;
            shortest_length = dist;
        }
        else{
            if(dist < shortest_length){
                closest = j;
                shortest_length = dist;
            }
        }
    }
    int[] near_neighb = findCentroid(rois.get(closest));
    corresp_point.add(new Point2D.Double(near_neighb[0],near_neighb[1]))
}

// find transformations
double[] corr_dest,corr_source;
ArrayList matches = new ArrayList();
for(int mapping = 0; mapping < corresp_point.size(); ++mapping) {
    corr_dest = [(Double)imp_mp.xpoints[mapping], (Double)imp_mp.ypoints[mapping]]
    corr_source = [corresp_point.get(mapping).getX(), corresp_point.get(mapping).getY()];
    matches.add(new PointMatch(new Point(corr_source), new Point(corr_dest)));
}
AffineModel2D afm = new AffineModel2D();
((Model)afm).fit(matches);
double[] trans_matrix = new double[6];
((Affine2D)afm).toArray(trans_matrix);

// apply on all Roi's
int enlarge_factor = 0;
int[] image_bounds = [imp.getWidth(), imp.getHeight()];
AffineTransform aft = new AffineTransform();
aft.setTransform(trans_matrix[0], trans_matrix[1], trans_matrix[2], trans_matrix[3], trans_matrix[4], trans_matrix[5]);
ArrayList<Roi> roi_out = Stimulus_API.TransformRois(rois, aft, enlarge_factor, image_bounds);

// compare
RoiManager roi_man_new = new RoiManager();
roi_man.open(inputdir + "roi.zip")
roi_man_new.setTitle("ROI MAN NEW");
for (int i = 0; i < roi_out.size(); i++) {
    roi_man_new.addRoi(roi_out.get(i))
}
roi_man_new.addRoi((Roi)point_roi);
roi_man_new.show();
