import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.semgraph.*;
import edu.stanford.nlp.ling.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

public class CoreNLPDemo {
    public static void main(String[] args) {
        try {
            // -------------------------------
            // 1. Configure CoreNLP Pipeline
            // -------------------------------
            Properties props = new Properties();
            props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner,parse,depparse,sentiment");

            // Explicit POS model for compatibility
            props.setProperty("pos.model", "edu/stanford/nlp/models/pos-tagger/english-left3words-distsim.tagger");

            StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

            // -------------------------------
            // 2. Input Text
            // -------------------------------
            String text = "Kosgi Santosh sent an email to Stanford University. He didn’t get a reply.";
            CoreDocument document = new CoreDocument(text);
            pipeline.annotate(document);

            // -------------------------------
            // 3. Define Output Directory and File
            // -------------------------------
            String outputDir = "results/";
            File directory = new File(outputDir);
            if (!directory.exists()) {
                boolean created = directory.mkdirs();
                if (created) {
                    System.out.println("[INFO] Created output directory: " + directory.getAbsolutePath());
                } else {
                    System.err.println("[ERROR] Could not create output directory. Check permissions.");
                    return;
                }
            }

            // Add timestamp to output file
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String outputPath = outputDir + "analysis_output_" + timeStamp + ".txt";

            // -------------------------------
            // 4. Write Annotated Results
            // -------------------------------
            try (FileWriter writer = new FileWriter(outputPath)) {
                writer.write("=== Stanford CoreNLP Analysis Output ===\n\n");

                for (CoreSentence sentence : document.sentences()) {
                    writer.write("Sentence: " + sentence.text() + "\n");
                    writer.write("Sentiment: " + sentence.sentiment() + "\n");
                    writer.write("Parse Tree: " + sentence.constituencyParse() + "\n");
                    writer.write("Dependencies: " + sentence.dependencyParse() + "\n");
                    writer.write("Entities: " + sentence.entityMentions() + "\n\n");
                }

                // -------------------------------
                // 5. Coreference Chains
                // -------------------------------
                writer.write("=== Coreference Chains ===\n");
                if (document.corefChains() != null) {
                    for (var entry : document.corefChains().entrySet()) {
                        writer.write("Chain " + entry.getKey() + ": " + entry.getValue() + "\n");
                    }
                } else {
                    writer.write("No coreference chains found.\n");
                }

                writer.write("\n=== End of Analysis ===\n");
                System.out.println("[SUCCESS] Results written to: " + outputPath);

            } catch (IOException e) {
                System.err.println("[ERROR] Cannot write to file: " + outputPath);
                System.err.println("Details: " + e.getMessage());
            }

        } catch (Exception e) {
            System.err.println("[FATAL] Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}