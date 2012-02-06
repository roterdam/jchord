package chord.analyses.typestate;

import chord.util.ArraySet;

/***
 * This is the Object that is used to represent the state of a heap object
 * @author machiry
 *
 */

public class TypeState {
	private static ArraySet<TypeState> validStates;
	
	
	private String name;
	
	private static TypeState errorState;
		
	static{
		validStates = new ArraySet<TypeState>();
		errorState = null;
	}
	
	private TypeState(String name)
	{
		this.name = name;
	}
	
	public static void insertState(String name,boolean isErrorState)
	{
		TypeState state = new TypeState(name);
		if(isErrorState && errorState==null)
		{
			errorState = state;
		}
		validStates.add(state);
	}
		
	public static TypeState getState(String name)
	{
		TypeState stateOb = new TypeState(name);
		if(validStates.contains(stateOb))
		{
			validStates.get(validStates.indexOf(stateOb));
		}
		return null;
	}
	
	public static boolean isErrorState(TypeState currentstate)
	{
		return currentstate != null && errorState==currentstate;
	}
	
	
	//Overriding equals and getHashCode 
	
	public int hashcode()
	{
		return this.name.hashCode();
	}
	
	public boolean equals(Object ob)
	{
		if(ob == this)
		{
			return true;
		}
		if(!(ob instanceof TypeState))
		{
			return false;
		}
		TypeState secondObject = (TypeState)ob;
		return secondObject.name.equals(this.name);
	}
}
