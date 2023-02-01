#################################################################
####################### BUILD STAGE #############################
#################################################################
FROM eclipse-temurin:17-jdk as builder

WORKDIR /workspace/app

COPY gradle gradle
COPY gradlew .
COPY build.gradle .
COPY settings.gradle .
COPY src src

RUN ./gradlew clean build
RUN mkdir -p build/dependency && (cd build/dependency; jar -xf ../libs/*.jar)


##################################################################
######################## TARGET STAGE ############################
##################################################################
FROM eclipse-temurin:17-jdk-alpine

ARG DEPENDENCY=/workspace/app/build/dependency

COPY --from=builder ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=builder ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=builder ${DEPENDENCY}/BOOT-INF/classes /app

# Copy launcher
COPY --chown=root docker /app

# Env variables for launcher
ENV     CPU_CORES 4
ENV     JAVA_XMS 256m
ENV     JAVA_XMX 4G

CMD ["/app/run.sh"]