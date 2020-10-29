import java.awt.Color;
import java.util.ArrayList;

public class DecentSpectroscopy // Not completely focused yet, streches toward the red side. Errors on the red part ar e far larger than errors on the blue part.
{								// Could be because of the camera optics thing I realized
	private static double D_CAMERA_GRATING = 3; // mm
	private static double D_GRATING_REFINER = 327.7; //mm ACTUALLY SHOULD BE 169.5 unless you zoomed in ACTUALLY NOW USING BIG BOX ITS 326.7
	private static final double D_GRATING_LINES = 2000; // nm
	private static double D_LENGTH_REFINER = 370.5;
	
	private static double N = 1;
	private static double CAMERA_ERROR = 205.12;
	
	private static int THRESHOLD_INTENSITY_MIN = 0;
	private static int THRESHOLD_INTENSITY_MAX = 210;
	private static int BOOST = 0;
	private static int RADIUS = 2;
	
	private static int INTENSITY_NOISE = 15;
	
	private static int FALIURE_PENALTY = 0;
	
	private static double VISIBILITY_ASSISTANCE = 1.2;
	
	private static int STD_WIDTH = 1000;
	
	private static Picture Spectrum = new Picture("imgs/Data/Spectra.jpg"); // use .png for the wiki spectrum
	private static double SPECTRUM_MAX = 800; // 800 for Spectra.jpg
	private static double SPECTRUM_MIN = 350; // 350 for Spectra.jpg
	
	private static boolean UNSAVED = true;
	
	private static int RANGE = 10;
	
    public static double intensity(Color color, boolean weighted) 
    {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        if (r == g && r == b) return r; 
        
        float[] hsbv = new float[3];
        Color.RGBtoHSB(r, g, b, hsbv);
        
        
        if (0.299*r + 0.587*g + 0.114*b > THRESHOLD_INTENSITY_MIN)
        	return 0.299*r + 0.587*g + 0.114*b; 
        
        return hsbv[1]*255 - FALIURE_PENALTY;
        
        
        //return (0.299*r + 0.587*g + 0.114*b + hsbv[1]*255)/2;
        // This was slightly modified in order to include more in the final output
    }
    
    // public static double antiintensity(Color color) -- This would be for colors in the near-ultraviolet range.
	
	public static int getWavelength(double l)
    {
    	double h = Math.sqrt(Math.pow(D_CAMERA_GRATING + D_GRATING_REFINER, 2) + Math.pow(l, 2));
    	return (int) (l/h * D_GRATING_LINES/ N - CAMERA_ERROR);
    }
	
	public static Color getColor(int wavelength, double intensity)
	{
		if (wavelength > 740 || wavelength < 380)
			return new Color((int)intensity, (int)intensity, (int)intensity);
		
		Color col = Spectrum.get(Math.min((int) ((wavelength - SPECTRUM_MIN) / (SPECTRUM_MAX - SPECTRUM_MIN) * Spectrum.width()), Spectrum.width()-1), Spectrum.height()/2);
		return new Color(Math.min((int)(col.getRed() * (intensity/255)), 255), Math.min((int)(col.getGreen() * (intensity/255)), 255), Math.min((int)(col.getBlue() * (intensity/255)), 255));
	}
	
	private static int[] getVisibleWVals(Picture spec, Picture output1, Picture output2) // TODO: Rather than look at one slice, find the max over a set of slices
    {
    	int width  = spec.width();
        int height = spec.height();
        
        int[] wvals = new int[STD_WIDTH];
        int[] ovals = new int[STD_WIDTH];
        
        for (int i = 0; i < STD_WIDTH; i++)
        {
        	wvals[i] = 0;
        	ovals[i] = 0;
        }
        
        int x = 0;
        double xi = 0;
        for (int i = 0; i < width; i++)
        {
        	Color color = spec.get(i, height/2);
        	if (intensity(color, false) >= xi)
        	{
        		xi = intensity(color, false);
        		x = i;
        	}
        }
        
        for (int col = 0; col < width; col++)
        {
        	for (int z = -1 * RANGE; z <= RANGE; z++)
        	{
	        	double l;
	    		l = (x - col) * D_LENGTH_REFINER / (x); 
	        	Color color = spec.get(col, height/2 + z);
	        	
	        	if (intensity(color, false) - INTENSITY_NOISE + BOOST > THRESHOLD_INTENSITY_MIN && intensity(color, false) - INTENSITY_NOISE + BOOST < THRESHOLD_INTENSITY_MAX)
	        	{
		        	int wavelength = getWavelength(l);
		        	
		        	if (wavelength >= 370 && wavelength <= 741)
		        	{
			        	double intensity = intensity(color, false) - INTENSITY_NOISE + BOOST;
			        	
			        	wvals[wavelength] = (int) ((wvals[wavelength] * ovals[wavelength] + intensity)/(ovals[wavelength] + 1));
			        	//HM - wvals[wavelength] = (int) (Math.sqrt((Math.pow(wvals[wavelength],2) * ovals[wavelength] + Math.pow(intensity, 2))/(ovals[wavelength]+1)));
			        	//GM - wvals[wavelength] = (int) (Math.pow((Math.pow(wvals[wavelength], ovals[wavelength]) * intensity), 1.0/(ovals[wavelength] + 1)));
			        	
			        	ovals[wavelength]++;
			        	
			        	output2.set(wavelength, RANGE + z, color);
			        	
			        	color = getColor(wavelength, 255);//intensity);
			        	spec.set(col, height/2 + 10, color);
			        	for (int i = 0; i < output1.height(); i++)
			        		output1.set(col, i, color);
		        	}
	        	}
	        	spec.set(col, height/2 - 10, getColor(getWavelength(l), 255));
        	}
        }
        
        return wvals;
    }
    
