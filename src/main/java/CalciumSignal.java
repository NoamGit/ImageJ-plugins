import biz.source_code.dsp.filter.*;
import ij.gui.Plot;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import weka.core.AlgVector;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
/**
 * Created by noambox on 8/30/2015.
 */
public class CalciumSignal {

    /* Properties */
    protected AlgVector signalRaw;
    protected double activityStatus = 0;
    protected ArrayList<Double> SignalProcessed = new ArrayList<Double>();
    private IirFilterCoefficients filterCoefficients;

    /* Methods */

    // Default constructer with filter's coeff initialization
    /* Filters details are - Chebychev 1 :: Fs = 100Hz :: Fpass = 0.2 Hz :: Fstop = 0.3 Hz :: minimum order */
    // Designed with Matlab
    public CalciumSignal(){
        filterCoefficients = new IirFilterCoefficients();
        filterCoefficients.b = new double[]{0.028,  0.053, 0.071,  0.053, 0.028}; // default LPF coefficients
        filterCoefficients.a = new double[]{1.000, -2.026, 2.148, -1.159, 0.279};
    }

    // Plot signal methods
    public void showSignal(ArrayList<Double> Signal) {
        double[] x = new double[Signal.size()];
        double values[] = new double[Signal.size()];
        for (int i = 0; i < x.length; i++) {
            x[i] = i;
            values[i] = Signal.get(i);
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
        plot.addPoints(x, sig2.getElements(), Plot.LINE);
        plot.show();
    }

    public void showSignal() {
        double[] x = new double[this.signalRaw.numElements()];
        for (int i = 0; i < x.length; i++)
            x[i] = i;
        Plot plot = new Plot("Plot window","x","values",x, this.signalRaw.getElements());
        plot.show();
    }

    // detrend signal by LPF and linear fitting to the 5% precentile
    // Source - http://www.source-code.biz/dsp/java/
    // TODO - fix the filtering issue
     public void DetrendSignal(){
         double frequencyInHz = 0.2;
         double samplingRateInHz = 31.25;
         double filterCutoffFreq = frequencyInHz/samplingRateInHz;
         IirFilterCoefficients filterCoefficients = IirFilterDesignFisher.design(FilterPassType.lowpass, FilterCharacteristicsType.chebyshev, 10, -50, 0.018, 1);
         IirFilter LPF = new IirFilter(filterCoefficients);
         //IirFilter LPF = new IirFilter(this.filterCoefficients);
         ArrayList<Double> temp = new ArrayList<Double>();
         for (int i = 0; i < this.signalSize(); i++ ) {
             this.SignalProcessed.add(i,this.signalRaw.getElement(i) - LPF.step(this.signalRaw.getElement(i)));
             temp.add(LPF.step(this.signalRaw.getElement(i)));
         }
         compareSignal(temp, this.signalRaw);
     }

    protected void setSignal(double[] values) {
        this.signalRaw = new AlgVector(values.length);
        this.signalRaw.setElements(values);
    }

    public int signalSize() {
        return this.signalRaw.numElements();
    }

    /*  Main  */
    public static void main(String arg[]) throws IOException {

            // test class
            CalciumSignal sig = new CalciumSignal();
            File traceSample = new File("C:\\Users\\noambox\\Desktop\\Test Images - ImageJ\\trace sample\\cell2.xlsx");
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(traceSample);
                // Finds the workbook instance for XLSX file XSSFWorkbook myWorkBook = new XSSFWorkbook (fis);
                XSSFWorkbook myWorkBook = new XSSFWorkbook(fis);
                // Return first sheet from the XLSX workbook
                XSSFSheet mySheet = myWorkBook.getSheetAt(0);

                Iterator rows = mySheet.rowIterator();
                int idx = 0;
                double[] values = new double[2139];

                while (rows.hasNext() && idx < 2139)
                {
                    Row row = (XSSFRow) rows.next();
                    values[idx] = row.getCell(1).getNumericCellValue();
                    idx++;
                }
                sig.setSignal(values);
                //sig.showSignal();
                sig.DetrendSignal();
                //sig.showSignal(sig.SignalProcessed);
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }


    }
}