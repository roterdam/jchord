package chord.analyses.typestate;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/***
 * This class holds th parsing logic of the TypeStateSpecification
 * 
 * @author machiry
 *Sample State Specification:
 *
 *MethodTransitions // This contains the possible transitions in the form methodname-initialstate-finalstate 
 * //for each method in each line
 * //each method can have multiple state transitions for each state; its a 1-n mapping
 * //Class name should be provided in the form of 'ClassName-init-expectedInitialState'  
 *Lock-init-UnLocked
 *lock-UnLocked-Locked
 *unLock-Locked-UnLocked
 *
 *Asserts //This contains asserts in the form methodname-possiblestate1-possiblestate2-possiblestate3 etc..
 * //for each method in separate line
 *lock-UnLocked
 *unLock-Locked
 *
 */

public class TypeStateParser {
	private static String methodTransitionsStart = "MethodTransitions";
	private static String methodAssertionsStart = "Asserts";
	private static String delimiter = "-";
	private static String commentPrefix = "//";
	private static String initState = "init";
	private static String errorStateName = "Error";
	
	
	/***
	 * This function parses the provided input file and parses its contents in to TypeStateSpec
	 * 
	 * @param fileName
	 * @return true if parsing is success full else false
	 */
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
				if(currentLine.startsWith(commentPrefix))
				{
					continue;
				}
				if(currentLine.toLowerCase().startsWith(methodTransitionsStart))
				{
					inMethodTransitions = true;
					inMethodAssertions = false;
					continue;
				}
				if(currentLine.toLowerCase().startsWith(methodAssertionsStart))
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
					TypeState.insertState(splitStrings[2]);
					
					if(splitStrings[1].equals(initState))
					{
						TypeStateSpec.addStartInfo(splitStrings[0], TypeState.getState(splitStrings[2]));
					}
					else
					{
						TypeState.insertState(splitStrings[1]);
						TypeStateSpec.addMethodTransition(splitStrings[0], TypeState.getState(splitStrings[1]), TypeState.getState(splitStrings[1]));
					}
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
						TypeState.insertState(splitStrings[i]);
						TypeStateSpec.addMethodAssertion(splitStrings[0],TypeState.getState(splitStrings[i]));
					}
					if(parsingError)
					{
						break;
					}					
					
				}				
			}
			if((inMethodAssertions || inMethodTransitions) && !parsingError && TypeStateSpec.getInitialState() != null){
				isFileGood = true;
				TypeState.insertState(errorStateName);
				TypeStateSpec.addErrorState(TypeState.getState(errorStateName));
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return isFileGood;
	}
}
