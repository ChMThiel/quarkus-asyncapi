package io.quarkiverse.asyncapi.annotation.scanner;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import com.asyncapi.v2.model.AsyncAPI;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;

public class AsyncAPIResourceGenerator {

    @Record(RUNTIME_INIT)
    @BuildStep
    void scanAsyncAPIs(CombinedIndexBuildItem aIndex, ScannedAsyncApi aScannedAsyncApi) {
        AsyncAPI asyncAPI = new AsyncApiAnnotationScanner().scan(aIndex.getIndex());
        aScannedAsyncApi.setAsyncAPI(asyncAPI);
    }
}
