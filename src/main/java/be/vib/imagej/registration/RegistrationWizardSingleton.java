package be.vib.imagej.registration;

import java.awt.Dimension;

// A Bill Pugh singleton for the registration wizard.
public class RegistrationWizardSingleton
{
	private RegistrationWizardSingleton()
	{
	}
	
	public static Wizard getInstance()
	{
		return SingletonHelper.INSTANCE;
	}

	private static class SingletonHelper
	{
		private static final Wizard INSTANCE = createWizard();
	}
	
	private static Wizard createWizard()
	{
		Wizard wizard = new Wizard("EM Registration", new WizardModel());

		WizardPage[] pages = { new WizardPageInitializeQuasar(wizard, "Initialization"),
							   new WizardPageSelectFolders(wizard, "Select Folders"),
							   new WizardPageSpecifyPatch(wizard, "Specify Patch"),
							   new WizardPageRegistration(wizard, "Registration") };
		
		wizard.build(pages);
		wizard.setMinimumSize(new Dimension(650, 540)); // IMPROVEME: this ensures that WizardPageRegistration is tall enough, and WizardPageSelectFolders wide enough, enforce that there instead of here.
		wizard.pack();
		wizard.moveToMiddleOfScreen();
				
		return wizard;
	}	
}
