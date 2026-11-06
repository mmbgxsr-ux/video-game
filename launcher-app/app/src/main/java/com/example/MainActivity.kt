package com.example.multilauncher

import android.app.Activity
import android.app.ActivityOptions
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.*
import kotlin.math.ceil
import kotlin.math.sqrt

class MainActivity : Activity() {

    private val checks = linkedMapOf<String, CheckBox>()      // package -> checkbox
    private val prefs by lazy { getSharedPreferences("groups", MODE_PRIVATE) }
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var list: LinearLayout
    private lateinit var copiesSpinner: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (16 * resources.displayMetrics.density).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
            layoutDirection = View.LAYOUT_DIRECTION_LOCALE
        }

        root.addView(TextView(this).apply {
            text = "مشغّل التطبيقات المتعددة"
            textSize = 22f
        })

        // عدد النسخ لكل تطبيق
        val copiesRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        copiesRow.addView(TextView(this).apply { text = "عدد النوافذ لكل تطبيق:  " })
        copiesSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity, android.R.layout.simple_spinner_dropdown_item,
                listOf(1, 2, 3, 4)
            )
        }
        copiesRow.addView(copiesSpinner)
        root.addView(copiesRow)

        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply { addView(list) },
            LinearLayout.LayoutParams(-1, 0, 1f))

        val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        fun btn(t: String, onClick: () -> Unit) = Button(this).apply {
            text = t; setOnClickListener { onClick() }
        }
        buttons.addView(btn("تشغيل") { launchAll() }, LinearLayout.LayoutParams(0, -2, 1f))
        buttons.addView(btn("حفظ مجموعة") { saveGroup() }, LinearLayout.LayoutParams(0, -2, 1f))
        buttons.addView(btn("تحميل مجموعة") { loadGroup() }, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(buttons)

        setContentView(root)
        loadApps()
    }

    private fun loadApps() {
        Thread {
            val pm = packageManager
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val apps = pm.queryIntentActivities(intent, 0)
                .map { it.loadLabel(pm).toString() to it.activityInfo.packageName }
                .filter { it.second != packageName }
                .distinctBy { it.second }
                .sortedBy { it.first.lowercase() }
            runOnUiThread {
                apps.forEach { (label, pkg) ->
                    val cb = CheckBox(this).apply { text = label }
                    checks[pkg] = cb
                    list.addView(cb)
                }
            }
        }.start()
    }

    private fun launchAll() {
        val pkgs = checks.filter { it.value.isChecked }.keys.toList()
        if (pkgs.isEmpty()) { toast("اختر تطبيقًا واحدًا على الأقل"); return }

        val copies = copiesSpinner.selectedItem as Int
        val jobs = pkgs.flatMap { p -> List(copies) { p } }
        val n = jobs.size
        val cols = ceil(sqrt(n.toDouble())).toInt()
        val rows = ceil(n.toDouble() / cols).toInt()
        val dm = resources.displayMetrics
        val w = dm.widthPixels / cols
        val h = dm.heightPixels / rows

        jobs.forEachIndexed { i, pkg ->
            handler.postDelayed({
                val intent = packageManager.getLaunchIntentForPackage(pkg)
                if (intent == null) { toast("تعذّر فتح $pkg"); return@postDelayed }
                intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                    Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT
                )
                val c = i % cols
                val r = i / cols
                // حدود النافذة تُستخدم فقط في وضع النوافذ الحرة (Freeform)
                val opts = ActivityOptions.makeBasic()
                    .setLaunchBounds(Rect(c * w, r * h, (c + 1) * w, (r + 1) * h))
                try {
                    startActivity(intent, opts.toBundle())
                } catch (e: Exception) {
                    toast("فشل فتح $pkg")
                }
            }, i * 900L)
        }
    }

    private fun saveGroup() {
        val selected = checks.filter { it.value.isChecked }.keys
        if (selected.isEmpty()) { toast("لا يوجد تطبيقات محددة"); return }
        val input = EditText(this).apply { hint = "اسم المجموعة" }
        AlertDialog.Builder(this)
            .setTitle("حفظ المجموعة")
            .setView(input)
            .setPositiveButton("حفظ") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    prefs.edit().putStringSet(name, selected.toSet()).apply()
                    toast("تم الحفظ")
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun loadGroup() {
        val names = prefs.all.keys.sorted()
        if (names.isEmpty()) { toast("لا توجد مجموعات محفوظة"); return }
        AlertDialog.Builder(this)
            .setTitle("اختر مجموعة")
            .setItems(names.toTypedArray()) { _, i ->
                val set = prefs.getStringSet(names[i], emptySet()) ?: emptySet()
                checks.forEach { (pkg, cb) -> cb.isChecked = pkg in set }
            }
            .setNeutralButton("حذف…") { _, _ -> deleteGroup(names) }
            .show()
    }

    private fun deleteGroup(names: List<String>) {
        AlertDialog.Builder(this)
            .setTitle("حذف مجموعة")
            .setItems(names.toTypedArray()) { _, i ->
                prefs.edit().remove(names[i]).apply()
            }
            .show()
    }

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()
}
