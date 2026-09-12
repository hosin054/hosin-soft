package com.example.ui.screens.users

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.User
import com.example.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val users by viewModel.allUsers.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedRoleFilter by remember { mutableStateOf("ALL") }

    var showAddEditDialog by remember { mutableStateOf(false) }
    var userToEdit by remember { mutableStateOf<User?>(null) }

    var showSwitchUserDialog by remember { mutableStateOf(false) }
    var userToSwitchTo by remember { mutableStateOf<User?>(null) }
    var switchPinInput by remember { mutableStateOf("") }
    var switchErrorMessage by remember { mutableStateOf<String?>(null) }

    var userToDelete by remember { mutableStateOf<User?>(null) }

    // Filter users
    val filteredUsers = users.filter { user ->
        val matchesSearch = user.fullName.contains(searchQuery, ignoreCase = true) ||
                user.username.contains(searchQuery, ignoreCase = true)
        val matchesRole = when (selectedRoleFilter) {
            "ADMIN" -> user.role == "ADMIN"
            "CASHIER" -> user.role == "CASHIER"
            "ACCOUNTANT" -> user.role == "ACCOUNTANT"
            "INVENTORY_MANAGER" -> user.role == "INVENTORY_MANAGER"
            "CUSTOM" -> user.role == "CUSTOM"
            else -> true
        }
        matchesSearch && matchesRole
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = "إدارة المستخدمين والموظفين", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(
                            text = "تحديد الصلاحيات وتبديل الحسابات",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        switchErrorMessage = null
                        switchPinInput = ""
                        showSwitchUserDialog = true
                    }) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = "تبديل المستخدم", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            if (currentUser.canManageUsers) {
                ExtendedFloatingActionButton(
                    onClick = {
                        userToEdit = null
                        showAddEditDialog = true
                    },
                    icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                    text = { Text("إضافة موظف جديد", fontWeight = FontWeight.Bold) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Current Logged-in User Banner
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentUser.fullName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                RoleBadge(role = currentUser.role, viewModel = viewModel)
                            }
                            Text(
                                text = "اسم الدخول: @${currentUser.username} • الحساب النشط حالياً في النظام",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                switchErrorMessage = null
                                switchPinInput = ""
                                showSwitchUserDialog = true
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تبديل", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Notice if user cannot manage users
            if (!currentUser.canManageUsers) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "حسابك الحالي ليس لديه صلاحية تعديل صلاحيات المستخدمين، يمكنك فقط تبديل الحساب.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // Search Bar & Filter chips
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("بحث باسم الموظف أو اسم الدخول...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "مسح")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedRoleFilter == "ALL",
                            onClick = { selectedRoleFilter = "ALL" },
                            label = { Text("الكل (${users.size})") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedRoleFilter == "ADMIN",
                            onClick = { selectedRoleFilter = "ADMIN" },
                            label = { Text("المدراء (${users.count { it.role == "ADMIN" }})") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedRoleFilter == "CASHIER",
                            onClick = { selectedRoleFilter = "CASHIER" },
                            label = { Text("الكاشير (${users.count { it.role == "CASHIER" }})") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedRoleFilter == "ACCOUNTANT",
                            onClick = { selectedRoleFilter = "ACCOUNTANT" },
                            label = { Text("المحاسبين (${users.count { it.role == "ACCOUNTANT" }})") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedRoleFilter == "INVENTORY_MANAGER",
                            onClick = { selectedRoleFilter = "INVENTORY_MANAGER" },
                            label = { Text("أمين المخزن (${users.count { it.role == "INVENTORY_MANAGER" }})") }
                        )
                    }
                }
            }

            // List of Users
            if (filteredUsers.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا توجد نتائج مطابقة",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filteredUsers, key = { it.id }) { user ->
                    UserCard(
                        user = user,
                        isCurrent = user.id == currentUser.id,
                        canManage = currentUser.canManageUsers,
                        onSwitchTo = {
                            if (user.id == currentUser.id) {
                                viewModel.showMessage("أنت بالفعل تستخدم هذا الحساب")
                            } else if (user.passwordHash.isBlank()) {
                                viewModel.switchUser(user, "") { _, _ -> }
                            } else {
                                userToSwitchTo = user
                                switchPinInput = ""
                                switchErrorMessage = null
                            }
                        },
                        onEdit = {
                            userToEdit = user
                            showAddEditDialog = true
                        },
                        onDelete = {
                            userToDelete = user
                        },
                        viewModel = viewModel
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(70.dp))
            }
        }
    }

    // --- Switch User PIN Dialog ---
    if (userToSwitchTo != null) {
        val target = userToSwitchTo!!
        AlertDialog(
            onDismissRequest = { userToSwitchTo = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تسجيل الدخول: ${target.fullName}")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "أدخل الرمز السري / كلمة المرور الخاصة بحساب ${target.fullName} (${viewModel.getRoleArabicName(target.role)}):",
                        fontSize = 13.sp
                    )

                    OutlinedTextField(
                        value = switchPinInput,
                        onValueChange = {
                            switchPinInput = it
                            switchErrorMessage = null
                        },
                        label = { Text("الرمز السري") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (switchErrorMessage != null) {
                        Text(
                            text = switchErrorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.switchUser(target, switchPinInput.trim()) { success, msg ->
                            if (success) {
                                userToSwitchTo = null
                            } else {
                                switchErrorMessage = msg
                            }
                        }
                    }
                ) {
                    Text("دخول")
                }
            },
            dismissButton = {
                TextButton(onClick = { userToSwitchTo = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // --- Switch User Picker List Dialog ---
    if (showSwitchUserDialog) {
        AlertDialog(
            onDismissRequest = { showSwitchUserDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.People, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("اختر حساب الموظف للدخول")
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    users.forEach { user ->
                        val isSelected = user.id == currentUser.id
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showSwitchUserDialog = false
                                    if (isSelected) {
                                        viewModel.showMessage("أنت بالفعل على حساب ${user.fullName}")
                                    } else if (user.passwordHash.isBlank()) {
                                        viewModel.switchUser(user, "") { _, _ -> }
                                    } else {
                                        userToSwitchTo = user
                                        switchPinInput = ""
                                        switchErrorMessage = null
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (isSelected) Icons.Default.CheckCircle else Icons.Default.Person,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = user.fullName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        text = "${viewModel.getRoleArabicName(user.role)} • @${user.username}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (user.passwordHash.isNotBlank()) {
                                    Icon(Icons.Default.Lock, contentDescription = "محمي", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSwitchUserDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }

    // --- Delete Confirmation Dialog ---
    if (userToDelete != null) {
        val target = userToDelete!!
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = { Text("تأكيد حذف الحساب") },
            text = {
                Text("هل أنت متأكد من رغبتك في حذف حساب الموظف \"${target.fullName}\" (@${target.username})؟ لن يتمكن من تسجيل الدخول للنظام بعد الحذف.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteUser(target) { success, _ ->
                            if (success) {
                                userToDelete = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف نهائي")
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // --- Add/Edit User Dialog ---
    if (showAddEditDialog) {
        AddEditUserDialog(
            userToEdit = userToEdit,
            onDismiss = { showAddEditDialog = false },
            onSave = { userToSave ->
                viewModel.saveUser(userToSave) {
                    showAddEditDialog = false
                }
            },
            viewModel = viewModel
        )
    }
}

@Composable
fun UserCard(
    user: User,
    isCurrent: Boolean,
    canManage: Boolean,
    onSwitchTo: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    viewModel: MainViewModel
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(if (isCurrent) 2.dp else 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Avatar, Name, Role, Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = getRoleColor(user.role).copy(alpha = 0.15f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            getRoleIcon(user.role),
                            contentDescription = null,
                            tint = getRoleColor(user.role),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = user.fullName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        if (isCurrent) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(2.dp)
                            ) {
                                Text(
                                    text = "الحالي",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "@${user.username}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(text = "•", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        RoleBadge(role = user.role, viewModel = viewModel)
                        if (user.passwordHash.isNotBlank()) {
                            Icon(Icons.Default.Lock, contentDescription = "محمي برقم سري", modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Active status badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (user.isActive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                ) {
                    Text(
                        text = if (user.isActive) "نشط" else "معطل",
                        fontSize = 10.sp,
                        color = if (user.isActive) Color(0xFF2E7D32) else Color(0xFFC62828),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Permissions Tags / Summary
            Text(text = "الصلاحيات الممنوحة:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRowPermissions(user = user)

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isCurrent) {
                    OutlinedButton(
                        onClick = onSwitchTo,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("دخول بهذا الحساب", fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (canManage) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "تعديل الصلاحيات", tint = MaterialTheme.colorScheme.primary)
                    }

                    if (!isCurrent) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRowPermissions(user: User) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (user.canSell) PermissionChip(label = "🛒 بيع / POS", granted = true)
        if (user.canGiveDiscount) PermissionChip(label = "🏷️ خصومات", granted = true)
        if (user.canPurchase) PermissionChip(label = "📦 مشتريات وتوريد", granted = true)
        if (user.canViewProfits) PermissionChip(label = "💰 رؤية الأرباح والتكلفة", granted = true)
        if (user.canViewReports) PermissionChip(label = "📊 تقارير مالية", granted = true)
        if (user.canManageInventory) PermissionChip(label = "🏬 إدارة المخزون", granted = true)
        if (user.canManageCustomers) PermissionChip(label = "👥 إدارة العملاء", granted = true)
        if (user.canManageSuppliers) PermissionChip(label = "🚚 إدارة الموردين", granted = true)
        if (user.canManageExpenses) PermissionChip(label = "💸 المصروفات", granted = true)
        if (user.canManageSettings) PermissionChip(label = "⚙️ إعدادات النظام", granted = true)
        if (user.canManageUsers) PermissionChip(label = "🛡️ إدارة الموظفين", granted = true)

        if (!user.canSell && !user.canPurchase && !user.canViewReports && !user.canManageInventory) {
            PermissionChip(label = "لا توجد صلاحيات مفعلة", granted = false)
        }
    }
}

@Composable
fun PermissionChip(label: String, granted: Boolean) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (granted) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFFFEBEE),
        modifier = Modifier.padding(vertical = 1.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = if (granted) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFFC62828),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun RoleBadge(role: String, viewModel: MainViewModel) {
    val color = getRoleColor(role)
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = viewModel.getRoleArabicName(role).split(" ").firstOrNull() ?: role,
            fontSize = 10.sp,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

fun getRoleColor(role: String): Color {
    return when (role) {
        "ADMIN" -> Color(0xFF673AB7) // Purple
        "CASHIER" -> Color(0xFF00897B) // Teal
        "ACCOUNTANT" -> Color(0xFF1976D2) // Blue
        "INVENTORY_MANAGER" -> Color(0xFFE65100) // Orange
        "CUSTOM" -> Color(0xFF3F51B5) // Indigo
        else -> Color(0xFF546E7A)
    }
}

fun getRoleIcon(role: String): ImageVector {
    return when (role) {
        "ADMIN" -> Icons.Default.AdminPanelSettings
        "CASHIER" -> Icons.Default.PointOfSale
        "ACCOUNTANT" -> Icons.Default.AccountBalance
        "INVENTORY_MANAGER" -> Icons.Default.Inventory2
        "CUSTOM" -> Icons.Default.ManageAccounts
        else -> Icons.Default.Person
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditUserDialog(
    userToEdit: User?,
    onDismiss: () -> Unit,
    onSave: (User) -> Unit,
    viewModel: MainViewModel
) {
    var fullName by remember { mutableStateOf(userToEdit?.fullName ?: "") }
    var username by remember { mutableStateOf(userToEdit?.username ?: "") }
    var pinPassword by remember { mutableStateOf(userToEdit?.passwordHash ?: "") }
    var role by remember { mutableStateOf(userToEdit?.role ?: "CASHIER") }
    var isActive by remember { mutableStateOf(userToEdit?.isActive ?: true) }

    // Permissions
    var canSell by remember { mutableStateOf(userToEdit?.canSell ?: true) }
    var canGiveDiscount by remember { mutableStateOf(userToEdit?.canGiveDiscount ?: false) }
    var canPurchase by remember { mutableStateOf(userToEdit?.canPurchase ?: false) }
    var canViewProfits by remember { mutableStateOf(userToEdit?.canViewProfits ?: false) }
    var canViewReports by remember { mutableStateOf(userToEdit?.canViewReports ?: false) }
    var canManageInventory by remember { mutableStateOf(userToEdit?.canManageInventory ?: false) }
    var canManageCustomers by remember { mutableStateOf(userToEdit?.canManageCustomers ?: true) }
    var canManageSuppliers by remember { mutableStateOf(userToEdit?.canManageSuppliers ?: false) }
    var canManageExpenses by remember { mutableStateOf(userToEdit?.canManageExpenses ?: false) }
    var canManageSettings by remember { mutableStateOf(userToEdit?.canManageSettings ?: false) }
    var canManageUsers by remember { mutableStateOf(userToEdit?.canManageUsers ?: false) }

    // Helper to set preset permissions based on role
    fun applyRolePreset(selectedRole: String) {
        role = selectedRole
        when (selectedRole) {
            "ADMIN" -> {
                canSell = true
                canGiveDiscount = true
                canPurchase = true
                canViewProfits = true
                canViewReports = true
                canManageInventory = true
                canManageCustomers = true
                canManageSuppliers = true
                canManageExpenses = true
                canManageSettings = true
                canManageUsers = true
            }
            "CASHIER" -> {
                canSell = true
                canGiveDiscount = false
                canPurchase = false
                canViewProfits = false
                canViewReports = false
                canManageInventory = false
                canManageCustomers = true
                canManageSuppliers = false
                canManageExpenses = false
                canManageSettings = false
                canManageUsers = false
            }
            "ACCOUNTANT" -> {
                canSell = true
                canGiveDiscount = true
                canPurchase = true
                canViewProfits = true
                canViewReports = true
                canManageInventory = true
                canManageCustomers = true
                canManageSuppliers = true
                canManageExpenses = true
                canManageSettings = false
                canManageUsers = false
            }
            "INVENTORY_MANAGER" -> {
                canSell = false
                canGiveDiscount = false
                canPurchase = true
                canViewProfits = false
                canViewReports = false
                canManageInventory = true
                canManageCustomers = false
                canManageSuppliers = true
                canManageExpenses = false
                canManageSettings = false
                canManageUsers = false
            }
            "CUSTOM" -> {
                // leave current switches as they are
            }
        }
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (userToEdit == null) Icons.Default.PersonAdd else Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (userToEdit == null) "إضافة موظف جديد" else "تعديل بيانات وصلاحيات الموظف",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Basic Info
                Text(text = "البيانات الأساسية للحساب:", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("الاسم الكامل للموظف *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("اسم المستخدم للدخول (باللغة الإنجليزية/أرقام) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = pinPassword,
                    onValueChange = { pinPassword = it },
                    label = { Text("الرمز السري / كلمة المرور (اختياري للتبديل السريع)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "حالة الحساب (نشط ومفعل)", fontSize = 13.sp)
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                }

                HorizontalDivider()

                // Role Presets
                Text(text = "الدور الوظيفي (قالب صلاحيات جاهز):", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    RolePresetOption(
                        title = "👑 مدير عام (صلاحيات كاملة)",
                        description = "كافة الصلاحيات مفتوحة بما فيها الإعدادات والمستخدمين",
                        selected = role == "ADMIN",
                        onClick = { applyRolePreset("ADMIN") }
                    )
                    RolePresetOption(
                        title = "🛒 كاشير / بائع",
                        description = "نقطة البيع والعملاء فقط، محجوب عنه الأرباح والتقارير والمشتريات",
                        selected = role == "CASHIER",
                        onClick = { applyRolePreset("CASHIER") }
                    )
                    RolePresetOption(
                        title = "💼 محاسب مالي",
                        description = "البيع والشراء، التقارير والأرباح، المصروفات، والحسابات",
                        selected = role == "ACCOUNTANT",
                        onClick = { applyRolePreset("ACCOUNTANT") }
                    )
                    RolePresetOption(
                        title = "📦 أمين مخزن ومستودع",
                        description = "المخزون والجرد، فواتير الشراء، وإدارة الأصناف",
                        selected = role == "INVENTORY_MANAGER",
                        onClick = { applyRolePreset("INVENTORY_MANAGER") }
                    )
                    RolePresetOption(
                        title = "⚙️ موظف مخصص (تحديد يدوي)",
                        description = "تحديد الصلاحيات بشكل منفرد ومخصص",
                        selected = role == "CUSTOM",
                        onClick = { role = "CUSTOM" }
                    )
                }

                HorizontalDivider()

                // Detailed Granular Switches
                Text(text = "تخصيص الصلاحيات بدقة لهذا الموظف:", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                // Sales & POS Group
                PermissionSectionCard(title = "صلاحيات البيع ونقاط البيع") {
                    PermissionToggleItem(
                        title = "البيع ونقطة البيع (POS)",
                        subtitle = "إصدار فواتير المبيعات للزبائن",
                        checked = canSell,
                        onCheckedChange = { canSell = it; role = "CUSTOM" }
                    )
                    PermissionToggleItem(
                        title = "منح الخصومات للزبائن",
                        subtitle = "تطبيق خصم مالي على الفاتورة",
                        checked = canGiveDiscount,
                        onCheckedChange = { canGiveDiscount = it; role = "CUSTOM" }
                    )
                }

                // Inventory & Purchases Group
                PermissionSectionCard(title = "صلاحيات المشتريات والمخزن") {
                    PermissionToggleItem(
                        title = "مشتريات وتوريد المحل",
                        subtitle = "تسجيل فواتير الشراء من الموردين",
                        checked = canPurchase,
                        onCheckedChange = { canPurchase = it; role = "CUSTOM" }
                    )
                    PermissionToggleItem(
                        title = "إدارة المخزون والمنتجات",
                        subtitle = "إضافة وتعديل وحذف المنتجات وضبط الجرد",
                        checked = canManageInventory,
                        onCheckedChange = { canManageInventory = it; role = "CUSTOM" }
                    )
                }

                // Profits & Financial Reports Group
                PermissionSectionCard(title = "صلاحيات الأرباح والتقارير المالية") {
                    PermissionToggleItem(
                        title = "رؤية الأرباح وتكلفة الشراء",
                        subtitle = "إظهار أسعار التكلفة وهامش الربح في الشاشات والتقارير",
                        checked = canViewProfits,
                        onCheckedChange = { canViewProfits = it; role = "CUSTOM" }
                    )
                    PermissionToggleItem(
                        title = "التقارير المالية وحركة الصندوق",
                        subtitle = "الاطلاع على تقارير المبيعات والمصروفات وحركة النقدية",
                        checked = canViewReports,
                        onCheckedChange = { canViewReports = it; role = "CUSTOM" }
                    )
                }

                // Accounts & Expenses Group
                PermissionSectionCard(title = "صلاحيات الحسابات والمصروفات") {
                    PermissionToggleItem(
                        title = "إدارة العملاء والديون",
                        subtitle = "إضافة وتعديل العملاء وسندات القبض",
                        checked = canManageCustomers,
                        onCheckedChange = { canManageCustomers = it; role = "CUSTOM" }
                    )
                    PermissionToggleItem(
                        title = "إدارة الموردين والمستحقات",
                        subtitle = "إضافة وتعديل الموردين وسندات الصرف",
                        checked = canManageSuppliers,
                        onCheckedChange = { canManageSuppliers = it; role = "CUSTOM" }
                    )
                    PermissionToggleItem(
                        title = "إدارة المصروفات",
                        subtitle = "تسجيل مصروفات المتجر وفواتير النفقات",
                        checked = canManageExpenses,
                        onCheckedChange = { canManageExpenses = it; role = "CUSTOM" }
                    )
                }

                // System & Users Admin Group
                PermissionSectionCard(title = "صلاحيات النظام والإدارة") {
                    PermissionToggleItem(
                        title = "إعدادات النظام والنسخ الاحتياطي",
                        subtitle = "تعديل إعدادات المتجر واستيراد/تصدير إكسل",
                        checked = canManageSettings,
                        onCheckedChange = { canManageSettings = it; role = "CUSTOM" }
                    )
                    PermissionToggleItem(
                        title = "إدارة المستخدمين والموظفين",
                        subtitle = "إضافة وتعديل حسابات الموظفين وصلاحياتهم",
                        checked = canManageUsers,
                        onCheckedChange = { canManageUsers = it; role = "CUSTOM" }
                    )
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fullName.isBlank()) {
                        errorMessage = "يرجى كتابة الاسم الكامل للموظف"
                        return@Button
                    }
                    if (username.isBlank()) {
                        errorMessage = "يرجى كتابة اسم المستخدم للدخول"
                        return@Button
                    }

                    val user = (userToEdit ?: User(username = "", passwordHash = "", fullName = "")).copy(
                        fullName = fullName.trim(),
                        username = username.trim().lowercase(),
                        passwordHash = pinPassword.trim(),
                        role = role,
                        isActive = isActive,
                        canSell = canSell,
                        canGiveDiscount = canGiveDiscount,
                        canPurchase = canPurchase,
                        canViewProfits = canViewProfits,
                        canViewReports = canViewReports,
                        canManageInventory = canManageInventory,
                        canManageCustomers = canManageCustomers,
                        canManageSuppliers = canManageSuppliers,
                        canManageExpenses = canManageExpenses,
                        canManageSettings = canManageSettings,
                        canManageUsers = canManageUsers
                    )
                    onSave(user)
                }
            ) {
                Text("حفظ الموظف")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun RolePresetOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    fontSize = 10.sp,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PermissionSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
fun PermissionToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(text = subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}
