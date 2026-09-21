// Trivium · Guías y material de estudio (Panel de Padres)
//
// El padre sube la guía que su hijo está estudiando y Trivium la usa para
// decidir QUÉ practicar. La guía marca la PRIORIDAD; el desempeño del niño
// sigue determinando el DOMINIO. La política vive en la app
// (curriculum/StudyGuide.kt); aquí solo se captura el material y se muestra
// el avance.
//
// Los temas se escriben a mano por ahora. Cuando el análisis automático del
// documento esté listo rellenará ese campo y el resto del flujo no cambia:
// una guía siempre acaba siendo una lista de temas con una fecha opcional.

import {
  collection, getDocs, doc, setDoc, deleteDoc, serverTimestamp,
} from "https://www.gstatic.com/firebasejs/10.12.2/firebase-firestore.js";
import {
  getStorage, ref as storageRef, uploadBytes, getDownloadURL,
} from "https://www.gstatic.com/firebasejs/10.12.2/firebase-storage.js";

const $ = (id) => document.getElementById(id);
const MODE_LABEL = { EXAM_PREP: "📝 Examen", LEARNING: "🧠 Aprendizaje" };
const UPLOAD_ERROR = {
  "storage/retry-limit-exceeded":
    "no hubo conexión con Firebase Storage (¿está activado en el proyecto?)",
  "storage/unauthorized": "Storage lo rechazó (revisa storage.rules; el límite es 15 MB)",
};
const escapeHtml = (value) => String(value ?? "")
  .replace(/&/g, "&amp;")
  .replace(/</g, "&lt;")
  .replace(/>/g, "&gt;")
  .replace(/"/g, "&quot;")
  .replace(/'/g, "&#39;");
const safeFileUrl = (value) => {
  try {
    const url = new URL(String(value));
    return url.protocol === "https:" ? url.href : null;
  } catch (_) { return null; }
};

let db = null;
let storage = null;
let child = null; // { id, name }

const fmtDay = (s) => {
  if (!s) return "";
  const [y, m, d] = s.split("-");
  return `${d}/${m}/${y}`;
};

const daysToExam = (iso) => {
  if (!iso) return null;
  const hoy = new Date();
  hoy.setHours(0, 0, 0, 0);
  return Math.round((new Date(`${iso}T00:00:00`) - hoy) / 86400000);
};

const short = (skillId) => skillId.replace(/^sec1\./, "");

/** Se llama una vez al arrancar el panel. */
export function initGuides(app, firestore) {
  db = firestore;
  storage = getStorage(app);
  // El SDK reintenta los errores de red durante 10 minutos, y un bucket que
  // no existe (Storage sin activar) llega como error de red: el navegador
  // bloquea la subida por CORS. Así el fallo se conoce en segundos. Solo
  // limita los reintentos; una subida lenta que ya está en curso no se corta.
  storage.maxUploadRetryTime = 15000;

  $("toggle-upload").addEventListener("click", () => {
    $("guide-form").classList.toggle("hidden");
    $("guide-error").textContent = "";
  });

  $("g-cancel").addEventListener("click", () => {
    $("guide-form").classList.add("hidden");
    resetForm();
  });

  document.querySelectorAll('input[name="g-mode"]').forEach((r) =>
    r.addEventListener("change", syncDateField)
  );

  $("guide-form").addEventListener("submit", onSubmit);
}

/** Se llama al abrir el detalle de un hijo. */
export function openGuidesFor(childId, childName) {
  child = { id: childId, name: childName };
  $("guide-form").classList.add("hidden");
  $("guide-status").textContent = "";
  refresh();
}

// La fecha solo tiene sentido en modo examen.
function syncDateField() {
  const esExamen =
    document.querySelector('input[name="g-mode"]:checked').value === "EXAM_PREP";
  $("g-date-wrap").style.opacity = esExamen ? "1" : ".4";
  $("g-date").disabled = !esExamen;
}

// reset() vuelve a marcar "Preparar examen" sin disparar "change": sin esto
// la fecha se quedaba deshabilitada después de una guía de aprendizaje.
function resetForm() {
  $("guide-form").reset();
  syncDateField();
}

async function onSubmit(e) {
  e.preventDefault();
  if (!child) return;

  const err = $("guide-error");
  const btn = $("g-submit");
  err.textContent = "";

  const mode = document.querySelector('input[name="g-mode"]:checked').value;
  const topics = $("g-topics").value.split("\n").map((t) => t.trim()).filter(Boolean);
  const spellingWords = $("g-spelling-words").value
    .split(/[\n,]+/)
    .map((w) => w.trim().toLowerCase())
    .filter((w) => /^[a-z]{2,15}$/.test(w))
    .filter((w, i, all) => all.indexOf(w) === i)
    .slice(0, 20);
  const examDate = $("g-date").value;
  const correctTarget = Math.max(10, Math.min(100, Number($("g-correct-target").value) || 30));

  if (topics.length === 0) {
    err.textContent = "Escribe al menos un tema: es lo que Trivium usa para elegir los ejercicios.";
    return;
  }
  if (mode === "EXAM_PREP" && !examDate) {
    err.textContent = "Para preparar un examen hace falta la fecha.";
    return;
  }

  btn.disabled = true;
  btn.textContent = "Guardando…";
  try {
    const id = `g_${Date.now()}`;
    const guideRef = doc(db, "children", child.id, "guides", id);
    const file = $("g-file").files[0];

    // La guía se guarda sin esperar al archivo: los temas son lo que de
    // verdad usa el motor. El archivo es OPCIONAL y se adjunta después.
    await setDoc(guideRef, {
      title: $("g-title").value.trim(),
      subject: $("g-subject").value,
      mode,
      examDate: mode === "EXAM_PREP" ? examDate : null,
      topics,
      spellingWords,
      correctTarget,
      fileUrl: null,
      fileName: null,
      paused: false,
      createdAt: serverTimestamp(),
    });

    resetForm();
    $("guide-form").classList.add("hidden");
    refresh();
    if (file) attachFile(guideRef, child.id, id, file);
  } catch (e2) {
    console.error(e2);
    err.textContent = "No se pudo guardar la guía. Revisa las reglas de Firestore.";
  } finally {
    btn.disabled = false;
    btn.textContent = "Guardar guía";
  }
}

/**
 * Sube el archivo de una guía que ya está guardada y le anota el enlace.
 * Corre en segundo plano porque el formulario ya se cerró: el avance y el
 * resultado se ven en #guide-status.
 */
async function attachFile(guideRef, childId, guideId, file) {
  const status = (text, warn = false) => {
    if (child?.id !== childId) return; // el padre ya está viendo a otro hijo
    $("guide-status").textContent = text;
    $("guide-status").classList.toggle("warn", warn);
  };
  status(`Subiendo «${file.name}»…`);
  try {
    const path = `guides/${childId}/${guideId}_${file.name}`;
    const snap = await uploadBytes(storageRef(storage, path), file);
    await setDoc(guideRef, {
      fileUrl: await getDownloadURL(snap.ref),
      fileName: file.name,
    }, { merge: true });
    status("");
    if (child?.id === childId) refresh();
  } catch (upErr) {
    console.warn("No se pudo subir el archivo:", upErr);
    const motivo = UPLOAD_ERROR[upErr?.code] ?? `error ${upErr?.code ?? "desconocido"}`;
    status(`La guía se guardó, pero «${file.name}» no se adjuntó: ${motivo}.`, true);
  }
}

async function refresh() {
  if (!child) return;
  const box = $("guides-list");
  box.innerHTML = "<p class='muted' style='font-size:13px'>Cargando…</p>";

  let guias, skills;
  try {
    [guias, skills] = await Promise.all([
      getDocs(collection(db, "children", child.id, "guides")),
      getDocs(collection(db, "children", child.id, "skills")),
    ]);
  } catch (e) {
    console.error(e);
    box.innerHTML = "<p class='muted' style='font-size:13px'>No se pudieron cargar las guías.</p>";
    return;
  }

  // Dominio por habilidad, subido por la tablet (ProgressSync.reportSkill).
  const dominio = {};
  skills.forEach((s) => (dominio[s.id] = s.data()));

  if (guias.empty) {
    box.innerHTML = "<p class='muted' style='font-size:13px'>Todavía no has subido material.</p>";
    return;
  }

  // Lo más urgente primero: las guías con examen cercano arriba.
  const orden = [...guias.docs].sort((a, b) => {
    const av = a.data().examDate ?? "9999-99-99";
    const bv = b.data().examDate ?? "9999-99-99";
    return av < bv ? -1 : 1;
  });

  box.innerHTML = "";
  orden.forEach((g) => box.appendChild(render(g.id, g.data(), dominio)));
}

function render(id, g, dominio) {
  const el = document.createElement("div");
  el.className = "guide";

  const dias = daysToExam(g.examDate);
  let cuando = "Sin fecha de examen";
  if (dias !== null) {
    if (dias < 0) cuando = `Examen pasado (${fmtDay(g.examDate)}) · sigue en el repaso`;
    else if (dias === 0) cuando = "⚠️ El examen es HOY";
    else if (dias === 1) cuando = "⚠️ El examen es mañana";
    else cuando = `Faltan ${dias} días · ${fmtDay(g.examDate)}`;
  }

  const pill = g.paused
    ? '<span class="pill paused">Pausada</span>'
    : `<span class="pill ${g.mode === "EXAM_PREP" ? "exam" : "learn"}">${
        escapeHtml(MODE_LABEL[g.mode] ?? g.mode)
      }</span>`;

  // Habilidades con práctica registrada, de peor a mejor. El emparejamiento
  // tema→habilidad lo hace la app; aquí se muestra lo que ya tiene medición.
  const medidas = Object.entries(dominio)
    .filter(([, d]) => (d.practices ?? 0) > 0)
    .sort((a, b) => (a[1].accuracy ?? 0) - (b[1].accuracy ?? 0));

  const barras = medidas
    .slice(0, 6)
    .map(([sid, d]) => {
      const pct = Math.max(0, Math.min(100, Math.round(Number(d.accuracy) * 100 || 0)));
      const color = pct >= 80 ? "var(--green)" : pct >= 60 ? "var(--amber)" : "var(--red)";
      return `<div class="bar-row">
        <span>${escapeHtml(short(sid))}</span>
        <span class="bar"><i style="width:${pct}%; background:${color}"></i></span>
        <span class="muted">${pct}%</span>
      </div>`;
    })
    .join("");

  const flojo = medidas[0];
  const takeaway = flojo
    ? `<div class="takeaway">⚠️ Necesita reforzar <b>${escapeHtml(short(flojo[0]))}</b>
       (va en ${Math.round((flojo[1].accuracy ?? 0) * 100)}%).
       Trivium seguirá trabajando ese tema y lo volverá a evaluar.</div>`
    : `<div class="takeaway">Todavía no hay práctica registrada de estos temas.
       Aparecerán aquí en cuanto ${escapeHtml(child.name)} empiece sus retos.</div>`;

  const fileUrl = safeFileUrl(g.fileUrl);
  const enlace = fileUrl
    ? ` · <a href="${escapeHtml(fileUrl)}" target="_blank" rel="noopener" style="color:var(--indigo)">ver archivo</a>`
    : "";

  el.innerHTML = `
    <div class="guide-top">
      <div>
        <h4>${escapeHtml(g.title ?? "(sin título)")} ${pill}</h4>
        <div class="meta">${escapeHtml(cuando)} · ${escapeHtml(g.topics?.length ?? 0)} temas${enlace}</div>
      </div>
      <div class="guide-actions">
        <button class="ghost" data-act="pause">${g.paused ? "Reanudar" : "Pausar"}</button>
        <button class="ghost" data-act="del">Eliminar</button>
      </div>
    </div>
    <div class="meta" style="margin-top:8px">${escapeHtml((g.topics ?? []).join(" · "))}</div>
    ${String((g.topics ?? []).join(" ")).toLowerCase().includes("spelling")
      ? `<div class="meta" style="margin-top:6px">Meta del refuerzo: <b>${escapeHtml(g.correctTarget ?? 30)} respuestas correctas</b></div>`
      : ""}
    ${(g.spellingWords ?? []).length
      ? `<div class="meta" style="margin-top:6px">Palabras: ${escapeHtml((g.spellingWords ?? []).join(" · "))}</div>`
      : ""}
    <div class="bars">${barras}</div>
    ${takeaway}
  `;

  el.querySelector('[data-act="pause"]').addEventListener("click", async () => {
    await setDoc(
      doc(db, "children", child.id, "guides", id),
      { paused: !g.paused },
      { merge: true }
    );
    refresh();
  });

  el.querySelector('[data-act="del"]').addEventListener("click", async () => {
    if (!confirm(`¿Eliminar «${g.title}»? El historial de práctica NO se borra.`)) return;
    await deleteDoc(doc(db, "children", child.id, "guides", id));
    refresh();
  });

  return el;
}
