package sliteanalysis;

import cellMagicWand.Constants;
import ij.*;
import ij.gui.*;
import ij.measure.Calibration;
import ij.plugin.AVI_Reader;
import ij.plugin.PlugIn;
import ij.plugin.SubstackMaker;
import ij.plugin.filter.Analyzer;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import net.imagej.Main;
import org.apache.commons.math3.filter.KalmanFilter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.List;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

/**
 * Created by noambox on 12/10/2015.
 */
public class AAP_wStimulus implements PlugIn {

    /* Class Variabels*/
    protected ImagePlus imp_r;
    protected ImagePlus imp_s;
    protected int[] imp_s_index;
    protected ArrayList<PolygonRoi> cells_roi = new ArrayList<>();
    protected double dt_r;
    protected double DT_S;

    /* UI parameters */
    protected boolean removeFirst;
    protected Overlay ovr = new Overlay();

    /* Dark noise Parameters*/
    Roi NOISE_ROI;

    /* Kalman Param*/
    protected static double KM_GAIN;
    protected double KL_PRECVAR;
    protected boolean useArtifact;
    public boolean useKalman;
    public boolean replaceArtifact;

    /* stimulus parameter */
    protected double GETPEAKSCONST;
    protected CellManager cm;

    /* Methods */
    @Override
    public void run(String argv) {
        if(this.setup()) { // validates conditions
            // remove first slice
            if(removeFirst){
                ImageStack t_stack = imp_r.getStack(); // deletes first slice
                t_stack.deleteSlice(1);
                imp_r.setStack(t_stack);
            }
            else{
                // if first slice wasn't removed we must move the first index of the stim in binSize
                this.imp_s_index[0] -= Math.round( (float) (this.dt_r/this.DT_S) );
            }

            // get Cells by segmentation;
            IJ.showStatus(" Cell Segmentation...");
            ImagePlus avr_imp = AApSegmetator.getAverageIm(imp_r);
            AApSegmetator segmentator = new AApSegmetator(avr_imp);
            this.cells_roi = segmentator.SegmentCellsCWT();
            //this.cells_roi = segmentator.SegmentCellsML();

            //Resample stimulus
            ArrayList<Double> stim_vector = new ArrayList<>();
            if(!this.useArtifact){
                IJ.showStatus(" Stimulus artifact removal....");
                Stimulus_API sapi = new Stimulus_API();
                sapi.setup("", this.imp_s, this.cells_roi, 0);
                stim_vector = sapi.ResampleStimulus(this.dt_r, imp_r.getStackSize(), this.imp_s_index[0], this.imp_s_index[1]);
            }
            else {
                double[] var1 = Activity_Analysis.getAverageSignal(imp_r);
                double cutoff = Prefs.get("sliteanalysis.cCUTOFF", AAP_Constants.cCUTOFF);
                double[] var1_5 = CalciumSignal.DetrendSignal(var1, cutoff);
                Object[] var2 = Extract_Stimulus.getPeaks(var1_5, GETPEAKSCONST);
                TimeSeries var3 = Extract_Stimulus.getStimulusArray(var2, imp_r.getStackSize());
                for (int i = 0; i < var3.Signal.length; i++) {
                    stim_vector.add(var3.Signal[i]);
                }
            }

            // Remove stimulus slices
            if(this.replaceArtifact) {
                ArrayList<Integer> stim_sampels = new ArrayList<>();
                for (int i = 0; i < stim_vector.size(); i++) {
                    if (stim_vector.get(i) > 0) {
                        stim_sampels.add(i);
                    }
                }
                this.imp_r = ReplaceSlices(this.imp_r, stim_sampels);
            }

            // Dark noise
            if (NOISE_ROI.getBounds().getWidth() != 1 || NOISE_ROI.getBounds().getHeight()!= 1){
                IJ.showStatus(" Dark noise removal...");
                imp_r = DarkNoiseRemoval(imp_r, NOISE_ROI);
            }
            //imp_r.show();

            // Kalman filter
            if(this.useKalman) {
                ImageStack ims = imp_r.getStack();
                Kalman_Stack_Filter kl = new Kalman_Stack_Filter();
                kl.filter(ims, this.KL_PRECVAR, this.KM_GAIN);
                imp_r.setStack(ims);
            }

            CalciumSignal.showSignal(stim_vector);
            this.imp_r.show();
//            IJ.run("In [+]", "");
//            IJ.run("In [+]", "");

            // wrap everything with the Cell manager
            this.cm = toCellManager(this.imp_r, this.cells_roi,avr_imp, stim_vector );
            this.cm.m_lis.listen();
//            this.cm = toCellManager(this.imp_r, this.cells_roi,avr_imp);
        }

        // run local method

    }

