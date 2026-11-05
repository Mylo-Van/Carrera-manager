package com.fccareer.manager

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

// ---------- Modelo ----------
class SS(val season: String, val pj: Int, val g: Int, val a: Int, val pc: Int)

class Player(
    var id: Long = System.nanoTime(), var name: String = "", var nat: String = "", var age: Int = 18,
    var pos: String = "", var num: String = "", var bio: String = "", var cantera: Boolean = false,
    var sold: Boolean = false, var photo: String = "", var pj: Int = 0, var g: Int = 0, var a: Int = 0,
    var pc: Int = 0, val hist: MutableList<SS> = mutableListOf()
) {
    fun tot(k: String): Int {
        val h = when (k) { "pj" -> hist.sumOf { it.pj }; "g" -> hist.sumOf { it.g }; "a" -> hist.sumOf { it.a }; else -> hist.sumOf { it.pc } }
        val c = when (k) { "pj" -> pj; "g" -> g; "a" -> a; else -> pc }
        return h + c
    }
}

class Title(val comp: String, val season: String)
class Final(val comp: String, val season: String, val rival: String, val score: String, val res: String)
class Summ(val season: String, val txt: String)
class Rec(val key: String, val label: String, val v: Int, val who: String, val season: String)

class Career(
    val id: Long = System.nanoTime(), var club: String = "", var country: String = "", var coach: String = "",
    var season: String = "", var color: Int = 0xFF0A8F5B.toInt(), var crest: String = "", var bg: String = "",
    var coachPhoto: String = "", val players: MutableList<Player> = mutableListOf(),
    val titles: MutableList<Title> = mutableListOf(), val finals: MutableList<Final> = mutableListOf(),
    val summ: MutableList<Summ> = mutableListOf(), val rec: MutableList<Rec> = mutableListOf()
)

val PALETTE = listOf(0xFFC62828, 0xFF1565C0, 0xFF0A8F5B, 0xFFF9A825, 0xFF6A1B9A, 0xFF37474F).map { it.toInt() }

// ---------- JSON ----------
fun JSONArray.objs(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

fun Player.toJ(): JSONObject = JSONObject().put("id", id).put("name", name).put("nat", nat).put("age", age)
    .put("pos", pos).put("num", num).put("bio", bio).put("cantera", cantera).put("sold", sold).put("photo", photo)
    .put("pj", pj).put("g", g).put("a", a).put("pc", pc)
    .put("hist", JSONArray(hist.map { JSONObject().put("s", it.season).put("pj", it.pj).put("g", it.g).put("a", it.a).put("pc", it.pc) }))

fun playerFrom(o: JSONObject): Player = Player(
    o.optLong("id"), o.optString("name"), o.optString("nat"), o.optInt("age"), o.optString("pos"),
    o.optString("num"), o.optString("bio"), o.optBoolean("cantera"), o.optBoolean("sold"), o.optString("photo"),
    o.optInt("pj"), o.optInt("g"), o.optInt("a"), o.optInt("pc"),
    (o.optJSONArray("hist")?.objs()?.map { SS(it.optString("s"), it.optInt("pj"), it.optInt("g"), it.optInt("a"), it.optInt("pc")) } ?: emptyList()).toMutableList()
)

fun Career.toJ(): JSONObject = JSONObject().put("id", id).put("club", club).put("country", country).put("coach", coach)
    .put("season", season).put("color", color).put("crest", crest).put("bg", bg).put("cp", coachPhoto)
    .put("players", JSONArray(players.map { it.toJ() }))
    .put("titles", JSONArray(titles.map { JSONObject().put("c", it.comp).put("s", it.season) }))
    .put("finals", JSONArray(finals.map { JSONObject().put("c", it.comp).put("s", it.season).put("r", it.rival).put("sc", it.score).put("res", it.res) }))
    .put("summ", JSONArray(summ.map { JSONObject().put("s", it.season).put("t", it.txt) }))
    .put("rec", JSONArray(rec.map { JSONObject().put("k", it.key).put("l", it.label).put("v", it.v).put("w", it.who).put("s", it.season) }))

fun careerFrom(o: JSONObject): Career = Career(
    o.optLong("id"), o.optString("club"), o.optString("country"), o.optString("coach"), o.optString("season"),
    o.optInt("color"), o.optString("crest"), o.optString("bg"), o.optString("cp"),
    (o.optJSONArray("players")?.objs()?.map { playerFrom(it) } ?: emptyList()).toMutableList(),
    (o.optJSONArray("titles")?.objs()?.map { Title(it.optString("c"), it.optString("s")) } ?: emptyList()).toMutableList(),
    (o.optJSONArray("finals")?.objs()?.map { Final(it.optString("c"), it.optString("s"), it.optString("r"), it.optString("sc"), it.optString("res")) } ?: emptyList()).toMutableList(),
    (o.optJSONArray("summ")?.objs()?.map { Summ(it.optString("s"), it.optString("t")) } ?: emptyList()).toMutableList(),
    (o.optJSONArray("rec")?.objs()?.map { Rec(it.optString("k"), it.optString("l"), it.optInt("v"), it.optString("w"), it.optString("s")) } ?: emptyList()).toMutableList()
)

// ---------- Almacén ----------
class Store(ctx: Context) {
    var v by mutableIntStateOf(0)
    var theme by mutableIntStateOf(0)
    var cur by mutableStateOf<Career?>(null)
    val careers = mutableListOf<Career>()
    private val f = File(ctx.filesDir, "db.json")

    init { if (f.exists()) load(f.readText()) }

    fun load(t: String?) {
        if (t == null) return
        try {
            val o = JSONObject(t)
            val list = o.getJSONArray("careers").objs().map { careerFrom(it) }
            careers.clear(); careers.addAll(list)
            theme = o.optInt("theme")
            val id = o.optLong("cur")
            cur = careers.find { it.id == id }
        } catch (e: Exception) { }
    }

    fun json(): String = JSONObject().put("theme", theme).put("cur", cur?.id ?: 0L)
        .put("careers", JSONArray(careers.map { it.toJ() })).toString()

    fun mut(b: () -> Unit) {
        b()
        try { f.writeText(json()) } catch (e: Exception) { }
        v++
    }
}

fun checkRec(c: Career) {
    fun up(k: String, l: String, v: Int, who: String) {
        if (v <= 0) return
        val r = c.rec.find { it.key == k }
        if (r == null) c.rec.add(Rec(k, l, v, who, c.season))
        else if (v > r.v) { c.rec.remove(r); c.rec.add(Rec(k, l, v, who, c.season)) }
    }
    c.players.maxByOrNull { it.g }?.let { up("g", "Máximo goleador en una temporada", it.g, it.name) }
    c.players.maxByOrNull { it.a }?.let { up("a", "Máximo asistente en una temporada", it.a, it.name) }
    c.players.maxByOrNull { it.pj }?.let { up("pj", "Más partidos en una temporada", it.pj, it.name) }
    up("tg", "Más goles del equipo en una temporada", c.players.sumOf { it.g }, c.club)
}

fun bmp(p: String): ImageBitmap? {
    if (p.isEmpty()) return null
    return try {
        val o = BitmapFactory.Options(); o.inSampleSize = 4
        BitmapFactory.decodeFile(p, o)?.asImageBitmap()
    } catch (e: Exception) { null }
}

// ---------- Actividad ----------
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val s = Store(applicationContext)
        setContent { App(s) }
    }
}

