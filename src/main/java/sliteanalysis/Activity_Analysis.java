package sliteanalysis;

import fiji.threshold.Auto_Local_Threshold;
import fiji.threshold.Auto_Threshold;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.blob.Blob;
import ij.blob.ManyBlobs;
import ij.gui.*;
import ij.io.OpenDialog;
import ij.io.SaveDialog;
import ij.measure.Calibration;
import ij.plugin.ImageCalculator;
import ij.plugin.ZProjector;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.MaximumFinder;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import net.imagej.Main;
import trainableSegmentation.WekaSegmentation;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;

/*
author: Noam Cohen
*/
/* This plugin is for the use of Calcium activity Analysis.
It detects cells, Calculates their activity trace and returns the dF/F of the interesting ones
*/

/*@Plugin(type = Command.class, headless = true,
        menuPath = "Analyze>Activity Analysis")*/
public class Activity_Analysis implements PlugInFilter {
    // User Parameters
    private double BL_NOISE_PREC = 0.3;
    private double LPF_THRES = 0.002;
    private double DT = 0.1;
    private Checkbox ELLIPS_CHB;

    // Parameters - Erode
    private String MORPH_PROC = "Open";
    private int MORPH_ITER = 1;
    private int MORPH_COUNT = 5;

    // Parameters - Find Maxima
    private double FM_TOL = 8;
    private int FM_OUT = 1; // IN_TOLERANCE

    // Parameters -  Threshold
    private String THRESH_METHOD = "Mean"; // Midgray
    private int THRESH_RADIUS = 10; // 15
    private double THRESH_P1 = -30; // -80

    // Parameters - Blob filtering
    private int ELLIPSE_a = 100; // pixel 100
    private int ELLIPSE_b = 55; // pixel 55
    private int X_AXIS_FOV = 500; // [um] Horizontal real fov in image
    private double CELL_R_MIN = 5; // [um]
    private double CELL_R_MAX = 30; // [um]
    private int CIRC_MAX = 1500;
    private double AR_MAX = 2.8;
    private ManyBlobs allBlobs;
    private ImageStack stack; // the stack that we work on
    private ImagePlus imp;

    private PolygonRoi currentROI;

    // Parameters - Segmentation
    // TODO load classifier and do feature selection
    private String CLASSIFPATH = "C:\\Users\\niel\\Documents\\Noam\\Repos\\Fiji\\Fiji"; // SS version
    private String CLASSI = "\\Self Customized Parameters\\Classifiers\\classifier1-12.model";

//    private String CLASSIFPATH = "C:\\Users\\noambox\\Desktop\\AAP plugin\\ClassifierTrain";
//    private String CLASSI = "\\classifier1-8.model";

    // Visualization
    private Overlay overLay = new Overlay();

