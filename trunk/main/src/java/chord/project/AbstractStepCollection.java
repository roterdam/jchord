package chord.project;

import java.util.List;

import CnCHJ.api.ItemCollection;

// abstract since does not implement the run(Object ctrl) method
public abstract class AbstractStepCollection implements IStepCollection {
	protected String name;
    protected List<ItemCollection> consumedDataCollections;
    protected List<ItemCollection> producedDataCollections;
    protected List<ICtrlCollection> producedCtrlCollections;
    protected ICtrlCollection prescribingCollection;
	@Override
	public void setName(String name) {
		this.name = name;
	}
	@Override
	public String getName() {
		return name;
	}
	@Override
	public void setPrescribingCollection(ICtrlCollection c) {
		prescribingCollection = c;
	}
	@Override
	public ICtrlCollection getPrescribingCollection() {
		return prescribingCollection;
	}
	@Override
	public void setConsumedDataCollections(List<ItemCollection> c) {
		consumedDataCollections = c;
	}
	@Override
	public List<ItemCollection> getConsumedDataCollections() {
		return consumedDataCollections;
	}
	@Override
	public void setProducedDataCollections(List<ItemCollection> c) {
		producedDataCollections = c;
	}
	@Override
	public List<ItemCollection> getProducedDataCollections() {
		return producedDataCollections;
	}
	@Override
	public void setProducedCtrlCollections(List<ICtrlCollection> c) {
		producedCtrlCollections = c;
	}
	@Override
	public List<ICtrlCollection> getProducedCtrlCollections() {
		return producedCtrlCollections;
	}
}
