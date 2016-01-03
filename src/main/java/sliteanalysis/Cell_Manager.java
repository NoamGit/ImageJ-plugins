package sliteanalysis;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.awt.List;
import java.util.stream.Collectors;
import java.util.zip.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

import com.sun.tools.classfile.Opcode;
import com.sun.xml.internal.ws.api.model.ExceptionType;
import ij.*;
import ij.measure.Calibration;
import ij.plugin.PointToolOptions;
import ij.process.*;
import ij.gui.*;
import ij.io.*;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.plugin.filter.PlugInFilter;
import ij.plugin.filter.Analyzer;
import ij.plugin.frame.PlugInFrame;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.awt.datatransfer.*;

/* Cell manager class */
class CellManager extends PlugInFrame implements ActionListener, ItemListener,
        ClipboardOwner, KeyListener, Runnable {

    Panel panel;
    static Frame instance;
    ImagePlus avr_imp;
    ArrayList<Double> stimulus1D = new ArrayList<>();
    double dt_r = 0;
    Overlay overlay;
    ImagePlus imp;
    JTable table;
    CellManagerTableModel tmodel;
    Checkbox coordinates, center, slices;
    CheckboxGroup labelType;
    boolean done = false;
    Canvas previousCanvas = null;
    Thread thread;
    JFileChooser fc;

    public CellManager(ImagePlus avr_imp, ImagePlus imp, Overlay pOverlay,double dt) {
        super("Cell Manager");
        if (instance!=null) {
            instance.toFront();
            return;
        }
        instance = this;
        setLayout(new FlowLayout(FlowLayout.CENTER,5,5));
        //int rows = 28;//was 25
        //list = new List(rows, true);
        //list.add("012345678901234567");
        //list.addItemListener(this);
        //add(list);
        int twidth = 200;
        int theight = 450;
        this.dt_r = dt;
        this.avr_imp = avr_imp;
        this.overlay = pOverlay;
        this.imp = imp;
        tmodel = new CellManagerTableModel();
        table = new JTable(tmodel);
        table.setPreferredScrollableViewportSize(new Dimension(twidth, theight));
        table.setShowGrid(false);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);

    /*    // create status colum for activity display
        TableColumn statusCol = new TableColumn();
        statusCol.setHeaderValue("Status");
        table.addColumn(statusCol);*/

        // Set the width of the first and last column (here we can decide what data to show in table)
        table.getColumnModel().getColumn(0).setPreferredWidth(25); // #
        table.getColumnModel().getColumn(2).setMinWidth(0);
        table.getColumnModel().getColumn(2).setMaxWidth(0);
        table.getColumnModel().getColumn(2).setPreferredWidth(0);
        table.getColumnModel().getColumn(3).setMinWidth(0);
        table.getColumnModel().getColumn(3).setMaxWidth(0);
        table.getColumnModel().getColumn(3).setPreferredWidth(0);
        table.getColumnModel().getColumn(4).setPreferredWidth(25); // Var
        table.getColumnModel().getColumn(5).setMinWidth(0); // stimulus
        table.getColumnModel().getColumn(5).setMaxWidth(0);
        table.getColumnModel().getColumn(5).setPreferredWidth(0);


        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane);

        ListSelectionModel rowSM = table.getSelectionModel();
        rowSM.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                ListSelectionModel lsm = (ListSelectionModel)e.getSource();
                if (lsm.isSelectionEmpty()) {// Do nothing
                } else {
                    int selectedRow = lsm.getMinSelectionIndex();
                    restore(selectedRow);
                }
            }
        });

        panel = new Panel();
        panel.setLayout(new GridLayout(21, 1, 1, 1));// was GridLayout(16, 1, 1, 1))
//        addButton("Add <SP>");
//        addButton("Add+Draw<CR>");
//        addButton("Add Particles");
//        addButton("Update");
//        addButton("Measure");
//        addButton("Draw");
//        addButton("Fill");
        addButton("Delete");
        addButton("Open");
        addButton("Open All");
        addButton("Save RAW");
        addButton("Save DF");
        addButton("Toggle Select All");// was 'Select All'
        addButton("Add Cell");
        addButton("Magic Add Cell");
        addButton("RAW Signal");
        addButton("DF/F");
        addButton("Multi");
        addButton("Label All ROIs");
        addButton("Copy List");

        //Checkboxes

        labelType = new CheckboxGroup();
        panel.add(new Label("Labels:"));

        center = new Checkbox("Center");
        center.setCheckboxGroup(labelType);
        panel.add(center);

        coordinates = new Checkbox("Coord.");
        coordinates.setCheckboxGroup(labelType);
        panel.add(coordinates);
        center.setState(Prefs.get("multimeasure.center", true));
        coordinates.setState(!Prefs.get("multimeasure.center", true));

        panel.add(new Label("Multi Option:"));
        slices = new Checkbox("Label Slices");
        panel.add(slices);
        slices.setState(Prefs.get("multimeasure.slices", false));


        add(panel);

        pack();
        //list.delItem(0);
        GUI.center(this);
        show();
        thread = new Thread(this, "Multi_Measure");
        thread.start();
    }

    public CellManager(ImagePlus avr_imp, ImagePlus imp, Overlay pOverlay, ArrayList<Double> stimulus1D, double dt) {
        super("Cell Manager");
        if (instance!=null) {
            instance.toFront();
            return;
        }
        instance = this;
        setLayout(new FlowLayout(FlowLayout.CENTER,5,5));
        int twidth = 200;
        int theight = 450;

        this.dt_r = dt;
        this.avr_imp = avr_imp;
        this.overlay = pOverlay;
        this.imp = imp;
        this.stimulus1D = stimulus1D;
        tmodel = new CellManagerTableModel();
        table = new JTable(tmodel);
        table.setPreferredScrollableViewportSize(new Dimension(twidth, theight));
        table.setShowGrid(false);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);

    /*    // create status colum for activity display
        TableColumn statusCol = new TableColumn();
        statusCol.setHeaderValue("Status");
        table.addColumn(statusCol);*/

        // Set the width of the first and last column
        table.getColumnModel().getColumn(0).setPreferredWidth(25); // #
        table.getColumnModel().getColumn(2).setMinWidth(0);
        table.getColumnModel().getColumn(2).setMaxWidth(0);
        table.getColumnModel().getColumn(2).setPreferredWidth(0);
        table.getColumnModel().getColumn(3).setMinWidth(0);
        table.getColumnModel().getColumn(3).setMaxWidth(0);
        table.getColumnModel().getColumn(3).setPreferredWidth(0);
        table.getColumnModel().getColumn(4).setPreferredWidth(25); // Var
        table.getColumnModel().getColumn(5).setMinWidth(0); // stimulus
        table.getColumnModel().getColumn(5).setMaxWidth(0);
        table.getColumnModel().getColumn(5).setPreferredWidth(0);


        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane);

        ListSelectionModel rowSM = table.getSelectionModel();
        rowSM.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                ListSelectionModel lsm = (ListSelectionModel)e.getSource();
                if (lsm.isSelectionEmpty()) {// Do nothing
                } else {
                    int selectedRow = lsm.getMinSelectionIndex();
                    restore(selectedRow);
                }
            }
        });

        panel = new Panel();
        panel.setLayout(new GridLayout(21, 1, 1, 1));// was GridLayout(16, 1, 1, 1))
