package be.vib.imagej.registration;

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
		wizard.pack();
		wizard.moveToMiddleOfScreen();
				
		return wizard;
	}	
}
