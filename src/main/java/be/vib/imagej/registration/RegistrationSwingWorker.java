package be.vib.imagej.registration;

import java.awt.Rectangle;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import be.vib.bits.QExecutor;
// import ij.IJ; // For debugging, e.g. to save imageProcessor to TIFF
import ij.ImagePlus;
import ij.io.FileSaver;
import ij.io.Opener;
import ij.process.ImageProcessor;

// The RegistrationSwingWorker class is the main access point from ImageJ
// to our registration machinery. It iterates over the images (=z-slices) in the input folder
// and registers them one by one with their predecessor slice.
public class RegistrationSwingWorker extends SwingWorker<Void, Double>
{
	private RegistrationParameters params;
	private JProgressBar progressBar;
	private Runnable whenDone;  // Will be run on the EDT as soon as the RegistrationSwingWorker is done registering. Can be used to indicate in the UI that we are done.
	private Consumer<String> whenError;
	
	private Registerer registerer;
	
	// The progress bar accepts values from 0 - 1000 (for 0 to 100%)
	// but with 10x accuracy so the progress bar also moves if we only make 0.1% progress,
	// which is common because we typically process stacks with hundreds of files)
	public static final int progressBarScaleFactor = 10;
	
	public RegistrationSwingWorker(RegistrationParameters params, JProgressBar progressBar, Runnable whenDone, Consumer<String>  whenError)
	{
		this.params = params;
		this.progressBar = progressBar;
		this.whenDone = whenDone;
		this.whenError = whenError;
		this.registerer = new Registerer();
	}
	
	@Override
	protected Void doInBackground() throws Exception
	{
		// The method doInBackground is run is a thread different from the Java Event Dispatch Thread (EDT).
		// Do not update Java Swing components here.
				
		List<Path> slices = getSlicesForRegistration();
		register(slices, params.outputFolder, params.templatePatchRect, params.maxShiftX, params.maxShiftY, params.autoCropRect);
		return null;
	}
	
	@Override
	protected void process(List<Double> percentages)
	{
		// Method process() is executed on the Java EDT, so we can update the UI here.	
		
		for (Double percentage : percentages)
		{
			progressBar.setValue((int)(progressBarScaleFactor * percentage));
			progressBar.setString(String.format("%.1f%%",percentage));
		}
	}
	
	@Override
	protected void done()  
	{
		// Method done() is executed on the Java EDT, we can update the UI here.	
		try
		{
			get();
			whenDone.run();
		}
		catch (ExecutionException e)
		{
			// We get here when an exception is thrown in doInBackground().
			whenError.accept(e.getCause().getMessage());
        }
		catch (CancellationException e)
		{
			// We get here if the users presses the cancel button during registration
			whenDone.run();	
		}
		catch (Exception e)
		{
			whenError.accept(e.getMessage());
		}		
	}

	private List<Path> getSlicesForRegistration()
	{
		List<Path> slices = null;
		
		if (params.sliceThicknessCorrection)
		{
			ResampleInfo[] resampleInfo = SliceThicknessCorrection.nearestNeighborResample(params.inputFiles, params.sliceThicknessNM, params.preserveSliceOrder);
			SliceThicknessCorrection.printResampleInfo(resampleInfo);
			slices = SliceThicknessCorrection.getResampledFiles(resampleInfo);
		}
		else
		{
			slices = params.inputFiles;
		}
		return slices;
	}
	
