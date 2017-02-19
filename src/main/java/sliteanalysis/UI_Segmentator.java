package sliteanalysis;

import graphcut.Graph_Cut;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.ImageRoi;
import ij.gui.PolygonRoi;
import ij.io.OpenDialog;
import ij.plugin.Converter;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import ij.process.LUT;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

/**
 * Created by noambox on 13/02/2017.
 */
public class UI_Segmentator extends JFrame implements ChangeListener{
    protected ImagePlus imp;
    ImagePlus bkg_imp = null;
    protected ImageIcon imcon;
    protected AApSegmetator _segmentator;
    OpenDialog _mdl_dialog = null;
    ArrayList<PolygonRoi> _cells = null;
    boolean invoked = false;

    // segmentation local parameters
    private double FM_TOL = 8;
    private int CMW_MAX = 16;
    private int CMW_MIN = 3;
    private double ENLARGEROI = 2;
    private float BKG_FOREGROUNDBIAS = 0.5F;
    private float BKG_SMOOTH = 5F;
    private double BKG_OPACITY = 0.2;

    private JPanel rootPanel;
    private JTabbedPane settings_TAB;
    private JButton run_BTN;
    private JLabel previewBox;
    private JCheckBox preview_CHBX;
    private JPanel param_TAB;
    private JPanel stack_TAB;
    private JSlider fmaxtol_SLD;
    private JSlider cmw_SLD;
    private JSlider gc_fg_SLD;
    private JLabel fmaxtol_LBL;
    private JTextField logTextField;
    private JCheckBox rmvBkg_CHBX;
    private JSlider gc_sm_SLD;
    private JButton previewBRButton;
    private JButton brwmdl_BTN;
    private JTextField calssipath_LBL;
    private JPanel process_TAB;
    private JTextField framerate_TXT;
    private JFormattedTextField thres_TXT;
    private JTextField kalmangain_TXT;
    private JTextField kalmanvar_TXT;
    private JTextField cutoff_TXT;
    private JTextField filterord_TXT;
    private JTextField perc_TXT;
    private JTextField detvar_TXT;
    private JCheckBox useKalman_CBX;
    private JRadioButton onlySegmentRadioButton;
    private JRadioButton segmentAnalyzeRadioButton;