@Composable
fun App(s: Store) {
    val tick = s.v
    val c = s.cur
    val prim = when {
        s.theme == 2 -> Color(0xFF4CD08C)
        s.theme == 0 && c != null -> Color(c.color)
        else -> Color(0xFF0A8F5B)
    }
    val cs = if (s.theme == 2) darkColorScheme(primary = prim) else lightColorScheme(primary = prim)
    MaterialTheme(colorScheme = cs) {
        Surface(Modifier.fillMaxSize()) {
            if (c == null) Home(s) else CareerUI(s, c)
        }
    }
}

// ---------- Componentes ----------
@Composable
fun F(label: String, v: String, num: Boolean = false, on: (String) -> Unit) {
    OutlinedTextField(
        value = v, onValueChange = on, modifier = Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (num) KeyboardType.Number else KeyboardType.Text)
    )
}

@Composable
fun Pic(path: String, size: Dp, shape: Shape = CircleShape) {
    val b = remember(path) { bmp(path) }
    Box(Modifier.size(size).clip(shape).background(Color.Gray.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
        if (b != null) Image(bitmap = b, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Text("⚽")
    }
}

@Composable
fun Pick(label: String, path: String, onSet: (String) -> Unit) {
    val ctx = LocalContext.current
    val l = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { u: Uri? ->
        if (u != null) {
            try {
                val out = File(ctx.filesDir, "img_${System.nanoTime()}.jpg")
                ctx.contentResolver.openInputStream(u)?.use { i -> out.outputStream().use { o -> i.copyTo(o) } }
                onSet(out.absolutePath)
            } catch (e: Exception) { }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Pic(path, 48.dp)
        Spacer(Modifier.width(8.dp))
        OutlinedButton(onClick = { l.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text(label) }
    }
}

@Composable
fun Form(title: String, ok: Boolean, onClose: () -> Unit, onOk: () -> Unit, body: @Composable ColumnScope.() -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = { TextButton(onClick = onOk, enabled = ok) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onClose) { Text("Cancelar") } },
        title = { Text(title) },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp), content = body) }
    )
}

@Composable
fun Confirm(msg: String, onNo: () -> Unit, onYes: () -> Unit) {
    AlertDialog(
        onDismissRequest = onNo,
        confirmButton = { TextButton(onClick = onYes) { Text("Sí") } },
        dismissButton = { TextButton(onClick = onNo) { Text("No") } },
        text = { Text(msg) }
    )
}

