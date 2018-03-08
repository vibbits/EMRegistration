package be.vib.imagej.registration;

import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, menuPath = "Plugins>EM Registration>Register")
public class RegistrationIJ2 implements Command
{
	@Parameter
	private LogService log;
	
	@Override
	public void run() 
	{
		log.info("VIB EM Registration plugin");
		
		Wizard wizard = RegistrationWizardSingleton.getInstance();
		wizard.setVisible(true);
		
		// After displaying the registration wizard the ImageJ plugin run() method finishes immediately,
		// but the wizard is still visible and active.
	}
}
	