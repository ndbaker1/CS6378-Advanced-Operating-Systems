import java.io.FileWriter;
import java.io.File;

public class MetricLogger {
  public static void init(final Config config) {
    // Create metrics directory
    new File(config.project_path, "metrics").mkdir();
  }

  public synchronized static void record(final int node, final Config config, final String metricType, final double value) {
    try {
      final FileWriter outputWriter = new FileWriter(config.project_path + "/metrics/" + node + ".out", true);
      final String executionParameters = String.join(":",
        String.valueOf(config.nodes),
        String.valueOf(config.interRequestDelay),
        String.valueOf(config.csExecutionTime)
      );
      outputWriter.write(executionParameters + " " + metricType + " " + value + "\n");
      outputWriter.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
