# Sistema Punto de Venta - Aplicación POS para Android

<div align="center">
  <img src="app/src/main/res/drawable/logo_img.jpeg" alt="POS System Logo" width="150" />
  <br/>
  <h1>Sistema de Punto de Venta para Android</h1>
  <p>Una solución POS completa y moderna construida con Kotlin y Jetpack Compose</p>
</div>

---

## Descripción General del Proyecto

Este es un **Sistema de Punto de Venta (POS) completo para Android** construido enteramente en **Kotlin** utilizando **Jetpack Compose** para la capa de interfaz de usuario. La aplicación cuenta con una arquitectura moderna con capacidades offline-first, sincronización en tiempo real y soporte para múltiples métodos de pago.

### Características Principales

- **Gestión de Ventas**: Crear, administrar y rastrear transacciones de venta
- **Gestión de Inventario**: Administración de productos y categorías con escaneo de códigos de barras
- **Operaciones de Caja Registradora**: Abrir/cerrar cajones de efectivo, rastrear transacciones
- **Gestión de Usuarios y Roles**: Sistema completo de autenticación y autorización
- **Soporte Offline**: Funcionalidad offline completa con sincronización automática
- **Múltiples Métodos de Pago**: Efectivo, pagos con tarjeta Stripe
- **Reportes y Análisis**: Informes de ventas e insights de negocio
- **Reembolsos y Notas de Crédito**: Manejar devoluciones y emitir notas de crédito
- **Autenticación Biométrica**: Acceso seguro con huella digital/biometría
- **Soporte Multi-Sucursal**: Administrar múltiples ubicaciones de negocio

### Arquitectura

La aplicación sigue los principios de **Arquitectura Limpia** con las siguientes capas:
```
app/src/main/java/com/dev/point_of_sale_sistem_with_kotlin_pos/
├── core/                # Funcionalidad central e infraestructura
│   ├── capabilities/    # Capacidades de la app y gestión de estado
│   ├── network/        # Monitoreo de red y cliente Supabase
│   └── sync/           # Lógica de sincronización
├── data/               # Capa de datos
│   ├── local/          # Base de datos Room, DAOs, entidades
│   ├── mappers/        # Mapeadores de datos
│   └── sales/          # Fuentes de datos de ventas
├── intents/            # Intents de lógica de negocio (patrón MVI)
├── models/             # Modelos de dominio y estados
├── repository/         # Implementaciones de repositorios
├── security/           # Utilidades de seguridad
├── ui/                 # Interfaz de usuario Jetpack Compose
│   ├── screens/        # Pantallas de la app
│   └── theme/          # Tematización Material3
└── viewmodel/          # ViewModels
```

---

## Presentación del Equipo

### Nombre del Proyecto
**Sistema de Punto de Venta para Android (POS)**

### Miembros del Equipo

| Rol | Nombre | Responsabilidad |
|------|------|-------------------|
| Desarrollador Principal | Angel Perez | Arquitectura, Sistemas Centrales, Integración Supabase |
| Desarrollador Móvil | Delanny Mauro | Implementación Android, Interfaz Compose |

### Contacto del Equipo de Desarrollo
- **Organización**: Equipo de Desarrollo
- **Repositorio**: https://github.com/angel1325t/
- **Soporte**: angelalexanderperezmartinez47@gmail.com

---

## Pasos para la Ejecución del Proyecto

### Requisitos Previos

Antes de ejecutar el proyecto, asegúrate de tener instalado lo siguiente:

1. **Android Studio** (Se recomienda la última versión)
   - Descargar desde: https://developer.android.com/studio
   - Versión: Hedgehog (2023.1.1) o más reciente

2. **Java Development Kit (JDK) 17**
   - OpenJDK 17: https://adoptium.net/
   - O usar el JDK incluido en Android Studio

3. **Android SDK**
   - SDK Mínimo: 24 (Android 7.0)
   - SDK Objetivo: 36
   - SDK de Compilación: 36

4. **Gradle**
   - Versión: 8.4 o más reciente (incluido en gradle wrapper)

### Pasos de Instalación

1. **Clonar el Repositorio**
```bash
git clone https://github.com/angel1325t/Point-of-Sale-Sistem-with-kotlin-POS.git
cd Point-of-Sale-Sistem-with-kotlin-POS
```

2. **Configurar Propiedades Locales**