    protected void loadPrefs() {
        GETPEAKSCONST  = Prefs.get("sliteanalysis.cGETPEAKSCONST", AAP_Constants.cGETPEAKSCONST);
        double Fs  = Prefs.get("sliteanalysis.cSTIMULUS_FR", AAP_Constants.cSTIMULUS_FR);
        DT_S = 1/Fs;
        removeFirst =Prefs.get("sliteanalysis.cREMOVEFIRST",AAP_Constants.cREMOVEFIRST);
        int nrl_u = (int) Prefs.get("sliteanalysis.cNOISE_ROI_UP",AAP_Constants.cNOISE_ROI_UP);
        int nrl_l = (int) Prefs.get("sliteanalysis.cNOISE_ROI_LOW",AAP_Constants.cNOISE_ROI_LOW);
        NOISE_ROI = new Roi(nrl_u,nrl_u,nrl_l,nrl_l);
        KM_GAIN = Prefs.get("sliteanalysis.cKM_GAIN",AAP_Constants.cKM_GAIN);
        KL_PRECVAR = Prefs.get("sliteanalysis.cKM_PRECVAR",AAP_Constants.cKM_PRECVAR);
        useArtifact = Prefs.get("sliteanalysis.cUSEARTIFACT",AAP_Constants.cUSEARTIFACT);
        useKalman = Prefs.get("sliteanalysis.cUSEKALMAN",AAP_Constants.cUSEKALMAN);
        replaceArtifact = Prefs.get("sliteanalysis.cREPLACEARTIFACT",AAP_Constants.cREPLACEARTIFACT);
    }

    /* Replaces all sample index in imp with neighbor sample*/
    public static ImagePlus ReplaceSlices(ImagePlus imp, ArrayList<Integer> samples) {
        ImageStack ims = imp.getStack();
        Iterator itr = samples.iterator();
        while(itr.hasNext()){
            Integer k = (Integer) itr.next();
            if( k == 0 ){
                continue;
            }
            ImageProcessor ip_slice = ims.getProcessor(k);
//            double value_1 = getMeanOfSlice(ims.getProcessor(k-1));
//            double value_2 = getMeanOfSlice(ims.getProcessor(k));
//            double value_3 = getMeanOfSlice(ims.getProcessor(k+1));
//            double value_4 = getMeanOfSlice(ims.getProcessor(k+2));

            //System.out.print("Replacing "+ value_ori+"with " +value_re );
            ims.addSlice("", ip_slice, k );
            ims.deleteSlice(k +2);
        }
        imp.setStack(ims);
        return imp;
    }

    private static double getMeanOfSlice(ImageProcessor ip_slice) {
        int[][] var1 = ip_slice.getIntArray();
        int mean = 0;
        for (int i = 0; i < var1.length; i++) {
            for (int j = 0; j < var1[i].length; j++) {
                mean += var1[i][j];
            }
        }
        return (double) mean/ip_slice.getPixelCount();
    }

    /* Wraps everything into a Cell Manager Class. stimulus is 1D ArrayList*/
    protected CellManager toCellManager(ImagePlus imp, ArrayList<PolygonRoi> rois, ImagePlus av_imp, ArrayList<Double> stim) {
        CellManager cm = new CellManager(av_imp, imp, this.ovr, stim, this.dt_r );
        Iterator itr = rois.iterator();
        ArrayList<Double> av_sig = new ArrayList<>();
        PolygonRoi roi;
        while(itr.hasNext()){
            roi = (PolygonRoi) itr.next();
            av_sig = Activity_Analysis.getAverageSignal(imp, roi);
            CalciumSignal ca_sig = new CalciumSignal(av_sig, this.dt_r);
            ovr.add(roi);
            ca_sig.DeltaF();
            cm.addCell(ca_sig, roi);
            }
//            catch(Exception e){
//                IJ.showMessage(e.getMessage());
//            }
//        }
        ovr.setLabelColor(Color.WHITE);
//        ovr.setFillColor(Color.GREEN);
//        ovr.setStrokeColor(Color.getHSBColor((float) 0.29710147,(float) 0.4509804,(float) 1));
        ovr.setStrokeColor(Color.getHSBColor((float) 0.1666,(float) 0.651,(float) 1));
        av_imp.setOverlay(ovr);
        av_imp.show();
        IJ.run("In [+]", "");
        IJ.run("In [+]", "");
        return cm;
    }

