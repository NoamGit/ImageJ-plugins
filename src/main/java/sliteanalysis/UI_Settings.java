package sliteanalysis;

import ij.Prefs;
import ij.gui.GenericDialog;

import javax.swing.*;
import java.awt.*;
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

        JPanel entry0 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        entry0.add(new JLabel("Peaks var threshold:"));
        entry0.add(new JTextField(Double.toString(AAP_Constants.cGETPEAKSCONST), 4));
        panel.add(entry0);

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

    protected static JComponent makeSegPanel(String text) {
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

    protected static JComponent makeProcessingPanel(String text) {
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
        Prefs.savePreferences(); // Throws error probabely because the ij is not initialized
    }

}
