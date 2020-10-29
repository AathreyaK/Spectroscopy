import java.awt.Color;

public class BetterSpectroscopy // THIS IS OUTDATED AND WONT WORK WELL ANYMORE
{
	private static double D_CAMERA_GRATING = 10; // mm
	private static double D_GRATING_REFINER = 169.5; //mm
	private static double D_GRATING_LINES = 2000; // nm
	private static double D_LENGTH_REFINER = 92.5; // mm // 73.5 is the small black one, 92.5 is the big white one
	private static Color INDICATOR = Color.white;
	
	private static int THRESHOLD_INTENSITY_MIN = 7;
	private static int THRESHOLD_INTENSITY_MAX = 245;
	
	private static int AMBIENT_INTENSITY_ADDITION = 53;
	private static int FALIURE_PENALTY = 82;
	
	private static double VISIBILITY_ASSISTANCE = 1.4;
	
	private static int STD_WIDTH = 1000;
	
	private static Picture Spectrum = new Picture("imgs/Data/Spectra.png");

    public static double intensity(Color color, boolean weighted) 
    {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        if (r == g && r == b) return r; 
        
        float[] hsbv = new float[3];
        Color.RGBtoHSB(r, g, b, hsbv);
        
        if (0.299*r + 0.587*g + 0.114*b - AMBIENT_INTENSITY_ADDITION > THRESHOLD_INTENSITY_MIN)
        	return 0.299*r + 0.587*g + 0.114*b; 
        
        return hsbv[1]*255 - FALIURE_PENALTY;
        
        // This was slightly modified in order to include more in the final output
    }
	
	public static int getWavelength(double l) // L is distance to source, l is distance from 
    {
    	double h = Math.sqrt(Math.pow(D_CAMERA_GRATING, 2) + Math.pow(l, 2));
    	return (int) (l/h * D_GRATING_LINES);
    }
	
	public static Color getColor(int wavelength, double intensity)
	{
		if (wavelength > 740 || wavelength < 380)
			return new Color((int)intensity, (int)intensity, (int)intensity);
		
		Color col = Spectrum.get(Math.min((int) ((wavelength - 380) / (740 - 380.0) * Spectrum.width()), Spectrum.width()-1), Spectrum.height()/2);
		return new Color(Math.min((int)(col.getRed() * (intensity/255)), 255), Math.min((int)(col.getGreen() * (intensity/255)), 255), Math.min((int)(col.getBlue() * (intensity/255)), 255));
	}
	
	private static int[] getVisibleWVals(Picture spec)
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
        
        int e = 0;
        for (int i = 0; i < x; i++)
        {
        	if (spec.get(i, height/2).equals(INDICATOR))
        	{
        		e = i;
        		break;
        	}
        }
        
        for (int col = e; col < width; col++)
        {
        	Color color = spec.get(col, height/2);
        	
        	if (intensity(color, false) - AMBIENT_INTENSITY_ADDITION > THRESHOLD_INTENSITY_MIN && intensity(color, false) - AMBIENT_INTENSITY_ADDITION < THRESHOLD_INTENSITY_MAX)
        	{
        		double l;
        		l = D_CAMERA_GRATING/D_GRATING_REFINER * (x - col) * D_LENGTH_REFINER / (x - e); // Guess screen 150 mm away 
	        	int wavelength = getWavelength(l);
	        	
	        	if (wavelength >= 300 && wavelength <= 701)
	        	{
		        	//int d = Math.abs(x - col)/x;
		        	double r = 1;
		        	double intensity = intensity(color, false) * r - AMBIENT_INTENSITY_ADDITION;
		        	
		        	//AM - wvals[wavelength] = (int) ((wvals[wavelength] * ovals[wavelength] + intensity)/(ovals[wavelength] + 1));
		        	wvals[wavelength] = (int) (Math.sqrt((Math.pow(wvals[wavelength],2) * ovals[wavelength] + Math.pow(intensity, 2))/(ovals[wavelength]+1)));
		        	//GM - wvals[wavelength] = (int) (Math.pow((Math.pow(wvals[wavelength], ovals[wavelength]) * intensity), 1.0/(ovals[wavelength] + 1)));
		        	
		        	ovals[wavelength]++;
		        	
		        	color = getColor(wavelength, 255);//intensity);
		        	spec.set(col, height/2 + 10, color);
	        	}
        	}
        }
        
        return wvals;
    }
    
    public static void getAnalysis(Picture spec) // Finds the elemental composition of the spectrum
    {
    	int[] data = new int[STD_WIDTH];
    }
    
    public static Picture plot(Picture spec, Picture graph, Color col, boolean fill)
    {
        int[] wvals = getVisibleWVals(spec);
        
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
        			
        			for (int j = 255; j >= 255-(int) wvals[i]; j--)
        				graph.set(i, j, color);
        		}
        		else
        		{
        			for (int j = 255; j >= 255-(int) wvals[i]; j--)
        				graph.set(i, j, col);
        		}
        	}
        	else
        	{
	        	if (col == null)
	        		graph.set(i, 255-(int) wvals[i], getColor(i, 255.0));
	        	else
	        		graph.set(i, 255-(int) wvals[i], col);
        	}
        }
        
        return graph;
    }
    
    public static Picture lineSpec(Picture spec, Picture graph)
    {
    	int[] wvals = getVisibleWVals(spec);
    	
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
    
    public static void main(String[] args) 
    {
        Picture picture = new Picture(args[0]);
        
        Picture newPic = new Picture(STD_WIDTH, 256);
        Picture newPic2 = new Picture(STD_WIDTH, 256);
        
        for (int row = 0; row < 256; row++)
        	for (int col = 0; col < STD_WIDTH; col++)
        		newPic.set(col, row, Color.black);
        for (int row = 0; row < 256; row++)
        	for (int col = 0; col < STD_WIDTH; col++)
        		newPic2.set(col, row, Color.black);
        
        plot(picture, newPic, null, true);
        
        addLines(newPic, 20, 5);
        addLines(newPic, 100, 2);
        
        lineSpec(picture, newPic2);
        
        Picture[] data = new Picture[2];
        data[0] = newPic;
        data[1] = newPic2;
        Picture merged = combineData(data);
        
        merged.show();
        
        for (int i = 0; i < picture.width(); i++)
        	picture.set(i, picture.height()/2, Color.black);
        
        int[] wvals = getVisibleWVals(picture);
        for (int i = 0; i < 1000; i++)
        	if (wvals[i] == 0)
        		System.out.print(i + " ");
        
        picture.show();
    }
	
}

/*Copyright 2000-2017, Robert Sedgewick and Kevin Wayne and now Aathreya Kadambi.*/
