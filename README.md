# Point of Sale System - Android POS Application

<div align="center">
  <img src="app/src/main/res/drawable/logo_img.jpeg" alt="POS System Logo" width="150" />

  <br/>
  <h1>Android Point of Sale System</h1>
  <p>A comprehensive, modern POS solution built with Kotlin and Jetpack Compose</p>
</div>

---

## Project Overview

This is a complete **Android Point of Sale (POS) System** built entirely in **Kotlin** using **Jetpack Compose** for the UI layer. The application features a modern architecture with offline-first capabilities, real-time synchronization, and multi-payment support.

### Key Features

- **Sales Management**: Create, manage, and track sales transactions
- **Inventory Management**: Product and category management with barcode scanning
- **Cash Register Operations**: Open/close cash drawers, track transactions
- **User & Role Management**: Complete authentication and authorization system
- **Offline Support**: Full offline functionality with automatic sync
- **Multiple Payment Methods**: Cash, Stripe card payments
- **Reporting & Analytics**: Sales reports and business insights
- **Refund & Credit Notes**: Handle returns and issue credit notes
- **Biometric Authentication**: Secure access with fingerprint/biometrics
- **Multi-Branch Support**: Manage multiple business locations

### Architecture

The application follows **Clean Architecture** principles with the following layers:

```
app/src/main/java/com/dev/point_of_sale_sistem_with_kotlin_pos/
├── core/                  # Core functionality and infrastructure
│   ├── capabilities/      # App capabilities and state management
│   ├── network/           # Network monitoring and Supabase client
│   └── sync/              # Synchronization logic
├── data/                  # Data layer
│   ├── local/             # Room database, DAOs, entities
│   ├── mappers/           # Data mappers
│   └── sales/             # Sales data sources
├── intents/               # Business logic intents (MVI pattern)
├── models/                # Domain models and states
├── repository/            # Repository implementations
├── security/              # Security utilities
├── ui/                    # Jetpack Compose UI
│   ├── screens/           # App screens
│   └── theme/             # Material3 theming
└── viewmodel/             # ViewModels
```

---

## Team Presentation

### Project Name
**Android Point of Sale System (POS)**

### Team Members

| Role | Name | Responsibility |
|------|------|----------------|
| Lead Developer | Development Team | Architecture, Core Systems |
| Backend Integration | Development Team | Supabase Integration |
| Mobile Developer | Development Team | Android Implementation |
| UI/UX Designer | Development Team | Compose Interface |

### Development Team Contact
- **Organization**: Development Team
- **Repository**: [GitHub Repository]
- **Support**: [Contact Email]

---

## Project Execution Steps

### Prerequisites

Before running the project, ensure you have the following installed:

1. **Android Studio** (Latest version recommended)
   - Download from: https://developer.android.com/studio
   - Version: Hedgehog (2023.1.1) or newer

2. **Java Development Kit (JDK) 17**
   - OpenJDK 17: https://adoptium.net/
   - Or use Android Studio's bundled JDK

3. **Android SDK**
   - Minimum SDK: 24 (Android 7.0)
   - Target SDK: 36
   - Compile SDK: 36

4. **Gradle**
   - Version: 8.4 or newer (included in gradle wrapper)

### Installation Steps

1. **Clone the Repository**
   ```bash
   git clone https://github.com/your-repository/Point-of-Sale-Sistem-with-kotlin-POS.git
   cd Point-of-Sale-Sistem-with-kotlin-POS
   ```

2. **Configure Local Properties**
   Create or edit `local.properties` in the project root:
   ```properties
   sdk.dir=C:\\Android\\SDK
   SUPABASE_URL=https://your-project.supabase.co
   SUPABASE_KEY=your-anon-key
   STRIPE_PUBLISHABLE_KEY=pk_test_your_key
   ```

3. **Open in Android Studio**
   - Launch Android Studio
   - Select "Open" and navigate to the project directory
   - Wait for Gradle sync to complete

4. **Build the Project**
   ```bash
   ./gradlew assembleDebug
   ```

5. **Run on Emulator or Device**
   - Connect an Android device or start an emulator
   - Click "Run" in Android Studio or use:
   ```bash
   ./gradlew installDebug
   ```

### Building for Release

