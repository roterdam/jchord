package net.javacoding.jspider;

import net.javacoding.jspider.core.impl.CLI;
import net.javacoding.jspider.core.util.config.ConfigurationFactory;
import net.javacoding.jspider.tool.*;
import net.javacoding.jspider.tool.util.*;

import java.net.URL;

/**
 * $Id: JSpiderTool.java,v 1.4 2003/04/03 15:57:13 vanrogu Exp $
 */
public class JSpiderTool {

    public static Tool tool;
    protected Flags flags;
    protected Parameters params;
    protected String url;

    JSpiderTool(String url, Flags flags, Parameters params, Tool tool) {
        this.flags = flags;
        this.params = params;
        this.tool = tool;
        this.url = url;
    }

    void execute() throws Exception {
        ConfigurationFactory.getConfiguration(ConfigurationFactory.CONFIG_TOOL);
        JSpider jspider = new JSpider(new URL(url));
        jspider.start();
    }


    public static void main(String[] args) throws Exception {

        if (args.length < 2) {

            CLI.printSignature();
            System.out.println();
            System.out.println("Usage: jspider-tool [toolName] [URL] [params...]");

        } else {

            Flags flags = FlagsFactory.createFlags(args);
            Parameters params = ParametersFactory.createParameters(args);
            Tool tool = ToolFactory.createTool(args);
            String url = URLFactory.createURL(args);

            if (tool != null) {

                int expectedParams = tool.getParameterCount();
                int actualParams = params.getValues().length;

                if (expectedParams == actualParams) {
                    tool.setParameters(params);
                    JSpiderTool jspiderTool = new JSpiderTool(url, flags, params, tool);
                    jspiderTool.execute();
                } else {
                    System.out.println("Usage: jspider-tool " + tool.getName() + " [url] " + tool.getUsage());
                }
            } else {
                System.err.println("Tool with specified name not found");
            }
        }
    }

}
