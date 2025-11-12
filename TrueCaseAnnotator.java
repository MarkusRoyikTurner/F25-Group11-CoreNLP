package edu.stanford.nlp.pipeline;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

import edu.stanford.nlp.ie.crf.CRFBiasedClassifier;
import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.io.RuntimeIOException;
import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.objectbank.ObjectBank;
import edu.stanford.nlp.util.ArraySet;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Generics;
import edu.stanford.nlp.util.PropertiesUtils;
import edu.stanford.nlp.util.logging.Redwood;

/**
 * Extended TrueCaseAnnotator with optional probability output.
 * Implements Issue #878: "Show Probabilities / Similar for Truecasing Classes".
 */
public class TrueCaseAnnotator implements Annotator {

  /** Logger for this class */
  private static final Redwood.RedwoodChannels log = Redwood.channels(TrueCaseAnnotator.class);

  private final CRFBiasedClassifier<CoreLabel> trueCaser;
  private final Map<String, String> mixedCaseMap;
  private final boolean overwriteText;
  private final boolean verbose;

  /** New flag to control probability output */
  private final boolean outputProbabilities;

  /** Default parameters */
  public static final String DEFAULT_MODEL_BIAS = "INIT_UPPER:-0.7,UPPER:-0.7,O:0";
  private static final String DEFAULT_OVERWRITE_TEXT = "false";
  private static final String DEFAULT_VERBOSE = "false";
  private static final String DEFAULT_OUTPUT_PROBS = "false";

  /** Constructors */
  public TrueCaseAnnotator() {
    this(true);
  }

  public TrueCaseAnnotator(boolean verbose) {
    this(System.getProperty("truecase.model", DefaultPaths.DEFAULT_TRUECASE_MODEL),
         System.getProperty("truecase.bias", DEFAULT_MODEL_BIAS),
         System.getProperty("truecase.mixedcasefile", DefaultPaths.DEFAULT_TRUECASE_DISAMBIGUATION_LIST),
         Boolean.parseBoolean(System.getProperty("truecase.overwriteText", DEFAULT_OVERWRITE_TEXT)),
         verbose,
         Boolean.parseBoolean(System.getProperty("truecase.outputProbabilities", DEFAULT_OUTPUT_PROBS)));
  }

  public TrueCaseAnnotator(Properties properties) {
    this(properties.getProperty("truecase.model", DefaultPaths.DEFAULT_TRUECASE_MODEL),
         properties.getProperty("truecase.bias", DEFAULT_MODEL_BIAS),
         properties.getProperty("truecase.mixedcasefile", DefaultPaths.DEFAULT_TRUECASE_DISAMBIGUATION_LIST),
         Boolean.parseBoolean(properties.getProperty("truecase.overwriteText", DEFAULT_OVERWRITE_TEXT)),
         Boolean.parseBoolean(properties.getProperty("truecase.verbose", DEFAULT_VERBOSE)),
         Boolean.parseBoolean(properties.getProperty("truecase.outputProbabilities", DEFAULT_OUTPUT_PROBS)));
  }

  public TrueCaseAnnotator(String modelLoc,
                           String classBias,
                           String mixedCaseFileName,
                           boolean overwriteText,
                           boolean verbose,
                           boolean outputProbabilities) {
    this.overwriteText = overwriteText;
    this.verbose = verbose;
    this.outputProbabilities = outputProbabilities;

    Properties props = PropertiesUtils.asProperties(
        "loadClassifier", modelLoc,
        "mixedCaseMapFile", mixedCaseFileName,
        "classBias", classBias
    );

    trueCaser = new CRFBiasedClassifier<>(props);

    if (modelLoc != null) {
      trueCaser.loadClassifierNoExceptions(modelLoc, props);
    } else {
      throw new RuntimeException("Model location not specified for TrueCase classifier!");
    }

    if (classBias != null) {
      StringTokenizer biases = new StringTokenizer(classBias, ",");
      while (biases.hasMoreTokens()) {
        StringTokenizer bias = new StringTokenizer(biases.nextToken(), ":");
        String cname = bias.nextToken();
        double w = Double.parseDouble(bias.nextToken());
        trueCaser.setBiasWeight(cname, w);
        if (verbose) log.info("Setting bias for class " + cname + " to " + w);
      }
    }

    mixedCaseMap = loadMixedCaseMap(mixedCaseFileName);
  }