    // default constructor
    public UI_Segmentator(){
        pack();
        ImagePlus open_imp = WindowManager.getCurrentImage();
        setSize(850,500);
        setTitle("AAP segmentation");
        setContentPane(rootPanel);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // preprocessing image
        if(open_imp.getImageStackSize() > 1){
            imp = AApSegmetator.getAverageIm(open_imp);
        }
        else{
            imp = open_imp;
        }

        imcon = new ImageIcon();
        imcon.setImage(imp.getImage());
        _segmentator = new AApSegmetator(imp);
        previewBox.setIcon(imcon);

        // findmaxima parameter change
        fmaxtol_SLD.addChangeListener(this);

        // change in cell size parameter (Cell Magic Wand parameter)
        cmw_SLD.addChangeListener(this);

        setVisible(true);
        previewBRButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updatePreview();
            }
        });
        brwmdl_BTN.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                _mdl_dialog = new OpenDialog("Choose classification model...", "");
                if(_mdl_dialog.getFileName().contains(".model")){
                    calssipath_LBL.setText(_mdl_dialog.getDirectory());
                    logTextField.setText("Classification model loaded!");
                }
                else{
                    logTextField.setText("WARNING::Selected file is not a classification model!");
                    _mdl_dialog = null;
                }
                }
        });
        run_BTN.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                // update AAP_constant parameters
                savePref();

                // close UI
                dispose();

                imp.setHideOverlay(true);
                imp.setOverlay(null);

                // set segmentation algorithm and run
                _segmentator = new AApSegmetator(imp);
                if(_mdl_dialog == null){ // run segmentation CMW mode
                    _cells = _segmentator.SegmentForgroundCellsCWT(bkg_imp);
                }
                else{ // run segmentation ML mode
                    _cells = _segmentator.SegmentForgroundCellsML(_mdl_dialog, bkg_imp);
                }

                if(onlySegmentRadioButton.isSelected())
                {
                    // show cells in cell manager
                    RoiManager rm = new RoiManager();
                    for (int i = 0; i < _cells.size(); i++) {
                        rm.addRoi(_cells.get(i));
                    }
                }
                else if(segmentAnalyzeRadioButton.isSelected()){

                    // Validate data type (Stacks)
                    //ImagePlus imp = IJ.getImage();
                    if (imp.getStackSize()==1)
                    {IJ.error("Stack required"); return;}
                    System.out.print("DEBUG\n");
                    AAP_woStimulus aap = new AAP_woStimulus();
                    aap.run_auto(imp,_cells);
                }

            }
        });
        framerate_TXT.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                super.keyTyped(e);
                char c = e.getKeyChar();
                if(!(Character.isDigit(c) || (c==KeyEvent.VK_BACK_SPACE) || (c == KeyEvent.VK_DELETE))){
                    e.consume();
                }
            }
        });
        thres_TXT.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                super.keyTyped(e);
                char c = e.getKeyChar();
                if(!(Character.isDigit(c) || (c==KeyEvent.VK_BACK_SPACE) || (c == KeyEvent.VK_DELETE))){
                    e.consume();
                }
            }
        });
        kalmangain_TXT.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                super.keyTyped(e);
                char c = e.getKeyChar();
                if(!(Character.isDigit(c) || (c==KeyEvent.VK_BACK_SPACE) || (c == KeyEvent.VK_DELETE))){
                    e.consume();
                }
            }
        });
        kalmanvar_TXT.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                super.keyTyped(e);
                char c = e.getKeyChar();
                if(!(Character.isDigit(c) || (c==KeyEvent.VK_BACK_SPACE) || (c == KeyEvent.VK_DELETE))){
                    e.consume();
                }
            }
        });
        detvar_TXT.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                super.keyTyped(e);
                char c = e.getKeyChar();
                if(!(Character.isDigit(c) || (c==KeyEvent.VK_BACK_SPACE) || (c == KeyEvent.VK_DELETE))){
                    e.consume();
                }
            }
        });
        perc_TXT.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                super.keyTyped(e);
                char c = e.getKeyChar();
                if(!(Character.isDigit(c) || (c==KeyEvent.VK_BACK_SPACE) || (c == KeyEvent.VK_DELETE))){
                    e.consume();
                }
            }
        });
        filterord_TXT.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                super.keyTyped(e);
                char c = e.getKeyChar();
                if(!(Character.isDigit(c) || (c==KeyEvent.VK_BACK_SPACE) || (c == KeyEvent.VK_DELETE))){
                    e.consume();
                }
            }
        });
        cutoff_TXT.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                super.keyTyped(e);
                char c = e.getKeyChar();
                if(!(Character.isDigit(c) || (c==KeyEvent.VK_BACK_SPACE) || (c == KeyEvent.VK_DELETE))){
                    e.consume();
                }
            }
        });
    }

    private void savePref() {

        Prefs.set("sliteanalysis.cGETPEAKSCONST", Double.parseDouble(thres_TXT.getText()));
        Prefs.set("sliteanalysis.cSTIMULUS_FR",Double.parseDouble(framerate_TXT.getText()));
        Prefs.set("sliteanalysis.cKM_GAIN", Double.parseDouble(kalmangain_TXT.getText()));
        Prefs.set("sliteanalysis.cKM_PRECVAR", Double.parseDouble(kalmanvar_TXT.getText()));

        Prefs.set("sliteanalysis.cFM_TOL", fmaxtol_SLD.getValue());
        Prefs.set("sliteanalysis.cCM_MAX", cmw_SLD.getValue());
//        Prefs.set("sliteanalysis.cENLARGEROI", text_value.get(8));

        Prefs.set("sliteanalysis.cCUTOFF", Double.parseDouble(cutoff_TXT.getText()));
        Prefs.set("sliteanalysis.cORDER", Double.parseDouble(filterord_TXT.getText()));
        Prefs.set("sliteanalysis.cNOISEPERCENTILE", Float.parseFloat(perc_TXT.getText()));
        Prefs.set("sliteanalysis.cDETECTIONVARIANCE", Float.parseFloat(detvar_TXT.getText()));

        Prefs.set("sliteanalysis.cUSEKALMAN",useKalman_CBX.isSelected());

        Prefs.savePreferences(); // Throws error probabely because the ij is not initialized
    }

    // constructor for invoked UI (not most elegant way but I have no time...)
    public UI_Segmentator(String argv){
        pack();
        this.invoked = true;
        ImagePlus open_imp = WindowManager.getCurrentImage();
        setSize(850,500);
        setTitle("AAP segmentation");
        setContentPane(rootPanel);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // preprocessing image
        if(open_imp.getImageStackSize() > 1){
            imp = AApSegmetator.getAverageIm(open_imp);
        }
        else{
            imp = open_imp;
        }

        imcon = new ImageIcon();
        imcon.setImage(imp.getImage());
        _segmentator = new AApSegmetator(imp);
        previewBox.setIcon(imcon);

        // findmaxima parameter change
        fmaxtol_SLD.addChangeListener(this);

        // change in cell size parameter (Cell Magic Wand parameter)
        cmw_SLD.addChangeListener(this);

        setVisible(true);
        previewBRButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updatePreview();
            }
        });
        brwmdl_BTN.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                _mdl_dialog = new OpenDialog("Choose classification model...", "");
                if(_mdl_dialog.getFileName().contains(".model")){
                    calssipath_LBL.setText(_mdl_dialog.getDirectory());
                    logTextField.setText("Classification model loaded!");
                }
                else{
                    logTextField.setText("WARNING::Selected file is not a classification model!");
                    _mdl_dialog = null;
                }
            }
        });
    }

    public void stateChanged(ChangeEvent e){
        _segmentator.setCMWMax(cmw_SLD.getValue());
        _segmentator.setFmTolerance(fmaxtol_SLD.getValue());
        BKG_FOREGROUNDBIAS = gc_fg_SLD.getValue()/1000F;
        BKG_SMOOTH = gc_sm_SLD.getValue()/100F;
        updatePreview();
    }

    private void updatePreview(){
        ArrayList<PolygonRoi> rois = _segmentator.SegmentCellsCWT_engine(imp);
        bkg_imp = new ImagePlus();
        ImagePlus imp_bkg_overlay = new ImagePlus();
        if(rmvBkg_CHBX.isSelected()){
            Graph_Cut bkg_rmvr = new Graph_Cut();
            bkg_imp = bkg_rmvr.processSingleChannelImage(imp, null, BKG_FOREGROUNDBIAS,BKG_SMOOTH,500);
            Converter cnv = new Converter();
            cnv.run("8-bit");
            bkg_imp.getBitDepth();
//            System.out.print(bkg_imp.getBitDepth());

            // change background image LUT and set it as overlay
            imp_bkg_overlay = setImageOverlay(imp,bkg_imp,BKG_OPACITY);
        }

        ij.gui.Overlay overlay = imp_bkg_overlay.getOverlay();
        overlay = (overlay == null) ? new ij.gui.Overlay() : overlay;
        for(int i = rois.size()-1 ; i > -1; i--){
            //filter rois in background
            if(bkg_imp.getProcessor() != null) {
                double[] roi_center = rois.get(i).getContourCentroid();
                if (bkg_imp.getProcessor().getPixel((int)roi_center[0],(int)roi_center[1]) != 0) {
                    rois.remove(i);
                    continue;
                }
            }
            overlay.add(rois.get(i));
            }
        overlay.setLabelColor(Color.WHITE);
        overlay.setStrokeColor(Color.getHSBColor((float) 0.1666,(float) 0.651,(float) 1));
        imp.setOverlay(overlay);

        if(preview_CHBX.isSelected()){
            imcon.setImage(imp.flatten().getImage());
            previewBox.updateUI();
        }
    }

