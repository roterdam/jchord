package chord.project;

import CnCHJ.api.ItemCollection;

public class DefaultDataCollection implements IDataCollection {
	protected String name;
    protected ItemCollection ic;
	@Override
	public void setName(String name) {
		this.name = name;
	}
	@Override
	public String getName() {
		return name;
	}
	@Override
	public void setItemCollection(ItemCollection ic) {
		this.ic = ic;
    }
	@Override
	public ItemCollection getItemCollection() {
        return ic;
    }
}
