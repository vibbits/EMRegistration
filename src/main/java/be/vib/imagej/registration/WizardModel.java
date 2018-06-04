package be.vib.imagej.registration;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
	private ImagePlus referenceImage;
	
	public WizardModel()
	{
		this.inputFolder = null;
		this.outputFolder = null;
		this.inputFiles = null;  // we'll read the actual input files (there may be thousands in the input folder) when we actually need them
		this.referenceImage = null;
	}
	
	public void reset()
	{
	}	

	public Path getInputFolder()  // may return null
	{
		return inputFolder;
	}

	public void setInputFolder(Path inputFolder)  // inputFolder may be null
	{
		if (inputFolder != this.inputFolder)
		{
			this.inputFolder = inputFolder;
			this.referenceImage = null;  // the input folder has changed, so the reference image (used for specifying the template region for registration) must be reloaded too
		}
	}

	public Path getOutputFolder()  // may return null
	{
		return outputFolder;
	}

	public void setOutputFolder(Path outputFolder)  // outputFolder may be null
	{
		this.outputFolder = outputFolder;
	}

	public void scanInputFolder(String filePattern)
	{
		this.inputFiles = getFiles(inputFolder, filePattern);  // inputFiles will be null if inputFolder does not exist or could not be enumerated
		
//		// Begin debugging
//		System.out.println("Input files:");
//		if (this.inputFiles != null)
//			for (Path path : this.inputFiles)
//				System.out.println(path.toString());
//		// End debugging
	}
	
	public List<Path> getInputFiles()  // may return null
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
	
	private List<Path> getFiles(Path folder, String filePattern)
	{	
		if (folder == null)
		{
			return null;
		}
		
		try
		{
			PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + filePattern);
			
			List<Path> paths = Files.walk(folder, 1)
			                        .filter(Files::isRegularFile)
			                        .filter(p -> matcher.matches(p.getFileName()))
			                        .collect(Collectors.toList());
			return paths;
		}
		catch (IOException e)
		{
			// (This exception typically occurs while the user is typing a folder name,
			// before the folder name is typed in completely.)
			return null;
		}
	}
}
