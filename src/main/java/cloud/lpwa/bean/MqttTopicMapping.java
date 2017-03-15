package cloud.lpwa.bean;

import org.eclipse.paho.client.mqttv3.MqttTopic;

import java.lang.reflect.Method;

/**
 * 主题方法对应关系 @WsnController @WsnMapping
 * Created by hzdxb on 2017/2/26.
 */
public class MqttTopicMapping {

    private int qos;

    private String topic;

    private Method method;

    private Object instance;
//
//    public MqttTopicMapping(int qos, String topic, Method method) {
//        this.qos = qos;
//        this.topic = topic;
//        this.method = method;
//    }


    public MqttTopicMapping(int qos, String topic, Method method, Object instance) {
        this.qos = qos;
        this.topic = topic;
        this.method = method;
        this.instance = instance;
    }

    public boolean match(String recTopic) {
        if (this.topic.equals(recTopic)) {
            return true;
        }

        String[] thisTopics = this.topic.split(MqttTopic.TOPIC_LEVEL_SEPARATOR);
        String[] outTopics = recTopic.split(MqttTopic.TOPIC_LEVEL_SEPARATOR);
        if (!this.topic.contains("#")) {
            //不包含# 简单匹配

            // 长度不同 不匹配
            if (thisTopics.length != outTopics.length) {
                return false;
            }

            for (int i = 0; i < thisTopics.length; i++) {
                //如果不是通配 也不相等
                if (!thisTopics[i].equals(MqttTopic.SINGLE_LEVEL_WILDCARD) && !thisTopics[i].equals(outTopics[i])) {
                    return false;
                }
            }

        } else {
            // 有#的匹配

            if (thisTopics.length > outTopics.length) {
                return false;
            }

            for (int i = 0; i < thisTopics.length; i++) {
                String thisTopic = thisTopics[i];
                String outTopic = outTopics[i];


                //如果不是通配 也不相等
                if (!thisTopic.equals(outTopic) && !thisTopic.equals(MqttTopic.SINGLE_LEVEL_WILDCARD) && !MqttTopic.MULTI_LEVEL_WILDCARD.equals(thisTopic)) {
                    return false;
                }
            }

        }

        return true;

    }

    public Object getInstance() {
        return instance;
    }

    public void setInstance(Object instance) {
        this.instance = instance;
    }

    public int getQos() {
        return qos;
    }

    public void setQos(int qos) {
        this.qos = qos;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }
}
