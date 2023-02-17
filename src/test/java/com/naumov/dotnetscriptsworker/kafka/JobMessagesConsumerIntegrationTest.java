package com.naumov.dotnetscriptsworker.kafka;

import com.naumov.dotnetscriptsworker.AbstractIntegrationTest;
import com.naumov.dotnetscriptsworker.config.KafkaPropertyMapWrapper;
import com.naumov.dotnetscriptsworker.dto.cons.JobConfig;
import com.naumov.dotnetscriptsworker.dto.cons.JobTaskMessage;
import com.naumov.dotnetscriptsworker.dto.prod.*;
import com.naumov.dotnetscriptsworker.model.JobTask;
import com.naumov.dotnetscriptsworker.service.ContainerService;
import com.naumov.dotnetscriptsworker.service.JobFilesService;
import com.naumov.dotnetscriptsworker.service.JobService;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.naumov.dotnetscriptsworker.TestFileUtil.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DirtiesContext
class JobMessagesConsumerIntegrationTest extends AbstractIntegrationTest {
    private static final long MESSAGE_WAITING_TIMEOUT_MS = 10000;
    @Value("${worker.kafka.jobs-topic-name}")
    private String jobsTopic;
    @Value("${worker.kafka.running-topic-name}")
    private String runningTopic;
    @Value("${worker.kafka.finished-topic-name}")
    private String finishedTopic;
    @Value("${worker.sandbox.job-timeout-sec}")
    private Integer jobTimeoutSec;
    @Value("${worker.sandbox.job-files-host-dir}")
    private Path tempDirPath;
    @Autowired
    @Qualifier("commonProducerProperties")
    private KafkaPropertyMapWrapper producerProps;
    @Autowired
    @Qualifier("commonConsumerProperties")
    private KafkaPropertyMapWrapper commonConsumerProps;
    @SpyBean
    private JobFilesService jobFilesService;
    @SpyBean
    private ContainerService containerServiceSpy;
    @SpyBean
    private JobMessagesConsumer jobMessagesConsumerSpy;
    @SpyBean
    private JobService jobServiceSpy;
    @MockBean
    private Reporter<JobTaskMessage> jobTaskReporterMock;

    // test producer
    private KafkaTemplate<String, JobTaskMessage> taskMessagesProducer;

    // test consumer containers
    private KafkaMessageListenerContainer<String, JobStartedMessage> startedListenerContainer;
    private KafkaMessageListenerContainer<String, JobFinishedMessage> finishedListenerContainer;

    // queues for consumed messages
    private BlockingQueue<ConsumerRecord<String, JobStartedMessage>> consumedStarted;
    private BlockingQueue<ConsumerRecord<String, JobFinishedMessage>> consumedFinished;

    @BeforeEach
    void setup() throws IOException {
        //setup test producer
        var taskProducerFactory = new DefaultKafkaProducerFactory<String, JobTaskMessage>(producerProps.toMap());
        taskMessagesProducer = new KafkaTemplate<>(taskProducerFactory);

        // setup test consumers
        consumedStarted = new LinkedBlockingQueue<>();
        consumedFinished = new LinkedBlockingQueue<>();
        startedListenerContainer = initContainer(JobStartedMessage.class, runningTopic, consumedStarted);
        finishedListenerContainer = initContainer(JobFinishedMessage.class, finishedTopic, consumedFinished);

        // initial files checkup
        assertTrue(isDirectoryExists(tempDirPath));
        clearDirectoryIfExists(tempDirPath);
    }

