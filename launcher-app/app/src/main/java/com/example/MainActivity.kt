package com.example.multilauncher

import android.app.Activity
import android.app.ActivityOptions
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
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
    private lateinit var copiesInput: EditText

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

        // عدد النوافذ لكل تطبيق (بدون حد أقصى)
        val copiesRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        copiesRow.addView(TextView(this).apply { text = "عدد النوافذ لكل تطبيق:  " })
        copiesInput = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText("1")
            setSingleLine()
            minEms = 3
            gravity = Gravity.CENTER
        }
        copiesRow.addView(copiesInput)
        root.addView(copiesRow)

        // بحث
        val search = EditText(this).apply {
            hint = "ابحث عن تطبيق"
            setSingleLine()
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) { filterApps(s?.toString().orEmpty()) }
                override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            })
        }
        root.addView(search)

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

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
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
                if (isDestroyed) return@runOnUiThread
                apps.forEach { (label, pkg) ->
                    val cb = CheckBox(this).apply { text = label }
                    checks[pkg] = cb
                    list.addView(cb)
                }
            }
        }.start()
    }

    private fun filterApps(q: String) {
        checks.values.forEach { cb ->
            cb.visibility = if (cb.text.contains(q.trim(), ignoreCase = true)) View.VISIBLE else View.GONE
        }
    }

    private fun launchAll() {
        val pkgs = checks.filter { it.value.isChecked }.keys.toList()
        if (pkgs.isEmpty()) { toast("اختر تطبيقًا واحدًا على الأقل"); return }

        val copies = (copiesInput.text.toString().toIntOrNull() ?: 1).coerceAtLeast(1)
        val jobs = pkgs.flatMap { p -> List(copies) { p } }

        val warnings = mutableListOf<String>()
        if (!isInMultiWindowMode) {
            warnings += "المشغّل ليس في وضع تقسيم الشاشة أو النوافذ الحرة. أندرويد يمنع التطبيق الذي صار في الخلفية من فتح غيره، فقد يُفتح تطبيق واحد فقط."
        }
        if (jobs.size > 12) {
            warnings += "ستُفتح ${jobs.size} نافذة، وقد يتباطأ الهاتف."
        }

        if (warnings.isEmpty()) {
            startJobs(jobs)
        } else {
            AlertDialog.Builder(this)
                .setTitle("تنبيه")
                .setMessage(warnings.joinToString("\n\n"))
                .setPositiveButton("متابعة") { _, _ -> startJobs(jobs) }
                .setNegativeButton("إلغاء", null)
                .show()
        }
    }

    private fun startJobs(jobs: List<String>) {
        handler.removeCallbacksAndMessages(null)   // يمنع تراكم تشغيلين متتاليين

        val n = jobs.size
        val cols = ceil(sqrt(n.toDouble())).toInt()
        val rows = ceil(n.toDouble() / cols).toInt()
        val dm = resources.displayMetrics
        val w = dm.widthPixels / cols
        val h = dm.heightPixels / rows
        val failed = mutableListOf<String>()

        jobs.forEachIndexed { i, pkg ->
            handler.postDelayed({
                val intent = packageManager.getLaunchIntentForPackage(pkg)
                if (intent == null) { failed += pkg; return@postDelayed }
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
                    failed += pkg
                }
            }, i * 900L)
        }

        handler.postDelayed({
            if (failed.isNotEmpty()) toast("تعذّر فتح ${failed.size} نافذة")
        }, n * 900L + 300L)
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
