package net.javacoding.jspider.mod.plugin.velocity;


import net.javacoding.jspider.api.event.JSpiderEvent;
import net.javacoding.jspider.api.event.engine.SpideringStoppedEvent;
import net.javacoding.jspider.core.logging.Log;
import net.javacoding.jspider.core.logging.LogFactory;
import net.javacoding.jspider.core.util.config.*;
import net.javacoding.jspider.spi.Plugin;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;

import java.io.*;
import java.util.*;


/**
 *
 * $Id: VelocityPlugin.java,v 1.7 2003/04/09 17:08:14 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class VelocityPlugin implements Plugin {

    public static final String MODULE_NAME = "Velocity Template JSpider module";
    public static final String MODULE_VERSION = "v1.0";
    public static final String MODULE_DESCRIPTION = "A JSpider module that renders output via velocity templates";
    public static final String MODULE_VENDOR = "http://www.javacoding.net";

    public static final String TRACE_FILENAME = "trace.filename";
    public static final String DUMP_FILENAME = "dump.filename";
    public static final String TRACE_WRITE = "trace.write";
    public static final String DUMP_WRITE = "dump.write";
    public static final String TEMPLATEFOLDER = "templatefolder";

    public static final String DEFAULT_TRACE_FILENAME = "velocity-trace.out";
    public static final String DEFAULT_DUMP_FILENAME = "velocity-dump.out";
    public static final boolean DEFAULT_TRACE_WRITE = true;
    public static final boolean DEFAULT_DUMP_WRITE = false;
    public static final String DEFAULT_TEMPLATEFOLDER = "velocity";

    protected String name;
    protected PropertySet config;
    protected Map templates;
    protected Writer traceWriter;
    protected Writer dumpWriter;
    protected VelocityEngine velocityEngine;

    protected boolean writeTrace;
    protected boolean writeDump;

    protected Log log;

    public VelocityPlugin(String name, PropertySet config) {
        this.name = name;
        this.config = config;
        log = LogFactory.getLog(VelocityPlugin.class);
    }

    public String getName() {
        return MODULE_NAME;
    }

    public String getVersion() {
        return MODULE_VERSION;
    }

    public String getDescription() {
        return MODULE_DESCRIPTION;
    }

    public String getVendor() {
        return MODULE_VENDOR;
    }

    public void initialize() {
        String traceFileName = config.getString(TRACE_FILENAME, DEFAULT_TRACE_FILENAME);
        String dumpFileName = config.getString(DUMP_FILENAME, DEFAULT_DUMP_FILENAME);
        writeTrace = config.getBoolean(TRACE_WRITE, DEFAULT_TRACE_WRITE);
        writeDump = config.getBoolean(DUMP_WRITE, DEFAULT_DUMP_WRITE);

        log.info("writing trace file: " + writeTrace);
        log.info("writing dump file: " + writeDump);

        String templateFolderName = config.getString(TEMPLATEFOLDER, DEFAULT_TEMPLATEFOLDER);
        log.info("Velocity template folder : " + templateFolderName);
        try {
            JSpiderConfiguration jspiderConfig = ConfigurationFactory.getConfiguration();
            File folder = jspiderConfig.getPluginConfigurationFolder(templateFolderName);
            velocityEngine = new VelocityEngine();
            Vector paths = new Vector();
            paths.add(folder.getAbsolutePath());
            velocityEngine.setProperty("file.resource.loader.path", paths);
            log.debug("file.resource.loader.path set to " + folder.getAbsolutePath());
            velocityEngine.init();
            log.debug("velocity.init() done");

            if (writeTrace) {
                traceWriter = new FileWriter(new File(jspiderConfig.getDefaultOutputFolder(), traceFileName));
                log.debug("opened trace file '" + traceFileName + "'");
                log.info("Writing to trace file: " + traceFileName);
            }

            if (writeDump) {
                dumpWriter = new FileWriter(new File(jspiderConfig.getDefaultOutputFolder(), dumpFileName));
                log.debug("opened dump file '" + dumpFileName + "'");
                log.info("Writing to dump file: " + dumpFileName);
            }

        } catch (IOException e) {
            log.error("i/o exception", e);
        } catch (Exception e) {
            log.error("exception", e);
        }
        templates = new HashMap();
    }

    public void shutdown() {
        templates = null;
        if (writeTrace) {
            log.debug("closing trace file...");
            try {
                traceWriter.flush();
                traceWriter.close();
            } catch (IOException e) {
                log.error("i/o exception closing trace file", e);
            }
        }
        if (writeDump) {
            log.debug("closing dump file...");
            try {
                dumpWriter.flush();
                dumpWriter.close();
            } catch (IOException e) {
                log.error("i/o exception closing dump file", e);
            }
        }
        log.debug("shutdown.");
    }

    public void notify(JSpiderEvent event) {
        if (event instanceof SpideringStoppedEvent) {
            if (writeDump) {
                try {
                    SpideringStoppedEvent ste = (SpideringStoppedEvent) event;
                    Template template = getTemplate("dump");
                    if (template == null) {
                        log.error("couldn't load 'dump' template");
                    } else {
                        log.info("writing dump - this could take a while");
                        Context ctx = new VelocityContext();
                        ctx.put("resources", ste.getResources());
                        ctx.put("sites", ste.getSites());
                        template.merge(ctx, dumpWriter);
                        log.debug("dump written");
                    }
                } catch (Exception e) {
                    log.error("exception while merging template", e);
                }
            }
        }

        if (writeTrace) {
            try {
                String eventName = event.getName();
                Template template = getTemplate(eventName);
                if (template == null) {
                    template = getTemplate("default");
                }
                Context ctx = new VelocityContext();
                ctx.put("eventName", eventName);
                ctx.put("event", event);
                template.merge(ctx, traceWriter);
            } catch (Exception e) {
                log.error("exception while merging template", e);
            }
        }
    }

    protected Template getTemplate(String eventName) {
        Template template = (Template) templates.get(eventName);
        if (template == null) {
            String templateName = "";
            try {
                templateName = eventName.replace('.', '/') + ".vm";
                log.debug("loading velocity template '" + templateName);
                template = velocityEngine.getTemplate(templateName);
                log.debug("loaded velocity template '" + templateName);
                templates.put(eventName, template);
            } catch (Exception e) {
                log.error("exception while loading template " + templateName, e);
            }
        }
        return template;
    }

}
