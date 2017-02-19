package sliteanalysis;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;

import javax.swing.*;
import java.io.FileNotFoundException;

/**
 * Opens the Cell manager without performing any action.
 * Created by noambox on 19/02/2017.
 */
public class OpenCellManager {

    /* Class Variabels*/

    /* Methods */
    public void run(String argv) {
        openCM();
    }

    private void openCM() {
        // Nimbus look and feel
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // If Nimbus is not available, you can set the GUI to another look and feel.
        }

        ImagePlus open_imp = WindowManager.getCurrentImage();
        ImagePlus avg_imp = AApSegmetator.getAverageIm(open_imp);
        avg_imp.show();
        CellManager cm = new CellManager(avg_imp, open_imp,null, 0.1D);
//        System.exit(0);

//        SaveDialog sd = new SaveDialog("Save Signals...", nameData, ".xlsx");
//        String dir = sd.getDirectory();
    }

    public static void main(final String... args) throws FileNotFoundException {
        // open sample image
        String path = System.getProperty("user.dir") + "\\data";
        System.out.print(path + '\n');
        ImagePlus imp = IJ.openImage(path + "\\noam\\ca_mov_1.tif"); // DEBUB
        imp.show();

        String argv = "";
        OpenCellManager op_cm = new OpenCellManager();
        op_cm.run(argv);

    }

}
