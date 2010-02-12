package chord.util;

import java.util.List;
import java.util.Set;

/**
 * Specification for indexing a set of objects by the order in which
 * the objects are added to the set.
 * <p>
 * The only mutating operation is {@link #add(Object)}, in particular,
 * objects cannot be removed.
 * <p>
 * Provides O(1) time operations for adding a given object, getting
 * the object at a given index, and optionally membership testing for
 * a given object.
 * 
 * @param	<T>	The type of objects in the set.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public interface IndexSet<T> extends List<T>, Set<T> {

}
