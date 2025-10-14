# Etapa de construcción
FROM eclipse-temurin:21-jdk as build
WORKDIR /app

COPY . .

# Da permisos de ejecución al wrapper de Maven
RUN chmod +x ./mvnw

# Construye el .jar (omite tests si quieres más velocidad)
RUN ./mvnw clean package -DskipTests

# Etapa de ejecución
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copia el .jar desde la etapa de build
COPY --from=build /app/target/*.jar app.jar

# Expone el puerto por defecto de Spring Boot
EXPOSE 5431

# Comando para ejecutar el .jar
ENTRYPOINT ["java", "-jar", "app.jar"]
