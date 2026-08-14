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

  $("toggle-upload").addEventListener("click", () => {
    $("guide-form").classList.toggle("hidden");
    $("guide-error").textContent = "";
  });

  $("g-cancel").addEventListener("click", () => {
    $("guide-form").classList.add("hidden");
    $("guide-form").reset();
  });

  // La fecha solo tiene sentido en modo examen.
  document.querySelectorAll('input[name="g-mode"]').forEach((r) =>
    r.addEventListener("change", () => {
      const esExamen =
        document.querySelector('input[name="g-mode"]:checked').value === "EXAM_PREP";
      $("g-date-wrap").style.opacity = esExamen ? "1" : ".4";
      $("g-date").disabled = !esExamen;
    })
  );

  $("guide-form").addEventListener("submit", onSubmit);
}

/** Se llama al abrir el detalle de un hijo. */
export function openGuidesFor(childId, childName) {
  child = { id: childId, name: childName };
  $("guide-form").classList.add("hidden");
  refresh();
}

async function onSubmit(e) {
  e.preventDefault();
  if (!child) return;

  const err = $("guide-error");
  const btn = $("g-submit");
  err.textContent = "";

  const mode = document.querySelector('input[name="g-mode"]:checked').value;
  const topics = $("g-topics").value.split("\n").map((t) => t.trim()).filter(Boolean);
  const examDate = $("g-date").value;

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
    let fileUrl = null;
    let fileName = null;

    // El archivo es OPCIONAL: los temas son lo que de verdad usa el motor.
    // Si la subida falla, la guía se guarda igual en vez de perderse.
    const file = $("g-file").files[0];
    if (file) {
      try {
        const path = `guides/${child.id}/${id}_${file.name}`;
        const snap = await uploadBytes(storageRef(storage, path), file);
        fileUrl = await getDownloadURL(snap.ref);
        fileName = file.name;
      } catch (upErr) {
        console.warn("No se pudo subir el archivo:", upErr);
        err.textContent = "La guía se guardó, pero el archivo no subió (¿Storage habilitado?).";
      }
    }

    await setDoc(doc(db, "children", child.id, "guides", id), {
      title: $("g-title").value.trim(),
      subject: $("g-subject").value,
      mode,
      examDate: mode === "EXAM_PREP" ? examDate : null,
      topics,
      fileUrl,
      fileName,
      paused: false,
      createdAt: serverTimestamp(),
    });

    $("guide-form").reset();
    $("guide-form").classList.add("hidden");
    refresh();
  } catch (e2) {
    console.error(e2);
    err.textContent = "No se pudo guardar la guía. Revisa las reglas de Firestore.";
  } finally {
    btn.disabled = false;
    btn.textContent = "Guardar guía";
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
        MODE_LABEL[g.mode] ?? g.mode
      }</span>`;

  // Habilidades con práctica registrada, de peor a mejor. El emparejamiento
  // tema→habilidad lo hace la app; aquí se muestra lo que ya tiene medición.
  const medidas = Object.entries(dominio)
    .filter(([, d]) => (d.practices ?? 0) > 0)
    .sort((a, b) => (a[1].accuracy ?? 0) - (b[1].accuracy ?? 0));

  const barras = medidas
    .slice(0, 6)
    .map(([sid, d]) => {
      const pct = Math.round((d.accuracy ?? 0) * 100);
      const color = pct >= 80 ? "var(--green)" : pct >= 60 ? "var(--amber)" : "var(--red)";
      return `<div class="bar-row">
        <span>${short(sid)}</span>
        <span class="bar"><i style="width:${pct}%; background:${color}"></i></span>
        <span class="muted">${pct}%</span>
      </div>`;
    })
    .join("");

  const flojo = medidas[0];
  const takeaway = flojo
    ? `<div class="takeaway">⚠️ Necesita reforzar <b>${short(flojo[0])}</b>
       (va en ${Math.round((flojo[1].accuracy ?? 0) * 100)}%).
       Trivium seguirá trabajando ese tema y lo volverá a evaluar.</div>`
    : `<div class="takeaway">Todavía no hay práctica registrada de estos temas.
       Aparecerán aquí en cuanto ${child.name} empiece sus retos.</div>`;

  const enlace = g.fileUrl
    ? ` · <a href="${g.fileUrl}" target="_blank" rel="noopener" style="color:var(--indigo)">ver archivo</a>`
    : "";

  el.innerHTML = `
    <div class="guide-top">
      <div>
        <h4>${g.title ?? "(sin título)"} ${pill}</h4>
        <div class="meta">${cuando} · ${g.topics?.length ?? 0} temas${enlace}</div>
      </div>
      <div class="guide-actions">
        <button class="ghost" data-act="pause">${g.paused ? "Reanudar" : "Pausar"}</button>
        <button class="ghost" data-act="del">Eliminar</button>
      </div>
    </div>
    <div class="meta" style="margin-top:8px">${(g.topics ?? []).join(" · ")}</div>
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
