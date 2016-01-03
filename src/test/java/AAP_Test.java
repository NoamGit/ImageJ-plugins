import com.sun.org.apache.xpath.internal.operations.Bool;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.GenericDialog;
import net.imagej.Main;
import org.scijava.ui.UIService;
import ij.gui.PolygonRoi;
import ij.plugin.frame.RoiManager;
import sliteanalysis.AAP_Constants;
import sliteanalysis.AApSegmetator;
import org.scijava.ui.UIService;

import javax.swing.*;
import javax.swing.JTabbedPane;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.util.ArrayList;

/**
 * Created by noambox on 12/20/2015.
 */
public class AAP_Test {

//    @org.junit.Test
//     public void toolSelectionTest(){
//        String path = "C:\\Users\\noambox\\Desktop\\AAP plugin\\ClassifierTrain\\151206TRAIN\\";
//        ImagePlus imp = IJ.openImage(path + "av1rotate.tif"); // DEBUG
//        imp.show();
//        IJ.run("In [+]", "");
//        IJ.run("In [+]", "");
//    }

    @org.junit.Test
    public void uiTest() {
        GenericDialog gd = new GenericDialog("AAP - 1.1.0");
        String path = "C:\\Users\\noambox\\Desktop\\AAP plugin\\ClassifierTrain\\151206TRAIN\\";
        ImagePlus imp = IJ.openImage(path + "av1rotate.tif"); // DEBUG
        // gd.setInsets(10, 45, 0);
        ArrayList<String> idsList = new ArrayList<>();
        idsList.add(imp.getTitle());

       /* PopupMenu settings = new PopupMenu();
        settings;
        gd();*/

        gd.addChoice("Stimulus stack", new String[]{imp.getTitle()}, imp.getTitle());
        gd.addMessage("Enter a range (e.g. 2-14)...", null, Color.darkGray);
        gd.addStringField("Slices of Response:", "1-400", 40);
        gd.addCheckbox("Remove first slice", true);
        gd.addCheckbox("Use Artifact from response", true);
        JButton settings = new JButton("settings");
        settings.setBackground(Color.lightGray);
        settings.setBorderPainted(true);
        settings.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openSettings();
            }
        });
        gd.add(settings);
        gd.showDialog();
        if (gd.wasCanceled()) {
            return;
        } else {
            ImagePlus out = WindowManager.getImage(0);
        }
    }

    private void openSettings() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                int out = 0;
                JFrame frame = new JFrame("Settings");
                GenericDialog gd = new GenericDialog("settings",frame);
                gd.setLayout(new BoxLayout(gd, BoxLayout.PAGE_AXIS));

                // UI components
                JComponent panel1 = makeStackPanel("Stack Settings");
                JComponent panel2 = makeSegPanel("Cell Detection");
                JComponent panel3 = makeProcessingPanel("Calcium Signal Processing");

                JTabbedPane tabbedPane = new JTabbedPane();
                tabbedPane.addTab("Stack", panel1);
                tabbedPane.addTab("Segmentation", panel2);
                tabbedPane.addTab("Processing", panel3);
                gd.add(tabbedPane);

                gd.pack();
                gd.showDialog();
                ArrayList<String> text_value = readStringsFromPanel(panel1);
                text_value.addAll(readStringsFromPanel(panel2));
                text_value.addAll(readStringsFromPanel(panel3));

                ArrayList<Boolean> chk_value = readCheckBoxFromPanel(panel1);
                chk_value.addAll(readCheckBoxFromPanel(panel2));
                chk_value.addAll(readCheckBoxFromPanel(panel3));

                savePrefs(text_value, chk_value);
            }
        });

    }

    private void savePrefs(ArrayList<String> text_value, ArrayList<Boolean> chk_value) {
        Prefs.set("sliteAnalysis.cSTIMULUS_FR", text_value.get(0));
        Prefs.set("sliteAnalysis.cKM_GAIN", text_value.get(1));
        Prefs.set("sliteAnalysis.cKM_PRECVAR", text_value.get(2));
        Prefs.set("sliteAnalysis.cNOISE_ROI_UP", text_value.get(3));
        Prefs.set("sliteAnalysis.cNOISE_ROI_LOW", text_value.get(4));

        Prefs.set("sliteAnalysis.cFM_TOL", text_value.get(5));
        Prefs.set("sliteAnalysis.cCM_MAX", text_value.get(6));
        Prefs.set("sliteAnalysis.cENLARGEROI", text_value.get(7));
        Prefs.set("sliteAnalysis.cELLIPSE_a", text_value.get(8));
        Prefs.set("sliteAnalysis.cELLIPSE_b", text_value.get(9));

        Prefs.set("sliteAnalysis.cCUTOFF", text_value.get(10));
        Prefs.set("sliteAnalysis.cORDER", text_value.get(11));
        Prefs.set("sliteAnalysis.cNOISEPERCENTILE", text_value.get(12));
        Prefs.set("sliteAnalysis.cDETECTIONVARIANCE", text_value.get(13));

        Prefs.set("sliteAnalysis.cREMOVEFIRST", chk_value.get(0));
        Prefs.set("sliteAnalysis.cUSEARTIFACT", chk_value.get(1));
        Prefs.savePreferences();
    }

    private ArrayList<String> readStringsFromPanel(JComponent comp) {
        Component[] p_comp = comp.getComponents();
        ArrayList<String> fieldValues = new ArrayList<>();
        for (int i = 0; i < p_comp.length; i++) {
            if( p_comp[i] instanceof JLabel){
                continue;
            }
            Component[] sub_components =  ((JPanel) p_comp[i]).getComponents();
            for (int j = 0; j < sub_components.length; j++) {
                if(sub_components[j] instanceof JTextField)
                    fieldValues.add(((JTextField)sub_components[j]).getText());
            }
        }
        return fieldValues;
    }

    private ArrayList<Boolean> readCheckBoxFromPanel(JComponent comp) {
        Component[] p_comp = comp.getComponents();
        ArrayList<Boolean> fieldValues = new ArrayList<>();
        for (int i = 0; i < p_comp.length; i++) {
            if( p_comp[i] instanceof JLabel){
                continue;
            }
            Component[] sub_componnts =  ((JPanel) p_comp[i]).getComponents();
            for (int j = 0; j < sub_componnts.length; j++) {
                if(sub_componnts[j] instanceof JCheckBox)
                    fieldValues.add(((JCheckBox) sub_componnts[j]).isSelected());
            }
        }
        return fieldValues;
    }

    // See  http://da2i.univ-lille1.fr/doc/tutorial-java/uiswing/components/examples/TextInputDemo.java
    protected JComponent makeStackPanel(String text) {

        JPanel panel = new JPanel();
        BoxLayout box = new BoxLayout(panel, BoxLayout.PAGE_AXIS);
        panel.setLayout(box);

        JPanel entry1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        entry1.add(new JLabel("Stimulus frame rate (Hz):"));
        entry1.add(new JTextField(Double.toString(1/AAP_Constants.cSTIMULUS_FR), 4));
        panel.add(entry1);

        JPanel entry2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        entry2.add(new JLabel("Kalman Gain:"));
        entry2.add(new JTextField(Double.toString(AAP_Constants.cKM_GAIN), 4));
        panel.add(entry2);

        JPanel entry3 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        entry3.add(new JLabel("Kalman var:"));
        entry3.add(new JTextField(Double.toString(AAP_Constants.cKM_PRECVAR), 4));
        panel.add(entry3);

        JPanel entry4 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        entry4.add(new JLabel("Remove slice(0):"));
        entry4.add(new JCheckBox("",AAP_Constants.cREMOVEFIRST));
        panel.add(entry4);

        JPanel entry5 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        entry5.add(new JLabel("Noise ROI bounderies:"));
        entry5.add(new JTextField(Double.toString(AAP_Constants.cNOISE_ROI_UP), 4));
        entry5.add(new JTextField(Double.toString(AAP_Constants.cNOISE_ROI_LOW), 4));
        panel.add(entry5);

        JPanel entry6 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        entry6.add(new JLabel("Use Artifact:"));
        entry6.add(new JCheckBox("",AAP_Constants.cUSEARTIFACT));
        panel.add(entry6);

        return panel;
    }

    protected JComponent makeSegPanel(String text) {
        JPanel panel = new JPanel();
        BoxLayout box = new BoxLayout(panel, BoxLayout.PAGE_AXIS);
        panel.setLayout(box);

        JPanel entry1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        entry1.add(new JLabel("Find Maxima Tolerance:"));
        entry1.add(new JTextField(Double.toString(AAP_Constants.cFM_TOL), 4));
        panel.add(entry1);

        JPanel entry2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        entry2.add(new JLabel("Cell's Max Diameter:"));
        entry2.add(new JTextField(Double.toString(AAP_Constants.cCM_MAX), 4));
        panel.add(entry2);

        JPanel entry3 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        entry3.add(new JLabel("Enlarge factor:"));
        entry3.add(new JTextField(Double.toString(AAP_Constants.cENLARGEROI), 4));
        panel.add(entry3);

        JPanel entry4 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        entry4.add(new JLabel("Ellipse Center B:"));
        entry4.add(new JTextField(Double.toString(AAP_Constants.cELLIPSE_a), 4));
        panel.add(entry4);

        JPanel entry5 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        entry5.add(new JLabel("Ellipse Center B:"));
        entry5.add(new JTextField(Double.toString(AAP_Constants.cELLIPSE_b), 4));
        panel.add(entry5);

        return panel;
    }

    protected JComponent makeProcessingPanel(String text) {
        JPanel panel = new JPanel();
        BoxLayout box = new BoxLayout(panel, BoxLayout.PAGE_AXIS);
        panel.setLayout(box);
        JLabel details2 = new JLabel("Chebychev LPF filter for trend estimation",JLabel.LEFT);
        JLabel details1 = new JLabel("Settings of the DF/F processing uses",JLabel.LEFT);
        details1.setFont(Font.getFont(Font.SANS_SERIF));
        details2.setFont(Font.getFont(Font.SANS_SERIF));
        panel.add(details1);
        panel.add(details2);
        panel.add(new JLabel("", JLabel.CENTER));

        JPanel entry1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        entry1.add(new JLabel("Cut off Frequency (Hz):"));
        entry1.add(new JTextField(Double.toString(AAP_Constants.cCUTOFF), 4));
        panel.add(entry1);

        JPanel entry2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        entry2.add(new JLabel("Filter's order:"));
        entry2.add(new JTextField(Double.toString(AAP_Constants.cORDER),4));
        panel.add(entry2);

        JPanel entry3 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        entry3.add(new JLabel("BaseLine Noise Percentile:"));
        entry3.add(new JTextField(String.format("%.2f", AAP_Constants.cNOISEPERCENTILE) , 4));
        panel.add(entry3);

        JPanel entry4 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        entry4.add(new JLabel("Detection Variance:"));
        entry4.add(new JTextField(Double.toString(AAP_Constants.cDETECTIONVARIANCE), 4));
        panel.add(entry4);

        return panel;
    }



    /** Tests the plugin. */
    public static void main(final String... args) {
    }

}
