package chord.analyses.typestate;

import java.util.HashMap;

import com.sun.org.apache.xpath.internal.operations.Bool;

import chord.util.ArraySet;

/***
 * This is representation of the type state specification each method: Can have
 * multiple transitions depending on the state of the object on which it is
 * called from Can have multiple assertions on the state which the object should
 * be for the method to operate properly
 * 
 * @author machiry
 * 
 */
public class TypeStateSpec {

	// Might need to change if we can have transition from one state to more
	// than one state
	private static HashMap<String, HashMap<TypeState, TypeState>> methodTransitions;
	private static HashMap<String, ArraySet<TypeState>> methodAssertions;
	private static String typeOfObject;
	private static TypeState initialState;
	private static TypeState errorState;
	
	static {
		methodAssertions = new HashMap<String, ArraySet<TypeState>>();
		methodTransitions = new HashMap<String, HashMap<TypeState, TypeState>>();
		typeOfObject = null;
		initialState = null;

	}

	public static String getObjecttype(){
		return typeOfObject;
	}
	
	public static TypeState getInitialState() {
		return initialState;
	}
	
	public static TypeState getErrorState(){
		return errorState;
	}
	
	/***
	 * This function given the function name and the source state will give the target state
	 *  if the  source state satisfies Assertion (if there is some) if its doesn't satisfy this will return errorState
	 *  if the  source state has transition defined under this method the target state will be returned if there is no
	 *  defined transition then error state will be returned
	 *  
	 * @param methodName
	 * @param sourceState
	 * @return target TypeState
	 */
	public static TypeState getTargetState(String methodName,TypeState sourceState){
		TypeState targetState = sourceState;
		if(!sourceState.equals(errorState) && isMethodOfInterest(methodName)){
			if(methodAssertions.containsKey(methodName))
			{
				if(!methodAssertions.get(methodName).contains(sourceState))
				{
					return errorState;
				}
				
			}
			if(methodTransitions.containsKey(methodName)){
				if(methodTransitions.get(methodName).containsKey(sourceState))
				{
					targetState = methodTransitions.get(methodName).get(sourceState);
				}
				else
				{
					return errorState;
				}
			}
			
		}
		return targetState;		
	}
	
	/***
	 * This will give all the transitions valid for the provided method name
	 * @param methodName
	 * @return
	 */
	public static HashMap<TypeState,TypeState> getMethodTransitions(String methodName){
		if(!methodTransitions.containsKey(methodName)){
			return null;
		}
		return methodTransitions.get(methodName);
	}
	
	/***
	 * This will give all assertions that need to be hold true for the provided method
	 * @param methodName
	 * @return will return the set of assertion states
	 */
	public static ArraySet<TypeState> getMethodAssertions(String methodName){
		if(!methodAssertions.containsKey(methodName)){
			return null;
		}
		return methodAssertions.get(methodName);
	}
	
	/***
	 *  For a given method this will give whether it has any assertions or state transitions defined
	 *  
	 * @param methodName
	 * @return true if the method has atleast one transition or assertion defined else false
	 */
 	public static Boolean isMethodOfInterest(String methodName){
		return methodAssertions.containsKey(methodName) || methodTransitions.containsKey(methodName);
	}

	/***
	 * 
	 * This method adds a method transition along with the method name to the state specification
	 * 
	 * @param methodName
	 * @param sourceState
	 * @param targetState
	 * @return true if the transition is new else false
	 */
	public static Boolean addMethodTransition(String methodName,
			TypeState sourceState, TypeState targetState) {
		HashMap<TypeState, TypeState> targetStateMap = methodTransitions
				.get(methodName);
		if (targetStateMap == null) {
			targetStateMap = new HashMap<TypeState, TypeState>();
			methodTransitions.put(methodName, targetStateMap);
		}
		if (!targetStateMap.containsKey(sourceState)) {
			targetStateMap.put(sourceState, targetState);
			return true;
		}
		return false;
	}

	/***
	 * Adds the given assert state to the method assert state
	 * 
	 * @param methodName
	 * @param assertState
	 * @return
	 */
	public static Boolean addMethodAssertion(String methodName,
			TypeState assertState) {
		ArraySet<TypeState> targetSet = methodAssertions.get(methodName);
		if (targetSet == null) {
			targetSet = new ArraySet<TypeState>();
			methodAssertions.put(methodName, targetSet);
		}

		if (!targetSet.contains(assertState)) {
			targetSet.add(assertState);
			return true;
		}
		return false;
	}
	
	/***
	 * This method adds the Start info to the state specification
	 * @param typeName
	 * @param startState
	 */
	public static void addStartInfo(String typeName,TypeState startState){
		initialState = startState;
		typeOfObject = typeName;
	}
	
	/***
	 * This method adds the error state to the type specification
	 * 
	 * @param error
	 */
	public static void addErrorState(TypeState error){
		errorState = error;
	}
}