//        addButton("Add <SP>");
//        addButton("Add+Draw<CR>");
//        addButton("Add Particles");
//        addButton("Update");
//        addButton("Measure");
//        addButton("Draw");
//        addButton("Fill");
        addButton("Delete");
        addButton("Open");
        addButton("Open All");
        addButton("Save RAW");
        addButton("Save DF");
        addButton("Toggle Select All");// was 'Select All'
        addButton("Add Cell");
        addButton("Magic Add Cell");
        addButton("RAW Signal");
        addButton("DF/F");
        addButton("Multi");
        addButton("Label All ROIs");
        addButton("Copy List");

        //Checkboxes

        labelType = new CheckboxGroup();
        panel.add(new Label("Labels:"));

        center = new Checkbox("Center");
        center.setCheckboxGroup(labelType);
        panel.add(center);

        coordinates = new Checkbox("Coord.");
        coordinates.setCheckboxGroup(labelType);
        panel.add(coordinates);
        center.setState(Prefs.get("multimeasure.center", true));
        coordinates.setState(!Prefs.get("multimeasure.center", true));

        panel.add(new Label("Multi Option:"));
        slices = new Checkbox("Label Slices");
        panel.add(slices);
        slices.setState(Prefs.get("multimeasure.slices", false));


        add(panel);

        pack();
        //list.delItem(0);
        GUI.center(this);
        show();
        thread = new Thread(this, "Multi_Measure");
        thread.start();
    }

    public void run() {
        while (!done) {
            try {Thread.sleep(500);}
            catch(InterruptedException e) {}
            ImagePlus avr_imp = WindowManager.getCurrentImage();
            if (avr_imp != null){
                ImageWindow win = avr_imp.getWindow();
                ImageCanvas canvas = win.getCanvas();
                if (canvas != previousCanvas){
                    if(previousCanvas != null)
                        previousCanvas.removeKeyListener(this);
                    canvas.addKeyListener(this);
                    previousCanvas = canvas;
                }
            }
        }
    }

    public void windowClosing(WindowEvent e) {
        super.windowClosing(e);
        done = true;
    }

    void addButton(String label) {
        Button b = new Button(label);
        b.addActionListener(this);
        switch(label) {
            case "Add Cell":
                b.setBackground(Color.getHSBColor((float) 0.6212122,(float) 0.36065573, (float) 0.95686275));
                break;
            case "DF/F":
                b.setBackground(Color.getHSBColor((float) 0,(float) 0.36032388, (float) 0.96862745));
                break;
            case "RAW Signal":
                b.setBackground(Color.getHSBColor((float) 0,(float) 0.36032388, (float) 0.96862745));
                break;
            case "Magic Add Cell":
                b.setBackground(Color.getHSBColor((float) 0.6212122,(float) 0.36065573, (float) 0.95686275));
                break;
            default:
                break;
        }
        panel.add(b);
    }

    public void actionPerformed(ActionEvent e) {
        String label = e.getActionCommand();
        if (label==null)
            return;
        String command = label;
        if (command.equals("Add <SP>"))
            add();
        if (command.equals("Add+Draw<CR>"))
            addAndDraw();
        if (command.equals("Add Particles"))
            addParticles();
        else if (command.equals("Update"))
            updateActiveRoi();
        else if (command.equals("Delete"))
            delete();
        else if (command.equals("Open"))
            open(null);
        else if (command.equals("Open All"))
            openAll();
        else if (command.equals("Save DF"))
            save("processed");
        else if (command.equals("Save RAW"))
            save("raw");
        else if (command.equals("Toggle Select All"))// was 'Select All'
            selectAll();
        else if (command.equals("Measure"))
            measure();
        else if (command.equals("Add Cell"))
            additionalCell();
        else if (command.equals("Magic Add Cell"))
            addWithMagicWandCell();
        else if (command.equals("RAW Signal"))
            dfOverF("raw");
        else if (command.equals("DF/F"))
            dfOverF("df");
        else if (command.equals("Multi"))
            multi();
        else if (command.equals("Draw"))
            draw();
        else if (command.equals("Fill"))
            fill();
        else if (command.equals("Label All ROIs"))
            labelROIs();
        else if (command.equals("Copy List"))
            copyList();
    }


    public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange()==ItemEvent.SELECTED
                && WindowManager.getCurrentImage()!=null) {
            int index = 0;
            try {index = Integer.parseInt(e.getItem().toString());}
            catch (NumberFormatException ex) {}
            restore(index);
        }
    }

    public void addCell(CalciumSignal cas, Roi roi){
    /* Adds the roi of an calcium singal with its properties*/
        String type = "Cell ";
        Rectangle r = roi.getBoundingRect();
        //String label = type+" ("+(r.x+r.width/2)+","+(r.y+r.height/2)+")";

        String label = null;
        if(center.getState())
            label = type+(r.x+r.width/2)+"-"+(r.y+r.height/2);
        else
            label =  type+".x"+ (r.x)+".y"+(r.y)+".w"+(r.width)+".h"+(r.height);
        //list.add(label);
        //rois.put(label, roi.clone());
        tmodel.addRoi(label, roi.clone(), cas);

//        table.setValueAt(Double.toString(cas.activityVariance), tmodel.numRows - 1, 4);
//        table.setValueAt("test", tmodel.numRows - 1,4);

    }

    /*Add Cell with Magic wand*/
    public void addWithMagicWandCell() throws MissingResourceException {

        // get Roi Selection
        float[] xpoints ,ypoints;
        xpoints = ypoints = null;
        try {
            xpoints = IJ.getImage().getRoi().getFloatPolygon().xpoints;
            ypoints = IJ.getImage().getRoi().getFloatPolygon().ypoints;
            if( !IJ.getImage().getRoi().getTypeAsString().equals("Point") )
                throw new DataFormatException();
        }
        catch(NullPointerException e){
            IJ.error("No Roi is selected...");
            return;
        }
        catch (DataFormatException e) {
            IJ.error("Roi type is not poit/multipoints...");
            return;
        }
        Cell_Magic_Wand_Tool cmw = new Cell_Magic_Wand_Tool();
        for (int i = 0; i <xpoints.length; i++) {
            PolygonRoi roi = cmw.makeRoi((int) xpoints[i], (int) ypoints[i], avr_imp);
            // extract Ca signal from roi and process
            float[] values = getRoiSignal(roi);
            CalciumSignal ca_sig = new CalciumSignal(values,this.dt_r);
            ca_sig.DeltaF();

            // add cell location and signal to list
            try{
                this.addCell(ca_sig, roi);
                this.overlay.add(roi);
                this.overlay.setLabelColor(Color.WHITE);
//                this.overlay.setStrokeColor(Color.GREEN);
                this.overlay.setStrokeColor(Color.getHSBColor((float) 0.1666,(float) 0.651,(float) 1));
                this.avr_imp.setOverlay(this.overlay);
            } catch(Exception e){
                IJ.showMessage(e.getMessage());
            }
            this.avr_imp.show();
        }
    }

    public void additionalCell(){
    /* Add's Selection based ROI cell to table*/

        // get Roi Selection
        Roi roi = null;
        roi = avr_imp.getRoi();
        if(roi == null)
                roi = imp.getRoi();
        if(roi == null){
                IJ.log("No ROI was selected...");
                return;
        }

        // extract Ca signal from roi and process
        float[] values = getRoiSignal(roi);
        CalciumSignal ca_sig = new CalciumSignal(values,this.dt_r);
        ca_sig.DeltaF();

        // add cell location and signal to list
        try{
            this.addCell(ca_sig, roi);
            this.overlay.add(roi);
            this.overlay.setLabelColor(Color.WHITE);
//            this.overlay.setStrokeColor(Color.GREEN);
            this.overlay.setStrokeColor(Color.getHSBColor((float) 0.1666,(float) 0.651,(float) 1));
            this.avr_imp.setOverlay(this.overlay);
            this.avr_imp.show();
        } catch(Exception e){
            IJ.showMessage(e.getMessage());
        }
    }

    boolean add() {
        ImagePlus avr_imp = getImage();
        if (avr_imp==null)
            return false;
        Roi roi = avr_imp.getRoi();
        if (roi==null) {
            error("The active image does not have an ROI.");
            return false;
        }
        String type = null;
        switch (roi.getType()) {
            case Roi.RECTANGLE: type ="R"; break;
            case Roi.OVAL: type = "O"; break;
            case Roi.POLYGON: type = "PG"; break;
            case Roi.FREEROI: type = "FH"; break;
            case Roi.TRACED_ROI: type = "T"; break;
            case Roi.LINE: type = "L"; break;
            case Roi.POLYLINE: type = "PL"; break;
            case Roi.FREELINE: type = "FL"; break;
        }
        if (type==null)
            return false;
        Rectangle r = roi.getBoundingRect();
        //String label = type+" ("+(r.x+r.width/2)+","+(r.y+r.height/2)+")";

        String label = null;
        if(center.getState())
            label = type+(r.x+r.width/2)+"-"+(r.y+r.height/2);
        else
            label =  type+".x"+ (r.x)+".y"+(r.y)+".w"+(r.width)+".h"+(r.height);
        //list.add(label);
        //rois.put(label, roi.clone());
        tmodel.addRoi(label, roi.clone());

        return true;
    }

    boolean addParticles() {
        ImagePlus avr_imp = getImage();
        if (avr_imp==null)
            return false;
        ResultsTable rt = Analyzer.getResultsTable();
        if(rt == null){
            IJ.showMessage("Add Particles requres Analyze -> Analyze Particles to \n"+
                    "be run first with Record Stats selected.");
            return false;
        }
        int nP = rt.getCounter();
        if(nP == 0)
            return false;
        int xCol = rt.getColumnIndex("XStart");
        int yCol = rt.getColumnIndex("YStart");
        if((xCol == ResultsTable.COLUMN_NOT_FOUND)||(yCol == ResultsTable.COLUMN_NOT_FOUND)){
            IJ.showMessage("Add Particles requres Analyze -> Analyze Particles to \n"+
                    "be run first with Record Stats selected.");
            return false;
        }
        ImageProcessor ip = avr_imp.getProcessor();
        int tMin = (int)ip.getMinThreshold();
        for (int i = 0; i < nP; i++){
            Wand w = new Wand(ip);
            int xStart = (int)rt.getValue("XStart", i);
            int yStart = (int)rt.getValue("YStart", i);
            if (tMin==ip.NO_THRESHOLD)
                w.autoOutline(xStart, yStart);
            else
                w.autoOutline(xStart, yStart, (int)tMin, (int)ip.getMaxThreshold());
            if (w.npoints>0) {
                Roi roi = new PolygonRoi(w.xpoints, w.ypoints, w.npoints, Roi.TRACED_ROI);
                Rectangle r = roi.getBoundingRect();

                String label = null;
                if(center.getState())
                    label = "PG"+(r.x+r.width/2)+"-"+(r.y+r.height/2);
                else
                    label =  "PG"+".x"+ (r.x)+".y"+(r.y)+".w"+(r.width)+".h"+(r.height);
                //list.add(label);
                //rois.put(label, roi.clone());
                tmodel.addRoi(label, roi.clone());
            }
        }
        return true;
    }

    void addAndDraw() {
        boolean roiadded = add();
        if (roiadded) {
            //list.select(//list.getItemCount()-1);
            int i = table.getRowCount() - 1;
            table.setRowSelectionInterval(i,i);
            draw();
        }
        IJ.run("Restore Selection");
    }

    boolean delete() {

        // first delete from point rois
        float[] xpoints ,ypoints;
        xpoints = ypoints = null;
        try {
            if( IJ.getImage().getRoi().getTypeAsString().equals("Point") ) { // equals("Rectangle")
                xpoints = IJ.getImage().getRoi().getFloatPolygon().xpoints;
                ypoints = IJ.getImage().getRoi().getFloatPolygon().ypoints;
                int x, y;
//                ArrayList<Roi> roiArray  = new ArrayList<>();
//                ArrayList<Integer> indArray = new ArrayList<>();
//                for (int i = 0; i < this.tmodel.getRowCount(); i++) {
//                    roiArray.add((Roi) this.tmodel.getValueAt(i, 2));
//                    indArray.add((Integer) this.tmodel.getValueAt(i,0));
//                }
//                for (int j = 0; j < xpoints.length; j++) { // for each Roi Point
//                    x = (int) xpoints[j];
//                    y = (int) ypoints[j];
//                    Collections.reverse(roiArray);
//                    Collections.reverse(indArray);
//                    Iterator itrRoi = roiArray.iterator();
//                    Iterator itrInd = indArray.iterator();
//                    while (itrRoi.hasNext() & itrInd.hasNext()) {
//                        if (((Roi) itrRoi.next()).contains(x, y)) {
//                            int indx = (Integer) itrInd.next();
//                            this.overlay.remove(this.overlay.get(indx));
//                            this.avr_imp.setOverlay(this.overlay);
//                            tmodel.removeRoi(indx);
//                            itrRoi.remove();
//                            itrInd.remove();
//                        }
//                        else // skip this sample
//                            itrInd.next();
//                    }
//                }
//                this.avr_imp.show();
//                return true; // deleted from image

                Roi[] roiArray = new Roi[this.tmodel.getRowCount()];
                for (int i = 0; i < this.tmodel.getRowCount(); i++) {
                    roiArray[i] = (Roi) this.tmodel.getValueAt(i, 2);
                }
                ArrayList<Integer> indx2remove = new ArrayList<>();
                for (int j = 0; j < xpoints.length; j++) { // for each Roi Point
                    x = (int) xpoints[j];
                    y = (int) ypoints[j];
                    for (int i = roiArray.length - 1; i >= 0 ; i--) {
                        if (roiArray[i].contains(x, y)) { // clears all selected x, y that are in a roi bottom up
                            indx2remove.add(i);
                        }
                    }
                }

                // Create a list with the distinct elements using stream.
                Collections.sort(indx2remove);
                Iterator itr_ind = indx2remove.iterator();
                ArrayList<Integer> unique_list = new ArrayList<>();
                while (itr_ind.hasNext()){
                    Integer k = (Integer) itr_ind.next();
                    if(!unique_list.contains(k))
                        unique_list.add(k);
                }
                Collections.reverse(unique_list);
                Iterator itr_unq = unique_list.iterator();
                while (itr_unq.hasNext()){
                    int k = (int) itr_unq.next();
                    this.overlay.remove(this.overlay.get(k));
                    this.avr_imp.setOverlay(this.overlay);
                    tmodel.removeRoi(k);
                    roiArray[k] =  new Roi(new Rectangle(0,0,0,0)); // "nullifies" roi
                }

//                this.overlay.remove(this.overlay.get(i));
//                this.avr_imp.setOverlay(this.overlay);
//                tmodel.removeRoi(i);
//                roiArray[i] =  new Roi(new Rectangle(0,0,0,0)); // "nullifies" roi

                this.avr_imp.show();
                return true; // deleted from image

            }
            else
                throw new NullPointerException();
        }
        catch(NullPointerException e){
            IJ.showStatus("No Point ROIs, deleting only from Jtable...");
        }

        // if no point rois delete from table
        if(table.getRowCount() == 0) {
            // was if (list.getItemCount()==0)
            IJ.showStatus("The ROI list is empty.");
            return false;
        }
        int index[] = table.getSelectedRows(); // was list.getSelectedIndexes();
        if (index.length==0)
            return error("At least one ROI in the list or on the Image must be selected.");
        for (int i=index.length-1; i>=0; i--) {
            String label = (String)tmodel.getValueAt(index[i],1);
            //rois.remove(label);
            //list.delItem(index[i]);
            this.overlay.remove(this.overlay.get(index[i]));
            this.avr_imp.setOverlay(this.overlay);
            this.avr_imp.show();
            tmodel.removeRoi(index[i]);
        }
        return true;
    }

    boolean restore(int index) {
        //String label = list.getItem(index);
        //String label = (String)tmodel.getValueAt(index,1);
        //Roi roi = (Roi)rois.get(label);
        Roi roi = (Roi)tmodel.getValueAt(index,2);
        ImagePlus avr_imp = getImage();
        if (avr_imp==null)
            return error("No image selected.");
        Rectangle r = roi.getBoundingRect();
        if (r.x+r.width>avr_imp.getWidth() || r.y+r.height>avr_imp.getHeight())
            return error("This ROI does not fit the current image.");
        avr_imp.setRoi(roi);
        return true;
    }

	/* These open and save methods are replaced by the methods of the integrated ROI manager of ImageJ.
	 * They provide better functionality and should ease merging this manager into ImageJ
	 */
