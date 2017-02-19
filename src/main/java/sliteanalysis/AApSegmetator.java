package sliteanalysis;

import fiji.threshold.Auto_Local_Threshold;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.blob.Blob;
import ij.blob.ManyBlobs;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.io.OpenDialog;
import ij.io.RoiEncoder;
import ij.plugin.*;
import ij.plugin.filter.MaximumFinder;
import ij.plugin.frame.RoiManager;
import ij.process.ByteProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import jdk.internal.org.objectweb.asm.tree.analysis.Analyzer;
import net.imagej.Main;
import trainableSegmentation.WekaSegmentation;
import cellMagicWand.Constants;
import cellMagicWand.PolarTransform;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

/**
 * Created by noambox on 12/13/2015.
 */
public class AApSegmetator {

    /*-------------------------------------- Members and Variables --------------------------------------*/
    private ImagePlus imp;

    // User Parameters
    private double BL_NOISE_PREC = 0.3;
    private double LPF_THRES = 0.002;
    private int objective = 20; // ussually using 20X

    // Parameters - Erode
    private String MORPH_PROC = "Open";
    private int MORPH_ITER = 1;
    private int MORPH_COUNT = 5;

    // Parameters - Find Maxima and Cell Magic Wand Param
    private double FM_TOL = 8;
    private int FM_OUT = 1; // IN_TOLERANCE
    private int CM_MAX = 16;
    private int CM_MIN = 3;
    private double ENLARGEROI = 1;

    // Parameters -  Threshold
    private String THRESH_METHOD = "Mean"; // Midgray
    private int THRESH_RADIUS = 10; // 15
    private double THRESH_P1 = -30; // -80

    // Parameters - Blob filtering
    private int ELLIPSE_a = 80; // pixel 100
    private int ELLIPSE_b = 100; // pixel 55
    private int X_AXIS_FOV = 500; // [um] Horizontal real fov in image
    private double CELL_R_MIN = 5; // [um]
    private double CELL_R_MAX = 30; // [um]
    private int CIRC_MAX = 1500;
    private double AR_MAX = 2.8;
    private ManyBlobs allBlobs;

    // Parameters - Segmentation
    private String CLASSIFPATH = System.getProperty("user.dir") + "\\data";; // SS version
    private String CLASSI = "\\classifier\\classifier1-12.model";

    // Parameters Visualization and scale
    private Overlay overLay = new Overlay();
    private int ObjectiveM = 20;

    /*-------------------------------------- Methods --------------------------------------*/

    public AApSegmetator(ImagePlus t_imp){
        loadPrefs();
        this.imp = t_imp;
    }

    /* some basic setup functions for altering the parameters*/
    public void setupMorph(){

    }

    public void setupBlobFiltering(){

    }

    private void loadPrefs() {
        this.FM_TOL = Prefs.get("sliteanalysis.cFM_TOL", AAP_Constants.cFM_TOL);
        this.CM_MAX = (int) Prefs.get("sliteanalysis.cCM_MAX",AAP_Constants.cCM_MAX);
        this.ENLARGEROI = (int) Prefs.get("sliteanalysis.cENLARGEROI",AAP_Constants.cENLARGEROI);
        this.ELLIPSE_a = (int) Prefs.get("sliteanalysis.cELLIPSE_a",AAP_Constants.cELLIPSE_a);
        this.ELLIPSE_b = (int) Prefs.get("sliteanalysis.cELLIPSE_b",AAP_Constants.cELLIPSE_b);
    }

