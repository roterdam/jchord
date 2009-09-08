package net.javacoding.jspider.core.task.work;

import net.javacoding.jspider.api.event.resource.ResourceIgnoredForParsingEvent;
import net.javacoding.jspider.api.model.Decision;
import net.javacoding.jspider.core.SpiderContext;
import net.javacoding.jspider.core.util.URLUtil;
import net.javacoding.jspider.core.storage.Storage;
import net.javacoding.jspider.core.task.WorkerTask;

import java.net.URL;

public class DecideOnParsingTask extends BaseWorkerTaskImpl {

    protected URL url;

    public DecideOnParsingTask(SpiderContext context, URL url ) {
        super(context, WorkerTask.WORKERTASK_THINKERTASK);
        this.url = url;
    }

    public void prepare() {
    }

    public void execute() {
        Storage storage = context.getStorage();

        Decision parseDecision = context.getSiteParserRules(storage.getSiteDAO().find(URLUtil.getSiteURL(url))).applyRules(context, null, url);
        storage.getDecisionDAO().saveParseDecision(storage.getResourceDAO().getResource(url), parseDecision);

        if (parseDecision.getDecision() == Decision.RULE_ACCEPT || parseDecision.getDecision() == Decision.RULE_DONTCARE) {
            context.getAgent().scheduleForParsing(url);
        } else {
            storage.getResourceDAO().setIgnoredForParsing(url);
            context.getEventDispatcher().dispatch(new ResourceIgnoredForParsingEvent(storage.getResourceDAO().getResource(url)));
        }
    }

}
