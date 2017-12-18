package com.kerui.jobs;

import com.kerui.utils.FileSystemClassLoader;
import com.kerui.utils.PropertiesUtils;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

/**
 * 通过 配置文件动态加载每个job
 */
@Slf4j
@Component
public class JobManager {

    @Autowired
    private Scheduler scheduler;

    public void init() throws Exception {

        //加载 总的配置
        Properties contextJobs = PropertiesUtils.getProperties("context-job.properties");
        Enumeration<Object> elements = contextJobs.elements();

        URL url ;
        List<String> urlList = new ArrayList<String>();
        //每个job 的依赖
        List<URL> jarsList = new ArrayList<URL>();

        //job 类
        List<String> classNameList = new ArrayList<>();
        while (elements.hasMoreElements()) {
            //配置信息
            String element = (String) elements.nextElement();
            //得到该job 的 上级目录（相对）
             String parentDir = element.substring(0, element.lastIndexOf("/"));
            log.info("parentDir :{}", parentDir);
            //读取每个 job的 配置
            Properties jobProperties = PropertiesUtils.getProperties(element);
            //得到每个job 的类名
            String className = jobProperties.getProperty("className");

            //得到.class 文件所在的目录
            File classFile = new File(parentDir);
            String absolutePath = classFile.getAbsolutePath();
            log.info("class absolutePath :{}", absolutePath);
            urlList.add(absolutePath);

            File jarFile = new File(parentDir + "/" + "libs");
            File[] files = jarFile.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".jar");
                }
            });
            for (File jar : files) {
                String path = jar.getPath();
                URL jarUrl = new URL("file:" + path);
                jarsList.add(jarUrl);
            }
            classNameList.add(className);
        }

        //加载 libs
        URL[] jarURLs = jarsList.toArray(new URL[]{});
        URLClassLoader jarURLClassLoader = new URLClassLoader(jarURLs);
//        jarURLClassLoader.loadClass("com.kerui.Job2");
        jarURLClassLoader.close();





        //加载 job 类
        FileSystemClassLoader fileSystemClassLoader;
        for (int i = 0;i<urlList.size(); i++) {
            String absolutePath = urlList.get(i);
            String className = classNameList.get(i);
            fileSystemClassLoader  = new FileSystemClassLoader(absolutePath);
            Class<? extends Job> jobClass = (Class<? extends Job>) fileSystemClassLoader.loadClass(className);
            setJob(this.scheduler, jobClass, className,"*/5 * * * * ?");
        }
    }

    public void setJob(Scheduler scheduler,
                       Class<? extends Job> jobClazz,
                       String className,
                       String cronExpression) throws SchedulerException {
        JobDetail jobDetail = JobBuilder.newJob(jobClazz)
                .withIdentity(className, className)
                .build();
        CronScheduleBuilder scheduleBuilder = CronScheduleBuilder
                .cronSchedule(cronExpression);
        CronTrigger cronTrigger = TriggerBuilder
                .newTrigger().withIdentity(className, className)
                .withSchedule(scheduleBuilder).build();
        scheduler.scheduleJob(jobDetail,cronTrigger);
    }
}
