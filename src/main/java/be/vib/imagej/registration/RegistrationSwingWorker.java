package be.vib.imagej.registration;

import java.nio.file.Path;
import java.util.List;

import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

public class RegistrationSwingWorker extends SwingWorker<Void, Integer>
{
	private RegistrationParameters params;
	private JProgressBar progressBar;
	private Runnable whenDone;  // Will be run on the EDT as soon as the RegistrationSwingWorker is done registering. Can be used to indicate in the UI that we are done.
	
	private class SwingRegistrationEngine extends RegistrationEngine  // CHECKME: can we get rid of the SwingRegistrationEngine class?
	{
		SwingRegistrationEngine()
		{
			super();
		}
		
		@Override 
		public void publish(Integer... chunks )
		{
			RegistrationSwingWorker.this.publish(chunks);
		}
		
		@Override 
		public void process(List<Integer> chunks)
		{
			RegistrationSwingWorker.this.process(chunks);
		}
		
		@Override 
		public boolean isCancelled()
		{
			return RegistrationSwingWorker.this.isCancelled();
		}
	}
	
	public RegistrationSwingWorker(RegistrationParameters params, JProgressBar progressBar, Runnable whenDone)
	{
		this.params = params;
		this.progressBar = progressBar;
		this.whenDone = whenDone;
	}
	
	@Override
	public Void doInBackground()
	{
		// The method doInBackground is run is a thread different from the Java Event Dispatch Thread (EDT).
		// Do not update Java Swing components here.
		
		List<Path> slices = null;
		
		if (params.sliceThicknessCorrection)
		{
			ResampleInfo[] resampleInfo = SliceThicknessCorrection.nearestNeighborResample(params.inputFiles, params.sliceThickness);
			SliceThicknessCorrection.printResampleInfo(resampleInfo);
			slices = SliceThicknessCorrection.getResampledFiles(resampleInfo);
		}
		else
		{
			slices = params.inputFiles;
		}
		
		RegistrationEngine engine = new SwingRegistrationEngine();
		engine.register(slices, params.outputFolder, params.rect, params.maxShiftX, params.maxShiftY);
		return null;
	}
	
	@Override
	protected void process(List<Integer> percentages)
	{
		// Method process() is executed on the Java EDT, so we can update the UI here.	
		for (Integer percentage : percentages)
		{
			progressBar.setValue(percentage);
		}
	}
	
	@Override
	public void done()  
	{
		// Method done() is executed on the Java EDT, we can update the UI here.	
		whenDone.run();
		
		if (isCancelled())
			return;
	}
}
