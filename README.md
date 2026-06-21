# PDG1_SistemaGestionDeMonitorias

## Guía de Ejecución — Gestión de Monitorías (SIGMA)

Este manual describe los pasos necesarios para inicializar los tres servicios principales del sistema desde la consola.

## 1. Inicializar API Banner

Primero, debemos levantar el servicio de la API de Banner.

cd API-Banner-main

Ejecutar:
./mvnw spring-boot:run

## 2. Inicializar Backend SIGMA

En una nueva terminal, ingresa al repositorio del backend principal y ejecútalo.

cd PDG-SIGMA-BACKEND-main

Ejecutar:
./mvnw spring-boot:run

## 3. Inicializar Frontend
Finalmente, en una nueva terminal, levanta la interfaz de usuario.

cd PDG-SIGMA-Front

Ejecutar:
npm start