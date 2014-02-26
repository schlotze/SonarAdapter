package eu.uqasar.sonar.adapter;

import eu.uqasar.adapter.SystemAdapter;
import eu.uqasar.adapter.exception.uQasarException;
import eu.uqasar.adapter.model.*;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONObject;
import org.json.JSONArray;

public class SonarAdapter implements SystemAdapter {
  private String host;
  private String port;
  private String queryTemplate;
  static final Map<uQasarMetric, String> UQMETRIC_TO_SONARMETRIC
    = new TreeMap<uQasarMetric, String>() {{
      put(uQasarMetric.NCLOC, "ncloc");
      put(uQasarMetric.STATEMENTS, "statements");
      put(uQasarMetric.DUPLICATED_LINES, "duplicated_lines");
      put(uQasarMetric.DUPLICATED_LINES_DENSITY, "duplicated_lines_density");
      put(uQasarMetric.COMPLEXITY, "complexity");
      put(uQasarMetric.UT_COVERAGE, "coverage");
      put(uQasarMetric.AT_COVERAGE, "it_coverage");
      put(uQasarMetric.TEST_SUCCESS_DENSITY, "test_success_density");
    }};
  
  private Requester requester = new DefaultRequester();
  
  public SonarAdapter(String host, String port) {
    this.host = host;
    this.port = port;
    queryTemplate = "http://" + host + ":" + port + "/api/resources?resource=";
  }

  public void injectRequester(Requester requester){
    this.requester = requester;
  }
  
  @Override
  public List<Measurement> query(String project, uQasarMetric metric) throws uQasarException {
    List<Measurement> measurements = new LinkedList<Measurement>();
    String query = queryTemplate + project + "&metrics=" + mapMetricName(metric);
    
    // Parse the resulting json. The structure of the response should be
    // static, so the following static approach should suffice
    JSONArray jsonArray = new JSONArray(querySonar(query));
    String result = null;
    if(jsonArray.length() != 0){
      JSONObject firstObj = jsonArray.getJSONObject(0);
      if (firstObj != null){
        result = JSONObject.valueToString(firstObj.getJSONArray("msr")
                                          .getJSONObject(0)
                                          .get("val"));
      }
    }

    measurements.add(new Measurement(metric, result));
    return measurements;
  }

  private String mapMetricName(uQasarMetric metric) throws uQasarException{
    String sonarMetricName = UQMETRIC_TO_SONARMETRIC.get(metric);
    if(sonarMetricName == null){
      throw new uQasarException(String.format("The UASAR metric %s is unknown in SonarQube", metric));
    }
    return sonarMetricName;
  }
  
  private String querySonar(String query) throws uQasarException{
    return requester.fetch(query);
  }

  public static void main(String[] argv){
    System.out.println("called!");
    
    String projName = "org.codehaus.sonar-plugins.cxx:cxx";
    
    SonarAdapter adapter = new SonarAdapter("localhost", "9000");
    try{
      List<Measurement> result = adapter.query(projName, uQasarMetric.NCLOC);
      System.out.println(result);
    } catch(uQasarException u){
      System.out.println("BOOM!: " + u);
    }
  }
}
