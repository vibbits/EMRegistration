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
	public double sliceThickness;  // in microns; only relevant if sliceThicknessCorrection == true
	
	public RegistrationParameters(List<Path> inputFiles, Path outputFolder, Rectangle rect, int maxShiftX, int maxShiftY, boolean sliceThicknessCorrection, double sliceThickness)
	{
		this.inputFiles = inputFiles;
		this.outputFolder = outputFolder;
		this.rect = rect;
		this.maxShiftX = maxShiftX;
		this.maxShiftY = maxShiftY;
	}
}
