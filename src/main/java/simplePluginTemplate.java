import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import sun.plugin2.main.server.Plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;

/**
 * Created by noambox on ******
 */
public class simplePluginTemplate implements PlugIn {

    /* Class Variabels*/
    ImagePlus imp;
    String type = "numeric";

    /* Methods */
    public void run(String argv) {
        // validate conditions

        // run local method

    }

    public static void main(final String... args) throws FileNotFoundException{
        simplePluginTemplate pt = new simplePluginTemplate();
        String path;
        try {
            path = "C:\\Users\\Noam\\Dropbox\\# graduate studies m.sc\\# SLITE\\ij - plugin data\\"; // LAB
            ImagePlus imp_test = IJ.openImage(path + "FLASH_20msON_20Hz_SLITE_1.tif");
            if(imp_test == null){
                throw new FileNotFoundException("Your not in Lab....");
            }
        }
        catch(FileNotFoundException error){
            path = "C:\\Users\\noambox\\Dropbox\\# Graduate studies M.Sc\\# SLITE\\ij - plugin data\\"; //HOME
        }

        // For Numeric Signal Analysis
        if(pt.type.equals("numeric") == true) {
            File traceSample = new File(path+"trace sample\\Cell8.xlsx");
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

                while (rows.hasNext() && idx < 2139) {
                    Row row = (XSSFRow) rows.next();
                    values[idx] = row.getCell(1).getNumericCellValue();
                    idx++;
                }
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        // For image (stack) analysis
        else if(pt.type.equals("numeric") == false) {
            pt.imp = IJ.openImage(path + "FLASH_20msON_20Hz_SLITE_1.tif"); // DEBUG
        }

        String argv = "";
        pt.run(argv);
    }
}
