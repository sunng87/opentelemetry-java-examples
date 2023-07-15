package io.opentelemetry.example.metrics;

import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.SERVICE_NAME;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import java.time.Duration;

/**
 * Example of using a Long Gauge to measure execution time of method. The gauge callback will get
 * executed every collection interval. This is useful for expensive measurements that would be
 * wastefully to calculate each request.
 */
public final class GaugeExample {

  static OpenTelemetry initOpenTelemetry() {
    // Include required service.name resource attribute on all spans and metrics
    Resource resource =
        Resource.getDefault()
            .merge(Resource.builder().put(SERVICE_NAME, "OtlpExporterExample").build());

    OpenTelemetrySdk openTelemetrySdk =
        OpenTelemetrySdk.builder()
            .setMeterProvider(
                SdkMeterProvider.builder()
                    .setResource(resource)
                    .registerMetricReader(
                        PeriodicMetricReader.builder(
                                OtlpHttpMetricExporter.builder()
                                    .setEndpoint("http://127.0.0.1:4000/v1/otlp/v1/metrics")
                                    .build())
                            .setInterval(Duration.ofMillis(5000))
                            .build())
                    .build())
            .buildAndRegisterGlobal();

    Runtime.getRuntime().addShutdownHook(new Thread(openTelemetrySdk::close));

    return openTelemetrySdk;
  }

  public static void main(String[] args) throws Exception {
    OpenTelemetry openTelemetry = initOpenTelemetry();

    Attributes attrs = Attributes.builder().put("host", "testhost").build();

    Meter sampleMeter = openTelemetry.getMeter("io.opentelemetry.example.metrics");
    sampleMeter
        .gaugeBuilder("jvm.memory.free")
        .setDescription("Reports JVM memory usage.")
        .setUnit("byte")
        .buildWithCallback(result -> result.record(Runtime.getRuntime().freeMemory(), attrs));

    while (true) {
      Thread.sleep(2000);
    }
  }
}
