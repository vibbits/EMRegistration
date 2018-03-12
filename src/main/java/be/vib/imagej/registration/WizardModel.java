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
		this.inputFolder = inputFolder;
		Prefs.set(keyPrefInputFolder, inputFolder.toString());
		
		this.setInputFiles(getFiles(inputFolder));
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
	
	public List<Path> getInputFiles()
	{
		return inputFiles;
	}

	private void setInputFiles(List<Path> inputFiles)
	{
		this.inputFiles = inputFiles;
	}

	public ImagePlus getReferenceImage()
	{
		return referenceImage;
	}

	public void setReferenceImage(ImagePlus referenceImage)
	{
		this.referenceImage = referenceImage;
	}
	
	private List<Path> getFiles(Path folder)  // TODO: what if folder does not exist? Should we let the throw the exception or return an empty list? Maybe throw the exception otherwise there is no wat to tell these 2 situations apart
	{		
		List<Path> paths = new ArrayList<Path>();
		
		try
		{
			paths = Files.walk(folder, 1)
			        .filter(Files::isRegularFile)
			        .collect(Collectors.toList());
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		paths.forEach(System.out::println);

		return paths;
	}
}
