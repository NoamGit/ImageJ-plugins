package sliteanalysis;

import Jama.Matrix;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ErrorMsg;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.gui.*;
import ij.measure.Calibration;
import ij.plugin.RoiEnlarger;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.MaximumFinder;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import imagescience.feature.Statistics;
import mpicbg.models.*;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.ml.distance.DistanceMeasure;
import vib.app.ImageMetaData;
import weka.core.DistanceFunction;

import java.awt.*;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.reflect.Array;
import java.util.*;

/**
 * Created by noambox on 12/7/2015.
 */

public class Stimulus_API {
    /* Members */
    private ArrayList<Roi> rois = new ArrayList<>();
    private ImagePlus imp;
    private double dt = 1/60D; // 60 Hz default movie sr
    private double EXTRACTSTIM_THRES;
    private AffineTransform aft = new AffineTransform();
    private int ENLARGEROI;

    /* Methods */
    /**
     * @param e_r enlarge rois in e_r pixels*/
    public int setup(String arg, ImagePlus t_imp, ArrayList<PolygonRoi> t_roi, int e_r) {
        this.imp = t_imp;
        this.ENLARGEROI = e_r;
        for(int i=0;i<t_roi.size();i++){
            this.rois.add(t_roi.get(i));;
        }
        return 1;
    }

    /* set dt */
    public void setDt(double t_dt){
        this.dt = t_dt;
    }

