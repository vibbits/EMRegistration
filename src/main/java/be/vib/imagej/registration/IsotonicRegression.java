package be.vib.imagej.registration;

public class IsotonicRegression
{
	// Perform unweighted linear ordering isotonic regression
	public static double[] Fit(double[] a)
	{
		final int n = a.length;
		
		// Initialize each weight to 1
		double[] w = new double[n];
		for (int i = 0; i < n; i++)
			w[i] = 1.0;
		
		return Fit(a, w);
	}
	
	// Perform linear ordering isotonic regression
	// via the Pool Adjacent Violators Algorithm.
	//
	// The array a[] holds the input values, w[] the weights
	// (w[i] > 0 for all i), i=1..n
	//
	// The function IsotonicRegression(a, w) returns an array y[] such that
	//    sum over all i of w[i] * (y[i] - a[i])^2 is minimized
	// with respect to
	//    y[1] <= y[2] <= ... <= y[n]
	//
	// References:
	// - http://stat.wikia.com/wiki/Isotonic_regression
	// - https://en.wikipedia.org/wiki/Isotonic_regression
	//
	public static double[] Fit(double[] a, double[] w)
	{
		final int n = a.length;
		assert(w.length == n);
		
		int[] S = new int[n+1];  // defines which old point the new point corresponds
		double[] aprime = new double[n];
		double[] wprime = new double[n];
		
		aprime[0] = a[0];
		wprime[0] = w[0];
		
		S[0] = 0;  // will never be changed below
		S[1] = 1;  // this may get updated below
		
		int j = 0;
		
		for (int i = 1; i < n; i++)
		{
			j += 1;
			
			aprime[j] = a[i];
			wprime[j] = w[i];
			
			while ((j > 0) && (aprime[j] < aprime[j-1]))
			{
				aprime[j-1] = (wprime[j] * aprime[j] + wprime[j-1] * aprime[j-1]) / (wprime[j] + wprime[j-1]);
				wprime[j-1] += wprime[j];
				j -= 1;
			}
			
			S[j+1] = i + 1;
		}
		
		assert(j >= 0);
		assert(j <= n-1);
				
		// The values aprime[0 to j inclusive] are the increasing output values of the isotonic regression,
		// but where successive identical values are coalesced into a single number. From the S[] array we can recover the
		// actual result array which will have (non-strictly) increasing values where identical numbers are repeated.
		// So aprime[] may have less elements than a[] and y[], but a[] and y[] will have the same number of elements.
		
		double[] y = new double[n];
		for (int k = 0; k <= j; k++)
		{
			for (int l = S[k]; l < S[k+1]; l++)
			{
				y[l] = aprime[k];
			}
		}
		
		return y;
	}
	
	private static void CheckEquality(double[] y, double[] target, String testcase)
	{
		if (y.length != target.length)
		{
			System.out.println("FAILED: " + testcase + ": Arrays differ in length.");
			return;
		}
		
		double eps = 1e-5;
		for (int i = 0; i < y.length; i++)
		{
			if (Math.abs(y[i] - target[i]) > eps)
			{
				System.out.println("FAILED: " + testcase + ": values differ at index " + i + " (target: " + target[i] + "; actual: " + y[i] + ")");
				return;
			}
		}
		System.out.println("SUCCESS: " + testcase);
	}
	
	public static void main(String[] args)
	{
		// Define some test cases
		final double a1[] = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0};
		final double y1[] = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0};
		
		final double a2[] = {10.0, 9.0, 8.0, 7.0, 6.0, 5.0, 4.0, 3.0, 2.0, 1.0};
		final double y2[] = { 5.5, 5.5, 5.5, 5.5, 5.5, 5.5, 5.5, 5.5, 5.5, 5.5};
		
		final double a3[] = {1.0, 2.0, 1.8, 1.5, 4.0, 5.0, 4.8, 8.0, 10.0};
		final double y3[] = {1.0, 1.7666666666666666, 1.7666666666666666, 1.7666666666666666, 4.0, 4.9, 4.9, 8.0, 10.0};

		final double a4[] = {1.0, 2.0, 1.8, 1.5, 4.0, 5.0, 4.8, 8.0, 7.0};
		final double y4[] = {1.0, 1.7666666666666666, 1.7666666666666666, 1.7666666666666666, 4.0, 4.9, 4.9, 7.5, 7.5};

		final double a5[] = {1.0};
		final double y5[] = {1.0};

		final double a6[] = {2.0, 1.0};
		final double y6[] = {1.5, 1.5};

		// Test a few cases		
		CheckEquality(IsotonicRegression.Fit(a1), y1, "Testcase 1");
		CheckEquality(IsotonicRegression.Fit(a2), y2, "Testcase 2");
		CheckEquality(IsotonicRegression.Fit(a3), y3, "Testcase 3");
		CheckEquality(IsotonicRegression.Fit(a4), y4, "Testcase 4");
		CheckEquality(IsotonicRegression.Fit(a5), y5, "Testcase 5");
		CheckEquality(IsotonicRegression.Fit(a6), y6, "Testcase 6");
	}
}
