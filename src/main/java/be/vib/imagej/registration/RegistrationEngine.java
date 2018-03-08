package be.vib.imagej.registration;

import java.awt.Rectangle;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import be.vib.bits.QExecutor;
import be.vib.bits.QFunction;
import be.vib.bits.QValue;
//import be.vib.imagej.ImageUtils;
import ij.ImagePlus;
import ij.ImageStack;
import ij.io.Opener;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

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
	
	public void register(List<Path> inputFiles, Path outputFolder, Rectangle rect)
	{
		int sliceNr = 0;
		for (Path inputFile : inputFiles)
		{
			if (isCancelled())
				continue; // CHECKME: break instead?
			
			Opener opener = new Opener();  
			System.out.print("Loading " + inputFile.toString() + "...");
			ImagePlus img = opener.openImage(inputFile.toString());
			System.out.println(" done.");
			
//			try
//			{
//				// FIXME! how does registerer get its params?
//			    registerer.setParams(xxxxx);
//				xxxresult = QExecutor.getInstance().submit(registerer).get(); // TODO: check what happens to quasar::exception_t if thrown from C++ during the registration task.
//                // TODO: shift slice over offset that was calculated and save it to the outputFolder
//			}
//			catch (ExecutionException | InterruptedException e)
//			{
//				e.printStackTrace();
//			}
			
			// Progress feedback
			sliceNr++;
			final int numSlices = inputFiles.size();
			publish((100 * sliceNr) / numSlices);
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
	
}