    /* Affine transforms an Image and displays */
    public static ImagePlus AffineTransformIm(ImagePlus tImp, AffineTransform tAfft){
        ImagePlus imp_temp_1 = new ImagePlus();
        imp_temp_1.setImage(tImp);
        AffineTransformOp aTOp = new AffineTransformOp(tAfft,AffineTransformOp.TYPE_BICUBIC);
        BufferedImage aTOpResult = new BufferedImage(tImp.getWidth(), tImp.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        aTOp.filter(tImp.getBufferedImage(), aTOpResult);
        ImagePlus out = new ImagePlus("transformed image", aTOpResult);
        out.show();
        IJ.run("In [+]", "");
        return out;
    }
    /* Loads the transformation file obtained by ImageJ*/
    public void loadTransformation(final String file) {

        // Read lines:
        final Vector<String> lines = new Vector<String>();
        String line = null;
        try {
            final BufferedReader br = new BufferedReader(new FileReader(file));
            line = br.readLine();
            while (line != null) {
                line = line.trim();
                if (!line.equals(""))
                    lines.add(line);
                line = br.readLine();
            }
            br.close();
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Unable to find "+file);
        } catch (Throwable e) {
            throw new IllegalArgumentException("Error reading from "+file);
        }

        // Convert lines:
        if (lines.size() != 4)
            throw new IllegalArgumentException("File "+file+" does not contain a 4 x 4 matrix");
        String delim = "\t";
        line = lines.get(0);
        if (line.indexOf(",") >= 0) delim = ",";
        else if (line.indexOf(" ") >= 0) delim = " ";
        final double[][] matrix = new double[4][4];
        for (int r=0; r<4; ++r) {
            line = lines.get(r);
            final StringTokenizer st = new StringTokenizer(line,delim);
            if (st.countTokens() != 4)
                throw new IllegalArgumentException("File "+file+" does not contain a 4 x 4 matrix");
            for (int c=0; c<4; ++c) {
                try {
                    matrix[r][c] = Double.parseDouble(st.nextToken());
                } catch (Throwable e) {
                    throw new IllegalArgumentException("Error reading element ("+r+","+c+") in "+file);
                }
            }
        }

        /*
         *      [ x']   [  m00  m01  m02  ] [ x ]   [ m00x + m01y + m02 ]
         *      [ y'] = [  m10  m11  m12  ] [ y ] = [ m10x + m11y + m12 ]
         *      [ 1 ]   [   0    0    1   ] [ 1 ]   [         1         ]
         */
        aft.setTransform(matrix[1][1], matrix[2][1], matrix[1][2], matrix[2][2], matrix[1][3], matrix[3][2]);
    }

    /**
     * @param image_bounds {image_width, image_height}
     * Transforms all rois according to transformation matrix */
    public static ArrayList<Roi> TransformRois( ArrayList<Roi> rois_in, AffineTransform atf, int enlarge_factor, int[] image_bounds){
        ArrayList<Roi> rois_out = new ArrayList<>(rois_in.size());
        AffineTransform aT = new AffineTransform(atf);
        for (int i = 0; i < rois_in.size(); i++) {
            FloatPolygon floatp = rois_in.get(i).getFloatPolygon();
            Point2D[] roi_coord_src = float2PointArray(floatp.xpoints, floatp.ypoints);
            Point2D[] roi_coord_dest = new Point2D[roi_coord_src.length];
            aT.transform(roi_coord_src, 0, roi_coord_dest, 0, roi_coord_dest.length);
            RealMatrix xy_dest = PointArr2float(roi_coord_dest);
            float[] x_float = CalciumSignal.DoubletoFloat(xy_dest.getColumn(0));
            float[] y_float = CalciumSignal.DoubletoFloat(xy_dest.getColumn(1));
            for (int j = 0; j < x_float.length; j++) {
                x_float[j] = x_float[j] < 0 ? 0 : x_float[j];
                x_float[j] = x_float[j] > image_bounds[0] ? image_bounds[0] : x_float[j];
                y_float[j] = y_float[j] < 0 ? 0 : y_float[j];
                y_float[j] = y_float[j] > image_bounds[1] ? image_bounds[1] : y_float[j];
            }
            PolygonRoi t_roi = new PolygonRoi(x_float,y_float,Roi.FREEROI);
            rois_out.add(i, RoiEnlarger.enlarge(t_roi, enlarge_factor));
        }
        return rois_out;
    }

    public static ArrayList<PolygonRoi> TransformPolygonRois( ArrayList<PolygonRoi> rois_in, AffineTransform atf, int enlarge_factor, int[] image_bounds){
        ArrayList<PolygonRoi> rois_out = new ArrayList<>(rois_in.size());
        AffineTransform aT = new AffineTransform(atf);
        for (int i = 0; i < rois_in.size(); i++) {
            FloatPolygon floatp = rois_in.get(i).getFloatPolygon();
            Point2D[] roi_coord_src = float2PointArray(floatp.xpoints, floatp.ypoints);
            Point2D[] roi_coord_dest = new Point2D[roi_coord_src.length];
            aT.transform(roi_coord_src, 0, roi_coord_dest, 0, roi_coord_dest.length);
            RealMatrix xy_dest = PointArr2float(roi_coord_dest);
            float[] x_float = CalciumSignal.DoubletoFloat(xy_dest.getColumn(0));
            float[] y_float = CalciumSignal.DoubletoFloat(xy_dest.getColumn(1));
            for (int j = 0; j < x_float.length; j++) {
                x_float[j] = x_float[j] < 0 ? 0 : x_float[j];
                x_float[j] = x_float[j] > image_bounds[0] ? image_bounds[0] : x_float[j];
                y_float[j] = y_float[j] < 0 ? 0 : y_float[j];
                y_float[j] = y_float[j] > image_bounds[1] ? image_bounds[1] : y_float[j];
            }
            PolygonRoi t_roi = new PolygonRoi(x_float,y_float,Roi.FREEROI);
            rois_out.add(i, (PolygonRoi) RoiEnlarger.enlarge(t_roi, enlarge_factor));
        }
        return rois_out;
    }

//    public static ArrayList<PolygonRoi> TransformPolygonRois( ArrayList<PolygonRoi> rois_in, AffineTransform atf, int enlarge_factor, int[] image_bounds){
//        ArrayList<PolygonRoi> rois_out = new ArrayList<>(rois_in.size());
//        AffineTransform aT = new AffineTransform(atf);
//        for (int i = 0; i < rois_in.size(); i++) {
//            FloatPolygon floatp = rois_in.get(i).getFloatPolygon();
//            Point2D[] roi_coord_src = float2PointArray(floatp.xpoints, floatp.ypoints);
//            Point2D[] roi_coord_dest = new Point2D[roi_coord_src.length];
//            aT.transform(roi_coord_src, 0, roi_coord_dest, 0, roi_coord_dest.length);
//            RealMatrix xy_dest = PointArr2float(roi_coord_dest);
//            float[] x_float = CalciumSignal.DoubletoFloat(xy_dest.getColumn(0));
//            float[] y_float = CalciumSignal.DoubletoFloat(xy_dest.getColumn(1));
//            for (int j = 0; j < x_float.length; j++) {
//                x_float[j] = x_float[j] < 0 ? 0 : x_float[j];
//                x_float[j] = x_float[j] > image_bounds[0] ? image_bounds[0] : x_float[j];
//                y_float[j] = y_float[j] < 0 ? 0 : y_float[j];
//                y_float[j] = y_float[j] > image_bounds[1] ? image_bounds[1] : y_float[j];
//            }
//            PolygonRoi t_roi = new PolygonRoi(x_float,y_float,Roi.FREEROI);
//            rois_out.set(i, (PolygonRoi) RoiEnlarger.enlarge(t_roi,enlarge_factor));
//        }
//        return rois_out;
//    }

    // TODO:  use Time Analyser instead
    /**
     * @param rois input rois
     * @param imp average image */
    public static void fitRois( ArrayList<PolygonRoi> rois, ImagePlus imp ) throws NotEnoughDataPointsException, IllDefinedDataPointsException {
        // find maxima in image as data set (ds)
        int tolarance = 5;
        MaximumFinder mf = new MaximumFinder();
        Polygon imp_mp = mf.getMaxima(imp.getProcessor(),tolarance, true); // imp max points
        int max_points = imp_mp.xpoints.length > 15 ? 15 : imp_mp.ypoints.length;
        PointRoi point_roi = new PointRoi(imp_mp.xpoints, imp_mp.ypoints, max_points);

        // compare ds to existing centers and extract best correspondences
        ArrayList<Point2D> corresp_point = new ArrayList();
        double dist = 0;
        int closest = 0;
        double shortest_length = 0;
        for (int i = 0; i <point_roi.getXCoordinates().length; i++) {
            for (int j = 0; j < rois.size(); j++) {
                int[] roi_centroid = findCentroid(rois.get(j));
                dist = Point2D.distance(imp_mp.xpoints[i], imp_mp.ypoints[i], roi_centroid[0],roi_centroid[1]);
                if(j == 0) {
                    closest = j;
                    shortest_length = dist;
                }
                else{
                    if(dist < shortest_length){
                        closest = j;
                        shortest_length = dist;
                    }
                }
            }
            int[] near_neighb = findCentroid(rois.get(closest));
            corresp_point.add(new Point2D.Double(near_neighb[0],near_neighb[1]));
        }

        // find transformations
        double[] corr_dest;
        double[] corr_source;
        ArrayList<double[]> destArray = new ArrayList<>();
        ArrayList<double[]> sourceArray = new ArrayList<>();
        ArrayList matches = new ArrayList();
        for(int mapping = 0; mapping < corresp_point.size(); ++mapping) {
            corr_dest = new double[]{(double)imp_mp.xpoints[mapping], (double)imp_mp.ypoints[mapping]};
            corr_source = new double[]{corresp_point.get(mapping).getX(), corresp_point.get(mapping).getY()};
            matches.add(new PointMatch(new mpicbg.models.Point(corr_source), new mpicbg.models.Point(corr_dest)));
            destArray.add(corr_dest);
            sourceArray.add(corr_source);
        }
        SimilarityModel2D afm = new SimilarityModel2D();
        try {
            (afm).fit(matches);
            System.out.printf("\nfit-transformation cost - %.3f MSE",findCost(destArray, sourceArray, afm));
            if(findCost(destArray, sourceArray, afm) > 5){
                System.out.print("\n!WARNING! Transformation had high MSE! ");
            }
        } catch (NotEnoughDataPointsException e) {
            e.printStackTrace();
        }
        double[] trans_matrix = new double[6];
        (afm).toArray(trans_matrix);

        // apply on all Roi's
        int enlarge_factor = 0;
        int[] image_bounds = {imp.getWidth(), imp.getHeight()};
        AffineTransform aft = new AffineTransform();
        aft.setTransform(trans_matrix[0], trans_matrix[1], trans_matrix[2], trans_matrix[3], trans_matrix[4], trans_matrix[5]);
        ArrayList<PolygonRoi> roi_trans = Stimulus_API.TransformPolygonRois(rois, aft, enlarge_factor, image_bounds);
        for (int i = 0; i < rois.size(); i++) {
            String name = "Cell_"+(i+1)+"_"+(int)rois.get(i).getXBase()+"_"+(int)rois.get(i).getYBase();
            rois.set(i, roi_trans.get(i));
            rois.get(i).setName(name);
        }
    }

    static double findCost(ArrayList<double[]> dest,ArrayList<double[]> source, SimilarityModel2D model){
        double MSE = 0;
        for (int i = 0; i < dest.size(); i++) {
            double[] xy_dest = model.apply(source.get(i));
            double[] xy_src = source.get(i);
            MSE += Math.sqrt(Math.pow((xy_src[0] - xy_dest[0]), 2) + Math.pow((xy_src[1]-xy_dest[1]), 2));
        }
        return MSE/dest.size();
    }

    // define functions
    static int[] findCentroid(Roi roi){
        Polygon roi_poly = roi.getPolygon();
        double x_cent =0 ,y_cent = 0;
        int x_cent_size = roi_poly.xpoints.length;
        for (int j = 0; j < x_cent_size; j++) {
            x_cent += roi_poly.xpoints[j];
            y_cent += roi_poly.ypoints[j];
        }
        int[] out = {(int)(x_cent/x_cent_size), (int)(y_cent/x_cent_size)};
        return out;
    }

    /**
     * @param r_in input rois
     * @param avr_imp average image */
    public static ArrayList<PolygonRoi> recenterRois( ArrayList<PolygonRoi> r_in, ImagePlus avr_imp ) {
        // TSA parameters
        double Fs  = Prefs.get("sliteanalysis.cSTIMULUS_FR", AAP_Constants.cSTIMULUS_FR);
        int MaxIteration = (int) Prefs.get("sliteanalysis.cMAXITER_ED", AAP_Constants.cMAXITER_ED);
        double CLimit = Prefs.get("sliteanalysis.cCLIMIT_ED", AAP_Constants.cCLIMIT_ED);
        int Width = (int) Prefs.get("sliteanalysis.cWIDTH_ED", AAP_Constants.cWIDTH_ED); //width and height of a ROI
        int Height = (int) Prefs.get("sliteanalysis.cHEIGHT_ED", AAP_Constants.cHEIGHT_ED);
        double Scale = Prefs.get("sliteanalysis.cSCALE_ED", AAP_Constants.cSCALE_ED);
    /*    int MaxIteration = 15; //default value
        double CLimit = 0.1; // default value
        int Width = 10; //width and height of a ROI
        int Height = 10;
        double Scale = 0.5;*/
        ShapeRoi all = new ShapeRoi(new OvalRoi(0, 0, Width, Height));

        ArrayList<PolygonRoi> r_out = new ArrayList<>(r_in.size());
        Roi[] roi_out2 = new Roi[0];
        if (avr_imp != null) {
            Roi[] rois = r_in.toArray(new Roi[r_in.size()]);
            if (rois.length == 0) {
                IJ.showMessage("ROI file is empty...");
                return null;
            }

            int CurROIWidth = Width;
            int CurROIHeight = Height;
            int Width_temp = (int) (Width * Scale);
            int Height_temp = (int) (Height * Scale);
            Roi[] roi_out1 = ScaleROIS(Width_temp, Height_temp, rois);
            rois = roi_out1;

            ImageStatistics stat = new ImageStatistics();
            ij.measure.Calibration calib = avr_imp.getCalibration();
            double xScale = calib.pixelWidth;
            double yScale = calib.pixelHeight;
            ShapeRoi temp = null;
            boolean Converge = false;
            int New_x = 0;
            int New_y = 0;
            double xMovement = 0, yMovement = 0;
            java.awt.Rectangle Boundary;
            int i;
            for (i = 0; i < rois.length; i++) {
                Roi CurRoi = rois[i];

//                // Resize/Scale ROI (replaces 'ScaleROI')
//                Width = (int) (Width * Scale);
//                Height = (int) (Height * Scale);
//                Rectangle BRect = CurRoi.getBounds();
//                int new_x_scale = Math.round(BRect.x + (BRect.width - Width) / 2);
//                int new_y_scale = Math.round(BRect.y + (BRect.height - Height) / 2);
//                CurRoi.setLocation(new_x_scale, new_y_scale);

//                CurRoi.setName(CurRoi.getName());

                Boundary = CurRoi.getBounds();
                Converge = false;
                avr_imp.setRoi(CurRoi);
                double OldDiff = 0, NewDiff = 0;
                int Old_x, Old_y;
                int Iteration;
                for (Iteration = 1; Iteration <= MaxIteration && !Converge; Iteration++) {
                    stat = avr_imp.getStatistics(64 + 32); //Calculate center of Mass and Centroid;
                    // Calculate movements
                    xMovement = (stat.xCentroid - stat.xCenterOfMass) / xScale;
                    yMovement = (stat.yCentroid - stat.yCenterOfMass) / yScale;
                    if (Math.abs(xMovement) < 1 && xMovement != 0 && yMovement != 0 && Math.abs(yMovement) < 1) { //Now search nearby;
                        if (Math.abs(xMovement) > Math.abs(yMovement)) {
                            New_x = (xMovement > 0) ? (int) Math.round(stat.xCentroid / xScale - (Boundary.getWidth() / 2.0) - 1) : (int) Math.round(stat.xCentroid / xScale - (Boundary.getWidth() / 2.0) + 1);
                            New_y = (int) Math.round(stat.yCentroid / yScale - (Boundary.getHeight() / 2.0));
                        } else {
                            New_y = (yMovement > 0) ? (int) Math.round(stat.yCentroid / yScale - (Boundary.getHeight() / 2.0) - 1) : (int) Math.round(stat.yCentroid / yScale - (Boundary.getHeight() / 2.0) + 1);
                            New_x = (int) Math.round(stat.xCentroid / xScale - (Boundary.getWidth() / 2.0));
                        }
                    } else {
                        New_x = (int) Math.round(((stat.xCenterOfMass / xScale) - (Boundary.getWidth() / 2.0)));
                        New_y = (int) Math.round(((stat.yCenterOfMass / yScale) - (Boundary.getHeight() / 2.0)));

                    }
                    Converge = (Math.abs(xMovement) < CLimit && Math.abs(yMovement) < CLimit) ? true : false;
                    CurRoi.setLocation(New_x, New_y);
                    avr_imp.setRoi(CurRoi);
                }
                temp = new ShapeRoi(CurRoi);
                all = (i == 0) ? new ShapeRoi(CurRoi) : all.xor(temp);
//                }

//                // Resize/Scale ROI back (replaces 'ScaleROIS')
//                int Width_back = CurROIWidth;
//                int Height_back = CurROIHeight;
//                BRect = CurRoi.getBounds();
//                new_x_scale = Math.round(BRect.x + (BRect.width - Width_back) / 2);
//                new_y_scale = Math.round(BRect.y + (BRect.height - Height_back) / 2);
//                CurRoi.setLocation(new_x_scale, new_y_scale);
//                CurRoi.setName(CurRoi.getName());
//                r_out.add((PolygonRoi) CurRoi);

                // Debug

//                temp = new ShapeRoi(CurRoi);
//                all = (i == 0) ? new ShapeRoi(CurRoi) : all.xor(temp);

                       /* if(!Converge)
                                IJ.log(indexes[i] + "\t ROI did not converge" );*/
                       /* else
                               IJ.log(indexes[i] + "\t ROI converged" );*/

            }

            roi_out2 = ScaleROIS(CurROIWidth, CurROIHeight, rois);
            for (int j = 0; j < roi_out2.length; j++) {
                r_out.add((PolygonRoi) roi_out2[j]);
            }
//            System.out.print("\nNum iter - " + Iteration);
        }
//        avr_imp.setRoi(all);
        return r_out;
    }


    public static  Roi[] ScaleROIS(int Width, int Height, Roi[] rois){

        java.awt.Rectangle BRect ;
        Roi[] roiout = new Roi[rois.length];
        Roi CurRoi,tmpRoi;
        int NewX, NewY;
        for(int i = 0 ; i < rois.length ; i++){
            CurRoi = rois[i];
            BRect = CurRoi.getBounds();
            NewX = Math.round(BRect.x + (BRect.width - Width) / 2);
            NewY = Math.round(BRect.y + (BRect.height - Height)/2);
            CurRoi.setLocation(NewX,NewY);
            CurRoi.setName(CurRoi.getName());
            roiout[i] = CurRoi;
        }
        return roiout;
    }


    /**
     *  finds te signals that are the means of the ROIs and stores them as TimeSeries Array
    * with the same order of the ROIs. This function works with rotation of the ROIs
     * @param aft the Affine Transformation matrix for the ROIs
    * */
    public TimeSeriesMatrix FindRoisMeanSignals(AffineTransform aft){
        // for debbuging
//        String path = "D:\\# Projects (Noam)\\# SLITE\\# DATA\\301115Retina - DATA\\Loc 3\\post\\s_r_reg\\";  // REMOVE
//        ImagePlus test_imp = IJ.openImage(path + "niel_source.tif"); // REMOVE

        // Step 0 - Initializations
        ImageStack stack = this.imp.getStack();
        int size = stack.getSize();
        TimeSeriesMatrix tsarray = new TimeSeriesMatrix(rois.size(),size);

        // Step 1 - move all Rois to new coordinates with the transformation
        int[] image_bounds = {this.imp.getWidth(), this.imp.getHeight()};
        this.rois = TransformRois( this.rois, aft, this.ENLARGEROI,image_bounds);

        // Step 2 - for each Roi find the signal from stack
        ImageProcessor ip = this.imp.getProcessor();
        double minThreshold = ip.getMinThreshold();
        double maxThreshold = ip.getMaxThreshold();
        double[] values = new double[size];
        Calibration cal = this.imp.getCalibration();
        //Analyzer analyzer = new Analyzer(imp);
        int measurements = Analyzer.getMeasurements();
        for (int i = 0; i < rois.size(); i++) {
            Roi roi_iter = rois.get(i);

//            test_imp.setRoi(roi_iter); // REMOVE
//            test_imp.show(); // REMOVE

            for (int j=1; j<=size; j++) {
                ip = stack.getProcessor(j);
                if (minThreshold!=ImageProcessor.NO_THRESHOLD)
                    ip.setThreshold(minThreshold,maxThreshold,ImageProcessor.NO_LUT_UPDATE);
                ip.setRoi(roi_iter);
                ImageStatistics stats = ImageStatistics.getStatistics(ip, measurements, cal);
                values[j-1] = (float)stats.mean;
            }
            tsarray.setSignal(i,values); // NaN is out of region ROIs
        }

        // Step 3 - store and return values
        return tsarray;
    }

    /* Extract stimulus times from the signal matrix*/ // TODO needs to be checked
    private ArrayList<ArrayList<Double>> ExtractStimTimes(TimeSeriesMatrix stim_mean_mat) {
        ArrayList<ArrayList<Double>> out_times = new ArrayList<ArrayList<Double>>(stim_mean_mat.getNumSignals());
        for (int i = 0; i < stim_mean_mat.getNumSignals(); i++) {
            Object[] Peaks = Extract_Stimulus.getPeaks(stim_mean_mat.getSignal(i), 0);
            out_times.get(i).addAll(Arrays.<Double>asList(Sample2Time((Double[]) Peaks, this.dt)));
        }

        return out_times;
    }

    /* turns Double[] samples to time according to specified dt */
    public static Double[] Sample2Time(Double[] samples, double dt){
        Double[] out = new Double[samples.length];
        for (int i = 0; i < samples.length; i++) {
            out[i] = samples[i] * dt;
        }
        return out;
    }

    /**
     * This method takes the stimulus mean matrix and the stimulation times that are significant and creates the downsampled
     * version of the stimulus with specified dt and signal length
     * @param signal_length the length of the destination signal (how many samples there is?)
     * */
    public TimeSeriesMatrix ResampleAllStimulus(TimeSeriesMatrix stim_mean_mat, double dt_dest, int signal_length) {
        TimeSeriesMatrix out = new TimeSeriesMatrix(stim_mean_mat.getNumSignals(), signal_length, dt_dest);
        int binSize = (int) Math.round(dt_dest/this.dt); // Calculate bin Size for mean/peak value extraction
        for (int i = 0; i < stim_mean_mat.getNumSignals(); i++) {
            for (int j = 1; j < signal_length; j++) {
                if(binSize * j >= stim_mean_mat.getLength()){
                    continue;
                }
                double value = CalciumSignal.average(Arrays.copyOfRange(
                        stim_mean_mat.getSignal(i),binSize * (j-1),binSize * j));    // extract mean for each bin
                out.setSignalToEntry(i,j-1,value);                                    // register value to correct location in ou)
            }
        }
        return out;
    }
    
    /**
     *  Returns resampled mean stimulus of the imp
     *  @param first,last indexes of first and last slice in stimulus which is relevant*/
    public ArrayList<Double> ResampleStimulus(double dt_dest, int signal_lenght,int first,int last){
        ArrayList<Double> stim_resampled = new ArrayList<>(signal_lenght);
        int binSize = (int) Math.round(dt_dest/this.dt);
        double[] mean_sig = Activity_Analysis.getAverageSignal(this.imp, first, last);
        double[] mean_sig_wz = new double[mean_sig.length+1]; // adding 0 to the array
        mean_sig_wz[0] = 0;
        for (int i = 0; i < mean_sig.length; i++) {
            mean_sig_wz[i+1] = mean_sig[i];
        }

        for (int i = 1; i < signal_lenght; i++) {
            if(binSize * i >= mean_sig_wz.length){
                continue;
            }
            try {
                double value = CalciumSignal.average(Arrays.copyOfRange(mean_sig_wz, binSize * (i - 1), binSize * i));    // extract mean for each bin
                stim_resampled.add(value);                                    // register value to correct location in output
            }
            catch(Exception e){
                System.out.print(e.getMessage());
            }
        }
        return stim_resampled;
    }

    /** Tests the plugin. */
    public void test_RoiRegist(ImagePlus tImp, ArrayList<Roi> tRoiset, AffineTransform tAfft){
        /* tests if a Roi can be transormed and then transformed back */

        IJ.run(imp, "Select All", "");
        ImagePlus imp2 = AffineTransformIm(tImp, tAfft);

        // Affine Transfrom to a region
        for (int i = 0; i < tRoiset.size(); i++) {
            FloatPolygon floatp = tRoiset.get(i).getFloatPolygon();
            Point2D[] roi_coord_src = float2PointArray(floatp.xpoints, floatp.ypoints);
            Point2D[] roi_coord_dest = new Point2D[roi_coord_src.length];
            AffineTransform aT = new AffineTransform(tAfft);
            aT.transform(roi_coord_src,0,roi_coord_dest,0,roi_coord_dest.length);
            RealMatrix xy_dest = PointArr2float(roi_coord_dest);
            PolygonRoi roi_dest = new PolygonRoi(CalciumSignal.DoubletoFloat(xy_dest.getColumn(0)),
                    CalciumSignal.DoubletoFloat(xy_dest.getColumn(1)),Roi.FREEROI);
            imp2.setRoi(roi_dest, true);
            imp2.show();
        }
        IJ.run("In [+]", "");
    }

    private static RealMatrix PointArr2float(Point2D[] roi_coord_dest) {
        RealMatrix out = MatrixUtils.createRealMatrix(roi_coord_dest.length, 2);
        for(int i = 0;i<roi_coord_dest.length;i++){
            out.addToEntry(i, 0, roi_coord_dest[i].getX()); // set X
            out.addToEntry(i, 1, roi_coord_dest[i].getY()); // set Y
        }
        return out;
    }

    /* convert xfloat[], yfloat[] to Point2D[] */
    public static Point2D[] float2PointArray(float[] x, float[] y){
        if(x.length != y.length) {
            new ErrorMsg("inputs don't have same length");
            return null;
        }
        Point2D[] out = new Point2D[x.length];
        for(int i = 0;i<x.length;i++) {
            out[i] = new Point2D.Float();
            out[i].setLocation(x[i], y[i]);
        }
        return out;
    }

    /*main*/
    public static void main(final String... args) {
        String path;
        ImagePlus imp, stim_stack = null;
//        try {
////            path = "C:\\Users\\Noam\\Dropbox\\# graduate studies m.sc\\# SLITE\\ij - plugin data\\"; // LAB
////            imp = IJ.openImage(path + "FLASH_20msON_20Hz_SLITE_1.tif");
//
//            path = "D:\\# Projects (Noam)\\# SLITE\\# DATA\\301115Retina - DATA\\Loc 3\\post\\s_r_reg\\";
//            imp = IJ.openImage(path + "niel_pre2chan.tif"); // DEBUG
//            stim_stack = IJ.openVirtual(path + "Flashing4 (1-9000).tif"); // DEBUG
//            //stim_stack = IJ.openVirtual(path + "stack_test.tif"); // DEBUG
//            if(imp == null){
//                throw new FileNotFoundException("Your not in Lab....");
//            }
//        }
//        catch(FileNotFoundException error){
//            path = "C:\\Users\\noambox\\Dropbox\\# Graduate studies M.Sc\\# SLITE\\ij - plugin data\\"; //HOME
//            imp = IJ.openImage(path + "FLASH_20msON_20Hz_SLITE_1.tif"); // DEBUG
//
//        }

        path = "C:\\Users\\noambox\\Documents\\Sync\\Neural data analysis\\Test ED out\\for recenter test"; // LAB
        imp = IJ.openImage(path + "\\image2.tif");
        String roi_path = "C:\\Users\\noambox\\Documents\\Sync\\Neural data analysis\\Test ED out\\for recenter test\\RoiSet_input.zip";
        Stimulus_API sapi = new Stimulus_API();

        imp.show();
        IJ.run("In [+]", "");
        IJ.run("In [+]", "");

        // Recenter Test implementation
        ArrayList<PolygonRoi> rois = AAP_woStimulus.loadROIZip(roi_path);
        ArrayList<PolygonRoi> roi_out = sapi.recenterRois(rois, imp);


 /*        single roi implementation
        IJ.open(path + "0101-0091.roi");
        roi = imp.getRoi();
        IJ.run("In [+]", "");
        sapi.setup("", imp, roi);*/

/*

        // multiRoi_test implementation
        File dir_roiset = new File(path + "RoiSet\\");
        int numoffiles = dir_roiset.listFiles().length;
        ArrayList<PolygonRoi> roiset = new ArrayList<>(numoffiles);
        for(int i=0;i<numoffiles;i++){
            IJ.open(path + "RoiSet\\" + dir_roiset.listFiles()[i].getName());
            roiset.set(i, (PolygonRoi) imp.getRoi() );
        }
        int enlarge_roi = 3;
        double[] flatmatrix = {0.9964273636760552, -0.08691893249609045, 0.08691893249609045,
                0.9964273636760552, 16.633309220222923, -19.486581339366438};

        sapi.setup("", stim_stack, roiset,enlarge_roi);
        AffineTransform aft = new AffineTransform(flatmatrix);

       // sapi.test_RoiRegist(imp, roiset, aft);
        TimeSeriesMatrix stim_mean_mat = sapi.FindRoisMeanSignals(aft);
        // ArrayList<ArrayList<Double>> stim_time = sapi.ExtractStimTimes(stim_mean_mat); // TODO test

        double dt_dest = 1/10D;
        int signal_length = 3000;
        TimeSeriesMatrix stim_mean_resampled = sapi.ResampleAllStimulus(stim_mean_mat, dt_dest, signal_length);*/
    }

}
