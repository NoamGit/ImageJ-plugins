import graphcut.Graph_Cut;
import ij.IJ;
import ij.ImagePlus;
import ij.LookUpTable;
import ij.gui.ImageRoi;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.plugin.Converter;
import ij.plugin.LUT_Editor;
import ij.plugin.LutLoader;
import ij.process.ImageProcessor;
import ij.process.LUT;
import net.imagej.display.ColorTables;
import net.imagej.lut.LUTSelector;
import net.imagej.lut.LUTService;
import net.imagej.lut.io.LUTIOPlugin;
import sliteanalysis.AApSegmetator;
import sliteanalysis.UI_Segmentator;

import java.awt.*;
import java.awt.image.ColorModel;
import java.util.ArrayList;

/**
 * Created by noambox on 13/02/2017.
 */
public class Main {
    public static void main(String[] args){

        String path = System.getProperty("user.dir") + "\\data";
        ImagePlus imp = IJ.openImage(path + "\\noam\\ca_av_1.tif"); // DEBUB

        // try converting
        Converter cnv = new Converter();
        imp.show();
        cnv.run("8-bit");
        int bits = imp.getBitDepth();
        System.out.print(bits);

        // try the Graph cuts
        Graph_Cut bkg_rmvr = new Graph_Cut();

        // continue with the background elimination
        ImagePlus bkg_imp = bkg_rmvr.processSingleChannelImage(imp, null, 0.84F,2,500);

        // try changing LUT
        bkg_imp.setLut(LUT.createLutFromColor(Color.CYAN));

        //merge overlay with ROIs
        AApSegmetator seg = new AApSegmetator(imp);
        ArrayList<PolygonRoi> rois = seg.SegmentCellsCWT_engine(imp);

        //setting background to original image
        imp = UI_Segmentator.setImageOverlay(imp,bkg_imp,null);
        Overlay overlay = imp.getOverlay();
        for(int i = 0; i <rois.size(); i++){
            //check if ROI is in background
            double[] roi_center = rois.get(i).getContourCentroid();
            System.out.print(bkg_imp.getProcessor().getPixel((int)roi_center[0],(int)roi_center[1]));
            overlay.add(rois.get(i));
        }
        overlay.setLabelColor(Color.WHITE);
        overlay.setStrokeColor(Color.getHSBColor((float) 0.1666,(float) 0.651,(float) 1));
        imp.setOverlay(overlay);

        imp.show();



    }
}
