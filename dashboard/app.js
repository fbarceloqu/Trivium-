// Trivium · Panel de Padres (Fase B, solo lectura)
// Firebase por CDN (sin build). Requiere dashboard/firebase-config.js (ver example).

import { initializeApp } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-app.js";
import {
  getAuth, onAuthStateChanged, GoogleAuthProvider, signInWithPopup, signOut,
} from "https://www.gstatic.com/firebasejs/10.12.2/firebase-auth.js";
import {
  getFirestore, collection, getDocs, doc, getDoc, setDoc, serverTimestamp,
} from "https://www.gstatic.com/firebasejs/10.12.2/firebase-firestore.js";
import { initGuides, openGuidesFor } from "./guides.js";

const $ = (id) => document.getElementById(id);
const show = (id) => $(id).classList.remove("hidden");
const hide = (id) => $(id).classList.add("hidden");

// --- Config (si falta, mostrar instrucciones en vez de romper) ---
let firebaseConfig;
try {
  ({ firebaseConfig } = await import("./firebase-config.js"));
} catch (err) {
  // No ocultar la causa real: ayuda muchísimo a diagnosticar (404 del
  // servidor vs. error de sintaxis en el archivo vs. archivo vacío, etc.)
  console.error("No se pudo cargar dashboard/firebase-config.js:", err);
  show("config-missing");
  throw err;
}

const app = initializeApp(firebaseConfig);
const auth = getAuth(app);
const db = getFirestore(app);
initGuides(app, db);

// Solo estas cuentas de Google pueden ver el panel. Edita esta lista para
// agregar/quitar padres autorizados (además, refuerza esto en firestore.rules).
const ALLOWED_PARENT_EMAILS = [
  "fco.quintanar@gmail.com",
  "anaid.torresu@gmail.com",
];

const GRADE_LABELS = {
  PREESCOLAR: "Preescolar / 1º",
  PRIMARIA: "Primaria",
  SECUNDARIA: "Secundaria",
};
let selectedChild = null;

