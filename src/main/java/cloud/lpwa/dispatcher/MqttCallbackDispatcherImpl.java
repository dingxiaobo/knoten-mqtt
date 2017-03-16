package cloud.lpwa.dispatcher;

import cloud.lpwa.annotations.*;
import cloud.lpwa.bean.KnotenMessage;
import cloud.lpwa.bean.MqttTopicMapping;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * 回调事件实现类
 * Created by hzdxb on 2017/2/25.
 */
@Component
public class MqttCallbackDispatcherImpl implements MqttCallbackDispatcher {
    private Logger logger = LoggerFactory.getLogger(this.getClass());


    private List<MqttTopicMapping> mqttTopicMappings;

    private MqttClientAdapter mqttClientAdapter;

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        try {
            logger.debug("deliveryComplete :\"{}\"", token.getMessage());
        } catch (MqttException e) {
            logger.error("deliveryComplete FAILED!! :\"{}\"", e.getMessage());
            e.printStackTrace();
        }
    }


    @Override
    public void handle(Collection<MqttTopicMapping> mqttTopicMappings, MqttClientAdapter mqttClientAdapter) {

        this.mqttClientAdapter = mqttClientAdapter;

        List<MqttTopicMapping> l = new ArrayList<>(mqttTopicMappings);

        final Collator collator = Collator.getInstance(Locale.ENGLISH);
        //优先匹配规则排序
        l.sort((o1, o2) -> {
            String s1 = o1.getTopic();
            String s2 = o2.getTopic();

            //如果有多级通配符 调到后面
            if (s1.contains(MqttTopic.MULTI_LEVEL_WILDCARD) || s2.contains(MqttTopic.MULTI_LEVEL_WILDCARD)) {
                if (s1.contains(MqttTopic.MULTI_LEVEL_WILDCARD) && s2.contains(MqttTopic.MULTI_LEVEL_WILDCARD)) {
                    int i = s2.split(MqttTopic.TOPIC_LEVEL_SEPARATOR).length - s1.split(MqttTopic.TOPIC_LEVEL_SEPARATOR).length;
                    return i == 0 ? collator.compare(s1, s2) : i;
                } else {
                    return s1.contains(MqttTopic.MULTI_LEVEL_WILDCARD) ? 1 : -1;
                }
            }

            //如果有单级通配符 调到后面
            if (s1.contains(MqttTopic.SINGLE_LEVEL_WILDCARD) || s2.contains(MqttTopic.SINGLE_LEVEL_WILDCARD)) {
                if (s1.contains(MqttTopic.SINGLE_LEVEL_WILDCARD) && s2.contains(MqttTopic.SINGLE_LEVEL_WILDCARD)) {
                    int i = s2.substring(0, s2.indexOf(MqttTopic.SINGLE_LEVEL_WILDCARD)).split(MqttTopic.TOPIC_LEVEL_SEPARATOR).length
                            - s1.substring(0, s1.indexOf(MqttTopic.SINGLE_LEVEL_WILDCARD)).split(MqttTopic.TOPIC_LEVEL_SEPARATOR).length;
                    return i == 0 ? collator.compare(s1, s2) : i;
                } else {
                    return s1.contains(MqttTopic.SINGLE_LEVEL_WILDCARD) ? 1 : -1;
                }
            }

            return collator.compare(s1, s2);
        });

        this.mqttTopicMappings = l;
    }

    @Override
    public void connectionLost(Throwable cause) {
        logger.warn("connectionLost, trying to reconnect. \"{}\" ", cause.getMessage());
        logger.warn("可能是ClientId重复，请检查lpwa.cloud.mq.server.client.id配置");
        cause.printStackTrace();
        mqttClientAdapter.getMqttClient();
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        logger.debug("messageArrived topic:\"{}\" message:\"{}\"", topic, message);

//查找有没有对应的topicMapping
        MqttTopicMapping mqttTopicMapping = findMethodByTopic(topic);
        if (mqttTopicMapping == null || mqttTopicMapping.getMethod() == null) {
            logger.warn("Message had been ignored. Unable to find the WsnController method of topic [\"{}\"]", topic);
        } else {

            Method method = mqttTopicMapping.getMethod();
            logger.info("Executing. topic:\"{}\", method:\"{}\", mapping:\"{}\"", topic, method.getName(), mqttTopicMapping.getTopic());

            Parameter[] parameters = method.getParameters();

            Object[] args = new Object[parameters.length];

            for (int i = 0; i < parameters.length; i++) {
                Parameter parameter = parameters[i];


                //参数赋值

                if (parameter.isAnnotationPresent(WsnBytes.class)) {
                    //wsnBytes
                    if (parameter.getType() != (byte[].class)) {
                        logger.error("@WsnBytes can be used in 'byte[]', error in method:\"{}\"", method.getName());
                    }
                    args[i] = message.getPayload();
                } else if (parameter.isAnnotationPresent(WsnString.class)) {
                    //wsnString
                    if (parameter.getType() != (String.class)) {
                        logger.error("@WsnString can be used in 'String', error in method:\"{}\"", method.getName());
                    }
                    args[i] = message.toString();
                } else if (parameter.isAnnotationPresent(WsnQos.class)) {
                    //wsnQos
                    if (parameter.getType() != (Integer.class) && parameter.getType() != (int.class)) {
                        logger.error("@WsnQos can be used in 'Integer', error in method:\"{}\"", method.getName());
                    }
                    args[i] = mqttTopicMapping.getQos();
                } else if (parameter.isAnnotationPresent(WsnTopic.class)) {
                    //wsnTopic
                    if (parameter.getType() != (String.class)) {
                        logger.error("@WsnTopic can be used in 'String', error in method:\"{}\"", method.getName());
                    }
                    args[i] = topic;
                } else if (parameter.isAnnotationPresent(WsnPathVariable.class)) {
                    //wsnPathVariable

                    String[] split = getWsnPathVariable(mqttTopicMapping.getTopic(), topic);

                    //1.顺序问题
                    //2.类型问题
                    WsnPathVariable wsnPathVariable = parameter.getAnnotation(WsnPathVariable.class);
                    int index = wsnPathVariable.value();
                    boolean multi = wsnPathVariable.multi();
                    if (multi) {
                        if (parameter.getType() != (String.class)) {
                            logger.error("@WsnPathVariable can be used in 'String' when multi==true, error in method:\"{}\"", method.getName());
                        }
                        args[i] = split[split.length];
                    } else {
                        if (parameter.getType() == (String.class)) {
                            args[i] = split[index];
                        } else if (parameter.getType() == (Integer.class)) {
                            args[i] = Integer.parseInt(split[index]);
                        } else if (parameter.getType() == (Boolean.class)) {
                            args[i] = Boolean.parseBoolean(split[index]);
                        } else if (parameter.getType() == (Long.class)) {
                            args[i] = Long.parseLong(split[index]);
                        } else if (parameter.getType() == (Short.class)) {
                            args[i] = Short.parseShort(split[index]);
                        } else if (parameter.getType() == (Double.class)) {
                            args[i] = Double.parseDouble(split[index]);
                        } else if (parameter.getType() == (Float.class)) {
                            args[i] = Float.parseFloat(split[index]);
                        } else {
                            logger.error("@WsnPathVariable can be used in 'String | Integer | Boolean | Long | Short | Double | Float', error in method:\"{}\"", method.getName());
                        }
                    }
                }


            }


            Object invoke = null;
            //  2017/2/26 invoke
            try {
                invoke = method.invoke(mqttTopicMapping.getInstance(), args);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }


            if (KnotenMessage.class == method.getReturnType() || Collection.class == method.getReturnType()) {

                if (KnotenMessage.class == method.getReturnType()) {

                    //需要回复消息
                    KnotenMessage knotenMessage = (KnotenMessage) invoke;

                    logger.info("Callback Msg:{}", knotenMessage.getMqttMessage().toString());

                    //默认主题就是发信息来的主题
                    if (StringUtils.isEmpty(knotenMessage.getTopic())) {
                        knotenMessage.setTopic(topic);
                    }

                    mqttClientAdapter.send(knotenMessage);
                } else if (Collection.class == method.getReturnType()) {
                    try {
                        Collection<KnotenMessage> messages = (Collection<KnotenMessage>) invoke;
                        for (KnotenMessage tk : messages) {
                            mqttClientAdapter.send(tk);
                        }
                    } catch (Exception e) {
                        logger.error("Collection ReturnType detected but Cast failed, make sure you have returned type:'Collection<KnotenMessage>'");
                        e.printStackTrace();
                    }
                } else {
                    logger.info("KnotenMessage == null detected, recommended return void if you don't need send messages to broker, in method:{}", method.getName());
                }
            }
        }
    }


    private String[] getWsnPathVariable(String patten, String topic) {
        int count = 0;
        for (int i = 0; i < patten.length(); i++) {
            if (MqttTopic.SINGLE_LEVEL_WILDCARD.equals(patten.substring(i, i + 1))
                    || MqttTopic.MULTI_LEVEL_WILDCARD.equals(patten.substring(i, i + 1))) {
                count++;
            }
        }

        String[] resultStrs = new String[count];
        count = 0;
        String[] pattenSplit = patten.split(MqttTopic.TOPIC_LEVEL_SEPARATOR);
        String[] topicSplit = topic.split(MqttTopic.TOPIC_LEVEL_SEPARATOR);


        for (int i = 0; i < pattenSplit.length; i++) {
            String s = pattenSplit[i];
            if (MqttTopic.SINGLE_LEVEL_WILDCARD.equals(s)) {
//                +
                resultStrs[count] = topicSplit[i];
                count++;
            } else if (MqttTopic.MULTI_LEVEL_WILDCARD.equals(s)) {
//                #
                String tmp = "";
                for (int j = i; j < pattenSplit.length; j++) {
                    tmp += pattenSplit[j];
                    if (j != pattenSplit.length - 1) {
                        tmp += MqttTopic.TOPIC_LEVEL_SEPARATOR;
                    }
                }
                resultStrs[resultStrs.length] = tmp;
            }
        }

        return resultStrs;
    }

    private MqttTopicMapping findMethodByTopic(String topic) {
        // mqtt 通配符 http://blog.csdn.net/waltonhuang/article/details/52066908
        for (MqttTopicMapping mqttTopicMapping : mqttTopicMappings) {
            if (mqttTopicMapping.match(topic)) {
                return mqttTopicMapping;
            }
        }
        return null;
    }


}
