package sliteanalysis;

import biz.source_code.dsp.filter.IirFilterCoefficients;
import ij.ImagePlus;
import ij.Prefs;
import ij.blob.ManyBlobs;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import weka.core.AlgVector;

import java.util.ArrayList;

/**
 * Created by noambox on 12/21/2015.
 */
public class AAP_Constants {
    
    // Stack Constants
        public static final double cGETPEAKSCONST = 1.5;
        public static final double cSTIMULUS_FR = 1/60D; // 60 Hz default stimulus sr
        public static final boolean cREMOVEFIRST = true;
        public static final int cNOISE_ROI_UP = 0;
        public static final int cNOISE_ROI_LOW = 0;
        public static final double cKM_GAIN = 0.8;
        public static final double cKM_PRECVAR = 0.05;
        public static final boolean cUSEARTIFACT = false;
        public static boolean cUSEKALMAN = false;
        public static boolean cREPLACEARTIFACT = false;

    //Segmentation Constants
    
        // User Parameters
        public static final double cBL_NOISE_PREC = 0.3;
        public static final double cLPF_THRES = 0.002;
        public static final int cOBJECTIVE = 20; // ussually using 20X
        // Parameters - Erode
        public static final String cMORPH_PROC = "Open";
        public static final int cMORPH_ITER = 1;
        public static final int cMORPH_COUNT = 5;
        // Parameters - Find Maxima and Cell Magic Wand Param
        public static final double cFM_TOL = 12;
        public static final int cFM_OUT = 1; // IN_TOLERANCE
        public static final int cCM_MAX = 16;
        public static final int cCM_MIN = 4;
        public static final double cENLARGEROI = 2;
        // Parameters -  Threshold
        public static final String cTHRESH_METHOD = "Mean"; // Midgray
        public static final int cTHRESH_RADIUS = 10; // 15
        public static final double cTHRESH_P1 = -30; // -80
        // Parameters - Blob filtering
        public static final int cELLIPSE_a = 80; // pixel 100
        public static final int cELLIPSE_b = 100; // pixel 55
        public static final int cX_AXIS_FOV = 500; // [um] Horizontal real fov in image
        public static final double cCELL_R_MIN = 5; // [um]
        public static final double cCELL_R_MAX = 30; // [um]
        public static final int cCIRC_MAX = 1500;
        public static final double cAR_MAX = 2.8;
        // Parameters - Segmentation
        public static final String CLASSIFPATH = "C:\\Users\\niel\\Documents\\Noam\\Repos\\Fiji\\Fiji"; // SS version
        public static final String CLASSI = "\\Self Customized Parameters\\Classifiers\\classifier1-12.model";
        // Parameters Visualization and scale
        public static final int cOBJECTIVE_MAGNIFY = 20;
    
    // Signal Processing Constants
        // Detrending with chebychev LPF
        public static final double cCUTOFF = 0.001;
        public static final double cORDER = 5;
        public static final double[] cFILT_COEFF_B = {2.094852E-14,  1.047426E-13, 2.094852E-13,  2.094852E-13, 1.047426E-13, 2.094852E-14};
        public static final double[] cFILT_COEFF_A = {1.000, -4.9923054980901425, 9.96927569026119, -9.95399387801544, 4.9693826781446955, -0.9923589922996323};
        public static final float cNOISEPERCENTILE = (float)0.3;; //30% percentile
        public static final float cDETECTIONVARIANCE = (float)2;; // over this threshold there is a good chance for cell's activity
        public static final double activityVariance = 0;

    // Automatic Recenter Constant
        public static final int cMAXITER_ED = 10;
        public static final double cCLIMIT_ED = 5;
        public static final double cSCALE_ED = 1;
        public static final int  cWIDTH_ED = 10;
        public static final int  cHEIGHT_ED = 10;

}