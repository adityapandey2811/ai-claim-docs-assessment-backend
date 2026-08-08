FROM eclipse-temurin:21-jdk

RUN apt-get update && \
    apt-get install -y maven tesseract-ocr tesseract-ocr-eng && \
    rm -rf /var/lib/apt/lists/*

RUN tesseract --version

RUN find /usr/share -name eng.traineddata

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

ENV dataPath=/usr/share/tesseract-ocr/5/tessdata

EXPOSE 8080

CMD ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar target/invoice-intelligence-0.0.1-SNAPSHOT.jar"]