//    /**
//     * Custom canvas to deal with zooming an panning.
//     *
//     * (shamelessly stolen from the Trainable_Segmentation plugin)
//     */
//    @SuppressWarnings("serial")
//    private class CustomCanvas extends OverlayedImageCanvas {
//        CustomCanvas(ImagePlus imp) {
//            super(imp);
//            Dimension dim = new Dimension(Math.min(512, imp.getWidth()), Math.min(512, imp.getHeight()));
//            setMinimumSize(dim);
//            setSize(dim.width, dim.height);
//            setDstDimensions(dim.width, dim.height);
//            addKeyListener(new KeyAdapter() {
//                public void keyReleased(KeyEvent ke) {
//                    repaint();
//                }
//            });
//        }


    /**
     * Modified from Graph_cuts. Original "Toggle between overlay and original image"
     */
    public static ImagePlus setImageOverlay(ImagePlus display_imp, ImagePlus overlay_imp, Double opacity) {
            if(opacity == null){
                opacity = 0.2;
            }
            ImageProcessor overlay_ip = overlay_imp.getProcessor().duplicate();
            overlay_ip.setLut(LUT.createLutFromColor(Color.CYAN));
            ImageRoi imageroi = new ImageRoi(0,0,overlay_ip);
            imageroi.setZeroTransparent(true);
            imageroi.setOpacity(opacity);
            ij.gui.Overlay overlay = new ij.gui.Overlay(imageroi);
            display_imp.setOverlay(overlay);
            return display_imp;
    }

    private void run(){
        // checks

        //  check if there is an image
        imp = WindowManager.getCurrentImage();
        if (IJ.versionLessThan("1.40c") || imp ==null) {
            return;
        }

        IJ.hideProcessStackDialog = true;

        UI_Segmentator uis = new UI_Segmentator();
    }

    public int setup(ImagePlus imp) {
        this.imp = imp;
        return 1;
    }

    public static void main(String[] args) throws Throwable {
        // open sample image
        String path = System.getProperty("user.dir") + "\\data";
        System.out.print(path + '\n');
        ImagePlus imp = IJ.openImage(path + "\\noam\\ca_av_1.tif"); // DEBUB

        imp.show();

        UI_Segmentator uis = new UI_Segmentator();
    }
}