Crear o editar `local.properties` en la raíz del proyecto:
```properties
sdk.dir=C:\\Android\\SDK
SUPABASE_URL=https://uhtlmanoxrfdefelpybs.supabase.co
SUPABASE_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVodGxtYW5veHJmZGVmZWxweWJzIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjA3OTQ5MjEsImV4cCI6MjA3NjM3MDkyMX0.K4D40jXz3Q3T_iq4bGmm1AeOqOzft1hpTi7eVb2siR0
STRIPE_PUBLISHABLE_KEY=pk_test_51SkQhlAqdnCr1KIDe9wN7kt21QejQp5xxXdg1FZh7oVmlKiMZmK7o8dUvwrn5iUEhGLxB11jTxrNcjjN328ElOwX00jJnAr3zv
```

3. **Abrir en Android Studio**
   - Lanzar Android Studio
   - Seleccionar "Open" y navegar al directorio del proyecto
   - Esperar a que se complete la sincronización de Gradle

4. **Compilar el Proyecto**
```bash
./gradlew assembleDebug
```

5. **Ejecutar en Emulador o Dispositivo**
   - Conectar un dispositivo Android o iniciar un emulador
   - Hacer clic en "Run" en Android Studio o usar:
```bash
./gradlew installDebug
```

### Compilación para Producción
```bash
./gradlew assembleRelease
```

Para generar un APK firmado:
1. Crear un archivo keystore
2. Configurar la firma en `build.gradle.kts`
3. Compilar la variante de release

---

## Información de la Base de Datos

### Nombre de la Base de Datos

La aplicación utiliza dos sistemas de base de datos:

#### 1. **Supabase (PostgreSQL)**
- **Base de datos**: PostgreSQL 15+ (alojada en Supabase)
- **URL en la nube**: `https://uhtlmanoxrfdefelpybs.supabase.co`
- **Conexión**: Vía API REST de Supabase y PostgREST

#### 2. **Room Database (Local)**
- **Nombre de la base de datos**: `offline_pos_database`
- **Tipo**: SQLite (vía Room)
- **Ubicación**: Almacenamiento interno del dispositivo
- **Tablas**:
  - `offline_sales` - Transacciones de venta
  - `offline_sale_details` - Artículos de línea de venta
  - `offline_cash_register_history` - Operaciones de caja registradora
  - `offline_product_stock` - Inventario de productos
  - `sync_queue` - Cola de sincronización

### Esquema de la Base de Datos

#### Tablas Principales (Supabase)

| Tabla | Descripción |
|-------|-------------|
| `users` | Cuentas de usuario con autenticación |
| `roles` | Roles y permisos de usuario |
| `branches` | Ubicaciones/sucursales del negocio |
| `categories` | Categorías de productos |
| `products` | Inventario de productos |
| `suppliers` | Gestión de proveedores |
| `sales` | Transacciones de venta |
| `sale_details` | Artículos individuales de venta |
| `cash_register_history` | Operaciones de cajón de efectivo |
| `credit_notes` | Créditos de reembolso |
| `refunds` | Transacciones de devolución |

#### Clases de Entidad
```kotlin
// Entidades principales definidas en:
app/src/main/java/com/dev/point_of_sale_sistem_with_kotlin_pos/data/local/entities/
- OfflineSaleEntity
- OfflineSaleDetailEntity
- OfflineCashRegisterHistoryEntity
- OfflineProductStockEntity
- SyncQueueEntity
```

---

### Flujo de Autenticación

1. **Pantalla de Login**: Autenticación con email/contraseña vía Supabase Auth
2. **Opción Biométrica**: Autenticación con huella digital/rostro (si está habilitada)
3. **Gestión de Sesión**: Sesiones basadas en tokens JWT
4. **Cierre de Sesión Automático**: Timeout configurable para seguridad

---

## Dependencias

### Dependencias Principales

#### AndroidX & Jetpack
```kotlin
implementation("androidx.core:core-ktx:1.13.1")
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
implementation("androidx.activity:activity-compose:1.9.2")
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.ui:ui-graphics")
implementation("androidx.compose.ui:ui-tooling-preview")
implementation("androidx.material3:material3:1.3.1")
implementation("androidx.navigation:navigation-compose:2.8.0")
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
```

#### Base de Datos
```kotlin
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
ksp("androidx.room:room-compiler:2.6.1")
```

