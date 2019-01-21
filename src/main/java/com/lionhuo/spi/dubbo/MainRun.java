/**
 * Copyright (c) 2012 Conversant Solutions. All rights reserved.
 * <p>
 * Created on 2019/1/21.
 */
package com.lionhuo.spi.dubbo;

import com.lionhuo.spi.java.Man;
import com.lionhuo.spi.java.People;
import org.junit.Test;

import java.io.IOException;

public class MainRun {
    
    public static void main(String[] args) throws IllegalAccessException, IOException, InstantiationException {
        ExtensionLoader<People> extensionLoader = ExtensionLoader.getExtensionLoader(People.class);
        People man = extensionLoader.getExtension("man");
        man.say();
        People woman = extensionLoader.getExtension("woman");
        woman.say();
    }
//    @Test
//    public void runExtension() throws IllegalAccessException, IOException, InstantiationException {
//        ExtensionLoader<People> extensionLoader = ExtensionLoader.getExtensionLoader(People.class);
//        People man = extensionLoader.getExtension("man");
//        man.say();
//        People woman = extensionLoader.getExtension("woman");
//        woman.say();
//    }
}
