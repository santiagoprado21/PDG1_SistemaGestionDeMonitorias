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

# Render asigna el puerto via variable de entorno PORT
EXPOSE 5435

# Usa PORT si lo provee Render, si no usa 5435 por defecto
CMD java -Dserver.port=${PORT:-5435} -jar app.jar
