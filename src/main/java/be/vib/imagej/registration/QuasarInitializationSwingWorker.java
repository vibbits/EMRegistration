package be.vib.imagej.registration;

import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import be.vib.bits.JavaQuasarBridge;
import ij.IJ;

public class QuasarInitializationSwingWorker extends SwingWorker<Void, Void>
{
	private Runnable onSuccess;
	private Runnable onFailure;
	
	public QuasarInitializationSwingWorker(Runnable onSuccess, Runnable onFailure) 
	{
		this.onSuccess = onSuccess;
		this.onFailure = onFailure;
	}
	
	@Override
	public Void doInBackground() throws InterruptedException, ExecutionException
	{	
		JavaQuasarBridge.startQuasar("cuda", false); // throws a RuntimeException on failure - if so it gets wrapped as an ExecutionException and caught in done()	
		JavaQuasarBridge.extractAndLoadModule("be.vib.imagej.registration.QuasarInitializationSwingWorker", "qlib/registration.qlib", "registration.qlib", "vib_em_registration_");
		JavaQuasarBridge.setQuasarPath(getFijiQuasarPath());
		return null;
	}
	
	@Override
	public void done()
	{
		try
		{
			get(); // get the result of doInBackground() - Void if it was successful, an ExecutionException if it failed
			onSuccess.run();
		}
		catch (ExecutionException e)
		{
			e.getCause().printStackTrace();
			onFailure.run();
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
	}
	
	private static String getFijiQuasarPath()
	{
		// TODO: check platform (windows, linux, mac) and architecture (32/64 bit) - for now only 64bit Windows is supported
		return IJ.getDir("imagej") + java.io.File.separator + "jars" + java.io.File.separator + "win64" + java.io.File.separator + "Quasar";
	}
}