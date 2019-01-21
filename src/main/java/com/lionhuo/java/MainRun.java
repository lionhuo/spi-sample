/**
 * Copyright (c) 2012 Conversant Solutions. All rights reserved.
 * <p>
 * Created on 2019/1/21.
 */
package com.lionhuo.java;

import java.util.ServiceLoader;

public class MainRun {
    public static void main(String[] args) {
        ServiceLoader<People> serviceLoader = ServiceLoader.load(People.class);
        System.out.println("Java SPI");
        serviceLoader.forEach(People::say);
    }
}
