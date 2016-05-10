package sliteanalysis;

import ij.*;
import ij.gui.GenericDialog;
import ij.gui.MessageDialog;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.io.OpenDialog;
import ij.io.Opener;
import ij.io.RoiDecoder;
import ij.plugin.AVI_Reader;
import ij.plugin.PlugIn;
import ij.plugin.SubstackMaker;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by noambox on 12/20/2015.
 */
public class AAP_woStimulus extends AAP_wStimulus implements PlugIn {

    @Override
    public void run(String argv) {
        if(this.setup()) { // validates conditions
            if(removeFirst){
                ImageStack t_stack = imp_r.getStack(); // deletes first slice
                t_stack.deleteSlice(1);
                imp_r.setStack(t_stack);
            }

            //Resample stimulus
            if(this.useArtifact) {
                IJ.showStatus(" Obtaining artifact ...");
                ArrayList<Double> stim_vector = new ArrayList<>();
                double[] var1 = Activity_Analysis.getAverageSignal(imp_r);
                double cutoff = Prefs.get("sliteanalysis.cCUTOFF", AAP_Constants.cCUTOFF);
                double[] var1_5 = CalciumSignal.DetrendSignal(var1, cutoff);
                Object[] var2 = Extract_Stimulus.getPeaks(var1_5, GETPEAKSCONST);
                TimeSeries var3 = Extract_Stimulus.getStimulusArray(var2, imp_r.getStackSize());
                for (int i = 0; i < var3.Signal.length; i++) {
                    stim_vector.add(var3.Signal[i]);
                }
                if (NOISE_ROI.getBounds().getWidth() != 1 || NOISE_ROI.getBounds().getHeight()!= 1){
                    IJ.showStatus(" Dark noise removal...");
                    imp_r = DarkNoiseRemoval(imp_r, NOISE_ROI);
                }

                // get Cells by segmentation;
                IJ.showStatus(" Cell Segmentation...");
                ImagePlus avr_imp = AApSegmetator.getAverageIm(imp_r);
                AApSegmetator segmentator = new AApSegmetator(avr_imp);
                this.cells_roi = segmentator.SegmentCellsCWT();
                //this.cells_roi = segmentator.SegmentCellsML();

                // Remove stimulus slices
                if(this.replaceArtifact) {
                    ArrayList<Integer> stim_sampels = new ArrayList<>();
                    for (int i = 0; i < stim_vector.size(); i++) {
                        if (stim_vector.get(i) > 0)
                            stim_sampels.add(i);
                    }
                    this.imp_r = ReplaceSlices(this.imp_r, stim_sampels);
                }

                this.imp_r.show();
//                IJ.run("In [+]", "");
//                IJ.run("In [+]", "");

                // Kalman filter
                if(this.useKalman) {
                    ImageStack ims = imp_r.getStack();
                    Kalman_Stack_Filter kl = new Kalman_Stack_Filter();
                    kl.filter(ims, this.KL_PRECVAR, this.KM_GAIN);
                    imp_r.setStack(ims);
                }

                CalciumSignal.showSignal(stim_vector);


                // wrap everything with the Cell manager
                this.cm = toCellManager(this.imp_r, this.cells_roi,avr_imp, stim_vector);
                this.cm.m_lis.listen();
                return;
            }

            if (NOISE_ROI.getBounds().getWidth() != 1 || NOISE_ROI.getBounds().getHeight()!= 1){
                IJ.showStatus(" Dark noise removal...");
                imp_r = DarkNoiseRemoval(imp_r, NOISE_ROI);
            }

            // get Cells by segmentation;
            IJ.showStatus(" Cell Segmentation...");
            ImagePlus avr_imp = AApSegmetator.getAverageIm(imp_r);
            AApSegmetator segmentator = new AApSegmetator(avr_imp);
            this.cells_roi = segmentator.SegmentCellsCWT();
            //this.cells_roi = segmentator.SegmentCellsML();

            // Kalman filter
            if(this.useKalman) {
                ImageStack ims = imp_r.getStack();
                Kalman_Stack_Filter kl = new Kalman_Stack_Filter();
                kl.filter(ims, this.KL_PRECVAR, this.KM_GAIN);
                imp_r.setStack(ims);
            }

            this.imp_r.show();
//            IJ.run("In [+]", "");
//            IJ.run("In [+]", "");

            // wrap everything with the Cell manager
            this.cm = toCellManager(this.imp_r, this.cells_roi,avr_imp);
            this.cm.m_lis.listen();
        }

        // run local method

    }

