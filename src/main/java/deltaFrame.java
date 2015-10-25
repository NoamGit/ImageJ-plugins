/**
 * Created by Noam on 10/19/2015.
 */
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.io.FileSaver;
import ij.io.Opener;
import ij.plugin.ImageCalculator;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import ij.util.StringSorter;
import sun.plugin.javascript.navig.ImageArray;
import weka.core.AlgVector;


public class deltaFrame implements PlugIn {

    /* Class Variabels*/
    ImagePlus imp;
    ImagePlus returnImp = new ImagePlus();
    int default_amplitude = 1;

    /* Methods */
    public void run(String argv) {
        // validate conditions
        if (imp.getStackSize()==1)
        {IJ.error("Stack required"); return;}

        // run gui and Load images
        GenericDialog gd = new GenericDialog("DeltaSlice settings");
        gd.addNumericField("Gain:",default_amplitude,2);
        this.default_amplitude = (int) gd.getNextNumber();
        gd.showDialog();
        if (gd.wasCanceled()) {
            IJ.error("PlugIn canceled!");
            return;
        }

        // run local method
        returnImp = imp.duplicate();
        delta_frame();

    }

    private void delta_frame() {
        ImageStack returnStack = returnImp.createEmptyStack();
        ImageStack imp_stack = this.imp.getStack();
        Object[] imageArray = imp_stack.getImageArray();
        Object[] outputArray = new Object[imp_stack.getSize()];
        outputArray[0] = subtractByte((byte[]) imageArray[0], (byte[]) imageArray[1],this.default_amplitude);

        // initialize first and second frame in the deltaOutput and bar
        IJ.showProgress(0,imp_stack.getSize());
        returnStack.addSlice(String.valueOf(0), outputArray[0]);
        returnStack.addSlice(String.valueOf(1), outputArray[0]);

        // loop
        for (int k = 1; k < imp_stack.getSize()-1; k++) {
            IJ.showProgress(k,imp_stack.getSize());
            outputArray[k] = subtractByte((byte[]) imageArray[k], (byte[]) imageArray[k+1],this.default_amplitude);
            returnStack.addSlice(String.valueOf(k + 1), outputArray[k]);
        }
        returnImp.setStack(returnStack);
        returnImp.show();
    }

    public static Object subtractByte(byte[] a, byte[] b, int gain){
        byte[] c = new byte[a.length];
        for(int k = 0; k <a.length; k++){
            int a_int = 0xff & a[k];
            int b_int = 0xff & b[k];
            c[k] = (byte) (Math.multiplyExact(Math.abs(a_int-b_int), gain) & 0xff);
            if(c[k] >  (byte) 127){ // > (int) 255
                c[k] = (byte) 127;
            }
        }
        return c;
    }

//    public static void main(final String... args) {
//        deltaFrame df = new deltaFrame();
////        String path = "C:\\Users\\noambox\\Dropbox\\# Graduate studies M.Sc\\# SLITE\\ij - plugin data\\";
//        String path = "C:\\Users\\Noam\\Dropbox\\# graduate studies m.sc\\# SLITE\\ij - plugin data\\";
////        ImagePlus imp = IJ.openImage(path+"FLASH_20msON_10Hz_SLITE_1.tif"); // DEBUG
//        df.imp = IJ.openImage(path + "FLASH_20msON_20Hz_SLITE_1.tif"); // DEBUG
//        String argv = "";
//        df.run(argv);
//    }
}