    /* METHODS */
    // Parameters for ZProfile modification
    private static String[] choices = {"time", "z-axis"};

    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;
        return 1;
    }

    /*Run method for full Activity Analysis*/
    public void run(ImageProcessor ip) {
//        this.imp = IJ.getImage();
        IJ.hideProcessStackDialog = true;
        this.stack = imp.getStack();

        // Validate data type (Stacks)
        //ImagePlus imp = IJ.getImage();
        if (imp.getStackSize()==1)
        {IJ.error("Stack required"); return;}

        // Open GUI
        UserInterface();

        // Get Average image for further Segmentation
        ImagePlus avr_img = getAverageIm();

        // Segment data with classifier
        File f1 = new File(this.CLASSIFPATH);
        if(!f1.exists()){
            this.CLASSIFPATH = "C:\\Users\\noambox\\Documents\\NielFiji-repo\\Fiji"; // load noambox version
            File f2 = new File(this.CLASSIFPATH);
            if(!f2.exists())
            {
                IJ.showMessage("Error", "There is no classifier in speciied path....");
                return;
            }
        }
        WekaSegmentation segmentator = new WekaSegmentation( avr_img );
        segmentator.loadClassifier(this.CLASSIFPATH + this.CLASSI);
        ImagePlus imp_prob = segmentator.applyClassifier(avr_img, 0, true); // get probabilities image

//                                ImagePlus imp_temp_0 = new ImagePlus();
//                                imp_temp_0.setImage(imp_prob);
//                                imp_temp_0.show();
//                                IJ.run("In [+]", "");
//                                IJ.run("In [+]", "");

        // Threshold ,Binary & Erode
//        ImagePlus imp_prob = IJ.openImage(path+"ProbImage.tif"); // DEBUG
        ImageStack probStack = imp_prob.getStack();
        probStack.deleteLastSlice();
        imp_prob.setStack(probStack);
        ImageConverter converter = new ImageConverter(imp_prob);
        converter.convertToGray8();
        Auto_Local_Threshold thresholder = new Auto_Local_Threshold();
        Object[] result = thresholder.exec(imp_prob, THRESH_METHOD, THRESH_RADIUS, THRESH_P1, 0, true);        // Better to use auto local threshold than auto-threshold
        imp_prob = ((ImagePlus) result[0]);

        // Find maxima implementation
        MaximumFinder maximumFinder = new MaximumFinder();
        ByteProcessor maxmask = maximumFinder.findMaxima(avr_img.getProcessor(), this.FM_TOL, ImageProcessor.NO_THRESHOLD, this.FM_OUT, true, false);
        ImagePlus max_imp = new ImagePlus();
        maxmask.erode(1,255);
        max_imp.setProcessor(maxmask);

//                                ImagePlus imp_temp_1 = new ImagePlus();
//                                imp_temp_1.setImage(imp_prob);
//                                imp_temp_1.show();
//                                IJ.run("In [+]", "");
//                                IJ.run("In [+]", "");
//
//                                max_imp.show();
//                                IJ.run("In [+]", "");
//                                IJ.run("In [+]", "");

        IJ.run(imp_prob, "Options...", "iterations=" + MORPH_ITER + " count=" + MORPH_COUNT + " black do=" + MORPH_PROC);
        ImageCalculator impcalculator = new ImageCalculator();
        imp_prob = impcalculator.run("OR create", imp_prob, max_imp);

//                                ImagePlus imp_temp_3 = new ImagePlus();
//                                imp_temp_3.setImage(imp_prob);
//                                imp_temp_3.show();
//                                IJ.run("In [+]", "");
//                                IJ.run("In [+]", "");

        IJ.run(imp_prob, "Watershed", "");

        // Add location features and detect blobs
        IJ.run(imp_prob, "Set Scale...", "distance=0.1 known=0.01 pixel=1 unit=unit"); // moves to [mm] scale

//                                ImagePlus imp_temp_2 = new ImagePlus();
//                                imp_temp_2.setImage(imp_prob);
//                                imp_temp_2.show();
//                                IJ.run("In [+]", "");
//                                IJ.run("In [+]", "");

        ManyBlobs cellLocation = FilterAndGetCells(imp_prob);

//                                cellLocation.getLabeledImage().show();
//                                IJ.run("In [+]", "");
//                                IJ.run("In [+]", "");

        // Add Cs Signal as blob feature
        MyBlobFeature myOwnFeature = new MyBlobFeature();
        Blob.addCustomFeature(myOwnFeature);
        int size = cellLocation.size();
        CellManager cm = new CellManager(avr_img, imp, this.overLay, this.DT);

                              /*  avr_img.show();
                                IJ.run("In [+]", "");
                                IJ.run("In [+]", "");*/

        // for evey Blob take the trace form the stack

        cellLocation.getLabeledImage();
        double dt = findSignalDt();
        for (int k=0; k<size;k++){
            try{
//            CalciumSignal ca_sig = new CalciumSignal(getBlobTimeProfile(cellLocation.get(k)), this.DT, (float) this.BL_NOISE_PREC, this.LPF_THRES);
            CalciumSignal ca_sig = new CalciumSignal(getBlobTimeProfile(cellLocation.get(k)), dt);

                overLay.add(this.currentROI);
            ca_sig.DeltaF();
            cm.addCell(ca_sig, this.currentROI);
            }
            catch(Exception e){
                IJ.showMessage(e.getMessage());
            }
//                                double a = cellLocation.get(k).getAreaConvexHull();
//                                double b = cellLocation.get(k).getEnclosedArea();
//                                double c = cellLocation.get(k).getAspectRatio();
//                                double d = cellLocation.get(k).getCircularity();
//                                IJ.showMessage("cell " + k+"\nAreaConv " + a + " AreaEnc " + b +"\nAspec " + c + " Circu " + d);
        }
        overLay.setLabelColor(Color.WHITE);
        overLay.setFillColor(Color.GREEN);
        overLay.setStrokeColor(Color.YELLOW);
        avr_img.setOverlay(overLay);
        avr_img.show();
        IJ.run("In [+]", "");
        IJ.run("In [+]", "");
        IJ.selectWindow("Log");
        // IJ.run("Close");
        return;
    }


    private double findSignalDt() {
        String fileTitle = imp.getTitle().toLowerCase();
        if(fileTitle.contains("hz") == true){
            int indx = fileTitle.indexOf("hz");
            String str =  fileTitle.substring(indx-2,indx);
            return 1/Double.valueOf(str);
        }
        else{
            return 0; // Hz
        }
    }

    public static double findSignalDt(String filename) {
        String fileTitle = filename.toLowerCase();
        if(fileTitle.contains("hz") == true){
            int indx = fileTitle.indexOf("hz");
            String str =  fileTitle.substring(indx-2,indx);
            return 1/Double.valueOf(str);
        }
        else{
            return 1; // Hz
        }
    }

    private void UserInterface(){
    /* Method fore opening the user interface */

//        GenericDialog gd = new GenericDialog("Activity Analysis settings");
//        Panel panel = new
//        gd.addPanel();
//        gd.addNumericField("Gain:",default_amplitude,2);
//
//        this.default_amplitude = (int) gd.getNextNumber();
//        gd.showDialog();
//        if (gd.wasCanceled()) {
//            IJ.error("PlugIn canceled!");
//            return;
//        }

//        /*for ADI*/
//
//        GenericDialog gd = new GenericDialog("DeltaSlice settings");
////        gd.addDialogListener(dl);
//        gd.addNumericField("Base Line noise percentage:", this.BL_NOISE_PREC, 3);
//        gd.addNumericField("dt:",this.DT,3);
//        gd.addNumericField("LPF threshold (Hz):", this.LPF_THRES, 3);
//        gd.addCheckbox("Do ellipse filtering:", true);
//        gd.addCheckbox("Load Classifyer:", true);
//
//        gd.showDialog();
//
//        this.BL_NOISE_PREC = gd.getNextNumber();
//        this.DT = gd.getNextNumber();
//        this.LPF_THRES = gd.getNextNumber();
//        this.ELLIPS_CHB = (Checkbox) gd.getCheckboxes().get(0);
//        Checkbox classif_chbx = (Checkbox) gd.getCheckboxes().get(1);
//
//        if(classif_chbx.getState()){
//            OpenDialog od = new OpenDialog("Choose model file (Classifyer)");
//            CLASSIFPATH = od.getDirectory();
//            CLASSI = od.getFileName();
//        }
//        if (gd.wasCanceled()) {
//            IJ.error("PlugIn canceled!");
//            return;
//        }
    }

    /* method for complete calcium signal analysis */
    public void SingleCellActivityAnalysis(Blob cell){
        float[] trace = getBlobTimeProfile(cell);
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
            filteredBlobs = filterCirc.filterBlobs(0, 1, "LocationFeature", imp.getWidth(), imp.getHeight(), ELLIPSE_a, ELLIPSE_b);
        }
        else if(imp.getWidth() < imp.getHeight()){
            filteredBlobs = filterCirc.filterBlobs(0, 1, "LocationFeature", imp.getHeight(), imp.getWidth(), ELLIPSE_a, ELLIPSE_b);
        }
            IJ.log("** Location filtering - "+ filteredBlobs.size()+" left...");
            return filteredBlobs;
