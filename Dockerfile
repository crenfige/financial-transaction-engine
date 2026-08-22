# Stage 1: Build
FROM eclipse-temurin:24-jdk-alpine AS builder
WORKDIR /workspace/app

# Copiar configuración de Maven y cachear dependencias
COPY pom.xml ./
COPY .mvn/ .mvn/
COPY mvnw ./
RUN chmod +x ./mvnw
RUN ./mvnw dependency:go-offline -B

# Compilar empaquetado sin ejecutar tests en el contenedor
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Stage 2: Runtime image ligera
FROM eclipse-temurin:24-jre-alpine
WORKDIR /app

# Crear usuario no root por seguridad
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /workspace/app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]