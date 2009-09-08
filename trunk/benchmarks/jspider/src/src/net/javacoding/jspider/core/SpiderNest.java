package net.javacoding.jspider.core;


import net.javacoding.jspider.core.impl.SpiderImpl;
import net.javacoding.jspider.core.util.config.*;


/**
 *
 * $Id: SpiderNest.java,v 1.6 2003/04/02 20:54:59 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class SpiderNest {

    public static final int DEFAULT_SPIDER_THREADS = 1;
    public static final int DEFAULT_THINKER_THREADS = 1;

    public SpiderNest() {

    }

    public Spider breedSpider(SpiderContext context) {

        PropertySet props = ConfigurationFactory.getConfiguration().getJSpiderConfiguration();
        PropertySet threadProps = new MappedPropertySet ( ConfigConstants.CONFIG_THREADING, props);
        PropertySet spidersProps = new MappedPropertySet ( ConfigConstants.CONFIG_THREADING_SPIDERS, threadProps);
        PropertySet thinkerProps = new MappedPropertySet ( ConfigConstants.CONFIG_THREADING_THINKERS, threadProps);
        int spiderThreads = spidersProps.getInteger(ConfigConstants.CONFIG_THREADING_COUNT, DEFAULT_SPIDER_THREADS);
        int thinkerThreads = thinkerProps.getInteger(ConfigConstants.CONFIG_THREADING_COUNT, DEFAULT_THINKER_THREADS);

        return new SpiderImpl(context, spiderThreads, thinkerThreads);
    }

}
