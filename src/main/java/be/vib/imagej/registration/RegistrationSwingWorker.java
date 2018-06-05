package be.vib.imagej.registration;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

public class RegistrationSwingWorker extends SwingWorker<Void, Double>
{
	private RegistrationParameters params;
	private JProgressBar progressBar;
	private Runnable whenDone;  // Will be run on the EDT as soon as the RegistrationSwingWorker is done registering. Can be used to indicate in the UI that we are done.
	private Consumer<String> whenError;
	
	// The progressbar accepts values from 0 - 1000 (for 0 to 100%)
	// but with 10x accuracy so the progressbar also moves if we only make 0.1% progress,
	// which is common because we typically process stacks with hundreds of files)
	public static final int progressBarScaleFactor = 10;
	
	private class SwingRegistrationEngine extends RegistrationEngine  // CHECKME: can we get rid of the SwingRegistrationEngine class?
	{
		SwingRegistrationEngine()
		{
			super();
		}
		
		@Override 
		protected void publish(Double... chunks )
		{
			RegistrationSwingWorker.this.publish(chunks);
		}
		
		@Override 
		protected void process(List<Double> chunks)
		{
			RegistrationSwingWorker.this.process(chunks);
		}
		
		@Override 
		public boolean isCancelled()
		{
			return RegistrationSwingWorker.this.isCancelled();
		}
	}
	
	public RegistrationSwingWorker(RegistrationParameters params, JProgressBar progressBar, Runnable whenDone, Consumer<String>  whenError)
	{
		this.params = params;
		this.progressBar = progressBar;
		this.whenDone = whenDone;
		this.whenError = whenError;
		
		// Debug
		// System.out.println("RegistrationSwingWorker maxShiftX=" + params.maxShiftX + " sliceThicknessCorrection=" + params.sliceThicknessCorrection + " sliceThicknessNM=" + params.sliceThicknessNM + " autoCropRect=" + params.autoCropRect);
	}
	
	@Override
	protected Void doInBackground() throws Exception
	{
		// The method doInBackground is run is a thread different from the Java Event Dispatch Thread (EDT).
		// Do not update Java Swing components here.
				
		List<Path> slices = null;
		
		if (params.sliceThicknessCorrection)
		{
			ResampleInfo[] resampleInfo = SliceThicknessCorrection.nearestNeighborResample(params.inputFiles, params.sliceThicknessNM);
			SliceThicknessCorrection.printResampleInfo(resampleInfo);
			slices = SliceThicknessCorrection.getResampledFiles(resampleInfo);
		}
		else
		{
			slices = params.inputFiles;
		}
		
		RegistrationEngine engine = new SwingRegistrationEngine();
		engine.register(slices, params.outputFolder, params.templatePatchRect, params.maxShiftX, params.maxShiftY, params.autoCropRect);
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
}
