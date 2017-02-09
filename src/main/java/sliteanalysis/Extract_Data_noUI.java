package sliteanalysis;

import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;


import ij.IJ;
import ij.plugin.PlugIn;
import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.NotEnoughDataPointsException;
import org.apache.commons.io.FileUtils;

import javax.swing.*;
import java.io.FileNotFoundException;
import java.util.*;
import javax.swing.UIManager.*;

/**
 * Created by noambox on 5/29/2016.
 * Extract data for scripting in fiji
 */


public class Extract_Data_noUI implements PlugIn {

    /* Class Variabels*/

    /* Methods */
    public void run(String argv) {
        // choose input directory and output directory
        try {
            openUI();
        } catch (NotEnoughDataPointsException e) {
            e.printStackTrace();
        } catch (IllDefinedDataPointsException e) {
            e.printStackTrace();
        }
    }

    private boolean openUI() throws NotEnoughDataPointsException, IllDefinedDataPointsException {
        GenericDialog gd = new GenericDialog("Extract data no ui");
        gd.setLayout(new BoxLayout(gd, BoxLayout.Y_AXIS)); // check
        gd.addStringField("input folder", "D:\\Noam\\# FINAL ANALYSIS\\160502RetNew gcamp6s - ANALYZE\\L1\\", 1);
        gd.add(Box.createRigidArea(new Dimension(0, 2)));
        gd.addStringField("artifact folder:", "D:\\Noam\\# FINAL ANALYSIS\\Artifact movies\\", 1);
        gd.add(Box.createRigidArea(new Dimension(0, 10)));
        gd.addStringField("output folder", "C:\\Users\\noambox\\Documents\\Sync\\Neural data analysis\\160502 - gcamp6s\\L1\\", 1);
        gd.add(Box.createRigidArea(new Dimension(0, 2)));
        gd.addStringField("RoiSet file:", "C:\\Users\\noambox\\Documents\\Sync\\Neural data analysis\\160502 - gcamp6s\\L1\\RoiSet_Source.zip", 1);
        gd.add(Box.createRigidArea(new Dimension(0, 10)));
        JButton settings = new JButton("Recenter settings");
        settings.setBackground(Color.lightGray);
        settings.setBorderPainted(true);
        settings.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                UI_Settings.openSettings_ED();
            }
        });
        gd.add(settings);
        gd.pack();
        gd.add(Box.createRigidArea(new Dimension(0, 10)));
        gd.showDialog();


        if(gd.wasCanceled()) {
            gd.dispose();
            return false;
        } else {
            String inputpath = gd.getNextString();
            String artifactpath = gd.getNextString();
            String outputpath = gd.getNextString();
            String roipath = gd.getNextString();
            gd.dispose();

            File input_file = new File(inputpath);
            Iterator itr = FileUtils.iterateFiles(input_file, new String[]{"tiff", "tif"}, false);
            File file_iter = null;
            File art_iter = null;
            AAP_woStimulus aap = new AAP_woStimulus();
            Queue queue = new LinkedList();
            String[] arti_type_list = {"ORI","CNT"};
            String[] no_words = {"TPLSM","FLS","FL"};
            while(itr.hasNext()) {
                file_iter = (File) itr.next();
                long size = file_iter.length();
                if (!stringContainsItemFromList(file_iter.getName(),no_words) && size > 8 * 30 ) { // exclude TPLSM videos and file under 'lbd_kb' KB (default 30)
                    if( (stringContainsItemFromList(file_iter.getName(),arti_type_list)) && artifactpath != null){
                        File arti_file = new File(artifactpath);
                        Iterator itr_art = FileUtils.iterateFiles(arti_file, new String[]{"tiff", "tif"}, false);
                        while(itr_art.hasNext()) {
                            // add artifact video to queue
                            art_iter = (File) itr_art.next();
                            if(art_iter.length() == size){
                                queue.add(art_iter);
                                if(arti_type_list.length == 2 && file_iter.getName().contains("ORI")){ arti_type_list = new String[]{"CNT"};}
                                else if(arti_type_list.length == 2 && file_iter.getName().contains("CNT")){ arti_type_list = new String[]{"ORI"};}
                                else {arti_type_list = new String[]{};}
                                break;
                            }
                        }
                    }
//                IJ.log(file_iter.getName());
                    IJ.log("Processes " + file_iter.getName());
                    System.out.println("%%% Total memory (bytes): " + Runtime.getRuntime().totalMemory());
                    System.out.println("%%% Max memory (bytes): " + Runtime.getRuntime().maxMemory());
                    System.out.println("Processes " + file_iter.getName());
                    try {
                        aap.run_auto(file_iter, outputpath, roipath);
                    }
                    catch (OutOfMemoryError error) {
                        System.out.print("Garbage collector initiated...");
                        System.gc();
                        aap.run_auto(file_iter, outputpath, roipath);
                    }
                    aap = new AAP_woStimulus();
                }
            }
            while(!queue.isEmpty()){
                file_iter = (File) queue.element();
//                IJ.log(file_iter.getName());
                IJ.log("Processes Artifact " + file_iter.getName());
                System.out.println("%%% Total memory (bytes): " + Runtime.getRuntime().totalMemory());
                System.out.println("%%% Max memory (bytes): " + Runtime.getRuntime().maxMemory());
                System.out.println("Processes Artifact " + file_iter.getName());
                try{
                    aap.run_auto(file_iter, outputpath, roipath);
                }
                catch(OutOfMemoryError error){
                    System.out.print("Garbage collector initiated...");
                    System.gc();
                    aap.run_auto(file_iter, outputpath, roipath);
                }
                aap = new AAP_woStimulus();
                queue.remove();
            }


            return true;
        }
    }

    public static boolean stringContainsItemFromList(String inputString, String[] items)
    {
        for(int i =0; i < items.length; i++)
        {
            if(inputString.contains(items[i]))
            {
                return true;
            }
        }
        return false;
    }

    public static void main(final String... args) throws FileNotFoundException {
        Extract_Data_noUI pt = new Extract_Data_noUI();

        String argv = "";
//        ij.ImageJ.main(new String[] {"temp"});
        pt.run(argv);

    }
}