#### Supabase
```kotlin
implementation(platform("io.github.jan-tennert.supabase:bom:3.2.4"))
implementation("io.github.jan-tennert.supabase:postgrest-kt")
implementation("io.github.jan-tennert.supabase:auth-kt")
implementation("io.github.jan-tennert.supabase:storage-kt")
implementation("io.github.jan-tennert.supabase:functions-kt")
```

#### Pagos
```kotlin
implementation("com.stripe:stripe-android:20.49.0")
```

#### Firebase
```kotlin
implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
implementation("com.google.firebase:firebase-messaging")
```

#### Cámara y Escaneo
```kotlin
implementation("androidx.camera:camera-camera2:1.4.0")
implementation("androidx.camera:camera-lifecycle:1.4.0")
implementation("androidx.camera:camera-view:1.4.0")
implementation("com.google.mlkit:barcode-scanning:17.3.0")
```

#### Coroutines y Serialización
```kotlin
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
```

#### Seguridad
```kotlin
implementation("androidx.security:security-crypto:1.1.0-alpha06")
implementation("androidx.biometric:biometric:1.2.0-alpha05")
```

#### Red
```kotlin
implementation("io.ktor:ktor-client-core:3.3.1")
implementation("io.ktor:ktor-client-android:3.3.1")
```

### Lista Completa de Dependencias

Ver `app/build.gradle.kts` para la lista completa de dependencias.

### Versiones de Dependencias

| Dependencia | Versión | Propósito |
|------------|---------|-----------|
| Kotlin | 1.9.24 | Lenguaje |
| Compose BOM | 2024.09.00 | Framework de UI |
| Room | 2.6.1 | Base de datos local |
| Supabase | 3.2.4 | Servicios de backend |
| Stripe | 20.49.0 | Procesamiento de pagos |
| Firebase | 33.5.1 | Notificaciones push |
| CameraX | 1.4.0 | Escaneo de códigos de barras |
| Ktor | 3.3.1 | Cliente HTTP |

---

## Detalles de Implementación de API

### 1. **API de Autenticación Supabase**

**Archivo**: `repository/admin/users/UserRepository.kt`
```kotlin
class UserRepository(private val supabase: SupabaseClient) {
    // Gestión de login y sesión
    suspend fun getCurrentUser(): UserModel?
    suspend fun getAllActiveUsers(): List
    suspend fun createUser(email, roleId, companyId, branchId): UserModel
    suspend fun updateUser(authId, branchId, roleId): UserModel
    suspend fun deleteUser(authId)
}
```

**Características**:
- Autenticación basada en tokens JWT
- Persistencia de sesión con `AndroidSessionManager`
- Control de acceso basado en roles
- Soporte para eliminación suave

### 2. **API de Base de Datos Supabase (PostgREST)**

**Archivos**:
- `repository/admin/products/ProductRepository.kt`
- `repository/admin/categories/CategoryRepository.kt`
- `repository/admin/branches/BranchRepository.kt`
```kotlin
class ProductRepository(private val supabase: SupabaseClient) {
    suspend fun getAllProducts(): List
    suspend fun getProductById(id: Int): Product?
    suspend fun createProduct(product: Product): Product
    suspend fun updateProduct(id: Int, product: Product)
    suspend fun deleteProduct(id: Int)
    suspend fun searchProducts(query: String): List
}
```

**Operaciones**:
- Operaciones CRUD vía PostgREST
- Filtrado y ordenamiento
- Suscripciones en tiempo real
- Operaciones por lotes

### 3. **API de Pagos Stripe**

**Archivo**: `repository/sales/sales_orders/StripePaymentRepository.kt`
```kotlin
class StripePaymentRepository(
    private val context: Context,
    private val publishableKey: String,
    private val supabase: SupabaseClient
) {
    suspend fun createPaymentIntent(amount, currency, saleId): Result
    suspend fun confirmPayment(paymentIntentId): Result
}
```

**Flujo**:
1. Crear PaymentIntent vía Edge Function de Supabase
2. Presentar hoja de pago de Stripe
3. Confirmar pago y verificar estado
4. Registrar comprobante de pago en la base de datos

### 4. **API de Firebase Cloud Messaging**

**Archivo**: `FcmService.kt`
```kotlin
class FcmService : FirebaseMessagingService() {
    override fun onNewToken(token: String)
    override fun onMessageReceived(message: RemoteMessage)
}
```

