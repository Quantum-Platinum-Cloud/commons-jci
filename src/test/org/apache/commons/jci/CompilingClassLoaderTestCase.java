package org.apache.commons.jci;

import java.io.File;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public final class CompilingClassLoaderTestCase extends AbstractTestCase {

    private final static Log log = LogFactory.getLog(CompilingClassLoaderTestCase.class);
    
    private final Signal reload = new Signal();

    private CompilingClassLoader cl;
    private ReloadingListener listener;
    
    protected void setUp() throws Exception {
        super.setUp();
        
        listener = new ReloadingListener() {
            public void reload() {
                synchronized(reload) {
                    reload.triggered = true;
                    reload.notify();
                }
            }
        };

        cl = new CompilingClassLoader(this.getClass().getClassLoader(), directory);
        cl.addListener(listener);
        cl.start();
    }

    private void initialCompile() throws Exception {

        waitForSignal(reload);

        writeFile(new File(directory, "jci/Simple.java"),
                "package jci;\n"
                + "public class Simple { \n"
                + "  public String toString() { \n"
                + "    return \"Simple\"; \n"
                + "  } \n"
                + "} \n"
        );
        
        writeFile(new File(directory, "jci/Extended.java"),
                "package jci;\n"
                + "public class Extended extends Simple { \n"
                + "  public String toString() { \n"
                + "    return \"Extended:\" + super.toString(); \n"
                + "  } \n"
                + "} \n"
        );
        
        waitForSignal(reload);
    }
    
    
    public void testCreate() throws Exception {
        initialCompile();
        
        Object o;
        
        o = cl.loadClass("jci.Simple").newInstance();        
        assertTrue("Simple".equals(o.toString()));
        
        o = cl.loadClass("jci.Extended").newInstance();        
        assertTrue("Extended:Simple".equals(o.toString()));
    }

    public void testChange() throws Exception {        
        initialCompile();

        Object o;
        
        o = cl.loadClass("jci.Simple").newInstance();        
        assertTrue("Simple".equals(o.toString()));
        
        o = cl.loadClass("jci.Extended").newInstance();        
        assertTrue("Extended:Simple".equals(o.toString()));

        writeFile(new File(directory, "jci/Simple.java"),
                "package jci;\n"
                + "public class Simple { \n"
                + "  public String toString() { \n"
                + "    return \"SIMPLE\"; \n"
                + "  } \n"
                + "} \n"
        );

        waitForSignal(reload);
    
        o = cl.loadClass("jci.Simple").newInstance();        
        assertTrue("SIMPLE".equals(o.toString()));
        
        o = cl.loadClass("jci.Extended").newInstance();        
        assertTrue("Extended:SIMPLE".equals(o.toString()));
    }

    public void testDelete() throws Exception {
        initialCompile();

        Object o;
        
        o = cl.loadClass("jci.Simple").newInstance();        
        assertTrue("Simple".equals(o.toString()));
        
        o = cl.loadClass("jci.Extended").newInstance();        
        assertTrue("Extended:Simple".equals(o.toString()));
        
        assertTrue(new File(directory, "jci/Extended.java").delete());
        
        waitForSignal(reload);

        o = cl.loadClass("jci.Simple").newInstance();        
        assertTrue("Simple".equals(o.toString()));

        try {
            o = cl.loadClass("jci.Extended").newInstance();
            fail();
        } catch(final ClassNotFoundException e) {
            assertTrue("jci.Extended".equals(e.getMessage()));
        }
        
    }

    public void testDeleteDependency() throws Exception {        
        initialCompile();

        Object o;
        
        o = cl.loadClass("jci.Simple").newInstance();        
        assertTrue("Simple".equals(o.toString()));
        
        o = cl.loadClass("jci.Extended").newInstance();        
        assertTrue("Extended:Simple".equals(o.toString()));
        
        assertTrue(new File(directory, "jci/Simple.java").delete());
        
        waitForSignal(reload);

        try {
            o = cl.loadClass("jci.Extended").newInstance();
            fail();
        } catch(final NoClassDefFoundError e) {
            assertTrue("jci/Simple".equals(e.getMessage()));
        }
        
    }

    protected void tearDown() throws Exception {
        cl.stop();
        super.tearDown();
    }
    
}
