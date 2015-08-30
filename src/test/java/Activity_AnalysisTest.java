import ij.ImagePlus;
import ij.blob.Blob;
import ij.blob.ManyBlobs;

import java.net.URL;

import static org.junit.Assert.assertEquals;
/**
 * Created by noambox on 8/27/2015.
 */
public class Activity_AnalysisTest {

    @org.junit.Test
    public void testSpecialBlobFeature() throws NoSuchMethodException {
        URL url = this.getClass().getClassLoader().getResource("3blobs.tif");
        ImagePlus ip = new ImagePlus(url.getPath());
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