@Composable
fun Backup(s: Store) {
    val ctx = LocalContext.current
    val ex = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { u: Uri? ->
        if (u != null) {
            try { ctx.contentResolver.openOutputStream(u)?.use { it.write(s.json().toByteArray()) } } catch (e: Exception) { }
        }
    }
    val im = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u: Uri? ->
        if (u != null) {
            try {
                val t = ctx.contentResolver.openInputStream(u)?.bufferedReader()?.use { it.readText() }
                s.mut { s.load(t) }
            } catch (e: Exception) { }
        }
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Copia de seguridad", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { ex.launch("fc-career-backup.json") }) { Text("Exportar") }
                OutlinedButton(onClick = { im.launch(arrayOf("*/*")) }) { Text("Importar") }
            }
            Text("Tema", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Equipo", "Blanco", "Oscuro").forEachIndexed { i, n ->
                    FilterChip(selected = s.theme == i, onClick = { s.mut { s.theme = i } }, label = { Text(n) })
                }
            }
        }
    }
}

// ---------- Inicio ----------
@Composable
fun Home(s: Store) {
    var del by remember { mutableStateOf<Career?>(null) }
    Scaffold { pad ->
        LazyColumn(Modifier.padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text("⚽ FC Career Manager", fontSize = 24.sp, fontWeight = FontWeight.Bold) }
            items(s.careers.toList()) { c ->
                Card(Modifier.fillMaxWidth().clickable { s.mut { s.cur = c } }) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Pic(c.crest, 48.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(c.club, fontWeight = FontWeight.Bold)
                            Text("DT ${c.coach} · ${c.season} · ${c.titles.size} títulos")
                        }
                        TextButton(onClick = { del = c }) { Text("Borrar") }
                    }
                }
            }
            item { NewCareer(s) }
            item { Backup(s) }
        }
    }
    del?.let { d -> Confirm("¿Borrar la carrera de ${d.club}?", { del = null }) { s.mut { s.careers.remove(d) }; del = null } }
}

@Composable
fun NewCareer(s: Store) {
    var club by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var coach by remember { mutableStateOf("") }
    var season by remember { mutableStateOf("2025/26") }
    var crest by remember { mutableStateOf("") }
    var color by remember { mutableStateOf(PALETTE[0]) }
    var pick by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Nueva carrera", fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = { pick = true }, modifier = Modifier.fillMaxWidth()) { Text("📚 Elegir equipo del catálogo") }
            F("Club (o escribilo a mano)", club) { club = it }
            F("País · Liga", country) { country = it }
            F("Nombre del DT", coach) { coach = it }
            F("Temporada", season) { season = it }
            Pick("Escudo", crest) { crest = it }
            if (pick) TeamPicker({ pick = false }) { t, l -> club = t; country = l; pick = false }
            Text("Color del equipo")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PALETTE.forEach { col ->
                    Box(
                        Modifier.size(32.dp).clip(CircleShape).background(Color(col))
                            .then(if (col == color) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                            .clickable { color = col }
                    )
                }
            }
            Button(onClick = {
                s.mut {
                    val c = Career(club = club, country = country, coach = coach, season = season, color = color, crest = crest)
                    s.careers.add(c); s.cur = c
                }
            }, enabled = club.isNotBlank()) { Text("Crear carrera") }
        }
    }
}

// ---------- Carrera ----------
@Composable
fun CareerUI(s: Store, c: Career) {
    var tab by remember { mutableIntStateOf(0) }
    var sel by remember { mutableStateOf<Player?>(null) }
    val tabs = listOf("👥" to "Plantel", "📊" to "Top 10", "🏆" to "Títulos", "🥇" to "Récords", "📅" to "Temporada", "⚙️" to "Más")
    Scaffold(bottomBar = {
        NavigationBar {
            tabs.forEachIndexed { i, t ->
                NavigationBarItem(
                    selected = tab == i && sel == null, onClick = { tab = i; sel = null },
                    icon = { Text(t.first) }, label = { Text(t.second, fontSize = 10.sp, maxLines = 1) }
                )
            }
        }
    }) { pad ->
        Column(Modifier.padding(pad)) {
            Header(c)
            Box(Modifier.weight(1f)) {
                val p = sel
                if (p != null) PlayerDetail(s, c, p) { sel = null }
                else when (tab) {
                    0 -> Squad(s, c) { sel = it }
                    1 -> Top(c) { sel = it }
                    2 -> Titles(s, c)
                    3 -> Records(c)
                    4 -> Season(s, c)
                    else -> More(s, c)
                }
            }
        }
    }
}

@Composable
fun Header(c: Career) {
    Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary)) {
        val b = remember(c.bg) { bmp(c.bg) }
        if (b != null) Image(bitmap = b, contentDescription = null, modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop, alpha = 0.35f)
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Pic(c.crest, 56.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(c.club, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("DT ${c.coach} · Temporada ${c.season}", color = Color.White, fontSize = 13.sp)
                Text("🏆 ${c.titles.size} títulos · 🏟️ ${c.finals.size} finales", color = Color.White, fontSize = 13.sp)
            }
            Pic(c.coachPhoto, 44.dp)
        }
    }
}

