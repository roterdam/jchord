package net.javacoding.jspider.core.rule;


import net.javacoding.jspider.api.model.Site;
import net.javacoding.jspider.core.rule.impl.RuleSetImpl;
import net.javacoding.jspider.core.util.config.*;
import net.javacoding.jspider.core.logging.LogFactory;
import net.javacoding.jspider.core.logging.Log;
import net.javacoding.jspider.spi.Rule;

import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;


/**
 *
 * $Id: RuleFactory.java,v 1.12 2003/04/29 17:53:48 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class RuleFactory {

    protected static Ruleset generalSpiderRules;
    protected static Ruleset generalParserRules;

    public static synchronized Ruleset createGeneralSpiderRules() {
        if (generalSpiderRules == null) {
            PropertySet props = ConfigurationFactory.getConfiguration().getJSpiderConfiguration();
            PropertySet jspiderProps = new MappedPropertySet ( ConfigConstants.JSPIDER, props );
            generalSpiderRules = createRules(jspiderProps, "spider", null);
        }
        return generalSpiderRules;
    }

    public static synchronized Ruleset createGeneralParserRules() {
        if (generalParserRules == null) {
            PropertySet props = ConfigurationFactory.getConfiguration().getJSpiderConfiguration();
            PropertySet jspiderProps = new MappedPropertySet ( ConfigConstants.JSPIDER, props );
            generalParserRules = createRules(jspiderProps, "parser", null);
        }
        return generalParserRules;
    }

    public static Ruleset createSiteSpiderRules(Site site) {
        PropertySet props = ConfigurationFactory.getConfiguration().getSiteConfiguration(site);
        PropertySet siteProps = new MappedPropertySet ( ConfigConstants.SITE, props );
        return createRules(siteProps, "spider", generalSpiderRules);
    }

    public static Ruleset createSiteParserRules(Site site) {
        PropertySet props = ConfigurationFactory.getConfiguration().getSiteConfiguration(site);
        PropertySet siteProps = new MappedPropertySet ( ConfigConstants.SITE, props );
        return createRules(siteProps, "parser", generalParserRules);
    }

    protected static Ruleset createRules(PropertySet props, String purpose, Ruleset generalRules) {
        PropertySet rulesProps = new MappedPropertySet(ConfigConstants.RULES, props);

        int rulesCount = rulesProps.getInteger(purpose + "." + ConfigConstants.RULES_COUNT, 0);

        Log log = LogFactory.getLog(RuleFactory.class);

        List rules = new ArrayList();

        for (int i = 0; i < rulesCount; i++) {
            String prefix = purpose + "." + (i + 1);
            PropertySet ruleProps = new MappedPropertySet(prefix, rulesProps);
            Class ruleClass = ruleProps.getClass(ConfigConstants.RULE_CLASS, null);
            Rule rule = null;

            if (ruleClass != null) {

                try {
                    Class[] paramTypes = new Class[1];
                    paramTypes[0] = PropertySet.class;

                    Object params[] = new Object[1];
                    params[0] = new MappedPropertySet ( ConfigConstants.RULE_CONFIG, ruleProps);

                    Constructor constructor = ruleClass.getDeclaredConstructor(paramTypes);
                    rule = (Rule) constructor.newInstance(params);

                } catch (NoSuchMethodException e) {
                    log.debug("rule " + ruleClass.getName() + " hasn't got a config-param constructor");
                } catch (SecurityException e) {
                    log.info("SecurityException finding constructor");
                } catch (InstantiationException e) {
                    log.info("InstantiationException finding constructor");
                } catch (IllegalAccessException e) {
                    log.info("IllegalAccessException finding constructor");
                } catch (InvocationTargetException e) {
                    log.info("InvocationTargetException finding constructor");
                }

                if (rule == null) {
                    try {
                        rule = (Rule) ruleClass.newInstance();
                    } catch (InstantiationException e) {
                        log.error("InstantiationException on Rule", e);
                    } catch (IllegalAccessException e) {
                        log.error("IllegalAccessException on Rule instantiation", e);
                    }
                }
                if (rule == null) {
                    log.error("rule couldn't be instantiated");
                } else {
                    log.debug ( "added rule " + rule.getClass().getName() + " to " + purpose + " ruleset");
                    rules.add(rule);
                }
            } else {
                log.error("cannot find rule class '" + ruleProps.getString(ConfigConstants.RULE_CLASS, "<unknown>") + "' for " + purpose + " rules");
            }
        }
        if (generalRules == null) {
            return new RuleSetImpl(Ruleset.RULESET_GENERAL, rules);
        } else {
            return new RuleSetImpl(Ruleset.RULESET_SITE, generalRules, rules);
        }
    }
}
