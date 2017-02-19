package sliteanalysis;

import ij.IJ;
import ij.ImagePlus;

import javax.swing.*;
import java.io.FileNotFoundException;

/**
 * This is the implementation of the Segmentation plugin
 * Created by noambox on 19/02/2017.
 */
public class Segmentator {

     /* Class Variabels*/

    /* Methods */
    public void run(String argv) {
        openUI();
    }

    private void openUI() {
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
        UI_Segmentator uis = new UI_Segmentator();
        uis.setVisible(true);
//        System.exit(0);

//        SaveDialog sd = new SaveDialog("Save Signals...", nameData, ".xlsx");
//        String dir = sd.getDirectory();
    }

    public static void main(final String... args) throws FileNotFoundException {
        // open sample image
        String path = System.getProperty("user.dir") + "\\data";
        System.out.print(path + '\n');
        ImagePlus imp = IJ.openImage(path + "\\noam\\ca_av_1.tif"); // DEBUB
        imp.show();

        String argv = "";
        Segmentator seg = new Segmentator();
//        ij.ImageJ.main(new String[] {"temp"});
        seg.run(argv);

    }
}
