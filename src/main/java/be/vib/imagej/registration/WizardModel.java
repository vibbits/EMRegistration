package be.vib.imagej.registration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import ij.ImagePlus;
import ij.Prefs;

/**
 * WizardModel stores all model data for the wizard. It is shared by all wizard pages.
 * Typically user input done in one wizard page is stored in the model, and will influence
 * what is shown on another wizard page later.
 */
public class WizardModel
{
	private static final String keyPrefInputFolder = "be.vib.imagej.registration.inputFolder";
	private static final String keyPrefOutputFolder = "be.vib.imagej.registration.outputFolder";
	
	private Path inputFolder;
	private Path outputFolder;
	private List<Path> inputFiles = new ArrayList<Path>();
	private ImagePlus referenceImage;
	
	public WizardModel()
	{
		this.inputFolder = Paths.get(Prefs.get(keyPrefInputFolder, ""));
		this.outputFolder = Paths.get(Prefs.get(keyPrefOutputFolder, ""));
		this.inputFiles = null;  // we'll read the actual input files (there may be thousands in the input folder) when we actually need them
		this.referenceImage = null;
	}
	
	public void reset()
	{
	}	

	public Path getInputFolder()
	{
		return inputFolder;
	}

	public void setInputFolder(Path inputFolder)
	{
		if (inputFolder != this.inputFolder)
		{
			this.referenceImage = null;  // the input folder has changed, so the reference image (used for specifying the template region for registraion) must be reloaded too

			this.inputFolder = inputFolder;
			Prefs.set(keyPrefInputFolder, inputFolder.toString()); // FIXME: we probably do not want to do this if the folder does not exist?
		}
	}

	public Path getOutputFolder()
	{
		return outputFolder;
	}

	public void setOutputFolder(Path outputFolder)
	{
		this.outputFolder = outputFolder;
		Prefs.set(keyPrefOutputFolder, outputFolder.toString());
	}

	public void scanInputFolder()
	{
		this.inputFiles = getFiles(inputFolder);  // inputFiles will be null if inputFolder does not exist or could not be enumerated
	}
	
	public List<Path> getInputFiles()
	{
		return inputFiles;
	}

	public ImagePlus getReferenceImage()
	{
		return referenceImage;
	}

	public void setReferenceImage(ImagePlus referenceImage)
	{
		this.referenceImage = referenceImage;
	}
	
	public void lockReferenceImage(boolean lock)
	{
		if (referenceImage == null)
			return;
		
		if (lock && !referenceImage.isLocked())
			referenceImage.lock();
		else if (!lock && referenceImage.isLocked())
			referenceImage.unlock();
	}
	
	// TODO: maybe add a file filter (so we can exclude irrelevant files that happen to be in the folder too)
	private List<Path> getFiles(Path folder)
	{		
		try
		{
			List<Path> paths = Files.walk(folder, 1)
			                        .filter(Files::isRegularFile)
			                        .collect(Collectors.toList());
			return paths;
		}
		catch (IOException e)
		{
			System.out.println("Caught exception " + e + " while walking folder " + folder);
			return null;
		}
	}
}
