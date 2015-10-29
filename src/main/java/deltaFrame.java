/**
 * Created by Noam on 10/19/2015.
 */
import biz.source_code.dsp.filter.FilterCharacteristicsType;
import biz.source_code.dsp.filter.FilterPassType;
import biz.source_code.dsp.filter.IirFilterCoefficients;
import biz.source_code.dsp.filter.IirFilterDesignFisher;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.blob.Blob;
import ij.gui.GenericDialog;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.io.FileSaver;
import ij.io.Opener;
import ij.measure.Calibration;
import ij.plugin.ImageCalculator;
import ij.plugin.PlugIn;
import ij.plugin.filter.Analyzer;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.util.StringSorter;
import sliteanalysis.CalciumSignal;
import sun.plugin.javascript.navig.ImageArray;
import weka.core.AlgVector;

import java.awt.*;
import java.util.ArrayList;


public class deltaFrame implements PlugIn {

    /* Class Variabels*/
    ImagePlus imp;
    ImageStack imp_stack;
    ImageProcessor ip;
    ImagePlus returnImp = new ImagePlus();
    int default_amplitude = 1;

    /* Methods */
    public void run(String argv) {
        // validate conditions
        this.imp = IJ.getImage();
        imp_stack = this.imp.getStack();
        ip = imp.getProcessor();

        if (imp.getStackSize()==1)
        {IJ.error("Stack required"); return;}

        // run gui and Load images
        GenericDialog gd = new GenericDialog("DeltaSlice settings");
        gd.addNumericField("Gain:",default_amplitude,2);
        this.default_amplitude = (int) gd.getNextNumber();
        gd.showDialog();
        if (gd.wasCanceled()) {
            IJ.error("PlugIn canceled!");
            return;
        }

        // Detrend
        returnImp = imp.duplicate();
        double[] trend = detrend();

        // TODO get baseline frame (frame range 0 trand range)

        // TODO subtract baseline frame from each frame (frame[k]-trend[k]-baseFrame)
        // run local method
        delta_frame( trend ); // fix

    }

    public double[] detrend(){
    /* Applies LPF on the average trace and subtracts from each frame the estimted trend*/
        float[] av_sig = getAverageSignal();
        CalciumSignal ca_sig = new CalciumSignal(av_sig);
        double mean = (double) ca_sig.average(av_sig);
        double[] trend = new double[ca_sig.signalSize()];

        ArrayList<Double> trend_centered = ca_sig.DetrendSignal();
        for (int k = 0; k < ca_sig.signalSize(); k++){
            trend[k] = trend_centered.get(k) + mean;
        }
    return trend;
    }


    private float[] getAverageSignal() {
        double minThreshold = ip.getMinThreshold();
        double maxThreshold = ip.getMaxThreshold();
//        Polygon blobContour = cell.getOuterContour();
//        this.currentROI =  new PolygonRoi(blobContour.xpoints,blobContour.ypoints,blobContour.npoints, Roi.FREELINE) ;
        int size = this.imp.getNSlices();
        float[] values = new float[size];
        Calibration cal = imp.getCalibration();
        Analyzer analyzer = new Analyzer(imp);
        int measurements = Analyzer.getMeasurements();
        for (int i=1; i<=size; i++) {
            ip = imp_stack.getProcessor(i);
            if (minThreshold!=ImageProcessor.NO_THRESHOLD)
                ip.setThreshold(minThreshold,maxThreshold,ImageProcessor.NO_LUT_UPDATE);
//            ip.setRoi(this.currentROI);
            ImageStatistics stats = ImageStatistics.getStatistics(ip, measurements, cal);
//            analyzer.saveResults(stats, this.currentROI);
            values[i-1] = (float) stats.mean;
        }
        return values;
    }

    private void delta_frame( double[] trend ) {
        ImageStack returnStack = returnImp.createEmptyStack();
        imp_stack = this.imp.getStack();
        Object[] imageArray = imp_stack.getImageArray();
        Object[] outputArray = new Object[imp_stack.getSize()];

        //
        outputArray[0] = subtractByte((byte[]) imageArray[0], (byte[]) imageArray[1],this.default_amplitude);

        // initialize first and second frame in the deltaOutput and bar
        IJ.showProgress(0,imp_stack.getSize());
        returnStack.addSlice(String.valueOf(0), outputArray[0]);
        returnStack.addSlice(String.valueOf(1), outputArray[0]);

        // loop for every couple of pics
        for (int k = 1; k < imp_stack.getSize()-1; k++) {
            IJ.showProgress(k,imp_stack.getSize());
            outputArray[k] = subtractByte((byte[]) imageArray[k], (byte[]) imageArray[k+1],this.default_amplitude);
            returnStack.addSlice(String.valueOf(k + 1), outputArray[k]);
        }
        returnImp.setStack(returnStack);
        returnImp.show();
    }

    public static Object subtractByte(byte[] a, byte[] b, int gain){
        byte[] c = new byte[a.length];
        for(int k = 0; k <a.length; k++){
            int a_int = 0xff & a[k];
            int b_int = 0xff & b[k];
            c[k] = (byte) (Math.multiplyExact(Math.abs(a_int-b_int), gain) & 0xff);
            if(c[k] >  (byte) 127){ // > (int) 255
                c[k] = (byte) 127;
            }
        }
        return c;
    }

//    public static void main(final String... args) {
//        deltaFrame df = new deltaFrame();
////        String path = "C:\\Users\\noambox\\Dropbox\\# Graduate studies M.Sc\\# SLITE\\ij - plugin data\\";
//        String path = "C:\\Users\\Noam\\Dropbox\\# graduate studies m.sc\\# SLITE\\ij - plugin data\\";
////        ImagePlus imp = IJ.openImage(path+"FLASH_20msON_10Hz_SLITE_1.tif"); // DEBUG
//        df.imp = IJ.openImage(path + "FLASH_20msON_20Hz_SLITE_1.tif"); // DEBUG
//        String argv = "";
//        df.run(argv);
//    }
}