```bash
./gradlew assembleRelease
```

To generate a signed APK:
1. Create a keystore file
2. Configure signing in `build.gradle.kts`
3. Build release variant

---

## Database Information

### Database Name

The application uses two database systems:

#### 1. **Supabase (PostgreSQL)**
- **Database**: PostgreSQL 15+ (hosted on Supabase)
- **Cloud URL**: `https://uhtlmanoxrfdefelpybs.supabase.co`
- **Connection**: Via Supabase REST API and PostgREST

#### 2. **Room Database (Local)**
- **Database Name**: `offline_pos_database`
- **Type**: SQLite (via Room)
- **Location**: Device internal storage
- **Tables**:
  - `offline_sales` - Sales transactions
  - `offline_sale_details` - Sale line items
  - `offline_cash_register_history` - Cash register operations
  - `offline_product_stock` - Product inventory
  - `sync_queue` - Synchronization queue

### Database Schema

#### Main Tables (Supabase)

| Table | Description |
|-------|-------------|
| `users` | User accounts with authentication |
| `roles` | User roles and permissions |
| `branches` | Business locations/branches |
| `categories` | Product categories |
| `products` | Product inventory |
| `suppliers` | Vendor management |
| `sales` | Sales transactions |
| `sale_details` | Individual sale items |
| `cash_register_history` | Cash drawer operations |
| `credit_notes` | Refund credits |
| `refunds` | Return transactions |

#### Entity Classes

```kotlin
// Core entities defined in:
app/src/main/java/com/dev/point_of_sale_sistem_with_kotlin_pos/data/local/entities/

- OfflineSaleEntity
- OfflineSaleDetailEntity
- OfflineCashRegisterHistoryEntity
- OfflineProductStockEntity
- SyncQueueEntity
```

---

### Authentication Flow

1. **Login Screen**: Email/password authentication via Supabase Auth
2. **Biometric Option**: Fingerprint/Face authentication (if enabled)
3. **Session Management**: JWT token-based sessions
4. **Auto-Logout**: Configurable timeout for security

---

## Dependencies

### Core Dependencies

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

#### Database
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

#### Payments
```kotlin
implementation("com.stripe:stripe-android:20.49.0")
```

#### Firebase
```kotlin
implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
implementation("com.google.firebase:firebase-messaging")
```

#### Camera & Scanning
```kotlin
implementation("androidx.camera:camera-camera2:1.4.0")
implementation("androidx.camera:camera-lifecycle:1.4.0")
implementation("androidx.camera:camera-view:1.4.0")
implementation("com.google.mlkit:barcode-scanning:17.3.0")
```

#### Coroutines & Serialization
```kotlin
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
```

#### Security
```kotlin
implementation("androidx.security:security-crypto:1.1.0-alpha06")
implementation("androidx.biometric:biometric:1.2.0-alpha05")
```

#### Network
```kotlin
implementation("io.ktor:ktor-client-core:3.3.1")
implementation("io.ktor:ktor-client-android:3.3.1")
```

### Full Dependency List

See `app/build.gradle.kts` for the complete list of dependencies.

### Dependency Versions

| Dependency | Version | Purpose |
|------------|---------|---------|
| Kotlin | 1.9.24 | Language |
| Compose BOM | 2024.09.00 | UI Framework |
| Room | 2.6.1 | Local Database |
| Supabase | 3.2.4 | Backend Services |
| Stripe | 20.49.0 | Payment Processing |
| Firebase | 33.5.1 | Push Notifications |
| CameraX | 1.4.0 | Barcode Scanning |
| Ktor | 3.3.1 | HTTP Client |

---

## API Implementation Details

### 1. **Supabase Authentication API**

**File**: `repository/admin/users/UserRepository.kt`

```kotlin
class UserRepository(private val supabase: SupabaseClient) {
    
    // Login and session management
    suspend fun getCurrentUser(): UserModel?
    suspend fun getAllActiveUsers(): List<UserModel>
    suspend fun createUser(email, roleId, companyId, branchId): UserModel
    suspend fun updateUser(authId, branchId, roleId): UserModel
    suspend fun deleteUser(authId)
}
```

**Features**:
- JWT token-based authentication
- Session persistence with `AndroidSessionManager`
- Role-based access control
- Soft delete support

