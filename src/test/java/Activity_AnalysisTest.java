import ij.IJ;
import ij.ImagePlus;
import ij.blob.Blob;
import ij.blob.ManyBlobs;
import org.junit.*;
import sliteanalysis.Activity_Analysis;
import sliteanalysis.MyBlobFeature;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertEquals;

public class Activity_AnalysisTest {

    @org.junit.Test
    public void testSpecialBlobFeature() throws NoSuchMethodException {
        Activity_Analysis activity_an = new Activity_Analysis();
        ImagePlus ip =  IJ.openImage("C:\\Users\\noambox\\Desktop\\Test Images - ImageJ\\3blobs.tif");
//        URL url = this.getClass().getClassLoader().getResource("3blobs.tif");
//        ImagePlus ip = new ImagePlus(url.getPath());
        ManyBlobs mb = new ManyBlobs(ip);
        mb.findConnectedComponents();
        MyBlobFeature test = new MyBlobFeature();
        Blob.addCustomFeature(test);
        int a = 10;
        float c = 1.5f;
        ManyBlobs filtered = mb.filterBlobs(0,10, "LocationFeature",ip.getWidth(),ip.getHeight());
        assertEquals(0, filtered.size()); //All blobs have a greater distance. So it should be 0

    }
}