/**
 * ENGG*4450 Phase 4 – Build and Test Implementation
 * Feature: Truecase Probability Output (Issue #878)
 * Modified by Markus Royik-Turner
 *
 * This serializer now supports the "truecase.outputProbabilities" flag.
 * When enabled, each token in the CoreNLP pipeline includes a
 * "truecaseProbs" JSON object showing class probabilities
 * (e.g., LOWER, UPPER, TITLE, INITCAP) normalized to ≈ 1.0.
 *
 * Default behavior remains unchanged to ensure backward compatibility.
 */

package edu.stanford.nlp.pipeline;

import com.fasterxml.jackson.core.JsonGenerator;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

public class JSONSerializer {

    /** Flag for enabling probability output */
    private final boolean outputTruecaseProbs;

    /**
     * Constructor: reads the CoreNLP property "truecase.outputProbabilities".
     * Default is false for backward compatibility.
     */
    public JSONSerializer(Properties props) {
        this.outputTruecaseProbs = Boolean.parseBoolean(
                props.getProperty("truecase.outputProbabilities", "false")
        );
    }

    /**
     * Serializes an individual token to JSON using the Jackson generator.
     * Includes "truecaseProbs" only if the property flag is enabled.
     */
    public void jsonForToken(CoreLabel token, JsonGenerator generator) throws IOException {
        generator.writeStartObject();

        // --- Basic Token Fields ---
        generator.writeStringField("word", token.word());

        if (token.lemma() != null) {
            generator.writeStringField("lemma", token.lemma());
        }
        if (token.containsKey(CoreAnnotations.TrueCaseAnnotation.class)) {
            generator.writeStringField("truecase",
                    token.get(CoreAnnotations.TrueCaseAnnotation.class));
        }
        if (token.containsKey(CoreAnnotations.TrueCaseTextAnnotation.class)) {
            generator.writeStringField("truecaseText",
                    token.get(CoreAnnotations.TrueCaseTextAnnotation.class));
        }
        if (token.containsKey(CoreAnnotations.TrueCaseConfidenceAnnotation.class)) {
            generator.writeNumberField("truecaseConfidence",
                    token.get(CoreAnnotations.TrueCaseConfidenceAnnotation.class));
        }

        // --- Enhanced Feature: Probability Distribution ---
        if (outputTruecaseProbs &&
            token.containsKey(CoreAnnotations.TruecaseProbsAnnotation.class)) {

            Map<String, Double> probs =
                    token.get(CoreAnnotations.TruecaseProbsAnnotation.class);

            if (probs != null && !probs.isEmpty()) {
                generator.writeFieldName("truecaseProbs");
                generator.writeStartObject();

                // Write each class → probability pair (rounded to 3 decimals)
                for (Map.Entry<String, Double> entry : probs.entrySet()) {
                    generator.writeNumberField(entry.getKey(),
                            roundTo3Decimals(entry.getValue()));
                }

                generator.writeEndObject(); // end truecaseProbs
            }
        }

        generator.writeEndObject(); // end token
    }

    /**
     * Utility: round double values to 3 decimal places for cleaner JSON output.
     */
    private double roundTo3Decimals(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
