package chord.analyses.typestate;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class TypeStateParser {
	private static String methodTransitionsStart = "";
	private static String methodAssertionsStart = "";
	private static String delimiter = "-";
	public static Boolean parseStateSpec(String fileName)
	{
		Boolean isFileGood = false;
		try {
			BufferedReader reader = new BufferedReader(new FileReader(fileName));
			String currentLine = null;
			Boolean inMethodTransitions = false;
			Boolean inMethodAssertions = false;
			Boolean parsingError = true;
			while((currentLine = reader.readLine())!=null){
				currentLine = currentLine.trim();
				if(currentLine.startsWith(methodTransitionsStart))
				{
					inMethodTransitions = true;
					inMethodAssertions = false;
					continue;
				}
				if(currentLine.startsWith(methodAssertionsStart))
				{
					inMethodAssertions = true;
					inMethodTransitions = false;
					continue;
				}
				if(inMethodTransitions && !currentLine.isEmpty())
				{
					String splitStrings[] = currentLine.split(delimiter);
					if(splitStrings.length != 3)
					{
						parsingError = true;
						break;
					}
					
					splitStrings[0] = splitStrings[0].trim();
					splitStrings[1] = splitStrings[1].trim();
					splitStrings[2] = splitStrings[2].trim();
					
					if(splitStrings[0].length() <=0 || splitStrings[1].length()<=0 || splitStrings[2].length() <=0)
					{
						parsingError = true;
						break;
					}
					
					TypeState.insertState(splitStrings[1], false);
					TypeState.insertState(splitStrings[2], false);
					TypeStateSpec.addMethodTransition(splitStrings[0], TypeState.getState(splitStrings[1]), TypeState.getState(splitStrings[1]));
				}
				if(inMethodAssertions && !currentLine.isEmpty())
				{
					String splitStrings[] = currentLine.split(delimiter);
					if(splitStrings.length < 2)
					{
						parsingError = true;
						break;
					}
					for(int i=1;i<splitStrings.length;i++)
					{
						splitStrings[i] = splitStrings[i].trim();
						if(splitStrings[i].length() <= 0)
						{
							parsingError = true;
							break;
						}
						TypeState.insertState(splitStrings[i], false);
						TypeStateSpec.addMethodAssertion(splitStrings[0],TypeState.getState(splitStrings[i]));
					}
					if(parsingError)
					{
						break;
					}					
					
				}				
			}
			if((inMethodAssertions || inMethodTransitions) && !parsingError){
				isFileGood = true;
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return isFileGood;
	}
}
