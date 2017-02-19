package sliteanalysis;

import ij.Prefs;
import ij.gui.GenericDialog;
import ij.gui.Roi;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

/**
 * Created by noambox on 12/21/2015.
 */
public class UI_Settings {

    /* Methods*/
    public static void openSettings() {
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
                gd.pack();

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

    public static void openSettings_ED() {
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
                JComponent panel4 = makeRecenterPanel("Recenter parameters");
                gd.pack();

                JTabbedPane tabbedPane = new JTabbedPane();
                tabbedPane.addTab("Stack", panel1);
                tabbedPane.addTab("Segmentation", panel2);
                tabbedPane.addTab("Processing", panel3);
                tabbedPane.addTab("Recenter", panel4);
                gd.add(tabbedPane);

                gd.pack();
                gd.showDialog();
                ArrayList<String> text_value = readStringsFromPanel(panel1);
                text_value.addAll(readStringsFromPanel(panel2));
                text_value.addAll(readStringsFromPanel(panel3));
                text_value.addAll(readStringsFromPanel(panel4));

                ArrayList<Boolean> chk_value = readCheckBoxFromPanel(panel1);
                chk_value.addAll(readCheckBoxFromPanel(panel2));
                chk_value.addAll(readCheckBoxFromPanel(panel3));

                savePrefs_ED(text_value, chk_value);
            }
        });

    }

    public static void openSettingsNow() {
//        try {
//            SwingUtilities.invokeAndWait(new Runnable() {
//                public void run() {
                    int out = 0;
                    JFrame frame = new JFrame("Settings");
                    GenericDialog gd = new GenericDialog("settings", frame);
                    gd.setLayout(new BoxLayout(gd, BoxLayout.PAGE_AXIS));

                    // UI components
                    JComponent panel1 = makeStackPanel("Stack Settings");
                    JComponent panel2 = makeSegPanel("Cell Detection");
                    JComponent panel3 = makeProcessingPanel("Calcium Signal Processing");
                    gd.pack();

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
//            });
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        } catch (InvocationTargetException e) {
//            e.printStackTrace();
//        }
//    }


    private static ArrayList<String> readStringsFromPanel(JComponent comp) {
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

    private static ArrayList<Boolean> readCheckBoxFromPanel(JComponent comp) {
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
    protected static JComponent makeStackPanel(String text) {

        JPanel panel = new JPanel();
        BoxLayout box = new BoxLayout(panel, BoxLayout.PAGE_AXIS);
        panel.setLayout(box);

        double getpeak_default  = Prefs.get("sliteanalysis.cGETPEAKSCONST", AAP_Constants.cGETPEAKSCONST);
        double fs_default  = Prefs.get("sliteanalysis.cSTIMULUS_FR", AAP_Constants.cSTIMULUS_FR);
        double sr_default = (1/fs_default) < 1D ? fs_default : 1/fs_default; // sampling rate is always by default over 1 hz
        boolean removeFirst =Prefs.get("sliteanalysis.cREMOVEFIRST",AAP_Constants.cREMOVEFIRST);
        int nrl_u = (int) Prefs.get("sliteanalysis.cNOISE_ROI_UP",AAP_Constants.cNOISE_ROI_UP);
        int nrl_l = (int) Prefs.get("sliteanalysis.cNOISE_ROI_LOW",AAP_Constants.cNOISE_ROI_LOW);
        double KM_GAIN = Prefs.get("sliteanalysis.cKM_GAIN",AAP_Constants.cKM_GAIN);
        double KM_PRECVAR = Prefs.get("sliteanalysis.cKM_PRECVAR",AAP_Constants.cKM_PRECVAR);
        boolean useArtifact = Prefs.get("sliteanalysis.cUSEARTIFACT",AAP_Constants.cUSEARTIFACT);
        boolean useKalman = Prefs.get("sliteanalysis.cUSEKALMAN",AAP_Constants.cUSEKALMAN);
        boolean replaceArtifact = Prefs.get("sliteanalysis.cREPLACEARTIFACT",AAP_Constants.cREPLACEARTIFACT);

        JPanel entry0 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        entry0.add(new JLabel("Threshold on the variance for Peak detection:"));
        entry0.add(new JTextField(Double.toString(getpeak_default), 4));
        panel.add(entry0);

        JPanel entry1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        entry1.add(new JLabel("Stimulus frame rate (Hz):"));
        entry1.add(new JTextField(Double.toString(sr_default), 4));
        panel.add(entry1);

        JPanel entry2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        entry2.add(new JLabel("Kalman Gain:"));
        entry2.add(new JTextField(Double.toString(KM_GAIN), 4));
        panel.add(entry2);

        JPanel entry3 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        entry3.add(new JLabel("Kalman var:"));
        entry3.add(new JTextField(Double.toString(KM_PRECVAR), 4));
        panel.add(entry3);

        JPanel entry4 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        entry4.add(new JLabel("Remove slice(0):"));
        entry4.add(new JCheckBox("",removeFirst));
        panel.add(entry4);

        JPanel entry5 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        entry5.add(new JLabel("Noise ROI bounderies:"));
        entry5.add(new JTextField(Double.toString(nrl_u), 4));
        entry5.add(new JTextField(Double.toString(nrl_l), 4));
        panel.add(entry5);

        JPanel entry6 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        entry6.add(new JLabel("Use Artifact:"));
        entry6.add(new JCheckBox("",useArtifact));
        panel.add(entry6);

        JPanel entry7 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        entry7.add(new JLabel("Replace Artifact slices:"));
        entry7.add(new JCheckBox("",replaceArtifact));
        panel.add(entry7);

        JPanel entry8 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        entry8.add(new JLabel("Use Kalman Filter:"));
        entry8.add(new JCheckBox("", useKalman));
        panel.add(entry8);

        return panel;
    }

    protected static JComponent makeSegPanel(String text) {

        double FM_TOL = Prefs.get("sliteanalysis.cFM_TOL", AAP_Constants.cFM_TOL);
        double CM_MAX = (int) Prefs.get("sliteanalysis.cCM_MAX",AAP_Constants.cCM_MAX);
        double ENLARGEROI = (int) Prefs.get("sliteanalysis.cENLARGEROI",AAP_Constants.cENLARGEROI);
        double ELLIPSE_a = (int) Prefs.get("sliteanalysis.cELLIPSE_a",AAP_Constants.cELLIPSE_a);
        double ELLIPSE_b = (int) Prefs.get("sliteanalysis.cELLIPSE_b",AAP_Constants.cELLIPSE_b);

        JPanel panel = new JPanel();
        BoxLayout box = new BoxLayout(panel, BoxLayout.PAGE_AXIS);
        panel.setLayout(box);

        JPanel entry1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        entry1.add(new JLabel("Find Maxima Tolerance:"));
        entry1.add(new JTextField(Double.toString(FM_TOL), 4));
        panel.add(entry1);

        JPanel entry2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        entry2.add(new JLabel("Cell's Max Diameter:"));
        entry2.add(new JTextField(Double.toString(CM_MAX), 4));
        panel.add(entry2);

        JPanel entry3 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        entry3.add(new JLabel("Enlarge factor:"));
        entry3.add(new JTextField(Double.toString(ENLARGEROI), 4));
        panel.add(entry3);

        JPanel entry4 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        entry4.add(new JLabel("Ellipse Center B:"));
        entry4.add(new JTextField(Double.toString(ELLIPSE_a), 4));
        panel.add(entry4);

        JPanel entry5 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        entry5.add(new JLabel("Ellipse Center B:"));
        entry5.add(new JTextField(Double.toString(ELLIPSE_b), 4));
        panel.add(entry5);

        return panel;
    }

    protected static JComponent makeRecenterPanel(String text) {
        int maxiter = (int) Prefs.get("sliteanalysis.cMAXITER_ED", AAP_Constants.cMAXITER_ED);
        double limit = Prefs.get("sliteanalysis.cCLIMIT_ED",AAP_Constants.cCLIMIT_ED);
        double scale = Prefs.get("sliteanalysis.cSCALE_ED",AAP_Constants.cSCALE_ED);
        int width  = (int) Prefs.get("sliteanalysis.cWIDTH_ED",AAP_Constants.cWIDTH_ED);
        int height = (int) Prefs.get("sliteanalysis.cHEIGHT_ED",AAP_Constants.cHEIGHT_ED);

        JPanel panel = new JPanel();
        BoxLayout box = new BoxLayout(panel, BoxLayout.PAGE_AXIS);
        panel.setLayout(box);

        JPanel entry1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        entry1.add(new JLabel("Max iterations:"));
        entry1.add(new JTextField(Double.toString(maxiter), 4));
        panel.add(entry1);

        JPanel entry2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        entry2.add(new JLabel("Convergence limit:"));
        entry2.add(new JTextField(Double.toString(limit), 4));
        panel.add(entry2);

        JPanel entry3 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        entry3.add(new JLabel("Roi Scale:"));
        entry3.add(new JTextField(Double.toString(scale), 4));
        panel.add(entry3);

        JPanel entry4 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        entry4.add(new JLabel("Roi width:"));
        entry4.add(new JTextField(Double.toString(width), 4));
        panel.add(entry4);

        JPanel entry5 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        entry5.add(new JLabel("Roi height:"));
        entry5.add(new JTextField(Double.toString(height), 4));
        panel.add(entry5);

        return panel;
    }

    protected static JComponent makeProcessingPanel(String text) {
        JPanel panel = new JPanel();
        BoxLayout box = new BoxLayout(panel, BoxLayout.PAGE_AXIS);
        panel.setLayout(box);
        JPanel text1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel details2 = new JLabel("<html>Chebychev LPF filter for trend estimation<br>Settings of the DF/F processing uses</html>",SwingConstants.CENTER);
        details2.setFont(Font.getFont(Font.SANS_SERIF));
        text1.add(details2);
        panel.add(text1);
//        JLabel details2 = new JLabel("Chebychev LPF filter for trend estimation",JLabel.LEFT);
//        JPanel text2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
//        JLabel details1 = new JLabel("Settings of the DF/F processing uses",JLabel.LEFT);
//        details1.setFont(Font.getFont(Font.SANS_SERIF));
//        text2.add(details1);
//        panel.add(text2);
//        JLabel details1 = new JLabel("Settings of the DF/F processing uses",JLabel.LEFT);
//        panel.add(details1);
//        panel.add(details2);
        panel.add(new JLabel("", JLabel.CENTER));

        double CUTOFF = Prefs.get("sliteanalysis.cCUTOFF", AAP_Constants.cCUTOFF);
        float noisePrecentile = (float) Prefs.get("sliteanalysis.cNOISEPERCENTILE",AAP_Constants.cNOISEPERCENTILE);
        float detectionVariance = (int) Prefs.get("sliteanalysis.cDETECTIONVARIANCE",AAP_Constants.cDETECTIONVARIANCE);
        double ORDER = (int) Prefs.get("sliteanalysis.cORDER",AAP_Constants.cORDER);

        JPanel entry1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        entry1.add(new JLabel("Cut off Frequency (Hz):"));
        entry1.add(new JTextField(Double.toString(CUTOFF), 4));
        panel.add(entry1);

        JPanel entry2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        entry2.add(new JLabel("Filter's order:"));
        entry2.add(new JTextField(Double.toString(ORDER),4));
        panel.add(entry2);

        JPanel entry3 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        entry3.add(new JLabel("BaseLine Noise Percentile:"));
        entry3.add(new JTextField(String.format("%.2f", noisePrecentile) , 4));
        panel.add(entry3);

        JPanel entry4 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        entry4.add(new JLabel("Detection Variance:"));
        entry4.add(new JTextField(Double.toString(detectionVariance), 4));
        panel.add(entry4);

        panel.revalidate();
        panel.repaint();
        return panel;
    }

    private static void savePrefs(ArrayList<String> text_value, ArrayList<Boolean> chk_value) {
        Prefs.set("sliteanalysis.cGETPEAKSCONST", text_value.get(0));
        Prefs.set("sliteanalysis.cSTIMULUS_FR", text_value.get(1));
        Prefs.set("sliteanalysis.cKM_GAIN", text_value.get(2));
        Prefs.set("sliteanalysis.cKM_PRECVAR", text_value.get(3));
        Prefs.set("sliteanalysis.cNOISE_ROI_UP", text_value.get(4));
        Prefs.set("sliteanalysis.cNOISE_ROI_LOW", text_value.get(5));

        Prefs.set("sliteanalysis.cFM_TOL", text_value.get(6));
        Prefs.set("sliteanalysis.cCM_MAX", text_value.get(7));
        Prefs.set("sliteanalysis.cENLARGEROI", text_value.get(8));
        Prefs.set("sliteanalysis.cELLIPSE_a", text_value.get(9));
        Prefs.set("sliteanalysis.cELLIPSE_b", text_value.get(10));

        Prefs.set("sliteanalysis.cCUTOFF", text_value.get(11));
        Prefs.set("sliteanalysis.cORDER", text_value.get(12));
        Prefs.set("sliteanalysis.cNOISEPERCENTILE", text_value.get(13));
        Prefs.set("sliteanalysis.cDETECTIONVARIANCE", text_value.get(14));

        Prefs.set("sliteanalysis.cREMOVEFIRST", chk_value.get(0));
        Prefs.set("sliteanalysis.cUSEARTIFACT", chk_value.get(1));
        Prefs.set("sliteanalysis.cREPLACEARTIFACT",chk_value.get(2));
        Prefs.set("sliteanalysis.cUSEKALMAN",chk_value.get(3));
        Prefs.savePreferences(); // Throws error probabely because the ij is not initialized
    }

    private static void savePrefs_ED(ArrayList<String> text_value, ArrayList<Boolean> chk_value) {
        Prefs.set("sliteanalysis.cGETPEAKSCONST", text_value.get(0));
        Prefs.set("sliteanalysis.cSTIMULUS_FR", text_value.get(1));
        Prefs.set("sliteanalysis.cKM_GAIN", text_value.get(2));
        Prefs.set("sliteanalysis.cKM_PRECVAR", text_value.get(3));
        Prefs.set("sliteanalysis.cNOISE_ROI_UP", text_value.get(4));

        Prefs.set("sliteanalysis.cFM_TOL", text_value.get(6));
        Prefs.set("sliteanalysis.cCM_MAX", text_value.get(7));
        Prefs.set("sliteanalysis.cENLARGEROI", text_value.get(8));
        Prefs.set("sliteanalysis.cELLIPSE_a", text_value.get(9));
        Prefs.set("sliteanalysis.cELLIPSE_b", text_value.get(10));

        Prefs.set("sliteanalysis.cCUTOFF", text_value.get(11));
        Prefs.set("sliteanalysis.cORDER", text_value.get(12));
        Prefs.set("sliteanalysis.cNOISEPERCENTILE", text_value.get(13));
        Prefs.set("sliteanalysis.cDETECTIONVARIANCE", text_value.get(14));

        Prefs.set("sliteanalysis.cREMOVEFIRST", chk_value.get(0));
        Prefs.set("sliteanalysis.cUSEARTIFACT", chk_value.get(1));
        Prefs.set("sliteanalysis.cREPLACEARTIFACT",chk_value.get(2));
        Prefs.set("sliteanalysis.cUSEKALMAN",chk_value.get(3));

        Prefs.set("sliteanalysis.cMAXITER_ED", text_value.get(15));
        Prefs.set("sliteanalysis.cCLIMIT_ED", text_value.get(16));
        Prefs.set("sliteanalysis.cSCALE_ED", text_value.get(17));
        Prefs.set("sliteanalysis.cWIDTH_ED", text_value.get(18));
        Prefs.set("sliteanalysis.cHEIGHT_ED", text_value.get(19));

        Prefs.savePreferences(); // Throws error probabely because the ij is not initialized
    }

}
