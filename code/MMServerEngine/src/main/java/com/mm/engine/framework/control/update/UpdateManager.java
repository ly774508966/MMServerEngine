package com.mm.engine.framework.control.update;

import com.mm.engine.framework.control.ServiceHelper;
import com.mm.engine.framework.control.annotation.Updatable;
import com.mm.engine.framework.server.Server;
import com.mm.engine.framework.tool.helper.BeanHelper;
import org.omg.CORBA.OBJ_ADAPTER;
import org.omg.CORBA.SystemException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Created by Administrator on 2015/11/23.
 *
 * 更新器，完成对所有更新服务的更新功能，如下：
 * 1对于同步更新器，每隔固定周期就会更新一次，更新周期由系统配置文件设置，如果更新内容耗时小于更新周期，将通过等待维持更新周期，如果更新内容耗时大于更新周期
 * 更新周期将被迫降低，出现这种情况，说明需要适当的减少更新内容耗时或增加更新周期
 * 2对于异步更新器，将提供线程池进行更新
 */
public class UpdateManager {
    private static List<UpdatableBean> asyncUpdatableList=new ArrayList<>();
    private static List<UpdatableBean> syncUpdatableList=new ArrayList<>();

    private static final int syncUpdateInterval;
    // 线程数量可以是处理器数量*2+1：Runtime.getRuntime().availableProcessors()
    // 线程池这里最好也重写，给线程命名标记
    private static ScheduledExecutorService asyncExecutor = new ScheduledThreadPoolExecutor(6, new RejectedExecutionHandler() {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            // 拒绝执行处理

        }
    }){
        protected void afterExecute(Runnable r, Throwable t) {
            // 执行后处理，注意异常的处理
        }
    };
    private static ScheduledExecutorService syncExecutor=Executors.newSingleThreadScheduledExecutor();

    static {
        Map<Class<?>,List<Method>> updatableClassMap= ServiceHelper.getUpdatableClassMap();
        for(Map.Entry<Class<?>,List<Method>> entry : updatableClassMap.entrySet()){
            Object service= BeanHelper.getServiceBean(entry.getKey());
            List<Method> methodList=entry.getValue();
            for(Method method : methodList){
                method.setAccessible(true);// 取消 Java 语言访问检查
                Updatable updatable=method.getAnnotation(Updatable.class);// 前面ServiceHelper已经进行了校验此处不用重复校验
                UpdatableBean updatableBean=new UpdatableBean(service,method,updatable.isAsynchronous(),updatable.cycle());
                if(updatable.isAsynchronous()){
                    asyncUpdatableList.add(updatableBean);
                }else {
                    syncUpdatableList.add(updatableBean);
                }
            }
        }
        // 从配置文件取出同步更新时间间隔
        syncUpdateInterval= Server.getEngineConfigure().getUpdateCycle();
    }

    public static void stop(){
        syncExecutor.shutdown();
        asyncExecutor.shutdown();
    }

    public static void start(){
        // 启动同步更新器
        for(UpdatableBean updatableBean : syncUpdatableList){
            updatableBean.lastUpdateTime=System.nanoTime();
        }
        for(UpdatableBean updatableBean : asyncUpdatableList){
            updatableBean.lastUpdateTime=System.nanoTime();
        }
        syncExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                //lastUpdateTime.
                for(UpdatableBean updatableBean : syncUpdatableList){
                    updatableBean.execute();
                }
            }
        },syncUpdateInterval,syncUpdateInterval,TimeUnit.MILLISECONDS);
        // 启动异步更新器
        for(final UpdatableBean updatableBean : asyncUpdatableList){
            asyncExecutor.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    updatableBean.execute();
                }
            },updatableBean.getInterval(),updatableBean.getInterval(),TimeUnit.MILLISECONDS);
        }
    }

    private static class UpdatableBean{
        private boolean isAsynchronous;
        private int interval;
        private long lastUpdateTime;

        private Object service;
        private Method method;

        private UpdatableBean(Object service,Method method,boolean isAsynchronous,int interval){
            this.service=service;
            this.method=method;
            this.isAsynchronous=isAsynchronous;
            this.interval=interval;
        }
        // 这里必须捕获异常，防止阻塞其它更新器
        private void execute(){
            try {
                long currentTime=System.nanoTime();
                method.invoke(service,(int)((currentTime-lastUpdateTime)/1000000));
                lastUpdateTime=currentTime;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }

        public boolean isAsynchronous() {
            return isAsynchronous;
        }

        public int getInterval() {
            return interval;
        }

        public Object getService() {
            return service;
        }

        public Method getMethod() {
            return method;
        }

        public long getLastUpdateTime() {
            return lastUpdateTime;
        }
    }
}