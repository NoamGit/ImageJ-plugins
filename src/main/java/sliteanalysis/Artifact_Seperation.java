package sliteanalysis;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.plugin.ImageCalculator;
import ij.plugin.PlugIn;
import java.io.FileNotFoundException;

/**
 * Created by noambox on 3/1/2016.
 * assuming baseline for the first 'last_bl_sample' samples
 */

public class Artifact_Seperation implements PlugIn {

    /* Class Variabels*/
    ImagePlus imp;

    /* Methods */
    public void run(String argv) {
        ImagePlus imp = WindowManager.getCurrentImage();
        if(imp == null) {
            IJ.noImage();
        } else {
            int numSlice =imp.getStackSize();
            if(numSlice == 1) {
                IJ.error("Must call this plugin on image stack.");
            }
            // run UI for picking starting slice
            String sPrompt = "Enter noise variance Threshold:";
            int nv_thres = (int)IJ.getNumber(sPrompt, 1.0D);
            if(nv_thres != -2147483648) {
                        int last_bl_sample = nv_thres; //get from user or find automatically
                        ImageStack stack = imp.getStack();

                        // create output images
                        ImageStack sig = new ImageStack(imp.getWidth(),imp.getHeight());
                        ImageStack arti = new ImageStack(imp.getWidth(),imp.getHeight());
                        ImageStack out = new ImageStack(imp.getWidth(),imp.getHeight());

                        // run local method
                        ImageCalculator im_calc = new ImageCalculator();
                        ImagePlus current_S;
                        ImagePlus next_S;
                        current_S = new ImagePlus("Current",stack.getProcessor(last_bl_sample));
                        for (int i = last_bl_sample+1; i <= numSlice; i++) {
                            next_S = new ImagePlus("Next",stack.getProcessor(i));
                            ImagePlus diff_S = im_calc.run("create subtract", next_S,current_S );

                            // for Yoni
                            current_S = next_S;
                            out.addSlice(diff_S.getProcessor());

//                            arti.addSlice(diff_S.getProcessor());
//                            current_S = im_calc.run("create subtract", next_S,diff_S);
//                            sig.addSlice(current_S.getProcessor());
                        }

                        // for Yoni
                        ImagePlus im_out = new ImagePlus("signal",out);
                        im_out.show();

//                        ImagePlus sig_out = new ImagePlus("signal",sig);
//                        sig_out.show();
//
//                        ImagePlus arti_out = new ImagePlus("artifact",arti);
//                        arti_out.show();
                } else {
                    IJ.error("No threshold was specified!");
                }
            }
        }

    public static void main(final String... args) throws FileNotFoundException {
        Artifact_Seperation artifact_seperation = new Artifact_Seperation();
        String path = "C:\\";
        try {
//            path = "D:\\# Projects (Noam)\\# SLITE\\# DATA\\AAP 1.1.0\\"; // LAB
            ImagePlus imp_test = IJ.openImage(path + "Stack_with_noam.tif");
            if(imp_test == null){
                throw new FileNotFoundException("Your not in Lab....");
            }
        }
        catch(FileNotFoundException error){
            System.out.print("Your not in lab change... path!");
        }

        // For image (stack) analysis
        artifact_seperation.imp = IJ.openImage(path + "Stack_with_noam.tif");
        // artifact_seperation.imp = IJ.openImage(path + "for_naive_demixing_2.tif"); // DEBUG

        String argv = "";
        artifact_seperation.imp.show();
        artifact_seperation.run(argv);
    }
}
