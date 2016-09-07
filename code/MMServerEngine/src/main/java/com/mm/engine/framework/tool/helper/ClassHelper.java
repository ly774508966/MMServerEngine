package com.mm.engine.framework.tool.helper;

import com.mm.engine.framework.tool.AnnotationClassTemplate;
import com.mm.engine.framework.tool.ClassTemplate;
import com.mm.engine.framework.tool.SupperClassTemplate;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * 根据条件获取相关类
 *
 * @author huangyong
 * @since 1.0
 */
public class ClassHelper {

    /**
     * 获取基础包名
     */
    public static final String basePackage = "com.mm.engine";
    public static final String appPackage="com.summer";

    static {
        // 系统包
        // 用户定义的包
        // 要校验两个包是否重复
    }

    public static boolean containPacket(String packetName){
        if(packetName.startsWith(basePackage) || packetName.startsWith(appPackage)){
            return true;
        }
        return false;
    }

    /**
     * 获取基础包名中的所有类
     */
    public static  List<Class<?>> getClassList() {
        List<Class<?>> result=new ClassTemplate(basePackage) {
            @Override
            public boolean checkAddClass(Class<?> cls) {
                String className = cls.getName();
                String pkgName = className.substring(0, className.lastIndexOf("."));
                return pkgName.startsWith(packageName);
            }
        }.getClassList();
        result.addAll(new ClassTemplate(appPackage) {
            @Override
            public boolean checkAddClass(Class<?> cls) {
                String className = cls.getName();
                String pkgName = className.substring(0, className.lastIndexOf("."));
                return pkgName.startsWith(appPackage);
            }
        }.getClassList());
        return  result;
//        return new ClassTemplate(basePackage) {
//            @Override
//            public boolean checkAddClass(Class<?> cls) {
//                String className = cls.getName();
//                String pkgName = className.substring(0, className.lastIndexOf("."));
//                return pkgName.startsWith(packageName);
//            }
//        }.getClassList();
    }

    /**
     * 获取基础包名中指定父类或接口的相关类
     */
    public static List<Class<?>> getClassListBySuper(Class<?> superClass) {
        List<Class<?>> result=new SupperClassTemplate(basePackage, superClass) {
            @Override
            public boolean checkAddClass(Class<?> cls) {
                return superClass.isAssignableFrom(cls) && !superClass.equals(cls);
            }
        }.getClassList();
        result.addAll(new SupperClassTemplate(appPackage, superClass) {
            @Override
            public boolean checkAddClass(Class<?> cls) {
                return superClass.isAssignableFrom(cls) && !superClass.equals(cls);
            }
        }.getClassList());
        return result;
//        return new SupperClassTemplate(basePackage, superClass) {
//            @Override
//            public boolean checkAddClass(Class<?> cls) {
//                return superClass.isAssignableFrom(cls) && !superClass.equals(cls);
//            }
//        }.getClassList();
    }

    /**
     * 获取基础包名中指定注解的相关类
     */
    public static List<Class<?>> getClassListByAnnotation(Class<? extends Annotation> annotationClass) {
        List<Class<?>> result=new AnnotationClassTemplate(basePackage, annotationClass) {
            @Override
            public boolean checkAddClass(Class<?> cls) {
                return cls.isAnnotationPresent(annotationClass);
            }
        }.getClassList();
        result.addAll(new AnnotationClassTemplate(appPackage, annotationClass) {
            @Override
            public boolean checkAddClass(Class<?> cls) {
                return cls.isAnnotationPresent(annotationClass);
            }
        }.getClassList());
        return result;
//        return new AnnotationClassTemplate(basePackage, annotationClass) {
//            @Override
//            public boolean checkAddClass(Class<?> cls) {
//                return cls.isAnnotationPresent(annotationClass);
//            }
//        }.getClassList();
    }
}