/*	void open() {
		Macro.setOptions(null);
		OpenDialog od = new OpenDialog("Open ROI...", "");
		String directory = od.getDirectory();
		String name = od.getFileName();
		String label;

		if(name.endsWith(".roi")){ label = name.substring(0, name.lastIndexOf(".roi")); }
		else{ label = name; }

		if (name==null)
			return;
		String path = directory + name;
		Opener o = new Opener();
		Roi roi = o.openRoi(path);
		if (roi!=null) {
			//list.add(name);
			//rois.put(name, roi);
			tmodel.addRoi(label, roi);
		}
	}


	void openAll() {
		Macro.setOptions(null);
		Macro.setOptions(null);
		OpenDialog od = new OpenDialog("Select a file in the folder...", "");
		if (od.getFileName()==null) return;
		String dir  = od.getDirectory();
		String[] files = new File(dir).list();
		String name, label;
		if (files==null) return;
		for (int i=0; i<files.length; i++) {
			File f = new File(dir+files[i]);
			if (!f.isDirectory() && files[i].endsWith(".roi")) {
				Roi roi = new Opener().openRoi(dir+files[i]);
				if (roi!=null) {
					name = files[i];
					if(name.endsWith(".roi")){ label = name.substring(0, name.lastIndexOf(".roi")); }
					else{ label = name; }
					//list.add(files[i]);
					//rois.put(files[i], roi);
					tmodel.addRoi(label, roi);
				}
			}
		}
	}

	boolean save() {
		if(table.getRowCount() == 0)
			// was if (list.getItemCount()==0)
			return error("The ROI list is empty.");
		int index[] = table.getSelectedRows();//list.getSelectedIndexes();
		if (index.length==0)
			return error("At least one ROI in the list must be selected.");
		String name = (String)tmodel.getValueAt(index[0],1);
		// was String name = list.getItem(index[0]);
		Macro.setOptions(null);
		SaveDialog sd = new SaveDialog("Save ROI...", name, ".roi");
		name = sd.getFileName();
		if (name == null)
			return false;
		String dir = sd.getDirectory();
		for (int i=0; i<index.length; i++) {
			if (restore(index[i])) {
				if (index.length>1)
					name = (String)tmodel.getValueAt(index[i],1) + ".roi";
				//was list.getItem(index[i])+".roi";
				//IJ.run("ROI...", "path='"+dir+name+"'");
				IJ.saveAs("Selection", dir+name);
			} else
				break;
		}
		return true;
	}*/

    /*
     * These open save methods are taken from the integrated ROI manager of ImageJ written by Wayne Rasband
     */
    void open(String path) {
        Macro.setOptions(null);
        String name = null;
        if (path==null) {
            OpenDialog od = new OpenDialog("Open Selection(s)...", "");
            String directory = od.getDirectory();
            name = od.getFileName();
            if (name==null)
                return;
            path = directory + name;
        }
        //if (Recorder.record) Recorder.record("roiManager", "Open", path);
        if (path.endsWith(".zip")) {
            openZip(path);
            return;
        }
        Opener o = new Opener();
        if (name==null) name = o.getName(path);
        Roi roi = o.openRoi(path);
        if (roi!=null) {
            if (name.endsWith(".roi"))
                name = name.substring(0, name.length()-4);
            name = getUniqueName(name);
            //list.add(name);
            //rois.put(name, roi);
            tmodel.addRoi(name, roi);
        }
    }
    /*
     * Quite a few things have been changed by Ulrik Stervbo in this method compared to the original
     * The original method is found below this method
     *
     * This is what I've done:
     *      Only read .roi files in the zip-file
     *      Do not delete existing entrie in the manager
     *
     * If we delete the current entries one cannot open rois from different sources (co-workers and such)
     */
    void openZip(String path) {
        ZipInputStream in = null;
        ByteArrayOutputStream out;
        boolean noFilesOpened = true; // we're pessimistic and expect that the zip file dosent contain any .roi
        try {
            in = new ZipInputStream(new FileInputStream(path));
            byte[] buf = new byte[1024];
            int len;
            // The original while was: while(true) do something which is not very good
            ZipEntry entry = in.getNextEntry();
            while (entry!=null) {
				/* If we try to open a non-roi file an error is thrown and nothing is opened into
				 * the Roi manager - not a very nice thing to do! Of course we'd expect the zip file to
				 * contain nothing but .roi files, but who knows what users do?
				 *
				 * The easy solution to this problem is to open only .roi files in the zip file.
				 * Another solution is to play with the getRoi of the RoiDecoder. This solution is more
				 * difficult and may not better in a general perspective.
				 *
				 * At any rate I'm a lazy b'stard - I only open files if they end with '.roi'
				 */

                String name = entry.getName();
                if (name.endsWith(".roi")) {
                    out = new ByteArrayOutputStream();
                    while ((len = in.read(buf)) > 0)
                        out.write(buf, 0, len);
                    out.close();
                    byte[] bytes = out.toByteArray();
                    RoiDecoder rd = new RoiDecoder(bytes, name);
                    Roi roi = rd.getRoi();
                    if (roi!=null) {
                        name = name.substring(0, name.length()-4);

                        name = getUniqueName(name);
                        tmodel.addRoi(name, roi);
                        noFilesOpened = false; // We just added a .roi
                    }
                }
                entry = in.getNextEntry();
            }
            in.close();
        } catch (IOException e) { error(e.toString()); }
        if(noFilesOpened){ error("This ZIP archive does not appear to contain \".roi\" files"); }
    }

	/*
	 * The original openZip methig before Ulrik started playing and messing about
	 */
