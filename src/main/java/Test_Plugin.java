import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

public class Test_Plugin implements PlugInFilter {
    private ImagePlus imp;

    /* Class Variabels*/


    /* Methods */
/*    public void run(String argv) {
        IJ.showMessage("Plugin works...");
    }*/

    public static void main(final String... args) {
    }

    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;
        return 0;
    }

    public void run(ImageProcessor ip) {
        IJ.showMessage("Plugin works...");
    }
}



