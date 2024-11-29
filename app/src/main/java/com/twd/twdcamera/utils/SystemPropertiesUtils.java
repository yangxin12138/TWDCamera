package com.twd.twdcamera.utils;

import java.lang.reflect.Method;

public class SystemPropertiesUtils {

    private static final String CLASS_NAME = "android.os.SystemProperties";
    public static String getProperty(String key,String defaultValue){
        String value = defaultValue;
        try {
            Class<?> c = Class.forName(CLASS_NAME);
            Method get = c.getMethod("get",String.class,String.class);
            value = (String) (get.invoke(c,key,defaultValue));
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            return value;
        }
    }

    public static void setProperty(String key,String value){
        try{
            Class<?> c = Class.forName(CLASS_NAME);
            Method set = c.getMethod("set",String.class,String.class);
            set.invoke(c,key,value);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
