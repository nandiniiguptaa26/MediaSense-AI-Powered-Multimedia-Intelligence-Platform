//package com.mediasense.converter.conversion.service.integration;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.mediasense.converter.conversion.config.GCSConfig;
//import com.mediasense.converter.conversion.service.ConversionJobListener;
//import com.mediasense.converter.conversion.service.MediaConversionService;
//import com.mediasense.converter.dto.JobPayload;
//import org.apache.kafka.clients.producer.ProducerRecord;
//import org.apache.kafka.common.serialization.StringSerializer;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.Mockito;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
//import org.springframework.kafka.core.DefaultKafkaProducerFactory;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.kafka.test.context.EmbeddedKafka;
//import org.springframework.kafka.test.EmbeddedKafkaBroker;
//import org.springframework.kafka.test.utils.ContainerTestUtils;
//import org.springframework.kafka.test.utils.KafkaTestUtils;
//import org.springframework.test.context.ActiveProfiles;
//
//import java.util.Map;
//
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.times;
//import static org.mockito.Mockito.verify;
//
//@SpringBootTest
//@EmbeddedKafka(partitions = 1, topics = { "conversion-jobs" })
//@ActiveProfiles("test")
//public class ConversionJobListenerIntegrationTest {
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @Autowired
//    private KafkaListenerEndpointRegistry registry;
//
//    @Autowired
//    private GCSConfig gcsConfig;
//
//    @Autowired
//    private ConversionJobListener listener;
//
//    @MockBean
//    private MediaConversionService mediaConversionService;
//
//    private KafkaTemplate<String, String> kafkaTemplate;
//
//    @BeforeEach
//    void setup(@Autowired EmbeddedKafkaBroker broker) {
//        Map<String, Object> producerProps = KafkaTestUtils.producerProps(broker);
//
//        DefaultKafkaProducerFactory<String, String> pf =
//                new DefaultKafkaProducerFactory<>(producerProps, new StringSerializer(), new StringSerializer());
//
//        kafkaTemplate = new KafkaTemplate<>(pf);
//
//        Mockito.reset(mediaConversionService);
//
//        // Wait for listener container to be assigned
//        registry.getListenerContainers().forEach(container ->
//                ContainerTestUtils.waitForAssignment(container, 1)
//        );
//    }
//
//    @Test
//    void testKafkaMessageTriggersConversion() throws Exception {
//        // Arrange
//        JobPayload job = new JobPayload();
//        job.setBlobName("test-video.mp4");
//        job.setOutputFormat("mp4");
//
//        String json = objectMapper.writeValueAsString(job);
//
//        // Act
//        kafkaTemplate.send(new ProducerRecord<>("conversion-jobs", json));
//
//        // Give listener time to consume
//        Thread.sleep(2000);
//
//        // Assert
//        verify(mediaConversionService, times(1)).convertMedia(
//                anyLong(),
//                eq(gcsConfig.getInputBucket()),
//                eq(gcsConfig.getOutputBucket()),
//                eq("test-video.mp4"),
//                eq("video/mp4"),
//                eq("mp4"),
//                isNull(),
//                isNull()
//        );
//    }
//}