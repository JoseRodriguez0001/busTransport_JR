# BusTransportJR - Sistema de Reservas de Transporte Intermunicipal

##  Descripción
**BusTransportJR** es un sistema backend desarrollado en **Spring Boot** para la gestión integral de reservas de transporte intermunicipal y encomiendas.  
El sistema permite la venta de pasajes con asientos numerados, gestión de rutas con paradas intermedias, manejo de equipaje, envío de encomiendas y administración completa de operaciones de despacho.

Desarrollado como proyecto final de la asignatura **Programación Web** por **Jose Rodriguez y Robert Gonzalez**.

---

##  Funcionalidades Principales

### 🚌 Gestión de Viajes y Reservas
- **Búsqueda de rutas y viajes:** consulta de salidas disponibles según origen, destino y fecha.  
- **Reserva de asientos:** sistema de *hold* temporal (10 minutos) para bloquear asientos durante el proceso de compra.  
- **Venta por tramos:** permite vender pasajes parciales entre paradas intermedias sin solapamiento.  
- **Asientos preferenciales:** primera fila reservada para personas con necesidades especiales.  

### 💳 Sistema de Pagos
- Múltiples métodos de pago: efectivo, transferencia, tarjeta y QR.  
- Generación de códigos QR únicos por ticket.  
- Comprobantes digitales de compra.  

### 🎒 Gestión de Equipaje
- Registro y etiquetado de equipaje con códigos únicos.  
- Cálculo de tarifas por exceso de peso.  
- Validación de límite máximo por maleta (30 kg) y por pasajero (2 maletas).  
- Límite gratuito de 20 kg.  

### 📦 Encomiendas
- Registro de paquetes con datos de remitente y destinatario.  
- Trazabilidad de estados: `CREATED → IN_TRANSIT → DELIVERED/FAILED`.  
- Sistema de OTP para confirmación de entrega.  
- Registro fotográfico de entregas.  

### 🖥️ Panel de Despacho
- Asignación de conductores y buses a viajes.  
- Control de estados de viaje (`SCHEDULED → BOARDING → DEPARTED → ARRIVED`).  
- Gestión de abordaje y validación de tickets.  
- Registro de incidentes.  

### 💲 Tarifas Dinámicas
- Precios base por tramo configurables.  
- Cálculo automático según ocupación del bus.  
- Descuentos por tipo de pasajero (configurable).  

### 🛠️ Administración
- Gestión de usuarios con 5 roles: `PASSENGER`, `CLERK`, `DRIVER`, `DISPATCHER`, `ADMIN`.  
- Configuración del sistema mediante parámetros (tiempos de hold, tarifas, límites).  
- KPIs de ocupación, ingresos y operación.  

---

##  Arquitectura Técnica

### 🧰 Stack Tecnológico
- **Framework:** Spring Boot 3.4.0  
- **Base de Datos:** PostgreSQL 17 con Testcontainers  
- **Seguridad:** JWT + Spring Security  
- **Mapeo:** MapStruct  
- **Migraciones:** Flyway  
- **Testing:** JUnit 5, Mockito, MockMvc  
- **Build:** Maven  

### 🧱 Capas de la Aplicación
Controllers (DTOs + Validación)

↓

Services (Lógica de Negocio)

↓

Repositories (Spring Data JPA)

↓

PostgreSQL

---

## 🧪 Pruebas

El proyecto cuenta con una cobertura exhaustiva de pruebas:

- **Total de Tests:** 499 tests  
- **Tipos de Tests:**
  - Tests Unitarios de Servicios (con Mockito)  
  - Tests de Integración de Repositorios (con Testcontainers)  
  - Tests de Controladores (con MockMvc)  

**Coverage Aproximado:** 85-90% en servicios y repositorios.

Todas las pruebas usan Testcontainers para garantizar un PostgreSQL real durante la ejecución de tests, asegurando confiabilidad y consistencia.

---

## 🚀 Características Destacadas
- **Gestión de tramos:** permite vender asientos por segmentos sin conflictos.  
- **Hold automático:** reserva temporal con expiración.  
- **Validaciones de negocio:** peso de equipaje, capacidad de buses, estados de tickets.  
- **Auditoría completa:** timestamps en todas las entidades.  
- **Código limpio:** arquitectura clara, nombres descriptivos y principios SOLID.  

---

## 👥 Autores
- **Jose Rodriguez**  +  **Robert Gonzalez** 

Proyecto final — *Programación Web*  
**Universidad del Magdalena**
