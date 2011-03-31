package conf_analyzer.stubs;

import java.util.*;

public class ModelMap<K,V> implements SortedMap<K, V>,Map.Entry<K, V> {

	K key;
	V value;
	@Override
	public void clear() {
		
	}

	@Override
	public boolean containsKey(Object key2) {
				return key.equals(key2);
	}

	@Override
	public boolean containsValue(Object value2) {
		return value.equals(value2);
	}

	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		ModelList s = new ModelList();
		s.add(this);
		return s;
	}

	@Override
	public V get(Object key) {
		return value;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public Set<K> keySet() {
		ModelList<K> s = new ModelList<K>();
		s.add(key);
		return s;
	}

	@Override
	public V put(K key, V value) {
		V oldV = this.value;
		this.key = key;
		this.value = value;
		return oldV;
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> t) {
		if(t instanceof ModelMap) {
			ModelMap<K,V> t2 = (ModelMap<K,V>) t;
			this.key = t2.key;
			this.value = t2.value;
		}
	}

	@Override
	public V remove(Object key) {
		return value;
	}

	@Override
	public int size() {
		return 0;
	}

	@Override
	public Collection<V> values() {
		ModelList<V> s = new ModelList<V>();
		s.add(value);
		return s;
	}

	@Override
	public K getKey() {
		return key;
	}

	@Override
	public V getValue() {
		return value;
	}

	@Override
	public V setValue(V value) {
		V oldV = this.value;
		this.value = value;
		return oldV;
	}

	@Override
	public Comparator<? super K> comparator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public K firstKey() {
		return key;
	}

	@Override
	public SortedMap<K, V> headMap(K toKey) {
		return this;
	}

	@Override
	public K lastKey() {
		return key;
	}

	@Override
	public SortedMap<K, V> subMap(K fromKey, K toKey) {
		return this;
	}

	@Override
	public SortedMap<K, V> tailMap(K fromKey) {
		return this;
	}

}