@Composable
fun PlayerDialog(init: Player?, onClose: () -> Unit, onSave: (Player) -> Unit) {
    var name by remember { mutableStateOf(init?.name ?: "") }
    var nat by remember { mutableStateOf(init?.nat ?: "") }
    var age by remember { mutableStateOf((init?.age ?: 18).toString()) }
    var pos by remember { mutableStateOf(init?.pos ?: "") }
    var num by remember { mutableStateOf(init?.num ?: "") }
    var bio by remember { mutableStateOf(init?.bio ?: "") }
    var can by remember { mutableStateOf(init?.cantera ?: false) }
    var photo by remember { mutableStateOf(init?.photo ?: "") }
    Form(if (init == null) "Nuevo jugador" else "Editar jugador", name.isNotBlank(), onClose, {
        val p = init ?: Player()
        p.name = name; p.nat = nat; p.age = age.toIntOrNull() ?: 18; p.pos = pos; p.num = num
        p.bio = bio; p.cantera = can; p.photo = photo
        onSave(p)
    }) {
        F("Nombre", name) { name = it }
        F("Nacionalidad", nat) { nat = it }
        F("Edad", age, true) { age = it }
        F("Posición", pos) { pos = it }
        F("Número de camiseta", num, true) { num = it }
        F("Biografía / historia", bio) { bio = it }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = can, onCheckedChange = { can = it })
            Spacer(Modifier.width(8.dp))
            Text("🌱 Canterano")
        }
        Pick("Foto", photo) { photo = it }
    }
}

@Composable
fun Squad(s: Store, c: Career, open: (Player) -> Unit) {
    var dlg by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Button(onClick = { dlg = true }, modifier = Modifier.fillMaxWidth()) { Text("+ Agregar jugador / canterano") } }
        items(c.players.filter { !it.sold }) { p ->
            Card(Modifier.fillMaxWidth().clickable { open(p) }) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Pic(p.photo, 44.dp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text((if (p.cantera) "🌱 " else "") + p.name, fontWeight = FontWeight.Bold)
                        Text("${p.pos} · ${p.nat} · ${p.age} años · #${p.num}", fontSize = 13.sp)
                    }
                }
            }
        }
    }
    if (dlg) PlayerDialog(null, { dlg = false }) { p -> s.mut { c.players.add(p) }; dlg = false }
}

@Composable
fun PlayerDetail(s: Store, c: Career, p: Player, back: () -> Unit) {
    var edit by remember { mutableStateOf(false) }
    var del by remember { mutableStateOf(false) }
    var pj by remember(p.id) { mutableStateOf(p.pj.toString()) }
    var g by remember(p.id) { mutableStateOf(p.g.toString()) }
    var a by remember(p.id) { mutableStateOf(p.a.toString()) }
    var pc by remember(p.id) { mutableStateOf(p.pc.toString()) }
    LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { TextButton(onClick = back) { Text("← Volver") } }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pic(p.photo, 72.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text((if (p.cantera) "🌱 " else "") + p.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("${p.pos} · ${p.nat} · ${p.age} años · #${p.num}")
                    if (p.sold) Text("Vendido", color = MaterialTheme.colorScheme.error)
                }
            }
        }
        if (p.bio.isNotBlank()) item { Text(p.bio) }
        item { Text("Temporada ${c.season}", fontWeight = FontWeight.Bold) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.weight(1f)) { F("PJ", pj, true) { pj = it } }
                Box(Modifier.weight(1f)) { F("G", g, true) { g = it } }
                Box(Modifier.weight(1f)) { F("A", a, true) { a = it } }
                Box(Modifier.weight(1f)) { F("PC", pc, true) { pc = it } }
            }
        }
        item {
            Button(onClick = {
                s.mut {
                    p.pj = pj.toIntOrNull() ?: 0; p.g = g.toIntOrNull() ?: 0
                    p.a = a.toIntOrNull() ?: 0; p.pc = pc.toIntOrNull() ?: 0
                    checkRec(c)
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Guardar estadísticas") }
        }
        item { Text("Histórico en el club: ${p.tot("pj")} PJ · ${p.tot("g")} G · ${p.tot("a")} A · ${p.tot("pc")} PC", fontWeight = FontWeight.Bold) }
        items(p.hist.toList()) { h -> Text("${h.season}: ${h.pj} PJ · ${h.g} G · ${h.a} A · ${h.pc} PC") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { edit = true }) { Text("Editar") }
                OutlinedButton(onClick = { s.mut { p.sold = !p.sold } }) { Text(if (p.sold) "Recomprar" else "Vender") }
                OutlinedButton(onClick = { del = true }) { Text("Eliminar") }
            }
        }
    }
    if (edit) PlayerDialog(p, { edit = false }) { s.mut { }; edit = false }
    if (del) Confirm("¿Eliminar a ${p.name} y todo su historial?", { del = false }) { s.mut { c.players.remove(p) }; del = false; back() }
}

