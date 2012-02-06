package chord.analyses.typestate;

import java.util.HashMap;

import com.sun.org.apache.xpath.internal.operations.Bool;

import chord.util.ArraySet;

/***
 * This is representation of the type state specification
 * each method:
 * 	Can have multiple transitions depending on the state of the object on which it is called from
 *  Can have multiple assertions on the state which the object should be for the method to operate properly 
 * @author machiry
 *
 */
public class TypeStateSpec {
	
	//Might need to change if we can have transition from one state to more than one state
	private static HashMap<String,HashMap<TypeState,TypeState>> methodTransitions;
	private static HashMap<String,ArraySet<TypeState>> methodAssertions;
	
	static
	{
		methodAssertions = new HashMap<String, ArraySet<TypeState>>();
		methodTransitions = new HashMap<String, HashMap<TypeState,TypeState>>();
	}
	public static Boolean addMethodTransition(String methodName,TypeState sourceState,TypeState targetState)
	{
		HashMap<TypeState,TypeState> targetStateMap = methodTransitions.get(methodName);
		if(targetStateMap == null)
		{
			targetStateMap = new HashMap<TypeState, TypeState>();
			methodTransitions.put(methodName, targetStateMap);
		}
		if(!targetStateMap.containsKey(sourceState))
		{
			targetStateMap.put(sourceState, targetState);
			return true;			
		}		
		return false;
	}
	
	public static Boolean addMethodAssertion(String methodName,TypeState assertState)
	{
		ArraySet<TypeState> targetSet = methodAssertions.get(methodName);
		if(targetSet == null)
		{
			targetSet = new ArraySet<TypeState>();
			methodAssertions.put(methodName, targetSet);
		}
		
		if(!targetSet.contains(assertState))
		{
			targetSet.add(assertState);
			return true;
		}
		return false;
	}
}
