package sliteanalysis;

import ij.IJ;
import ij.io.OpenDialog;
import org.apache.commons.io.FileUtils;
import ij.io.SaveDialog;
import javafx.stage.DirectoryChooser;
import org.apache.commons.io.filefilter.IOFileFilter;

import javax.naming.spi.DirectoryManager;
import javax.swing.*;
import java.awt.event.*;
import java.io.File;
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
                UI_Settings.openSettings();
            }
        });
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
                System.out.println("Processes " + file_iter.getName());
                try {
                    aap.run_auto(file_iter, this.output_dir, this.roi_dir);
                }
                catch (OutOfMemoryError error) {
                    System.out.print("Garbage collector initiated...");
                    System.gc();
                    aap.run_auto(file_iter, this.output_dir, this.roi_dir);
                }
                aap = new AAP_woStimulus();
            }
        }
        while(!queue.isEmpty()){
            file_iter = (File) queue.element();
            IJ.log(file_iter.getName());
            IJ.showStatus("Processes Artifact " + file_iter.getName());
            System.out.println("Processes Artifact " + file_iter.getName());
            try{
            aap.run_auto(file_iter, this.output_dir, this.roi_dir);
            }
            catch(OutOfMemoryError error){
                System.out.print("Garbage collector initiated...");
                System.gc();
                aap.run_auto(file_iter, this.output_dir, this.roi_dir);
            }
            aap = new AAP_woStimulus();
            queue.remove();
        }
    }

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

    public static void main(String[] args) {
        Dialog_Extract_Data dialog = new Dialog_Extract_Data();
        dialog.pack();
        dialog.setVisible(true);
//        System.exit(0);
    }
}