    /**
     * Segments Average image using Fast Random forest classfier and Find Maxima function
     * the probability image is morphologically processed and filtered using blob filtering
     * */
    public ArrayList<PolygonRoi> SegmentCellsML(){
        ArrayList<PolygonRoi> cell_rois = new ArrayList<>();

        // Validate data type (Stacks)
        File f1 = new File(this.CLASSIFPATH);
        if(!f1.exists()){
            this.CLASSIFPATH = System.getProperty("user.dir")+"\\Documents\\NielFiji-repo\\Fiji"; // load
            File f2 = new File(this.CLASSIFPATH);
            if(!f2.exists())
            {
                IJ.error("There is no classifier in specified path....");
                return cell_rois;
            }
        }

        // Open GUI
        //UserInterface();

        // Get Average image for further Segmentation
        ImagePlus avr_img = getAverageIm(this.imp);
        this.imp.show();
        WekaSegmentation segmentator = new WekaSegmentation( avr_img );
        segmentator.loadClassifier(this.CLASSIFPATH + this.CLASSI);
        ImagePlus imp_prob = segmentator.applyClassifier(avr_img, 0, true); // get probabilities image


        // Threshold ,Binary & Erode
        ImageStack probStack = imp_prob.getStack();
        probStack.deleteLastSlice();
        imp_prob.setStack(probStack);
        ImageConverter converter = new ImageConverter(imp_prob);
        converter.convertToGray8();

        // use non-local mean segmentation to find cells
        cell_rois = SegmentCellsCWT_engine(imp_prob);
        return cell_rois;

//        Auto_Local_Threshold thresholder = new Auto_Local_Threshold();
//        Object[] result = thresholder.exec(imp_prob, THRESH_METHOD, THRESH_RADIUS, THRESH_P1, 0, true);        // Better to use auto local threshold than auto-threshold
//        imp_prob = ((ImagePlus) result[0]);
//
//        // Find maxima implementation
//        MaximumFinder maximumFinder = new MaximumFinder();
//        ByteProcessor maxmask = maximumFinder.findMaxima(avr_img.getProcessor(), this.FM_TOL, ImageProcessor.NO_THRESHOLD, this.FM_OUT, true, false);
//        ImagePlus max_imp = new ImagePlus();
//        maxmask.erode(1, 255);
//        max_imp.setProcessor(maxmask);
//
////                                imp.show();
////                                IJ.run("In [+]", "");
////                                IJ.run("In [+]", "");
//
////                                ImagePlus imp_temp_1 = new ImagePlus();
////                                imp_temp_1.setImage(imp_prob);
////                                imp_temp_1.show();
////                                IJ.run("In [+]", "");
////                                IJ.run("In [+]", "");
////
////                                max_imp.show();
////                                IJ.run("In [+]", "");
////                                IJ.run("In [+]", "");
//
//        IJ.run(imp_prob, "Options...", "iterations=" + MORPH_ITER + " count=" + MORPH_COUNT + " black do=" + MORPH_PROC);
//        ImageCalculator impcalculator = new ImageCalculator();
//        imp_prob = impcalculator.run("OR create", imp_prob, max_imp);
//
////                                ImagePlus imp_temp_3 = new ImagePlus();
////                                imp_temp_3.setImage(imp_prob);
////                                imp_temp_3.show();
////                                IJ.run("In [+]", "");
////                                IJ.run("In [+]", "");
//
//        IJ.run(imp_prob, "Watershed", "");
//
//        // scale
//        String scale_string = "250";
//        switch(this.ObjectiveM){
//            case(10):
//                scale_string = "1000";
//                break;
//            case(20):
//                scale_string = "490";
//                break;
//            case(40):
//                scale_string = "250";
//                break;
//        }
//        IJ.run(imp_prob, "Set Scale...", "distance=250 known="+scale_string+" pixel=1 unit=um"); // moves to [um] scale
//
//                                ImagePlus imp_temp_2 = new ImagePlus();
//                                imp_temp_2.setImage(imp_prob);
//                                imp_temp_2.show();
//                                IJ.run("In [+]", "");
//                                IJ.run("In [+]", "");
//        ManyBlobs cellLocation = FilterAndGetCells(imp_prob);
//
////                                cellLocation.getLabeledImage().show();
////                                IJ.run("In [+]", "");
////                                IJ.run("In [+]", "");
//
//        for (int i = 0; i < cellLocation.size(); i++) {
//            Polygon blobContour = cellLocation.get(i).getOuterContour();
//            if (cell_rois != null)
//                cell_rois.add(new PolygonRoi(blobContour.xpoints, blobContour.ypoints, blobContour.npoints, Roi.FREELINE));
//        }
//            return cell_rois;
    }