### 2. **Supabase Database API (PostgREST)**

**Files**:
- `repository/admin/products/ProductRepository.kt`
- `repository/admin/categories/CategoryRepository.kt`
- `repository/admin/branches/BranchRepository.kt`

```kotlin
class ProductRepository(private val supabase: SupabaseClient) {
    
    suspend fun getAllProducts(): List<Product>
    suspend fun getProductById(id: Int): Product?
    suspend fun createProduct(product: Product): Product
    suspend fun updateProduct(id: Int, product: Product)
    suspend fun deleteProduct(id: Int)
    suspend fun searchProducts(query: String): List<Product>
}
```

**Operations**:
- CRUD operations via PostgREST
- Filtering and sorting
- Real-time subscriptions
- Batch operations

### 3. **Stripe Payment API**

**File**: `repository/sales/sales_orders/StripePaymentRepository.kt`

```kotlin
class StripePaymentRepository(
    private val context: Context,
    private val publishableKey: String,
    private val supabase: SupabaseClient
) {
    suspend fun createPaymentIntent(amount, currency, saleId): Result<PaymentIntentResponse>
    suspend fun confirmPayment(paymentIntentId): Result<StripePaymentConfirmation>
}
```

**Flow**:
1. Create PaymentIntent via Supabase Edge Function
2. Present Stripe payment sheet
3. Confirm payment and verify status
4. Record payment proof in database

### 4. **Firebase Cloud Messaging API**

**File**: `FcmService.kt`

```kotlin
class FcmService : FirebaseMessagingService() {
    override fun onNewToken(token: String)
    override fun onMessageReceived(message: RemoteMessage)
}
```

**Features**:
- Push notifications for sync updates
- Real-time sale notifications
- System-wide alerts

### 5. **Local Database API (Room)**

