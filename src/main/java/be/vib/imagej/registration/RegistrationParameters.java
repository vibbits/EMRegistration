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
	
	public Rectangle autoCropRect;  // if null, then no auto-crop is performed
	
	public RegistrationParameters(List<Path> inputFiles, Path outputFolder, Rectangle templatePatchRect, int maxShiftX, int maxShiftY, boolean sliceThicknessCorrection, double sliceThicknessNM, Rectangle autoCropRect)
	{
		this.inputFiles = inputFiles;
		this.outputFolder = outputFolder;
		this.templatePatchRect = templatePatchRect;
		this.maxShiftX = maxShiftX;
		this.maxShiftY = maxShiftY;
		this.sliceThicknessCorrection = sliceThicknessCorrection;
		this.sliceThicknessNM = sliceThicknessNM;
		this.autoCropRect = autoCropRect;  
	}
}
