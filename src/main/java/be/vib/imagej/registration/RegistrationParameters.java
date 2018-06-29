package be.vib.imagej.registration;

import java.awt.Rectangle;
import java.nio.file.Path;
import java.util.List;

public class RegistrationParameters
{
	public List<Path> inputFiles;
	public Path outputFolder;
	public Rectangle templatePatchRect;
	
	public int maxShiftX;
	public int maxShiftY;
	
	public boolean sliceThicknessCorrection;
	public double sliceThicknessNM;  // in nanometers; only relevant if sliceThicknessCorrection == true
	public boolean preserveSliceOrder; // if false, nearest neighbor sampling with pick the slice with the closest z (even if this results in out-of-order slices because of neagtive slice thicknesses reported by the EM microscope); if true, isotonic regression will be performed on the reported z values, resulting in in-order slice sampling (this is more realistic)
	
	public Rectangle autoCropRect;  // if null, then no auto-crop is performed
	
	public RegistrationParameters(List<Path> inputFiles, Path outputFolder, Rectangle templatePatchRect, int maxShiftX, int maxShiftY, boolean sliceThicknessCorrection, double sliceThicknessNM, boolean preserveSliceOrder, Rectangle autoCropRect)
	{
		this.inputFiles = inputFiles;
		this.outputFolder = outputFolder;
		this.templatePatchRect = templatePatchRect;
		this.maxShiftX = maxShiftX;
		this.maxShiftY = maxShiftY;
		this.sliceThicknessCorrection = sliceThicknessCorrection;
		this.sliceThicknessNM = sliceThicknessNM;
		this.preserveSliceOrder = preserveSliceOrder;
		this.autoCropRect = autoCropRect;  
	}
}
