package com.netease.edu.eds.trace.support;

import com.netease.edu.eds.trace.core.Invoker;
import com.netease.edu.eds.trace.instrument.async.bootstrapclass.BootstrapInterceptorSupport;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.utility.JavaModule;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.logging.Level;

/**
 * @author hzfjd
 * @create 18/7/2
 **/
public class BootstrapDelegateListener extends AgentBuilder.Listener.Adapter {

    private static String                             CLASS_FILE_EXTENSION            = ".class";
    private static ConcurrentHashMap<String, Boolean> classInjectedMap                = new ConcurrentHashMap<>();

    private Set<String>                               classNamesToBeLoadedByBootstrap = new HashSet<>();
    private Set<Class>                                classesToBeLoadedByBootstrap    = new HashSet<>();

    public Set<String> getClassNamesToBeLoadedByBootstrap() {
        return classNamesToBeLoadedByBootstrap;
    }

    public Set<Class> getClassesToBeLoadedByBootstrap() {
        return classesToBeLoadedByBootstrap;
    }

    private static Class[] basicBootstrapIntrumentSupportClass = { BootstrapInterceptorSupport.class,
                                                                   BootstrapInterceptorSupport.OriginCall.class,
                                                                   Invoker.class };

    public static AgentBuilder.Listener newBootstrapAgentBuildLister(Class... classes) {
        BootstrapDelegateListener listener = new BootstrapDelegateListener();

        for (Class clazz : basicBootstrapIntrumentSupportClass) {
            listener.getClassesToBeLoadedByBootstrap().add(clazz);
            listener.getClassNamesToBeLoadedByBootstrap().add(clazz.getName());
        }

        for (Class clazz : classes) {
            listener.getClassesToBeLoadedByBootstrap().add(clazz);
            listener.getClassNamesToBeLoadedByBootstrap().add(clazz.getName());
        }
        return listener;
    }

    @Override
    public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module,
                                 boolean loaded, DynamicType dynamicType) {
        TraceInstrumentationHolder.getLog().info(String.format("type: %s loaded by %s will be transformed.",
                                                               typeDescription.getTypeName(), classLoader));

        String onTransformClassName = typeDescription.getTypeName();

        TraceInstrumentationHolder.getLog().info(String.format("types: %s related by onTransformClassName: %s will be inject to bootstrap classpath.",
                                                               getClassNamesToBeLoadedByBootstrap(),
                                                               onTransformClassName));

        for (Class injectingClazz : getClassesToBeLoadedByBootstrap()) {
            injectClassToBootstrap(injectingClazz);
        }

    }

    private void injectClassToBootstrap(Class injectingClazz) {
        String injectingClassName = injectingClazz.getName();
        TraceInstrumentationHolder.getLog().info(String.format("injecting class: %s ...", injectingClassName));
        Boolean classInjected = classInjectedMap.get(injectingClassName);
        if (classInjected != null && classInjected.booleanValue()) {
            return;
        }
        synchronized (classInjectedMap) {
            classInjected = classInjectedMap.get(injectingClassName);
            if (classInjected != null && classInjected.booleanValue()) {
                return;
            }
            JarFile jarFile = inject(injectingClazz);
            if (jarFile != null) {
                TraceInstrumentationHolder.getInstumentation().appendToBootstrapClassLoaderSearch(jarFile);
                classInjectedMap.putIfAbsent(injectingClassName, Boolean.TRUE);
                TraceInstrumentationHolder.getLog().info(String.format("injected class: %s .", injectingClassName));
            }
        }
    }

    public JarFile inject(Class clazz) {
        String folder = TraceInstrumentationHolder.getBootstrapLibPath();
        String randomString = UUID.randomUUID().toString().replaceAll("-", "");

        InputStream inputStream = null;
        JarOutputStream jarOutputStream = null;
        ByteArrayOutputStream outputStream = null;
        try {
            // copy class from classpath
            String path = clazz.getName().replace('.', '/').concat(CLASS_FILE_EXTENSION);
            inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
            File file = new File(folder + File.separator + "trace-bootstrap-" + clazz.getSimpleName() + "-"
                                 + randomString + ".jar");
            TraceInstrumentationHolder.getLog().info(String.format("injecting jar path will be: %s .",
                                                                   file.getAbsolutePath()));

            if (!file.createNewFile()) {
                throw new IllegalStateException("Cannot create file " + file);
            }
            jarOutputStream = new JarOutputStream(new FileOutputStream(file));
            outputStream = new ByteArrayOutputStream();
            IOUtils.copy(inputStream, outputStream);

            // write to jar
            jarOutputStream.putNextEntry(new JarEntry(path));
            jarOutputStream.write(outputStream.toByteArray());
            // must close before generate jar file.
            jarOutputStream.close();
            return new JarFile(file);

        } catch (Exception exception) {
            TraceInstrumentationHolder.getLog().log(Level.SEVERE,
                                                    String.format("class: %s cant inject to jar:", clazz.getName()),
                                                    exception);
            return null;
        } finally {
            IOUtils.closeQuietly(inputStream);
            IOUtils.closeQuietly(outputStream);
            IOUtils.closeQuietly(jarOutputStream);

        }

    }

    @Override
    public void onError(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded,
                        Throwable throwable) {

        TraceInstrumentationHolder.getLog().log(Level.SEVERE,
                                                String.format("type: %s loaded by %s can't be transformed cause by error:",
                                                              typeName, classLoader),
                                                throwable);

    }

}
