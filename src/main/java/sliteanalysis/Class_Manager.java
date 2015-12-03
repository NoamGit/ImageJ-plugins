package sliteanalysis;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.plugin.PlugIn;

/** Bob Dougherty.  This is a copy of Wayne Rasband's ROI Manager with
 one new method added: multi() performs measurements for several ROI's in a stack
 and arranges the results with one line per slice.  By constast, the measure() method
 produces several lines per slice.  The results from multi() may be easier to import into
 a spreadsheet program for plotting or additional analysis.  This capability was
 requested by Kurt M. Scudder

 Version 0 6/24/2002
 Version 1 6/26/2002 Updated to Wayne's version of ImageJ 1.28k
 Version 2 11/24/2002 Made some changes suggested by Tony Collins
 Version 3 7/20/2004  Added "Add Particles"
 Version 3.1 7/21 Fixed bugs spotted by Andreas Schleifenbaum
 Version 3.2 3/12/2005 Updated save method.  Requires ImageJ 1.33 for IJ.saveAs.
 Version 4   1/13/2006 Applied many enhancements, including JTable, provided by Ulrik Stervbo.
 Added option for labeling slices.

 */
/*	License:
 Copyright (c) 2002, 2006, OptiNav, Inc.
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:

 Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.
 Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in the
 documentation and/or other materials provided with the distribution.
 Neither the name of OptiNav, Inc. nor the names of its contributors
 may be used to endorse or promote products derived from this software
 without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
public class Class_Manager implements PlugIn {
    CellManager mm;

    public void run(String arg) {
        if (IJ.versionLessThan("1.33"))return;
        ImagePlus avr_imp = IJ.openImage("C:\\Users\\noambox\\Dropbox\\# Graduate studies M.Sc\\# SLITE\\ij - plugin data\\avr_image.tif"); // DEBUG
        Overlay ovr = new Overlay();
        mm = new CellManager(avr_imp,avr_imp, ovr);
    }
}