    public static ArrayList<Integer>[] getBasicAnalysis(Picture spec, Picture output1, Picture output2) // Finds the peak and trough wavelengths
    {
    	int[] data = new int[STD_WIDTH];
    	data = getVisibleWVals(spec, output1, output2);
    	
    	ArrayList<Integer> peaks = new ArrayList<Integer>();
    	ArrayList<Integer> troughs = new ArrayList<Integer>();
    	
    	for (int i = 0; i < STD_WIDTH - 1; i++)
    	{
    		// We want to see when the derivative is closest to zero OR the derivative changes sign.
    		boolean peak = true;
    		boolean trough = true;
    		for (int j = -1*RADIUS; j <= RADIUS; j++)
    		{	
    			int k = Math.max(0, i + j);
    			k = Math.min(k, STD_WIDTH - 1);
    			
    			if (!(data[i] >= data[k]))
    				peak = false;
    			if (!(data[i] <= data[k]))
    				trough = false;
    			
    			if (!peak && !trough)
    				break;
    		}
    		
    		if (peak && !trough)
    			peaks.add(i);
    		if (trough && !peak)
    			troughs.add(i);
    	}
    	
    	@SuppressWarnings("unchecked")
		ArrayList<Integer>[] result = new ArrayList[2];
    	result[0] = peaks;
    	result[1] = troughs;
    	return result;
    }
    
    public static void getCompleteAnalysis(Picture spec) // Finds the elemental composition of the spectrum
    {
    	
    }
    
    public static Picture plot(Picture spec, Picture graph, Color col, boolean fill, Picture output1, Picture output2, boolean plotExtrema)
    {
        int[] wvals = getVisibleWVals(spec, output1, output2);
        
        for (int i = 0; i < STD_WIDTH; i++)
        {
        	if (fill)
        	{
        		if (col == null)
        		{
        			Color color;
        			if (!(i < 380 || i > 740))
        				color = getColor(i, 255);
        			else
        				color = Color.gray;
        			
        			for (int j = 255; j >= 255-((int) wvals[i]*VISIBILITY_ASSISTANCE); j--)
        				graph.set(i, j, color);
        		}
        		else
        		{
        			for (int j = 255; j >= 255-((int) wvals[i]*VISIBILITY_ASSISTANCE); j--)
        				graph.set(i, j, col);
        		}
        	}
        	else
        	{
	        	if (col == null)
	        		graph.set(i, 255-(int) (wvals[i]*VISIBILITY_ASSISTANCE), getColor(i, 255.0));
	        	else
	        		graph.set(i, 255-(int) (wvals[i]*VISIBILITY_ASSISTANCE), col);
        	}
        }
        
        ArrayList<Integer>[] basicAnalysis = getBasicAnalysis(spec, output1, output2);
        for (int i = 0; i < basicAnalysis[0].size(); i++)
        {
        	for (int j = (int) (255-((int) wvals[basicAnalysis[0].get(i)]*VISIBILITY_ASSISTANCE/2)); j >= 255-((int) wvals[basicAnalysis[0].get(i)]*VISIBILITY_ASSISTANCE); j--)
				graph.set(basicAnalysis[0].get(i), j, Color.MAGENTA);
        }
        for (int i = 0; i < basicAnalysis[1].size(); i++)
        {
        	for (int j = 255; j >= 255-((int) wvals[basicAnalysis[1].get(i)]*VISIBILITY_ASSISTANCE); j--)
				graph.set(basicAnalysis[1].get(i), j, Color.WHITE);
        }
        
        return graph;
    }
    