//        }
//          return filterCirc;
    }

    /* get signal values for specific blob */
    private float[] getBlobTimeProfile(Blob cell) {
        ImageProcessor ip = imp.getProcessor();
        double minThreshold = ip.getMinThreshold();
        double maxThreshold = ip.getMaxThreshold();
        Polygon blobContour = cell.getOuterContour();
        this.currentROI =  new PolygonRoi(blobContour.xpoints,blobContour.ypoints,blobContour.npoints, Roi.FREELINE) ;
        int size = this.stack.getSize();
        float[] values = new float[size];
        Calibration cal = imp.getCalibration();
        Analyzer analyzer = new Analyzer(imp);
        int measurements = Analyzer.getMeasurements();
        int current = imp.getCurrentSlice();
        for (int i=1; i<=size; i++) { // TODO check if trims number of samples
            ip = this.stack.getProcessor(i);
            if (minThreshold!=ImageProcessor.NO_THRESHOLD)
                ip.setThreshold(minThreshold,maxThreshold,ImageProcessor.NO_LUT_UPDATE);
            ip.setRoi(this.currentROI);
            ImageStatistics stats = ImageStatistics.getStatistics(ip, measurements, cal);
            analyzer.saveResults(stats, this.currentROI);
            values[i-1] = (float)stats.mean;
        }
        return values;
    }


    /* Get Average image for processing */
    private ImagePlus getAverageIm(){
        ZProjector stackimp = new ZProjector(this.imp);
        stackimp.setMethod(0); // 0 for average method
        stackimp.doProjection(true);
        ImagePlus avr_img = stackimp.getProjection();
        return avr_img;
    }

    static public double[] getAverageSignal(ImagePlus imp) {
        ImageProcessor ip = imp.getProcessor();
        ImageStack imp_stack = imp.getStack();
        double minThreshold = ip.getMinThreshold();
        double maxThreshold = ip.getMaxThreshold();
        int size = imp_stack.getSize();
        double[] values = new double[size];
        Calibration cal = imp.getCalibration();
        Analyzer analyzer = new Analyzer(imp);
        int measurements = Analyzer.getMeasurements();
        for (int i=1; i<=size; i++) {
            ip = imp_stack.getProcessor(i);
            if (minThreshold!=ImageProcessor.NO_THRESHOLD)
                ip.setThreshold(minThreshold,maxThreshold,ImageProcessor.NO_LUT_UPDATE);
            ImageStatistics stats = ImageStatistics.getStatistics(ip, measurements, cal);
            values[i-1] = stats.mean;
        }
        return values;
    }

    static public ArrayList<Double> getAverageSignal(ImagePlus imp, Roi roi) {
        ImageProcessor ip = imp.getProcessor();
        ImageStack imp_stack = imp.getStack();
        double minThreshold = ip.getMinThreshold();
        double maxThreshold = ip.getMaxThreshold();
        int size = imp_stack.getSize();
        ArrayList<Double> values = new ArrayList<>();
        Calibration cal = imp.getCalibration();
        int measurements = Analyzer.getMeasurements();
        for (int i=1; i<=size; i++) {
            ip = imp_stack.getProcessor(i);
            if (minThreshold!=ImageProcessor.NO_THRESHOLD)
                ip.setThreshold(minThreshold,maxThreshold,ImageProcessor.NO_LUT_UPDATE);
            ip.setRoi(roi);
            ImageStatistics stats = ImageStatistics.getStatistics(ip, measurements, cal);
            values.add(stats.mean);
        }
        return values;
    }

    static public double[] getAverageSignal(ImagePlus imp, int first, int last) {
        ImageProcessor ip = imp.getProcessor();
        ImageStack imp_stack = imp.getStack();
        double minThreshold = ip.getMinThreshold();
        double maxThreshold = ip.getMaxThreshold();
        //int size = imp_stack.getSize();
        double[] values = new double[last - first + 1];
        Calibration cal = imp.getCalibration();
        int measurements = Analyzer.getMeasurements();
        for (int i=first; i<=last; i++) {
            ip = imp_stack.getProcessor(i);
            if (minThreshold!=ImageProcessor.NO_THRESHOLD)
                ip.setThreshold(minThreshold,maxThreshold,ImageProcessor.NO_LUT_UPDATE);
            ImageStatistics stats = ImageStatistics.getStatistics(ip, measurements, cal);
            values[i-first] = stats.mean;
        }
        return values;
    }

    // TODO - Iterative watershed algorithm with minimum segment size
    private Blob iterativeBlobSplit(Blob blob){
        return blob;
    }

    // Duplicate image method from Auto_Local_Threshold
    private ImagePlus duplicateImage(ImageProcessor iProcessor) {
        int w = iProcessor.getWidth();
        int h = iProcessor.getHeight();
        ImagePlus iPlus = NewImage.createByteImage("Image", w, h, 1, NewImage.FILL_BLACK);
        ImageProcessor imageProcessor = iPlus.getProcessor();
        imageProcessor.copyBits(iProcessor, 0, 0, Blitter.COPY);
        return iPlus;
    }


    /** Tests the plugin. */
    public static void main(final String... args) {
        String path;
        ImagePlus imp;
        try {
//            path = "C:\\Users\\Noam\\Dropbox\\# graduate studies m.sc\\# SLITE\\ij - plugin data\\"; // LAB
//            imp = IJ.openImage(path + "FLASH_20msON_20Hz_SLITE_1.tif");

            path = "D:\\# Projects (Noam)\\# SLITE\\# DATA\\301115Retina - DATA\\Loc 3\\post\\";
            imp = IJ.openImage(path + "OFLASH_20msON_10sec_10Hz_SLITE_test.tif"); // DEBUG

            if(imp == null){
                throw new FileNotFoundException("Your not in Lab....");
            }
        }
        catch(FileNotFoundException error){
            path = "C:\\Users\\noambox\\Dropbox\\# Graduate studies M.Sc\\# SLITE\\ij - plugin data\\"; //HOME
            imp = IJ.openImage(path + "FLASH_20msON_20Hz_SLITE_1.tif"); // DEBUG
        }


        Activity_Analysis acta = new Activity_Analysis();
        acta.setup("", imp);
        imp.show();
        IJ.run("In [+]", "");
        IJ.run("In [+]", "");
        acta.run(imp.getProcessor());
    }

}
