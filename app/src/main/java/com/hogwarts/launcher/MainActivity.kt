package com.hogwarts.launcher

import android.app.Activity
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextClock
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private data class Shortcut(
        val label: String,
        val icon: Int,
        val keyword: String,
        val packages: List<String>
    )

    private data class AppEntry(
        val label: String,
        val pkg: String,
        val intent: Intent,
        val info: ResolveInfo
    )

    private val gold = Color.parseColor("#E8B04A")
    private val moon = Color.parseColor("#D6E6FF")

    private val shortcuts = listOf(
        Shortcut("Netflix", R.drawable.ic_netflix, "netflix", listOf("com.netflix.ninja")),
        Shortcut("YouTube", R.drawable.ic_youtube, "youtube", listOf("com.google.android.youtube.tv")),
        Shortcut("Prime Video", R.drawable.ic_prime, "prime video", listOf("com.amazon.amazonvideo.livingroom")),
        Shortcut("Disney+", R.drawable.ic_disney, "disney", listOf("com.disney.disneyplus")),
        Shortcut("Globoplay", R.drawable.ic_globoplay, "globoplay", listOf("com.globo.globotv")),
        Shortcut("UniTV", R.drawable.ic_unitv, "unitv", emptyList())
    )

    private var apps: List<AppEntry> = emptyList()
    private val adapter = AppAdapter()

    private lateinit var homeView: FrameLayout
    private lateinit var allAppsView: LinearLayout
    private lateinit var grid: GridView
    private var firstTile: View? = null
    private lateinit var owl: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = FrameLayout(this)

        val bg = ImageView(this)
        bg.setImageResource(R.drawable.bg_hogwarts)
        bg.scaleType = ImageView.ScaleType.CENTER_CROP
        root.addView(bg, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        homeView = buildHome()
        allAppsView = buildAllApps()
        allAppsView.visibility = View.GONE

        root.addView(homeView, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        root.addView(allAppsView, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        apps = loadApps()
        adapter.notifyDataSetChanged()
        if (homeView.visibility == View.VISIBLE) {
            firstTile?.requestFocus()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        showHome()
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        if (allAppsView.visibility == View.VISIBLE) {
            showHome()
        }
        // Na tela inicial o botao Voltar nao faz nada, como em todo launcher.
    }

    // ---------- Telas ----------

    private fun buildHome(): FrameLayout {
        val home = FrameLayout(this)

        val title = TextView(this)
        title.text = "Hogwarts"
        title.setTextColor(gold)
        title.textSize = 40f
        title.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        title.setShadowLayer(18f, 0f, 0f, gold)
        val titleLp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.TOP or Gravity.START)
        titleLp.leftMargin = dp(48)
        titleLp.topMargin = dp(32)
        home.addView(title, titleLp)

        val clock = TextClock(this)
        clock.format12Hour = "HH:mm"
        clock.format24Hour = "HH:mm"
        clock.textSize = 28f
        clock.setTextColor(moon)
        clock.typeface = Typeface.SERIF
        val clockLp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.TOP or Gravity.END)
        clockLp.rightMargin = dp(48)
        clockLp.topMargin = dp(40)
        home.addView(clock, clockLp)

        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER
        row.setPadding(dp(24), 0, dp(24), 0)

        for (s in shortcuts) {
            val t = tile(s.label, s.icon, { moveOwl(it) }) { openShortcut(s) }
            if (firstTile == null) firstTile = t
            row.addView(t, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
        row.addView(
            tile("Todos os apps", R.drawable.ic_biblioteca, { moveOwl(it) }) { showAll() },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        val rowLp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM)
        rowLp.bottomMargin = dp(48)
        home.addView(row, rowLp)

        owl = ImageView(this)
        owl.setImageResource(R.drawable.owl)
        owl.visibility = View.INVISIBLE
        home.addView(owl, FrameLayout.LayoutParams(dp(72), dp(72), Gravity.TOP or Gravity.START))

        return home
    }

    private fun buildAllApps(): LinearLayout {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setBackgroundColor(Color.parseColor("#E00A1630"))
        box.setPadding(dp(48), dp(32), dp(48), dp(24))

        val title = TextView(this)
        title.text = "Todos os apps"
        title.setTextColor(gold)
        title.textSize = 32f
        title.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        box.addView(title, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val selector = GradientDrawable()
        selector.cornerRadius = dp(14).toFloat()
        selector.setColor(0x44E8B04A)
        selector.setStroke(dp(2), gold)

        grid = GridView(this)
        grid.numColumns = 6
        grid.stretchMode = GridView.STRETCH_COLUMN_WIDTH
        grid.verticalSpacing = dp(8)
        grid.horizontalSpacing = dp(8)
        grid.setSelector(selector)
        grid.adapter = adapter
        grid.setOnItemClickListener { _, _, position, _ -> launchApp(apps[position]) }
        val gridLp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        gridLp.topMargin = dp(16)
        box.addView(grid, gridLp)

        return box
    }

    private fun tile(label: String, icon: Int, onFocused: (View) -> Unit, onClick: () -> Unit): View {
        val t = LinearLayout(this)
        t.orientation = LinearLayout.VERTICAL
        t.gravity = Gravity.CENTER_HORIZONTAL
        t.isFocusable = true
        t.isClickable = true
        t.setPadding(dp(8), dp(10), dp(8), dp(8))

        val img = ImageView(this)
        img.setImageResource(icon)
        t.addView(img, LinearLayout.LayoutParams(dp(84), dp(84)))

        val tv = TextView(this)
        tv.text = label
        tv.setTextColor(moon)
        tv.textSize = 15f
        tv.typeface = Typeface.SERIF
        tv.gravity = Gravity.CENTER
        tv.maxLines = 1
        tv.ellipsize = TextUtils.TruncateAt.END
        val tvLp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        tvLp.topMargin = dp(6)
        t.addView(tv, tvLp)

        t.setOnClickListener { onClick() }
        t.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) onFocused(v)
            val scale = if (hasFocus) 1.15f else 1f
            v.animate().scaleX(scale).scaleY(scale).setDuration(150).start()
            if (hasFocus) {
                val frame = GradientDrawable()
                frame.cornerRadius = dp(18).toFloat()
                frame.setColor(0x33000000)
                frame.setStroke(dp(3), gold)
                v.background = frame
            } else {
                v.background = null
            }
            tv.setTextColor(if (hasFocus) gold else moon)
        }
        return t
    }

    // A coruja Edwiga pousa sobre o atalho escolhido
    private fun moveOwl(target: View) {
        target.post {
            val t = IntArray(2)
            val h = IntArray(2)
            target.getLocationInWindow(t)
            homeView.getLocationInWindow(h)
            val tx = (t[0] - h[0]) + target.width / 2f - owl.width / 2f
            val ty = (t[1] - h[1]) - owl.height + dp(2).toFloat()
            if (owl.visibility != View.VISIBLE) {
                owl.x = tx
                owl.y = ty - dp(40)
                owl.alpha = 0f
                owl.visibility = View.VISIBLE
            }
            owl.animate().x(tx).y(ty).alpha(1f).setDuration(350).start()
        }
    }

    private fun showAll() {
        homeView.visibility = View.GONE
        allAppsView.visibility = View.VISIBLE
        grid.requestFocus()
        grid.setSelection(0)
    }

    private fun showHome() {
        allAppsView.visibility = View.GONE
        homeView.visibility = View.VISIBLE
        firstTile?.requestFocus()
    }

    // ---------- Apps ----------

    private fun loadApps(): List<AppEntry> {
        val found = LinkedHashMap<String, AppEntry>()
        val categories = listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)
        for (cat in categories) {
            val query = Intent(Intent.ACTION_MAIN)
            query.addCategory(cat)
            for (ri in packageManager.queryIntentActivities(query, 0)) {
                val pkg = ri.activityInfo.packageName
                if (pkg == packageName || found.containsKey(pkg)) continue
                val launch = Intent(Intent.ACTION_MAIN)
                launch.addCategory(cat)
                launch.setClassName(pkg, ri.activityInfo.name)
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                found[pkg] = AppEntry(ri.loadLabel(packageManager).toString(), pkg, launch, ri)
            }
        }
        return found.values.sortedBy { it.label.lowercase() }
    }

    private fun findApp(s: Shortcut): AppEntry? {
        return apps.firstOrNull { it.pkg in s.packages }
            ?: apps.firstOrNull { it.label.equals(s.keyword, ignoreCase = true) }
            ?: apps.firstOrNull { it.label.contains(s.keyword, ignoreCase = true) }
    }

    private fun openShortcut(s: Shortcut) {
        val app = findApp(s)
        if (app != null) {
            launchApp(app)
        } else {
            Toast.makeText(this, s.label + " não está instalado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchApp(e: AppEntry) {
        try {
            startActivity(e.intent)
        } catch (ex: Exception) {
            Toast.makeText(this, "Não foi possível abrir " + e.label, Toast.LENGTH_SHORT).show()
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    // ---------- Lista de todos os apps ----------

    private inner class AppAdapter : BaseAdapter() {
        override fun getCount(): Int = apps.size
        override fun getItem(position: Int): Any = apps[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val e = apps[position]
            val v: LinearLayout
            if (convertView is LinearLayout) {
                v = convertView
            } else {
                v = LinearLayout(this@MainActivity)
                v.orientation = LinearLayout.VERTICAL
                v.gravity = Gravity.CENTER_HORIZONTAL
                v.setPadding(dp(8), dp(12), dp(8), dp(12))
                v.addView(ImageView(this@MainActivity), LinearLayout.LayoutParams(dp(72), dp(72)))
                val tv = TextView(this@MainActivity)
                tv.setTextColor(moon)
                tv.textSize = 14f
                tv.typeface = Typeface.SERIF
                tv.gravity = Gravity.CENTER
                tv.maxLines = 1
                tv.ellipsize = TextUtils.TruncateAt.END
                v.addView(tv, LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            }
            (v.getChildAt(0) as ImageView).setImageDrawable(e.info.loadIcon(packageManager))
            (v.getChildAt(1) as TextView).text = e.label
            return v
        }
    }
}
