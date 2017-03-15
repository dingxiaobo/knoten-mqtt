package cloud.lpwa;

import cloud.lpwa.annotations.WsnController;
import cloud.lpwa.dispatcher.MqttClientAdapter;
import cloud.lpwa.annotations.WsnMapping;
import cloud.lpwa.bean.MqttTopicMapping;
import cloud.lpwa.dispatcher.MqttCallbackDispatcher;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.*;

/**
 * mqtt启动器
 * Created by hzdxb on 2017/2/26.
 */
@Component
public class MqttStarter implements ApplicationContextAware {

    private final MqttClientAdapter mqttClientAdapter;
    private final MqttCallbackDispatcher mqttCallbackDispatcher;
    private ApplicationContext applicationContext;
    private Map<String, Object> wsnControllers;
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${lpwa.cloud.mq.server.qos:1}")
    private Integer qos;

    @Value("${lpwa.cloud.mq.knoten.enable:true}")
    private Boolean enableKnoten;

    @Autowired
    public MqttStarter(MqttClientAdapter mqttClientAdapter, MqttCallbackDispatcher mqttCallbackDispatcher) {
        this.mqttCallbackDispatcher = mqttCallbackDispatcher;
        this.mqttClientAdapter = mqttClientAdapter;

    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        if (enableKnoten) {
            logger.info("Starting Knoten mqtt service broker:\"{}\"", mqttClientAdapter.getMqttClient().getServerURI());
            this.wsnControllers = applicationContext.getBeansWithAnnotation(WsnController.class);
            Collection<MqttTopicMapping> topics = topics();
            mqttCallbackDispatcher.handle(topics, mqttClientAdapter);
            subscribe(topics);
        } else {
            logger.info("Knoten Mqtt Dependency Detected, but not enable, add config 'lpwa.cloud.mq.knoten.enable=true' to application.properties if you want.");
        }
    }

    private IMqttClient getIMqttClient() {
        return mqttClientAdapter.getMqttClient();
    }

    private void subscribe(Collection<MqttTopicMapping> topics) {
        for (MqttTopicMapping mqttTopicMapping : topics) {
            try {
                getIMqttClient().subscribe(mqttTopicMapping.getTopic(), mqttTopicMapping.getQos());
                logger.info("Topic subscribed topic=\"{}\", qos=\"{}\"", mqttTopicMapping.getTopic(), mqttTopicMapping.getQos());
            } catch (MqttException e) {
                logger.error("IMqttClient subscribe error in topic=\"{}\", qos=\"{}\"", mqttTopicMapping.getTopic(), mqttTopicMapping.getQos());
                e.printStackTrace();
                System.exit(-1);
            }
        }
    }


    private Collection<MqttTopicMapping> topics() {

        List<MqttTopicMapping> mqttTopicMappings = new ArrayList<>();


        logger.info("Count WsnControllers:\"{}\"", wsnControllers.size());

        //遍历bean
        for (String key : wsnControllers.keySet()) {
            Object bean = wsnControllers.get(key);
            Class<?> beanClass = bean.getClass();
            //只处理 包含 @WsnController的 bean
            if (beanClass.isAnnotationPresent(WsnController.class)) {
                logger.info("wsnController name=\"{}\" class=\"{}\"", key, beanClass);
                //类上面的 @WsnMapping 注解
                String[] typeMappings = {""};
                int[] typeQos = {-1};

                if (beanClass.isAnnotationPresent(WsnMapping.class)) {
                    //规范mapping
                    typeMappings = beanClass.getAnnotation(WsnMapping.class).value();
                    typeQos = beanClass.getAnnotation(WsnMapping.class).qos();
                }
                //生成类路径映射
                Map<String, Integer> typeMappingMap = mappingsTrim(typeMappings, typeQos);

                //qos 需要与 mapping 一一对应
                if (typeQos.length > 1 && typeMappings.length != typeQos.length) {
                    logger.error("Sizeof qos should equals sizeof value in @WsnMapping in class \"{}\"", beanClass.getName());
                }


                //方法上面的 @WsnMapping 注解
                Method[] methods = beanClass.getMethods();
                for (Method m : methods) {
                    if (m.isAnnotationPresent(WsnMapping.class)) {
                        //规范mapping
                        String[] methodMappings = m.getAnnotation(WsnMapping.class).value();
                        int[] methodQos = m.getAnnotation(WsnMapping.class).qos();

                        //方法上的映射
                        Map<String, Integer> methodMappingMap = mappingsTrim(methodMappings, methodQos);


                        //遍历拼接mapping

                        for (String typeMapping : typeMappingMap.keySet()) {
                            for (String methodMapping : methodMappingMap.keySet()) {
                                //拼接
                                int finalQos = qos;
                                if (0 <= typeMappingMap.get(typeMapping) && 0 > methodMappingMap.get(methodMapping)) {
                                    finalQos = typeMappingMap.get(typeMapping);
                                } else if (0 <= methodMappingMap.get(methodMapping)) {
                                    finalQos = methodMappingMap.get(methodMapping);
                                }
                                mqttTopicMappings.add(new MqttTopicMapping(finalQos, mappingXGTrim(typeMapping + methodMapping), m, bean));
                            }
                        }
                    }
                }


            }
        }

        return mqttTopicMappings;
    }

    private Map<String, Integer> mappingsTrim(String[] mappings, int[] qos) {
        Map<String, Integer> map = new HashMap<>();

        //多个mapping对应一个qos
        if (mappings.length > 1 && qos.length == 1) {
            int val = qos[0];
            qos = new int[mappings.length];
            for (int i = 0; i < mappings.length; i++) {
                qos[i] = val;
            }
        }


        for (int i = 0; i < mappings.length; i++) {
            String mapping = mappings[i];
            int q = qos[i];
            map.put(mappingTrim(mapping), q);
        }

        return map;
    }


    private String mappingTrim(String mapping) {
        mapping = mapping.trim();

        if (mapping.indexOf(0) != '/') {
            mapping = "/" + mapping;
        }
        if (mapping.indexOf(mapping.length()) == '/') {
            mapping = mapping.substring(0, mapping.length() - 1);
        }

        return mappingXGTrim(mapping);
    }

    private String mappingXGTrim(String mapping) {
        if (mapping.contains("//")) {
            int start = mapping.indexOf("//");
            mapping = mapping.substring(0, start) + mapping.substring(start + 1, mapping.length());

            return mappingXGTrim(mapping);
        }
        return mapping;
    }

}
