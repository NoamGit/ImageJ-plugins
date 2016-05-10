package sliteanalysis;

import ij.IJ;
import ij.plugin.PlugIn;

import javax.swing.*;
import java.io.FileNotFoundException;
import javax.swing.UIManager.*;

/**
 * Created by noambox on 5/4/2016.
 */

public class Extract_Data implements PlugIn {

    /* Class Variabels*/

    /* Methods */
    public void run(String argv) {
        // choose input directory and output directory
        openUI();
    }

    private void openUI() {
        // Nimbus look and feel
        try {
            for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // If Nimbus is not available, you can set the GUI to another look and feel.
        }
        Dialog_Extract_Data ed_dialog = new Dialog_Extract_Data();
        ed_dialog.pack();
        ed_dialog.setVisible(true);
//        System.exit(0);

//        SaveDialog sd = new SaveDialog("Save Signals...", nameData, ".xlsx");
//        String dir = sd.getDirectory();
    }

    public static void main(final String... args) throws FileNotFoundException {
        Extract_Data pt = new Extract_Data();

        String argv = "";
//        ij.ImageJ.main(new String[] {"temp"});
        pt.run(argv);
    }
}