/*    void openZip(String path) {
        ZipInputStream in = null;
        ByteArrayOutputStream out;
        try {
            in = new ZipInputStream(new FileInputStream(path));
            byte[] buf = new byte[1024];
            int len;
            boolean firstTime = true;
            while (true) {
                ZipEntry entry = in.getNextEntry();
                if (entry==null)
                    {in.close(); return;}
                String name = entry.getName();
                if (!name.endsWith(".roi")) {
                    error("This ZIP archive does not appear to contain \".roi\" files");
                }
                out = new ByteArrayOutputStream();
                while ((len = in.read(buf)) > 0)
                    out.write(buf, 0, len);
                out.close();
                byte[] bytes = out.toByteArray();
                RoiDecoder rd = new RoiDecoder(bytes, name);
                Roi roi = rd.getRoi();
                if (roi!=null) {
                    if (firstTime) {
                        if (list.getItemCount()>0) delete(true);
                        if (canceled)
                            {in.close(); return;}
                        firstTime = false;
                    }
                    if (name.endsWith(".roi"))
                        name = name.substring(0, name.length()-4);
                    name = getUniqueName(name);
                    list.add(name);
                    rois.put(name, roi);
                }
            }
        } catch (IOException e) {
            error(""+e);
        }
    }*/

    void openAll() {
        IJ.setKeyUp(KeyEvent.VK_ALT);
        Macro.setOptions(null);
        // The original code contained a bug around here
        // my solution was to remove
        // String dir  = IJ.getDirectory("Open All...");
        // and add:
        OpenDialog od = new OpenDialog("Select a file in the folder...", "");
        String dir  = od.getDirectory();
        if (dir==null) return;
        String[] files = new File(dir).list();
        if (files==null) return;
        for (int i=0; i<files.length; i++) {
            File f = new File(dir+files[i]);
            if (!f.isDirectory() && files[i].endsWith(".roi")) {
                Roi roi = new Opener().openRoi(dir+files[i]);
                if (roi!=null) {
                    String name = files[i];
                    if (name.endsWith(".roi"))
                        name = name.substring(0, name.length()-4);
                    name = getUniqueName(name);
                    //list.add(name);
                    //rois.put(name, roi);
                    tmodel.addRoi(name, roi);
                }
            }
        }
    }

    String getUniqueName(String name) {
        String name2 = name;
        int n = 1;
        int rownum = tmodel.getRowNumber(name2, 1);

        while(rownum != -1){
            name2 = name+"-"+n;
            n++;
            rownum = tmodel.getRowNumber(name2,1);
        }
/*		int n = 1;
		Roi roi2 = (Roi)rois.get(name2);
		while (roi2!=null) {
			roi2 = (Roi)rois.get(name2);
			if (roi2!=null)
				name2 = name+"-"+n;
			n++;
			roi2 = (Roi)rois.get(name2);
		}*/
        return name2;
    }

    boolean save(String saveType) {
        if (table.getRowCount() == 0) // was if (list.getItemCount()==0)
            return error("The list is empty."); // was "The selection list is empty"
        int indexes[] = table.getSelectedRows();//list.getSelectedIndexes();
        JFileChooser fc = new JFileChooser();
        // I dont get this - first we say: if nothing is selected, say so and then we select all items.
        // what is the point in that?
        // if (indexes.length==0)
        //	indexes = getAllIndexes();
        if (indexes.length == 0) {
            error("At least one ROI must be selected from the list.");
            return false;
        } else {
            String nameData = "DataProcessed_" + imp.getTitle().subSequence(0, imp.getTitle().length() - 4) + ".xlsx";;
            if (saveType.compareTo("raw") == 0){
                nameData = "DataRaw_" + imp.getTitle().subSequence(0, imp.getTitle().length() - 4) + ".xlsx";
            }
            String nameRoi = "RoiSet_" + imp.getTitle().subSequence(0, imp.getTitle().length() - 4) + ".zip";
            Macro.setOptions(null);
            SaveDialog sd = new SaveDialog("Save Signals...", nameData, ".xlsx");
            String dir = sd.getDirectory();
            nameData = sd.getFileName();
//            int returnVal = fc.showSaveDialog(CellManager.this);
//            saveMultiple(indexes, fc.getSelectedFile().getPath());
            saveMultiple(indexes, dir, nameRoi, nameData, saveType);
            return true;
        }
//
//        String name = (String) tmodel.getValueAt(indexes[0],1); // was list.getItem(indexes[0]);
//        Macro.setOptions(null);
//        SaveDialog sd = new SaveDialog("Save Selection...", name, ".roi");
//        String name2 = sd.getFileName();
//        if (name2 == null)
//            return false;
//        // The user has changed the name of the file so the name of the ROI should also change!
//        String dir = sd.getDirectory();
//        String newName;
//        // If the new name ends with '.roi' it must be removed
//        if(name2.endsWith(".roi")){ newName = name2.substring(0, name2.length()-4); }
//        else{ newName = name2; }
//        int rownum = tmodel.getRowNumber(name,1);
//        // If nothing was found a -1 is returned - better make some use of it!
//        if(rownum == -1){ return error("No entry matching " + name + " was found."); }
//
//        // OK - we're all good! Update the entry
//        tmodel.updateRoi(rownum, newName, tmodel.getValueAt(rownum, 2), (Boolean) tmodel.getValueAt(rownum,3));
//
//        // Before I changed things is looked like this
//        //Roi roi = (Roi)rois.get(name);
//        //rois.remove(name);
//        //if (!name2.endsWith(".roi")) name2 = name2+".roi";
//        //String newName = name2.substring(0, name2.length()-4);
//        //rois.put(newName, roi);
//        //roi.setName(newName);
//        //list.replaceItem(newName, indexes[0]);
//        if (restore(indexes[0]))
//            IJ.run("Selection...", "path='"+dir+newName+".roi'");
//        return true;
    }

    void saveMultiple(int[] indexes, String path, String nameRoi, String nameData, String saveType) {
        Macro.setOptions(null);
        if (path==null) {
            SaveDialog sd = new SaveDialog("Save ROIs...", "RoiSet", ".zip");
            String name = sd.getFileName();
            if (name == null)
                return;
            String dir = sd.getDirectory();
            path = dir+name;
        }
        try {
            // save ROI to ZIP
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(path+nameRoi));
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(zos));
            RoiEncoder re = new RoiEncoder(out);
            String name = "";
            CalciumSignal cas = new CalciumSignal();
            Roi roi = null;

            // Save Roi
            for (int i=0; i<indexes.length; i++) {
                //String label = list.getItem(indexes[i]);
                //Roi roi = (Roi)rois.get(label);
                //if (!label.endsWith(".roi")) label += ".roi";
                //zos.putNextEntry(new ZipEntry(label));
                // Get the name of the roi and the roi
                name = (String)tmodel.getValueAt(indexes[i],1);
                roi = (Roi)tmodel.getValueAt(indexes[i],2);
                zos.putNextEntry(new ZipEntry(name + ".roi"));
                re.write(roi);
                out.flush();
            }
            out.close();

            // Save signals with apache POI
            File myFile = new File(path+nameData);
            XSSFWorkbook myWorkBook = new XSSFWorkbook(); // Return first sheet from the XLSX workbook
            XSSFSheet mySheet = myWorkBook.createSheet(); // Get iterator to all the rows in current sheet

            int rownum = 0;
            if(saveType.compareTo("processed") == 0){
                cas = (CalciumSignal) tmodel.getValueAt(indexes[0],3);
                int signalSize = cas.signalSize();
                for (int rowNum =0; rowNum<signalSize; rowNum++) { // for signal size
                    Row row = mySheet.createRow(rownum++); // create row
                    int cellnum = 0;

                    // write headers in first column
                    if(rowNum == 0){
                        Cell cell = row.createCell(cellnum++);
                        cell.setCellValue("Time");
                        cell = row.createCell(cellnum++);
                        cell.setCellValue("Stim");
                        for (int colNum=0; colNum<indexes.length; colNum++) { // for number of cells
                            cell = row.createCell(cellnum++);
                            cell.setCellValue((String)tmodel.getValueAt(indexes[colNum],1));
                        }
                        continue;
                    }

                    // next start plugin in values
                    Cell cell;
                    cell = row.createCell(cellnum++);
                    cell.setCellValue((rowNum-1) * cas.getdt()); // set time
                    cell = row.createCell(cellnum++);
                    // TODO insert stimulus
                    cell.setCellValue(this.stimulus1D.get(rowNum)); //set stimulus
                    for (int colNum=0; colNum<indexes.length; colNum++) { // for number of cells
                        cell = row.createCell(cellnum++);
                        CalciumSignal currentCas = (CalciumSignal) tmodel.getValueAt(indexes[colNum],3);
                        cell.setCellValue(currentCas.SignalProcessed.get(rowNum));
                    }
                }
            }

            else if(saveType.compareTo("raw") == 0){
                cas = (CalciumSignal) tmodel.getValueAt(indexes[0],3);
                int signalSize = cas.signalSize();
                for (int rowNum =0; rowNum<signalSize; rowNum++) { // for signal size
                    Row row = mySheet.createRow(rownum++); // create row
                    int cellnum = 0;

                    // write headers in first column
                    if(rowNum == 0){
                        Cell cell = row.createCell(cellnum++);
                        cell.setCellValue("Time");
                        cell = row.createCell(cellnum++);
                        cell.setCellValue("Stim");
                        for (int colNum=0; colNum<indexes.length; colNum++) { // for number of cells - sets all Ca signals in the row
                            cell = row.createCell(cellnum++);
                            cell.setCellValue((String)tmodel.getValueAt(indexes[colNum],1));
                        }
                        continue;
                    }

                    // next start plugin in values
                    Cell cell;
                    cell = row.createCell(cellnum++);
                    cell.setCellValue((rowNum-1) * cas.getdt()); // set time
                    cell = row.createCell(cellnum++);
                    // TODO insert stimulus
                    cell.setCellValue(this.stimulus1D.get(rowNum)); //set stimulus
                    for (int colNum=0; colNum<indexes.length; colNum++) { // for number of cells - sets all Ca signals in the row
                        cell = row.createCell(cellnum++);
                        CalciumSignal currentCas = (CalciumSignal) tmodel.getValueAt(indexes[colNum],3);
                        cell.setCellValue(currentCas.signalRaw.getElement(rowNum));
                    }
                }
            }

            FileOutputStream fos = new FileOutputStream(myFile);
            myWorkBook.write(fos);
            System.out.println("Writing on XLSX file Finished ...");

        }
        catch (IOException e) {
            error(""+e);
            return;
        }
        //if (Recorder.record) Recorder.record("roiManager", "Save", path);
        return;
    }

    void selectAll(){
        int selcount = table.getSelectedRowCount();  // was list.getItemCount();
        int totcount = table.getRowCount();

        if(selcount < totcount){ table.selectAll(); }
        else{ table.clearSelection(); }

        //  		for (int i=0; i<count; i++) {
        //  			if (!list.isIndexSelected(i))
        //  				allSelected = false;
        //  		}
        //  		for (int i=0; i<count; i++) {
        //  			if (allSelected)
        //  				list.deselect(i);
        //  			else
        //  				list.select(i);
        //  		}
    }

    boolean measure() {
        ImagePlus avr_imp = getImage();
        if (avr_imp==null)
            return false;
        int[] index = table.getSelectedRows();//list.getSelectedIndexes();
        if (index.length==0)
            return error("At least one ROI must be selected from the list.");

        int setup = IJ.setupDialog(avr_imp, 0);
        if (setup==PlugInFilter.DONE)
            return false;
        int nSlices = setup==PlugInFilter.DOES_STACKS?imp.getStackSize():1;
        int currentSlice = avr_imp.getCurrentSlice();
        for (int slice=1; slice<=nSlices; slice++) {
            avr_imp.setSlice(slice);
            for (int i=0; i<index.length; i++) {
                if (restore(index[i]))
                    IJ.run("Measure");
                else
                    break;
            }
        }
        avr_imp.setSlice(currentSlice);
        if (index.length>1)
            IJ.run("Select None");
        return true;
    }

    boolean dfOverF(String str) {
        int[] index = table.getSelectedRows();//list.getSelectedIndexes(); table is the table of ROI's in the GUI
        if (index.length == 0) {
            return error("At least one ROI must be selected from the list.");
        }
        int setup = IJ.setupDialog(this.avr_imp, 0);
        if (setup == PlugInFilter.DONE) {
            return false;
        }
        for (int i = 0; i < index.length; i++) {
            CalciumSignal cas;
            if (restore(index[i])) {
                cas = (CalciumSignal) tmodel.getValueAt(index[i], 3);
//                table.();
                if(cas.SignalProcessed.isEmpty()){
                    IJ.log("Signal could not be processed...");
                }
                else if (str.equals("raw")) { // DEBUGGING: for NaN exception handeling
                    cas.showSignal("Cell "+ Integer.toString(index[i]+1));
                }
                else if(str.equals("df")) {
                    cas.showSignalProccesed("Cell "+ Integer.toString(index[i]+1));
                }
            }
            else
                break;
        }
        return true;
    }

    boolean multi() {
        Prefs.set("multimeasure.center", center.getState());
        Prefs.set("multimeasure.slices", slices.getState());
        ImagePlus avr_imp = getImage();
        if (avr_imp==null)
            return false;
        int[] index = table.getSelectedRows();//list.getSelectedIndexes();
        if (index.length==0)
            return error("At least one ROI must be selected from the list.");

        int setup = IJ.setupDialog(avr_imp, 0);
        if (setup==PlugInFilter.DONE)
            return false;
        int nSlices = setup==PlugInFilter.DOES_STACKS?avr_imp.getStackSize():1;
        int currentSlice = avr_imp.getCurrentSlice();

        int measurements = Analyzer.getMeasurements();
        Analyzer.setMeasurements(measurements);
        Analyzer aSys = new Analyzer(); //System Analyzer
        ResultsTable rtSys = Analyzer.getResultsTable();
        ResultsTable rtMulti = new ResultsTable();
        Analyzer aMulti = new Analyzer(avr_imp,Analyzer.getMeasurements(),rtMulti); //Private Analyzer

        for (int slice=1; slice<=nSlices; slice++) {
            int sliceUse = slice;
            if(nSlices == 1)sliceUse = currentSlice;
            avr_imp.setSlice(sliceUse);
            rtMulti.incrementCounter();
            int roiIndex = 0;
            if(slices.getState())
                rtMulti.addValue("Slice",sliceUse);
            for (int i=0; i<index.length; i++) {
                if (restore(index[i])){
                    roiIndex++;
                    Roi roi = avr_imp.getRoi();
                    ImageStatistics stats = avr_imp.getStatistics(measurements);
                    aSys.saveResults(stats,roi); //Save measurements in system results table;
                    for (int j = 0; j < ResultsTable.MAX_COLUMNS; j++){
                        float[] col = rtSys.getColumn(j);
                        String head = rtSys.getColumnHeading(j);
                        if ((head != null)&&(col != null))
                            rtMulti.addValue(head+roiIndex,rtSys.getValue(j,rtSys.getCounter()-1));
                    }
                }
                else
                    break;
            }
            aMulti.displayResults();
            aMulti.updateHeadings();
        }

        avr_imp.setSlice(currentSlice);
        if (index.length>1)
            IJ.run("Select None");
        return true;
    }

    boolean copyList(){
        String s="";
        if(table.getRowCount() == 0)
            //was if (list.getItemCount()==0)
            return error("The ROI list is empty.");
        int index[] = table.getSelectedRows();//list.getSelectedIndexes();
        if (index.length==0)
            return error("At least one ROI in the list must be selected.");
        int numPad = numMeasurements() - 2;
        for (int i=0; i<index.length; i++) {
            if (restore(index[i])) {
                // I dont understand the purpose of this if-statement
                // seems to me that one cannot copy one single item
                //if (index.length>1){
                s +=  (String)tmodel.getValueAt(index[i],1);
                // was list.getItem(index[i]);
                if (i < (index.length-1) )
                    s += "\t";
                for (int j = 0; j < numPad; j++)
                    s += "\t";
                //}

            } else
                break;
        }
        Clipboard clip = getToolkit().getSystemClipboard();
        if (clip==null) return error("System clipboard missing");
        StringSelection cont = new StringSelection(s);
        clip.setContents(cont, this);
        return true;
    }

    public void lostOwnership (Clipboard clip, Transferable cont) {}

    int numMeasurements(){
        ResultsTable rt = Analyzer.getResultsTable();
        String headings = rt.getColumnHeadings();
        int len = headings.length();
        if (len == 0) return 0;
        int count = 0;
        for (int i = 0; i < len; i++)
            if (headings.charAt(i) == '\t') count++;
        return count;
    }

    boolean fill() {
        int[] index = table.getSelectedRows(); // was list.getSelectedIndexes();
        if (index.length==0)
            return error("At least one ROI must be selected from the list.");
        ImagePlus avr_imp = WindowManager.getCurrentImage();
        Undo.setup(Undo.COMPOUND_FILTER, avr_imp);
        for (int i=0; i<index.length; i++) {
            if (restore(index[i])) {
                IJ.run("Fill");
                IJ.run("Select None");
            } else
                break;
        }
        Undo.setup(Undo.COMPOUND_FILTER_DONE, avr_imp);
        return true;
    }

    boolean draw() {
        int[] index = table.getSelectedRows(); // was list.getSelectedIndexes();
        if (index.length==0)
            return error("At least one ROI must be selected from the list.");
        ImagePlus avr_imp = WindowManager.getCurrentImage();
        Undo.setup(Undo.COMPOUND_FILTER, avr_imp);
        for (int i=0; i<index.length; i++) {
            if (restore(index[i])) {
                IJ.run("Draw");
                IJ.run("Select None");
            } else
                break;
        }
        Undo.setup(Undo.COMPOUND_FILTER_DONE, avr_imp);
        return true;
    }

    public boolean labelROIs(){
        tmodel.reindexRois();
        table.selectAll(); // Select everything - otherwise the numbers in the table will not match the numbers put on the image as labels
        int[] index = table.getSelectedRows(); // was list.getSelectedIndexes();
        if (index.length==0)
            return error("At least one ROI must be selected from the list.");
        ImagePlus avr_imp = WindowManager.getCurrentImage();
        Undo.setup(Undo.COMPOUND_FILTER, avr_imp);

        IJ.run("Clear Results");

        for (int i=0; i<index.length; i++) {
            if (restore(index[i])) {
                IJ.run("Measure");
                IJ.run("Label");
                IJ.run("Select None");
            } else
                break;
        }
        table.clearSelection();
        Undo.setup(Undo.COMPOUND_FILTER_DONE, avr_imp);
        return true;
    }

    /* get signal values for specific Roi */
    private float[] getRoiSignal(Roi roi) {
        ImageProcessor ip = imp.getProcessor();
        ImageStack stack = imp.getStack();
        double minThreshold = ip.getMinThreshold();
        double maxThreshold = ip.getMaxThreshold();
        //Polygon blobContour = cell.getOuterContour();
        //this.currentROI =  new PolygonRoi(blobContour.xpoints,blobContour.ypoints,blobContour.npoints, Roi.FREELINE) ;
        int size = stack.getSize();
        float[] values = new float[size];
        Calibration cal = imp.getCalibration();
        Analyzer analyzer = new Analyzer(imp);
        int measurements = Analyzer.getMeasurements();
        for (int i=1; i<=size; i++) {
            ip = stack.getProcessor(i);
            if (minThreshold!=ImageProcessor.NO_THRESHOLD)
                ip.setThreshold(minThreshold,maxThreshold,ImageProcessor.NO_LUT_UPDATE);
            ip.setRoi(roi);
            ImageStatistics stats = ImageStatistics.getStatistics(ip, measurements, cal);
            analyzer.saveResults(stats,roi);
            values[i-1] = (float)stats.mean;
        }
        return values;
    }

    public boolean updateActiveRoi(){
        int[] index = table.getSelectedRows();
        ImagePlus avr_imp = getImage();
        if (avr_imp==null)
            return false;
        Roi roi = avr_imp.getRoi();
        if (roi==null){ return error("The active image does not have an ROI."); }

        if(index.length == 0){
            return error("At least one ROI must be selected from the list.");
        }
        else if(index.length > 1){
            return error("No more than one ROI at the time can be updated.");
        }
        else if(index.length == 1){
			/*
			 * This is not the nicest way (for the user) to experience an update but it's dead easy
			 * and should in far the most cases ensure that all ROIs have unique labels (and thus
			 * unique file names) - this is not nessecary the case when for instance the result of
			 * a Particle Analysis is added to the ROI manager
			 */
            // First delete the current entry
            delete();
            // Then add the active ROI
            add();
            return true;
        }
        else{ return false; }
    }

    int[] getAllIndexes() {
        int count = table.getRowCount(); // was list.getItemCount();
        int[] indexes = new int[count];
        for (int i=0; i<count; i++)
            indexes[i] = i;
        return indexes;
    }

    ImagePlus getImage() {
        ImagePlus avr_imp = WindowManager.getCurrentImage();
        if (avr_imp==null) {
            error("There are no images open.");
            return null;
        } else
            return avr_imp;
    }

    boolean error(String msg) {
        new MessageDialog(this, "Multi Measure", msg);
        return false;
    }

    public void processWindowEvent(WindowEvent e) {
        super.processWindowEvent(e);
        if (e.getID()==WindowEvent.WINDOW_CLOSING) {
            instance = null;
        }
    }

    /** Returns a reference to the ROI Manager
     or null if it is not open. */
    public static CellManager getInstance() {
        return (CellManager)instance;
    }

    /** Returns the ROI Hashtable. */
    public Hashtable getROIs() {
        // This is here only for possible backwards compability
        Hashtable rois = new Hashtable();
        int size = table.getRowCount();
        String label = "";
        Object roi = null;
        boolean status = false;
        for(int i = 0; i <= size; i++){
            label = (String)tmodel.getValueAt(i,1);
            roi = tmodel.getValueAt(i,2);
            status = (Boolean) tmodel.getValueAt(i,3);
            rois.put(label, roi);
        }
        return rois;
    }

    /** Returns the ROI list. */
    //public List getList() {
    //	return list;
    //}


    public void keyPressed(KeyEvent e) {
        /* This method is for keyboard event handling */
        final int SPACE = 32;
        final int CR = 10;
        int keyCode = e.getKeyCode();
        if (keyCode == SPACE)
            additionalCell();
        else if(keyCode == KeyEvent.VK_A)
            additionalCell();
        else if(keyCode == KeyEvent.VK_D)
            dfOverF("df");
        else if(keyCode == KeyEvent.VK_M)
            addWithMagicWandCell();
        else if (keyCode == CR)
            addAndDraw();
    }

    public void keyReleased (KeyEvent e) {}
    public void keyTyped (KeyEvent e) {}

    /** Tests the class. */
    public static void main(final String... args) {
        //Main.launch(args);
        IJ.runPlugIn(Class_Manager.class.getName(), "");
    }
}

