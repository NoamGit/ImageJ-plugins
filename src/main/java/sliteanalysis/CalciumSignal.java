package sliteanalysis;

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
    private double dt;
    protected ArrayList<Double> SignalProcessed = new ArrayList<Double>(); // gives finally the values for DF/F
    private float[] sig2Detrend;
    private float mean;
    private double CUTOFF = 0.002;
    private IirFilterCoefficients filterCoefficients;
    private float noisePrecentile; //30% percentile
    private float detectionVariance; // over this threshold there is a good chance for cell's activity
    public double activityVariance = 0;
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

    public CalciumSignal(float[] initSig, double dt){
        initilaize();
        this.dt = dt;
        this.signalRaw = new AlgVector(FloatToDouble(initSig));
        // subtract mean
        this.mean = average(initSig);
        this.sig2Detrend = initSig;
//        for (int i = 0; i < this.sig2Detrend.length; i++){
//            this.sig2Detrend[i] = this.sig2Detrend[i] - this.mean;
//        }
    }

    private void initilaize(){
        filterCoefficients = new IirFilterCoefficients();
        filterCoefficients.b = new double[]{2.094852E-14,  1.047426E-13, 2.094852E-13,  2.094852E-13, 1.047426E-13, 2.094852E-14}; // default LPF coefficients
        filterCoefficients.a = new double[]{1.000, -4.9923054980901425, 9.96927569026119, -9.95399387801544, 4.9693826781446955, -0.9923589922996323};
        this.CUTOFF = 0.001;
        this.noisePrecentile = (float)0.3;
        detectionVariance = (float)2;
        this.dt = 0.1;
    }

    public void setSignal(double[] values) {
        // subtract mean
        this.mean = (float) average(values);
        this.signalRaw = new AlgVector(values);
        this.sig2Detrend = new float[this.signalRaw.numElements()];
        for (int i = 0; i < this.signalRaw.numElements(); i++){
//            this.sig2Detrend[i] = (float)this.signalRaw.getElement(i) - this.mean;
            this.sig2Detrend[i] = (float)this.signalRaw.getElement(i);

        }
    }

    // SIMPLE HELPER FUNCTIONS

    public static double[] ArraytoDouble(Object[] array){
        double[] out = new double[array.length];
        int i = 0;
        for (Object f : array) {
            out[i++] = (Double)(f != null ? f : 0);
        }
        return out;
    }

    private static float[] DoubletoFloat(Double[] array){
        float[] out = new float[array.length];
        int i = 0;
        for (Double f : array) {
            out[i++] = (float)(f != null ? f : Float.NaN);
        }
        return out;
    }

    private float[] DoubletoFloat(double[] array) {
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

    public double getdt(){
        return this.dt;
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

    public static double sum(double[] array) {
        double sum = 0;
        for (int i = 0; i < array.length; i++){
            sum += array[i];
        }
        return sum;
    }

    public static double average(ArrayList<Double> list) {
        double average = sum(list) / list.size();
        return average;
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

    public static double variance(ArrayList<Double> list) {
        ArrayList<Double> meanVect = new ArrayList<Double>();
        double var = 0;
        double mean = average(list);
        for(int k=0;k<list.size();k++){
            var += Math.pow(list.get(k) - mean, 2);
        }
        return (var / (list.size()-1));
    }

    public static double variance(double[] array) {
        double var = 0;
        double mean = average(array);
        for(int k=0;k<array.length;k++){
            var += Math.pow(array[k] - mean, 2);
        }
        return (var / (array.length-1));
    }

    // PLOT METHODS

    static public void showSignal(ArrayList<Double> Signal) {
        double[] x = new double[Signal.size()];
        double values[] = new double[Signal.size()];
        for (int i = 0; i < x.length; i++) {
            x[i] = i;
            values[i] = Signal.get(i);
        }
        Plot plot = new Plot("Plot window","time","fluoe",x,values);
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

    static public void compareSignal(AlgVector sig2, ArrayList<Double> sig1){
        double[] x = new double[sig1.size()];
        double values[] = new double[sig1.size()];
        for (int i = 0; i < x.length; i++) {
            x[i] = i;
            values[i] = sig1.get(i);
        }
        Plot plot = new Plot("two Sig Comparison","time","fluor");
        plot.setColor(Color.RED);
        plot = new Plot("Plot window","x","values",x,sig2.getElements());
        plot.setColor(Color.BLUE);
        plot.draw();
        plot.addPoints(x,values, Plot.LINE);
        plot.show();
    }

    static public void compareSignal(AlgVector sig2, double[] sig1){
        double[] x = new double[sig1.length];
        for (int i = 0; i < x.length; i++) {
            x[i] = i;
        }
        Plot plot = new Plot("two Sig Comparison","time","fluor");
        plot.setColor(Color.RED);
        plot = new Plot("Plot window","x","values",x,sig2.getElements());
        plot.setColor(Color.BLUE);
        plot.draw();
        plot.addPoints(x,sig1, Plot.LINE);
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

    static public void compareSignal(ArrayList<Double> sigList, float[] sigFloat) {
        double[] x = new double[sigList.size()];
        double values[] = new double[sigList.size()];
        for (int i = 0; i < x.length; i++) {
            x[i] = i;
            values[i] = sigList.get(i);
        }
        Plot plot = new Plot("Plot window","x","values",x,values);
        plot.setColor(Color.RED);
        plot.draw();
        plot.addPoints(x, FloatToDouble(sigFloat), Plot.LINE);
        plot.show();
    }

    static public void compareSignal(double[] sigDouble, float[] sigFloat) {
        double[] x = new double[sigDouble.length];
        double values[] = new double[sigDouble.length];
        for (int i = 0; i < x.length; i++) {
            x[i] = i;
        }
        Plot plot = new Plot("Plot window","x","values",x,FloatToDouble(sigFloat));
        plot.setColor(Color.RED);
        plot.draw();
        plot.addPoints(x, sigDouble, Plot.LINE);
        plot.show();
    }

    public void showSignal() {
        double[] x = new double[this.signalRaw.numElements()];
        for (int i = 0; i < x.length; i++)
            x[i] = i;
        Plot plot = new Plot("Signal Raw","x","values",x, this.signalRaw.getElements());
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
        Plot plot = new Plot("Signal Processed", "x", "values", x, y);
        plot.show();
    }
    public void showSignalProccesed(String title) {
        double[] x = new double[this.SignalProcessed.size()];
        double[] y = new double[this.SignalProcessed.size()];
        for (int i = 0; i < x.length; i++) {
            x[i] = i * this.dt;
            y[i] = this.SignalProcessed.get(i);
        }
        Plot plot = new Plot(title, "time", "Fluo", x, y);
        plot.setColor(Color.DARK_GRAY);
        plot.show();
    }

    public void showSignal(String title) {
        double[] x = new double[this.signalRaw.numElements()];
        for (int i = 0; i < x.length; i++)
            x[i] = i * this.dt;
        Plot plot = new Plot(title,"x","values",x, this.signalRaw.getElements());
        plot.setColor(Color.BLUE);
        plot.show();
    }

    // SIGNAL ANALYSIS METHODS

    /* detrend signal by LPF ; Source - http://www.source-code.biz/dsp/java/ */
    public ArrayList<Double> DetrendSignal(){
        int order = 5;
        IirFilterCoefficients filterCoefficients = IirFilterDesignFisher.design(FilterPassType.lowpass, FilterCharacteristicsType.chebyshev, order, -0.1, this.CUTOFF, 1);
        ArrayList<Double> trend = filtfiltwithLPF(filterCoefficients);
        for (int i = 0; i < this.signalSize(); i++ ) {
            trend.set(i, (trend.get(i)));
//            this.SignalProcessed.add(i, this.sig2Detrend[i] - trend.get(i) );
            this.SignalProcessed.add(i, (double) this.sig2Detrend[i]);
            this.sig2Detrend[i] = (float) ((double) this.sig2Detrend[i] - trend.get(i));
        }

        // TEST
//        compareSignal(trend, this.sig2Detrend);
//        showSignal();
//        compareSignal(this.SignalProcessed, this.signalRaw);

        return trend;
    }


    private void detrendExp() {
    /* Detrends Raw signal by Exponential fitting with offset*/
        double[] x = new double[this.signalRaw.numElements()];
        for (int i = 0; i < this.signalRaw.numElements(); i++) {
            x[i] = (double) i;
        }
        CurveFitter fitter = new CurveFitter(x, FloatToDouble(this.sig2Detrend));
        fitter.doFit(CurveFitter.EXP_WITH_OFFSET);
        double[] paramExp = fitter.getParams();
        double[] expTrend = new double[this.sig2Detrend.length];
        for (int i = 0; i < expTrend.length; i++) {
            expTrend[i] = paramExp[0] * Math.exp(i * -paramExp[1]) + paramExp[2];
            this.sig2Detrend[i] = this.sig2Detrend[i] - (float) expTrend[i];
        }

//        compareSignal(this.signalRaw, expTrend);
    }

    /* Estimate baseline by linear regression to perecntile*/
    protected double[] EstimateBaseline() {
//        Double val1[] = new Double[this.SignalProcessed.size()];
//        val1 = this.SignalProcessed.toArray(val1);
//        float[] val3 = DoubletoFloat(val1);

//        float val1[] = new float[this.sig2Detrend.length];
        float[] val3 = this.sig2Detrend;
        int numbin = 100;
        edu.mines.jtk.dsp.Histogram hist = new edu.mines.jtk.dsp.Histogram(val3, numbin);

        // create cumulative distribution function
        // TODO - find right threshold
        float[] histcdf;
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
        Double thresValueCast = thresValue;
        ArrayList<Double> baselineValues_index = new ArrayList<Double>();
        for (int i = 0; i < this.sig2Detrend.length; i++) {
            if (this.sig2Detrend[i] <= thresValueCast) {
                baselineValues.add((double) this.sig2Detrend[i]);
                baselineValues_index.add((double) i);
            }
        }

        // TODO - fix show Message of goddness of fit
        CurveFitter fitter = new CurveFitter(getDoubleFromArrayList(baselineValues_index),getDoubleFromArrayList(baselineValues));
        fitter.doFit(CurveFitter.STRAIGHT_LINE);
//        fitter.doFit(CurveFitter.POLY3);

//        if (fitter.getRSquared() < 0.7){
////            IJ.error("");
//            IJ.showMessage("Detrending issue", "Goodness of fit of Linear regression is low = ( " +
//                    String.valueOf(fitter.getRSquared())+" )\n You might consider choosing a different noise percentile in histogram\n" +
//                    "(current noise percentile )" + String.valueOf(this.noisePrecentile));
//        }
        double[] paramLinearRegress = fitter.getParams();
        double[] fullBaseLineY = new double[this.sig2Detrend.length];
        for (int i = 0; i < fullBaseLineY.length; i++) {
            fullBaseLineY[i] = paramLinearRegress[0] + paramLinearRegress[1]*i ;
//            fullBaseLineY[i] = paramLinearRegress[0] + paramLinearRegress[1]*i + paramLinearRegress[2]*i*i + paramLinearRegress[3]*Math.pow(i,3);
        }

        //         TEST
//        Fitter.plot(fitter);
//        showSignal();
//        AlgVector temp = new AlgVector(fullBaseLineY);
//        showSignal();
//        compareSignal(this.SignalProcessed, DoubletoFloat(fullBaseLineY));
//        IJ.showMessage("Processed signal variance is " +
//                variance(this.SignalProcessed) + " )\n" );

        return fullBaseLineY;
    }


    /* Compute DF/F*/
    protected void DeltaF(ArrayList<Double> trend,double[] baseLine){
        double f0 = 0;
    for (int i = 0; i < this.SignalProcessed.size(); i++) {
        f0 = trend.get(i)+baseLine[i];
        this.SignalProcessed.set(i, (this.SignalProcessed.get(i) - f0)/Math.abs(baseLine[i]));
    }
        // Flag activity
        if(variance(this.SignalProcessed) > this.detectionVariance){
            this.isactiveFlag = true;
        }
    }

    /* Compute DF/F*/
    protected void DeltaF(){
        detrendExp();
        ArrayList<Double> trend = this.DetrendSignal();
        double[] bl = this.EstimateBaseline();
        double f0 = 0;
        for (int i = 0; i < this.SignalProcessed.size(); i++) {
            f0 = trend.get(i)+bl[i];
            this.SignalProcessed.set(i, (this.SignalProcessed.get(i) - f0)/Math.abs(bl[i]));
        }

        // Flag activity
        this.activityVariance = variance(this.SignalProcessed);
        if( this.activityVariance > this.detectionVariance){
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
        return finalSignal;
    }

    /* return signal size*/
    public int signalSize() {
        return this.signalRaw.numElements();
    }

    /*  Main  */
    public static void main(String arg[]) throws IOException {

        // test class
        String path = "C:\\Users\\noambox\\Dropbox\\# Graduate studies M.Sc\\# SLITE\\ij - plugin data\\";
//        String path = "C:\\Users\\Noam\\Dropbox\\# graduate studies m.sc\\# SLITE\\ij - plugin data\\";
        File traceSample = new File(path+"trace sample\\test_detrending_src.xlsx");
//        File traceSample = new File(path+"trace sample\\Cell8.xlsx");

        CalciumSignal sig = new CalciumSignal();
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

            showSignal(sig.sig2Detrend);

            // detrend according to exponential fit (works and manipulates on sig2Detrend)
            sig.detrendExp();
            showSignal(sig.sig2Detrend);

            // removes high freq with LPF (works and manipulates on sig2Detrend and sets SignalProccessed )
            ArrayList<Double> trend = sig.DetrendSignal();
            compareSignal(sig.SignalProcessed, trend);

            // Estimates baseline (works and manipulates on sig2Detrend)
            double[] bl = sig.EstimateBaseline();
//            sig.showSignalProccesed();
            compareSignal(bl, sig.sig2Detrend);

            // Computes DF  (works and manipulates on SignalProccessed)
            sig.DeltaF(trend, bl);
            sig.showSignalProccesed();

//            IJ.showMessage("activity variance - " + variance(sig.SignalProcessed));
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}