    private <T> KafkaMessageListenerContainer<String, T> initContainer(Class<T> messageClass,
                                                                       String topic,
                                                                       BlockingQueue<ConsumerRecord<String, T>> messageQueue) {
        Map<String, Object> consumerProps = commonConsumerProps.toMap();
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test");
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, messageClass.getName());
        DefaultKafkaConsumerFactory<String, T> consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps);

        var containerProperties = new ContainerProperties(topic);
        var container = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
        container.setupMessageListener((MessageListener<String, T>) messageQueue::add);
        container.start();
        ContainerTestUtils.waitForAssignment(container, 1);

        return container;
    }

    @AfterEach
    void teardown() throws IOException {
        startedListenerContainer.stop();
        finishedListenerContainer.stop();

        // files cleanup
        clearDirectoryIfExists(tempDirPath);
    }

    @Test
    void onJobTaskMessageHappyPath() throws Exception {
        // given
        assertTrue(containerServiceSpy.getAllContainersIds().isEmpty());
        assertTrue(isDirectoryEmpty(tempDirPath));

        UUID jobId = UUID.randomUUID();
        String nugetConfig = "<config />";
        String script = "script";

        JobTaskMessage jobTaskMessage = JobTaskMessage.builder()
                .jobId(jobId)
                .jobConfig(JobConfig.builder().nugetConfigXml(nugetConfig).build())
                .script(script)
                .build();

        // when
        taskMessagesProducer.send(jobsTopic, jobId.toString(), jobTaskMessage);

        // then
        ArgumentCaptor<JobTaskMessage> messageCaptor = ArgumentCaptor.forClass(JobTaskMessage.class);
        verify(jobMessagesConsumerSpy, timeout(MESSAGE_WAITING_TIMEOUT_MS).times(1))
                .onJobTaskMessage(messageCaptor.capture(), any());

        // --- message correct
        JobTaskMessage message = messageCaptor.getValue();
        assertNotNull(message);
        assertEquals(jobId, message.getJobId());
        assertEquals(script, jobTaskMessage.getScript());
        JobConfig jobConfig = jobTaskMessage.getJobConfig();
        assertNotNull(jobConfig);
        assertEquals(nugetConfig, jobConfig.getNugetConfigXml());

        // --- job processed successfully
        verify(jobTaskReporterMock, timeout(TimeUnit.SECONDS.toMillis(jobTimeoutSec)).times(1))
                .report(eq(jobTaskMessage));

        // --- jobs service was engaged during processing
        ArgumentCaptor<JobTask> jobTaskCaptor = ArgumentCaptor.forClass(JobTask.class);
        verify(jobServiceSpy, times(1)).runJob(jobTaskCaptor.capture());
        JobTask jobTask = jobTaskCaptor.getValue();
        assertNotNull(jobTask);
        assertEquals(jobId, jobTask.getJobId());
        assertEquals(script, jobTask.getJobScript());
        assertNotNull(jobTask.getMessageId());
        JobTask.JobConfig jobTaskJobConfig = jobTask.getJobConfig();
        assertNotNull(jobTaskJobConfig);
        assertEquals(nugetConfig, jobTaskJobConfig.getNugetConfigXml());

        // --- file service was engaged during processing
        verify(jobFilesService, times(1)).prepareJobFiles(refEq(jobTask));
        verify(jobFilesService, times(1)).cleanupJobFiles(eq(jobId));

        // --- container service was engaged during processing
        verify(containerServiceSpy, times(1)).createContainer(any(), any(), any(), any());
        verify(containerServiceSpy, times(1)).startContainer(any());
        verify(containerServiceSpy, times(1)).removeContainer(any(), anyBoolean());

        // --- started message sent and correct
        ConsumerRecord<String, JobStartedMessage> receivedStarted = consumedStarted.poll(MESSAGE_WAITING_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertNotNull(receivedStarted);
        assertEquals(jobId.toString(), receivedStarted.key());
        JobStartedMessage startedMessage = receivedStarted.value();
        assertNotNull(startedMessage);
        assertEquals(jobId, startedMessage.getJobId());

        // --- finished message sent and correct
        ConsumerRecord<String, JobFinishedMessage> receivedFinished = consumedFinished.poll(MESSAGE_WAITING_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertNotNull(receivedFinished);
        assertEquals(jobId.toString(), receivedFinished.key());
        JobFinishedMessage finishedMessage = receivedFinished.value();
        assertNotNull(finishedMessage);
        assertEquals(jobId, finishedMessage.getJobId());
        assertEquals(JobStatus.ACCEPTED, finishedMessage.getStatus());
        ScriptResults scriptResults = finishedMessage.getScriptResults();
        assertNotNull(scriptResults);
        assertEquals(JobCompletionStatus.SUCCEEDED, scriptResults.getFinishedWith());
        assertEquals("", scriptResults.getStdout());
        assertEquals("", scriptResults.getStderr());

        // --- container cleaned up
        assertTrue(containerServiceSpy.getAllContainersIds().isEmpty());

        // --- job files cleaned up
        assertTrue(isDirectoryEmpty(tempDirPath));
    }
}