@Composable
fun Top(c: Career, open: (Player) -> Unit) {
    var q by remember { mutableStateOf("") }
    val lists = listOf("pj" to "Más partidos jugados", "g" to "Máximos goleadores", "a" to "Máximos asistentes")
    fun top(k: String): List<Player> = c.players.filter { it.tot(k) > 0 }.sortedByDescending { it.tot(k) }.take(10)
    val inTop = lists.flatMap { top(it.first) }.toSet()
    LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(lists) { (k, t) ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(t, fontWeight = FontWeight.Bold)
                    val l = top(k)
                    var pos = 0
                    var prev = -1
                    l.forEachIndexed { i, p ->
                        val v = p.tot(k)
                        if (v != prev) { pos = i + 1; prev = v }
                        val col = when (pos) { 1 -> Color(0xFFD4A017); 2 -> Color(0xFF9E9E9E); 3 -> Color(0xFFB36B2E); else -> LocalContentColor.current }
                        Row(Modifier.fillMaxWidth().clickable { open(p) }.padding(vertical = 4.dp)) {
                            Text("$pos", Modifier.width(28.dp), fontWeight = FontWeight.Bold, color = col)
                            Text((if (p.cantera) "🌱 " else "") + p.name + (if (p.sold) " (ex)" else ""), Modifier.weight(1f))
                            Text("$v", fontWeight = FontWeight.Bold)
                        }
                    }
                    if (l.isEmpty()) Text("Sin datos todavía")
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Exjugadores", fontWeight = FontWeight.Bold)
                    F("Buscar por nombre", q) { q = it }
                    val ex = c.players.filter { it.sold && it !in inTop && it.name.contains(q, true) }
                    ex.forEach { p ->
                        Text("${p.name} · ${p.tot("pj")} PJ · ${p.tot("g")} G · ${p.tot("a")} A",
                            Modifier.fillMaxWidth().clickable { open(p) }.padding(vertical = 6.dp))
                    }
                    if (ex.isEmpty()) Text("Ninguno")
                }
            }
        }
    }
}

@Composable
fun Titles(s: Store, c: Career) {
    var dT by remember { mutableStateOf(false) }
    var dF by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { dT = true }) { Text("+ Título") }
                Button(onClick = { dF = true }) { Text("+ Final") }
            }
        }
        item { Text("Títulos del club (${c.titles.size})", fontWeight = FontWeight.Bold) }
        items(c.titles.toList()) { t ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pic(c.crest, 32.dp)
                Spacer(Modifier.width(8.dp))
                Text("🏆 ${t.comp} · ${t.season}", Modifier.weight(1f))
                TextButton(onClick = { s.mut { c.titles.remove(t) } }) { Text("✕") }
            }
        }
        item { Text("Finales", fontWeight = FontWeight.Bold) }
        c.finals.groupBy { it.season }.forEach { (sea, list) ->
            item { Text(sea, fontWeight = FontWeight.Bold) }
            items(list) { f ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Pic(c.crest, 32.dp)
                        Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                            Text(f.comp)
                            Text("${c.club} ${f.score} ${f.rival} — ${f.res}", fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = { s.mut { c.finals.remove(f) } }) { Text("✕") }
                    }
                }
            }
        }
    }
    if (dT) {
        var comp by remember { mutableStateOf("") }
        var sea by remember { mutableStateOf(c.season) }
        Form("Nuevo título", comp.isNotBlank(), { dT = false }, { s.mut { c.titles.add(Title(comp, sea)) }; dT = false }) {
            F("Competición (Liga, Champions League…)", comp) { comp = it }
            F("Temporada", sea) { sea = it }
        }
    }
    if (dF) {
        var comp by remember { mutableStateOf("") }
        var sea by remember { mutableStateOf(c.season) }
        var rival by remember { mutableStateOf("") }
        var score by remember { mutableStateOf("") }
        var res by remember { mutableStateOf("Ganada") }
        Form("Nueva final", comp.isNotBlank(), { dF = false }, { s.mut { c.finals.add(Final(comp, sea, rival, score, res)) }; dF = false }) {
            F("Competición", comp) { comp = it }
            F("Temporada", sea) { sea = it }
            F("Rival", rival) { rival = it }
            F("Marcador (ej. 2-1)", score) { score = it }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = res == "Ganada", onClick = { res = "Ganada" }, label = { Text("Ganada") })
                FilterChip(selected = res == "Perdida", onClick = { res = "Perdida" }, label = { Text("Perdida") })
            }
        }
    }
}

@Composable
fun Records(c: Career) {
    LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("Récords históricos del club", fontWeight = FontWeight.Bold) }
        if (c.rec.isEmpty()) item { Text("Se detectan solos cuando guardás estadísticas.") }
        items(c.rec.toList()) { r ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(r.label, fontWeight = FontWeight.Bold)
                    Text("${r.who} · ${r.v} · ${r.season}")
                }
            }
        }
    }
}

@Composable
fun Season(s: Store, c: Career) {
    var name by remember { mutableStateOf("") }
    var txt by remember { mutableStateOf("") }
    var ask by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Nueva temporada", fontWeight = FontWeight.Bold)
                    Text("Suma 1 año a cada jugador, guarda las estadísticas en el histórico y las reinicia.", fontSize = 13.sp)
                    F("Resumen de ${c.season}", txt) { txt = it }
                    F("Nombre de la nueva (ej. 2026/27)", name) { name = it }
                    Button(onClick = { ask = true }, enabled = name.isNotBlank()) { Text("Comenzar nueva temporada") }
                }
            }
        }
        items(c.summ.reversed()) { x ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(x.season, fontWeight = FontWeight.Bold)
                    Text(x.txt)
                }
            }
        }
    }
    if (ask) Confirm("¿Cerrar la temporada ${c.season} y comenzar ${name}?", { ask = false }) {
        s.mut {
            c.players.forEach { p ->
                p.hist.add(SS(c.season, p.pj, p.g, p.a, p.pc))
                p.pj = 0; p.g = 0; p.a = 0; p.pc = 0
                if (!p.sold) p.age += 1
            }
            c.summ.add(Summ(c.season, txt))
            c.season = name
        }
        name = ""; txt = ""; ask = false
    }
}