    /**
     * Wrapper for non-local mean segmentation
     * @return
     */
    public ArrayList<PolygonRoi> SegmentCellsCWT() {
        return SegmentCellsCWT_engine(imp);
    }

    /**
     * Segments using Cell wand tool and find maxima*/
    public ArrayList<PolygonRoi> SegmentCellsCWT_engine(ImagePlus image){
        ArrayList<PolygonRoi> cell_rois = new ArrayList<PolygonRoi>();

        // Remove Scale
        IJ.run(image, "Set Scale...", "distance=0");

        // Find maxima implementation
        MaximumFinder mf = new MaximumFinder();
        Cell_Magic_Wand_Tool cmwt = new Cell_Magic_Wand_Tool();
        cmwt.minDiameter = this.CM_MIN; // TODO move to Constant parameters and allow settings
        cmwt.maxDiameter = this.CM_MAX;

//        ImagePlus imp_temp_0 = new ImagePlus();
//        imp_temp_0.setImage(image);
//        imp_temp_0.show();
//        IJ.run("In [+]", "");
//        IJ.run("In [+]", "");

        Polygon maxPoints = mf.getMaxima(image.getProcessor(), this.FM_TOL, true);
        for (int i = 0; i < maxPoints.npoints; i++) {
            try{
                PolygonRoi t_roi = cmwt.makeRoi(maxPoints.xpoints[i],maxPoints.ypoints[i],image);
                if(t_roi!=null){
                    PolygonRoi enlarge_roi = (PolygonRoi) RoiEnlarger.enlarge(t_roi,this.ENLARGEROI );
                    Point left_rect_corner = enlarge_roi.getBounds().getLocation();
                    if(left_rect_corner.x == 0 | left_rect_corner.y == 0 |  // checks if the surrounding ROI gets out frame limits
                        left_rect_corner.x >= (imp.getWidth() - enlarge_roi.getBounds().getWidth()) |
                        left_rect_corner.y >= (imp.getHeight() - enlarge_roi.getBounds().getHeight()))
                                continue;
                    cell_rois.add((PolygonRoi) RoiEnlarger.enlarge(t_roi,this.ENLARGEROI ));}
    }
            catch(StackOverflowError e){
                continue;
            }
        }
        return cell_rois;
    }

    /* method for extracting cells data with ij_blob plugin */
    private ManyBlobs FilterAndGetCells(ImagePlus imp) {
        allBlobs = new ManyBlobs(imp); // Extended ArrayList
        allBlobs.setBackground(0);
        allBlobs.findConnectedComponents(); // Start the Connected Component Algorithm
        MyBlobFeature myOwnFeature = new MyBlobFeature();
        Blob.addCustomFeature(myOwnFeature);
        ManyBlobs filteredBlobs = new ManyBlobs();

        // Setting filter's parameters
        double scalinFactor = this.X_AXIS_FOV / imp.getWidth() * this.X_AXIS_FOV / imp.getWidth();
        int areaCovxMin = (int) ((this.CELL_R_MIN * this.CELL_R_MIN * Math.PI) / scalinFactor);
        int areaCovxMax = (int) ((this.CELL_R_MAX * this.CELL_R_MAX * Math.PI) / scalinFactor);

        ManyBlobs filterArea = allBlobs.filterBlobs(areaCovxMin, areaCovxMax, Blob.GETAREACONVEXHULL);
        IJ.log("** Area filtering - "+ filterArea.size()+" left...");
        ManyBlobs filterAspRatio = filterArea.filterBlobs(0.1, this.AR_MAX, Blob.GETASPECTRATIO); // perfect area yields 1
        IJ.log("** Ratio filtering - "+ filterAspRatio.size()+" left...");
        ManyBlobs filterCirc = filterAspRatio.filterBlobs(0, this.CIRC_MAX, Blob.GETCIRCULARITY); // perfect circle yields 1000
        IJ.log("** Circ filtering - "+ filterCirc.size()+" left...");
//        if(this.ELLIPS_CHB.getState()){
        if(imp.getWidth() > imp.getHeight()){
            filteredBlobs = filterCirc.filterBlobs(0, 1, "LocationFeature", imp.getWidth(), imp.getHeight(),ELLIPSE_b, ELLIPSE_a);
        }
        else if(imp.getWidth() < imp.getHeight()){
            filteredBlobs = filterCirc.filterBlobs(0, 1, "LocationFeature", imp.getHeight(), imp.getWidth(), ELLIPSE_b, ELLIPSE_a);
        }
        IJ.log("** Location filtering - "+ filteredBlobs.size()+" left...");
        return filteredBlobs;
//        }
//          return filterCirc;
    }

