package sliteanalysis;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.gui.Plot;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.plugin.*;
import ij.plugin.filter.Analyzer;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;


/**
 * Created by noambox on 10/29/2015.
 */
public class Extract_Stimulus implements PlugIn {

    /* Class Variabels*/
    ImagePlus imp;
    double[] stim_samp;
    TimeSeries stim;
    ImageStack imp_stack;
    private double STD_GAIN = 3;


    /* Methods */
/*    static public Object[] Extract_Stimulus(Roi roi, ImagePlus imp) {
        // TODO
        return new Object[0];
    }*/

    public void run(String argv) {
        // validate conditions
        this.imp = IJ.getImage();
        imp_stack = this.imp.getStack();

        if (imp.getStackSize()==1)
        {IJ.error("Stack required"); return;}

        // run gui
        GenericDialog gd = new GenericDialog("DeltaSlice settings");
        gd.addNumericField("Standard Deviation Gain:",STD_GAIN,3);
        gd.showDialog();
        if (gd.wasCanceled()) {
            IJ.error("PlugIn canceled!");
            return;
        }
        this.STD_GAIN = (int) gd.getNextNumber();


        // run local method
        double[] avr_sig = Activity_Analysis.getAverageSignal(this.imp);
        double[] detr_sig = CalciumSignal.DetrendSignal(avr_sig, 0.001);
        Object[] Peaks = getPeaks(detr_sig, this.STD_GAIN);
        this.stim_samp = CalciumSignal.ArraytoDouble(Peaks);
        stim = getStimulusArray(Peaks, avr_sig.length);
        Plot plot = new Plot("Stimulus","Samples","",stim.time,stim.Signal);
        plot.show();
    }


    public void setStdGain(int gain){
        this.STD_GAIN = gain;
    }

    static public Object[] getPeaks(double[] sig, double std_gain) {
        /* finds signal's stimulus times according to values over std*/

        ArrayList<Double> peaks = new ArrayList<Double>();
        double sig_std = Math.sqrt(CalciumSignal.variance(sig));
        double sig_mean = CalciumSignal.average(sig);
        for(double k = 0; k< sig.length;k++){
            if( sig[(int) k] > (sig_mean + std_gain * sig_std) ){
                peaks.add(k);
                    Double temp = k;
            }
        }
        return peaks.toArray();
    }

    static public TimeSeries getStimulusArray(Object[] peaks, int length){
        double[] stim = new double[length];
        for(int k = 0; k < peaks.length; k++){
            int index = ((Double) peaks[k]).intValue();
            stim[index] = 1;
        }
        TimeSeries ts = new TimeSeries(stim);
        return ts;
    }

    public static void main(final String... args) {

        String testFlag = "num"; // "im" or "numeric"
        Extract_Stimulus es = new Extract_Stimulus();
        String path;
        try {
            path = "C:\\Users\\Noam\\Dropbox\\# graduate studies m.sc\\# SLITE\\ij - plugin data\\"; // LAB
            ImagePlus imp_test = IJ.openImage(path + "FLASH_20msON_10Hz_SLITE_1.tif");
            if(imp_test == null){
                throw new FileNotFoundException("Your not in Lab....");
            }
        }
        catch(FileNotFoundException error){
            path = "C:\\Users\\noambox\\Dropbox\\# Graduate studies M.Sc\\# SLITE\\ij - plugin data\\"; //HOME
        }

        if(testFlag.equals("num") == true){
            File traceSample = new File(path+"trace sample\\ExtractStimTest.xlsx");
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(traceSample);
                // Finds the workbook instance for XLSX file XSSFWorkbook myWorkBook = new XSSFWorkbook (fis);
                XSSFWorkbook myWorkBook = new XSSFWorkbook(fis);
                // Return first sheet from the XLSX workbook
                XSSFSheet mySheet = myWorkBook.getSheetAt(0);

                Iterator rows = mySheet.rowIterator();
                int idx = 0;
                int value_length = 2998;
                double[] values = new double[value_length]; //2139 1103

                while (rows.hasNext() && idx < value_length) {
                    Row row = (XSSFRow) rows.next();
                    values[idx] = row.getCell(1).getNumericCellValue();
                    idx++;
                }

                double[] detr_sig = CalciumSignal.DetrendSignal(values, 0.001);
                TimeSeries stim = Extract_Stimulus.getStimulusArray(Extract_Stimulus.getPeaks(detr_sig, 2), values.length);
                stim.show();
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        else if(testFlag.equals("im") == true){
            es.imp = IJ.openImage(path + "FLASH_20msON_10Hz_SLITE_1.tif"); // DEBUG
            es.imp.show();
            es.run("");
        }
    }
}


