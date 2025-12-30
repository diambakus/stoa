# ---- Runtime stage ----
FROM openjdk:21-jdk-slim
LABEL authors="adruida"

EXPOSE 8080

# Create app user
RUN addgroup --system orakuma && adduser --system orakuma --ingroup orakuma
USER orakuma:orakuma

# Copy built jar from builder stage
COPY /target/*.jar /opt/app/stoa.jar

CMD ["java", "-showversion", "-jar", "/opt/app/stoa.jar"]