**Características**:
- Notificaciones push para actualizaciones de sincronización
- Notificaciones de ventas en tiempo real
- Alertas del sistema

### 5. **API de Base de Datos Local (Room)**

**Archivos**:
- `data/local/dao/OfflineSalesDao.kt`
- `data/local/dao/CashRegisterDao.kt`
- `data/local/dao/StockDao.kt`
- `data/local/dao/SyncQueueDao.kt`
```kotlin
@Dao
interface OfflineSalesDao {
    @Insert
    suspend fun insertSale(sale: OfflineSaleEntity)
    
    @Transaction
    @Query("SELECT * FROM offline_sales WHERE pendingSync = 1")
    suspend fun getPendingSalesWithDetails(): List
    
    @Query("UPDATE offline_sales SET pendingSync = 0 WHERE localSaleId = :localSaleId")
    suspend fun markAsSynced(localSaleId: String)
}
```

**Características**:
- Registro de ventas offline-first
- Gestión automática de cola de sincronización
- Resolución de conflictos
- Caché local

### 6. **Edge Functions API**

**Supabase Edge Functions**:
- `create-stripe-payment-intent` - Crear pagos Stripe
- `confirm-stripe-payment` - Confirmar pagos
- `sync-data` - Sincronizar datos offline

### 7. **API de Gestión de Ventas**

**Archivo**: `repository/sales/sales_orders/SalesRepository.kt`
```kotlin
class SalesRepository(private val supabase: SupabaseClient) {
    suspend fun createSale(sale: SaleRequest): SaleResponse
    suspend fun getSaleById(id: String): Sale?
    suspend fun getSalesByDateRange(start, end): List
    suspend fun cancelSale(id: String)
    suspend fun refundSale(id: String, amount: Double)
}
```

### 8. **API de Caja Registradora**

**Archivo**: `repository/sales/cash_register/CashRegisterRepository.kt`
```kotlin
class CashRegisterRepository(private val supabase: SupabaseClient) {
    suspend fun openRegister(amount: Double): CashRegisterSession
    suspend fun closeRegister(sessionId: String, finalAmount: Double)
    suspend fun getRegisterHistory(branchId): List
    suspend fun addTransaction(sessionId, transaction: Transaction)
}
```

### 9. **API de Generación de Reportes**

**Archivo**: `repository/sales/reports/SalesReportRepository.kt`
```kotlin
class SalesReportRepository(private val supabase: SupabaseClient) {
    suspend fun getDailySalesReport(date: LocalDate): SalesReport
    suspend fun getMonthlySalesReport(year: Int, month: Int): MonthlyReport
    suspend fun getTopProducts(limit: Int): List
    suspend fun getSalesByCategory(): Map
}
```

---

## Guía de Configuración

### 1. **Configuración de Supabase**

#### Variables de Entorno (local.properties)
```properties
# Configuración de Supabase
SUPABASE_URL=https://uhtlmanoxrfdefelpybs.supabase.co
SUPABASE_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVodGxtYW5veHJmZGVmZWxweWJzIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjA3OTQ5MjEsImV4cCI6MjA3NjM3MDkyMX0.K4D40jXz3Q3T_iq4bGmm1AeOqOzft1hpTi7eVb2siR0
```

#### Configuración de la Base de Datos

1. **Crear Proyecto Supabase**
   - Ir a: https://supabase.com
   - Crear nuevo proyecto

2. **Ejecutar Migraciones de Base de Datos**

Ejecutar scripts SQL en el Editor SQL de Supabase:
```sql
-- Habilitar Row Level Security
alter table users enable row level security;

-- Crear tablas (ver esquema de base de datos)
```

3. **Configurar Políticas RLS**
```sql
create policy "Los usuarios pueden ver sus propios datos"
on users for select
using (auth.uid() = auth_id);
```

#### Configuración de Edge Functions

Desplegar Supabase Edge Functions:
```bash
supabase functions new create-stripe-payment-intent
supabase functions new confirm-stripe-payment
```

### 2. **Configuración de Stripe**

#### Obtener Claves de Stripe

1. Crear cuenta Stripe: https://stripe.com
2. Obtener claves API de prueba desde el Dashboard
3. Agregar clave publicable a `local.properties`:
```properties
STRIPE_PUBLISHABLE_KEY=pk_test_51SkQhlAqdnCr1KIDe9wN7kt21QejQp5xxXdg1FZh7oVmlKiMZmK7o8dUvwrn5iUEhGLxB11jTxrNcjjN328ElOwX00jJnAr3zv
```

