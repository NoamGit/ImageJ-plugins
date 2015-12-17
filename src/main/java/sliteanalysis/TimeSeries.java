package sliteanalysis;

import ij.gui.Plot;

/**
 * Created by noambox on 10/29/2015.
 */
public class TimeSeries {

    /* Vars */
    public int[] sample;
    public double[] Signal;
    public double[] time;
    public double dt;

    /* Methods */
    public TimeSeries(){

    }

    public TimeSeries(int[] x, double[] y){
        this.sample = x;
        this.Signal = y;
    }

    public TimeSeries(double[] time, double[] y){
        this.sample = new int[y.length];
        this.time = new double[y.length];
        this.dt = time[2] - time[1];
        this.time = time;
        for(int k=0;k<y.length;k++){
            this.sample[k] = k;
        }
        this.Signal = y;
    }

    public TimeSeries(double[] y){
        this.sample = new int[y.length];
        this.time = new double[y.length];
        for(int k=0;k<y.length;k++){
            this.sample[k] = k;
            this.time[k] = k;
        }
        this.Signal = y;
    }

    /* Plot Methods */
    public void show() {
        Plot plot = new Plot("Plot window","x","y",this.time,this.Signal);
        plot.show();
    }

    static public void show(double[] samples, double[] Signal) {
        Plot plot = new Plot("Plot window","time","",samples,Signal);
        plot.show();
    }
}