/* CMT class */
class CellManagerTableModel extends AbstractTableModel{
    protected static int NUM_COLUMNS = 6;
    protected static int START_NUM_ROWS = 0;
    protected int nextEmptyRow = 0;
    protected int numRows = 0;

    static final public String LABELINDEX = "#";
    static final public String LABEL = "Label";
    static final public String ROI  = "Roi";
    static final public String CASIG  = "CaSignal";
    static final public String STIM = "Stimulus";
    static final public String STATUS  = "Var";


    protected Vector data = null;

    public CellManagerTableModel(){
        data = new Vector();
    }

    public String getColumnName(int column) {
        switch (column) {
            case 0:
                return LABELINDEX;
            case 1:
                return LABEL;
            case 2:
                return ROI;
            case 3:
                return CASIG;
            case 4:
                return STATUS;
            case 5:
                return STIM;
        }
        return "";
    }

    public int getColumnCount() {
        return NUM_COLUMNS;
    }

    public int getRowCount() {
        if (numRows < START_NUM_ROWS) {
            return START_NUM_ROWS;
        } else {
            return numRows;
        }
    }

    public Object getValueAt(int row, int column){
        try {
            CellManagerRoi mmr = (CellManagerRoi)data.elementAt(row);
            switch(column){
                case 0:
                    return new Integer(Integer.toString(mmr.getLabelindex()));
                case 1:
                    return mmr.getLabel();
                case 2:
                    return mmr.getRoi();
                case 3:
                    return mmr.getCaSignal();
                case 4:
                    return mmr.getCaVar();
                case 5:
                    return mmr.getStimulus();
            }
        } catch (Exception e) {
            IJ.showMessage(" ERROR: getVal of CMT  is out of bound");
        }
        return "";
    }

