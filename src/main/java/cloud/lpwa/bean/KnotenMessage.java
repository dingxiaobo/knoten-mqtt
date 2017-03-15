package cloud.lpwa.bean;

import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * 回复信息，如果WsnController返回类型是 KnotenMessage  那么就会发送一条信息
 * Created by hzdxb on 2017/2/27.
 */
public class KnotenMessage {

    private String topic;

    private MqttMessage mqttMessage;


    /**
     * 向发信息过来的主题返回一条消息
     *
     * @param content 回复的内容
     */
    public KnotenMessage(String content) {
        this.mqttMessage = new MqttMessage(content.getBytes());
    }

    /**
     * 向发信息过来的主题返回一条消息
     *
     * @param content 回复的内容
     */
    public KnotenMessage(byte[] content) {
        this.mqttMessage = new MqttMessage(content);
    }

    public KnotenMessage( String topic, String content) {
        this.topic = topic;
        this.mqttMessage = new MqttMessage(content.getBytes());
    }

    public KnotenMessage( String topic, byte[] content) {
        this.mqttMessage = new MqttMessage(content);
        this.topic = topic;
    }

    public KnotenMessage( String topic, int qos, String content) {
        this.topic = topic;
        this.mqttMessage.setQos(qos);
        this.mqttMessage = new MqttMessage(content.getBytes());
    }

    public KnotenMessage( String topic, int qos, byte[] content) {
        this.mqttMessage = new MqttMessage(content);
        this.mqttMessage.setQos(qos);
        this.topic = topic;
    }

    public KnotenMessage( String topic, MqttMessage mqttMessage) {
        this.topic = topic;
        this.mqttMessage = mqttMessage;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public MqttMessage getMqttMessage() {
        return mqttMessage;
    }

    public void setMqttMessage(MqttMessage mqttMessage) {
        this.mqttMessage = mqttMessage;
    }
}