@Composable
fun More(s: Store, c: Career) {
    var club by remember { mutableStateOf(c.club) }
    var coach by remember { mutableStateOf(c.coach) }
    LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Editar carrera", fontWeight = FontWeight.Bold)
                    F("Club", club) { club = it }
                    F("Entrenador", coach) { coach = it }
                    Button(onClick = { s.mut { c.club = club; c.coach = coach } }) { Text("Guardar nombres") }
                    Pick("Escudo del club", c.crest) { p -> s.mut { c.crest = p } }
                    Pick("Foto del DT", c.coachPhoto) { p -> s.mut { c.coachPhoto = p } }
                    Pick("Fondo de estadio", c.bg) { p -> s.mut { c.bg = p } }
                    Text("Color del equipo")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PALETTE.forEach { col ->
                            Box(Modifier.size(32.dp).clip(CircleShape).background(Color(col)).clickable { s.mut { c.color = col } })
                        }
                    }
                }
            }
        }
        item { Backup(s) }
        item { OutlinedButton(onClick = { s.mut { s.cur = null } }, modifier = Modifier.fillMaxWidth()) { Text("Mis carreras") } }
    }
}

// ---------- Catálogo ----------
class League(val country: String, val name: String, val teams: List<String>)

val LEAGUES: List<League> = CATALOG_RAW.trim().lines().filter { it.isNotBlank() }.map {
    val p = it.split("|")
    League(p[0].trim(), p[1].trim(), p[2].split(";").map { t -> t.trim() })
}

@Composable
fun TeamPicker(onClose: () -> Unit, onPick: (String, String) -> Unit) {
    var country by remember { mutableStateOf<String?>(null) }
    var league by remember { mutableStateOf<League?>(null) }
    var q by remember { mutableStateOf("") }
    val countries = remember { LEAGUES.map { it.country }.distinct() }
    val co = country
    val lg = league
    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = {
            TextButton(onClick = {
                if (lg != null) league = null else if (co != null) country = null else onClose()
            }) { Text(if (co == null) "Cerrar" else "← Atrás") }
        },
        title = { Text(lg?.name ?: co ?: "Elegí tu equipo") },
        text = {
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                if (lg != null) {
                    items(lg.teams) { t ->
                        Text(t, Modifier.fillMaxWidth().clickable { onPick(t, "${lg.country} · ${lg.name}") }.padding(vertical = 10.dp))
                    }
                } else if (co != null) {
                    items(LEAGUES.filter { it.country == co }) { l ->
                        Text("${l.name} (${l.teams.size})", Modifier.fillMaxWidth().clickable { league = l }.padding(vertical = 10.dp))
                    }
                } else {
                    item { F("Buscar equipo", q) { q = it } }
                    if (q.length >= 2) {
                        val res = LEAGUES.flatMap { l -> l.teams.filter { it.contains(q, true) }.map { t -> Pair(l, t) } }
                        items(res.take(30)) { r ->
                            Text("${r.second} — ${r.first.name}", Modifier.fillMaxWidth()
                                .clickable { onPick(r.second, "${r.first.country} · ${r.first.name}") }.padding(vertical = 10.dp))
                        }
                    } else {
                        items(countries) { c ->
                            Text(c, Modifier.fillMaxWidth().clickable { country = c }.padding(vertical = 10.dp))
                        }
                    }
                }
            }
        }
    )
}