    public int getRowNumber(Object obj, int column){
        int foundindex = -1;
        Object searchobj = obj;
        Object currobj = null;

        for(int i = 0; i < numRows; i++){
            currobj = getValueAt(i, column);
            if(searchobj.equals(currobj)){
                foundindex = i;
                break;
            }
        }

        return foundindex;
    }

    //public int getColumnNumber(String colname){ return -1; }

    public void updateRoi(int index, String label, Object roi, boolean activeFlag){
        CellManagerRoi currmmr = null;
        currmmr = (CellManagerRoi)data.elementAt(index);
        currmmr.setLabel(label);
        currmmr.setStatus(activeFlag);
        currmmr.setRoi(roi);
        data.setElementAt(currmmr, index);

        fireTableRowsUpdated(index, index);
    }

    public void addRoi(String label, Object roi){
        int lastelement = data.size() - 1;
        CellManagerRoi lastmmr = null;
        int lastlabelindex = 0;
        int index = -1;

        if(lastelement >= 0){
            lastmmr = (CellManagerRoi)data.elementAt(lastelement);
            lastlabelindex = lastmmr.getLabelindex();
        }

        if (numRows <= nextEmptyRow) {
            //add arow
            numRows++;
        }

        index = nextEmptyRow;

        nextEmptyRow++;

        data.add(new CellManagerRoi(lastlabelindex + 1, label, roi));

        fireTableRowsInserted(index, index);
    }

