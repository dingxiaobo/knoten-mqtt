package cloud.lpwa.dispatcher;

import cloud.lpwa.bean.MqttTopicMapping;
import org.eclipse.paho.client.mqttv3.MqttCallback;

import java.util.Collection;

/**
 * 回调事件分发器
 * Created by hzdxb on 2017/2/25.
 */
public interface MqttCallbackDispatcher extends MqttCallback {

    /**
     * handle 之后 就可以将请求地址和 方法关联起来
     *
     * @param mqttTopicMappings
     */
    void handle( Collection<MqttTopicMapping> mqttTopicMappings,  MqttClientAdapter mqttClientAdapter);

}
