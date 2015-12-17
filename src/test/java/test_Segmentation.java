import ij.IJ;
import ij.ImagePlus;
import ij.plugin.filter.MaximumFinder;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

/**
 * Created by noambox on 12/7/2015.
 */
public class test_Segmentation {

        @org.junit.Test
        public void testBinary(){
            ImagePlus imp =  IJ.openImage("C:\\Users\\noambox\\Desktop\\AAP plugin\\ClassifierTrain\\151206TRAIN\\av1.tif");
            MaximumFinder maximumFinder = new MaximumFinder();
            ByteProcessor maxmask = maximumFinder.findMaxima(imp.getProcessor(), 8, ImageProcessor.NO_THRESHOLD, 1, true, false);
            ImagePlus max_imp = new ImagePlus();
            maxmask.dilate(3,255);
            max_imp.setProcessor(maxmask);

            max_imp.show();
            IJ.run("In [+]", "");
            IJ.run("In [+]", "");
        }

}