	private void register(List<Path> inputFiles, Path outputFolder, Rectangle templatePatchRect, int maxShiftX, int maxShiftY, Rectangle autoCropRect) throws Exception  
	// autoCropRect==null means don't auto-crop
	{
		// Show info on reference patch
		System.out.println(String.format("Reference patch: top-left corner x=%d y=%d, width=%d height=%d", templatePatchRect.x, templatePatchRect.y, templatePatchRect.width, templatePatchRect.height));
		System.out.println("Maximum shift in pixels: X=" + maxShiftX + " Y=" + maxShiftY);
		
		if (autoCropRect != null)
		{
			System.out.println("Autocropping to " + autoCropRect);
						
			Rectangle newTemplatePatchRect = autoCropRect.intersection(templatePatchRect);
			if (newTemplatePatchRect.isEmpty())
				throw new RuntimeException("The auto-crop rectangle and the user-defined template patch do not overlap. After cropping there is no template patch to use for registration anymore. Please select a template patch that overlaps with the non-black region of the image.");
			
			newTemplatePatchRect.x = newTemplatePatchRect.x - autoCropRect.x;
			newTemplatePatchRect.y = newTemplatePatchRect.y - autoCropRect.y;
			
			templatePatchRect = newTemplatePatchRect;
			System.out.println("New template patch rect coordinates, with respect to autocropped image: " + templatePatchRect);
		}

		// Coordinates of the top-left corner of the reference patch (in the original image if autoCropRect is null, relative to TL corner of autoCropRect otherwise)
		final int initialX = templatePatchRect.x;
		final int initialY = templatePatchRect.y;
		
		// Extract reference patch from the first image
		ImagePlus firstImage = loadImage(inputFiles.get(0).toString(), autoCropRect);
		
		ImageProcessor referencePatch = cropImage(firstImage, templatePatchRect);
  		//IJ.save(new ImagePlus("reference patch", referencePatch), "e:\\emreg_refpatch.png");

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
			float averageSliceRegistrationDuration = -1.0f;  // -1 = sentinel = not estimated yet; otherwise time in ns
			
			System.out.println("Image " + sliceNr + "/" + numSlices + " : " + inputFile.toString() + "...");
			loadStart = System.nanoTime();
			ImagePlus imagePlus = loadImage(inputFile.toString(), autoCropRect);

			loadEnd = System.nanoTime();
			
			ImageProcessor image = imagePlus.getProcessor();
			if (image == null)
				throw new RuntimeException("Failed to get ImageProcessor for image " + inputFile.toString());

			// Find coordinates (with respect to the full image) of the region where we will look for the patch.
			// So this rectangular crop of the image needs to be sent to Quasar.
			int cropTopLeftX = Math.max(0, prevX - maxShiftX);
			int cropTopLeftY = Math.max(0, prevY - maxShiftY);
			int cropBottomRightX = Math.min(prevX + maxShiftX + referencePatch.getWidth(), image.getWidth() -  1);
			int cropBottomRightY = Math.min(prevY + maxShiftY + referencePatch.getHeight(), image.getHeight() - 1);
			
			Rectangle cropRect = new Rectangle(cropTopLeftX, cropTopLeftY, cropBottomRightX - cropTopLeftX, cropBottomRightY - cropTopLeftY);
			ImageProcessor croppedImage = cropImage(imagePlus, cropRect);
			
	  		//IJ.save(new ImagePlus("to register", croppedImage), "e:\\emreg_toregister"+sliceNr+".png");

			
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
			// to coordinates in the full original image.  (CHECKME: is comment correct if autoCropRect != null ?)
			int bestPosX = cropTopLeftX + result.posX;
			int bestPosY = cropTopLeftY + result.posY;
			
			// Shift the image to register it
			int shiftX = bestPosX - initialX;
			int shiftY = bestPosY - initialY;
			image.translate(-shiftX, -shiftY);
			System.out.println("Shift: dx=" + shiftX + " dy=" + shiftY + " compared to first slice; dx=" + (bestPosX - prevX) +" dy=" + (bestPosY - prevY) + " compared to previous slice");
			
			// Update the most recent position of the best matching patch,
			// we use its position as an estimate for the location of the patch in the next image.
			prevX = bestPosX;
			prevY = bestPosY;
			
			// Save the registered image to the output folder			
			saveStart = System.nanoTime();
			FileSaver saver = new FileSaver(imagePlus);
			Path resultPath = suggestOutputFilename(sliceNr - 1, inputFile, outputFolder);
			saver.saveAsTiff(resultPath.toString());
			saveEnd = System.nanoTime();
	
			// Show some timing statistics
			printStatistics(loadStart, loadEnd, registerStart, registerEnd, saveStart, saveEnd);
			
			// Update slice registration estimate
			// (Exponential moving average)
			float sliceRegistrationDuration = saveEnd - loadStart;
			float alpha = 0.5f;  // weight decrease factor for exponential moving average
			if (averageSliceRegistrationDuration < 0)  // we don't have an estimate yet
			{
				averageSliceRegistrationDuration = sliceRegistrationDuration;
			}
			else
			{
				averageSliceRegistrationDuration = alpha * sliceRegistrationDuration + (1.0f - alpha) * averageSliceRegistrationDuration;
			}
			System.out.println("ETA=" + humanReadableDuration((numSlices - sliceNr) * averageSliceRegistrationDuration));
			// TODO: somehow show this in the UI. In the slider? How does that work with publish()...?
			
			// Progress feedback
			publish((100.0 * sliceNr) / numSlices);
			sliceNr++;
		}
	}
	
	// humanReadableDuration() turns a duration in nanoseconds into a human readable string
	// with hours, minutes and seconds. If the duration is shorter than
	// an hour resp. a minute, only minutes and seconds resp. only seconds
	// are used in the string.
	private static String humanReadableDuration(float nanoSeconds)
	{
		long seconds = Math.round(nanoSeconds / 1e9f);
		long minutes = 0;
		long hours = 0;
		
		hours = seconds / 3600;
		seconds -= 3600 * hours;
		
		minutes = seconds / 60;
		seconds -= 60 * minutes;
		
		if (hours > 0)
		{
			return hours + " h " + minutes + " min " + seconds + " sec";
		}
		else if (minutes > 0)
		{
			return minutes + " min " + seconds + " sec";
		}
		else
		{
			return seconds + " sec";			
		}
	}
	
	// Returns an output file path like this:
	// desired output folder + slice nr + original filename (without extension) + _registered + original extension (if any)
	// IMPROVEME:
	// If we corrected for thickness, we probably want an indication of the sampled z and the closest original z (but we don't have that information available here)
	// (for now we added a counter prefix so that (1) if we correct for slice thickness and use the same input file multiple times, at least we get unique filenames;
	// and (2) if we re-order input files (because the input z's are not monotonically increasing, the registered output files reflect that different order!)
	// but this issue needs some more thought)
	private static Path suggestOutputFilename(int sliceNr, Path inputFilePath, Path outputFolder)
	{
		String prefix = String.format("%05d", sliceNr) + "_";
		String suffix = "_registered";	
		String filename = inputFilePath.getName(inputFilePath.getNameCount() - 1).toString(); // filename part only (including extension, if any)
		
	    int dotIndex = filename.toString().lastIndexOf('.');
	    if (dotIndex == -1)
	    {
	    	// Original file has no extension, just append the suffix.
	    	return Paths.get(outputFolder.toString(), prefix + filename + suffix);
	    }
	    else
	    {
	    	// Original file has an extension, insert the suffix just before the extension in the original filename
	    	String filenameWithoutExtension = filename.substring(0, dotIndex);
	    	String extension = filename.substring(dotIndex);
	    	return Paths.get(outputFolder.toString(), prefix + filenameWithoutExtension + suffix + extension);
	    }
	}
	
	// Print some timing statistics
	private static void printStatistics(long readFileStart, long readFileEnd, long registrationStart, long registrationEnd, long saveStart, long saveEnd) // time stamps in nanoseconds
	{
		long readTimeMilliSec = (readFileEnd - readFileStart) / 1000000;
		long registrationTimeMilliSec = (registrationEnd - registrationStart) / 1000000;
		long saveTimeMilliSec = (saveEnd - saveStart) / 1000000;
		System.out.println(String.format("Load %d, register %d, save %d (ms)", readTimeMilliSec, registrationTimeMilliSec, saveTimeMilliSec));
	}
	
	// Returns a copy of the given image cropped to a rectangular region of interest.
	private static ImageProcessor cropImage(ImagePlus imagePlus, Rectangle rect)
	{
		ImageProcessor imp = imagePlus.getProcessor();
		Rectangle origRoi = imp.getRoi();
		
		imp.setRoi(rect);
		ImageProcessor crop = imp.crop();
		imp.setRoi(origRoi);
		
		return crop;
	}
	
	private ImagePlus loadImage(String filename, Rectangle cropRect) throws Exception
	// if cropRect is non-null the image gets cropped to it after loading,
	// if cropRect is null the loaded image is returned as-is.
	// (should never return null)
	{
		Opener opener = new Opener();
		ImagePlus imagePlus = opener.openImage(filename);
		if (imagePlus == null)
		{
			throw new RuntimeException("Failed to load image " + filename);
		}
		
		if (cropRect == null)
		{
			return imagePlus;
		}
		else
		{
			imagePlus.setRoi(cropRect);
			return imagePlus.crop();
		}
	}
}
