/**
 * ENGG*4450 Phase 3 – Feature Enhancement (Issue #878)
 * Modification to JSONSerializer.java
 *
 * Adds support for the truecase.outputProbabilities property.
 * When enabled, the serializer includes the "truecaseProbs" object
 * in the output JSON for each token.
 */

import edu.stanford.nlp.ling.CoreLabel;
import org.json.JSONObject;
import java.util.Map;

public class JSONSerializer {

    private final boolean outputTruecaseProbs;

    /**
     * Constructor checks for property flag.
     * Default = false for backward compatibility.
     */
    public JSONSerializer(Properties props) {
        this.outputTruecaseProbs = Boolean.parseBoolean(
            props.getProperty("truecase.outputProbabilities", "false")
        );
    }

    /**
     * Serializes an individual token to JSON.
     * Adds the "truecaseProbs" object only if the flag is active.
     */
    public JSONObject serializeToken(CoreLabel token) {
        JSONObject json = new JSONObject();

        // Basic token fields (existing behavior)
        json.put("word", token.word());
        json.put("truecase", token.get(CoreAnnotations.TrueCaseAnnotation.class));
        json.put("truecaseConfidence", token.get(CoreAnnotations.TrueCaseConfidenceAnnotation.class));

        // --- New Feature Implementation ---
        if (outputTruecaseProbs) {
            Map<String, Double> probs = token.get(CoreAnnotations.TrueCaseProbsAnnotation.class);
            if (probs != null && !probs.isEmpty()) {
                JSONObject probsJson = new JSONObject();
                for (Map.Entry<String, Double> entry : probs.entrySet()) {
                    probsJson.put(entry.getKey(), roundTo3Decimals(entry.getValue()));
                }
                json.put("truecaseProbs", probsJson);
            }
        }

        return json;
    }

    /**
     * Utility method to round doubles to 3 decimal places.
     */
    private double roundTo3Decimals(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
