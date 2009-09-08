package net.javacoding.jspider.core.logging;

import net.javacoding.jspider.core.logging.impl.SystemOutLogProvider;
import net.javacoding.jspider.core.util.config.*;

import java.util.HashMap;
import java.util.Map;

/**
 * $Id: LogFactory.java,v 1.6 2003/04/02 20:55:07 vanrogu Exp $
 */
public class LogFactory {

    public static String[][] replacements = {
        {"net.javacoding.jspider.", ""}
    };

    public static Map loggers = new HashMap ( );
    public static LogFactory instance;

    protected LogProvider provider;


    protected LogFactory ( ) {
        PropertySet props = ConfigurationFactory.getConfiguration().getJSpiderConfiguration();
        PropertySet logProps = new MappedPropertySet ( ConfigConstants.CONFIG_LOG, props);
        Class providerClass = logProps.getClass(ConfigConstants.CONFIG_LOG_PROVIDER, SystemOutLogProvider.class);
        try {
            provider = (LogProvider) providerClass.newInstance();
        } catch (InstantiationException e) {
            // CANNOT HAPPEN - WE CHECKED BEFORE
        } catch (IllegalAccessException e) {
            // SHOULDN'T HAPPEN - NOT GONNA HANDLE IT
        }
    }

    protected static synchronized LogFactory getInstance ( ) {
        if ( instance == null ) {
            instance = new LogFactory ( );
        }
        return instance;
    }

    public static synchronized Log getLog ( Class category ) {
        return getLog ( category.getName() );
    }

    public static synchronized Log getLog ( String category ) {
        String effectiveCategory = translate ( category );
        Log log = (Log) loggers.get ( effectiveCategory );
        if ( log == null ) {
            log = getInstance().instantiate(effectiveCategory);
            loggers.put(effectiveCategory, log);
        }
        return log;
    }

    protected Log instantiate(String category) {
      return provider.createLog(category);
    }

    protected static String translate ( String category ) {
       String effectiveCategory = category;
        for (int i = 0; i < replacements.length; i++) {
            String[] replacement = replacements[i];
            if ( category.startsWith(replacement[0])) {
                effectiveCategory = replacement[1] + category.substring(replacement[0].length());
                break;
            }
        }
        return effectiveCategory;
    }

}
