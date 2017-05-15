# Knoten Mqtt
若版本号小于1.0.0，说明本程序目前尚未通过测试，只为cloud.lpwa项目使用，之后有时间再做1.0.0

## What`s this ?
- 这是一个依赖Spring boot的简化Mqtt开发的小工具
- 简单封装了org.eclipse.paho.client.mqttv3的mqtt消息收发
- 可以实现基于注解的Mqtt消息处理，大大简化了多topic时的消息处理操作。
- 具体使用请看Sample

## Get Start
###3 Steps to get start.
#### 1. jar
将jar放置如下位置，以0.0.1版本为例
```properties
src/main/resources/lib/knoten-mqtt-0.0.1.jar
```

#### 2.pom.xml
在Spring Boot项目中添加如下依赖(JDK1.8 Required)
```xml
<dependency>
	<groupId>lpwa.cloud</groupId>
	<artifactId>knoten-mqtt</artifactId>
	<scope>system</scope>
	<systemPath>${basedir}/src/main/resources/lib/knoten-mqtt-0.0.1.jar</systemPath>
</dependency>

<dependency>
	<groupId>org.eclipse.paho</groupId>
	<artifactId>org.eclipse.paho.client.mqttv3</artifactId>
	<version>1.0.2</version>
</dependency>
```

#### 3. application.properties
```properties
#是否开启mq服务 引入依赖后默认开启，需要关闭请加入第一条配置
#lpwa.cloud.mq.knoten.enable=false
#mqtt broker地址
lpwa.cloud.mq.server.broker=tcp://121.40.140.223:1883
#mqttClientId
lpwa.cloud.mq.server.client.id=clientId
lpwa.cloud.mq.server.clean-session=true
#lpwa.cloud.mq.server.qos=1
```



## Sample

```java
package lpwa.cloud.controller;

import lpwa.cloud.annotations.*;
import KnotenMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WsnController
@WsnMapping("sample")
public class SampleWsnController {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * 完整topic为 /sample/s1
     */
    @WsnMapping("s1")
    public void sample(
            /*mqtt消息 String value*/@WsnString String content,
            /*mqtt消息 bytes value*/@WsnBytes byte[] bytes
    ) {
        logger.info("content : '{}', bytes String value : '{}'", content, new String(bytes));
    }


    /**
     * 完整topic为 /sample/s2/+
     * 
     * 如果返回类型是 KnotenMessage，
     * 程序将在消息处理完之后发送一条消息，主题和内容是KnotenMessage的私有属性，
     * 如果topic为空，将会使用收到消息的主题 /sample/s2/+
     */
    @WsnMapping("s2/+")
    public KnotenMessage sampleReturn(
            /*匹配映射路径中的 + */@WsnPathVariable Integer id,
            /*收到消息的完整topic*/@WsnTopic String topic,
            /*收到消息的服务质量qos */@WsnQos Integer wsnQos,
            @WsnString String content
    ) {
        logger.info("id : '{}' , topic : '{}' , wsnQos : '{}' , content : {}", id, topic, wsnQos, content);
        return new KnotenMessage("/sample/return", "Message received '" + topic + "' , '" + content + "'");
    }

    /**
     * Collection<KnotenMessage> usage
     */
    @WsnMapping("/testlist")
    public Collection<KnotenMessage> testList() {
        List<KnotenMessage> knotenMessages = new ArrayList<>();
        knotenMessages.add(new KnotenMessage("/testlist1", "1"));
        knotenMessages.add(new KnotenMessage("/testlist1", "2"));
        knotenMessages.add(new KnotenMessage("/testlist1", "3"));
        knotenMessages.add(new KnotenMessage("/testlist1", "4"));

        return knotenMessages;
    }
}

```

## Release Notes

#### 0.0.8 `2017年5月15日07:50:57`
- 默认cleanSession为true
- 给clientId追加时间戳后9位，避免clientId重复造成冲突

