import fiji.threshold.Auto_Local_Threshold;
import fiji.threshold.Auto_Threshold;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.Binary;
import imagescience.transform.Crop;
import net.imagej.ops.Ops;
import net.imagej.ops.crop.CropImgPlus;
import net.imglib2.algorithm.morphology.MorphologyUtils;
import org.scijava.command.Command;
import ij.blob.*;
import org.junit.*;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import ij.*;
import ij.plugin.*;
import ij.plugin.filter.RGBStackSplitter;
import ij.process.*;
import ij.plugin.PlugIn;
import net.imagej.ImageJ;
import net.imagej.Main;
import trainableSegmentation.*;

import ij.gui.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.CropImageFilter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.BinaryOperator;

/*
author: Noam Cohen
*/
/* This plugin is tended for the use of Calcium activity Analysis.
It detects cells, Calculates their activity trace and returns the dF/F of the interesting ones
*/

@Plugin(type = Command.class, headless = true,
        menuPath = "Analyze>Activity Analysis")
public class Activity_Analysis implements PlugIn {

    // Parameters Erode
    private String MORPH_PROC = "Open";
    private String MORPH_ITER = "2";
    private String MORPH_COUNT = "2";

    // Parameter Threshold
    private String THRESH_METHOD = "MidGrey";
    private int THRESH_RADIUS = 15;
    private double THRESH_P1 = -80;

    // Parameters for blob filtering
    private int RADIUSFILTER = 50;
    private ManyBlobs allBlobs;
    private ImageStack stack; // the stack that we work on
    private ImagePlus imp;

    // Parameters for ZProfile modification
    private static String[] choices = {"time", "z-axis"};

    /*Run method for full Activity Analysis*/
    public void run(String arg) {
      /*  // 4 Debugging - load stack
        ImagePlus imp = IJ.openImage("C:\\Users\\noambox\\Desktop\\FLASH_20msON_10Hz_SLITE_1.tif");

        // Validate data type (Stacks)
        //ImagePlus imp = IJ.getImage();
        if (imp.getStackSize()==1)
        {IJ.error("Stack required"); return;}

        // Get Average image for further Segmentation
        ZProjector stackimp = new ZProjector(imp);
        stackimp.setMethod(0); // 0 for average method
        stackimp.doProjection(true);
        ImagePlus avr_img = stackimp.getProjection();

        // Segment data with classifier
        WekaSegmentation segmentator = new WekaSegmentation( avr_img );
        segmentator.loadClassifier("C:\\Users\\noambox\\Documents\\NielFiji-repo\\Fiji" +
                "\\Self Customized Parameters\\Classifiers\\classifier1.model");
        ImagePlus prob = segmentator.applyClassifier(avr_img, 0, true); // get probabilities image
*/

        // Binary, Erode
        imp = IJ.openImage("C:\\Users\\noambox\\Desktop\\Test Images - ImageJ\\ProbImage.tif"); // DEBUG
        ImageStack probStack = imp.getStack();
        probStack.deleteLastSlice();
        imp.setStack(probStack);
        ImageConverter converter = new ImageConverter(imp);
        converter.convertToGray8();
        Auto_Local_Threshold thresholder = new Auto_Local_Threshold();
        Object[] result = thresholder.exec(imp, THRESH_METHOD, THRESH_RADIUS, THRESH_P1, 0, false);
        IJ.run(imp, "Options...", "iterations=" + MORPH_ITER + "count=" + MORPH_COUNT + "black do=" + MORPH_PROC);
        IJ.run(imp, "Watershed", "");

        // Add features and detect blobs
        ManyBlobs allBlobs = new ManyBlobs(imp); // Extended ArrayList
        allBlobs.setBackground(1);
        allBlobs.findConnectedComponents(); // Start the Connected Component Algorithm
        MyBlobFeature myOwnFeature = new MyBlobFeature();
        Blob.addCustomFeature(myOwnFeature);

        //ManyBlobs filteredBlobs = allBlobs.filterBlobs(0,RADIUSFILTER,"LocationFeature",);
        ManyBlobs filterArea = allBlobs.filterBlobs(0.003,1, Blob.GETENCLOSEDAREA);
        ManyBlobs filterAspRatio = filterArea.filterBlobs(0.5, 2.5, Blob.GETASPECTRATIO);
        ImagePlus label_imp = filterAspRatio.getLabeledImage();
        label_imp.show();


       /* blobfeature();*/
/*        ImagePlus imp = IJ.openImage("C:\\Users\\noambox\\Desktop\\FLASH_20msON_10Hz_SLITE_1.tif");
        // for evey Blob take the trace form the stack
        int size = stack.getSize();

        for (int k=1; k<=size; k++){
            getBlobTimeProfile(filterAspRatio.get(k));
        }
//        prob_image.show();*/
        return;
    }


/*    public void blobfeature(){
        //URL url = this.getClass().getClassLoader().getResource("3blobs.tif");
       // ImagePlus ip = new ImagePlus(url.getPath());
        ManyBlobs mb = new ManyBlobs(imp);
        mb.findConnectedComponents();
        MyBlobFeature test = new MyBlobFeature();
        Blob.addCustomFeature(test);
        int a = 10;
        float c = 1.5f;
        ManyBlobs filtered = mb.filterBlobs(0,10, "LocationFeature",imp.getWidth(),imp.getHeight());
    }*/

