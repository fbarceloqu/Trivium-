// Trivium · Panel de Padres (Fase B, solo lectura)
// Firebase por CDN (sin build). Requiere dashboard/firebase-config.js (ver example).

import { initializeApp } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-app.js";
import {
  getAuth, onAuthStateChanged, GoogleAuthProvider, signInWithPopup, signOut,
} from "https://www.gstatic.com/firebasejs/10.12.2/firebase-auth.js";
import {
  getFirestore, collection, getDocs, doc, getDoc,
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
    `;
    el.addEventListener("click", () => showDetail(child.id, c));
    grid.appendChild(el);
  }
}

// --- Vista: historial de un hijo (últimos 14 días) ---
async function showDetail(childId, c) {
  hide("children-view"); show("detail-view");
  openGuidesFor(childId, c.name ?? childId);
  $("detail-name").textContent = c.name ?? childId;
  $("detail-sub").textContent =
    `${GRADE_LABELS[c.grade] ?? ""} · Dispositivo ${childId} · Última actividad: ${fmtDateTime(c.lastSeen)}`;

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
}
