package be.vib.imagej.registration;

import java.awt.Rectangle;
import java.nio.file.Path;
import java.util.List;

import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

public class RegistrationSwingWorker extends SwingWorker<Void, Integer>
{
	private List<Path> inputFiles;
	private Path outputFolder;
	private Rectangle rect;
	private int maxShiftX;
	private int maxShiftY;
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
	
	public RegistrationSwingWorker(List<Path> inputFiles, Path outputFolder, Rectangle rect, int maxShiftX, int maxShiftY, JProgressBar progressBar, Runnable whenDone)
	{
		this.inputFiles = inputFiles;
		this.outputFolder = outputFolder;
		this.rect = rect;
		this.maxShiftX = maxShiftX;
		this.maxShiftY = maxShiftY;
		this.progressBar = progressBar;
		this.whenDone = whenDone;
	}
	
	@Override
	public Void doInBackground()
	{
		// The method doInBackground is run is a thread different from the Java Event Dispatch Thread (EDT).
		// Do not update Java Swing components here.
		
		RegistrationEngine engine = new SwingRegistrationEngine();
		engine.register(inputFiles, outputFolder, rect, maxShiftX, maxShiftY);
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