    private void Delete(){
    }

    /*get signal values for specific blob*/
    private float[] getBlobTimeProfile(Blob cell) {
        ImageProcessor ip = imp.getProcessor();
        double minThreshold = ip.getMinThreshold();
        double maxThreshold = ip.getMaxThreshold();
        Polygon blobContour = cell.getOuterContour();
        PolygonRoi roi =  new PolygonRoi(blobContour.xpoints,blobContour.ypoints,blobContour.npoints, Roi.FREELINE) ;
        int size = stack.getSize();
        float[] values = new float[size];
        Calibration cal = imp.getCalibration();
        Analyzer analyzer = new Analyzer(imp);
        int measurements = Analyzer.getMeasurements();
        int current = imp.getCurrentSlice();
        for (int i=1; i<=size; i++) {
            ip = stack.getProcessor(i);
            if (minThreshold!=ImageProcessor.NO_THRESHOLD)
                ip.setThreshold(minThreshold,maxThreshold,ImageProcessor.NO_LUT_UPDATE);
            ip.setRoi(roi);
            ImageStatistics stats = ImageStatistics.getStatistics(ip, measurements, cal);
            analyzer.saveResults(stats, roi);
            values[i-1] = (float)stats.mean;
        }
        return values;
    }
/*
        if (imp.getStackSize()==1)
        {IJ.error("Stack required"); return;}
        String ErrorMessage = new String("One of your values was not properly formatted.\nThe default values will be used.");
        GenericDialog d = new GenericDialog("Kalman Stack Filter");
        d.addNumericField("Acquisition_noise variance estimate:", percentvar, 2);
        d.addNumericField("Bias to be placed on the prediction:", gain, 2);
        d.showDialog();
        if(d.wasCanceled()) return;
        double percentvar = d.getNextNumber();
        gain = d.getNextNumber();
        if(d.invalidNumber())
        {IJ.error("Invalid input Number"); return;}
        if(percentvar>1.0||gain>1.0||percentvar<0.0||gain<0.0){
            IJ.error(ErrorMessage);
            percentvar = 0.05;
            gain = 0.8;
        }
        ImageStack stack = imp.getStack();
        if (imp.getBitDepth()==24)
            stack = filterRGB(stack, percentvar, gain);
        else
            filter(stack, percentvar, gain);
        imp.setStack(null, stack);*//**//**/

