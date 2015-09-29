import biz.source_code.dsp.filter.*;
import biz.source_code.dsp.util.ArrayUtils;
import com.sun.org.glassfish.external.statistics.Statistic;
import ij.IJ;
import ij.gui.Plot;
import ij.plugin.frame.Fitter;
import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.descriptive.StatisticalSummaryValues;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import weka.core.AlgVector;
import ij.measure.CurveFitter;
import weka.core.Statistics;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
/**
 * Class for storing processing and detecting neural activity in calcium 1D signals
 * Created by noambox on 8/30/2015.
 */
public class CalciumSignal {

/* Properties */

    protected AlgVector signalRaw;
    protected ArrayList<Double> SignalProcessed = new ArrayList<Double>(); // gives finally the values for DF/F
    private float[] sig2Detrend;
    private float mean;
    private IirFilterCoefficients filterCoefficients;
    private float noisePrecentile; //30% percentile
    private float detectionVariance; // over this threshold there is a good chance for cell's activity
    public boolean isactiveFlag = false;

/* Methods */

    // Default constructer with filter's coeff initialization
    /* Filters details are - Chebychev 1 :: ripple dB = -0.1 :: filterCutoffFreq = frequencyInHz/samplingRateInHz = 0.0007 :: order ~ 5 */
    public CalciumSignal(){
        initilaize();
    }

    public CalciumSignal(float[] initSig){
        this.signalRaw = new AlgVector(FloatToDouble(initSig));
        // subtract mean
        this.mean = average(initSig);
        this.sig2Detrend = initSig;
        for (int i = 0; i < this.sig2Detrend.length; i++){
            this.sig2Detrend[i] = this.sig2Detrend[i] - this.mean;
        }
        initilaize();
    }

    private void initilaize(){
        filterCoefficients = new IirFilterCoefficients();
        filterCoefficients.b = new double[]{2.094852E-14,  1.047426E-13, 2.094852E-13,  2.094852E-13, 1.047426E-13, 2.094852E-14}; // default LPF coefficients
        filterCoefficients.a = new double[]{1.000, -4.9923054980901425, 9.96927569026119, -9.95399387801544, 4.9693826781446955, -0.9923589922996323};
        this.noisePrecentile = (float)0.7;
        detectionVariance = (float)2;
    }

    protected void setSignal(double[] values) {
        // subtract mean
        this.mean = (float) average(values);
        this.signalRaw = new AlgVector(values);
        this.sig2Detrend = new float[this.signalRaw.numElements()];
        for (int i = 0; i < this.signalRaw.numElements(); i++){
            this.sig2Detrend[i] = (float)this.signalRaw.getElement(i) - this.mean;
        }
    }

    // SIMPLE HELPER FUNCTIONS

    private static float[] DoubletoFloat(Double[] array){
        float[] out = new float[array.length];
        int i = 0;
        for (Double f : array) {
            out[i++] = (float)(f != null ? f : Float.NaN);
        }
        return out;
    }

    private static double[] FloatToDouble(float[] array){
        double[] out = new double[array.length];
        int i = 0;
        for (float f : array) {
            out[i++] = (double)(f);
        }
        return out;
    }

    public static double[] getDoubleFromArrayList(ArrayList list) {
        double[] targ = new double[list.size()];
        for (int i = 0; i < list.size(); i++)
            targ[i] = ((Double) list.get(i)).doubleValue();
        return targ;
    }

    public ArrayList<Double> getSignalProcessed(){
        return this.SignalProcessed;
    }

    public static double sum(ArrayList<Double> list) {

        double sum = 0;
        for (int i = 0; i < list.size(); i++){
            sum = sum + list.get(i);
        }

        return sum;
    }

    public static double average(ArrayList<Double> list) {
        double average = sum(list) / list.size();
        return average;
    }

    public static double variance(ArrayList<Double> list) {
        double sumMinusAverage = sum(list) - average(list);
        return sumMinusAverage * sumMinusAverage / (list.size()-1);
    }

    public static float average(float[] array){
        float sum = 0;
        for (int i = 0; i < array.length; i++){
            sum = sum + array[i];
        }
        float average = sum / array.length;
        return average;
    }

    public static double average(double[] array){
        double sum = 0;
        for (int i = 0; i < array.length; i++){
            sum = sum + array[i];
        }
        double average = sum / array.length;
        return average;
    }

    // PLOT METHODS

    static public void showSignal(ArrayList<Double> Signal) {
        double[] x = new double[Signal.size()];
        double values[] = new double[Signal.size()];
        for (int i = 0; i < x.length; i++) {
            x[i] = i;
            values[i] = Signal.get(i);
        }
        Plot plot = new Plot("Plot window","x","values",x,values);
        plot.show();
    }

    static public void showSignal(double[] Signal) {
        double[] x = new double[Signal.length];
        double values[] = new double[Signal.length];
        for (int i = 0; i < x.length; i++) {
            x[i] = i;
            values[i] = Signal[i];
        }
        Plot plot = new Plot("Plot window","x","values",x,values);
        plot.show();
    }

