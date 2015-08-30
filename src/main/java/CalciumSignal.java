import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;

/**
 * Created by noambox on 8/30/2015.
 */
public class CalciumSignal {

    /* Properties */
    private float[] signalRaw;
    private double activityStatus;
    private float[] SignalProcessed;

    /* Methods */
    private void showSignal() {

    }

    public static void main(String arg[]) {
        File traceSample = new File("C:\\Users\\noambox\\Desktop\\Test Images - ImageJ\\trace sample\\cell2.xlsx");
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(traceSample);

            // Finds the workbook instance for XLSX file XSSFWorkbook myWorkBook = new XSSFWorkbook (fis);
            XSSFWorkbook myWorkBook = new XSSFWorkbook(fis);

            // Return first sheet from the XLSX workbook
            XSSFSheet mySheet = myWorkBook.getSheetAt(0);

            // Get iterator to all the rows in current sheet
            Iterator<Row> rowIterator = mySheet.iterator();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


// Traversing over each row of XLSX file

    }
}