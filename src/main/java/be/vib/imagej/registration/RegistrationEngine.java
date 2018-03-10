package be.vib.imagej.registration;

import java.awt.Rectangle;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutionException;

import be.vib.bits.QExecutor;

import ij.ImagePlus;
import ij.io.FileSaver;
import ij.io.Opener;
import ij.process.ImageProcessor;

// The RegistrationEngine class is the main access point from ImageJ
// to our registration machinery. It iterates over the images (=z-slices) in the input folder
// and registers them one by one with their predecessor slice.
//
// The publish(), process() and isCancelled() methods are useful when the RegistrationEngine
// is used in combination with user interface elements that allow the user to interrupt (cancel)
// the registration calculations and that provide progress feedback.
public class RegistrationEngine
{
	private Registerer registerer;
	
	private final static int maxShiftX = 10;
	private final static int maxShiftY = 25;  // in pixels, all possible shifts (from -maxShiftX/Y to +maxShiftX/Y) are checked exhaustively for the optimal patch match
	
	public RegistrationEngine()
	{
		this.registerer = new Registerer();
	}
	
	public void register(List<Path> inputFiles, Path outputFolder, Rectangle rect)
	{
		// ImageJ image opener object
		Opener opener = new Opener();  

		// Coordinates of the top-left corner of the reference patch in the original image.
		final int initialX = rect.x;
		final int initialY = rect.y;
		
		// Extract reference patch from the first image
		ImagePlus firstImage = opener.openImage(inputFiles.get(0).toString());
		ImageProcessor referencePatch = cropImage(firstImage, rect);

		// Process all images in the input folder.
		
		int prevX = initialX;
		int prevY = initialY;
				
		final int numSlices = inputFiles.size();
		int sliceNr = 1;
		for (Path inputFile : inputFiles)
		{
			if (isCancelled())
				break;
			
			// For timing
			long loadStart = 0;
			long loadEnd = 0;
			long registerStart = 0;
			long registerEnd = 0;
			long saveStart = 0;
			long saveEnd = 0;
			
			System.out.println("Image " + sliceNr + "/" + numSlices + " : " + inputFile.toString() + "...");
			loadStart = System.nanoTime();
			ImagePlus imagePlus = opener.openImage(inputFile.toString());
			loadEnd = System.nanoTime();
			
			if (imagePlus == null)
				break; // TODO: this indicates an error during reading of the image - make sure the user is informed properly
			
			ImageProcessor image = imagePlus.getProcessor();
			if (image == null)
				break; // TODO: inform user

			// Find coordinates (with respect to the full image) of the region where we will look for the patch.
			// So this rectangular crop of the image needs to be sent to Quasar.
			int cropTopLeftX = Math.max(0, prevX - maxShiftX);
			int cropTopLeftY = Math.max(0, prevY - maxShiftY);
			int cropBottomRightX = Math.min(prevX + maxShiftX + referencePatch.getWidth(), image.getWidth() -  1);
			int cropBottomRightY = Math.min(prevY + maxShiftY + referencePatch.getHeight(), image.getHeight() - 1);
			
			Rectangle cropRect = new Rectangle(cropTopLeftX, cropTopLeftY, cropBottomRightX - cropTopLeftX, cropBottomRightY - cropTopLeftY);
			ImageProcessor croppedImage = cropImage(imagePlus, cropRect);
			
			// Calculate the shift required to register this slice to the previous one.
			RegistrationResult result = null;
			try
			{
				registerStart = System.nanoTime();
			    registerer.setParameters(croppedImage, referencePatch, 0, (cropBottomRightX - cropTopLeftX) - referencePatch.getWidth(), 0, (cropBottomRightY - cropTopLeftY) - referencePatch.getHeight());
				result = QExecutor.getInstance().submit(registerer).get(); // TODO: check what happens to quasar::exception_t if thrown from C++ during the registration task.
				registerEnd = System.nanoTime();
			}
			catch (ExecutionException | InterruptedException e)
			{
				e.printStackTrace();
				// TODO: handle exception nicely
				break;
			}
			
			// Convert coordinates returned from Quasar (which are of the reference patch with respect to the cropped image)
			// to coordinates in the full original image.
			int bestPosX = cropTopLeftX + result.posX;
			int bestPosY = cropTopLeftY + result.posY;
			
			// Shift the image to register it
			int shiftX = bestPosX - initialX;
			int shiftY = bestPosY - initialY;
			image.translate(-shiftX, -shiftY);
			System.out.println("Shift: X=" + shiftX + " Y=" + shiftY);
			
			// Update the most recent position of the best matching patch,
			// we use its position as an estimate for the location of the patch in the next image.
			prevX = bestPosX;
			prevY = bestPosY;
			
			// Save the registered image to the output folder			
			saveStart = System.nanoTime();
			FileSaver saver = new FileSaver(imagePlus);
			Path resultPath = suggestOutputFilename(inputFile, outputFolder);
			saver.saveAsTiff(resultPath.toString());
			saveEnd = System.nanoTime();
	
			// Show some timing statistics
			printStatistics(loadStart, loadEnd, registerStart, registerEnd, saveStart, saveEnd);
			
			// Progress feedback
			publish((100 * sliceNr) / numSlices);
			sliceNr++;
		}
	}
	
	public void publish(Integer... chunks)
	{
		// Will be overridden
	}
	
	public void process(List<Integer> percentages)
	{
		// Will be overridden
	}
	
	public boolean isCancelled()
	{
		// Will be overridden
		return false; 
	}
	
	// Returns an output file path like this:
	// desired output folder + original filename (without extension) + _registered + original extension (if any)
	private Path suggestOutputFilename(Path inputFilePath, Path outputFolder)
	{
		String suffix = "_registered";	
		String filename = inputFilePath.getName(inputFilePath.getNameCount() - 1).toString(); // filename part only (including extension, if any)
		
	    int dotIndex = filename.toString().lastIndexOf('.');
	    if (dotIndex == -1)
	    {
	    	// Original file has no extension, just append the suffix.
	    	return Paths.get(outputFolder.toString(), filename + suffix);
	    }
	    else
	    {
	    	// Original file has an extension, insert the suffix just before the extension in the original filename
	    	String filenameWithoutExtension = filename.substring(0, dotIndex);
	    	String extension = filename.substring(dotIndex);
	    	return Paths.get(outputFolder.toString(), filenameWithoutExtension + suffix + extension);
	    }
	}
	
	// Print some timing statistics
	private void printStatistics(long readFileStart, long readFileEnd, long registerStart, long registerEnd, long saveStart, long saveEnd)
	{
		long readDuration = (readFileEnd - readFileStart) / 1000000;
		long registerDuration = (registerEnd - registerStart) / 1000000;
		long saveDuration = (saveEnd - saveStart) / 1000000;
		System.out.println(String.format("Load %d, register %d, save %d (ms)", readDuration, registerDuration, saveDuration));
	}
	
	// Returns a copy of the given image cropped to a rectangular region of interest.
	private ImageProcessor cropImage(ImagePlus imagePlus, Rectangle rect)
	{
		ImageProcessor imp = imagePlus.getProcessor();
		Rectangle origRoi = imp.getRoi();
		
		imp.setRoi(rect);
		ImageProcessor crop = imp.crop();
		imp.setRoi(origRoi);
		
		return crop;
	}
}
