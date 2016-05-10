import java.awt.*;
import java.awt.event.*;
import java.util.*;
import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.plugin.frame.*;
import sliteanalysis.AAP_woStimulus;

import java.awt.datatransfer.*;

/* Version 3_3 06th Aug 2008 : Removed the rolling average option. Deleted unwanted parts of the code.
 * Fixed a bug that lost the ROI's when reset fucntion is invoked.
/* Version 3_2 04th May
 *Added measuring integrated intensity option by replacing the get mean Intensity button in earlier versions.
 *
/*Version 3_1a Bug fix. 03rd May
 * As the resutls table won't be able to handle more than 150 columns, the get average function was crashing. Now an active mesg.
 *is displayed and excess ROis are omitted from display in results table. However they are taken into account for average calculation.
 *
/* Version 3_1: 23rd Apr
 * Fixed the indexing problems and column naming while writing to the results table. The column labels are delimted by single tabs.
 * Tried to improve the rolling average UI.
/* Version 3: 17th Apr 07
/* Fixed a bug in recentering that crashed if the first image used to recenter is not open throughout.
 * Added scale ROI's before recentering option. Now the ROI's in the ROI Manager are scaled down(Scale < 1) or up(Scale > 1 )
 * before recentering. After recentering the orginal dimensions are restored. This leads to better recentering more specifically when
 * scaled down, the merging of ROIs can be greatly avoided.
 * Replaced LabelROI's button with reset. Label ROIs option is redundant due to implementation of showall option in ROI Manager.
 * Reset allows to either a) reset the ROI number to zero (when no ROIs are there in Manager) or b) renumber the ROIs in the
 * ROI Manager if the Keep Prefix while reset option is checked in auto ROI properties c) or rename all the ROIs using the prefix
 * and numbers starting afresh.
 * Added a rolling average option. This allows one to perform  sliding average in the forward direction (< i to i+BinWidth >) on the
 * stack.  b) or bining of an exsisting stack c) or both of these at specified intervals. This last option is to preserve the raw
 * data structure over the transients. It acheives it by breaking the averaging at the transients.
 *
/*Missing version notes:
/* Once uploaded to ImageJ website the Timeseries is renamed to Time Series Analyser plugin.
 *Teh version 2 Persist and Label ROIs option is implemented (requested by Meera). I forgot to add a release note during that time.
 *I am adding the note while writing a note for 2_1.
 *Version 2_1
 *a) cleaned up a bug that caused the add on click option to be on even after turning it off under some rare instances.
 *b) implemented apply to exisiting ROis option in Auto ROi properties dialog
 *c) Implemented start ROi number from also in Auto ROi properties dialog.
 *Both of them Pablo's request
/*
 * Time_Series_4_2H.java 09 Aug 06: Updated 23:18 Hrs
 * 1) Recentering is rewritten and improved recentering
 * 2) Added few error checks
 * 3) Changed the button texts
 * 4) Commented out the ROI not converged log
 *
 * Time Series 4H: 09 Aug 06: Updated 14:30 Hrs
 * 1) Implemented the average time trace calculation in a different thread as a result you see the image stack
 *    getting updated while calculation. Could prove useful to check the recentering during time trace
 *    measurement.
 *      a) Added an option "Live ImageStack" to turn off this option and calculate in one thread.
 *      b) At present recenter during trace measurement is not implemented for teh separate thread.
 * 2) Fixed a bug that was  generating time trace for the first "n" ROis iresspective of selection in the ROI manager,
 *      where "n" being number of selections. Works fine if none was selected then it calculates for all
 * 3) Add on Click option now adds the ROi's with names
 * 4) Results Table heading is derived from the names of the ROI's
 * 5) Toggle button for add on click has been changed to check box. Now it is more easy to know what the mouse click
 *    is going to do
 * 6) Made large structural changes. Designed a new class for handling XY data (Trace Data). Moved the time trace
 *    calculation to a separate class capable of running in its own thread.
 * 7) Fixed the issue of having to restart everytime for recentering when a new image is opened. Used run method similar to
 *    multi measure to keep track of the image updates.
 *
 * Known Issues:
 * 1) Still getMean do not have any functionality
 * 2) The chages in the ROI manager (such as deletion) is not kept track of while naming new ROIs
 * 3) Recentering is only accurate to a pixel(no sub pixel accuracy). So it does the job but could be improved.
 *
 *
 *Time Series 3H_3:
 *1) Bug fixes:
 *  a) Even after you close the TIme series window the mouse clicks where adding ROI's. It is fixed now
 *  b) If you close the time trace window that appears after get average the plot in the next run was not visible.
 *     It has been fixed in this version.
 *  c) Two or more windows of Time series appeared if you run the plugin again causing confusion in sharing the
 *      ROI Manager. This has been fixed
 2) Recentering during TimeTrace measurement is enabled.
 *Known issues:
 * 1) Image gets updated only at the end of the time trace measurement.
 * 2) No functionality for get mean button still.
 *
 *
 *Release Notes for version 3H_2 : 30th Jul 06
 * 1) rewrite the code for recentering to effect following things
 *  a) During the recentering, update the hashtable and roilist (similar to update command of ROIManager)
 *     instead of adding and deleting.
 *  b) Update the image while getaverage is called
 *  c) Implement recentering for individual slice
 *
 * Created on July 13, 2006, 12:12 AM
 *
 * Release Notes: 23 Jul 2006
 *
 * This plugin is for analysing the timelapse movies. Following features are available in version 3H
 * i) a) An automatic specification of ROI's as mouse is clicked on an image
 *    b) One can specify the nature of the autoROI (rectangle or oval and  size of it)
 *ii) a)Recenter the ROI based on the centriod of the region (this is done iteratively with a convergence chk.)
 *    b)Number of iterations for recentering and convergence limit can be specified in Set Recentering properties
 *    c) Lot of bugs in the recetentering section has now been fixed. Still requires a bit of tinkering.
 *iii) Ability to measure and tabulate the mean intensity along with the average is provided. The average
 *     is plotted.
 *Known Issues:
 *i) If the ROi list has rois of different sizes then while recentering things get messy (hoping to define messy soon)
 *
 *Planned features:
 *
 *i) Provison to draw all the ROIs that are in the list as one adds new ones
 *ii) Ability to plot the mean intensity of the individual ROi's in a seprarate plot
 *iii) Dynamic average plot that keeps track of the changes in the ROIList
 *iv) ...and many more as I keep thinking !!
 */



