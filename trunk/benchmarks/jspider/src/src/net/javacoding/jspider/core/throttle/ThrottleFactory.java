package net.javacoding.jspider.core.throttle;


import net.javacoding.jspider.api.model.Site;
import net.javacoding.jspider.core.logging.LogFactory;
import net.javacoding.jspider.core.logging.Log;
import net.javacoding.jspider.core.throttle.impl.DistributedLoadThrottleProvider;
import net.javacoding.jspider.core.util.config.*;


/**
 * Factory class that is responsible to create Throttle component instances
 * for use by the engine.
 *
 * <p>
 * Since we use a pluggable component setup, the throttle provider class and
 * the parameters used by the throttle component implementation are stored
 * in the jspider.properties file, under the conf/ folder, in the subfolder
 * of the used configuration.
 * </p>
 *
 * $Id: ThrottleFactory.java,v 1.9 2003/04/03 15:57:19 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class ThrottleFactory {


    /**
     * Creates a Throttle instance, based upon the settings in the config file.
     * @return Throttle instance
     */
    public Throttle createThrottle(Site site) {
        PropertySet props = ConfigurationFactory.getConfiguration().getSiteConfiguration(site);
        PropertySet throttleProps = new MappedPropertySet ( ConfigConstants.SITE_THROTTLE, props );
        Class providerClass = throttleProps.getClass(ConfigConstants.SITE_THROTTLE_PROVIDER, DistributedLoadThrottleProvider.class);
        Log log = LogFactory.getLog(ThrottleFactory.class);
        log.info("Throttle provider class is '" + providerClass + "'");

        try {
            ThrottleProvider provider = (ThrottleProvider) providerClass.newInstance();
            PropertySet throttleConfigProps = new MappedPropertySet ( ConfigConstants.SITE_THROTTLE_CONFIG, throttleProps );
            return provider.createThrottle(throttleConfigProps);
        } catch (InstantiationException e) {
            log.error("InstantiationException on Throttle", e);
            return null;
        } catch (IllegalAccessException e) {
            log.error("IllegalAccessException on Throttle", e);
            return null;
        }

    }
}
