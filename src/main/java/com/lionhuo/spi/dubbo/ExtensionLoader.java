/**
 * Copyright (c) 2012 Conversant Solutions. All rights reserved.
 * <p>
 * Created on 2019/1/21.
 */
package com.lionhuo.spi.dubbo;

import com.sun.deploy.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

public class ExtensionLoader<T> {

    private static final Pattern NAME_SEPARATOR = Pattern.compile("\\s*[,]+\\s*");

    private final ConcurrentMap<Class<?>, String> cachedNames = new ConcurrentHashMap<Class<?>, String>();

    private final Map<String, Object> cachedActivates = new ConcurrentHashMap<String, Object>();

    private final Holder<Map<String, Class<?>>> cachedClasses = new Holder<Map<String, Class<?>>>();

    private static final ConcurrentMap<Class<?>, Object> EXTENSION_INSTANCES = new ConcurrentHashMap<Class<?>, Object>();

    private final ConcurrentMap<String, Holder<Object>> cachedInstances = new ConcurrentHashMap<String, Holder<Object>>();

    private static final ConcurrentMap<Class<?>, ExtensionLoader<?>> EXTENSION_LOADERS = new ConcurrentHashMap<Class<?>, ExtensionLoader<?>>();

    private static final String DUBBO_DIRECTORY = "META-INF/dubbo/";
    
    private final Class<?> type;

    private ExtensionLoader(Class<?> type) {
        this.type = type;
    }

    public static <T> ExtensionLoader<T> getExtensionLoader(Class<T> type){
        ExtensionLoader<T> loader = (ExtensionLoader<T>) EXTENSION_LOADERS.get(type);
        if(loader == null){
            EXTENSION_LOADERS.putIfAbsent(type, new ExtensionLoader<>(type));
            loader = (ExtensionLoader<T>) EXTENSION_LOADERS.get(type);
        }

        return loader;
    }

    public T getExtension(String name) throws IllegalAccessException, IOException, InstantiationException {
        Holder<Object> holder = cachedInstances.get(name);
        if(holder == null){
            cachedInstances.putIfAbsent(name, new Holder<Object>());
            holder = cachedInstances.get(name);
        }

        Object instance = holder.get();
        if(instance == null){
            synchronized (holder){
                instance = holder.get();
                if(instance == null){
                    instance = createExtension(name);
                    holder.set(instance);
                }
            }
        }

        return (T) instance;
    }

    public T createExtension(String name) throws IOException, IllegalAccessException, InstantiationException {
        Class<?> clazz = getExtensionClasses().get(name);

        T instance = (T) EXTENSION_INSTANCES.get(clazz);
        if(instance == null){
            EXTENSION_INSTANCES.putIfAbsent(clazz, clazz.newInstance());
            instance = (T) EXTENSION_INSTANCES.get(clazz);
        }

        injectExtension(instance);

        return instance;
    }

    public Map<String, Class<?>> getExtensionClasses() throws IOException {
        Map<String, Class<?>> classes = cachedClasses.get();
        if(classes == null){
            synchronized (cachedClasses){
                classes = cachedClasses.get();
                if(classes == null){
                    classes = loadExtensionClasses();
                    cachedClasses.set(classes);
                }
            }
        }
        return classes;
    }

    public Map<String, Class<?>> loadExtensionClasses() throws IOException {
        final SPI defaultAnnotation = type.getAnnotation(SPI.class);
        if(defaultAnnotation != null){
            //do sth
        }

        Map<String, Class<?>> extensionClasses = new HashMap<String, Class<?>>();
        loadDirectory(extensionClasses, DUBBO_DIRECTORY, type.getName());
        return extensionClasses;
    }

    public void loadDirectory(Map<String, Class<?>> extensionClasses, String dir, String type) throws IOException {
        String filename = dir + type;
        Enumeration<URL> urls;
        ClassLoader classLoader = getClassLoader(ExtensionLoader.class);
        if(classLoader != null){
            urls = classLoader.getResources(filename);
        }else{
            urls = ClassLoader.getSystemResources(filename);
        }

        if(urls != null){
            while(urls.hasMoreElements()){
                URL resourceURL = urls.nextElement();
                loadResource(extensionClasses, classLoader, resourceURL);
            }
        }
    }

    public void loadResource(Map<String, Class<?>> extensionClasses,
                              ClassLoader classLoader, URL resourceURL) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(resourceURL.openStream(), "utf-8"));
        String line = null;
        while((line = reader.readLine()) != null){
            int ci = line.indexOf("#");
            if(ci > 0){
                line = line.substring(0, ci);
            }
            if(line.length() > 0){
                String name = null;
                int i = line.indexOf("=");
                if(i > 0){
                    name = line.substring(0, i).trim();
                    line = line.substring(i + 1).trim();
                }

                if(line.length() > 0){
                    try {
                        loadClass(extensionClasses, resourceURL, Class.forName(line, true, classLoader), name);
                    } catch (NoSuchMethodException|ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void loadClass(Map<String, Class<?>> extensionClasses, URL resourceURL, Class<?> clazz, String name) throws NoSuchMethodException {
        if(clazz.isAnnotationPresent(Annotation.class)){
            //if the class has annotation
        }else {
            clazz.getConstructor();//judge the constructor
            if(name == null || name.length() == 0){
                
            }
            
            String[] names = NAME_SEPARATOR.split(name);
            if(names != null && names.length > 0){
                Activate activate = clazz.getAnnotation(Activate.class);
                if(activate != null){
                    cachedActivates.put(names[0], activate);
                }

                for(String n : names){
                    if(!cachedNames.containsKey(clazz)){
                        cachedNames.putIfAbsent(clazz, n);
                    }

                    Class<?> c = extensionClasses.get(n);
                    if(c == null){
                        extensionClasses.put(n, clazz);
                    }else if(c != clazz){
                        throw new IllegalStateException("Duplicate extension");
                    }
                }
            }
        }
    }

    public ClassLoader getClassLoader(Class<?> clazz){
        ClassLoader cl = null;
        cl = Thread.currentThread().getContextClassLoader();

        if(cl == null){
            cl = clazz.getClassLoader();
            if(cl == null){
                cl = ClassLoader.getSystemClassLoader();
            }
        }

        return cl;
    }

    public T injectExtension(T instance){
        return instance;
    }
}