const todayStr = () => {
  const d = new Date();
  const p = (n) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`;
};

const fmtTime = (ts) =>
  ts?.toDate ? ts.toDate().toLocaleTimeString("es-MX", { hour: "2-digit", minute: "2-digit" }) : "—";
const fmtDateTime = (ts) =>
  ts?.toDate ? ts.toDate().toLocaleString("es-MX", { dateStyle: "medium", timeStyle: "short" }) : "—";

// --- Sesión ---
onAuthStateChanged(auth, (user) => {
  if (user && !user.isAnonymous && ALLOWED_PARENT_EMAILS.includes(user.email)) {
    hide("login-view"); show("logout-btn"); showChildren();
    return;
  }
  if (user && !user.isAnonymous) {
    // Cuenta de Google válida pero NO autorizada: fuera.
    $("login-error").textContent =
      `La cuenta ${user.email} no está autorizada para ver este panel.`;
    signOut(auth);
    return;
  }
  hide("children-view"); hide("detail-view"); hide("logout-btn"); show("login-view");
});

$("google-login-btn").addEventListener("click", async () => {
  $("login-error").textContent = "";
  try {
    await signInWithPopup(auth, new GoogleAuthProvider());
  } catch (err) {
    console.error(err);
    $("login-error").textContent =
      "No se pudo iniciar sesión con Google (¿el proveedor 'Google' está habilitado en Firebase Authentication?).";
  }
});

$("logout-btn").addEventListener("click", () => signOut(auth));
$("back-btn").addEventListener("click", () => { hide("detail-view"); show("children-view"); });
$("edit-child-btn").addEventListener("click", () => {
  if (!selectedChild) return;
  $("e-name").value = selectedChild.data.name ?? "";
  $("e-grade").value = selectedChild.data.grade ?? "PRIMARIA";
  $("e-email").value = selectedChild.data.authorizedEmail ?? "";
  $("edit-child-error").textContent = "";
  $("edit-child-form").classList.toggle("hidden");
});
$("e-cancel").addEventListener("click", () => hide("edit-child-form"));
$("edit-child-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  if (!selectedChild) return;
  const error = $("edit-child-error");
  const email = $("e-email").value.trim().toLowerCase();
  error.textContent = "";
  if (ALLOWED_PARENT_EMAILS.includes(email)) {
    error.textContent = "Usa la cuenta del alumno, no la cuenta de padre/madre.";
    return;
  }
  const button = event.submitter;
  button.disabled = true;
  try {
    await setDoc(doc(db, "children", selectedChild.id), {
      name: $("e-name").value.trim(), grade: $("e-grade").value,
      authorizedEmail: email, updatedAt: serverTimestamp()
    }, { merge: true });
    selectedChild.data = { ...selectedChild.data, name: $("e-name").value.trim(), grade: $("e-grade").value, authorizedEmail: email };
    $("detail-name").textContent = selectedChild.data.name;
    $("detail-sub").textContent = `${GRADE_LABELS[selectedChild.data.grade]} · ${email} · Última actividad: ${fmtDateTime(selectedChild.data.lastSeen)}`;
    hide("edit-child-form");
  } catch (err) {
    console.error(err); error.textContent = "No se pudo guardar. Revisa las reglas de Firestore.";
  } finally { button.disabled = false; }
});

$("toggle-child-form").addEventListener("click", () => {
  $("child-form").classList.toggle("hidden");
  $("child-error").textContent = "";
});
$("c-cancel").addEventListener("click", () => {
  $("child-form").reset(); hide("child-form");
});
$("child-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  const name = $("c-name").value.trim();
  const email = $("c-email").value.trim().toLowerCase();
  const error = $("child-error");
  error.textContent = "";
  if (!name || !email) return;
  if (ALLOWED_PARENT_EMAILS.includes(email)) {
    error.textContent = "Usa la cuenta Google del alumno, no una cuenta de padre/madre.";
    return;
  }
  const button = event.submitter;
  button.disabled = true;
  try {
    const id = `child_${Date.now()}`;
    await setDoc(doc(db, "children", id), {
      name, grade: $("c-grade").value, authorizedEmail: email,
      createdAt: serverTimestamp(), updatedAt: serverTimestamp(),
      source: "parent-dashboard"
    });
    $("child-form").reset(); hide("child-form"); showChildren();
  } catch (err) {
    console.error(err); error.textContent = "No se pudo crear el perfil. Revisa las reglas de Firestore.";
  } finally { button.disabled = false; }
});

// --- Vista: tarjetas de los hijos ---
async function showChildren() {
  hide("detail-view"); show("children-view");
  const grid = $("children-grid");
  grid.innerHTML = "<p class='muted'>Cargando…</p>";

  const snap = await getDocs(collection(db, "children"));
  grid.innerHTML = "";
  $("children-empty").style.display = snap.empty ? "block" : "none";

  for (const child of snap.docs) {
    const c = child.data();
    const day = await getDoc(doc(db, "children", child.id, "days", todayStr()));
    const d = day.exists() ? day.data() : null;

    const unlocked = !!d?.unlockedAt;
    const stat = (s) => (s ? `${s.correct ?? 0}/${s.attempts ?? 0}` : "—");
    const evas = d?.evasions?.count ?? 0;

    const el = document.createElement("div");
    el.className = "card child-card";
    el.innerHTML = `
      <h3>${c.name ?? child.id}</h3>
      <div class="grade">${GRADE_LABELS[c.grade] ?? c.grade ?? ""}</div>
      <div class="status ${unlocked ? "unlocked" : "locked"}">
        ${unlocked ? `🔓 Desbloqueada hoy a las ${fmtTime(d.unlockedAt)}` : "🔒 Tareas pendientes hoy"}
      </div>
      <div class="chips">
        <span class="chip">Mate: ${stat(d?.math)}</span>
        <span class="chip">Inglés: ${stat(d?.english)}</span>
        <span class="chip">Lectura: ${d?.reading ? (d.reading.score ?? d.reading.correct ?? 0) : "—"}</span>
        <span class="chip">${evas > 0 ? `⚠️ ${evas} intentos de salir` : "Sin evasiones"}</span>
      </div>
      <div class="lastseen">Última actividad: ${fmtDateTime(c.lastSeen)}</div>
      <div class="lastseen">Cuenta autorizada: ${c.authorizedEmail ?? "Pendiente de configurar"}</div>
    `;
    el.addEventListener("click", () => showDetail(child.id, c));
    grid.appendChild(el);
  }
}

// --- Vista: historial de un hijo (últimos 14 días) ---
async function showDetail(childId, c) {
  selectedChild = { id: childId, data: { ...c } };
  hide("children-view"); show("detail-view");
  hide("edit-child-form");
  openGuidesFor(childId, c.name ?? childId);
  $("detail-name").textContent = c.name ?? childId;
  $("detail-sub").textContent =
    `${GRADE_LABELS[c.grade] ?? ""} · ${c.authorizedEmail ?? "Sin correo autorizado"} · Última actividad: ${fmtDateTime(c.lastSeen)}`;

  const body = $("days-body");
  body.innerHTML = "<tr><td colspan='6' class='muted'>Cargando…</td></tr>";

  // Sin orderBy/limit en la consulta (evita depender de un índice compuesto
  // de Firestore); con ~365 días/año como mucho, ordenar en el navegador es
  // instantáneo. Los IDs de documento son "yyyy-MM-dd", ordenan bien como texto.
  const snap = await getDocs(collection(db, "children", childId, "days"));
  body.innerHTML = "";
  if (snap.empty) {
    body.innerHTML = "<tr><td colspan='6' class='muted'>Sin registros todavía.</td></tr>";
    return;
  }

  const days = [...snap.docs].sort((a, b) => (a.id < b.id ? 1 : -1)).slice(0, 14);

  for (const day of days) {
    const d = day.data();
    const stat = (s) => (s ? `${s.correct ?? 0}/${s.attempts ?? 0}` : "—");
    const reading = d.reading ? `${d.reading.score ?? d.reading.correct ?? 0}` : "—";
    const evas = d.evasions?.count ?? 0;
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${day.id}</td>
      <td>${stat(d.math)}</td>
      <td>${stat(d.english)}</td>
      <td>${reading}</td>
      <td class="${d.unlockedAt ? "ok" : "bad"}">${d.unlockedAt ? fmtTime(d.unlockedAt) : "No desbloqueó"}</td>
      <td class="${evas > 0 ? "warn" : ""}">${evas > 0 ? `⚠️ ${evas}` : "0"}</td>
    `;
    body.appendChild(tr);
  }

  renderLearningInsight(childId);
  renderSessions(childId);
}

