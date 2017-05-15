package cloud.lpwa.dispatcher;

import cloud.lpwa.bean.KnotenMessage;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class MqttClientAdapterImpl implements MqttClientAdapter {

    private final MqttCallback mqttCallback;
    @Value("${lpwa.cloud.mq.server.broker:tcp://121.40.140.223:1883}")
    private String broker;
    @Value("${lpwa.cloud.mq.server.client.id:KnotenJavaMqttClient}")
    private String clientId;
    @Value("${lpwa.cloud.mq.server.clean-session:true}")
    private Boolean cleanSession;
    private IMqttClient mqttClient;
    private Logger logger = LoggerFactory.getLogger(this.getClass());


    private ExecutorService executorService;

    @Autowired
    public MqttClientAdapterImpl(MqttCallback mqttCallback) {
        this.mqttCallback = mqttCallback;

        executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    public IMqttClient getMqttClient() {
        if (mqttClient == null || !mqttClient.isConnected()) {
            MqttClientPersistence persistence = new MemoryPersistence();
            try {
                this.clientId = this.clientId + (System.currentTimeMillis() % 1000000000L);
                mqttClient = new MqttClient(broker, clientId, persistence);

                MqttConnectOptions connOpts = new MqttConnectOptions();

                connOpts.setCleanSession(cleanSession);

                logger.info("Connecting to broker:\"{}\" , clientId:\"{}\"", broker, this.clientId);
                mqttClient.connect(connOpts);
                logger.info("Connect to broker successfully:\"{}\" , clientId:\"{}\"", broker, this.clientId);

                mqttClient.setCallback(mqttCallback);
                logger.info("MqttClient callback set");

            } catch (MqttException e) {
                logger.error("Connect to broker FAILED!! :\"{}\" ", e.getMessage());
                e.printStackTrace();
                System.exit(-1);
            }
        }
        return mqttClient;
    }

    @Override
    public void send(KnotenMessage knotenMessage) {
        executorService.execute(new Sender(getMqttClient(), knotenMessage));
    }


    private class Sender implements Runnable {

        private IMqttClient iMqttClient;

        private KnotenMessage knotenMessage;

        Sender(IMqttClient iMqttClient, KnotenMessage knotenMessage) {
            this.iMqttClient = iMqttClient;
            this.knotenMessage = knotenMessage;
        }

        public IMqttClient getiMqttClient() {
            return iMqttClient;
        }

        public void setiMqttClient(IMqttClient iMqttClient) {
            this.iMqttClient = iMqttClient;
        }

        public KnotenMessage getKnotenMessage() {
            return knotenMessage;
        }

        public void setKnotenMessage(KnotenMessage knotenMessage) {
            this.knotenMessage = knotenMessage;
        }

        @Override
        public void run() {
            try {
                logger.debug("Sending mqtt message topic:\"{}\", content:\"{}\"", knotenMessage.getTopic(), knotenMessage.getMqttMessage().toString());
                iMqttClient.publish(knotenMessage.getTopic(), knotenMessage.getMqttMessage());
            } catch (MqttException e) {
                e.printStackTrace();
                logger.error("Mqtt Message sent FAILED!! topic:\"{}\", message:\"{}\",  error:\"{}\"", knotenMessage.getTopic(), knotenMessage.getMqttMessage().toString(), e.getMessage());
            }
        }
    }


}
