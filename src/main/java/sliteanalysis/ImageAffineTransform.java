package sliteanalysis;

import ij.ImagePlus;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import ij.ImageStack;
import ij.IJ;
import ij.gui.ImageWindow;
import ij.process.ShortProcessor;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 *
 * @author nico
 */

public class ImageAffineTransform {

    /**
     *
     * @param input
     * @param af
     * @param interpolationType - AffineTransformOp.Type_Bilinear, or
     * AffineTransformOp.Type_Bicubic AffineTransformOp.Type_Nearest_Neighbor if
     * an invalid number is supplied, Nearest_neighbor will be used
     * @return transform image
     */
    public static BufferedImage transform(BufferedImage input, AffineTransform af,
                                          int interpolationType) {
        if (interpolationType != AffineTransformOp.TYPE_BICUBIC
                && interpolationType != AffineTransformOp.TYPE_BILINEAR
                && interpolationType != AffineTransformOp.TYPE_NEAREST_NEIGHBOR) {
            interpolationType = AffineTransformOp.TYPE_NEAREST_NEIGHBOR;
        }
        AffineTransformOp aOp = new AffineTransformOp(af, interpolationType);
        return aOp.filter(input, null);
    }


   /*
    * Beanshell script to test transformImagePlus:
    *

import java.awt.image.AffineTransformOp;

dc = edu.valelab.GaussianFit.DataCollectionForm.getInstance();
af = dc.getAffineTransform().clone();
af.invert();
siPlus = ij.IJ.getImage();
type = AffineTransformOp.TYPE_NEAREST_NEIGHBOR;
edu.valelab.GaussianFit.utils.ImageAffineTransform.transformImagePlus(siPlus, af, type);

    */


    /**
     * Given an input image and affine transform, will apply the affine transform
     * to the second channel and create a new window with the untouched
     * first channel and transformed second channel
     * For now only works on 16 bit (short) images, and 2-channel images (which
     * may contain multiple frames and slices)
     *
     * @param siPlus - input image
     * @param af - affine transform that will be applied
     * @param interpolationType - valid AffinTRansformOp type
     */
    public static void transformImagePlus(ImagePlus siPlus, AffineTransform af,
                                          int interpolationType) {

        if (siPlus.getNChannels() == 2) {

            if (interpolationType != AffineTransformOp.TYPE_BICUBIC
                    && interpolationType != AffineTransformOp.TYPE_BILINEAR
                    && interpolationType != AffineTransformOp.TYPE_NEAREST_NEIGHBOR) {
                interpolationType = AffineTransformOp.TYPE_NEAREST_NEIGHBOR;
            }

            // Part of work-around bug in AffineTransformOp
            Object rh = RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
            if (interpolationType == AffineTransformOp.TYPE_BICUBIC) {
                rh = RenderingHints.VALUE_INTERPOLATION_BICUBIC;
            }
            if (interpolationType == AffineTransformOp.TYPE_BILINEAR) {
                rh = RenderingHints.VALUE_INTERPOLATION_BILINEAR;
            }

            AffineTransformOp aOp = new AffineTransformOp(af, interpolationType);

            ImageStack stack = siPlus.getStack();
            if (stack.getProcessor(1).getBitDepth() > 8) {

                // first get the final width and height
                ShortProcessor testProc = (ShortProcessor) stack.getProcessor(2);
                BufferedImage bi = testProc.getBufferedImage();
                BufferedImage afBi = aOp.filter(bi, null);

                // take the minimum of the original and transformed images
                int width = Math.min(testProc.getWidth(), afBi.getWidth());
                int height = Math.min(testProc.getHeight(), afBi.getHeight());

                // Create the destination Window
                ImagePlus dest = IJ.createHyperStack(siPlus.getTitle() + "aligned",
                        width, height, siPlus.getNChannels(),
                        siPlus.getNSlices(), siPlus.getNFrames(), siPlus.getBitDepth());
                dest.copyScale(siPlus);

                ImageStack destStack = dest.getStack();
                BufferedImage aOpResult = new BufferedImage(afBi.getWidth(),
                        afBi.getHeight(), BufferedImage.TYPE_USHORT_GRAY);

                for (int i = 1; i <= stack.getSize(); i++) {
                    ShortProcessor proc = (ShortProcessor) stack.getProcessor(i);
                    ShortProcessor destProc;
                    if (i % 2 == 0) { // do the affine transformed
                        bi = proc.get16BitBufferedImage();
                        if (interpolationType ==
                                AffineTransformOp.TYPE_NEAREST_NEIGHBOR) {
                            BufferedImage bres = aOp.filter(bi, aOpResult);
                        } else {
                            // work around bug in AffineTransformationOp, see:
                            // http://stackoverflow.com/questions/2428109/java-error-on-bilinear-interpolation-of-16-bit-data
                            Graphics2D g = aOpResult.createGraphics();
                            g.transform(af);
                            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, rh);
                            g.drawImage(bi, null, 0, 0);
                        }
                        ImagePlus p = new ImagePlus("" + i, aOpResult);
                        destProc = (ShortProcessor) p.getProcessor();

                    } else {
                        destProc = (ShortProcessor) proc.duplicate();
                    }
                    destProc.setRoi(0, 0, width, height);
                    destProc = (ShortProcessor) destProc.crop();
                    destStack.setPixels(destProc.getPixels(), i);

                    // The following is weird, but was needed to get the first frame
                    // of the first channel to display.  Remove when solved in ImageJ
                    if (i == 1)
                        dest.setProcessor(destProc);
                }

                ImageWindow win = new ij.gui.StackWindow(dest);

            } else {
                ij.IJ.showMessage("This only works with 16 bit images");
            }
        } else {
            ij.IJ.showMessage("This only works with a 2 channel image");
        }

    }
}