**Files**:
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
    suspend fun getPendingSalesWithDetails(): List<OfflineSaleWithDetails>
    
    @Query("UPDATE offline_sales SET pendingSync = 0 WHERE localSaleId = :localSaleId")
    suspend fun markAsSynced(localSaleId: String)
}
```

**Features**:
- Offline-first sales recording
- Automatic sync queue management
- Conflict resolution
- Local caching

### 6. **Edge Functions API**

**Supabase Edge Functions**:
- `create-stripe-payment-intent` - Create Stripe payments
- `confirm-stripe-payment` - Confirm payments
- `sync-data` - Synchronize offline data

### 7. **Sales Management API**

**File**: `repository/sales/sales_orders/SalesRepository.kt`

```kotlin
class SalesRepository(private val supabase: SupabaseClient) {
    suspend fun createSale(sale: SaleRequest): SaleResponse
    suspend fun getSaleById(id: String): Sale?
    suspend fun getSalesByDateRange(start, end): List<Sale>
    suspend fun cancelSale(id: String)
    suspend fun refundSale(id: String, amount: Double)
}
```

### 8. **Cash Register API**

**File**: `repository/sales/cash_register/CashRegisterRepository.kt`

```kotlin
class CashRegisterRepository(private val supabase: SupabaseClient) {
    suspend fun openRegister(amount: Double): CashRegisterSession
    suspend fun closeRegister(sessionId: String, finalAmount: Double)
    suspend fun getRegisterHistory(branchId): List<CashRegisterHistory>
    suspend fun addTransaction(sessionId, transaction: Transaction)
}
```

### 9. **Report Generation API**

**File**: `repository/sales/reports/SalesReportRepository.kt`

```kotlin
class SalesReportRepository(private val supabase: SupabaseClient) {
    suspend fun getDailySalesReport(date: LocalDate): SalesReport
    suspend fun getMonthlySalesReport(year: Int, month: Int): MonthlyReport
    suspend fun getTopProducts(limit: Int): List<TopProduct>
    suspend fun getSalesByCategory(): Map<Category, Double>
}
```

---

## Configuration Guide

### 1. **Supabase Configuration**

#### Environment Variables (local.properties)

```properties
# Supabase Configuration
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_KEY=your-anon-key
```

#### Database Setup

1. **Create Supabase Project**
   - Go to: https://supabase.com
   - Create new project

2. **Run Database Migrations**
   Execute SQL scripts in Supabase SQL Editor:
   ```sql
   -- Enable Row Level Security
   alter table users enable row level security;
   
   -- Create tables (see database schema)
   ```

3. **Configure RLS Policies**
   ```sql
   create policy "Users can view own data"
   on users for select
   using (auth.uid() = auth_id);
   ```

#### Edge Functions Setup

Deploy Supabase Edge Functions:
```bash
supabase functions new create-stripe-payment-intent
supabase functions new confirm-stripe-payment
```

### 2. **Stripe Configuration**

#### Get Stripe Keys

1. Create Stripe account: https://stripe.com
2. Get test API keys from Dashboard
3. Add publishable key to `local.properties`:
   ```properties
   STRIPE_PUBLISHABLE_KEY=pk_test_your_key
   ```

#### Configure Stripe in Dashboard

1. Enable Stripe in Supabase
2. Configure webhooks for payment events
3. Set up currency and payment methods

### 3. **Firebase Configuration**

#### Create Firebase Project

1. Go to: https://console.firebase.google.com
2. Create new project
3. Add Android app with package name: `com.dev.point_of_sale_sistem_with_kotlin_pos`
4. Download `google-services.json` and place in `app/`

#### Enable FCM

1. Enable Cloud Messaging in Firebase Console
2. Configure notification channel
3. Set up topic subscriptions for multi-device sync

### 4. **Camera Configuration**

The app uses CameraX for barcode scanning. Ensure these permissions in `AndroidManifest.xml`:

```xml
<uses-feature android:name="android.hardware.camera" android:required="false" />
<uses-permission android:name="android.permission.CAMERA" />
```

### 5. **Security Configuration**

#### Biometric Setup

```kotlin
val biometricManager = BiometricManager.from(context)
val canAuthenticate = biometricManager.canAuthenticate(
    BiometricManager.Authenticators.BIOMETRIC_WEAK or
    BiometricManager.Authenticators.DEVICE_CREDENTIAL
)
```

#### Encrypted Storage

```kotlin
implementation("androidx.security:security-crypto:1.1.0-alpha06")
```

### 6. **Build Configuration**

#### Version Configuration

Edit `app/build.gradle.kts`:

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

#### ProGuard Rules

Add to `app/proguard-rules.pro`:

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

### 7. **Network Configuration**

#### Network Security

Allow cleartext for development in `AndroidManifest.xml`:

```xml
<application
    android:usesCleartextTraffic="true"
    >
</application>
```

For production, use HTTPS only.

#### Network Monitor

The app includes automatic network detection:

```kotlin
val networkMonitor = NetworkMonitor(context)
val isOnline = networkMonitor.isOnline // StateFlow<Boolean>
```

### 8. **Sync Configuration**

#### Automatic Sync Settings

Configure sync behavior in `SyncManager.kt`:

```kotlin
class SyncManager {
    companion object {
        const val SYNC_INTERVAL = 15 * 60 * 1000L // 15 minutes
        const val MAX_RETRIES = 3
    }
}
```

#### Manual Sync Trigger

```kotlin
syncManager.triggerSync()
```

---

## Testing

### Unit Tests

```bash
./gradlew test
```

### Instrumented Tests

```bash
./gradlew connectedAndroidTest
```

### Lint Checks

```bash
./gradlew lint
```

---

## Troubleshooting

### Common Issues

1. **Gradle Sync Failed**
   - Clean project: `./gradlew clean`
   - Invalidate caches and restart Android Studio

2. **Supabase Connection Error**
   - Verify URL and key in `local.properties`
   - Check internet connection
   - Verify RLS policies

3. **Stripe Payment Failed**
   - Check Stripe test mode keys
   - Verify Edge Function deployment

4. **FCM Not Working**
   - Verify `google-services.json` is correct
   - Check notification channel configuration

### Logs

View logs with:
```bash
adb logcat | grep -E "Supabase|Stripe|POS"
```

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## Support

For support, please open an issue in the repository or contact the development team.

---

## Changelog

### Version 1.0.0
- Initial release
- Complete POS functionality
- Offline support
- Multi-payment integration
- Biometric authentication

---

<div align="center">
  <p>Built with ❤️ using Kotlin, Jetpack Compose, and Supabase</p>
</div>
