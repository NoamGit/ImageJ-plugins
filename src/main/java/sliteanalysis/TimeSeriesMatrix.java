package sliteanalysis;

import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ErrorMsg;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

/**
 * Signals here are in the rows and the number of rows stands for the
 * number of signals in the same matrix.
 * Created by noambox on 12/8/2015.
 */
public class TimeSeriesMatrix extends TimeSeries {

    /* New Members */
    private RealMatrix sigMat;
    private int numOfSig;
    private int sigLength;

    public TimeSeriesMatrix(int rowDimension, int columnDimension){
        this.sigMat = MatrixUtils.createRealMatrix(rowDimension, columnDimension);
        this.numOfSig = rowDimension;
        this.sigLength = columnDimension;
    }

    public TimeSeriesMatrix(int rowDimension, int columnDimension, double t_dt){
        this.sigMat = MatrixUtils.createRealMatrix(rowDimension, columnDimension);
        this.numOfSig = rowDimension;
        this.sigLength = columnDimension;
        this.dt = t_dt;
    }

    public TimeSeriesMatrix(double[][] y, double dt){
        this.sample = new int[y[0].length];
        this.time = new double[y[0].length];
        this.dt = dt;
        this.numOfSig = y.length;
        this.sigLength = y[0].length;
        this.sigMat = MatrixUtils.createRealMatrix(y.length, y[0].length);
        for(int k=0;k<y[0].length;k++){
                this.sample[k] = k;
                this.time[k] = k * this.dt;
        }
        for (int i = 0; i < y.length; i++) {
            this.sigMat.setRow(i, y[i]);
        }
    }

    public TimeSeriesMatrix(RealMatrix mat, double dt){
        int sigLength = mat.getColumn(1).length;
        this.sample = new int[sigLength];
        this.time = new double[sigLength];
        this.dt = dt;
        for(int k=0;k<sigLength;k++){
            this.sample[k] = k;
            this.time[k] = k * this.dt;
        }
        this.sigMat = mat;
    }

    public void setSignal(int columnNumber, double[] sig){
        this.sigMat.setRow(columnNumber,sig);
    }

    /* adds signal to a new column */
    public void addSignal(double[] sig){
        if(sig.length != this.sigMat.getRowDimension()){
            new ErrorMsg("dimension mismatch in signal addition...");
        }
        double[][] temp = this.sigMat.getData();
        int columDim = this.sigMat.getColumnDimension();
        int rowDim = this.sigMat.getRowDimension();
        this.sigMat.createMatrix(rowDim, columDim + 1);
        this.sigMat.setSubMatrix(temp, 0, 0);
    }

    public void setSignalToEntry(int numSig, int numSampl, double value){
        this.sigMat.addToEntry(numSig, numSampl, value);
    }

    public void getSignalToEntry(int numSig, int numSampl){
        this.sigMat.getEntry(numSig, numSampl);
    }
    public int getNumSignals() {
        return this.numOfSig;
    }

    public double[] getSignal(int i) {
        return this.sigMat.getRow(i);
    }

    public int getLength() {
        return this.sigLength;
    }

    public void show(int sig2show){
        this.Signal = getSignal(sig2show);
        super.show();
    }
}
