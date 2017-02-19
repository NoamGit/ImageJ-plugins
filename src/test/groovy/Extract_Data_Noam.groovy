import ij.IJ
import org.apache.commons.io.FileUtils
import sliteanalysis.AAP_woStimulus
import sliteanalysis.Dialog_Extract_Data
import org.apache.poi.xssf.usermodel.*;
/**
 * Script for runnig over all my files and extract and save the data
 * Created by noambox on 5/24/2016.
 */

// defining input folders
ArrayList<String> inputdir = new ArrayList<>();
inputdir.add("D:\\Noam\\# FINAL ANALYSIS\\160509RetNew gcamp6s - ANALYZE\\L1");
inputdir.add("D:\\Noam\\# FINAL ANALYSIS\\160509RetNew gcamp6s - ANALYZE\\L2");
inputdir.add("D:\\Noam\\# FINAL ANALYSIS\\160503RetNew gcamp6s - ANALYZE\\L1");
inputdir.add("D:\\Noam\\# FINAL ANALYSIS\\160503RetNew gcamp6s - ANALYZE\\L2");
inputdir.add("D:\\Noam\\# FINAL ANALYSIS\\160502RetNew gcamp6s - ANALYZE\\L1");
inputdir.add("D:\\Noam\\# FINAL ANALYSIS\\160502RetNew gcamp6s - ANALYZE\\L2");
inputdir.add("D:\\Noam\\# FINAL ANALYSIS\\160502RetNew gcamp6s - ANALYZE\\L3");
inputdir.add("D:\\Noam\\# FINAL ANALYSIS\\160502RetNew gcamp6s - ANALYZE\\L4");
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

// defining output location
ArrayList<String> outputdir = new ArrayList<>();
outputdir.add("C:\\Users\\noambox\\Documents\\Sync\\Neural data\\160509 - gcamp6s\\L1\\");
outputdir.add("C:\\Users\\noambox\\Documents\\Sync\\Neural data\\160509 - gcamp6s\\L2\\");
outputdir.add("C:\\Users\\noambox\\Documents\\Sync\\Neural data\\160503 - gcamp6s\\L1\\");
outputdir.add("C:\\Users\\noambox\\Documents\\Sync\\Neural data\\160503 - gcamp6s\\L2\\");
outputdir.add("C:\\Users\\noambox\\Documents\\Sync\\Neural data\\160502 - gcamp6s\\L1\\");
outputdir.add("C:\\Users\\noambox\\Documents\\Sync\\Neural data\\160502 - gcamp6s\\L2\\");
outputdir.add("C:\\Users\\noambox\\Documents\\Sync\\Neural data\\160502 - gcamp6s\\L3\\");
outputdir.add("C:\\Users\\noambox\\Documents\\Sync\\Neural data\\160502 - gcamp6s\\L4\\");
outputdir.add("C:\\Users\\noambox\\Documents\\Sync\\Neural data\\160421 - gcamp6s\\L1\\");
outputdir.add("C:\\Users\\noambox\\Documents\\Sync\\Neural data\\160421 - gcamp6s\\L2\\");
outputdir.add("C:\\Users\\noambox\\Documents\\Sync\\Neural data\\160420 - gcamp6f\\L1\\");
outputdir.add("C:\\Users\\noambox\\Documents\\Sync\\Neural data\\160420 - gcamp6f\\L2\\");
outputdir.add("C:\\Users\\noambox\\Documents\\Sync\\Neural data\\160414 - gcamp6f\\L1\\");
outputdir.add("C:\\Users\\noambox\\Documents\\Sync\\Neural data\\160414 - gcamp6f\\L2\\");
outputdir.add("C:\\Users\\noambox\\Documents\\Sync\\Neural data\\160414 - gcamp6f\\L3\\");
outputdir.add("C:\\Users\\noambox\\Documents\\Sync\\Neural data\\160414 - gcamp6f\\L4\\");
outputdir.add("C:\\Users\\noambox\\Documents\\Sync\\Neural data\\160328 - gcamp6f\\L1\\");
outputdir.add("C:\\Users\\noambox\\Documents\\Sync\\Neural data\\160328 - gcamp6f\\L2\\");
outputdir.add("C:\\Users\\noambox\\Documents\\Sync\\Neural data\\160328 - gcamp6f\\L3\\");
outputdir.add("C:\\Users\\noambox\\Documents\\Sync\\Neural data\\160328 - gcamp6f\\L4\\");

// defining artifact
String artdir = new String("D:\\Noam\\# FINAL ANALYSIS\\Artifact movies\\");

Iterator<String> itr_in = inputdir.iterator();
Iterator<String> itr_out = outputdir.iterator();
String out;
String[] video_types = ["tiff", "tif"];
String[] cnt_string = ["CNT"];
String[] ori_string = ["ORI"];
String[] empty = [];
while(itr_in.hasNext() && itr_out.hasNext()) {
    out = itr_out.next();
//    File roi_file = FileUtils.getFile(out+"\\RoiSet_Source.zip");
    File roi_file = FileUtils.getFile(out+"\\RoiSet_source_fix.zip"); // for fullfield correction

    /*
    * onOk() implementation
    * */
    File input_file = new File(itr_in.next()); // defining input
        System.out.print("\n"+input_file.getPath()+" working on....");
    Iterator itr = FileUtils.iterateFiles(input_file, video_types, false);
    File file_iter = null;
    File art_iter = null;
    AAP_woStimulus aap = new AAP_woStimulus();
    Queue queue = new LinkedList();
    String[] arti_type_list = ["ORI","CNT"];
    String[] no_words = ["TPLSM","FLS","FL","ORI","CNT","BDN","sub"];
    while(itr.hasNext()) {
        file_iter = (File) itr.next();
        long size = file_iter.length();
        if (!Dialog_Extract_Data.stringContainsItemFromList(file_iter.getName(),no_words) && size > 8 * 30 ) { // exclude TPLSM videos and file under 'lbd_kb' KB (default 30)
            if( (Dialog_Extract_Data.stringContainsItemFromList(file_iter.getName(),arti_type_list)) && artdir != null){
                File arti_file = new File(artdir);
                Iterator itr_art = FileUtils.iterateFiles(arti_file, video_types, false);
                while(itr_art.hasNext()) {
                    // add artifact video to queue
                    art_iter = (File) itr_art.next();
                    if(art_iter.length() == size){
                        queue.add(art_iter);
                        if(arti_type_list.length == 2 && file_iter.getName().contains("ORI")){ arti_type_list = cnt_string;}
                        else if(arti_type_list.length == 2 && file_iter.getName().contains("CNT")){ arti_type_list = ori_string;}
                        else {arti_type_list = empty ;}
                        break;
                    }
                }
            }
//                IJ.log(file_iter.getName());
            IJ.showStatus("Processes " + file_iter.getName());
            System.out.println("Processes " + file_iter.getName());
            try {
                aap.run_auto(file_iter, out, roi_file.getPath());
            }
            catch (OutOfMemoryError error) {
                System.out.print("Garbage collector initiated...");
                System.gc();
                aap.run_auto(file_iter, out, roi_file.getPath());
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
            aap.run_auto(file_iter, out, roi_file.getPath());
        }
        catch(OutOfMemoryError error){
            System.out.print("Garbage collector initiated...");
            System.gc();
            aap.run_auto(file_iter, out, roi_file.getPath());
        }
        aap = new AAP_woStimulus();
        queue.remove();
    }

    // Test
//    System.out.print("\n" + roi_file.getPath());
}