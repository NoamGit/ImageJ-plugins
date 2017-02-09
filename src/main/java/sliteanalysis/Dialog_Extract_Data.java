package sliteanalysis;

import ij.IJ;
import ij.Prefs;
import ij.WindowManager;
import ij.io.OpenDialog;
import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.NotEnoughDataPointsException;
import org.apache.commons.io.FileUtils;
import ij.io.SaveDialog;
import javafx.stage.DirectoryChooser;
import org.apache.commons.io.filefilter.IOFileFilter;

import javax.naming.spi.DirectoryManager;
import javax.swing.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

public class Dialog_Extract_Data extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JButton browseIn;
    private JButton browseOut;
    private JButton browseROI;
    private JLabel labelIn;
    private JLabel labelOut;
    private JLabel labelROI;
    private JButton browseArti;
    private JLabel labelArti;
    private JButton settingsButton;
    //private JButton recenterButton;
    public ij.io.DirectoryChooser input_dir = null;
    public ij.io.DirectoryChooser output_dir = null;
    public ij.io.DirectoryChooser art_dir = null;
    public  OpenDialog roi_dir = null;
    private int lbd_kb = 30;

    public Dialog_Extract_Data() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        browseIn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                input_dir = onBrowseDir("select folder of video files...");
                labelIn.setText(input_dir.getDirectory());
                if (output_dir != null && roi_dir != null) {
                    buttonOK.setEnabled(true);
                }
            }
        });

        browseOut.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                output_dir = onBrowseDir("select destination folder...");
                labelOut.setText(output_dir.getDirectory());
                if(input_dir != null && roi_dir != null){
                    buttonOK.setEnabled(true);
                }
            }
        });

        browseArti.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                art_dir = onBrowseDir("select Artifact folder...");
                labelArti.setText(art_dir.getDirectory());
            }
        });

        browseROI.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                roi_dir = onBrowseROI();
                labelROI.setText(roi_dir.getDirectory());
                if(input_dir != null && output_dir != null){
                    buttonOK.setEnabled(true);
                }
            }
        });

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

// call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

// call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        settingsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                UI_Settings.openSettings_ED();
            }
        });