    public void addRoi(String label, Object roi, CalciumSignal cas){
        int lastelement = data.size() - 1;
        CellManagerRoi lastmmr = null;
        int lastlabelindex = 0;
        int index = -1;

        if(lastelement >= 0){
            lastmmr = (CellManagerRoi)data.elementAt(lastelement);
            lastlabelindex = lastmmr.getLabelindex();
        }

        if (numRows <= nextEmptyRow) {
            //add arow
            numRows++;
        }

        index = nextEmptyRow;

        nextEmptyRow++;

        data.add(new CellManagerRoi(lastlabelindex + 1, label, roi, cas));

        fireTableRowsInserted(index, index);
    }

    public void reindexRois(){
        int dl = data.size();
        Vector tmp = new Vector();
        CellManagerRoi currmmr = null;

        for(int i = 0; i < dl; i++){
            currmmr = (CellManagerRoi)data.elementAt(i);
            currmmr.setLabelindex(i + 1);
            data.setElementAt(currmmr,i);
        }
        fireTableRowsUpdated(dl, dl);
    }

    public void removeRoi(int index){
        data.remove(index);
        // Delete a row
        numRows--;
        fireTableRowsDeleted(index, index);
    }

    public void setValueAt(Object obj, int r, int c) {
        try {
            CellManagerRoi mmr = (CellManagerRoi)data.elementAt(r);
            switch(c){
                case 1:
                    mmr.setLabel(obj.toString());
                case 2:
                    mmr.setStatus((Boolean) obj);
                case 3:
                    mmr.setRoi( obj );
                case 4:
                    mmr.set_casVar((Double) obj);
            }
        } catch (Exception e) {
             IJ.showMessage("ERROR: setValue of CMT class was not performed - outbound c");        }
        //data[r][c] = ((Integer) obj).intValue();
        fireTableCellUpdated(r, c);
    }
}