    /* Wraps everything into a Cell Manager Class*/
    protected CellManager toCellManager(ImagePlus imp, ArrayList<PolygonRoi> rois, ImagePlus av_imp) {
        this.cm = new CellManager(av_imp, imp, this.ovr, this.dt_r );
        Iterator itr = rois.iterator();
        ArrayList<Double> av_sig = new ArrayList<>();
        PolygonRoi roi;
        int index = 1;
        CalciumSignal ca_sig;
        while(itr.hasNext()){

            // remove duplicate regions

            roi = (PolygonRoi) itr.next();
            try{
                av_sig = Activity_Analysis.getAverageSignal(imp, roi);
                ca_sig = new CalciumSignal(av_sig, this.dt_r);
            }
            catch(IllegalArgumentException e){
                System.out.print("\n!WARNING! Problem to extract signal of ROI - "+roi.getName());
                System.out.print("\n!WARNING! with center x - " + roi.getXBase() + " y - " + roi.getYBase());
                continue;
            }
            if(ovr.contains(roi)){
                System.out.print("\n!WARNING! Problem to include ROI - "+roi.getName()+" already included!");
                continue;
            }
            ovr.add(roi);
            ca_sig.DeltaF();
            cm.addCell(ca_sig, roi, index);
            index++;
        }
//            catch(Exception e){
//                IJ.showMessage(e.getMessage());
//            }
//        }
        ovr.setLabelColor(Color.WHITE);
//        ovr.setFillColor(Color.GREEN);
//        ovr.setStrokeColor(Color.getHSBColor((float) 0.29710147,(float) 0.4509804,(float) 1));
        ovr.setStrokeColor(Color.getHSBColor((float) 0.1666,(float) 0.651,(float) 1));

        av_imp.setOverlay(ovr);
        av_imp.show();
        IJ.run("In [+]", "");
//        IJ.run("In [+]", "");
        return cm;
    }

    /* Method takes roi and substract its mean value from each frame in the image*/
    public static ImagePlus DarkNoiseRemoval(ImagePlus imp_in, Roi noise_roi) {
        ImagePlus imp_out = imp_in.duplicate();
        int size = imp_in.getImageStackSize();
        ImageProcessor ip = imp_out.getProcessor();
        double minThreshold = ip.getMinThreshold();
        double maxThreshold = ip.getMaxThreshold();
        Calibration cal = imp_in.getCalibration();
        int measurements = Analyzer.getMeasurements();
        int current = imp_in.getCurrentSlice();
        ImageStack stack = imp_in.getStack();
        for (int i=1; i<=size; i++) {
            ip = stack.getProcessor(i);
            if (minThreshold!=ImageProcessor.NO_THRESHOLD) {
                ip.setThreshold(minThreshold,maxThreshold, ImageProcessor.NO_LUT_UPDATE);
            }
            ip.setRoi(noise_roi);
            ImageStatistics stats = ImageStatistics.getStatistics(ip, measurements, cal);
            ip.resetRoi();
            ip.subtract(stats.mean);
            stack.setProcessor(ip,i);
        }

        imp_out.setStack(stack);
        imp_out.setTitle(imp_in.getTitle());
        return imp_out;
    }

