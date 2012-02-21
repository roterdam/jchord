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
		
	static{
		validStates = new ArraySet<TypeState>();
	}
	
	private TypeState(String name)
	{
		this.name = name;
	}
	
	public static void insertState(String name)
	{
		validStates.add(new TypeState(name));
	}
		
	public static TypeState getState(String name)
	{
		TypeState stateOb = new TypeState(name);
		if(validStates.contains(stateOb))
		{
			return validStates.get(validStates.indexOf(stateOb));
		}
		return null;
	}
	
	//Overriding equals and getHashCode 
	
	@Override
	public int hashCode()
	{
		return this.name.hashCode();
	}
	
	@Override
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
	
	@Override
	public String toString(){
		return name;
	}
}
