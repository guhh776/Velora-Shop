package com.example.minishop

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
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

// ─── Models ───────────────────────────────────────────────────────────────────
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

// ─── Entry Point ──────────────────────────────────────────────────────────────
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

    BackHandler(
        enabled = currentScreen == AppScreen.USERS_LIST ||
                  currentScreen == AppScreen.LOGIN_LOGS ||
                  currentScreen == AppScreen.REGISTER
    ) {
        currentScreen = when (currentScreen) {
            AppScreen.REGISTER  -> AppScreen.LOGIN
            else                -> previousScreen
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
                onOpenUsers = {
                    previousScreen = AppScreen.LOGIN
                    currentScreen = AppScreen.USERS_LIST
                },
                onOpenLogs = {
                    previousScreen = AppScreen.LOGIN
                    currentScreen = AppScreen.LOGIN_LOGS
                }
            )
        }
        AppScreen.REGISTER -> {
            RegisterScreen(
                onRegisterSuccess = { currentScreen = AppScreen.LOGIN },
                onNavigateToLogin = { currentScreen = AppScreen.LOGIN },
                onOpenUsers = {
                    previousScreen = AppScreen.REGISTER
                    currentScreen = AppScreen.USERS_LIST
                },
                onOpenLogs = {
                    previousScreen = AppScreen.REGISTER
                    currentScreen = AppScreen.LOGIN_LOGS
                }
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
                onOpenUsers = {
                    previousScreen = AppScreen.SHOP
                    currentScreen = AppScreen.USERS_LIST
                },
                onOpenLogs = {
                    previousScreen = AppScreen.SHOP
                    currentScreen = AppScreen.LOGIN_LOGS
                }
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
// 1. LOGIN SCREEN
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
                title = {
                    Text(
                        text = "Velora Shop",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ShopPrimary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = onOpenUsers) {
                        Icon(Icons.Default.Group, contentDescription = "Foydalanuvchilar ro'yxati")
                    }
                    IconButton(onClick = onOpenLogs) {
                        Icon(Icons.Default.History, contentDescription = "Kirishlar tarixi")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(ShopBackground, Color(0xFFFFE0D6))
                    )
                )
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(Modifier.height(32.dp))

                // ─── Logo ───────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(ShopPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    text = "Tizimga Kirish",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = ShopTextDark
                )
                Text(
                    text = "Hisobingizga kiring",
                    fontSize = 14.sp,
                    color = ShopTextMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(Modifier.height(32.dp))

                // ─── Card ────────────────────────────────────────────────────
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Error banner
                        errorMessage?.let {
                            Surface(
                                color = Color(0xFFFFEBEB),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Error,
                                        contentDescription = null,
                                        tint = Color(0xFFD32F2F),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = it,
                                        color = Color(0xFFD32F2F),
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        // Login field
                        OutlinedTextField(
                            value = login,
                            onValueChange = { login = it; errorMessage = null },
                            label = { Text("Login") },
                            leadingIcon = {
                                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = ShopPrimary)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ShopPrimary,
                                focusedLabelColor = ShopPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Password field
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it; errorMessage = null },
                            label = { Text("Parol") },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = ShopPrimary)
                            },
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
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ShopPrimary,
                                focusedLabelColor = ShopPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Login Button
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
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, tint = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text("Tizimga Kirish", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ─── Navigate to Register ─────────────────────────────────────
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

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. REGISTER SCREEN
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
                title = {
                    Text("Velora Shop", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                },
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
                        Icon(Icons.Default.Group, contentDescription = "Foydalanuvchilar ro'yxati")
                    }
                    IconButton(onClick = onOpenLogs) {
                        Icon(Icons.Default.History, contentDescription = "Kirishlar tarixi")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(ShopBackground, Color(0xFFFFE0D6))
                    )
                )
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(28.dp))

                // ─── Logo ───────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(ShopPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Ro'yxatdan O'tish",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = ShopTextDark
                )
                Text(
                    text = "Yangi hisob yarating",
                    fontSize = 14.sp,
                    color = ShopTextMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(Modifier.height(24.dp))

                // ─── Card ────────────────────────────────────────────────────
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Error banner
                        errorMessage?.let {
                            Surface(
                                color = Color(0xFFFFEBEB),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Error,
                                        contentDescription = null,
                                        tint = Color(0xFFD32F2F),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(text = it, color = Color(0xFFD32F2F), fontSize = 13.sp)
                                }
                            }
                        }

                        // Username field
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it; errorMessage = null },
                            label = { Text("Foydalanuvchi ismi") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = ShopPrimary)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ShopPrimary,
                                focusedLabelColor = ShopPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Login field
                        OutlinedTextField(
                            value = login,
                            onValueChange = { login = it; errorMessage = null },
                            label = { Text("Login (unikal)") },
                            leadingIcon = {
                                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = ShopPrimary)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ShopPrimary,
                                focusedLabelColor = ShopPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Password field
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it; errorMessage = null },
                            label = { Text("Parol") },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = ShopPrimary)
                            },
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
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ShopPrimary,
                                focusedLabelColor = ShopPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Confirm Password field
                        val passwordsMatch = confirmPassword.isEmpty() || password == confirmPassword
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it; errorMessage = null },
                            label = { Text("Parolni tasdiqlash") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.LockReset,
                                    contentDescription = null,
                                    tint = if (passwordsMatch) ShopPrimary else Color(0xFFD32F2F)
                                )
                            },
                            trailingIcon = {
                                if (!passwordsMatch) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD32F2F))
                                } else {
                                    IconButton(onClick = { confirmVisible = !confirmVisible }) {
                                        Icon(
                                            if (confirmVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null
                                        )
                                    }
                                }
                            },
                            visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            isError = !passwordsMatch,
                            supportingText = {
                                if (!passwordsMatch) {
                                    Text("Parollar mos kelmadi!", color = Color(0xFFD32F2F), fontSize = 12.sp)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ShopPrimary,
                                focusedLabelColor = ShopPrimary,
                                errorBorderColor = Color(0xFFD32F2F)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Register Button
                        Button(
                            onClick = {
                                if (isLoading) return@Button
                                when {
                                    username.isBlank() || login.isBlank() || password.isBlank() -> {
                                        errorMessage = "Barcha maydonlarni to'ldiring!"
                                        return@Button
                                    }
                                    password != confirmPassword -> {
                                        errorMessage = "Parollar mos kelmadi!"
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
                                            Toast.makeText(context, "Ro'yxatdan o'tdingiz! Endi kiring.", Toast.LENGTH_LONG).show()
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
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text("Ro'yxatdan O'tish", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ─── Navigate to Login ────────────────────────────────────────
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Hisobingiz bormi? ", fontSize = 14.sp, color = ShopTextMuted)
                    Text(
                        text = "Kirish",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ShopPrimary,
                        modifier = Modifier.clickable { onNavigateToLogin() }
                    )
                }

                Spacer(Modifier.height(32.dp))
            }
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
        isLoading = true; errorMsg = null
        scope.launch {
            when (val res = SupabaseRepository.fetchRegisteredUsers()) {
                is Result.Success -> { users = res.data; isLoading = false }
                is Result.Error   -> { errorMsg = res.message; isLoading = false }
            }
        }
    }

    LaunchedEffect(Unit) { loadUsers() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Foydalanuvchilar", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                        Text("signup jadvali • ${users.size} ta", fontSize = 11.sp, color = Color.White.copy(0.8f))
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ShopPrimary, titleContentColor = Color.White)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ShopBackground)
                .padding(padding)
        ) {
            when {
                isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ShopPrimary)
                }
                errorMsg != null -> Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.WifiOff, contentDescription = null, tint = ShopTextMuted, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(errorMsg ?: "Xatolik", color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { loadUsers() }, colors = ButtonDefaults.buttonColors(containerColor = ShopPrimary)) {
                        Text("Qayta urinish")
                    }
                }
                users.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Group, contentDescription = null, tint = ShopTextMuted, modifier = Modifier.size(56.dp))
                        Spacer(Modifier.height(10.dp))
                        Text("Hech qanday foydalanuvchi yo'q", color = ShopTextMuted)
                    }
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(users) { user ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(1.dp),
                            modifier = Modifier.fillMaxWidth().border(1.dp, ShopCardBorder, RoundedCornerShape(14.dp))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(46.dp).clip(CircleShape).background(ShopPrimary.copy(0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        user.username.take(1).uppercase().ifEmpty { "?" },
                                        fontWeight = FontWeight.Bold, fontSize = 20.sp, color = ShopPrimary
                                    )
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(user.username, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ShopTextDark)
                                    Text("@${user.login}", fontSize = 13.sp, color = ShopTextMuted)
                                    if (user.createdAt.isNotEmpty()) {
                                        Text(
                                            "Ro'yxat: ${user.createdAt.take(10)}",
                                            fontSize = 11.sp, color = ShopTextMuted.copy(0.7f)
                                        )
                                    }
                                }
                                Surface(
                                    color = ShopPrimary.copy(0.1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("#${user.id}", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                        color = ShopPrimary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
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
        isLoading = true; errorMsg = null
        scope.launch {
            when (val res = SupabaseRepository.fetchLoginLogs()) {
                is Result.Success -> { logs = res.data; isLoading = false }
                is Result.Error   -> { errorMsg = res.message; isLoading = false }
            }
        }
    }

    LaunchedEffect(Unit) { loadLogs() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Kirishlar Tarixi", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                        Text("login jadvali • ${logs.size} ta urinish", fontSize = 11.sp, color = Color.White.copy(0.8f))
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ShopPrimary, titleContentColor = Color.White)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ShopBackground)
                .padding(padding)
        ) {
            when {
                isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ShopPrimary)
                }
                errorMsg != null -> Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.WifiOff, contentDescription = null, tint = ShopTextMuted, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(errorMsg ?: "Xatolik", color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { loadLogs() }, colors = ButtonDefaults.buttonColors(containerColor = ShopPrimary)) {
                        Text("Qayta urinish")
                    }
                }
                logs.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.History, contentDescription = null, tint = ShopTextMuted, modifier = Modifier.size(56.dp))
                        Spacer(Modifier.height(10.dp))
                        Text("Hech qanday kirish logi yo'q", color = ShopTextMuted)
                    }
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(logs) { log ->
                        val isSuccess = log.status.equals("SUCCESS", ignoreCase = true)
                        val statusColor = if (isSuccess) ShopSuccess else Color(0xFFD32F2F)
                        val bgColor = if (isSuccess) ShopSuccess.copy(0.08f) else Color(0xFFD32F2F).copy(0.08f)

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(1.dp),
                            modifier = Modifier.fillMaxWidth().border(1.dp, ShopCardBorder, RoundedCornerShape(14.dp))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(44.dp).clip(CircleShape).background(bgColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                        contentDescription = null,
                                        tint = statusColor,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("@${log.login}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ShopTextDark)
                                    Surface(color = bgColor, shape = RoundedCornerShape(6.dp)) {
                                        Text(
                                            log.status, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                            color = statusColor,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                    if (log.createdAt.isNotEmpty()) {
                                        Text(
                                            log.createdAt.take(19).replace('T', ' '),
                                            fontSize = 11.sp, color = ShopTextMuted.copy(0.7f)
                                        )
                                    }
                                }
                                Surface(
                                    color = ShopPrimary.copy(0.1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("#${log.id}", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                        color = ShopPrimary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
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
                        Text("Velora Shop", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                        Text("@$userLogin", fontSize = 11.sp, color = Color.White.copy(0.8f))
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
                        Icon(Icons.Default.ShoppingCart, contentDescription = "Cart", tint = Color.White)
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
                .background(ShopBackground)
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text("Featured Products", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ShopTextDark)
            Text("Explore our top picks for you", fontSize = 12.sp, color = ShopTextMuted)
            Spacer(Modifier.height(12.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(sampleProducts) { product ->
                    ProductGridCard(product = product, onAdd = {
                        onAddToCart()
                        Toast.makeText(context, "${product.name} savatchaga qo'shildi!", Toast.LENGTH_SHORT).show()
                    })
                }
            }
        }
    }
}

@Composable
fun ProductGridCard(product: Product, onAdd: () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, ShopCardBorder, RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Box(
                modifier = Modifier.fillMaxWidth().height(90.dp).clip(RoundedCornerShape(10.dp)).background(ShopBackground),
                contentAlignment = Alignment.Center
            ) {
                Text(text = product.iconEmoji, fontSize = 42.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(product.category.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ShopPrimary)
            Text(product.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ShopTextDark, maxLines = 1)
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("\$${product.price}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ShopTextDark)
                IconButton(
                    onClick = onAdd,
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(ShopPrimary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