    /* Setup GUI */
    protected boolean setup() {
        if(IJ.versionLessThan("1.40c")) {
            return false;
        } else {
            int[] ids = WindowManager.getIDList();
            if(ids != null && ids.length >= 2) {
                ArrayList titlesList = new ArrayList();
                ArrayList idsList = new ArrayList();
                String currentTitle = null;

                for(int titles = 0; titles < ids.length; ++titles) {
                    ImagePlus var1 = WindowManager.getImage(ids[titles]);
                        titlesList.add(var1.getTitle());
                        idsList.add(Integer.valueOf(ids[titles]));
                        if(var1 == WindowManager.getCurrentImage()) {
                            currentTitle = var1.getTitle();
                    }
                }

                if(titlesList.size() < 2) {
                    IJ.showMessage("You should have at least two stacks - stimulus and response.");
                    return false;
                } else {
                    String[] var8 = new String[titlesList.size()];
                    titlesList.toArray(var8);
                    if(currentTitle == null) {
                        currentTitle = var8[0];
                    }
                    SubstackMaker sm = new SubstackMaker();
                    GenericDialog gd = new GenericDialog("AAP - 1.1.0");
                    gd.setLayout(new BoxLayout(gd, BoxLayout.Y_AXIS)); // check
                    gd.addChoice("Stimulus stack", var8, currentTitle);
                    gd.addChoice("Response stack", var8, currentTitle.equals(var8[0]) ? var8[1] : var8[0]);
                    String defautltSlices = "1-" + WindowManager.getImage(((Integer) idsList.get(0)).intValue()).getStackSize();
                    gd.addMessage("Enter a range (e.g. 2-14)...", null, Color.darkGray);
                    gd.add(Box.createRigidArea(new Dimension(0, 2)));
                    gd.addStringField("Slices of Response:", defautltSlices, 40);
                    gd.add(Box.createRigidArea(new Dimension(0, 10)));
                    JButton settings = new JButton("settings");
                    settings.setBackground(Color.lightGray);
                    settings.setBorderPainted(true);
                    settings.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            UI_Settings.openSettings();
                        }
                    });
                    gd.add(settings);
                    gd.add(Box.createRigidArea(new Dimension(0, 10)));
                    gd.showDialog();
                    if(gd.wasCanceled()) {
                        return false;
                    } else {
                        loadPrefs();
                        this.imp_s = WindowManager.getImage(((Integer) idsList.get(gd.getNextChoiceIndex())).intValue());
                        this.imp_r = WindowManager.getImage(((Integer) idsList.get(gd.getNextChoiceIndex())).intValue());
                        String userInput= gd.getNextString();
                        this.dt_r = Activity_Analysis.findSignalDt(this.imp_r.getTitle());
                        if (userInput==null) {
                            return false;
                        }
                        IJ.showStatus(" Making substack copy of selection...");
                        this.imp_r  = sm.makeSubstack(this.imp_r, userInput);
                        this.imp_s_index = getFirstNLast(this.imp_r, userInput);
                        if(this.imp_s_index[0] != 1)
                            this.removeFirst = false;
                        //this.imp_s = makeStimSubstack(this.imp_s , userInput);
                        return true;
                    }
                }
            } else {
                IJ.showMessage("You should have at least two stacks open.");
                return false;
            }
        }
    }

    /* Make stimulus substack */
    private ImagePlus makeStimSubstack(ImagePlus imp, String userInput) {
        String stackTitle = "Substack ("+userInput+")";
        if (stackTitle.length()>25) {
            int idxA = stackTitle.indexOf(",",18);
            int idxB = stackTitle.lastIndexOf(",");
            if(idxA>=1 && idxB>=1){
                String strA = stackTitle.substring(0,idxA);
                String strB = stackTitle.substring(idxB+1);
                stackTitle = strA + ", ... " + strB;
            }
        }
        ImagePlus imp2 = null;
        try {
            int idx1 = userInput.indexOf("-");
            if (idx1>=1) {									// input displayed in range
                String rngStart = userInput.substring(0, idx1);
                String rngEnd = userInput.substring(idx1+1);
                Integer obj = new Integer(rngStart) ;
                int first = 1 + (obj.intValue()-1) * Math.round( (float) (this.dt_r/this.DT_S) );
                int inc = 1;
                int idx2 = rngEnd.indexOf("-");
                if (idx2>=1) {
                    String rngEndAndInc = rngEnd;
                    rngEnd = rngEndAndInc.substring(0, idx2);
                    String rngInc = rngEndAndInc.substring(idx2+1);
                    obj = new Integer(rngInc);
                    inc = 1 + (obj.intValue()-1) * Math.round( (float) (this.dt_r/this.DT_S) );
                }
                obj = new Integer(rngEnd);
                int last = (obj.intValue()) * Math.round( (float) (this.dt_r/this.DT_S) );
                imp2 = stackRange(imp, first, last, inc, stackTitle);
            } else {
                throw new Exception();
            }
        } catch (Exception e) {
            IJ.error("Substack Maker", "Invalid input string:        \n \n  \""+userInput+"\"");
        }
        return imp2;
    }

    /* Extracts indexes of first and last indexes of stim imp that match response imp*/
    protected int[] getFirstNLast(ImagePlus imp, String userInput){
        String stackTitle = "Substack ("+userInput+")";
        if (stackTitle.length()>25) {
            int idxA = stackTitle.indexOf(",",18);
            int idxB = stackTitle.lastIndexOf(",");
            if(idxA>=1 && idxB>=1){
                String strA = stackTitle.substring(0,idxA);
                String strB = stackTitle.substring(idxB+1);
                stackTitle = strA + ", ... " + strB;
            }
        }
        int[] out = new int[2];
        try {
            int idx1 = userInput.indexOf("-");
            if (idx1>=1) {									// input displayed in range
                String rngStart = userInput.substring(0, idx1);
                String rngEnd = userInput.substring(idx1+1);
                Integer obj = new Integer(rngStart) ;
                int first = 1 + (obj.intValue()-1) * Math.round( (float) (this.dt_r/this.DT_S) );
                int inc = 1;
                int idx2 = rngEnd.indexOf("-");
                if (idx2>=1) {
                    String rngEndAndInc = rngEnd;
                    rngEnd = rngEndAndInc.substring(0, idx2);
                    String rngInc = rngEndAndInc.substring(idx2+1);
                    obj = new Integer(rngInc);
                    inc = 1 + (obj.intValue()-1) * Math.round( (float) (this.dt_r/this.DT_S) );
                }
                obj = new Integer(rngEnd);
                int last = (obj.intValue()) * Math.round( (float) (this.dt_r/this.DT_S) );
                out[0] = first;
                out[1] = last;
            } else {
                throw new Exception();
            }
        } catch (Exception e) {
            IJ.error("Substack Maker", "Invalid input string:        \n \n  \""+userInput+"\"");
        }
        return out;
    }

    // extract range of slices
    ImagePlus stackRange(ImagePlus imp, int first, int last, int inc, String title) throws Exception {
        ImageStack stack = imp.getStack();
        ImageStack stack2 = null;
        Roi roi = imp.getRoi();
        for (int i= first, j=0; i<= last; i+=inc) {
            //IJ.log(first+" "+last+" "+inc+" "+i);
            IJ.showProgress(i-first,last - first);
            int currSlice = i-j;
            ImageProcessor ip2 = stack.getProcessor(currSlice);
            //ip2.setRoi(roi);
            //ip2 = ip2.crop();
            if (stack2==null)
                stack2 = new ImageStack(ip2.getWidth(), ip2.getHeight());
            stack2.addSlice(stack.getSliceLabel(currSlice), ip2);
        }
        ImagePlus substack = imp.createImagePlus();
        substack.setStack(title, stack2);
        substack.setCalibration(imp.getCalibration());
        return substack;
    }

    // Dark noise removal

    public static void main(final String... args) throws FileNotFoundException {
        AAP_wStimulus aap = new AAP_wStimulus();
        String path;
        try {
            path = "D:\\# Projects (Noam)\\# SLITE\\# DATA\\AAP 1.1.0\\"; // LAB
            ImagePlus imp_test = IJ.openImage(path + "TEXT_20msON_10Hz_SLITE_2.tif");
            if(imp_test == null){
                throw new FileNotFoundException("Your not in Lab....");
            }
        }
        catch(FileNotFoundException error){
            path = "C:\\Users\\noambox\\Dropbox\\# Graduate studies M.Sc\\# SLITE\\ij - plugin data\\"; //HOME
        }

        aap.imp_r = IJ.openImage(path + "test_TEXT_10Hz.tif"); // DEBUG
        aap.imp_s = AVI_Reader.open(path + "test_OLED.avi", true);
//        aap.imp_r = IJ.openImage(path + "TEXT_20msON_10Hz_SLITE_2.tif"); // DEBUG
//        aap.imp_s = AVI_Reader.open(path + "OLEDstim_ON0.02_OFF9.98_FlashingText 21.avi", true);
        aap.imp_r.show();
        aap.imp_s.show();
        String argv = "";
        aap.run(argv);
    }
}
