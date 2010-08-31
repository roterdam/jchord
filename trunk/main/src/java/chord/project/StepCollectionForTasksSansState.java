package chord.project;

public class StepCollectionForTasksSansState extends AbstractStepCollection {
    protected ITask task;
	// must be called in step collection initialization stage
	public void setTask(ITask task) {
		this.task = task;
	}
	@Override
	public void run(Object ctrl) {
		task.run(ctrl, this);
	}
}

