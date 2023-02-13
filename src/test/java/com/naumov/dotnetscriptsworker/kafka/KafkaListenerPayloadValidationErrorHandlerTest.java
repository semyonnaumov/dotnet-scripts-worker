package com.naumov.dotnetscriptsworker.kafka;

import org.apache.kafka.clients.consumer.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;

import static org.mockito.Mockito.*;

class KafkaListenerPayloadValidationErrorHandlerTest {

    @Test
    void handleError() {
        KafkaListenerPayloadValidationErrorHandler handler = new KafkaListenerPayloadValidationErrorHandler();
        ListenerExecutionFailedException exceptionMock = mock(ListenerExecutionFailedException.class);
        when(exceptionMock.getCause()).thenReturn(mock(MethodArgumentNotValidException.class));
        Acknowledgment ackMock = mock(Acknowledgment.class);

        handler.handleError(mock(Message.class), exceptionMock, mock(Consumer.class), ackMock);

        verify(ackMock, times(1)).acknowledge();
    }
}