/**
 *
 * @author balaji
 */
public class Time_Series_Analyzer_V3 extends PlugInFrame implements ActionListener, MouseListener, ItemListener,
        ClipboardOwner/**/, PlugIn, KeyListener/* for keyborad shortcut*/,Runnable,ImageListener{
    Panel panel;
    static Frame Instance;

    protected double MeanIntensity[] = null;
    protected double Err[] = null;
    private boolean ADD = false;
    private String Names[] = {"Rectangle","Oval","FreeHand (not implemented)"};
    private int ROIType = 1;
    private int Width = 10;
    private int Height = 10;
    private Roi AutoROI = new OvalRoi(0,0,Width,Height);
    private ShapeRoi all = new ShapeRoi(AutoROI);
    private int MaxIteration = 15;
    private double CLimit = 0.1;
    private double MagCal = 0.5;
    private boolean ReCtrMean = false;
    private boolean Label = true;
    private  ResultsTable rt;
    private PlotWindow graph;
    private int ROICount = 0;
    private String Prefix = new String("ROI");
    private Thread  thread;
    boolean done = false;
    private ImageCanvas previousCanvas = null;
    private boolean KeepPrefix = true;
    private int uiMeasure = 2; //2 for pixel average and 3 for integrated intensity;
    ImagePlus previousImp = null, processedImp = null;
    ImageStack AveStack = null ;
    java.awt.Checkbox AddOnClick, UpdateStack, persist, LiveGraph;

    public void lostOwnership (Clipboard clip, Transferable cont) {}

    public void setIntegratedIntensity(boolean IntegratedIntensity){
        uiMeasure = (IntegratedIntensity) ? 3 : 2;
    }

    public void run() {
        while (!done) {
            try {Thread.sleep(500);}
            catch(InterruptedException e) {}
            ImagePlus imp = WindowManager.getCurrentImage();
            if (imp != null){
                ImageCanvas canvas = imp.getCanvas();
                if (canvas != previousCanvas){
                    if(previousCanvas != null)
                        previousCanvas.removeMouseListener(this);
                    canvas.addMouseListener(this);
                    previousCanvas = canvas;
                }
            }
            else{
                if(previousCanvas != null)
                    previousCanvas.removeMouseListener(this);
                previousCanvas = null;
            }

        }
    }

    RoiManager getManager() {
        RoiManager instance = RoiManager.getInstance();
        if (instance == null)
            return new RoiManager ();
        else
            return instance;
    }

    public void windowClosed(WindowEvent e) {
        Instance = null;
        done = true;
        AddOnClick.setState(false);
        ROICount = 0;
        all = null;
        AutoROI = null;
        ImagePlus.removeImageListener(this);
        ImagePlus imp = WindowManager.getCurrentImage();
        //ImageWindow Win = imp.getWindow();
        if (imp == null){
            previousCanvas = null;
            super.windowClosed(e);
            return;
        }
        ImageCanvas canvas = imp.getCanvas();
        if(canvas != null){
            canvas.removeMouseListener(this);
            canvas.removeKeyListener(this);
        }
        if(previousCanvas != null)
            previousCanvas.removeMouseListener(this);
        previousCanvas = null;
        super.windowClosed(e);
    }

    public Time_Series_Analyzer_V3() {
        super ("Time Series V3_0");
        if (Instance != null){
            Instance.toFront();
        }
        else{
            Instance = this;
            ImagePlus.addImageListener(this);
            WindowManager.addWindow(this);
            setLayout(new FlowLayout(FlowLayout.CENTER,5,5));

            panel = new Panel();
            panel.setLayout(new GridLayout(10, 0, 0, 0));

            addButton("Auto ROI Properties");
            addButton("Recenter");
            addButton("Recenter Parameters");

            addButton("Get Average"); //Average over all the ROIs
            addButton("Get Total Intensity");
            addButton("Reset");

            addButton("Translate ROi's");
            //addButton("SetasAutoROi");
            AddOnClick = new Checkbox("Add On Click");
            panel.add(AddOnClick);
            AddOnClick.setState(false);
            panel.add(persist = new Checkbox("Persist", true));
            // panel.add(LiveGraph = new Checkbox("Live Graph", false));
            panel.add(UpdateStack = new Checkbox("New thread for measuring", true));
            add(panel);
            pack();
            //GUI.center(this);
            this.setVisible(true);
            thread = new Thread(this,"Time Series ");
            thread.setPriority(Math.max(thread.getPriority()-2,thread.MIN_PRIORITY));
            thread.start();
            getManager();
        }
    }
    void addButton(String label) {
        Button b = new Button(label);
        b.addActionListener(this);
        panel.add(b);
    }
    public void actionPerformed(ActionEvent e) {
        String label = e.getActionCommand();
        if (label==null)
            return;
        String command = label;
        if(command.equals("Auto ROI Properties"))
            SetAutoROIProperties();
        if(command.equals("Recenter"))
            recenter();
        if(command.equals("Recenter Parameters"))
            SetRecenterProp();
        if(command.equals("Get Average"))
            getAverage();
        if(command.equals("Get Total Intensity"))
            this.getIntegrated();
        if (command.equals("Reset")) {
            if (KeepPrefix)
                ResetNum();
            else
                RenameROIS();
        }
        if(command.equals("Translate ROi's")){
            MoveRois();
        }
        if(command.equals("SetasAutoROi"))
            DefAutoROi();
    }

    protected void DefAutoROi(){
        IJ.showMessage("Yet to be Implemented");

    }

    protected void MoveRois(){
        GenericDialog gd = new GenericDialog("Translate ROi's");
        gd.addNumericField("Enter the y shift(negative would move the ROis up)",0,0);
        gd.addNumericField("Enter the x shift (negative would move the ROis left)",0,0);
        gd.showDialog();
        int xShift = 0, yShift = 0;
        if (gd.wasCanceled())
            return;
        RoiManager manager = getManager();
        Roi[] rois = manager.getSelectedRoisAsArray();
        if (rois.length == 0) {
            IJ.showMessage("No rois in the ROI manager");
            return;
        }
        yShift = (int)gd.getNextNumber();
        xShift = (int)gd.getNextNumber();
        java.awt.Rectangle BRect ;
        Roi CurRoi,tmpRoi;
        int NewX, NewY;
        for(int i = 0 ; i < rois.length ; i++){
            CurRoi = rois[i];
            BRect = CurRoi.getBounds();
            NewX = Math.round(BRect.x  + xShift);
            NewY = Math.round(BRect.y  + yShift);
            CurRoi.setLocation(NewX,NewY);
        }
        manager.runCommand("show all");
    }

    public void itemStateChanged(ItemEvent e) {
        // Want to use it for dynamically updating the profile. Will be addresssed in later version

    }
    public void keyPressed(KeyEvent e) {}
    public void keyReleased (KeyEvent e) {}
    public void keyTyped (KeyEvent e) {}
    public void mousePressed(MouseEvent e){}
    public void mouseReleased(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}

    public void mouseClicked(MouseEvent e) {

        if (AddOnClick.getState()){
            int x = e.getX();
            int y = e.getY();

            ImagePlus imp = WindowManager.getCurrentImage();
            if (imp != null){
                ImageWindow Win = imp.getWindow();
                ImageCanvas canvas = Win.getCanvas();

                int offscreenX = canvas.offScreenX(x);
                int offscreenY = canvas.offScreenY(y);
                int Start_x = offscreenX - (int)(Width/2);
                int Start_y = offscreenY - (int)(Height/2);
                AutoROI.setLocation(Start_x, Start_y);
                ROICount++;
                imp.setRoi(AutoROI);
                String name;
                if (ROICount < 100){
                    name = (ROICount < 10 ) ? Prefix + "00" + ROICount :  Prefix + "0"  + ROICount;
                }
                else{
                    name = Prefix + ROICount;
                }
                //ROIList.add(name);
                Roi temp = (Roi)AutoROI.clone();
                temp.setName(name);
                getManager().addRoi(temp);
                if (persist.getState())
                    getManager().runCommand("show all");
                // if(LiveGraph.getState()){
                //  getAverage();
                //}
                   /* if(DisplayLabel.getState()){
                        AddLabel(temp, name);
                    }*/
            }
        }
    }

    public void ResetNum() {
        int[] indexes = getSelectedIndexes();
        if (indexes.length == 0) {
            IJ.showMessage("No rois in the ROI manager");
            return;
        }
        String Label = "";
        RoiManager manager = getManager();
        Roi[] rois = manager.getRoisAsArray();
        for (int i = 0 ; i < indexes.length ; i++,++ROICount){
            Roi tmpRoi =  rois[indexes[i]];
            Label = tmpRoi.getName();
            Label = Label.substring(0,(Label.length()-2));
            if (ROICount < 100)
                Label = (ROICount < 9 ) ? Prefix + "00" + (ROICount+1) :  Prefix + "0"  + (ROICount+1);
            else
                Label = Prefix + (ROICount +1);
            manager.select(indexes[i]);
            manager.runCommand("Rename",Label);
        }
        manager.runCommand("deselect");
        manager.runCommand("show all");
    }

    public void RenameROIS(){
        ROICount = 0;
        int[] indexes = getSelectedIndexes();
        if (indexes.length == 0) {
            IJ.showMessage("No rois in the ROI manager");
            return;
        }
        String Label = "";
        RoiManager manager = getManager();
        Roi[] rois = manager.getRoisAsArray();
        for (int i = 0 ; i < indexes.length ; i++,++ROICount){
            Roi tmpRoi =  rois[indexes[i]];
            if (ROICount < 100)
                Label = (ROICount < 9 ) ? Prefix + "00" + (ROICount+1) :  Prefix + "0"  + (ROICount +1);
            else
                Label = Prefix + (ROICount+1);
            manager.select(indexes[i]);
            manager.runCommand("Rename",Label);
        }
        manager.runCommand("deselect");
        manager.runCommand("show all");
    }

    public void mouseEntered(MouseEvent e) {}

    // This method is reqd. for the button interface
    public void SetAutoROIProperties(){
        ij.gui.GenericDialog gd = new ij.gui.GenericDialog("AutoROI properties");
        gd.addNumericField("Width: ", Width, 0);
        gd.addNumericField("Height: ", Height, 0);
        gd.addNumericField("Start the ROI number from",ROICount, 0);
        gd.addStringField("Prefix for AutoROI",Prefix);
        // boolean values[] = {false,true,false};
        gd.addChoice("ROI Type",Names,Names[ROIType]);
        gd.addCheckbox("Resize exisiting ROIS", false);
        gd.addCheckbox("Keep the prefix during reset",KeepPrefix);
        gd.showDialog();


        if(!gd.wasCanceled())
        {
            this.Width = (int)gd.getNextNumber();
            this.Height = (int)gd.getNextNumber();
            int Count = (int)gd.getNextNumber();
            if(Count != ROICount && Count > 1) ROICount = Count - 1;              // >1 is an indication the number is last ROI in the manager
            //IJ.log("New ROI"+ Width + " "+ Height); //for debugging
            this.Prefix = gd.getNextString();
            ROIType = gd.getNextChoiceIndex();
            switch (ROIType) {
                case 0:
                    this.AutoROI = new Roi(0,0,Width,Height);
                    break;
                case 1:
                    this.AutoROI = new OvalRoi(0,0,Width,Height);
                    break;
            }
            if(gd.getNextBoolean()){
                ResizeROIS();
            }
            KeepPrefix = gd.getNextBoolean();

        }

    }
    public void ScaleROIS(double Scale){
        Width = (int)(Width * Scale);
        Height = (int)(Height * Scale);
        switch (ROIType) {
            case 0:
                this.AutoROI = new Roi(0,0,Width,Height);
                break;
            case 1:
                this.AutoROI = new OvalRoi(0,0,Width,Height);
                break;
        }
        ResizeROIS();
    }

    public void ScaleROIS(int Width, int Height){
        this.Width = Width ;
        this.Height = Height ;
        switch (ROIType) {
            case 0:
                this.AutoROI = new Roi(0,0,Width,Height);
                break;
            case 1:
                this.AutoROI = new OvalRoi(0,0,Width,Height);
                break;
        }
        ResizeROIS();
    }

    public void ResizeROIS(){
        RoiManager manager = getManager();
        Roi[] rois = manager.getSelectedRoisAsArray();
        if (rois.length == 0) {
            IJ.showMessage("No rois in the ROI manager");
            return;
        }
        java.awt.Rectangle BRect ;
        Roi CurRoi,tmpRoi;
        int NewX, NewY;
        for(int i = 0 ; i < rois.length ; i++){
            CurRoi = rois[i];
            BRect = CurRoi.getBounds();
            NewX = Math.round(BRect.x + (BRect.width - Width)/2);
            NewY = Math.round(BRect.y + (BRect.height - Height)/2);
            CurRoi.setLocation(NewX,NewY);
            CurRoi.setName(CurRoi.getName());
        }
        manager.runCommand("show all");
    }

    public void SetRecenterProp(){
        ij.gui.GenericDialog gd = new ij.gui.GenericDialog("Recentering Properties");

        gd.addNumericField("Convergence Limit (Pixels) ", CLimit, 1);
        gd.addNumericField("Maximum Iterations: ", MaxIteration, 0);
        gd.addNumericField("Rescale ROI by ",MagCal,1);

        gd.addCheckbox("Recenter for measuring mean",ReCtrMean);
        gd.showDialog();
        if(!gd.wasCanceled())
        {
            CLimit = gd.getNextNumber();
            MaxIteration = (int)gd.getNextNumber();
            MagCal = gd.getNextNumber();
            ReCtrMean = (boolean) gd.getNextBoolean();
        }
    }

    public void recenter() {
        ImagePlus imp = WindowManager.getCurrentImage();
        if (imp == null) {
            IJ.showMessage("OOPS! no image open");
            return ;
        }
        int CurSlice = imp.getCurrentSlice();
        recenter(imp, CurSlice);
        imp.setRoi(all);
    }

    public void recenter(ImagePlus imp, int CurSlice){
        if(imp != null){

            RoiManager manager = getManager();
            Roi[] rois = manager.getSelectedRoisAsArray();
            if(rois.length == 0) {
                IJ.showMessage("No rois in the ROI manager");
                return;
            }

            int CurROIWidth = this.Width;
            int CurROIHeight = this.Height;
            ScaleROIS(MagCal);

            ImageStatistics stat = new ImageStatistics();
            ij.measure.Calibration calib = imp.getCalibration();
            double xScale = calib.pixelWidth;
            double yScale = calib.pixelHeight;
            ShapeRoi temp = null;
            boolean Converge = false;
            int New_x = 0;
            int New_y = 0;
            imp.setSlice(CurSlice);
            double xMovement = 0, yMovement = 0;
            java.awt.Rectangle Boundary;
            for(int i = 0; i < rois.length ; i++){
                Roi CurRoi =  rois[i];
                Boundary = CurRoi.getBounds();
                Converge = false;
                imp.setRoi(CurRoi);
                double OldDiff = 0,NewDiff = 0;
                int Old_x,Old_y;
                for(int Iteration = 1 ; Iteration <= MaxIteration  && !Converge; Iteration++){
                    stat = imp.getStatistics(64 + 32); //Calculate center of Mass and Centroid;
                    New_x = (int) Math.round(((stat.xCenterOfMass/xScale) - (Boundary.getWidth()/2.0)));
                    New_y = (int) Math.round(((stat.yCenterOfMass/yScale) - (Boundary.getHeight()/2.0)));
                    // Calculate movements
                    xMovement =(stat.xCentroid - stat.xCenterOfMass)/xScale;
                    yMovement = (stat.yCentroid - stat.yCenterOfMass)/yScale;
                    if( Math.abs(xMovement) < 1 && xMovement != 0 && yMovement != 0 && Math.abs(yMovement) < 1){ //Now search nearby;
                        if(Math.abs(xMovement) > Math.abs(yMovement)){
                            New_x = (xMovement > 0) ? (int)Math.round(stat.xCentroid/xScale - (Boundary.getWidth()/2.0) - 1) : (int)Math.round(stat.xCentroid/xScale - (Boundary.getWidth()/2.0) + 1);
                            New_y = (int) Math.round(stat.yCentroid/yScale - (Boundary.getHeight()/2.0));
                        }
                        else{
                            New_y = (yMovement > 0) ? (int)Math.round(stat.yCentroid/yScale -(Boundary.getHeight()/2.0)- 1) : (int)Math.round(stat.yCentroid/yScale - (Boundary.getHeight()/2.0)+ 1);
                            New_x = (int) Math.round(stat.xCentroid/xScale -(Boundary.getWidth()/2.0));
                        }
                    }
                    else{
                        New_x = (int)Math.round (((stat.xCenterOfMass/xScale) - (Boundary.getWidth()/2.0)));
                        New_y = (int)Math.round (((stat.yCenterOfMass/yScale) - (Boundary.getHeight()/2.0)));

                    }
                    Converge = ( Math.abs(xMovement) < CLimit && Math.abs(yMovement) < CLimit)  ? true : false ;
                    CurRoi.setLocation(New_x ,New_y);
                    imp.setRoi(CurRoi);
                }
                temp = new ShapeRoi(CurRoi);
                all = (i == 0) ? new ShapeRoi(CurRoi) : all.xor(temp);

                       /* if(!Converge)
                                IJ.log(indexes[i] + "\t ROI did not converge" );*/
                       /* else
                               IJ.log(indexes[i] + "\t ROI converged" );*/

            }
            ScaleROIS(CurROIWidth,CurROIHeight);

        }

    }


    public void recenter(ImagePlus imp, int CurSlice, ArrayList<PolygonRoi> r_in ){
        if(imp != null){

//            RoiManager manager = getManager();
//            Roi[] rois = manager.getSelectedRoisAsArray();
//            if(rois.length == 0) {
//                IJ.showMessage("No rois in the ROI manager");
//                return;
//            }
            Roi[] rois = r_in.toArray(new Roi[r_in.size()]);
            int CurROIWidth = this.Width;
            int CurROIHeight = this.Height;
            ScaleROIS(MagCal);

            ImageStatistics stat = new ImageStatistics();
            ij.measure.Calibration calib = imp.getCalibration();
            double xScale = calib.pixelWidth;
            double yScale = calib.pixelHeight;
            ShapeRoi temp = null;
            boolean Converge = false;
            int New_x = 0;
            int New_y = 0;
            imp.setSlice(CurSlice);
            double xMovement = 0, yMovement = 0;
            java.awt.Rectangle Boundary;
            for(int i = 0; i < rois.length ; i++){
                Roi CurRoi =  rois[i];
                Boundary = CurRoi.getBounds();
                Converge = false;
                imp.setRoi(CurRoi);
                double OldDiff = 0,NewDiff = 0;
                int Old_x,Old_y;
                for(int Iteration = 1 ; Iteration <= MaxIteration  && !Converge; Iteration++){
                    stat = imp.getStatistics(64 + 32); //Calculate center of Mass and Centroid;
                    New_x = (int) Math.round(((stat.xCenterOfMass/xScale) - (Boundary.getWidth()/2.0)));
                    New_y = (int) Math.round(((stat.yCenterOfMass/yScale) - (Boundary.getHeight()/2.0)));
                    // Calculate movements
                    xMovement =(stat.xCentroid - stat.xCenterOfMass)/xScale;
                    yMovement = (stat.yCentroid - stat.yCenterOfMass)/yScale;
                    if( Math.abs(xMovement) < 1 && xMovement != 0 && yMovement != 0 && Math.abs(yMovement) < 1){ //Now search nearby;
                        if(Math.abs(xMovement) > Math.abs(yMovement)){
                            New_x = (xMovement > 0) ? (int)Math.round(stat.xCentroid/xScale - (Boundary.getWidth()/2.0) - 1) : (int)Math.round(stat.xCentroid/xScale - (Boundary.getWidth()/2.0) + 1);
                            New_y = (int) Math.round(stat.yCentroid/yScale - (Boundary.getHeight()/2.0));
                        }
                        else{
                            New_y = (yMovement > 0) ? (int)Math.round(stat.yCentroid/yScale -(Boundary.getHeight()/2.0)- 1) : (int)Math.round(stat.yCentroid/yScale - (Boundary.getHeight()/2.0)+ 1);
                            New_x = (int) Math.round(stat.xCentroid/xScale -(Boundary.getWidth()/2.0));
                        }
                    }
                    else{
                        New_x = (int)Math.round (((stat.xCenterOfMass/xScale) - (Boundary.getWidth()/2.0)));
                        New_y = (int)Math.round (((stat.yCenterOfMass/yScale) - (Boundary.getHeight()/2.0)));

                    }
                    Converge = ( Math.abs(xMovement) < CLimit && Math.abs(yMovement) < CLimit)  ? true : false ;
                    CurRoi.setLocation(New_x ,New_y);
                    imp.setRoi(CurRoi);
                }
                temp = new ShapeRoi(CurRoi);
                all = (i == 0) ? new ShapeRoi(CurRoi) : all.xor(temp);

                       /* if(!Converge)
                                IJ.log(indexes[i] + "\t ROI did not converge" );*/
                       /* else
                               IJ.log(indexes[i] + "\t ROI converged" );*/

            }
            ScaleROIS(CurROIWidth,CurROIHeight);

        }

    }

    public int[] getSelectedIndexes() {
        int[] indexes = getManager().getSelectedIndexes();
        if (indexes==null || indexes.length==0)
            indexes = getAllIndexes();
        return indexes;
    }

    public int[] getAllIndexes(){
        int count = getManager().getCount();
        int[] indexes = new int[count];
        for (int i=0; i<count; i++)
            indexes[i] = i;
        return indexes;
    }

    public void getAveWithoutUpdate(boolean DispRes){
        ImagePlus imp = WindowManager.getCurrentImage();
        if (imp != null){
            RoiManager manager = getManager();
            Roi[] rois = manager.getSelectedRoisAsArray();
            if (rois.length == 0) {
                IJ.showMessage("You need to add atleast one ROI");
                return;
            }
            if(rois.length > 148){
                IJ.showMessage("Warning","Results table can  display 150 (148 ROis) columns only. Excess "+(rois.length - 148)+" ROis will be omitted");
            }
            ImageStatistics stat = new ImageStatistics();
            int MaxSlice = imp.getStackSize();
            if(MaxSlice < 2){
                IJ.showMessage("This plugin requires a ImageStack: ImageJ found" + MaxSlice + "slice only");
                return;
            }
            MeanIntensity = new double[MaxSlice];
            Err = new double[MaxSlice];
            //int StartSlice = imp.getCurrentSlice();
            String Mean = "";
            double Sum, SqSum, Variance;
            Roi roi;
            rt = new ResultsTable();
            ImageProcessor ip = imp.getProcessor();
            int nCol_Res_Tab = (rois.length > 147) ? 147 : rois.length;
            double Int = 0;
            imp.unlock();
            for (int CurSlice = 0 ; CurSlice < MaxSlice ; CurSlice ++){
                imp.setSlice(CurSlice+1);
                Sum = 0;
                SqSum = 0;
                rt.incrementCounter();
                if(ReCtrMean){
                    recenter(imp,CurSlice+1);
                    imp.setSlice(CurSlice+1);
                    imp.setRoi(all);
                }
                for (int CurIdx = 0; CurIdx < rois.length && CurIdx <= 147; CurIdx++){
                    roi = rois[CurIdx];
                    imp.setRoi(roi);
                    stat = imp.getStatistics(uiMeasure); // MEAN = 2
                    Int = (uiMeasure == 2) ? stat.mean : stat.mean *stat.area;
                    rt.addValue(roi.getName(),Int);
                    Sum += Int;
                    SqSum += (Int * Int) ;
                }
                MeanIntensity[CurSlice] = Sum/rois.length;
                Variance = ((SqSum/rois.length)- MeanIntensity[CurSlice]*MeanIntensity[CurSlice]);
                Err[CurSlice] = (true /*StdErr*/) ? java.lang.Math.sqrt(Variance/rois.length)
                        : java.lang.Math.sqrt(Variance);
                rt.addValue("Average",MeanIntensity[CurSlice]);
                rt.addValue("Err",Err[CurSlice]);
            }

            if(DispRes)
            {
                rt.show("Time Trace(s)");
                double [] xAxis = new double[MaxSlice];
                for(int nFrames = 1 ; nFrames <= MaxSlice ; nFrames++)
                    xAxis[nFrames-1] = nFrames;
                Plot plot = new Plot("Time Trace Average","Time (Frames)","Average Intensity",xAxis,MeanIntensity);
                //plot.addErrorBars(Err);
                plot.draw();
                if(WindowManager.getImage("Time Trace Average")== null)
                    graph = null;
                if(graph == null){
                    graph = plot.show();
                    //graph.addErrorBars(Err);
                    graph.addPoints(xAxis,MeanIntensity,PlotWindow.CIRCLE);
                }
                else{
                    graph.drawPlot(plot);
                    // graph.addErrorBars(Err);
                    graph.addPoints(xAxis,MeanIntensity,PlotWindow.CIRCLE);
                }
            }
        }
        return;
    }

    public void getAverage() {
        if (UpdateStack.getState()){
            ImagePlus imp = WindowManager.getCurrentImage();
            if(imp != null){
                ImageStack Stack = imp.getStack();
                if(Stack.getSize() < 2){
                    IJ.showMessage("This function requires stacks with more than 1 slice");
                    return;
                }
            }
            else {
                IJ.showMessage("OOPS! No images are open");
                return;
            }
            TimeTrace Trace = new TimeTrace(imp,getManager());
            Trace.setName("Trace");
            Trace.setPriority(Math.max(Trace.getPriority()-2,Trace.MIN_PRIORITY));
            Trace.start();
        } else {
            setIntegratedIntensity(false);
            getAveWithoutUpdate(true);
        }

    }

    public double[] getAverageData(){
        return (double[])MeanIntensity.clone();
    }

    public void showGraph(){
        IJ.showMessage("Not yet implemented");
    }


    private void getIntegrated() {
        //IJ.showMessage("Not yet Implemented");
        if(UpdateStack.getState()){
            ImagePlus imp = WindowManager.getCurrentImage();
            if(imp != null){
                ImageStack Stack = imp.getStack();
                if(Stack.getSize() < 2){
                    IJ.showMessage("This function requires stacks with more than 1 slice");
                    return;
                }
            }
            else{
                IJ.showMessage("OOPS! No images are open");
                return;
            }
            TimeTrace Trace = new TimeTrace(imp,getManager());
            Trace.setName("Trace");
            Trace.setPriority(Math.max(Trace.getPriority()-2,Trace.MIN_PRIORITY));
            Trace.setTotIntensity(true);
            Trace.start();
        }
        else{
            this.setIntegratedIntensity(true);
            getAveWithoutUpdate(true);
        }


    }

    public void imageOpened(ImagePlus imp) {
    }

    public void imageClosed(ImagePlus imp) {
        imp.getCanvas().removeMouseListener(this);
        imp.getCanvas().removeKeyListener(this);
        //

    }

    public void imageUpdated(ImagePlus imp) {
        if (/*Label*/ true){
            // LabelROIs();
        }
    }


    class TimeTrace extends Thread{
        ImagePlus imp;
        RoiManager Manager;
        TraceData[] Data;
        TraceData Average;
        TraceData Err;
        ResultsTable rt;
        double Variance;
        double MeanIntensity;
        double Error;
        boolean showAverage = true;
        boolean showAll = false;
        boolean CalAverage = true;
        PlotWindow graph;
        int uiMeasure = 2; // 2 for Pixel average and 3 for Total Intensity;

        public void setTotIntensity(boolean TotIntensity ){
            uiMeasure = (TotIntensity) ? 3 : 2;
        }

        public void setAverage(boolean x){
            CalAverage = x;
            if (!CalAverage)
                showAverage = false;
        }

        public void setDispAll(boolean x){
            showAll = x;
            if(x)
                CalAverage = x;
        }

        public void setAveDisp(boolean x){
            showAverage = x;
            if(x)
                CalAverage = x;
        }

        TimeTrace(ImagePlus imp, RoiManager Manager){
            if (imp != null && Manager != null){
                this.imp = imp;
                this.Manager = Manager;
            }
        }

        TimeTrace(){ // Not working properly I think it is timing issue. Need to fix it in later version
            imp = WindowManager.getCurrentImage();
            Manager = new RoiManager();
            if (imp == null || Manager == null){
                IJ.showMessage("Could not initialize ImagePlus/Roi manager");
                return ;
            }
        }

        public void run(){
            if (imp != null && Manager != null){
                Roi[] rois = getManager().getSelectedRoisAsArray();
                if (rois.length == 0) {
                    IJ.showMessage("You need to add atleast one ROI");
                    return;
                }
                if (rois.length > 148){
                    IJ.showMessage("Warning","Results table can  display 150 (148 ROis) columns only. Excess "+(rois.length - 148)+" ROis will be omitted");
                }
                ImageStatistics stat = new ImageStatistics();
                int MaxSlice = imp.getStackSize();
                if(CalAverage){
                    Average = new TraceData(MaxSlice);
                    Err = new TraceData(MaxSlice);
                }
                String Mean = "";
                double Sum, SqSum, Variance;
                Roi roi;
                rt = new ResultsTable();
                ImageProcessor ip = imp.getProcessor();
                imp.unlock();
                double Int = 0;
                for (int CurSlice = 0 ; CurSlice < MaxSlice ; CurSlice ++){
                    imp.setSlice(CurSlice+1);
                    Sum = 0;
                    SqSum = 0;
                    rt.incrementCounter();
                    /*if(ReCtrMean){
                        recenter(imp,CurSlice+1);
                        imp.setSlice(CurSlice+1);
                        imp.setRoi(all);
                    }*/
                    for (int CurIdx = 0; CurIdx < rois.length && CurIdx <= 147; CurIdx++){ //ImageJ results table can only handle 150 columns
                        roi = rois[CurIdx];
                        imp.setRoi(roi);
                        stat = imp.getStatistics(uiMeasure); // MEAN = 2
                        Int = (uiMeasure == 2) ? stat.mean : stat.mean *stat.area;
                        rt.addValue(roi.getName(),Int);
                        Sum += Int;
                        SqSum += (Int * Int) ;
                    }
                    if(CalAverage){
                        MeanIntensity = Sum/rois.length;
                        Average.addData(CurSlice, MeanIntensity);
                        Variance = ((SqSum/rois.length)- MeanIntensity*MeanIntensity);
                        Error = (true /*StdErr*/) ? java.lang.Math.sqrt(Variance/rois.length)
                                : java.lang.Math.sqrt(Variance);
                        Err.addData(CurSlice,Error);
                        rt.addValue("Average",MeanIntensity);
                        rt.addValue("Err",Error);
                    }

                }

                if(showAverage)
                {
                    rt.show("Time Trace(s)");
                    double [] xAxis = new double[MaxSlice];
                    for(int nFrames = 1 ; nFrames <= MaxSlice ; nFrames++)
                        xAxis[nFrames-1] = nFrames;
                    Plot plot = new Plot("Time Trace Average","Time (Frames)","Average Intensity",xAxis,Average.getY());
                    //plot.addErrorBars(Err);
                    plot.draw();
                    if(WindowManager.getImage("Time Trace Average")== null)
                        graph = null;
                    if(graph == null){
                        graph = plot.show();
                        //graph.addErrorBars(Err);
                        graph.addPoints(xAxis,Average.getY(),PlotWindow.CIRCLE);
                    }
                    else{
                        graph.drawPlot(plot);
                        // graph.addErrorBars(Err);
                        graph.addPoints(xAxis,Average.getY(),PlotWindow.CIRCLE);
                    }
                }
            }

        }

        public double[] getAverageData() {
            return (double[]) Average.getY().clone();
        }

        public int[] getSelectedIndexes() {
            int[] indexes = Manager.getSelectedIndexes();
            if (indexes==null || indexes.length==0)
                indexes = getAllIndexes();
            return indexes;
        }

        public int[] getAllIndexes(){
            int count = Manager.getCount();
            int[] indexes = new int[count];
            for (int i=0; i<count; i++)
                indexes[i] = i;
            return indexes;
        }

    }

    class TraceData extends Object{
        double[] xData = null;
        double[] yData = null;
        int CurrPos = 0;
        int DataLength = 0;
        //boolean Y_Only = false;

        public TraceData( int length){
            if (length > 0){
                DataLength = length;
                xData = new double[DataLength];
                yData = new double[DataLength];
            }
        }

        public TraceData( double[] x, double[] y){
            if( x != null && y != null){
                xData = (double[])x.clone();
                yData = (double[])y.clone();
                DataLength = Math.min(xData.length,yData.length);
            }
        }

        public boolean addData(double x, double y){
            if (CurrPos >= DataLength){
                IJ.showMessage("OOPS! I am full you can not add anymore to me");
                return false;
            }

            xData[CurrPos] = x;
            yData[CurrPos] = y;
            CurrPos++;
            return true;
        }

        public double getX(int pos){
            if(pos < DataLength)
                return xData[pos];
            return xData[DataLength];
        }

        public double getY(int pos){
            if(pos < DataLength) return yData[pos];
            return yData[DataLength];
        }

        public double[] getXY(int pos){
            double[] XY = new double[2];
            if (pos < DataLength){
                XY[1] = xData[pos];
                XY[2] = yData[pos];
            }
            else{
                XY[1] = xData[DataLength];
                XY[2] = yData[DataLength];
            }
            return XY;
        }

        public boolean setPosition(int pos){
            if(pos < DataLength){
                CurrPos = pos;
                return true;
            }
            return false;
        }

        public int getPosition(){
            return CurrPos;
        }

        public int getDataLength(){
            return DataLength;
        }

        public double[] getX(){
            return (double [])xData.clone();
        }

        public double[] getY(){
            return (double [])yData.clone();
        }

        public boolean setLength(int length){
            if (DataLength != 0)
                return false;
            if (length > 0){
                DataLength = length;
                xData = new double[DataLength];
                yData = new double[DataLength];
                return true;
            }
            return false;
        }

        public void OverrideLength(int length){
            if (length == 0){
                xData = null;
                yData = null;
                return;
            }
            DataLength = length;
            xData = new double[DataLength];
            yData = new double[DataLength];
            return;
        }
    }
    public static void main(final String... args) {
        String path;
        ImagePlus imp, stim_stack = null;
        path = "C:\\Users\\noambox\\Documents\\Sync\\Neural data analysis\\Test ED out\\for recenter test"; // LAB
        imp = IJ.openImage(path + "\\image2.tif");
        String roi_path = "C:\\Users\\noambox\\Documents\\Sync\\Neural data analysis\\Test ED out\\for recenter test\\RoiSet_input.zip";
        imp.show();
        IJ.run("In [+]", "");
        IJ.run("In [+]", "");
        Time_Series_Analyzer_V3 tta3 = new Time_Series_Analyzer_V3();

        // Recenter Test implementation
//        ArrayList<PolygonRoi> rois = AAP_woStimulus.loadROIZip(roi_path);
//        tta3.recenter(imp, 1, rois);
    }
}

