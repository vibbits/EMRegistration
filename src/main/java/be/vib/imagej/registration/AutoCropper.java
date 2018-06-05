package be.vib.imagej.registration;

import java.awt.Rectangle;

import ij.ImagePlus;
import ij.process.ImageProcessor;

public class AutoCropper
{
	// "Black" here means pixels with value 0.
	// Never returns null; the returned rectangle will be empty (isEmpty()==true) for images without black border.
	static public Rectangle getNonblackRegion(ImagePlus image)
	{
		if (image == null) return null;
		
		ImageProcessor imp = image.getProcessor();
		
		final int imageHeight = image.getHeight();
		final int imageWidth = image.getWidth();
		
		final int topBlackMargin = getNumBlackRows(imp, 0, imageHeight-1, +1);
		final int bottomBlackMargin = getNumBlackRows(imp, imageHeight-1, 0, -1);
		
		final int leftBlackMargin = getNumBlackColumns(imp, 0, imageWidth-1, +1);
		final int rightBlackMargin = getNumBlackColumns(imp, imageWidth-1, 0, -1);
		
		final int x = leftBlackMargin;
		final int y = topBlackMargin;
		final int width = imageWidth - leftBlackMargin - rightBlackMargin;
		final int height = imageHeight - topBlackMargin - bottomBlackMargin;
		
		if (width > 0 && height > 0) 
			return new Rectangle(x, y, width, height);
		else
			return new Rectangle();  // empty rectangle; signals a completely black image
	}
	
	// TODO? can we generalize/unify the black columns and rows functions below?

	private static int getNumBlackRows(ImageProcessor imp, int startRow, int endRow, int rowIncrement)
	{
		final int width = imp.getWidth();
		
		int numBlackRows = 0;
		for (int row = startRow; row != endRow; row += rowIncrement)
		{
			for (int col = 0; col < width; col++)
			{
				if (imp.get(col, row) != 0)
				{
					return numBlackRows;
				}
			}
			
			// All pixels on current row are black
			numBlackRows++;
		}
		return numBlackRows;
	}

	private static int getNumBlackColumns(ImageProcessor imp, int startCol, int endCol, int colIncrement)
	{
		final int height = imp.getHeight();
		
		int numBlackColumns = 0;
		for (int col = startCol; col != endCol; col += colIncrement)
		{
			for (int row = 0; row < height; row++)
			{
				if (imp.get(col, row) != 0)
				{
					return numBlackColumns;
				}
			}
			
			// All pixels on current column are black
			numBlackColumns++;
		}
		return numBlackColumns;
	}
	

}