    /*public void filter(ImageStack stack, double percentvar, double gain) {
        ImageProcessor ip = stack.getProcessor(1);
        int bitDepth = 0;
        if (ip instanceof ByteProcessor)
            bitDepth = 8;
        else if (ip instanceof ShortProcessor)
            bitDepth = 16;
        else if (ip instanceof FloatProcessor)
            bitDepth = 32;
        else
            throw new IllegalArgumentException("RGB stacks not supported");

        int width = stack.getWidth();
        int height = stack.getHeight();
        int dimension = width*height;
        int stacksize = stack.getSize();
        double[] stackslice = new double[dimension];
        double[] filteredslice = new double[dimension];
        double[] noisevar = new double[dimension];
        double[] average = new double[dimension];
        double[] predicted = new double[dimension];
        double[] predictedvar = new double[dimension];
        double[] observed = new double[dimension];
        double[] Kalman = new double[dimension];
        double[] corrected = new double[dimension];
        double[] correctedvar = new double[dimension];

        for (int i=0; i<dimension; ++i)
            noisevar[i] = percentvar;
        predicted = toDouble(stack.getPixels(1), bitDepth);
        predictedvar = noisevar;

        for(int i=1; i<stacksize; ++i) {
            IJ.showProgress(i, stacksize);
            stackslice = toDouble(stack.getPixels(i+1), bitDepth);
            observed = toDouble(stackslice, 64);
            for(int k=0;k<Kalman.length;++k)
                Kalman[k] = predictedvar[k]/(predictedvar[k]+noisevar[k]);
            for(int k=0;k<corrected.length;++k)
                corrected[k] = gain*predicted[k]+(1.0-gain)*observed[k]+Kalman[k]*(observed[k] - predicted[k]);
            for(int k=0;k<correctedvar.length;++k)
                correctedvar[k] = predictedvar[k]*(1.0 - Kalman[k]);
            predictedvar = correctedvar;
            predicted = corrected;
            stack.setPixels(fromDouble(corrected, bitDepth), i+1);
        }
    }*/
/*
    public ImageStack filterRGB(ImageStack stack, double percentvar, double gain) {
        RGBStackSplitter splitter = new RGBStackSplitter();
        splitter.split(stack, false);
        filter(splitter.red, percentvar, gain);
        filter(splitter.green, percentvar, gain);
        filter(splitter.blue, percentvar, gain);
        RGBStackMerge merge = new RGBStackMerge();
        ImageProcessor ip = splitter.red.getProcessor(1);
        return merge.mergeStacks(ip.getWidth(), ip.getHeight(), splitter.red.getSize(), splitter.red, splitter.green, splitter.blue, false);
    }

    public Object fromDouble(double[] array, int bitDepth) {
        switch (bitDepth) {
            case 8:
                byte[] bytes = new byte[array.length];
                for(int i=0; i<array.length; i++)
                    bytes[i] = (byte)array[i];
                return bytes;
            case 16:
                short[] shorts = new short[array.length];
                for(int i=0; i<array.length; i++)
                    shorts[i] = (short)array[i];
                return shorts;
            case 32:
                float[] floats = new float[array.length];
                for(int i=0; i<array.length; i++)
                    floats[i] = (float)array[i];
                return floats;
        }
        return null;
    }
*/
    /*
    public double[] toDouble(Object array, int bitDepth) {
        double[] doubles = null;
        switch (bitDepth) {
            case 8:
                byte[] bytes = (byte[])array;
                doubles = new double[bytes.length];
                for(int i=0; i<doubles.length; i++)
                    doubles[i] = (bytes[i]&0xff);
                break;
            case 16:
                short[] shorts = (short[])array;
                doubles = new double[shorts.length];
                for(int i=0; i<doubles.length; i++)
                    doubles[i] = (shorts[i]&0xffff);
                break;
            case 32:
                float[] floats = (float[])array;
                doubles = new double[floats.length];
                for(int i=0; i<doubles.length; i++)
                    doubles[i] = floats[i];
                break;
            case 64:
                double[] doubles0 = (double[])array;
                doubles = new double[doubles0.length];
                for(int i=0; i<doubles.length; i++)
                    doubles[i] = doubles0[i];
                break;
        }
        return doubles;
    }
    */

    // Iterative watershed for further dev
    // IMPLEMENT
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
        //Main.launch(args);
        IJ.runPlugIn(Activity_Analysis.class.getName(), "");
    }

}