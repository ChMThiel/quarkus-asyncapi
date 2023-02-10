package io.quarkiverse.asyncapi.annotation.scanner;

import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class AsyncApiAnnotationScannerTest {

    @Inject
    AsyncApiRecorder scannedAsyncApi;

    @Test
    void shouldScanEmitterAnnotations() throws Exception {
        //given
        assertThat(scannedAsyncApi).isNotNull();
        //when
        //        assertThat(optionalAsyncAPI).isPresent();
        //        AsyncAPI asyncAPI = optionalAsyncAPI.get();
        //        //then
        //        String openApiYaml = ObjectMapperFactory.yaml().writeValueAsString(asyncAPI);
        //        //TODO asserts
        //        System.out.println(openApiYaml);
    }

}
