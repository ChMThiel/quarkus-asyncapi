package io.quarkiverse.asyncapi.generator;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import com.asyncapi.v2.model.AsyncAPI;

import io.quarkiverse.asyncapi.annotation.scanner.ScannedAsyncApi;
import io.quarkiverse.asyncapi.config.AsyncAPISupplier;
import io.quarkiverse.asyncapi.config.JacksonAsyncAPISupplier;
import io.quarkiverse.asyncapi.generator.annotation.scanner.AsyncApiAnnotationScanner;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;

public class AsyncAPIResourceGenerator {

    @BuildStep
    void asyncAPIs(CombinedIndexBuildItem index, BuildProducer<AsyncAPIBuildItem> resourceProducer) {
        IndexView indexView = index.getIndex();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        for (ClassInfo supplier : indexView
                .getAllKnownSubclasses(DotName.createSimple(JacksonAsyncAPISupplier.class.getName()))) {
            try {
                resourceProducer
                        .produce(new AsyncAPIBuildItem(
                                (AsyncAPISupplier) cl.loadClass(supplier.name().toString()).getDeclaredConstructor()
                                        .newInstance()));
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }

    @Record(RUNTIME_INIT)
    @BuildStep
    void scanAsyncAPIs(CombinedIndexBuildItem aIndex, ScannedAsyncApi aScannedAsyncApi) {
        AsyncAPI asyncAPI = new AsyncApiAnnotationScanner().scan(aIndex.getIndex());
        aScannedAsyncApi.setAsyncAPI(asyncAPI);
    }
}
