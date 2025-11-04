/**
 * Feature Enhancement (ENGG*4450 Phase 3)
 * GitHub Issue #878 – Show probabilities for Truecaser classes.
 *
 * This stub illustrates how the TrueCaseAnnotator could output 
 * per-class probabilities via truecase.outputProbabilities property.
 */
package edu.stanford.nlp.pipeline;

public class TruecaseProbabilityExtension {
    public static void main(String[] args) {
        System.out.println("Simulated Truecase probabilities output:");
        System.out.println("{LOWER=0.06, UPPER=0.02, TITLE=0.10, INITCAP=0.82}");
    }
}