    public static Picture lineSpec(Picture spec, Picture graph, Picture output1, Picture output2)
    {
    	int[] wvals = getVisibleWVals(spec, output1, output2);
    	
        for (int i = 0; i < STD_WIDTH; i++)
        {
        	if (wvals[i] != 0)
        	{
        		for (int j = 0; j < 256; j++)
        		{
        			Color color;
        			if (!(i < 380 || i > 740))
        				color = getColor(i, wvals[i]*VISIBILITY_ASSISTANCE);
        			else
        				color = new Color(wvals[i], wvals[i], wvals[i]);
        			graph.set(i, j, color);
        		}
        
        	}
        }	
        
        return graph;
    }
    
    public static Picture addLines(Picture graph, int intervalX, int intervalY)
    {
    	for (int i = 0; i < graph.width(); i+=intervalX)
    	{
    		for (int j = 0; j < graph.height(); j+=intervalY)
    			graph.set(i, j, Color.gray);
    	}
    	return graph;
    }
    
    public static Picture combineData(Picture[] pictures)
    {
    	int width = STD_WIDTH;
    	int height = 0;
    	
    	for (Picture picture : pictures)
    	{
    		height += picture.height();
    	}
    	Picture result = new Picture(width, height+pictures.length-1);
    	
    	int currentrow = 0;
    	for (int i = 0; i < pictures.length; i++)
    	{
    		for (int row = 0; row < pictures[i].height(); row++)
    		{
    			for (int col = 0; col < STD_WIDTH; col++)
    			{
    				result.set(col, currentrow, pictures[i].get(col, row));
    			}
    			currentrow++;
    		}
    		
    		if (i != pictures.length - 1)
    		{
	    		for (int col = 0; col < STD_WIDTH; col++)
				{
					result.set(col, currentrow, Color.white);
				}
	    		currentrow++;
    		}
    	}
    	
    	return result;
    }
    
    public static Picture resize(int newX, int newY, Picture pic)
    {
    	double sW = newX/(double)pic.width();
        double sH = newY/(double)pic.height();
        Picture newPic = new Picture(newX, newY);
        
        for (int col = 0; col < newX; col++) 
        {
            for (int row = 0; row < newY; row++) 
            {
               newPic.set(col, row, pic.get((int)(col/sW), (int)(row/sH)));
            }
        }

        return newPic;
    }
    
    public static void main(String[] args) 
    {
        Picture picture = new Picture(args[0]); // TO MAXIMIZE ACCURACY, RESIZE THE DESIRED IMAGE SO THAT ON THE SCREEN THE SPECTRUM PART TAKES UP AT LEAST 12 CM
        Picture output1 = new Picture(picture.width(), 50);
        Picture output2 = new Picture(STD_WIDTH, 2*RANGE+1);
        
        Picture newPic = new Picture(STD_WIDTH, 256);
        Picture newPic2 = new Picture(STD_WIDTH, 256);
        
        for (int row = 0; row < 256; row++)
        	for (int col = 0; col < STD_WIDTH; col++)
        		newPic.set(col, row, Color.black);
        for (int row = 0; row < 256; row++)
        	for (int col = 0; col < STD_WIDTH; col++)
        		newPic2.set(col, row, Color.black);
        
        plot(picture, newPic, null, true, output1, output2, true);
        
        addLines(newPic, 20, 5);
        addLines(newPic, 100, 2);
        
        lineSpec(picture, newPic2, output1, output2);
        
        Picture[] data = new Picture[3];
        data[0] = newPic;
        data[1] = output2;
        data[2] = newPic2;
        Picture merged = combineData(data);
        
        merged = resize((int)(merged.width()*1.2), merged.height(), merged);
        merged.show();
        
        ArrayList<Integer>[] basicAnalysis = getBasicAnalysis(picture, output1, output2);
        System.out.println("Peaks: ");
        for (int i = 0; i < basicAnalysis[0].size(); i++)
        	System.out.print(basicAnalysis[0].get(i) + " ");
        System.out.println("\nTroughs: ");
        for (int i = 0; i < basicAnalysis[1].size(); i++)
        	System.out.print(basicAnalysis[1].get(i) + " ");
        
        for (int i = 0; i < picture.width(); i++)
        	picture.set(i, picture.height()/2, Color.black);
        
        addLines(output1, 20, 5);
        addLines(output1, 100, 2);
        if (UNSAVED)
        {
        	output1.save("imgs/Output/output.png");
        	UNSAVED = false;
        }
        
        picture = resize(picture.width()/5, picture.height()/5, picture);
        picture.show();
    }
	
}

/*Copyright 2000-2017, Robert Sedgewick and Kevin Wayne and now Aathreya Kadambi.*/