function skillLabel(id) {
  return String(id)
    .replace(/^sec1\./, "")
    .replace(/[._]/g, " ")
    .replace(/\b\w/g, (c) => c.toUpperCase());
}

async function renderLearningInsight(childId) {
  const box = $("learning-insight");
  try {
    const snap = await getDocs(collection(db, "children", childId, "skills"));
    const practiced = snap.docs.map((d) => ({ id: d.id, ...d.data() }))
      .filter((s) => (s.practices ?? 0) > 0)
      .sort((a, b) => (a.accuracy ?? 0) - (b.accuracy ?? 0));
    if (!practiced.length) {
      box.textContent = "Aún no hay suficientes resultados por habilidad. Trivium empezará a detectar fortalezas y refuerzos conforme practique.";
      return;
    }
    const weak = practiced[0];
    const strong = practiced[practiced.length - 1];
    const weakPct = Math.round((weak.accuracy ?? 0) * 100);
    const strongPct = Math.round((strong.accuracy ?? 0) * 100);
    const action = weakPct < 60
      ? "Conviene practicarlo pronto con ejemplos, ayuda y formatos más visuales."
      : weakPct < 80
        ? "Está en progreso: Trivium debe repetirlo de forma variada antes de espaciarlo."
        : "Va bien: puede espaciarse y presentarse en problemas de aplicación.";
    box.textContent = `Prioridad actual: ${skillLabel(weak.id)} (${weakPct}%). Fortaleza: ${skillLabel(strong.id)} (${strongPct}%). ${action}`;
  } catch (err) {
    console.error(err);
    box.textContent = "No se pudo cargar la memoria de aprendizaje.";
  }
}

async function renderSessions(childId) {
  const box = $("sessions-list");
  box.innerHTML = "<p class='muted' style='font-size:13px'>Cargando…</p>";
  try {
    const days = await getDocs(collection(db, "children", childId, "days"));
    const sessions = [];
    for (const day of days.docs) {
      const nested = await getDocs(collection(db, "children", childId, "days", day.id, "sessions"));
      nested.forEach((s) => sessions.push({ id: s.id, day: day.id, ...s.data() }));
    }
    sessions.sort((a, b) => (b.id > a.id ? 1 : -1));
    box.innerHTML = "";
    if (!sessions.length) {
      box.innerHTML = "<p class='muted' style='font-size:13px'>Aún no hay sesiones terminadas.</p>";
      return;
    }
    sessions.slice(0, 8).forEach((session) => {
      const details = document.createElement("details");
      details.className = "guide";
      const summary = document.createElement("summary");
      const pct = Math.round((session.accuracy ?? 0) * 100);
      summary.textContent = `${session.day} · ${session.stage} · ${session.correct ?? 0}/${session.attempts ?? 0} correctas (${pct}%)`;
      details.appendChild(summary);
      (session.exercises ?? []).forEach((exercise, i) => {
        const row = document.createElement("div");
        row.style.cssText = "border-top:1px solid var(--border); margin-top:10px; padding-top:10px; font-size:13px";
        const question = document.createElement("div"); question.textContent = `${i + 1}. ${exercise.question}`;
        const answer = document.createElement("div"); answer.textContent = `${exercise.correct ? "✓" : "✗"} Eligió: ${exercise.selected} · Correcta: ${exercise.answer}`;
        const explanation = document.createElement("div"); explanation.className = "muted"; explanation.textContent = exercise.explanation || "";
        row.append(question, answer, explanation); details.appendChild(row);
      });
      box.appendChild(details);
    });
  } catch (err) {
    console.error(err);
    box.innerHTML = "<p class='muted' style='font-size:13px'>No se pudieron cargar las sesiones.</p>";
  }
}