//        recenterButton.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                onRecenter();
//            }
//        });
    }

    private OpenDialog onBrowseROI() {
        OpenDialog od = new OpenDialog("Choose Roi file...", "");
        return od;
    }

    private ij.io.DirectoryChooser onBrowseDir(String title) {
        ij.io.DirectoryChooser dc = new ij.io.DirectoryChooser(title);
    return dc;
    }

    private void onOK() {
    // add your code here
        dispose();

        File input_file = new File(input_dir.getDirectory());
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
            if (!stringContainsItemFromList(file_iter.getName(),no_words) && size > 8 * lbd_kb ) { // exclude TPLSM videos and file under 'lbd_kb' KB (default 30)
                if( (stringContainsItemFromList(file_iter.getName(),arti_type_list)) && art_dir != null){
                    File arti_file = new File(art_dir.getDirectory());
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
                IJ.showStatus("Processes " + file_iter.getName());
                System.out.println("%%% Total memory (bytes): " + Runtime.getRuntime().totalMemory());
                System.out.println("%%% Max memory (bytes): " + Runtime.getRuntime().maxMemory());
                System.out.println("Processes " + file_iter.getName());
                try {
                    aap.run_auto(file_iter, this.output_dir, this.roi_dir);
                }
                catch (OutOfMemoryError error) {
                    System.out.print("Garbage collector initiated...");
                    System.gc();
                    try {
                        aap.run_auto(file_iter, this.output_dir, this.roi_dir);
                    } catch (NotEnoughDataPointsException e) {
                        e.printStackTrace();
                    } catch (IllDefinedDataPointsException e) {
                        e.printStackTrace();
                    }
                } catch (NotEnoughDataPointsException e) {
                    e.printStackTrace();
                } catch (IllDefinedDataPointsException e) {
                    e.printStackTrace();
                }
                aap = new AAP_woStimulus();
            }
        }
        while(!queue.isEmpty()){
            file_iter = (File) queue.element();
            IJ.log(file_iter.getName());
            IJ.showStatus("Processes Artifact " + file_iter.getName());
            System.out.println("%%% Total memory (bytes): " + Runtime.getRuntime().totalMemory());
            System.out.println("%%% Max memory (bytes): " + Runtime.getRuntime().maxMemory());
            System.out.println("Processes Artifact " + file_iter.getName());
            try{
            aap.run_auto(file_iter, this.output_dir, this.roi_dir);
            }
            catch(OutOfMemoryError error){
                System.out.print("Garbage collector initiated...");
                System.gc();
                try {
                    aap.run_auto(file_iter, this.output_dir, this.roi_dir);
                } catch (NotEnoughDataPointsException e) {
                    e.printStackTrace();
                } catch (IllDefinedDataPointsException e) {
                    e.printStackTrace();
                }
            } catch (NotEnoughDataPointsException e) {
                e.printStackTrace();
            } catch (IllDefinedDataPointsException e) {
                e.printStackTrace();
            }
            aap = new AAP_woStimulus();
            queue.remove();
        }
    }

  /*  private void onRecenter() {
        // add your code here if necessary

    }*/

    private void onCancel() {
    // add your code here if necessary
    dispose();
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

    //
    private void runScript() throws Throwable {
        // defining input folders
        ArrayList<String> inputdir = new ArrayList<>();
        inputdir.add("D:\\Noam\\# FINAL ANALYSIS\\160509RetNew gcamp6s - ANALYZE\\L1");
        inputdir.add("D:\\Noam\\# FINAL ANALYSIS\\160509RetNew gcamp6s - ANALYZE\\L2");
        inputdir.add("D:\\Noam\\# FINAL ANALYSIS\\160503RetNew gcamp6s - ANALYZE\\L1");
        inputdir.add("D:\\Noam\\# FINAL ANALYSIS\\160502RetNew gcamp6s - ANALYZE\\L1");
        inputdir.add("D:\\Noam\\# FINAL ANALYSIS\\160502RetNew gcamp6s - ANALYZE\\L2");
        inputdir.add("D:\\Noam\\# FINAL ANALYSIS\\160502RetNew gcamp6s - ANALYZE\\L3");
        inputdir.add("D:\\Noam\\# FINAL ANALYSIS\\160421RetNew gcamp6s - ANALYZE\\L1");
        inputdir.add("D:\\Noam\\# FINAL ANALYSIS\\160421RetNew gcamp6s - ANALYZE\\L2");
        inputdir.add("D:\\Noam\\# FINAL ANALYSIS\\160420RetNew gcamp6f - ANALYZE\\L1");
        inputdir.add("D:\\Noam\\# FINAL ANALYSIS\\160420RetNew gcamp6f - ANALYZE\\L2");
        inputdir.add("D:\\Noam\\# FINAL ANALYSIS\\160414RetNew gcamp6f - ANALYZE\\L1");
        inputdir.add("D:\\Noam\\# FINAL ANALYSIS\\160414RetNew gcamp6f - ANALYZE\\L2");
        inputdir.add("D:\\Noam\\# FINAL ANALYSIS\\160414RetNew gcamp6f - ANALYZE\\L3");
        inputdir.add("D:\\Noam\\# FINAL ANALYSIS\\160414RetNew gcamp6f - ANALYZE\\L4_new");
        inputdir.add("D:\\Noam\\# FINAL ANALYSIS\\160328Retina gcamp6f - ANALYZE\\L1");
        inputdir.add("D:\\Noam\\# FINAL ANALYSIS\\160328Retina gcamp6f - ANALYZE\\L2");
        inputdir.add("D:\\Noam\\# FINAL ANALYSIS\\160328Retina gcamp6f - ANALYZE\\L3");
        inputdir.add("D:\\Noam\\# FINAL ANALYSIS\\160328Retina gcamp6f - ANALYZE\\L4");

        inputdir.add("D:\\Noam\\# FINAL ANALYSIS\\160322Retina gcamp6f - ANALYZE\\L1\\");

        inputdir.add("D:\\Noam\\# FINAL ANALYSIS\\160315Retina gcamp6s - ANALYZE\\L1\\");
        inputdir.add("D:\\Noam\\# FINAL ANALYSIS\\160314Retina gcamp6s - ANALYZE\\L1\\");
        inputdir.add("D:\\Noam\\# FINAL ANALYSIS\\160314Retina gcamp6s - ANALYZE\\L2\\");
        inputdir.add("D:\\Noam\\# FINAL ANALYSIS\\160314Retina gcamp6s - ANALYZE\\L4\\");

        inputdir.add("D:\\Noam\\# FINAL ANALYSIS\\160126Retina gcamp6s - ANALYZE\\L1\\");
        inputdir.add("D:\\Noam\\# FINAL ANALYSIS\\160126Retina gcamp6s - ANALYZE\\L2\\");
        inputdir.add("D:\\Noam\\# FINAL ANALYSIS\\160126Retina gcamp6s - ANALYZE\\L5\\");

// defining output location
        ArrayList<String> outputdir = new ArrayList<>();
        outputdir.add("C:\\Users\\noambox\\Documents\\Sync\\Neural data\\160509 - gcamp6s\\L1\\");
        outputdir.add("C:\\Users\\noambox\\Documents\\Sync\\Neural data\\160509 - gcamp6s\\L2\\");
        outputdir.add("C:\\Users\\noambox\\Documents\\Sync\\Neural data\\160503 - gcamp6s\\L1\\");
        outputdir.add("C:\\Users\\noambox\\Documents\\Sync\\Neural data\\160502 - gcamp6s\\L1\\");
        outputdir.add("C:\\Users\\noambox\\Documents\\Sync\\Neural data\\160502 - gcamp6s\\L2\\");
        outputdir.add("C:\\Users\\noambox\\Documents\\Sync\\Neural data\\160502 - gcamp6s\\L3\\");
        outputdir.add("C:\\Users\\noambox\\Documents\\Sync\\Neural data\\160421 - gcamp6s\\L1\\");
        outputdir.add("C:\\Users\\noambox\\Documents\\Sync\\Neural data\\160421 - gcamp6s\\L2\\");
        outputdir.add("C:\\Users\\noambox\\Documents\\Sync\\Neural data\\160420 - gcamp6f\\L1\\");
        outputdir.add("C:\\Users\\noambox\\Documents\\Sync\\Neural data\\160420 - gcamp6f\\L2\\");
        outputdir.add("C:\\Users\\noambox\\Documents\\Sync\\Neural data\\160414 - gcamp6f\\L1\\");
        outputdir.add("C:\\Users\\noambox\\Documents\\Sync\\Neural data\\160414 - gcamp6f\\L2\\");
        outputdir.add("C:\\Users\\noambox\\Documents\\Sync\\Neural data\\160414 - gcamp6f\\L3\\");
        outputdir.add("C:\\Users\\noambox\\Documents\\Sync\\Neural data\\160414 - gcamp6f\\L4\\");
        outputdir.add("C:\\Users\\noambox\\Documents\\Sync\\Neural data\\160328 - gcamp6f - old\\L1\\");
        outputdir.add("C:\\Users\\noambox\\Documents\\Sync\\Neural data\\160328 - gcamp6f - old\\L2\\");
        outputdir.add("C:\\Users\\noambox\\Documents\\Sync\\Neural data\\160328 - gcamp6f - old\\L3\\");
        outputdir.add("C:\\Users\\noambox\\Documents\\Sync\\Neural data\\160328 - gcamp6f - old\\L4\\");

        outputdir.add("C:\\Users\\noambox\\Documents\\Sync\\Neural data\\160322 - gcamp6f - old\\L1\\");

        outputdir.add("C:\\Users\\noambox\\Documents\\Sync\\Neural data\\160315 - gcamp6s - old\\L1\\");
        outputdir.add("C:\\Users\\noambox\\Documents\\Sync\\Neural data\\160314 - gcamp6s - old\\L1\\");
        outputdir.add("C:\\Users\\noambox\\Documents\\Sync\\Neural data\\160314 - gcamp6s - old\\L2\\");
        outputdir.add("C:\\Users\\noambox\\Documents\\Sync\\Neural data\\160314 - gcamp6s - old\\L4\\");

        outputdir.add("C:\\Users\\noambox\\Documents\\Sync\\Neural data\\160126 - gcamp6s - old\\L1\\");
        outputdir.add("C:\\Users\\noambox\\Documents\\Sync\\Neural data\\160126 - gcamp6s - old\\L2\\");
        outputdir.add("C:\\Users\\noambox\\Documents\\Sync\\Neural data\\160126 - gcamp6s - old\\L5\\");


// defining artifact
        String artdir = new String("D:\\Noam\\# FINAL ANALYSIS\\Artifact movies\\");

        Iterator<String> itr_in = inputdir.iterator();
        Iterator<String> itr_out = outputdir.iterator();
        String out;
        String[] video_types = new String[]{"tiff", "tif"};
        String[] cnt_string = new String[]{"CNT"};
        String[] ori_string = new String[]{"ORI"};
        String[] empty = new String[]{};
        File input_file = null;
        File roi_file = null;
        File file_iter = null;
        File art_iter=  null;
        while(itr_in.hasNext() && itr_out.hasNext()) {

            out = itr_out.next();
//            roi_file = FileUtils.getFile(out + "\\RoiSet_Source.zip");
            roi_file = FileUtils.getFile(out + "\\RoiSet_source_fix.zip");
            boolean new_flag = !stringContainsItemFromList(out, new String[]{"old"});
            // onOk
            input_file = new File(itr_in.next()); // defining input
            System.out.print("\n ######### working on - " + input_file.getPath() + " ###########");
            Iterator itr = FileUtils.iterateFiles(input_file, video_types, false);

            AAP_woStimulus aap = new AAP_woStimulus();
            Queue queue = new LinkedList();
            ArrayList<String> art_names = new ArrayList<>();
//            String[] arti_type_list_old = new String[]{"ORI", "CNT"};
//            String[] arti_type_list_new = new String[]{"ORI","CNTnew"};
            String[] arti_type_list =  new_flag ? new String[]{"ORI","CNTnew"} : new String[]{"ORI", "CNT"};
//            String[] no_words =new String[]{"TPLSM", "FLS", "FL","NOSTIM"};
            String[] no_words =new String[]{"TPLSM", "FLS", "FL","NOSTIM", "ORI","CNT","BDN","sub"};
            File arti_file = new File(artdir);
            Iterator itr_art = FileUtils.iterateFiles(arti_file, video_types, false);
            while (itr.hasNext()) {
                file_iter = (File) itr.next();
                long size = file_iter.length();
                if (!stringContainsItemFromList(file_iter.getName(), no_words) && size > 8 * 30) { // exclude TPLSM videos and file under 'lbd_kb' KB (default 30)
                    if ((stringContainsItemFromList(file_iter.getName(), new String[]{"ORI", "CNT"})) && artdir != null) {
                        while (itr_art.hasNext()) { // looks for suitable arti files
                            // add artifact video to queue
                            art_iter = (File) itr_art.next();
                            if(!(stringContainsItemFromList(art_iter.getName(), arti_type_list))){ // has the same name and time (new) tag
                                continue;
                            }
                            if (art_iter.length() == size) { // same size as in arti stock
                                queue.add(art_iter);
                                art_names.add(file_iter.getName());
                                break;
//                                if (arti_type_list.length == 2 && file_iter.getName().contains("ORI")) {
//                                    arti_type_list = cnt_string;
//                                } else if (arti_type_list.length == 2 && file_iter.getName().contains("CNT")) {
//                                    arti_type_list = ori_string;
//                                } else {
//                                    arti_type_list = empty;
//                                }
                            }
                        }
                        itr_art = FileUtils.iterateFiles(arti_file, video_types, false);
                    }
//                IJ.log(file_iter.getName());
//                    IJ.log("Processes " + file_iter.getName());
                    System.out.println("Processes " + file_iter.getName());
//                    System.out.println("Total memory (bytes): " + Runtime.getRuntime().totalMemory());
//                    System.out.println("Max memory (bytes): " + Runtime.getRuntime().maxMemory());
                    try {
                        aap.run_auto(file_iter, out, roi_file.getPath());
                    } catch (OutOfMemoryError error) {
                        System.out.print("\nGarbage collector initiated...");
                        aap = null;
                        System.gc();
                        aap.run_auto(file_iter, out, roi_file.getPath());
                    }
                    aap.finalize();
                    System.gc();
                    aap = new AAP_woStimulus();
                }
            }
            Iterator iter_names = art_names.iterator();
            while (!queue.isEmpty()) {
                file_iter = (File) queue.element();
                String name_itr = (String) iter_names.next();
                roi_file = FileUtils.getFile(out + "\\RoiSet_"+name_itr.substring(0,name_itr.length()-4)+".zip");
//                IJ.log(file_iter.getName());
//                IJ.log("Processes Artifact " + file_iter.getName());
                System.out.println("Processes Artifact " + file_iter.getName());
                try {
                    aap.run_auto(file_iter, out, roi_file.getPath(), name_itr);
                } catch (OutOfMemoryError error) {
                    System.out.print("\n Garbage collector initiated...");
                    aap = null;
                    System.gc();
                    aap.run_auto(file_iter, out, roi_file.getPath());
                }
                aap = null;
                System.gc();
                aap = new AAP_woStimulus();
                queue.remove();
            }

            // release memory
            System.out.print("\n !!!!!! Done with !! - " + input_file.getPath() + " !!!!!!");
            aap = null;
            file_iter = null;
            art_iter = null;
            input_file = null;
            System.gc();
        }
    }


    public static void main(String[] args) throws Throwable {
        Dialog_Extract_Data dialog = new Dialog_Extract_Data();

//        dialog.pack();
//        dialog.setVisible(true);

        dialog.runScript();

        System.exit(0);
    }
}