    static public void showSignal(float[] Signal) {
        double[] x = new double[Signal.length];
        double values[] = new double[Signal.length];
        for (int i = 0; i < x.length; i++) {
            x[i] = i;
            values[i] =(double)Signal[i];
        }
        Plot plot = new Plot("Plot window","x","values",x,values);
        plot.show();
    }

    static public void compareSignal(ArrayList<Double> sig1, AlgVector sig2){
        double[] x = new double[sig1.size()];
        double values[] = new double[sig1.size()];
        for (int i = 0; i < x.length; i++) {
            x[i] = i;
            values[i] = sig1.get(i);
        }
        Plot plot = new Plot("Plot window","x","values",x,values);
        plot.setColor(Color.RED);
        plot.draw();
        plot.addPoints(x,sig2.getElements(), Plot.LINE);
        plot.show();
    }

    static public void compareSignal(ArrayList<Double> sig1, ArrayList<Double> sig2, ArrayList<Double> sig2Indx){
        double[] x = new double[sig1.size()];
        double values[] = new double[sig1.size()];
        for (int i = 0; i < x.length; i++) {
            x[i] = i;
            values[i] = sig1.get(i);
        }
        Plot plot = new Plot("Plot window","x","values",x,values);
        double[] sig2Double = getDoubleFromArrayList(sig2);
        plot.setColor(Color.RED);
        plot.draw();
        plot.addPoints(sig2Indx, sig2, Plot.LINE);
        plot.show();
    }

    static public void compareSignal(ArrayList<Double> sig1, ArrayList<Double> sig2){
        double[] x = new double[sig1.size()];
        double values[] = new double[sig1.size()];
        for (int i = 0; i < x.length; i++) {
            x[i] = i;
            values[i] = sig1.get(i);
        }
        Plot plot = new Plot("Plot window","x","values",x,values);
        double[] sig2Double = getDoubleFromArrayList(sig2);
        plot.setColor(Color.RED);
        plot.draw();
        plot.addPoints(x, sig2Double, Plot.LINE);
        plot.show();
    }

    public void showSignal() {
        double[] x = new double[this.signalRaw.numElements()];
        for (int i = 0; i < x.length; i++)
            x[i] = i;
        Plot plot = new Plot("Plot window","x","values",x, this.signalRaw.getElements());
        plot.setColor(Color.BLUE);
        plot.show();
    }

    public void showSignalProccesed() {
        double[] x = new double[this.SignalProcessed.size()];
        double[] y = new double[this.SignalProcessed.size()];
        for (int i = 0; i < x.length; i++) {
            x[i] = i;
            y[i] = this.SignalProcessed.get(i);
        }
        Plot plot = new Plot("Plot window", "x", "values", x, y);
        plot.show();
    }

    // SIGNAL ANALYSIS METHODS

    /* detrend signal by LPF ; Source - http://www.source-code.biz/dsp/java/ */
    public ArrayList<Double> DetrendSignal(){
        int order = 5;
         IirFilterCoefficients filterCoefficients = IirFilterDesignFisher.design(FilterPassType.lowpass, FilterCharacteristicsType.chebyshev, order, -0.1, 0.0008, 1);
        // TODO - find right coefficients
        ArrayList<Double> trend = filtfiltwithLPF(filterCoefficients);
        for (int i = 0; i < this.signalSize(); i++ ) {
            trend.set(i, ( trend.get(i) + this.mean ));
            this.SignalProcessed.add(i, this.signalRaw.getElement(i) - trend.get(i) );
        }

        // TEST
//        showSignal();
//        compareSignal(trend, this.signalRaw);
//        compareSignal(this.SignalProcessed, this.signalRaw);

        return trend;
    }

