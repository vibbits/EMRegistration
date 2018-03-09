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
	
	public RegistrationEngine()
	{
		this.registerer = new Registerer();
	}
	
	// IMPROVEME: we do not need to transfer the complete image to Quasar,
	//            only the region over which we search for a match
	
	// IMPROVEME: loading large images takes a significant amount of time. It may make sense to load
	//            the next image slice asynchronously, in parallel with registration of the current slice.
	
	public void register(List<Path> inputFiles, Path outputFolder, Rectangle rect)
	{
		// ImageJ image opener object
		Opener opener = new Opener();  

		// Extract reference patch from the first image
		ImagePlus firstImage = opener.openImage(inputFiles.get(0).toString());
		ImageProcessor referencePatch = cropImage(firstImage, rect);
		
		final int firstSliceX = rect.x;
		final int firstSliceY = rect.y;
		
		int prevX = firstSliceX;
		int prevY = firstSliceY;
				
		// --
		final int numSlices = inputFiles.size();
		int sliceNr = 1;
		for (Path inputFile : inputFiles)
		{
			if (isCancelled())
				break;
			
			System.out.print("Loading " + sliceNr + "/" + numSlices + " : " + inputFile.toString() + "...");
			ImagePlus imagePlus = opener.openImage(inputFile.toString());
			System.out.println(" done.");
			
			if (imagePlus == null)
				break; // TODO: this indicates an error during reading of the image - make sure the user is informed properly
			
			ImageProcessor image = imagePlus.getProcessor();
			if (image == null)
				break; // TODO: inform user
			
			try
			{
				// Calculate the shift required to register this slice to the previous one
			    registerer.setParameters(image, referencePatch, prevX, prevY);
				RegistrationResult result = QExecutor.getInstance().submit(registerer).get(); // TODO: check what happens to quasar::exception_t if thrown from C++ during the registration task.
								
				// Shift the image to register it
				int shiftX = result.posX - firstSliceX;
				int shiftY = result.posY - firstSliceY;
				image.translate(-shiftX, -shiftY);
				System.out.println("--> Shift: X=" + shiftX + " Y=" + shiftY);
				
				// Save the registered image to the output folder
				FileSaver saver = new FileSaver(imagePlus);
				Path resultPath = Paths.get(outputFolder.toString(), String.format("registered_slice%05d.tif", sliceNr));
				saver.saveAsTiff(resultPath.toString());
				
				// Free image resources now (to avoid Java out-of-memory issues)
				imagePlus.close();

				// Remember the position of the best matching patch,
				// we use its position as an estimate for its position in the next slice.
				prevX = result.posX;
				prevY = result.posY;
			}
			catch (ExecutionException | InterruptedException e)
			{
				e.printStackTrace();
			}
			
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
	
	private ImageProcessor cropImage(ImagePlus imagePlus, Rectangle rect)
	{
		ImageProcessor imp = imagePlus.getProcessor();
		imp.setRoi(rect);
		return imp.crop();
	}
}