const val CATALOG_RAW = """
Inglaterra|Premier League|Arsenal;Aston Villa;Bournemouth;Brentford;Brighton;Chelsea;Crystal Palace;Everton;Fulham;Ipswich Town;Leicester City;Liverpool;Manchester City;Manchester United;Newcastle United;Nottingham Forest;Southampton;Tottenham Hotspur;West Ham United;Wolverhampton
Inglaterra|Championship|Blackburn Rovers;Bristol City;Burnley;Cardiff City;Coventry City;Derby County;Hull City;Leeds United;Luton Town;Middlesbrough;Millwall;Norwich City;Oxford United;Plymouth Argyle;Portsmouth;Preston North End;Queens Park Rangers;Sheffield United;Sheffield Wednesday;Stoke City;Sunderland;Swansea City;Watford;West Bromwich Albion
Inglaterra|League One|Barnsley;Birmingham City;Blackpool;Bolton Wanderers;Bristol Rovers;Burton Albion;Cambridge United;Charlton Athletic;Crawley Town;Exeter City;Huddersfield Town;Leyton Orient;Lincoln City;Mansfield Town;Northampton Town;Peterborough United;Reading;Rotherham United;Shrewsbury Town;Stevenage;Stockport County;Wigan Athletic;Wrexham;Wycombe Wanderers
Inglaterra|League Two|Accrington Stanley;AFC Wimbledon;Barrow;Bradford City;Bromley;Carlisle United;Cheltenham Town;Chesterfield;Colchester United;Crewe Alexandra;Doncaster Rovers;Fleetwood Town;Gillingham;Grimsby Town;Harrogate Town;Milton Keynes Dons;Morecambe;Newport County;Notts County;Port Vale;Salford City;Swindon Town;Tranmere Rovers;Walsall
España|LALIGA EA SPORTS|Alavés;Athletic Club;Atlético de Madrid;Barcelona;Celta de Vigo;Espanyol;Getafe;Girona;Las Palmas;Leganés;Mallorca;Osasuna;Rayo Vallecano;Real Betis;Real Madrid;Real Sociedad;Sevilla;Valencia;Real Valladolid;Villarreal
España|LALIGA HYPERMOTION|Albacete;Almería;Burgos;Cádiz;Cartagena;Castellón;Córdoba;Deportivo La Coruña;Eibar;Eldense;Elche;Granada;Huesca;Levante;Málaga;Mirandés;Real Oviedo;Racing Santander;Racing Ferrol;Sporting Gijón;Tenerife;Real Zaragoza
Alemania|Bundesliga|Augsburg;Bayer Leverkusen;Bayern München;Borussia Dortmund;Borussia Mönchengladbach;Eintracht Frankfurt;Freiburg;Heidenheim;Hoffenheim;Holstein Kiel;Mainz 05;RB Leipzig;St. Pauli;Stuttgart;Union Berlin;Werder Bremen;Wolfsburg;Bochum
Alemania|2. Bundesliga|Braunschweig;Darmstadt;Fortuna Düsseldorf;Elversberg;Greuther Fürth;Hamburger SV;Hannover 96;Hertha BSC;Kaiserslautern;Karlsruher SC;Köln;Magdeburg;Nürnberg;Paderborn;Preußen Münster;Schalke 04;Ulm;Jahn Regensburg
Alemania|3. Liga|Alemannia Aachen;Arminia Bielefeld;Dynamo Dresden;Erzgebirge Aue;Hansa Rostock;Hannover 96 II;Ingolstadt;Mannheim;Oldenburg;Osnabrück;Saarbrücken;Sandhausen;1860 München;Verl;Viktoria Köln;Wehen Wiesbaden;Freiburg II;Borussia Dortmund II;Stuttgart II;Rot-Weiss Essen
Italia|Serie A|Atalanta;Bologna;Cagliari;Como;Empoli;Fiorentina;Genoa;Hellas Verona;Inter;Juventus;Lazio;Lecce;Milan;Monza;Napoli;Parma;Roma;Torino;Udinese;Venezia
Italia|Serie B|Bari;Brescia;Carrarese;Catanzaro;Cesena;Cittadella;Cosenza;Cremonese;Frosinone;Juve Stabia;Mantova;Modena;Palermo;Pisa;Reggiana;Salernitana;Sampdoria;Sassuolo;Spezia;Südtirol
Francia|Ligue 1|Angers;Auxerre;Brest;Le Havre;Lens;Lille;Lyon;Marseille;Monaco;Montpellier;Nantes;Nice;Paris Saint-Germain;Reims;Rennes;Saint-Étienne;Strasbourg;Toulouse
Francia|Ligue 2|Ajaccio;Amiens;Annecy;Bastia;Caen;Clermont;Dunkerque;Grenoble;Guingamp;Laval;Lorient;Martigues;Metz;Paris FC;Pau;Red Star;Rodez;Troyes
Países Bajos|Eredivisie|Ajax;AZ Alkmaar;Feyenoord;PSV;FC Twente;FC Utrecht;Sparta Rotterdam;Go Ahead Eagles;Heerenveen;NEC Nijmegen;Fortuna Sittard;Heracles;PEC Zwolle;Groningen;Willem II;RKC Waalwijk;Almere City;NAC Breda
Portugal|Liga Portugal|Benfica;Sporting CP;Porto;Braga;Vitória SC;Famalicão;Moreirense;Estoril;Casa Pia;Rio Ave;Santa Clara;Gil Vicente;Arouca;Boavista;Farense;Nacional;Estrela da Amadora;AVS
Escocia|Scottish Premiership|Celtic;Rangers;Aberdeen;Heart of Midlothian;Hibernian;Dundee;Dundee United;Kilmarnock;Motherwell;Ross County;St Johnstone;St Mirren
Bélgica|Jupiler Pro League|Club Brugge;Anderlecht;Union Saint-Gilloise;Genk;Gent;Antwerp;Standard Liège;Cercle Brugge;Mechelen;Westerlo;Charleroi;Sint-Truiden;OH Leuven;Kortrijk;Dender;Beerschot
Turquía|Süper Lig|Galatasaray;Fenerbahçe;Beşiktaş;Trabzonspor;Başakşehir;Adana Demirspor;Antalyaspor;Alanyaspor;Bodrum;Eyüpspor;Gaziantep;Göztepe;Hatayspor;Kasımpaşa;Kayserispor;Konyaspor;Çaykur Rizespor;Samsunspor;Sivasspor;Ankaragücü
Suiza|Super League|Young Boys;Basel;Servette;Zürich;Lugano;Lausanne-Sport;St. Gallen;Luzern;Sion;Grasshoppers;Winterthur;Yverdon
Austria|Bundesliga|Red Bull Salzburg;Sturm Graz;LASK;Rapid Wien;Austria Wien;Wolfsberger AC;Hartberg;WSG Tirol;Blau-Weiß Linz;Altach;Austria Klagenfurt;Grazer AK
Dinamarca|Superliga|FC Copenhagen;Midtjylland;Brøndby;Nordsjælland;AGF;Silkeborg;Randers;Viborg;Lyngby;Vejle;Hvidovre;Sønderjyske
Noruega|Eliteserien|Bodø/Glimt;Brann;Molde;Rosenborg;Viking;Lillestrøm;Tromsø;Sarpsborg 08;Fredrikstad;Haugesund;Strømsgodset;Kristiansund;Sandefjord;HamKam;Odd;KFUM Oslo
Suecia|Allsvenskan|Malmö FF;AIK;Djurgården;Hammarby;IFK Göteborg;BK Häcken;Elfsborg;Mjällby;Brommapojkarna;Kalmar;Halmstad;Norrköping;Sirius;Värnamo;Degerfors;GAIS
Polonia|Ekstraklasa|Legia Warszawa;Lech Poznań;Raków Częstochowa;Jagiellonia Białystok;Pogoń Szczecin;Górnik Zabrze;Cracovia;Śląsk Wrocław;Widzew Łódź;Piast Gliwice;Zagłębie Lubin;Korona Kielce;Motor Lublin;Radomiak;Puszcza Niepołomice;Lechia Gdańsk;Stal Mielec;GKS Katowice
Irlanda|League of Ireland|Shelbourne;Shamrock Rovers;Derry City;Bohemians;St Patrick's Athletic;Drogheda United;Sligo Rovers;Waterford;Galway United;Dundalk
Arabia Saudita|Saudi Pro League|Al-Hilal;Al-Nassr;Al-Ittihad;Al-Ahli;Al-Shabab;Al-Taawoun;Al-Fayha;Al-Ettifaq;Al-Fateh;Al-Raed;Al-Khaleej;Damac;Al-Riyadh;Al-Okhdood;Al-Orobah;Al-Kholood;Al-Wehda;Al-Qadsiah
Estados Unidos|MLS|Atlanta United;Austin FC;Charlotte FC;Chicago Fire;FC Cincinnati;Colorado Rapids;Columbus Crew;FC Dallas;D.C. United;Houston Dynamo;Inter Miami;LA Galaxy;LAFC;Minnesota United;CF Montréal;Nashville SC;New England Revolution;New York City FC;New York Red Bulls;Orlando City;Philadelphia Union;Portland Timbers;Real Salt Lake;San Diego FC;San Jose Earthquakes;Seattle Sounders;Sporting Kansas City;St. Louis City;Toronto FC;Vancouver Whitecaps
Australia|A-League|Adelaide United;Brisbane Roar;Central Coast Mariners;Macarthur FC;Melbourne City;Melbourne Victory;Newcastle Jets;Perth Glory;Sydney FC;Wellington Phoenix;Western Sydney Wanderers;Western United
Corea del Sur|K League 1|Ulsan HD;Jeonbuk Hyundai;Pohang Steelers;FC Seoul;Gimcheon Sangmu;Gangwon FC;Suwon FC;Daegu FC;Incheon United;Jeju United;Daejeon Hana Citizen;Gwangju FC
Argentina|Liga Profesional|Argentinos Juniors;Atlético Tucumán;Banfield;Barracas Central;Belgrano;Boca Juniors;Central Córdoba;Defensa y Justicia;Estudiantes;Gimnasia La Plata;Godoy Cruz;Huracán;Independiente;Independiente Rivadavia;Instituto;Lanús;Newell's Old Boys;Platense;Racing Club;River Plate;Rosario Central;San Lorenzo;Sarmiento;Talleres;Tigre;Unión;Vélez Sarsfield;Deportivo Riestra
Colombia|Liga BetPlay|América de Cali;Atlético Nacional;Millonarios;Deportivo Cali;Junior;Independiente Santa Fe;Once Caldas;Deportes Tolima;Independiente Medellín;Atlético Bucaramanga;Envigado;Fortaleza CEIF;Alianza FC;Águilas Doradas;Boyacá Chicó;Jaguares;La Equidad;Deportivo Pereira;Deportivo Pasto;Llaneros
México|Liga MX|América;Atlas;Atlético San Luis;Cruz Azul;FC Juárez;Guadalajara;León;Mazatlán;Monterrey;Necaxa;Pachuca;Puebla;Pumas UNAM;Querétaro;Santos Laguna;Tigres UANL;Tijuana;Toluca
Brasil|Brasileirão Série A|Atlético Mineiro;Bahia;Botafogo;Ceará;Corinthians;Cruzeiro;Flamengo;Fluminense;Fortaleza;Grêmio;Internacional;Juventude;Mirassol;Palmeiras;Red Bull Bragantino;Santos;São Paulo;Sport;Vasco da Gama;Vitória
"""