    /* Estimate baseline by linear regression to perecntile*/
    protected double[] EstimateBaseline() {
        Double val1[] = new Double[this.SignalProcessed.size()];
        val1 = this.SignalProcessed.toArray(val1);
        float[] val3 = DoubletoFloat(val1);
        int numbin = 100;
        edu.mines.jtk.dsp.Histogram hist = new edu.mines.jtk.dsp.Histogram(val3, numbin);

        // create cumulative distribution function
        // TODO - find right threshold
        float[] histcdf = new float[numbin];
        histcdf = hist.getDensities();
        double thresValue = 0;
        for (int i = 1; i < histcdf.length; i++) {
            histcdf[i] = histcdf[i] + histcdf[i - 1];
            if (histcdf[i - 1] <= this.noisePrecentile && this.noisePrecentile <= histcdf[i]) {
                thresValue = hist.getMinValue() + hist.getBinDelta() * i;
                break;
            }
        }

        // find baseline values
        ArrayList<Double> baselineValues = new ArrayList<Double>();
        Double thresValueCast = (Double) thresValue;
        ArrayList<Double> baselineValues_index = new ArrayList<Double>();
        for (int i = 0; i < this.SignalProcessed.size(); i++) {
            if (this.SignalProcessed.get(i) <= thresValueCast) {
                baselineValues.add(this.SignalProcessed.get(i));
                baselineValues_index.add((double) i);
            }
        }

        // TODO - fix show Message of goddness of fit
        CurveFitter fitter = new CurveFitter(getDoubleFromArrayList(baselineValues_index),getDoubleFromArrayList(baselineValues));
        fitter.doFit(CurveFitter.POLY3);

//        if (fitter.getRSquared() < 0.7){
////            IJ.error("");
//            IJ.showMessage("Detrending issue", "Goodness of fit of Linear regression is low = ( " +
//                    String.valueOf(fitter.getRSquared())+" )\n You might consider choosing a different noise percentile in histogram\n" +
//                    "(current noise percentile )" + String.valueOf(this.noisePrecentile));
//        }
        double[] paramLinearRegress = fitter.getParams();
        double[] fullBaseLineY = new double[this.SignalProcessed.size()];
        ArrayList<Double> SignalDetrend = new ArrayList<Double>();
        for (int i = 0; i < fullBaseLineY.length; i++) {
            fullBaseLineY[i] = paramLinearRegress[0] + paramLinearRegress[1]*i + paramLinearRegress[2]*i*i + paramLinearRegress[3]*Math.pow(i,3);
        }

        //         TEST
//        Fitter.plot(fitter);
//        showSignal();
//        compareSignal(SignalDetrend, this.SignalProcessed);
//        AlgVector temp = new AlgVector(fullBaseLineY);
//        compareSignal(SignalDetrend, temp);
//        showSignal();
//        showSignal(this.SignalProcessed);
//        IJ.showMessage("Processed signal variance is " +
//                variance(this.SignalProcessed) + " )\n" );

        return fullBaseLineY;
    }

    /* Compute DF/F*/
    protected void DeltaF(ArrayList<Double> trend,double[] baseLine){
        double f0 = 0;
    for (int i = 0; i < this.signalRaw.numElements(); i++) {
        f0 = trend.get(i)+baseLine[i];
        this.SignalProcessed.set(i, (this.signalRaw.getElement(i) - f0)/f0);
    }
        // Flag activity
        if(variance(this.SignalProcessed) > this.detectionVariance){
            this.isactiveFlag = true;
        }
    }

    /* Compute DF/F*/
    protected void DeltaF(){
        ArrayList<Double> trend = this.DetrendSignal();
        double[] bl = this.EstimateBaseline();
        double f0 = 0;
        for (int i = 0; i < this.signalRaw.numElements(); i++) {
            f0 = trend.get(i)+bl[i];
            this.SignalProcessed.set(i, (this.signalRaw.getElement(i) - f0)/f0);
        }
        // Flag activity
        if(variance(this.SignalProcessed) > this.detectionVariance){
            this.isactiveFlag = true;
        }
    }

    /* filtfilt - Zero phase filtering with specified LPF */
    protected ArrayList<Double> filtfiltwithLPF(IirFilterCoefficients filterCoefficients){
        IirFilter LPF = new IirFilter(filterCoefficients);
        int size = this.sig2Detrend.length;
        double[] forwardfilt = new double[size];
        double[] backfilt = new double[size];
        ArrayList<Double> finalSignal = new ArrayList<Double>(size);
        // forward filtering
        for (int i = 0; i < size; i++ ) {
            forwardfilt[i] = LPF.step(this.sig2Detrend[i]);
        }
        // backward filtering
        for (int i = 0; i < size; i++ ) {
            backfilt[i] = LPF.step(forwardfilt[size-1 - i]);
        }
        // reverse signal
        for (int i = 0; i < size; i++ ) {
            finalSignal.add(backfilt[size-1 - i]);
        }
        // showSignal(finalSignal);
        // compareSignal(finalSignal, this.signalRaw);
        return finalSignal;
    }

    /* return signal size*/
    public int signalSize() {
        return this.signalRaw.numElements();
    }

    /*  Main  */
    public static void main(String arg[]) throws IOException {

        // test class
        String path = "C:\\Users\\Noam\\Dropbox\\# graduate studies m.sc\\# SLITE\\ij - plugin data\\";
        CalciumSignal sig = new CalciumSignal();
        File traceSample = new File(path+"trace sample\\cell8.xlsx");
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(traceSample);
            // Finds the workbook instance for XLSX file XSSFWorkbook myWorkBook = new XSSFWorkbook (fis);
            XSSFWorkbook myWorkBook = new XSSFWorkbook(fis);
            // Return first sheet from the XLSX workbook
            XSSFSheet mySheet = myWorkBook.getSheetAt(0);

            Iterator rows = mySheet.rowIterator();
            int idx = 0;
            double[] values = new double[2139]; //2139 1103

            while (rows.hasNext() && idx < 2139)
            {
                Row row = (XSSFRow) rows.next();
                values[idx] = row.getCell(1).getNumericCellValue();
                idx++;
            }
            sig.setSignal(values);
            ArrayList<Double> trend = sig.DetrendSignal();
//            compareSignal(trend, sig.signalRaw);
            double[] bl = sig.EstimateBaseline();
//            showSignal(bl);
            sig.DeltaF(trend, bl);
            sig.showSignalProccesed();
            IJ.showMessage("activity variance - " + variance(sig.SignalProcessed));
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}

