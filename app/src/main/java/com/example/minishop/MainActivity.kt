package com.example.minishop

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.minishop.data.LoginLog
import com.example.minishop.data.Result
import com.example.minishop.data.SignupUser
import com.example.minishop.data.SupabaseRepository
import com.example.minishop.ui.theme.*
import kotlinx.coroutines.launch

data class Product(
    val id: Int,
    val name: String,
    val price: Double,
    val category: String,
    val iconEmoji: String
)

enum class AppScreen {
    LOGIN,
    REGISTER,
    SHOP,
    USERS_LIST,
    LOGIN_LOGS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MiniShopTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp() {
    var currentScreen by remember { mutableStateOf(AppScreen.LOGIN) }
    var previousScreen by remember { mutableStateOf(AppScreen.LOGIN) }
    var loggedInUserLogin by remember { mutableStateOf("") }
    var cartCount by remember { mutableStateOf(0) }

    fun navigateTo(screen: AppScreen) {
        if (screen == AppScreen.USERS_LIST || screen == AppScreen.LOGIN_LOGS) {
            previousScreen = currentScreen
        }
        currentScreen = screen
    }

    BackHandler(
        enabled = currentScreen == AppScreen.USERS_LIST ||
                currentScreen == AppScreen.LOGIN_LOGS ||
                currentScreen == AppScreen.REGISTER
    ) {
        currentScreen = when (currentScreen) {
            AppScreen.REGISTER -> AppScreen.LOGIN
            else -> previousScreen
        }
    }

    when (currentScreen) {
        AppScreen.LOGIN -> {
            LoginScreen(
                onLoginSuccess = { login ->
                    loggedInUserLogin = login
                    currentScreen = AppScreen.SHOP
                },
                onNavigateToRegister = { currentScreen = AppScreen.REGISTER },
                onOpenUsers = { navigateTo(AppScreen.USERS_LIST) },
                onOpenLogs = { navigateTo(AppScreen.LOGIN_LOGS) }
            )
        }
        AppScreen.REGISTER -> {
            RegisterScreen(
                onRegisterSuccess = { currentScreen = AppScreen.LOGIN },
                onNavigateToLogin = { currentScreen = AppScreen.LOGIN },
                onOpenUsers = { navigateTo(AppScreen.USERS_LIST) },
                onOpenLogs = { navigateTo(AppScreen.LOGIN_LOGS) }
            )
        }
        AppScreen.SHOP -> {
            ShopHomeScreen(
                userLogin = loggedInUserLogin,
                cartCount = cartCount,
                onAddToCart = { cartCount++ },
                onLogout = {
                    loggedInUserLogin = ""
                    cartCount = 0
                    currentScreen = AppScreen.LOGIN
                },
                onOpenUsers = { navigateTo(AppScreen.USERS_LIST) },
                onOpenLogs = { navigateTo(AppScreen.LOGIN_LOGS) }
            )
        }
        AppScreen.USERS_LIST -> {
            UsersListScreen(onBack = { currentScreen = previousScreen })
        }
        AppScreen.LOGIN_LOGS -> {
            LoginLogsScreen(onBack = { currentScreen = previousScreen })
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. SIMPLE LOGIN SCREEN
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit,
    onNavigateToRegister: () -> Unit,
    onOpenUsers: () -> Unit,
    onOpenLogs: () -> Unit
) {
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Velora Shop", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ShopPrimary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = onOpenUsers) {
                        Icon(Icons.Default.Group, contentDescription = "Foydalanuvchilar")
                    }
                    IconButton(onClick = onOpenLogs) {
                        Icon(Icons.Default.History, contentDescription = "Kirishlar tarixi")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Kirish",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tizimga kirish uchun login va parolni kiriting",
                fontSize = 14.sp,
                color = ShopTextMuted,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            OutlinedTextField(
                value = login,
                onValueChange = { login = it; errorMessage = null },
                label = { Text("Login") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; errorMessage = null },
                label = { Text("Parol") },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (isLoading) return@Button
                    if (login.isBlank() || password.isBlank()) {
                        errorMessage = "Login va parolni kiriting!"
                        return@Button
                    }
                    isLoading = true
                    errorMessage = null
                    scope.launch {
                        val res = SupabaseRepository.login(login.trim(), password)
                        isLoading = false
                        when (res) {
                            is Result.Success -> {
                                Toast.makeText(context, "Muvaffaqiyatli kirildi!", Toast.LENGTH_SHORT).show()
                                onLoginSuccess(login.trim())
                            }
                            is Result.Error -> {
                                errorMessage = res.message
                            }
                        }
                    }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = ShopPrimary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text("Tizimga kirish", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Hisobingiz yo'qmi? ", fontSize = 14.sp, color = ShopTextMuted)
                Text(
                    text = "Ro'yxatdan o'tish",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = ShopPrimary,
                    modifier = Modifier.clickable { onNavigateToRegister() }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. SIMPLE REGISTER SCREEN
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onOpenUsers: () -> Unit,
    onOpenLogs: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ro'yxatdan o'tish", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateToLogin) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Ortga", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ShopPrimary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = onOpenUsers) {
                        Icon(Icons.Default.Group, contentDescription = "Foydalanuvchilar")
                    }
                    IconButton(onClick = onOpenLogs) {
                        Icon(Icons.Default.History, contentDescription = "Kirishlar tarixi")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Yangi hisob yaratish",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Ma'lumotlaringizni to'ldiring",
                fontSize = 14.sp,
                color = ShopTextMuted
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            OutlinedTextField(
                value = username,
                onValueChange = { username = it; errorMessage = null },
                label = { Text("Foydalanuvchi ismi") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = login,
                onValueChange = { login = it; errorMessage = null },
                label = { Text("Login") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; errorMessage = null },
                label = { Text("Parol") },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; errorMessage = null },
                label = { Text("Parolni tasdiqlash") },
                trailingIcon = {
                    IconButton(onClick = { confirmVisible = !confirmVisible }) {
                        Icon(
                            if (confirmVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null
                        )
                    }
                },
                visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (isLoading) return@Button
                    when {
                        username.isBlank() || login.isBlank() || password.isBlank() -> {
                            errorMessage = "Barcha maydonlarni to'ldiring!"
                            return@Button
                        }
                        password != confirmPassword -> {
                            errorMessage = "Kiritilgan ikkala parol mos kelmadi!"
                            return@Button
                        }
                    }
                    isLoading = true
                    errorMessage = null
                    scope.launch {
                        val res = SupabaseRepository.register(
                            username = username.trim(),
                            login = login.trim(),
                            pass = password
                        )
                        isLoading = false
                        when (res) {
                            is Result.Success -> {
                                Toast.makeText(context, "Ro'yxatdan muvaffaqiyatli o'tdingiz!", Toast.LENGTH_LONG).show()
                                onRegisterSuccess()
                            }
                            is Result.Error -> {
                                errorMessage = res.message
                            }
                        }
                    }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = ShopPrimary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text("Ro'yxatdan o'tish", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Profilingiz bormi? ", fontSize = 14.sp, color = ShopTextMuted)
                Text(
                    text = "Kirish",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = ShopPrimary,
                    modifier = Modifier.clickable { onNavigateToLogin() }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. USERS LIST SCREEN
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersListScreen(onBack: () -> Unit) {
    var users by remember { mutableStateOf<List<SignupUser>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun loadUsers() {
        isLoading = true
        errorMsg = null
        scope.launch {
            when (val res = SupabaseRepository.fetchRegisteredUsers()) {
                is Result.Success -> {
                    users = res.data
                    isLoading = false
                }
                is Result.Error -> {
                    errorMsg = res.message
                    isLoading = false
                }
            }
        }
    }

    LaunchedEffect(Unit) { loadUsers() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Foydalanuvchilar (signup)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Jami: ${users.size}", fontSize = 12.sp, color = Color.White.copy(0.85f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Ortga", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { loadUsers() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Yangilash", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ShopPrimary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = ShopPrimary
                    )
                }
                errorMsg != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = errorMsg ?: "Xatolik", color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { loadUsers() }) {
                            Text("Qayta urinish")
                        }
                    }
                }
                users.isEmpty() -> {
                    Text(
                        text = "Foydalanuvchilar mavjud emas",
                        modifier = Modifier.align(Alignment.Center),
                        color = ShopTextMuted
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(users) { user ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = user.username, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text(text = "Login: ${user.login}", fontSize = 14.sp, color = ShopTextMuted)
                                        if (user.createdAt.isNotEmpty()) {
                                            Text(
                                                text = "Sana: ${user.createdAt.take(19).replace('T', ' ')}",
                                                fontSize = 11.sp,
                                                color = ShopTextMuted
                                            )
                                        }
                                    }
                                    Text(
                                        text = "#${user.id}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ShopPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. LOGIN LOGS SCREEN
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginLogsScreen(onBack: () -> Unit) {
    var logs by remember { mutableStateOf<List<LoginLog>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun loadLogs() {
        isLoading = true
        errorMsg = null
        scope.launch {
            when (val res = SupabaseRepository.fetchLoginLogs()) {
                is Result.Success -> {
                    logs = res.data
                    isLoading = false
                }
                is Result.Error -> {
                    errorMsg = res.message
                    isLoading = false
                }
            }
        }
    }

    LaunchedEffect(Unit) { loadLogs() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Kirishlar tarixi (login)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Jami urinishlar: ${logs.size}", fontSize = 12.sp, color = Color.White.copy(0.85f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Ortga", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { loadLogs() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Yangilash", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ShopPrimary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = ShopPrimary
                    )
                }
                errorMsg != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = errorMsg ?: "Xatolik", color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { loadLogs() }) {
                            Text("Qayta urinish")
                        }
                    }
                }
                logs.isEmpty() -> {
                    Text(
                        text = "Kirish loglari mavjud emas",
                        modifier = Modifier.align(Alignment.Center),
                        color = ShopTextMuted
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(logs) { log ->
                            val isSuccess = log.status.equals("SUCCESS", ignoreCase = true)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                        contentDescription = null,
                                        tint = if (isSuccess) ShopSuccess else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = "Login: ${log.login}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(
                                            text = "Status: ${log.status}",
                                            fontSize = 13.sp,
                                            color = if (isSuccess) ShopSuccess else MaterialTheme.colorScheme.error
                                        )
                                        if (log.createdAt.isNotEmpty()) {
                                            Text(
                                                text = log.createdAt.take(19).replace('T', ' '),
                                                fontSize = 11.sp,
                                                color = ShopTextMuted
                                            )
                                        }
                                    }
                                    Text(
                                        text = "#${log.id}",
                                        fontSize = 13.sp,
                                        color = ShopTextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. SHOP HOME SCREEN
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopHomeScreen(
    userLogin: String,
    cartCount: Int,
    onAddToCart: () -> Unit,
    onLogout: () -> Unit,
    onOpenUsers: () -> Unit,
    onOpenLogs: () -> Unit
) {
    val sampleProducts = remember {
        listOf(
            Product(1, "Wireless Headphones", 59.99, "Electronics", "🎧"),
            Product(2, "Running Sneakers", 84.50, "Fashion", "👟"),
            Product(3, "Smart Watch Pro", 129.00, "Electronics", "⌚"),
            Product(4, "Denim Jacket", 49.99, "Fashion", "🧥"),
            Product(5, "Stainless Bottle", 19.99, "Fitness", "🍶"),
            Product(6, "Classic Backpack", 39.50, "Accessories", "🎒")
        )
    }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Velora Shop", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(userLogin, fontSize = 12.sp, color = Color.White.copy(0.85f))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ShopPrimary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = onOpenUsers) {
                        Icon(Icons.Default.Group, contentDescription = "Foydalanuvchilar")
                    }
                    IconButton(onClick = onOpenLogs) {
                        Icon(Icons.Default.History, contentDescription = "Kirishlar tarixi")
                    }
                    BadgedBox(
                        badge = {
                            if (cartCount > 0) Badge(containerColor = ShopSuccess) {
                                Text("$cartCount", color = Color.White)
                            }
                        },
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = "Savatcha", tint = Color.White)
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Chiqish", tint = Color.White)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Mahsulotlar", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(sampleProducts) { product ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = product.iconEmoji, fontSize = 40.sp)
                            }
                            Text(text = product.category.uppercase(), fontSize = 10.sp, color = ShopPrimary, fontWeight = FontWeight.Bold)
                            Text(text = product.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "\$${product.price}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                IconButton(
                                    onClick = {
                                        onAddToCart()
                                        Toast.makeText(context, "${product.name} qo'shildi!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(ShopPrimary)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