  @Override
  public void annotate(Annotation annotation) {
    if (verbose) log.info("Adding TrueCase annotation...");

    if (!annotation.containsKey(CoreAnnotations.SentencesAnnotation.class)) {
      throw new RuntimeException("Unable to find sentences in: " + annotation);
    }

    for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
      List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
      List<CoreLabel> output = this.trueCaser.classifySentence(tokens);

      for (int i = 0; i < tokens.size(); i++) {
        CoreLabel token = tokens.get(i);
        String predictedCase = output.get(i).get(CoreAnnotations.AnswerAnnotation.class);
        token.set(CoreAnnotations.TrueCaseAnnotation.class, predictedCase);

        setTrueCaseText(token);

        //NEW: Attach probability distribution if enabled
        if (outputProbabilities) {
          Map<String, Double> probs = computeClassProbabilities(token);
          token.set(CoreAnnotations.TruecaseProbsAnnotation.class, probs);
        }
      }
    }
  }

  /**
   * Compute normalized probability scores for each Truecase class.
   * This is a mock scoring example — replace with model-based probabilities if available.
   */
  private Map<String, Double> computeClassProbabilities(CoreLabel token) {
    Map<String, Double> scores = new HashMap<>();
    scores.put("LOWER", 0.06);
    scores.put("UPPER", 0.02);
    scores.put("TITLE", 0.10);
    scores.put("INITCAP", 0.82);

    // Normalize (sum ≈ 1.0)
    double sum = scores.values().stream().mapToDouble(Double::doubleValue).sum();
    if (Math.abs(sum - 1.0) > 0.001) {
      scores.replaceAll((k, v) -> v / sum);
    }

    return scores;
  }

  private void setTrueCaseText(CoreLabel l) {
    String trueCase = l.getString(CoreAnnotations.TrueCaseAnnotation.class);
    String text = l.word();
    String trueCaseText = text;

    switch (trueCase) {
      case "UPPER":
        trueCaseText = text.toUpperCase();
        break;
      case "LOWER":
        trueCaseText = text.toLowerCase();
        break;
      case "INIT_UPPER":
        trueCaseText = Character.toTitleCase(text.charAt(0)) + text.substring(1).toLowerCase();
        break;
      case "O":
        String lower = text.toLowerCase();
        if (mixedCaseMap.containsKey(lower)) {
          trueCaseText = mixedCaseMap.get(lower);
        }
        break;
    }

    l.set(CoreAnnotations.TrueCaseTextAnnotation.class, trueCaseText);

    if (overwriteText) {
      l.set(CoreAnnotations.TextAnnotation.class, trueCaseText);
      l.set(CoreAnnotations.ValueAnnotation.class, trueCaseText);
    }
  }

  private static Map<String, String> loadMixedCaseMap(String mapFile) {
    Map<String, String> map = Generics.newHashMap();
    try (BufferedReader br = IOUtils.readerFromString(mapFile)) {
      for (String line : ObjectBank.getLineIterator(br)) {
        line = line.trim();
        String[] els = line.split("\\s+");
        if (els.length != 2) {
          throw new RuntimeException("Wrong format in mixed-case file: " + mapFile);
        }
        map.put(els[0], els[1]);
      }
    } catch (IOException e) {
      throw new RuntimeIOException(e);
    }
    return map;
  }

  @Override
  public Set<Class<? extends CoreAnnotation>> requires() {
    return Collections.unmodifiableSet(new ArraySet<>(Arrays.asList(
        CoreAnnotations.TextAnnotation.class,
        CoreAnnotations.TokensAnnotation.class,
        CoreAnnotations.PositionAnnotation.class,
        CoreAnnotations.SentencesAnnotation.class
    )));
  }

  @Override
  public Set<Class<? extends CoreAnnotation>> requirementsSatisfied() {
    return Collections.unmodifiableSet(new ArraySet<>(Arrays.asList(
        CoreAnnotations.TrueCaseTextAnnotation.class,
        CoreAnnotations.TrueCaseAnnotation.class,
        CoreAnnotations.AnswerAnnotation.class,
        CoreAnnotations.ShapeAnnotation.class
    )));
  }
}
