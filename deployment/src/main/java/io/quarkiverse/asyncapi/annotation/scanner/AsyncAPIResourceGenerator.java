package io.quarkiverse.asyncapi.annotation.scanner;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import java.util.List;
import java.util.function.Consumer;

import com.asyncapi.v2.model.AsyncAPI;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.vertx.http.deployment.FilterBuildItem;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.deployment.devmode.NotFoundPageDisplayableEndpointBuildItem;
import io.vertx.ext.web.Route;

public class AsyncAPIResourceGenerator {

    @Record(RUNTIME_INIT)
    @BuildStep
    void scanAsyncAPIs(
            CombinedIndexBuildItem aIndex,
            AsyncApiRecorder aScannedAsyncApi,
            AsyncApiRuntimeConfig aConfig) {
        AsyncAPI asyncAPI = new AsyncApiAnnotationScanner().scan(aIndex.getIndex(), aConfig);
        aScannedAsyncApi.setAsyncAPI(asyncAPI, aConfig);
    }

    //TODO config
    public static final String path = "/asyncapi";

    @BuildStep
    @Record(RUNTIME_INIT)
    void handler(LaunchModeBuildItem launch,
            BuildProducer<NotFoundPageDisplayableEndpointBuildItem> displayableEndpoints,
            BuildProducer<RouteBuildItem> routes,
            AsyncApiRecorder recorder,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            //            OpenApiRuntimeConfig openApiRuntimeConfig,
            ShutdownContextBuildItem shutdownContext,
            //            SmallRyeOpenApiConfig openApiConfig,
            List<FilterBuildItem> filterBuildItems) {
        AsyncApiHandler handler = new AsyncApiHandler();
        Consumer<Route> corsFilter = null;
        //        // Add CORS filter if the path is not attached to main root
        //        // as 'http-vertx' only adds CORS filter to http route path
        //        if (!nonApplicationRootPathBuildItem.isAttachedToMainRouter()) {
        //            for (FilterBuildItem filterBuildItem : filterBuildItems) {
        //                if (filterBuildItem.getPriority() == FilterBuildItem.CORS) {
        //                    corsFilter = recorder.corsFilter(filterBuildItem.toFilter());
        //                    break;
        //                }
        //            }
        //        }
        routes.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .routeFunction(path, corsFilter)
                .routeConfigKey("quarkus.asyncapi.path")
                .handler(handler)
                .displayOnNotFoundPage("Async API Schema document")
                .blockingRoute()
                .build());

        routes.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .routeFunction(path + ".json", corsFilter)
                .handler(handler)
                .build());
        //TODO

        routes.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .routeFunction(path + ".yaml", corsFilter)
                .handler(handler)
                .build());

        routes.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .routeFunction(path + ".yml", corsFilter)
                .handler(handler)
                .build());
    }
}
