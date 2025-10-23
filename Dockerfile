FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy the CoreNLP distribution and properties file
COPY stanford-corenlp-4.5.10 /app/stanford-corenlp-4.5.10
COPY CoreNLP.properties /app/CoreNLP.properties

# Expose the server port
EXPOSE 9000

# Set default memory allocation
ENV CORENLP_MEM=4g

# Start the Stanford CoreNLP server
ENTRYPOINT ["bash", "-c", "cd /app/stanford-corenlp-4.5.10 && java -mx${CORENLP_MEM} -cp '*' edu.stanford.nlp.pipeline.StanfordCoreNLPServer -port 9000 -timeout 15000 -props /app/CoreNLP.properties"]
