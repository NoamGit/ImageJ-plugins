import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;

import static org.junit.Assert.*;

/**
 * Created by noambox on 8/31/2015.
 */
public class CalciumSignalTest {

    @Test
    public void testSetSignal() {
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
            assertEquals(sig.signalSize(), 2139);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testDetrendSignal() {
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
            sig.DetrendSignal();
            sig.showSignal(sig.SignalProcessed);

        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}