    /* This is an implementation for run except it preformed without any user interface and parameters are prefixed */
    public void run_auto(File file_iter, ij.io.DirectoryChooser output_dir, OpenDialog roi_dir){

        // load image and set default parameters according to name
        super.loadPrefs();
        this.imp_r =  IJ.openImage(file_iter.getPath());
        this.dt_r = Activity_Analysis.findSignalDt(this.imp_r.getTitle());

        // remove first frame
        if(removeFirst){
            ImageStack t_stack = imp_r.getStack(); // deletes first slice
            t_stack.deleteSlice(1);
            imp_r.setStack(t_stack);
        }

        // load cells from file apply recentering and Kalman if needed
        ImagePlus avr_imp = AApSegmetator.getAverageIm(imp_r);
        this.cells_roi = loadROIZip(roi_dir.getPath());
        if(!file_iter.getPath().contains("Artif")){ //recenter ROIs according to image
            Stimulus_API.recenterRois(this.cells_roi, avr_imp);
        }
        if(this.useKalman) {
            IJ.showStatus(file_iter.getName() + " Kalman filterirng...");
            ImageStack ims = imp_r.getStack();
            Kalman_Stack_Filter kl = new Kalman_Stack_Filter();
            kl.filter(ims, this.KL_PRECVAR, this.KM_GAIN);
            imp_r.setStack(ims);
        }

        // wrap to Cell manager select all and save
        if(file_iter.getPath().contains("Artif")){
            imp_r.setTitle("Artif_"+imp_r.getTitle());
        }
        this.cm = toCellManager(this.imp_r, this.cells_roi,avr_imp);
        IJ.showStatus(file_iter.getName() + " saving to *.xlsx...");
        cm.selectAll();
        cm.save("raw", output_dir);

        // clears workspace
        imp_r.close();
        avr_imp.close();
        cm.close();
        cm.avr_imp.close();
        cm.instance = null;
        cm = null;
        imp_r = null;
        avr_imp = null;
        }

    /*
     * This method enables to load a zip file with ROI's into a Array<PolygonRoi> array
     */
    static public ArrayList<PolygonRoi> loadROIZip(String path) {
        ArrayList<PolygonRoi> roi_list = new ArrayList<>();
        ZipInputStream in = null;
        ByteArrayOutputStream out;
        boolean noFilesOpened = true; // we're pessimistic and expect that the zip file dosent contain any .roi
        try {
            in = new ZipInputStream(new FileInputStream(path));
            byte[] buf = new byte[1024];
            int len;
            // The original while was: while(true) do something which is not very good
            ZipEntry entry = in.getNextEntry();
            while (entry!=null) {
                String name = entry.getName();
                if (name.endsWith(".roi")) {
                    out = new ByteArrayOutputStream();
                    while ((len = in.read(buf)) > 0)
                        out.write(buf, 0, len);
                    out.close();
                    byte[] bytes = out.toByteArray();
                    RoiDecoder rd = new RoiDecoder(bytes, name);
                    Roi roi = rd.getRoi();
                    if (roi!=null) {
                        roi_list.add((PolygonRoi) roi);
                        noFilesOpened = false; // We just added a .roi
                    }
                }
                entry = in.getNextEntry();
            }
            IJ.showStatus("Loading Rois success!");
            in.close();
            return roi_list;
        } catch (IOException e) { IJ.log(e.toString()); }
        if(noFilesOpened){  IJ.log("This ZIP archive does not appear to contain \".roi\" files"); }
        return null;
    }

    @Override
    /* Setup GUI */
    protected final boolean setup() {
        if (IJ.versionLessThan("1.40c")) {
            return false;
        } else {
            int[] ids = WindowManager.getIDList();
                ArrayList titlesList = new ArrayList();
                ArrayList idsList = new ArrayList();
                String currentTitle = null;

                for(int titles = 0; titles < ids.length; ++titles) {
                    ImagePlus var1 = WindowManager.getImage(ids[titles]);
                    titlesList.add(var1.getTitle());
                    idsList.add(Integer.valueOf(ids[titles]));
                    if(var1 == WindowManager.getCurrentImage()) {
                        currentTitle = var1.getTitle();
                    }
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
                gd.pack();
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
                    String imp_title = this.imp_r.getTitle();
                    int indx = imp_title.indexOf(".");
                    imp_title =  imp_title.substring(0,indx);
                    this.imp_r  = sm.makeSubstack(this.imp_r, userInput );
                    this.imp_r.setTitle(imp_title + "_sub" +userInput);
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
//        aap.imp_r = IJ.openImage(path + "test_TEXT_10Hz.tif"); // DEBUG
        aap.imp_r = IJ.openImage(path + "FLS_10Hz_5.tif"); // DEBUG
        aap.imp_r.show();
        String argv = "";
        aap.run(argv);
    }
}
// TODO: implement abortion of the plugin
class runOnThread implements Runnable{

    public void run(){

        if (Thread.interrupted()) {
            // We've been interrupted
            return;
        }
    }

    public static void main(String args[]){
        (new Thread(new runOnThread())).start();
    }
}
