package be.vib.imagej.registration;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import ij.ImagePlus;


/**
 * WizardModel stores all model data for the wizard. It is shared by all wizard pages.
 * Typically user input done in one wizard page is stored in the model, and will influence
 * what is shown on another wizard page later.
 */
public class WizardModel
{
	private Path inputFolder;
	private Path outputFolder;
	private List<Path> inputFiles = new ArrayList<Path>();
	public ImagePlus referenceImage; // FIXME: getter/setter
	
	public Path getInputFolder()
	{
		return inputFolder;
	}

	public void setInputFolder(Path inputFolder)
	{
		this.inputFolder = inputFolder;
	}

	public Path getOutputFolder()
	{
		return outputFolder;
	}

	public void setOutputFolder(Path outputFolder)
	{
		this.outputFolder = outputFolder;
	}
	
	public List<Path> getInputFiles()
	{
		return inputFiles;
	}

	public void setInputFiles(List<Path> inputFiles)
	{
		this.inputFiles = inputFiles;
	}

	public WizardModel()
	{
	}
	
	public void reset()
	{
	}	
}
