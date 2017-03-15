package cloud.lpwa.dispatcher;

import cloud.lpwa.bean.KnotenMessage;
import org.eclipse.paho.client.mqttv3.IMqttClient;

/**
 * Created by hzdxb on 2017/2/25.
 */
public interface MqttClientAdapter   {

    /**
     * @return mqtt客户端
     */
    IMqttClient getMqttClient();

    void send(KnotenMessage knotenMessage);

}
