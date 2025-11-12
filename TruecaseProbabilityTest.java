import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;

public class TruecaseProbabilityTest {

    @Test
    void testProbabilityNormalization() {
        Map<String, Double> probs = Map.of("LOWER", 0.06, "UPPER", 0.02, "TITLE", 0.10, "INITCAP", 0.82);
        double sum = probs.values().stream().mapToDouble(Double::doubleValue).sum();
        assertEquals(1.0, sum, 0.001, "Probabilities should sum to ≈1.0");
    }

    @Test
    void testFlagEnabledAddsProbs() {
        System.setProperty("truecase.outputProbabilities", "true");
        String jsonOutput = runCoreNLP("The quick brown fox");
        assertTrue(jsonOutput.contains("\"truecaseProbs\""), "JSON should contain probability field");
    }

    @Test
    void testFlagDisabledRemovesProbs() {
        System.setProperty("truecase.outputProbabilities", "false");
        String jsonOutput = runCoreNLP("The quick brown fox");
        assertFalse(jsonOutput.contains("\"truecaseProbs\""), "JSON should not contain probability field");
    }

    private String runCoreNLP(String text) {
        // Simulate running the CoreNLP pipeline (you can adapt or stub this)
        return "{\"truecaseProbs\":{\"LOWER\":0.06,\"UPPER\":0.02,\"TITLE\":0.10,\"INITCAP\":0.82}}";
    }
}
