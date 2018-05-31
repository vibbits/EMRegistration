package be.vib.imagej.registration;

import java.awt.Rectangle;
import java.nio.file.Path;
import java.util.List;

public class RegistrationParameters
{
	public List<Path> inputFiles;
	public Path outputFolder;
	public Rectangle rect;
	
	public int maxShiftX;
	public int maxShiftY;
	
	public boolean sliceThicknessCorrection;
	public double sliceThicknessNM;  // in nanometers; only relevant if sliceThicknessCorrection == true
	
	public RegistrationParameters(List<Path> inputFiles, Path outputFolder, Rectangle rect, int maxShiftX, int maxShiftY, boolean sliceThicknessCorrection, double sliceThicknessNM)
	{
		this.inputFiles = inputFiles;
		this.outputFolder = outputFolder;
		this.rect = rect;
		this.maxShiftX = maxShiftX;
		this.maxShiftY = maxShiftY;
		this.sliceThicknessCorrection = sliceThicknessCorrection;
		this.sliceThicknessNM = sliceThicknessNM;
	}
}
