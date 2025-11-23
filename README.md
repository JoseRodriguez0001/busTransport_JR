# 🚌 BusTransportJR - Sistema de Gestión de Transporte Intermunicipal

<div align="center">

![Java](https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.0-brightgreen?style=for-the-badge&logo=spring)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-blue?style=for-the-badge&logo=postgresql)
![Maven](https://img.shields.io/badge/Maven-3.9+-red?style=for-the-badge&logo=apache-maven)
![License](https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge)

Sistema empresarial backend para la gestión integral de transporte de pasajeros y encomiendas con arquitectura escalable y seguridad JWT.

[Características](#-características-principales) •
[Arquitectura](#-arquitectura-técnica) •
[Instalación](#-instalación) •
[Testing](#-testing) •
[Licencia](#-licencia)

</div>

---

## 📋 Descripción

**BusTransportJR** es un sistema backend robusto desarrollado con **Spring Boot 3.4** que gestiona operaciones completas de una empresa de transporte intermunicipal. El sistema maneja la venta de pasajes con asientos numerados, gestión de rutas con múltiples paradas, control de equipaje, envío de encomiendas con trazabilidad completa y administración de operaciones de despacho en tiempo real.

### 🎯 Problema que Resuelve

Digitaliza y optimiza las operaciones de empresas de transporte terrestre, eliminando procesos manuales propensos a errores como venta de pasajes duplicados, control manual de equipaje, falta de trazabilidad en encomiendas y gestión ineficiente de rutas con paradas intermedias.

---

## ✨ Características Principales

### 🎫 Sistema de Reservas Inteligente
- **Hold temporal**: Reserva de 10 minutos que bloquea asientos durante el proceso de compra
- **Venta por tramos**: Permite vender pasajes parciales entre paradas intermedias sin solapamiento
- **Asientos preferenciales**: Primera fila reservada automáticamente para personas con necesidades especiales
- **Sobreventa controlada**: Sistema de overbooking con límites configurables y aprobación de despachador
- **Precios dinámicos**: Tarifas que varían según ocupación del bus y tipo de pasajero

### 💳 Gestión de Pagos
- Múltiples métodos: Efectivo, transferencia bancaria, tarjeta y código QR
- Generación automática de códigos QR únicos por transacción
- Comprobantes digitales con información detallada

### 🎒 Control de Equipaje
- Registro y etiquetado con códigos únicos de 6 dígitos
- Cálculo automático de tarifas por exceso de peso (límite gratuito: 20kg)
- Validación de límites por maleta (máx. 30kg) y por pasajero (máx. 2 maletas)

### 📦 Trazabilidad de Encomiendas
- Estados controlados: `CREATED → IN_TRANSIT → DELIVERED / FAILED`
- Sistema OTP (One-Time Password) para confirmación segura de entrega
- Registro fotográfico obligatorio en entregas
- Datos completos de remitente y destinatario

### 🖥️ Panel de Despacho
- Asignación dinámica de conductores y buses a viajes
- Control de abordaje con validación de tickets en tiempo real
- Registro y gestión de incidentes durante el viaje
- Dashboard con métricas operacionales (ocupación, ingresos, puntualidad)

### 🔐 Seguridad y Autenticación
- Autenticación JWT con refresh tokens
- 5 roles jerárquicos: `PASSENGER`, `CLERK`, `DRIVER`, `DISPATCHER`, `ADMIN`
- Control de acceso basado en roles (RBAC)
- Cifrado de contraseñas con BCrypt

---

## 🏗️ Arquitectura Técnica

### Stack Tecnológico

| Tecnología | Versión | Propósito |
|------------|---------|-----------|
| **Java** | 17 | Lenguaje de programación |
| **Spring Boot** | 3.4.0 | Framework principal |
| **Spring Security** | 6.x | Autenticación y autorización |
| **PostgreSQL** | 17 | Base de datos relacional |
| **JWT** | 0.11.5 | Tokens de autenticación |
| **MapStruct** | 1.5.5 | Mapeo objeto-DTO |
| **Flyway** | 10.x | Migraciones de BD |
| **Testcontainers** | 1.19.x | Testing con contenedores |
| **Maven** | 3.9+ | Gestión de dependencias |

### Arquitectura en Capas

```
┌─────────────────────────────────────────────┐
│           PRESENTATION LAYER                 │
│  Controllers + DTOs + Validation             │
└─────────────────┬───────────────────────────┘
                  │
┌─────────────────▼───────────────────────────┐
│           BUSINESS LAYER                     │
│  Services + Business Logic                   │
└─────────────────┬───────────────────────────┘
                  │
┌─────────────────▼───────────────────────────┐
│           PERSISTENCE LAYER                  │
│  Repositories + JPA Entities                 │
└─────────────────┬───────────────────────────┘
                  │
┌─────────────────▼───────────────────────────┐
│              DATABASE                        │
│  PostgreSQL 17                               │
└─────────────────────────────────────────────┘
```

### Principios de Diseño

- ✅ **SOLID Principles**: Código mantenible y extensible
- ✅ **Clean Architecture**: Separación clara de responsabilidades
- ✅ **RESTful API**: Endpoints siguiendo convenciones REST
- ✅ **DTOs**: Separación entre entidades de BD y objetos de transferencia

---

## 🚀 Instalación

### Prerrequisitos

- **Java 17** o superior
- **Maven 3.9+**
- **PostgreSQL 17** (o Docker)
- **Git**

### Configuración Rápida

1. **Clonar el repositorio**
```bash
git clone https://github.com/tu-usuario/busTransport_JR.git
cd busTransport_JR
```

2. **Configurar base de datos**
```bash
# Opción con Docker
docker run --name postgres-bus \
  -e POSTGRES_DB=busdb \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=tu_password \
  -p 5432:5432 \
  -d postgres:17
```

3. **Configurar variables de entorno**
```bash
# Linux/Mac
export POSTGRES_USER=postgres
export POSTGRES_PASSWORD=tu_password
export POSTGRES_DB=busdb
export JWT_SECRET=tu_clave_secreta_muy_larga_y_segura

# Windows PowerShell
$env:POSTGRES_USER="postgres"
$env:POSTGRES_PASSWORD="tu_password"
$env:POSTGRES_DB="busdb"
$env:JWT_SECRET="tu_clave_secreta_muy_larga_y_segura"
```

4. **Ejecutar la aplicación**
```bash
mvn spring-boot:run
# Disponible en: http://localhost:8080
```

---

## 🧪 Testing

### Cobertura de Tests

El proyecto cuenta con **499 tests** distribuidos en:

| Tipo de Test | Cantidad | Cobertura |
|--------------|----------|-----------|
| **Tests Unitarios** (Servicios) | ~300 | 90% |
| **Tests de Integración** (Repositorios) | ~120 | 85% |
| **Tests de Controladores** (MockMvc) | ~79 | 88% |

**Cobertura Global**: ~85-90% en todas las capas

### Ejecutar Tests

```bash
# Todos los tests
mvn test

# Con reporte de cobertura
mvn test jacoco:report
```

---

## 🔧 Características Técnicas Destacadas

### Sistema de Hold Temporal
Los asientos se reservan por 10 minutos durante la compra. Si no se confirma el pago, se liberan automáticamente mediante un proceso programado.

### Venta por Tramos
El sistema permite vender asientos en tramos intermedios sin conflictos:
```
Ruta: A → B → C → D

Pasajero 1: A → C (asiento 12) ✅
Pasajero 2: C → D (asiento 12) ✅ Sin conflicto
Pasajero 3: B → D (asiento 12) ❌ Conflicto detectado
```

### Validaciones de Negocio
- Peso máximo de equipaje (30kg por maleta, 2 maletas por pasajero)
- Capacidad máxima del bus
- Estados válidos de tickets y encomiendas
- Verificación de disponibilidad de asientos

---

## 🤝 Contribuciones

Las contribuciones son bienvenidas. Por favor:

1. Fork el proyecto
2. Crea una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add: nueva funcionalidad'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

---

## 📜 Licencia

Este proyecto está bajo la Licencia MIT - ver el archivo [LICENSE](LICENSE) para más detalles.

---

## 👥 Autores

<table>
  <tr>
    <td align="center">
      <a href="https://github.com/robertJr18">
        <img src="https://github.com/robertJr18.png" width="100px;" alt="Robert Gonzalez"/><br />
        <sub><b>Robert Gonzalez</b></sub>
      </a><br />
      <sub>Backend Developer</sub>
    </td>
    <td align="center">
      <a href="https://github.com/JoseRodriguez0001">
        <img src="https://github.com/JoseRodriguez0001.png" width="100px;" alt="Jose Rodriguez"/><br />
        <sub><b>Jose Rodriguez</b></sub>
      </a><br />
      <sub>Backend Developer</sub>
    </td>
  </tr>
</table>

---

## 🎓 Contexto Académico

**Proyecto Final** — *Programación Web*  
**Universidad del Magdalena** — Facultad de Ingeniería  
**Semestre**: 2025-2

---

<div align="center">

**⭐ Si este proyecto te fue útil, considera darle una estrella en GitHub ⭐**


</div>
