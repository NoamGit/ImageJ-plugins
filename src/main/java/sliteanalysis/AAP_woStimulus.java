package sliteanalysis;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.AVI_Reader;
import ij.plugin.PlugIn;
import ij.plugin.SubstackMaker;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.util.ArrayList;

/**
 * Created by noambox on 12/20/2015.
 */
public class AAP_woStimulus extends AAP_wStimulus implements PlugIn {

    @Override
    public void run(String argv) {
        if(this.setup()) { // validates conditions
            IJ.showStatus(" Dark noise removal...");
            imp_r = DarkNoiseRemoval(imp_r, NOISE_ROI);
            if(removeFirst){
                ImageStack t_stack = imp_r.getStack(); // deletes first slice
                t_stack.deleteSlice(1);
                imp_r.setStack(t_stack);
            }

            // get Cells by segmentation;
            IJ.showStatus(" Cell Segmentation...");
            ImagePlus avr_imp = AApSegmetator.getAverageIm(imp_r);
            AApSegmetator segmentator = new AApSegmetator(avr_imp);
            this.cells_roi = segmentator.SegmentCellsCWT();
            //this.cells_roi = segmentator.SegmentCellsML();

            //Resample stimulus
            if(this.useArtifact) {
                ArrayList<Double> stim_vector = new ArrayList<>();
                double[] var1 = Activity_Analysis.getAverageSignal(imp_r);
                Object[] var2 = Extract_Stimulus.getPeaks(var1, GETPEAKSCONST);
                TimeSeries var3 = Extract_Stimulus.getStimulusArray(var2, imp_r.getStackSize());
                for (int i = 0; i < var3.Signal.length; i++) {
                    stim_vector.add(var3.Signal[i]);
                }

                // Remove stimulus slices
                ArrayList<Integer> stim_sampels = new ArrayList<>();
                for (int i = 0; i < stim_vector.size(); i++) {
                    if (stim_vector.get(i) > 0)
                        stim_sampels.add(i);
                }
                this.imp_r = ReplaceSlices(this.imp_r,stim_sampels);

                // Kalman filter
                ImageStack ims = imp_r.getStack();
                Kalman_Stack_Filter kl = new Kalman_Stack_Filter();
                kl.filter(ims, this.KL_PRECVAR, this.KM_GAIN);
                imp_r.setStack(ims);

                // wrap everything with the Cell manager
                this.cm = toCellManager(this.imp_r, this.cells_roi,avr_imp, stim_vector);
                return;
            }

            // Kalman filter
            ImageStack ims = imp_r.getStack();
            Kalman_Stack_Filter kl = new Kalman_Stack_Filter();
            kl.filter(ims, this.KL_PRECVAR, this.KM_GAIN);
            imp_r.setStack(ims);

            // wrap everything with the Cell manager
            this.cm = toCellManager(this.imp_r, this.cells_roi,avr_imp);
        }

        // run local method

    }

    @Override
    /* Setup GUI */
    protected final boolean setup() {
        if(IJ.versionLessThan("1.40c")) {
            return false;
        } else {
            int[] ids = WindowManager.getIDList();
                ArrayList titlesList = new ArrayList();
                ArrayList idsList = new ArrayList();
                String currentTitle = null;

                ImagePlus var1 = WindowManager.getImage(ids[0]);
                titlesList.add(var1.getTitle());
                idsList.add(Integer.valueOf(ids[0]));
                if(var1 == WindowManager.getCurrentImage()) {
                    currentTitle = var1.getTitle();
                }

                String[] var8 = new String[titlesList.size()];
                titlesList.toArray(var8);
                if(currentTitle == null) {
                    currentTitle = var8[0];
                }
                SubstackMaker sm = new SubstackMaker();
                GenericDialog gd = new GenericDialog("AAP w/o stimulus");
                gd.setLayout(new BoxLayout(gd, BoxLayout.Y_AXIS)); // check
                gd.addChoice("Stack", var8, currentTitle);
                String defautltSlices = "1-" + WindowManager.getImage(((Integer) idsList.get(0)).intValue()).getStackSize();
                gd.addMessage("Enter a range (e.g. 2-14)...", null, Color.darkGray);
                gd.add(Box.createRigidArea(new Dimension(0, 2)));
                gd.addStringField("Slices of Response:", defautltSlices, 40);
                gd.add(Box.createRigidArea(new Dimension(0, 10)));
                JButton settings = new JButton("settings");
                settings.setBackground(Color.lightGray);
                settings.setBorderPainted(true);
                settings.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        UI_Settings.openSettings();
                    }
                });
                gd.add(settings);
                gd.add(Box.createRigidArea(new Dimension(0, 10)));
                gd.showDialog();
                if(gd.wasCanceled()) {
                    return false;
                } else {
                    super.loadPrefs();
                    this.imp_r = WindowManager.getImage(((Integer) idsList.get(gd.getNextChoiceIndex())).intValue());
                    String userInput= gd.getNextString();
                    this.dt_r = Activity_Analysis.findSignalDt(this.imp_r.getTitle());
                    if (userInput==null) {
                        return false;
                    }
                    IJ.showStatus(" Making substack copy of selection...");
                    this.imp_r  = sm.makeSubstack(this.imp_r, userInput);
                    return true;
                }

        }
    }

    public static void main(final String... args) throws FileNotFoundException {
        AAP_woStimulus aap = new AAP_woStimulus();
        String path;
        try {
            path = "D:\\# Projects (Noam)\\# SLITE\\# DATA\\AAP 1.1.0\\"; // LAB
            ImagePlus imp_test = IJ.openImage(path + "TEXT_20msON_10Hz_SLITE_2.tif");
            if(imp_test == null){
                throw new FileNotFoundException("Your not in Lab....");
            }
        }
        catch(FileNotFoundException error){
            path = "C:\\Users\\noambox\\Dropbox\\# Graduate studies M.Sc\\# SLITE\\ij - plugin data\\"; //HOME
        }

        //aap.imp_r = IJ.openImage(path + "TEXT_20msON_10Hz_SLITE_2.tif"); // DEBUG
        aap.imp_r = IJ.openImage(path + "test_TEXT_10Hz.tif"); // DEBUG

        aap.imp_r.show();
        String argv = "";
        aap.run(argv);
    }
}