    public ArrayList<PolygonRoi> SegmentForgroundCellsCWT(ImagePlus _bkg_imp) {
        ArrayList<PolygonRoi> cells = SegmentCellsCWT();
        if(_bkg_imp.getProcessor() != null){
            return FilterROIsByBackground(cells, _bkg_imp);
        }
        else{
            return cells;
        }
    }

    public ArrayList<PolygonRoi> SegmentForgroundCellsML(OpenDialog _mdl_path, ImagePlus _bkg_imp) {
        CLASSIFPATH = _mdl_path.getDirectory();
        CLASSI = _mdl_path.getFileName();
        ArrayList<PolygonRoi> cells = SegmentCellsML();
        if(_bkg_imp != null){
            return FilterROIsByBackground(cells, _bkg_imp);
        }
        else{
            return cells;
        }
    }

    /**
     * filters the input roi array according to a backgriound image
     * @param rois input ROIS
     * @param _bkg_imp binary background image
     * @return
     */
    public static  ArrayList<PolygonRoi> FilterROIsByBackground(ArrayList<PolygonRoi> rois, ImagePlus _bkg_imp){
        for(int i = rois.size()-1 ; i > -1; i--){
            //filter rois in background
            if(_bkg_imp.getProcessor() != null) {
                double[] roi_center = rois.get(i).getContourCentroid();
                if (_bkg_imp.getProcessor().getPixel((int)roi_center[0],(int)roi_center[1]) != 0) {
                    rois.remove(i);
                    continue;
                }
            }
        }
        return rois;
    }

    /* Get Average image for processing */
    public static ImagePlus getAverageIm(ImagePlus imp){
        ZProjector stackimp = new ZProjector(imp);
        stackimp.setMethod(0); // 0 for average method
        stackimp.doProjection(true);
        ImagePlus avr_img = stackimp.getProjection();
        return avr_img;
    }
    public void setFmTolerance(double n){FM_TOL = n;}
    public void setCMWMax(int n){CM_MAX = n;}
    public void setCMWMin(int n){CM_MIN = n;}

    public void setEnlargeRoi(int n){ENLARGEROI = n;}

    /* Main function for debugging */
    public static void main(final String... args) throws FileNotFoundException {
        String path = System.getProperty("user.dir") + "\\data";
        System.out.print(path + '\n');
//        try {
//            path = "C:\\Users\\noambox\\Desktop\\AAP plugin\\ClassifierTrain\\151206TRAIN\\"; // LAB
//            ImagePlus imp_test = IJ.openImage(path + "av1.tif");
//            if(imp_test == null){
//                throw new FileNotFoundException("Your not in Lab....");
//            }
//        }
//        catch(FileNotFoundException error){
//            path = "C:\\Users\\noambox\\Dropbox\\# Graduate studies M.Sc\\# SLITE\\ij - plugin data\\"; //HOME
//        }

//        ImagePlus imp = IJ.openImage(path + "\\noam\\ca_av_1.tif"); // DEBUG
        ImagePlus imp = IJ.openImage(path + "\\noam\\ca_mov_1.tif");
//        ImagePlus imp = IJ.openImage(path + "av1.tif"); // DEBUG

        AApSegmetator aap_seg = new AApSegmetator(imp);
        ArrayList<PolygonRoi> rois;

        rois = aap_seg.SegmentCellsML();
//        rois = aap_seg.SegmentCellsCWT();

        RoiManager rm = new RoiManager();
        for (int i = 0; i < rois.size(); i++) {
            rm.addRoi(rois.get(i));
        }
        imp.show();
        IJ.run("In [+]", "");
        IJ.run("In [+]", "");
    }
}
