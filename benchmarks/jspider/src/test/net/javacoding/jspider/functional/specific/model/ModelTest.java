package net.javacoding.jspider.functional.specific.model;

import junit.framework.TestCase;
import net.javacoding.jspider.JSpider;
import net.javacoding.jspider.api.model.Folder;
import net.javacoding.jspider.mockobjects.plugin.JUnitEventSink;
import net.javacoding.jspider.core.util.config.ConfigurationFactory;
import net.javacoding.jspider.core.util.config.JSpiderConfiguration;
import net.javacoding.jspider.core.util.URLUtil;
import net.javacoding.jspider.core.storage.Storage;
import net.javacoding.jspider.functional.TestingConstants;

import java.net.URL;

/**
 * $Id: ModelTest.java,v 1.1 2003/04/10 16:19:23 vanrogu Exp $
 * @todo elaborate to check better
 */
public class ModelTest extends TestCase {

    public static final String
      tree[][] = {
          {"testcases", "specific", "model"},
          {"testcases", "specific", "model", "test1"},
          {"testcases", "specific", "model", "test2"}
      };

    public static final int
      resourceCount[][] = {
          {0, 0, 1 },
          {0, 0, 1, 1},
          {0, 0, 1, 2}
      };

    protected JUnitEventSink sink;
    protected JSpiderConfiguration config;

    /**
     * Public constructor giving a name to the test.
     */
    public ModelTest ( ) {
        super ( "ParseTest ");
    }

    /**
     * JUnit's overridden setUp method
     * @throws java.lang.Exception in case something fails during setup
     */
    protected void setUp() throws Exception {
        System.err.println("setUp");
        config = ConfigurationFactory.getConfiguration(ConfigurationFactory.CONFIG_UNITTEST);
        sink = JUnitEventSink.getInstance();
    }

    /**
     * JUnit's overridden tearDown method
     * @throws java.lang.Exception in case something fails during tearDown
     */
    protected void tearDown() throws Exception {
        System.err.println("tearDown");
        ConfigurationFactory.cleanConfiguration();
        sink.reset();
    }

    /**
     * Test a simple parse.
     */
    public void testSimpleParse ( ) throws Exception {

        URL url = new URL ( "http://" + TestingConstants.HOST + "/testcases/specific/model/index.html" );

        JSpider jspider = new JSpider ( url );
        jspider.start ( );
        Storage storage = jspider.getContext().getStorage();
        testFolders ( storage );
    }


    public void testFolders ( Storage storage ) throws Exception {
        for (int i = 0; i < tree.length; i++) {
            String[] folders = tree[i];
            Folder[] rootFolders = storage.getSiteDAO().find(URLUtil.normalize(new URL("http", TestingConstants.HOST, ""))).getRootFolders();
            ensureFolders(i, rootFolders, folders, 0);

            //email
            //refs
            //resources
        }
    }

    public void ensureFolders ( int treeIndex, Folder[] currentLevel, String[] folderNames, int index ) {
        String name = folderNames[index];

        Folder foundFolder = null;
        for (int i = 0; i < currentLevel.length; i++) {
            Folder folder = currentLevel[i];
            if ( folder.getName().equals(name)){
                foundFolder = folder;
                assertEquals("folder " + name + " reported wrong number of resources", resourceCount[treeIndex][index], folder.getResources().length);
            }
        }
        assertNotNull("folder " + name + " not found", foundFolder);

        if ( (index+1) < folderNames.length ) {
            ensureFolders(treeIndex, foundFolder.getFolders(), folderNames, index+1);
        }
    }

}