/* CMR class */
class CellManagerRoi{
    private int _labelindex = 0;
    private String _label = "";
    private boolean _activeFlag = false;
    private Object _roi = null;
    private CalciumSignal _caSignal = new CalciumSignal();
    private double _casVar = 0;
    private TimeSeries _stimulus = new TimeSeries();

    public CellManagerRoi(int labelindex, String label){
        _labelindex = labelindex;
        _label = label;
    }

    public CellManagerRoi(int labelindex, String label, Object roi){
        _labelindex = labelindex;
        _label = label;
        _roi = roi;
    }

    public CellManagerRoi(int labelindex, String label, Object roi, CalciumSignal cas){
        _labelindex = labelindex;
        _label = label;
        _activeFlag = cas.isactiveFlag;
        _roi = roi;
        _caSignal = cas;
        _casVar  = cas.activityVariance;
    }

    public CellManagerRoi(int labelindex, String label, Object roi, CalciumSignal cas, TimeSeries stimulus){
        _labelindex = labelindex;
        _label = label;
        _activeFlag = cas.isactiveFlag;
        _roi = roi;
        _caSignal = cas;
        _casVar  = cas.activityVariance;
        _stimulus = stimulus;
    }

    public CellManagerRoi(String label){
        _label = label;
    }

    public CellManagerRoi(String label, Object roi){
        _label = label;
        _roi = roi;
    }

    public int getLabelindex(){ return _labelindex; }
    public CalciumSignal getCaSignal(){return _caSignal; };
    public String getLabel(){ return _label; }
    public Object getRoi(){ return _roi; }
    public boolean getStatus(){ return _activeFlag; }
    public double getCaVar(){ return _casVar;}
    public TimeSeries getStimulus(){return _stimulus;}

    public void set_casVar( double var ){ _casVar = var; }
    public void setLabelindex(int labelindex){ _labelindex = labelindex; }
    public void setLabel(String label){ _label = label; }
    public void setStatus(boolean status){ _activeFlag = status; }
    public void setRoi(Object roi){ _roi = roi; }
    public void setStim(TimeSeries stim) {_stimulus = stim;}
}
