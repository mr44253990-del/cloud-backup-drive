package com.example.ui.screens.money

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ai.MistralAiEngine
import com.example.data.database.entities.FinanceEntity
import com.example.ui.components.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MoneyScreen(
    financeList: List<FinanceEntity>,
    monthlyBudgetLimit: Double,
    onUpdateMonthlyBudget: (Double) -> Unit = {},
    onAddTransaction: (title: String, amount: Double, isIncome: Boolean, category: String, notes: String) -> Unit,
    onDeleteTransaction: (FinanceEntity) -> Unit,
    onAiParseExpense: (prompt: String, onSuccess: (MistralAiEngine.ParsedTransaction) -> Unit, onError: (String) -> Unit) -> Unit,
    onAiGetAdvice: (onResult: (String) -> Unit) -> Unit
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var dialogIsIncome by remember { mutableStateOf(false) }
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var showAiAdviceDialog by remember { mutableStateOf(false) }
    var aiAdviceText by remember { mutableStateOf("") }
    var isFetchingAdvice by remember { mutableStateOf(false) }

    // Budget custom edit state
    var showBudgetDialog by remember { mutableStateOf(false) }
    var budgetInput by remember { mutableStateOf(monthlyBudgetLimit.toInt().toString()) }

    // Invoice & Financial Tools dialog states
    var showInvoiceDialog by remember { mutableStateOf(false) }
    var showEmiDialog by remember { mutableStateOf(false) }
    var showBillSplitterDialog by remember { mutableStateOf(false) }
    var showCurrencyConverterDialog by remember { mutableStateOf(false) }

    // Natural Language AI Input state
    var naturalLanguageInput by remember { mutableStateOf("") }
    var isAiProcessing by remember { mutableStateOf(false) }

    val categories = remember {
        mutableStateListOf(
            "All", "Food", "Transport", "Internet", "Shopping", "Bills",
            "Education", "Salary", "Investment", "Healthcare", "Groceries", "Rent", "Freelance", "Other"
        )
    }

    val totalIncome = financeList.filter { it.isIncome }.sumOf { it.amount }
    val totalExpense = financeList.filter { !it.isIncome }.sumOf { it.amount }
    val netBalance = totalIncome - totalExpense

    val budgetProgress = if (monthlyBudgetLimit > 0) {
        (totalExpense / monthlyBudgetLimit).toFloat().coerceIn(0f, 1f)
    } else 0f

    val animatedBudgetProgress by animateFloatAsState(
        targetValue = budgetProgress,
        animationSpec = tween(800),
        label = "budgetProgress"
    )

    val filteredList = financeList.filter {
        selectedCategoryFilter == "All" || it.category.equals(selectedCategoryFilter, ignoreCase = true)
    }

    val currentMonthYear = remember {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        sdf.format(Date())
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy)
            .padding(horizontal = 16.dp)
            .testTag("money_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // 1. Header
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "FINANCE & BUDGET",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "Money & Budget",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Manage your finances • $currentMonthYear",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                Button(
                    onClick = {
                        isFetchingAdvice = true
                        showAiAdviceDialog = true
                        onAiGetAdvice { advice ->
                            aiAdviceText = advice
                            isFetchingAdvice = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "AI Advice", fontSize = 11.sp, color = NeonEmerald, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 2. Mistral AI Smart Natural Language Expense Assistant Card
        item {
            GlassCard(
                shape = RoundedCornerShape(20.dp),
                backgroundColor = DarkSurfaceElevated,
                borderColor = NeonCyan.copy(alpha = 0.5f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MISTRAL AI NATURAL EXPENSE ASSISTANT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Type in English or Bengali (e.g. 'দুপুরের খাবার ২৫০ টাকা' or '1200 internet bill') and AI will auto-categorize and save it:",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 15.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = naturalLanguageInput,
                        onValueChange = { naturalLanguageInput = it },
                        placeholder = { Text("e.g. Lunch 250 taka or Salary 45000...", fontSize = 12.sp, color = TextMuted) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = DarkSurfaceBorder
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            if (naturalLanguageInput.isNotBlank()) {
                                isAiProcessing = true
                                onAiParseExpense(
                                    naturalLanguageInput,
                                    { parsed ->
                                        isAiProcessing = false
                                        naturalLanguageInput = ""
                                        Toast.makeText(context, "Recorded: ${parsed.title} (৳${parsed.amount})", Toast.LENGTH_SHORT).show()
                                    },
                                    { errorMsg ->
                                        isAiProcessing = false
                                        Toast.makeText(context, "AI Error: $errorMsg", Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isAiProcessing && naturalLanguageInput.isNotBlank()
                    ) {
                        if (isAiProcessing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Record", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 3. Net Balance Glass Card
        item {
            GlassCard(
                shape = RoundedCornerShape(22.dp),
                backgroundColor = DarkSurfaceElevated,
                borderColor = ElectricBlue.copy(alpha = 0.5f)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "TOTAL BALANCE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "৳ ${String.format("%,.2f", netBalance)}",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (netBalance >= 0) NeonCyan else PriorityCritical
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Income Summary Card
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(DarkSurfaceGlass)
                                .border(1.dp, NeonEmerald.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = null,
                                        tint = NeonEmerald,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Income", fontSize = 11.sp, color = TextSecondary)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "৳ ${String.format("%,.0f", totalIncome)}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonEmerald
                                )
                            }
                        }

                        // Expense Summary Card
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(DarkSurfaceGlass)
                                .border(1.dp, NeonPink.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = null,
                                        tint = NeonPink,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Expenses", fontSize = 11.sp, color = TextSecondary)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "৳ ${String.format("%,.0f", totalExpense)}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonPink
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action Buttons: + Add Income, - Add Expense
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                dialogIsIncome = true
                                showAddDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("add_income_button")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Income", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                dialogIsIncome = false
                                showAddDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC2185B)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("add_expense_button")
                        ) {
                            Icon(imageVector = Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Expense", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Financial Tools Row
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showInvoiceDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.Receipt, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Invoice Maker", fontSize = 11.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { showEmiDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.Calculate, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("EMI Calculator", fontSize = 11.sp, color = NeonEmerald, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { showBillSplitterDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonPurple.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.Group, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Bill Splitter", fontSize = 11.sp, color = NeonPurple, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { showCurrencyConverterDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonAmber.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.CurrencyExchange, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Currency Rates", fontSize = 11.sp, color = NeonAmber, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 4. Monthly Budget Progress Card
        item {
            GlassCard(
                shape = RoundedCornerShape(20.dp),
                onClick = { showBudgetDialog = true }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "MONTHLY BUDGET",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Budget",
                            tint = NeonCyan,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = "৳ ${totalExpense.toInt()} / ৳ ${monthlyBudgetLimit.toInt()}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { animatedBudgetProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = if (budgetProgress > 0.9f) PriorityCritical else NeonCyan,
                    trackColor = DarkSurfaceBorder
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${(budgetProgress * 100).toInt()}% spent",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                    val remaining = (monthlyBudgetLimit - totalExpense).coerceAtLeast(0.0)
                    Text(
                        text = "৳ ${remaining.toInt()} left (Tap to change)",
                        fontSize = 11.sp,
                        color = if (remaining > 0) NeonEmerald else PriorityCritical
                    )
                }
            }
        }

        // 5. Category Filter Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = selectedCategoryFilter == cat
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) ElectricBlue else DarkSurfaceElevated)
                            .border(1.dp, if (isSelected) ElectricBlue else DarkSurfaceBorder, RoundedCornerShape(10.dp))
                            .clickable { selectedCategoryFilter = cat }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = cat,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else TextSecondary
                        )
                    }
                }
            }
        }

        // 6. Transactions Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TRANSACTIONS (${filteredList.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
            }
        }

        // 7. Transactions List
        if (filteredList.isEmpty()) {
            item {
                GlassCard {
                    Text(
                        text = "No transactions found in $selectedCategoryFilter. Use AI Record or buttons above to add!",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }
        } else {
            items(filteredList, key = { it.id }) { item ->
                TransactionRow(item = item, onDelete = { onDeleteTransaction(item) })
            }
        }
    }

    // AI Financial Advisor Dialog
    if (showAiAdviceDialog) {
        Dialog(onDismissRequest = { showAiAdviceDialog = false }) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = DarkSurfaceElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(22.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("MISTRAL AI FINANCIAL INSIGHTS", color = NeonEmerald, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    if (isFetchingAdvice) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                            CircularProgressIndicator(color = NeonEmerald, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Analyzing your cashflow & monthly budget...", color = TextSecondary, fontSize = 12.sp)
                        }
                    } else {
                        Text(
                            text = aiAdviceText,
                            fontSize = 13.sp,
                            color = TextPrimary,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { showAiAdviceDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Thank You", color = DeepNavy, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddTransactionDialog(
            isIncome = dialogIsIncome,
            categories = categories.filter { it != "All" },
            onDismiss = { showAddDialog = false },
            onSave = { title, amount, category, notes ->
                onAddTransaction(title, amount, dialogIsIncome, category, notes)
                showAddDialog = false
            }
        )
    }

    if (showInvoiceDialog) {
        InvoiceReceiptDialog(onDismiss = { showInvoiceDialog = false })
    }

    if (showEmiDialog) {
        EmiSavingsCalculatorDialog(onDismiss = { showEmiDialog = false })
    }

    if (showBillSplitterDialog) {
        BillSplitterDialog(onDismiss = { showBillSplitterDialog = false })
    }

    if (showCurrencyConverterDialog) {
        CurrencyConverterDialog(onDismiss = { showCurrencyConverterDialog = false })
    }

    if (showBudgetDialog) {
        CustomBudgetDialog(
            currentBudget = monthlyBudgetLimit,
            onDismiss = { showBudgetDialog = false },
            onSaveBudget = { newBudget ->
                onUpdateMonthlyBudget(newBudget)
                showBudgetDialog = false
            }
        )
    }
}

@Composable
fun CustomBudgetDialog(
    currentBudget: Double,
    onDismiss: () -> Unit,
    onSaveBudget: (Double) -> Unit
) {
    val context = LocalContext.current
    var budgetInput by remember { mutableStateOf(currentBudget.toInt().toString()) }
    val presets = listOf(5000, 10000, 15000, 20000, 30000, 50000, 100000)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = DarkSurfaceElevated,
            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("SET MONTHLY BUDGET", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Text("Enter your target monthly expense limit in BDT (৳):", fontSize = 12.sp, color = TextSecondary)

                OutlinedTextField(
                    value = budgetInput,
                    onValueChange = { budgetInput = it.filter { c -> c.isDigit() } },
                    label = { Text("Monthly Budget (৳)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = DarkSurfaceBorder
                    ),
                    singleLine = true
                )

                Text("Quick Preset Amounts:", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { preset ->
                        val isSelected = budgetInput == preset.toString()
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) ElectricBlue else DarkSurface)
                                .border(0.5.dp, if (isSelected) NeonCyan else DarkSurfaceBorder, RoundedCornerShape(8.dp))
                                .clickable { budgetInput = preset.toString() }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "৳ $preset",
                                fontSize = 11.sp,
                                color = if (isSelected) Color.White else TextSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        val amount = budgetInput.toDoubleOrNull() ?: 20000.0
                        if (amount > 0) {
                            onSaveBudget(amount)
                            Toast.makeText(context, "Monthly budget updated to ৳${amount.toInt()}!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Target Budget", color = DeepNavy, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun InvoiceReceiptDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var clientName by remember { mutableStateOf("") }
    var item1Title by remember { mutableStateOf("") }
    var item1Price by remember { mutableStateOf("") }
    var item2Title by remember { mutableStateOf("") }
    var item2Price by remember { mutableStateOf("") }
    var discountPercent by remember { mutableStateOf("0") }
    var taxPercent by remember { mutableStateOf("0") }

    val p1 = item1Price.toDoubleOrNull() ?: 0.0
    val p2 = item2Price.toDoubleOrNull() ?: 0.0
    val subtotal = p1 + p2
    val disc = (discountPercent.toDoubleOrNull() ?: 0.0) / 100.0 * subtotal
    val tax = (taxPercent.toDoubleOrNull() ?: 0.0) / 100.0 * (subtotal - disc)
    val total = (subtotal - disc + tax).coerceAtLeast(0.0)

    val invoiceNo = remember { "INV-" + (1000..9999).random() }
    val dateStr = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = DarkSurfaceElevated,
            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("INVOICE & RECEIPT MAKER", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("Client / Customer Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = DarkSurfaceBorder
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                item {
                    Text("Line Items & Services", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = item1Title,
                            onValueChange = { item1Title = it },
                            placeholder = { Text("Item 1 (e.g. Website Dev)", fontSize = 11.sp, color = TextMuted) },
                            modifier = Modifier.weight(1.8f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = DarkSurfaceBorder
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = item1Price,
                            onValueChange = { item1Price = it.filter { c -> c.isDigit() || c == '.' } },
                            placeholder = { Text("৳ Amount", fontSize = 11.sp, color = TextMuted) },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = DarkSurfaceBorder
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = item2Title,
                            onValueChange = { item2Title = it },
                            placeholder = { Text("Item 2 (e.g. Hosting)", fontSize = 11.sp, color = TextMuted) },
                            modifier = Modifier.weight(1.8f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = DarkSurfaceBorder
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = item2Price,
                            onValueChange = { item2Price = it.filter { c -> c.isDigit() || c == '.' } },
                            placeholder = { Text("৳ Amount", fontSize = 11.sp, color = TextMuted) },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = DarkSurfaceBorder
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                item {
                    // Summary card
                    GlassCard(backgroundColor = DarkSurface, shape = RoundedCornerShape(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subtotal:", color = TextSecondary, fontSize = 12.sp)
                            Text("৳ ${String.format("%,.2f", subtotal)}", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Grand Total Due:", color = NeonCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("৳ ${String.format("%,.2f", total)}", color = NeonCyan, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }

                item {
                    Button(
                        onClick = {
                            val formattedInvoice = """
                            ==============================
                                  INVOICE / RECEIPT
                            ==============================
                            Invoice No: $invoiceNo
                            Date: $dateStr
                            Client: ${clientName.ifBlank { "Valued Customer" }}
                            ------------------------------
                            1. ${item1Title.ifBlank { "Service Item" }}: ৳ $p1
                            ${if (p2 > 0) "2. ${item2Title.ifBlank { "Additional Item" }}: ৳ $p2" else ""}
                            ------------------------------
                            Subtotal: ৳ ${String.format("%,.2f", subtotal)}
                            TOTAL DUE: ৳ ${String.format("%,.2f", total)}
                            ==============================
                            Thank you for your business!
                            """.trimIndent()

                            val cb = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            cb.setPrimaryClip(android.content.ClipData.newPlainText("Invoice", formattedInvoice))
                            Toast.makeText(context, "Professional Invoice copied to clipboard!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Formatted Invoice Text")
                    }
                }
            }
        }
    }
}

@Composable
fun EmiSavingsCalculatorDialog(onDismiss: () -> Unit) {
    var loanAmountText by remember { mutableStateOf("100000") }
    var interestRateText by remember { mutableStateOf("9") }
    var monthsText by remember { mutableStateOf("12") }

    val principal = loanAmountText.toDoubleOrNull() ?: 0.0
    val rate = (interestRateText.toDoubleOrNull() ?: 0.0) / (12 * 100)
    val n = monthsText.toDoubleOrNull() ?: 1.0

    val emi = if (rate > 0 && n > 0 && principal > 0) {
        val factor = Math.pow(1.0 + rate, n)
        (principal * rate * factor) / (factor - 1)
    } else {
        if (n > 0) principal / n else 0.0
    }

    val totalPayment = emi * n
    val totalInterest = (totalPayment - principal).coerceAtLeast(0.0)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = DarkSurfaceElevated,
            border = androidx.compose.foundation.BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("LOAN EMI & SAVINGS PLANNER", color = NeonEmerald, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                OutlinedTextField(
                    value = loanAmountText,
                    onValueChange = { loanAmountText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Loan / Goal Amount (৳)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = interestRateText,
                        onValueChange = { interestRateText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Annual Rate %") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = monthsText,
                        onValueChange = { monthsText = it.filter { c -> c.isDigit() } },
                        label = { Text("Months") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                GlassCard(backgroundColor = DarkSurface, shape = RoundedCornerShape(12.dp)) {
                    Text("MONTHLY EMI PAYMENT", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                    Text("৳ ${String.format("%,.2f", emi)} / month", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = NeonEmerald)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Total Interest: ৳ ${String.format("%,.2f", totalInterest)}", fontSize = 11.sp, color = TextSecondary)
                    Text("Total Repayment: ৳ ${String.format("%,.2f", totalPayment)}", fontSize = 11.sp, color = NeonCyan)
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Done", color = DeepNavy, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TransactionRow(
    item: com.example.data.database.entities.FinanceEntity,
    onDelete: () -> Unit
) {
    val isIncome = item.isIncome
    val accentColor = if (isIncome) NeonEmerald else PriorityCritical

    GlassCard(
        shape = RoundedCornerShape(16.dp),
        backgroundColor = DarkSurfaceElevated
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                        .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = item.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.category,
                            fontSize = 11.sp,
                            color = NeonCyan
                        )
                        if (item.notes.isNotBlank()) {
                            Text(
                                text = " • ${item.notes}",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${if (isIncome) "+" else "-"}৳${String.format("%,.2f", item.amount)}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = accentColor
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Transaction",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AddTransactionDialog(
    isIncome: Boolean,
    categories: List<String>,
    onDismiss: () -> Unit,
    onSave: (title: String, amount: Double, category: String, notes: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull() ?: if (isIncome) "Salary" else "Food") }
    var notes by remember { mutableStateOf("") }

    val accentColor = if (isIncome) NeonEmerald else PriorityCritical

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = DarkSurfaceElevated,
            border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (isIncome) "Add Income (+৳)" else "Add Expense (-৳)",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = accentColor
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (e.g. Lunch, Freelance)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = DarkSurfaceBorder
                    )
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Amount (৳)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = DarkSurfaceBorder
                    )
                )

                // Category Selector
                Column {
                    Text("Category", fontSize = 12.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { cat ->
                            val isSelected = selectedCategory == cat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) accentColor else DarkSurfaceGlass)
                                    .border(1.dp, if (isSelected) accentColor else DarkSurfaceBorder, RoundedCornerShape(8.dp))
                                    .clickable { selectedCategory = cat }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) DeepNavy else TextPrimary
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = DarkSurfaceBorder
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }

                    Button(
                        onClick = {
                            val amount = amountText.toDoubleOrNull() ?: 0.0
                            if (title.isNotBlank() && amount > 0.0) {
                                onSave(title, amount, selectedCategory, notes)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save", color = DeepNavy, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun BillSplitterDialog(onDismiss: () -> Unit) {
    var totalBillText by remember { mutableStateOf("1500") }
    var peopleCountText by remember { mutableStateOf("3") }
    var tipPercentText by remember { mutableStateOf("10") }

    val totalBill = totalBillText.toDoubleOrNull() ?: 0.0
    val peopleCount = peopleCountText.toIntOrNull()?.coerceAtLeast(1) ?: 1
    val tipPercent = tipPercentText.toDoubleOrNull() ?: 0.0

    val tipAmount = totalBill * (tipPercent / 100.0)
    val grandTotal = totalBill + tipAmount
    val perPersonAmount = grandTotal / peopleCount

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = DarkSurfaceElevated,
            border = androidx.compose.foundation.BorderStroke(1.dp, NeonPurple.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("GROUP EXPENSE & BILL SPLITTER", color = NeonPurple, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Text("Split restaurant, travel or trip bills equally among friends:", fontSize = 12.sp, color = TextSecondary)

                OutlinedTextField(
                    value = totalBillText,
                    onValueChange = { totalBillText = it },
                    label = { Text("Total Bill Amount (৳)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = NeonPurple, unfocusedBorderColor = DarkSurfaceBorder),
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = peopleCountText,
                        onValueChange = { peopleCountText = it },
                        label = { Text("People Count") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = NeonPurple, unfocusedBorderColor = DarkSurfaceBorder),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = tipPercentText,
                        onValueChange = { tipPercentText = it },
                        label = { Text("Tip / Tax %") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = NeonPurple, unfocusedBorderColor = DarkSurfaceBorder),
                        singleLine = true
                    )
                }

                GlassCard(shape = RoundedCornerShape(14.dp), backgroundColor = DarkSurface) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Grand Total (inc. tip):", fontSize = 12.sp, color = TextSecondary)
                            Text("৳ ${String.format("%,.2f", grandTotal)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Divider(color = DarkSurfaceBorder, thickness = 0.5.dp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("EACH PERSON PAYS:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonPurple)
                            Text("৳ ${String.format("%,.2f", perPersonAmount)}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = NeonCyan)
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickCopyButton(
                        textToCopy = "Group Bill Split Result:\nTotal: ৳$grandTotal\nPeople: $peopleCount\nEach Person Pays: ৳${String.format("%.2f", perPersonAmount)}",
                        label = "Copy Breakdown",
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CurrencyConverterDialog(onDismiss: () -> Unit) {
    var bdtInputText by remember { mutableStateOf("1000") }
    val bdt = bdtInputText.toDoubleOrNull() ?: 0.0

    // Approximate conversion rates
    val usdRate = 120.0
    val eurRate = 131.5
    val inrRate = 1.42
    val sarRate = 32.0
    val aedRate = 32.7

    val usd = bdt / usdRate
    val eur = bdt / eurRate
    val inr = bdt / inrRate
    val sar = bdt / sarRate
    val aed = bdt / aedRate

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = DarkSurfaceElevated,
            border = androidx.compose.foundation.BorderStroke(1.dp, NeonAmber.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("MULTI-CURRENCY RATES CONVERTER", color = NeonAmber, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Text("Convert Bangladeshi Taka (৳ BDT) to world currencies:", fontSize = 12.sp, color = TextSecondary)

                OutlinedTextField(
                    value = bdtInputText,
                    onValueChange = { bdtInputText = it },
                    label = { Text("Amount in BDT (৳)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = NeonAmber, unfocusedBorderColor = DarkSurfaceBorder),
                    singleLine = true
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CurrencyRateRow(flag = "🇺🇸", code = "USD", label = "US Dollar", result = String.format("$ %.2f", usd))
                    CurrencyRateRow(flag = "🇪🇺", code = "EUR", label = "Euro", result = String.format("€ %.2f", eur))
                    CurrencyRateRow(flag = "🇮🇳", code = "INR", label = "Indian Rupee", result = String.format("₹ %.2f", inr))
                    CurrencyRateRow(flag = "🇸🇦", code = "SAR", label = "Saudi Riyal", result = String.format("SR %.2f", sar))
                    CurrencyRateRow(flag = "🇦🇪", code = "AED", label = "UAE Dirham", result = String.format("AED %.2f", aed))
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonAmber),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close", color = DeepNavy, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CurrencyRateRow(flag: String, code: String, label: String, result: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurface)
            .border(0.5.dp, DarkSurfaceBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(flag, fontSize = 16.sp)
            Column {
                Text(code, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(label, fontSize = 10.sp, color = TextMuted)
            }
        }
        Text(result, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = NeonCyan)
    }
}