#### Configurar Stripe en el Dashboard

1. Habilitar Stripe en Supabase
2. Configurar webhooks para eventos de pago
3. Configurar moneda y métodos de pago

### 3. **Configuración de Firebase**

#### Crear Proyecto Firebase

1. Ir a: https://console.firebase.google.com
2. Crear nuevo proyecto
3. Agregar app Android con nombre de paquete: `com.dev.point_of_sale_sistem_with_kotlin_pos`
4. Descargar `google-services.json` y colocarlo en `app/`

#### Habilitar FCM

1. Habilitar Cloud Messaging en Firebase Console
2. Configurar canal de notificaciones
3. Configurar suscripciones a temas para sincronización multi-dispositivo

### 4. **Configuración de Cámara**

La aplicación usa CameraX para escaneo de códigos de barras. Asegurar estos permisos en `AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" android:required="false" />
```

### 5. **Configuración de Seguridad**

#### Configuración Biométrica
```kotlin
val biometricManager = BiometricManager.from(context)
val canAuthenticate = biometricManager.canAuthenticate(
    BiometricManager.Authenticators.BIOMETRIC_WEAK or 
    BiometricManager.Authenticators.DEVICE_CREDENTIAL
)
```

#### Almacenamiento Encriptado
```kotlin
implementation("androidx.security:security-crypto:1.1.0-alpha06")
```

### 6. **Configuración de Compilación**

#### Configuración de Versión

Editar `app/build.gradle.kts`:
```kotlin
android {
    defaultConfig {
        applicationId = "com.dev.point_of_sale_sistem_with_kotlin_pos"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }
}
```

#### Reglas ProGuard

Agregar a `app/proguard-rules.pro`:
```proguard
# Supabase
-keep class io.github.jan.supabase.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Stripe
-keep class com.stripe.** { *; }
```

### 7. **Configuración de Red**

#### Seguridad de Red

Permitir cleartext para desarrollo en `AndroidManifest.xml`:
```xml
<application
    android:usesCleartextTraffic="true">
</application>
```

Para producción, usar solo HTTPS.

#### Monitor de Red

La aplicación incluye detección automática de red:
```kotlin
val networkMonitor = NetworkMonitor(context)
val isOnline = networkMonitor.isOnline // StateFlow
```

### 8. **Configuración de Sincronización**

#### Configuración de Sincronización Automática

Configurar comportamiento de sincronización en `SyncManager.kt`:
```kotlin
class SyncManager {
    companion object {
        const val SYNC_INTERVAL = 15 * 60 * 1000L // 15 minutos
        const val MAX_RETRIES = 3
    }
}
```

#### Activar Sincronización Manual
```kotlin
syncManager.triggerSync()
```

---

## Pruebas

### Pruebas Unitarias
```bash
./gradlew test
```

### Pruebas Instrumentadas
```bash
./gradlew connectedAndroidTest
```

### Verificaciones de Lint
```bash
./gradlew lint
```

---

## Solución de Problemas

### Problemas Comunes

1. **Fallo en Sincronización de Gradle**
   - Limpiar proyecto: `./gradlew clean`
   - Invalidar cachés y reiniciar Android Studio

2. **Error de Conexión Supabase**
   - Verificar URL y clave en `local.properties`
   - Verificar conexión a internet
   - Verificar políticas RLS

3. **Fallo en Pago Stripe**
   - Verificar claves de modo de prueba Stripe
   - Verificar despliegue de Edge Function

4. **FCM No Funciona**
   - Verificar que `google-services.json` sea correcto
   - Verificar configuración del canal de notificaciones

### Registros

Ver registros con:
```bash
adb logcat | grep -E "Supabase|Stripe|POS"
```

---

## Licencia

Este proyecto está licenciado bajo la Licencia MIT - ver el archivo [LICENSE](LICENSE) para detalles.

---

## Soporte

Para soporte, por favor abre un issue en el repositorio o contacta al equipo de desarrollo.

---

## Registro de Cambios

### Versión 1.0.0
- Lanzamiento inicial
- Funcionalidad POS completa
- Soporte offline
- Integración de múltiples métodos de pago
- Autenticación biométrica

---

<div align="center">
  <p>Construido con ❤️ usando Kotlin, Jetpack Compose y Supabase</p>
</div>
