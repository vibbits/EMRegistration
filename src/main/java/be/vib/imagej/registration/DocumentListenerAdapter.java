package be.vib.imagej.registration;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

class DocumentListenerAdapter implements DocumentListener
{
	public void handleChange(DocumentEvent e)
	{
		// Override this to execute the same method whenever *any* of the
		// three update methods get called. 
	}
	
	@Override
	public void changedUpdate(DocumentEvent e)
	{
		handleChange(e);
	}

	@Override
	public void insertUpdate(DocumentEvent e)
	{
		handleChange(e);
	}

	@Override
	public void removeUpdate(DocumentEvent e)
	{
		handleChange(e);			
	}
}