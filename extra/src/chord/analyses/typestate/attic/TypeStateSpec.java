package chord.analyses.typestate;
import java.util.HashMap;

import joeq.UTF.Utf8;

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
	private HashMap<Utf8, HashMap<TypeState, TypeState>> methodTransitions;
	private HashMap<Utf8, ArraySet<TypeState>> methodAssertions;
	private String typeOfObject;
	private TypeState initialState;
	private TypeState errorState;
	
	public TypeStateSpec(){
		methodTransitions = new HashMap<Utf8, HashMap<TypeState,TypeState>>();
		methodAssertions = new HashMap<Utf8, ArraySet<TypeState>>();
		typeOfObject = null;
		initialState = null;
		errorState = null;
	}
	
	public String getObjecttype(){
		return typeOfObject;
	}
	
	public TypeState getInitialState() {
		return initialState;
	}
	
	public TypeState getErrorState(){
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
	public TypeState getTargetState(String methodName,TypeState sourceState){
		Utf8 finalMethodName = Utf8.get(methodName);
		return getTargetState(finalMethodName,sourceState);
	}
	
	
	public TypeState getTargetState(Utf8 finalMethodName,TypeState sourceState){
		TypeState targetState = sourceState;
		if(!sourceState.equals(errorState) && isMethodOfInterest(finalMethodName)){
			if(methodAssertions.containsKey(finalMethodName))
			{
				if(!methodAssertions.get(finalMethodName).contains(sourceState))
				{
					return errorState;
				}
				
			}
			if(methodTransitions.containsKey(finalMethodName)){
				if(methodTransitions.get(finalMethodName).containsKey(sourceState))
				{
					targetState = methodTransitions.get(finalMethodName).get(sourceState);
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
	
	public HashMap<TypeState,TypeState> getMethodTransitions(String methodName){
		Utf8 finalMethodName = Utf8.get(methodName);
		return getMethodTransitions(finalMethodName);
	}
	
	public HashMap<TypeState,TypeState> getMethodTransitions(Utf8 finalMethodName){
		if(!methodTransitions.containsKey(finalMethodName)){
			return null;
		}
		return methodTransitions.get(finalMethodName);
	}
	
	/***
	 * This will give all assertions that need to be hold true for the provided method
	 * @param methodName
	 * @return will return the set of assertion states
	 */
	public ArraySet<TypeState> getMethodAssertions(String methodName){
		Utf8 finalMethodName = Utf8.get(methodName);
		return getMethodAssertions(finalMethodName);
	}
	
	public ArraySet<TypeState> getMethodAssertions(Utf8 finalMethodName){
		if(!methodAssertions.containsKey(finalMethodName)){
			return null;
		}
		return methodAssertions.get(finalMethodName);
	}
	
	public Boolean isMethodOfInterest(Utf8 methodName){
 		return methodAssertions.containsKey(methodName) || methodTransitions.containsKey(methodName);
	}
	
	/***
	 *  For a given method this will give whether it has any assertions or state transitions defined
	 *  
	 * @param methodName
	 * @return true if the method has atleast one transition or assertion defined else false
	 */
 	public Boolean isMethodOfInterest(String methodName){
 		return isMethodOfInterest(Utf8.get(methodName));
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
	public Boolean addMethodTransition(String methodName,
			TypeState sourceState, TypeState targetState) {
		Utf8 finalMethodName = Utf8.get(methodName);
		HashMap<TypeState, TypeState> targetStateMap = methodTransitions
				.get(finalMethodName);
		if (targetStateMap == null) {
			targetStateMap = new HashMap<TypeState, TypeState>();
			methodTransitions.put(finalMethodName, targetStateMap);
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
	public Boolean addMethodAssertion(String methodName,
			TypeState assertState) {
		Utf8 finalMethodName = Utf8.get(methodName);
		ArraySet<TypeState> targetSet = methodAssertions.get(finalMethodName);
		if (targetSet == null) {
			targetSet = new ArraySet<TypeState>();
			methodAssertions.put(finalMethodName, targetSet);
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
	public void addStartInfo(String typeName,TypeState startState){
		initialState = startState;
		typeOfObject = typeName;
	}
	
	/***
	 * This method adds the error state to the type specification
	 * 
	 * @param error
	 */
	public void addErrorState(TypeState error){
		errorState = error;
	}
}
