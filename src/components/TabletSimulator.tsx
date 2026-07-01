/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState, useEffect } from "react";
import { 
  Lock, Unlock, Youtube, Camera, GraduationCap, Settings, 
  AlertTriangle, CheckCircle, RefreshCw, ArrowLeft, BookOpen, 
  Binary, Phone, Shield, Send, Check, Sparkles, AlertCircle, Play,
  Gamepad2, MessageSquare, Plus, HelpCircle, FileText
} from "lucide-react";
import { LockState, AllowedApp, Challenge, SimLog, KioskSettings } from "../types";

interface TabletSimulatorProps {
  settings: KioskSettings;
  lockState: LockState;
  onStateChange: (state: LockState) => void;
  onAddLog: (tag: "SYSTEM" | "WATCHDOG" | "CHALLENGE" | "LAUNCHER", message: string, type: "info" | "warning" | "success" | "error") => void;
}

export default function TabletSimulator({
  settings,
  lockState,
  onStateChange,
  onAddLog,
}: TabletSimulatorProps) {
  // --- Challenge Progression States ---
  const [currentStage, setCurrentStage] = useState<"math" | "english" | "reading">("math");
  
  // Math Stage States
  const [mathCount, setMathCount] = useState(0); // target 10
  const [mathQuestion, setMathQuestion] = useState({ question: "", options: [] as string[], answer: "", explanation: "" });
  const [selectedMathOption, setSelectedMathOption] = useState<string | null>(null);
  const [mathIsCorrect, setMathIsCorrect] = useState<boolean | null>(null);

  // English (Duolingo Style) States
  const [englishCount, setEnglishCount] = useState(0); // target 3
  const [englishExercise, setEnglishExercise] = useState<any>(null);
  const [loadingEnglish, setLoadingEnglish] = useState(false);
  const [selectedEnglishOption, setSelectedEnglishOption] = useState<string | null>(null);
  const [englishIsCorrect, setEnglishIsCorrect] = useState<boolean | null>(null);

  // Reading Comprehension States
  const [readingPassage, setReadingPassage] = useState<{ title: string; text: string } | null>(null);
  const [loadingReading, setLoadingReading] = useState(false);
  const [userSummary, setUserSummary] = useState("");
  const [submittingSummary, setSubmittingSummary] = useState(false);
  const [summaryFeedback, setSummaryFeedback] = useState<any>(null);

  // Simulation Feedback States
  const [bounceEffect, setBounceEffect] = useState<string | null>(null);
  const [cheatedLog, setCheatedLog] = useState<string | null>(null);
  const [emergencyAlert, setEmergencyAlert] = useState(false);

  // List of standard apps for the UNLOCKED state (Tablet Libre)
  const STANDARD_APPS = [
    { id: "youtube", name: "YouTube", icon: Youtube, color: "bg-red-600", pkg: "com.google.android.youtube" },
    { id: "tiktok", name: "TikTok", icon: Play, color: "bg-zinc-900", pkg: "com.zhiliaoapp.musically" },
    { id: "roblox", name: "Roblox", icon: Gamepad2, color: "bg-slate-700", pkg: "com.roblox.client" },
    { id: "whatsapp", name: "WhatsApp", icon: MessageSquare, color: "bg-emerald-500", pkg: "com.whatsapp" },
    { id: "camera", name: "Cámara", icon: Camera, color: "bg-teal-500", pkg: "com.android.camera" },
    { id: "duolingo_full", name: "Duolingo", icon: GraduationCap, color: "bg-green-500", pkg: "com.duolingo" }
  ];

  // --- Dynamic Math Generator (10 Exercises) ---
  const generateMathQuestion = () => {
    setSelectedMathOption(null);
    setMathIsCorrect(null);

    // Differentiate algebra, multiplication, or addition depending on parents' configuration difficulty
    if (settings.difficulty === "Easy") {
      const n1 = Math.floor(Math.random() * 12) + 3;
      const n2 = Math.floor(Math.random() * 9) + 2;
      const ans = n1 + n2;
      const opts = [ans, ans + 2, ans - 3, ans * 2].sort(() => Math.random() - 0.5);
      setMathQuestion({
        question: `¿Cuánto es ${n1} + ${n2}?`,
        options: Array.from(new Set(opts)).map(String),
        answer: String(ans),
        explanation: `Suma directa: ${n1} + ${n2} es igual a ${ans}.`
      });
    } else if (settings.difficulty === "Medium") {
      const n1 = Math.floor(Math.random() * 8) + 4;
      const n2 = Math.floor(Math.random() * 7) + 3;
      const ans = n1 * n2;
      const opts = [ans, ans + 4, ans - 6, ans + 10].sort(() => Math.random() - 0.5);
      setMathQuestion({
        question: `¿Cuánto es ${n1} × ${n2}?`,
        options: Array.from(new Set(opts)).map(String),
        answer: String(ans),
        explanation: `Multiplicando ${n1} por ${n2} resulta en ${ans}.`
      });
    } else {
      const x = Math.floor(Math.random() * 7) + 3;
      const coeff = Math.floor(Math.random() * 3) + 2;
      const constant = Math.floor(Math.random() * 10) + 1;
      const rightSide = coeff * x + constant;
      const opts = [x, x + 2, x - 1, x * 2].sort(() => Math.random() - 0.5);
      setMathQuestion({
        question: `Resuelve la ecuación para X:  ${coeff}X + ${constant} = ${rightSide}`,
        options: Array.from(new Set(opts)).map(String),
        answer: String(x),
        explanation: `Restamos ${constant} de ambos lados: ${coeff}X = ${rightSide - constant}. Dividiendo entre ${coeff}: X = ${x}.`
      });
    }
  };

  // --- Dynamic English Exercise Fetcher (from Gemini / Fallback API) ---
  const fetchEnglishExercise = async () => {
    setLoadingEnglish(true);
    setSelectedEnglishOption(null);
    setEnglishIsCorrect(null);

    try {
      const res = await fetch("/api/ai/english-exercise");
      const data = await res.json();
      setEnglishExercise(data);
      onAddLog("CHALLENGE", `Cargada lección de inglés (${data.mode === "ai" ? "Gemini AI" : "Local Fallback"}): ${data.instruction}`, "info");
    } catch (e) {
      console.error(e);
      // Local recovery fallback
      setEnglishExercise({
        type: "multiple_choice",
        instruction: "Elige la forma correcta en pasado (Past Tense).",
        question: "Yesterday, she ______ (go) to the supermarket.",
        options: ["goed", "goes", "went", "going"],
        correctAnswer: "went",
        explanation: "El pasado de 'go' es irregular: 'went'."
      });
    } finally {
      setLoadingEnglish(false);
    }
  };

  // --- Dynamic Reading Passage Fetcher ---
  const fetchReadingPassage = async () => {
    setLoadingReading(true);
    setUserSummary("");
    setSummaryFeedback(null);

    try {
      const res = await fetch("/api/ai/reading-passage");
      const data = await res.json();
      setReadingPassage(data);
      onAddLog("CHALLENGE", `Cargada lectura educativa (${data.mode === "ai" ? "Gemini AI" : "Local Fallback"}): "${data.title}"`, "info");
    } catch (e) {
      console.error(e);
      setReadingPassage({
        title: "Las abejas y las flores",
        text: "Las abejas son insectos trabajadores que vuelan de flor en flor recolectando néctar para hacer miel. Al hacer esto, transportan el polen de las flores, lo cual ayuda a que crezcan nuevas plantas y frutos en el planeta."
      });
    } finally {
      setLoadingReading(false);
    }
  };

  // Run initial setups
  useEffect(() => {
    if (lockState === "LOCKED") {
      generateMathQuestion();
      fetchEnglishExercise();
      fetchReadingPassage();
      // Reset progress
      setMathCount(0);
      setEnglishCount(0);
      setCurrentStage("math");
    }
  }, [lockState, settings.difficulty]);

  // --- Math Handlers ---
  const handleMathAnswer = (option: string) => {
    if (mathIsCorrect !== null) return;
    setSelectedMathOption(option);
    const correct = option === mathQuestion.answer;
    setMathIsCorrect(correct);

    if (correct) {
      const nextCount = mathCount + 1;
      setMathCount(nextCount);
      onAddLog("CHALLENGE", `Matemáticas (${nextCount}/10): ¡Correcto! ${option}`, "success");

      setTimeout(() => {
        if (nextCount >= 10) {
          setCurrentStage("english");
          onAddLog("SYSTEM", "Fase 1 completada (10 ejercicios de Matemáticas). Iniciando lección de Inglés Duolingo.", "success");
        } else {
          generateMathQuestion();
        }
      }, 1500);
    } else {
      onAddLog("CHALLENGE", `Matemáticas: Respuesta incorrecta "${option}". Inténtalo otra vez.`, "error");
    }
  };

  // --- English Handlers ---
  const handleEnglishAnswer = (option: string) => {
    if (englishIsCorrect !== null) return;
    setSelectedEnglishOption(option);
    const correct = option === englishExercise.correctAnswer;
    setEnglishIsCorrect(correct);

    if (correct) {
      const nextCount = englishCount + 1;
      setEnglishCount(nextCount);
      onAddLog("CHALLENGE", `Inglés (${nextCount}/3): ¡Correcto! ${option}`, "success");

      setTimeout(() => {
        if (nextCount >= 3) {
          setCurrentStage("reading");
          onAddLog("SYSTEM", "Fase 2 completada (Lección de Inglés aprobada). Iniciando Comprensión Lectora.", "success");
        } else {
          fetchEnglishExercise();
        }
      }, 2000);
    } else {
      onAddLog("CHALLENGE", `Inglés: Respuesta incorrecta "${option}". Revisa la explicación gramatical.`, "error");
    }
  };

  // --- Reading Summary Submit Handler ---
  const handleSummarySubmit = async () => {
    if (!userSummary.trim()) return;
    setSubmittingSummary(true);
    setSummaryFeedback(null);
    onAddLog("CHALLENGE", "Enviando resumen escrito a calificar por Inteligencia Artificial (Gemini API)...", "info");

    try {
      const res = await fetch("/api/ai/evaluate-summary", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          readingText: readingPassage?.text,
          userSummary: userSummary
        })
      });
      const feedback = await res.json();
      setSummaryFeedback(feedback);

      if (feedback.approved) {
        onAddLog("CHALLENGE", `Comprensión Lectora Aprobada! Nota: ${feedback.score}/100. "${feedback.feedback}"`, "success");
        // UNLOCK the Kiosk!
        setTimeout(() => {
          onStateChange("UNLOCKED");
          onAddLog("SYSTEM", "¡TODOS LOS DESAFÍOS COMPLETADOS CON ÉXITO! El Kiosco se ha desbloqueado de forma segura.", "success");
        }, 2500);
      } else {
        onAddLog("CHALLENGE", `Resumen rechazado (${feedback.score}/100): ${feedback.feedback}`, "warning");
      }
    } catch (e: any) {
      console.error(e);
      // FAIL-CLOSED: un error de red NO debe desbloquear la tablet. Antes esto
      // aprobaba automáticamente (score 80) -> el niño solo tenía que cortar el WiFi.
      onAddLog("CHALLENGE", "No se pudo contactar al calificador IA. El resumen NO se aprueba; reintenta cuando haya conexión.", "error");
      setSummaryFeedback({
        approved: false,
        score: 0,
        feedback: "No pudimos evaluar tu resumen porque no hay conexión con el servidor.",
        suggestions: "Revisa la conexión a internet e intenta enviar tu resumen de nuevo."
      });
    } finally {
      setSubmittingSummary(false);
    }
  };

  // --- Intercepted Evasion / Close Actions ---
  const triggerEvadeAttempt = (packageName: string, label: string) => {
    onAddLog("WATCHDOG", `Intento de evasión detectado hacia: ${packageName} ("${label}")`, "warning");

    if (lockState === "LOCKED") {
      // Check emergency bypass (phone calling)
      const isEmergencyCall = settings.emergencyCalls && (packageName === "com.android.phone" || packageName === "emergency");
      if (isEmergencyCall) {
        setEmergencyAlert(true);
        onAddLog("SYSTEM", "Bypass de Emergencia: Abriendo Dialer nativo para llamadas de auxilio.", "info");
        return;
      }

      // Rebound/Intercept animation
      setBounceEffect(packageName);
      setCheatedLog(`KioskService interceptó cierre / cambio de app. Cerrando ${label} y reabriendo Kiosco de Desafíos de forma ineludible.`);
      onAddLog("WATCHDOG", `REBOTE AUTOMÁTICO ACTIVO: Forzando enfoque prioritario en KioscoSuave.`, "error");

      setTimeout(() => {
        setBounceEffect(null);
        setCheatedLog(null);
      }, 3500);
    } else {
      // Unlocked: Standard launcher allows any app, simulating total freedom
      onAddLog("LAUNCHER", `Dispositivo libre: Abriendo aplicación de forma nativa: ${packageName}`, "success");
      alert(`Simulador: Abriendo la aplicación "${label}" (${packageName}). ¡Diversión desbloqueada libremente!`);
    }
  };

  const currentLocalTime = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

  return (
    <div className="flex flex-col items-center w-full">
      {/* Dynamic Watchdog Notification Banner */}
      {bounceEffect && (
        <div className="w-full max-w-lg mb-4 p-3 bg-red-950/95 border border-red-500 rounded-lg text-red-200 text-xs flex items-center space-x-3 shadow-lg animate-bounce z-30">
          <Shield className="h-5 w-5 text-red-400 shrink-0 animate-pulse" />
          <div className="flex-1 font-mono leading-relaxed">
            <span className="font-bold text-red-400 block mb-0.5">⚠️ WATCHDOG ACTIVO (REBOTE)</span>
            {cheatedLog}
          </div>
        </div>
      )}

      {emergencyAlert && (
        <div className="fixed inset-0 bg-black/85 flex items-center justify-center p-4 z-50">
          <div className="bg-red-900 border border-red-500 rounded-xl p-6 max-w-sm text-center text-white space-y-4 shadow-2xl">
            <Phone className="h-16 w-16 text-white mx-auto animate-pulse" />
            <h3 className="text-xl font-bold">Llamada de Emergencia</h3>
            <p className="text-sm text-red-100">
              Se ha simulado el bypass para llamadas de emergencia (Dialer / 911). Esta acción está autorizada por el Device Admin.
            </p>
            <button 
              onClick={() => setEmergencyAlert(false)}
              className="px-4 py-2 bg-white text-red-900 font-bold rounded-lg text-sm hover:bg-red-100 transition-colors"
            >
              Cerrar Simulación
            </button>
          </div>
        </div>
      )}

      {/* Virtual Android Tablet Hardware Frame */}
      <div className="relative w-full max-w-md bg-slate-900 border-[10px] border-slate-950 rounded-[32px] shadow-2xl overflow-hidden aspect-[3/4.2] flex flex-col ring-4 ring-slate-800/50">
        
        {/* Tablet Camera Hole */}
        <div className="absolute top-1.5 left-1/2 -translate-x-1/2 w-2.5 h-2.5 bg-slate-800 rounded-full z-20"></div>

        {/* Status Bar */}
        <div className="h-6 bg-slate-950 text-[10px] font-mono font-semibold text-slate-400 flex justify-between items-center px-5 shrink-0 select-none border-b border-slate-900">
          <div className="flex items-center space-x-1.5">
            <Shield className={`h-3 w-3 ${lockState === "LOCKED" ? "text-red-500 animate-pulse" : "text-green-500"}`} />
            <span className="text-slate-300">KioscoSuave v1.1 (AI Hub)</span>
          </div>
          <div className="flex items-center space-x-3">
            <span className="bg-slate-800 px-1.5 py-0.5 rounded text-[9px] text-slate-400">
              {lockState}
            </span>
            <span>{currentLocalTime}</span>
          </div>
        </div>

        {/* Screen Content Wrapper */}
        <div className="flex-1 bg-slate-950 relative overflow-hidden flex flex-col font-sans">
          
          {/* LOCKED STATE VIEW (Kiosk Mode Active) */}
          {lockState === "LOCKED" ? (
            <div className="flex-1 flex flex-col justify-between p-4 bg-slate-900">
              
              {/* Top Lock Indicator & Stepper Progress */}
              <div className="border-b border-slate-850 pb-2.5 mb-2.5">
                <div className="flex justify-between items-center text-red-400 mb-2">
                  <div className="flex items-center space-x-2">
                    <Lock className="h-4 w-4 animate-pulse text-red-500" />
                    <span className="font-mono text-[10px] uppercase font-bold tracking-wider text-red-400">Tareas Pendientes</span>
                  </div>
                  <div className="text-[10px] text-slate-400 font-bold bg-slate-950 px-2 py-0.5 rounded border border-slate-850">
                    Kiosco Activo
                  </div>
                </div>

                {/* Challenge Stepper Progress Icons */}
                <div className="grid grid-cols-3 gap-1 text-center">
                  <div className={`p-1.5 rounded-lg border text-[10px] font-bold flex items-center justify-center space-x-1 ${
                    currentStage === "math" ? "bg-cyan-950/40 border-cyan-500 text-cyan-200" : "bg-slate-950/40 border-slate-850 text-slate-400"
                  }`}>
                    <Binary className="h-3 w-3" />
                    <span>Mate ({mathCount}/10)</span>
                  </div>

                  <div className={`p-1.5 rounded-lg border text-[10px] font-bold flex items-center justify-center space-x-1 ${
                    currentStage === "english" ? "bg-green-950/40 border-green-500 text-green-200" : "bg-slate-950/40 border-slate-850 text-slate-400"
                  }`}>
                    <GraduationCap className="h-3 w-3" />
                    <span>Inglés ({englishCount}/3)</span>
                  </div>

                  <div className={`p-1.5 rounded-lg border text-[10px] font-bold flex items-center justify-center space-x-1 ${
                    currentStage === "reading" ? "bg-amber-950/40 border-amber-500 text-amber-200" : "bg-slate-950/40 border-slate-850 text-slate-400"
                  }`}>
                    <BookOpen className="h-3 w-3" />
                    <span>Lectura (AI)</span>
                  </div>
                </div>
              </div>

              {/* STAGE 1: MATH WORKSPACE (10 EXERCISES) */}
              {currentStage === "math" && (
                <div className="flex-1 flex flex-col justify-center space-y-3.5">
                  <div className="flex items-center space-x-1.5 self-start px-2.5 py-1 rounded-full bg-cyan-950/40 border border-cyan-500/20 text-cyan-300">
                    <Binary className="h-3 w-3 animate-pulse" />
                    <span className="text-[10px] font-mono font-bold">EJERCICIO DE MATEMÁTICAS ({mathCount + 1} de 10)</span>
                  </div>

                  {/* Chalkboard chalkboard panel */}
                  <div className="bg-slate-950 border-2 border-slate-800 rounded-xl p-5 min-h-[110px] flex flex-col justify-center text-center shadow-inner relative">
                    <div className="absolute top-2 right-2 text-[9px] font-mono text-slate-600">Dificultad: {settings.difficulty}</div>
                    <p className="text-xl font-bold font-mono text-white tracking-wide">
                      {mathQuestion.question}
                    </p>
                  </div>

                  {/* Options */}
                  <div className="grid grid-cols-2 gap-2">
                    {mathQuestion.options.map((option, idx) => {
                      const isSelected = selectedMathOption === option;
                      let btnStyle = "bg-slate-800 border-slate-750 text-slate-300 hover:bg-slate-750";

                      if (isSelected) {
                        if (mathIsCorrect === true) btnStyle = "bg-green-900 border-green-500 text-green-100";
                        if (mathIsCorrect === false) btnStyle = "bg-red-900 border-red-500 text-red-100 animate-shake";
                      }

                      return (
                        <button
                          key={idx}
                          disabled={mathIsCorrect === true}
                          onClick={() => handleMathAnswer(option)}
                          className={`p-3 rounded-xl border text-xs font-mono font-bold text-center transition-all ${btnStyle}`}
                        >
                          {option}
                        </button>
                      );
                    })}
                  </div>

                  {/* Explanation feedback */}
                  {mathIsCorrect !== null && (
                    <div className={`p-2.5 rounded-lg text-[11px] leading-relaxed border ${
                      mathIsCorrect ? "bg-green-950/40 border-green-500/30 text-green-300" : "bg-red-950/40 border-red-500/30 text-red-300"
                    }`}>
                      <p className="font-bold flex items-center space-x-1">
                        {mathIsCorrect ? (
                          <><Check className="h-3.5 w-3.5" /> <span>¡Excelente respuesta!</span></>
                        ) : (
                          <><AlertCircle className="h-3.5 w-3.5" /> <span>Inténtalo de nuevo</span></>
                        )}
                      </p>
                      {mathIsCorrect && <p className="mt-0.5 text-[10px] text-slate-400">{mathQuestion.explanation}</p>}
                    </div>
                  )}
                </div>
              )}

              {/* STAGE 2: ENGLISH WORKSPACE (3 DUOLINGO-STYLE LESSONS) */}
              {currentStage === "english" && (
                <div className="flex-1 flex flex-col justify-center space-y-3">
                  <div className="flex items-center space-x-1.5 self-start px-2.5 py-1 rounded-full bg-green-950/40 border border-green-500/20 text-green-300">
                    <GraduationCap className="h-3 w-3 animate-pulse" />
                    <span className="text-[10px] font-mono font-bold">DUOLINGO INGLES ({englishCount + 1} de 3)</span>
                  </div>

                  {loadingEnglish ? (
                    <div className="flex-1 flex flex-col items-center justify-center space-y-2 text-slate-400">
                      <RefreshCw className="h-8 w-8 text-green-500 animate-spin" />
                      <p className="text-xs font-mono">Generando lección de inglés vía IA...</p>
                    </div>
                  ) : englishExercise ? (
                    <>
                      {/* Exercise Header */}
                      <div className="bg-slate-950 border border-slate-800 rounded-xl p-4 flex flex-col space-y-2 relative shadow-inner">
                        <span className="text-[9px] uppercase font-bold text-green-400 tracking-wider">Instrucción:</span>
                        <p className="text-xs text-slate-300 leading-normal font-semibold">
                          {englishExercise.instruction}
                        </p>
                        <div className="border-t border-slate-850 my-1 pt-1.5">
                          <p className="text-sm font-bold font-mono text-white bg-slate-900/50 p-2 rounded border border-slate-850 text-center">
                            {englishExercise.question}
                          </p>
                        </div>
                      </div>

                      {/* Options */}
                      <div className="grid grid-cols-2 gap-2">
                        {englishExercise.options.map((option: string, idx: number) => {
                          const isSelected = selectedEnglishOption === option;
                          let btnStyle = "bg-slate-800 border-slate-750 text-slate-300 hover:bg-slate-750";

                          if (isSelected) {
                            if (englishIsCorrect === true) btnStyle = "bg-green-900 border-green-500 text-green-100";
                            if (englishIsCorrect === false) btnStyle = "bg-red-900 border-red-500 text-red-100";
                          }

                          return (
                            <button
                              key={idx}
                              disabled={englishIsCorrect === true}
                              onClick={() => handleEnglishAnswer(option)}
                              className={`p-2.5 rounded-xl border text-xs font-semibold text-center transition-all ${btnStyle}`}
                            >
                              {option}
                            </button>
                          );
                        })}
                      </div>

                      {/* Feedback Explainer with AI Tag */}
                      {englishIsCorrect !== null && (
                        <div className={`p-2.5 rounded-xl text-[11px] leading-relaxed border ${
                          englishIsCorrect ? "bg-green-950/50 border-green-500/40 text-green-200" : "bg-red-950/50 border-red-500/40 text-red-200"
                        }`}>
                          <div className="flex justify-between items-center mb-0.5">
                            <span className="font-bold flex items-center space-x-1">
                              {englishIsCorrect ? (
                                <><CheckCircle className="h-3.5 w-3.5 text-green-400" /> <span>¡Excelente! Correcto.</span></>
                              ) : (
                                <><AlertCircle className="h-3.5 w-3.5 text-red-400" /> <span>Inténtalo otra vez.</span></>
                              )}
                            </span>
                            {englishExercise.mode === "ai" && (
                              <span className="text-[8px] bg-indigo-950 border border-indigo-500/20 text-indigo-400 px-1 py-0.25 rounded font-mono">
                                AI Content
                              </span>
                            )}
                          </div>
                          <p className="text-[10px] text-slate-400 leading-normal">
                            {englishExercise.explanation}
                          </p>
                        </div>
                      )}
                    </>
                  ) : null}
                </div>
              )}

              {/* STAGE 3: READING & AI SUMMARY WORKSPACE */}
              {currentStage === "reading" && (
                <div className="flex-1 flex flex-col justify-start space-y-2.5 overflow-y-auto pr-1">
                  <div className="flex items-center space-x-1.5 self-start px-2.5 py-1 rounded-full bg-amber-950/40 border border-amber-500/20 text-amber-300 shrink-0">
                    <Sparkles className="h-3 w-3 text-amber-400 animate-pulse" />
                    <span className="text-[10px] font-mono font-bold">COMPRENSIÓN LECTORA (CALIFICADO POR IA)</span>
                  </div>

                  {loadingReading ? (
                    <div className="flex-1 flex flex-col items-center justify-center space-y-2 text-slate-400 py-6">
                      <RefreshCw className="h-8 w-8 text-amber-500 animate-spin" />
                      <p className="text-xs font-mono">Generando lectura educativa con IA...</p>
                    </div>
                  ) : readingPassage ? (
                    <div className="space-y-2.5">
                      {/* Notebook paper reading text */}
                      <div className="bg-slate-950 border border-slate-800 rounded-xl p-3.5 shadow-inner">
                        <h4 className="text-xs font-bold text-amber-400 mb-1 flex items-center space-x-1">
                          <BookOpen className="h-3.5 w-3.5" />
                          <span>Lectura: {readingPassage.title}</span>
                        </h4>
                        <p className="text-[11.5px] text-slate-300 leading-relaxed font-sans italic bg-slate-900/40 p-2.5 rounded border border-slate-850">
                          "{readingPassage.text}"
                        </p>
                      </div>

                      {/* Text area for small summary */}
                      <div className="flex flex-col space-y-1">
                        <label className="text-[10px] font-semibold text-slate-400 flex justify-between">
                          <span>Escribe tu pequeño resumen en español:</span>
                          <span className="font-mono text-slate-500">{userSummary.trim().split(/\s+/).filter(Boolean).length} palabras</span>
                        </label>
                        <textarea
                          value={userSummary}
                          onChange={(e) => setUserSummary(e.target.value)}
                          placeholder="Escribe de qué se habla en el texto con tus propias palabras... (por ejemplo: El pingüino emperador vive en la fría Antártida y caza peces...)"
                          disabled={submittingSummary || (summaryFeedback && summaryFeedback.approved)}
                          className="w-full h-16 bg-slate-950 border border-slate-800 text-xs rounded-xl p-2.5 text-slate-200 placeholder-slate-600 focus:outline-none focus:border-indigo-500 resize-none leading-relaxed font-sans"
                        />
                      </div>

                      {/* AI grading feedback display */}
                      {summaryFeedback && (
                        <div className={`p-3 rounded-xl text-[11px] leading-relaxed border ${
                          summaryFeedback.approved 
                            ? "bg-green-950/40 border-green-500/40 text-green-200" 
                            : "bg-red-950/40 border-red-500/40 text-red-200"
                        }`}>
                          <div className="flex justify-between items-center mb-1">
                            <span className="font-bold flex items-center space-x-1 text-xs">
                              {summaryFeedback.approved ? (
                                <><CheckCircle className="h-4 w-4 text-green-400" /> <span>¡Completado! Nota: {summaryFeedback.score}/100</span></>
                              ) : (
                                <><AlertTriangle className="h-4 w-4 text-red-400" /> <span>Falta un poquito más</span></>
                              )}
                            </span>
                            <span className="text-[8px] bg-indigo-950 border border-indigo-500/20 text-indigo-400 px-1 py-0.25 rounded font-mono">
                              Gemini Evaluator
                            </span>
                          </div>
                          <p className="text-slate-300 text-[10px] leading-relaxed">
                            "{summaryFeedback.feedback}"
                          </p>
                          {summaryFeedback.suggestions && (
                            <p className="text-slate-400 text-[9.5px] mt-1 italic border-t border-slate-850 pt-1">
                              💡 Sugerencia: {summaryFeedback.suggestions}
                            </p>
                          )}
                        </div>
                      )}

                      {/* Submit button */}
                      {(!summaryFeedback || !summaryFeedback.approved) && (
                        <button
                          onClick={handleSummarySubmit}
                          disabled={submittingSummary || userSummary.trim().length < 15}
                          className="w-full py-2 bg-indigo-600 hover:bg-indigo-700 active:bg-indigo-800 disabled:bg-slate-800 disabled:text-slate-600 text-white rounded-xl text-xs font-bold transition-all flex items-center justify-center space-x-1.5 cursor-pointer shadow-md"
                        >
                          {submittingSummary ? (
                            <>
                              <RefreshCw className="h-3.5 w-3.5 animate-spin" />
                              <span>IA Evaluando tu Resumen...</span>
                            </>
                          ) : (
                            <>
                              <Send className="h-3.5 w-3.5" />
                              <span>Enviar Resumen a la IA</span>
                            </>
                          )}
                        </button>
                      )}
                    </div>
                  ) : null}
                </div>
              )}

              {/* Guardian Info Banner */}
              <div className="text-center text-[9px] text-slate-500 border-t border-slate-850 pt-2 flex justify-center items-center space-x-1 font-mono shrink-0">
                <Shield className="h-3 w-3 text-red-500/60" />
                <span>Kiosco Educativo Permanente • Conectado a Family Link</span>
              </div>

            </div>
          ) : (
            /* UNLOCKED STATE VIEW (Tablet Libre / Standard Launcher) */
            <div className="flex-1 flex flex-col bg-slate-900 justify-between">
              
              {/* Launcher Header */}
              <div className="h-10 border-b border-emerald-500/20 bg-slate-950 px-4 flex justify-between items-center text-emerald-400 shrink-0">
                <div className="flex items-center space-x-2">
                  <Unlock className="h-4 w-4 text-emerald-400" />
                  <span className="font-mono text-[10px] uppercase font-bold tracking-widest text-emerald-400">Tablet Desbloqueada</span>
                </div>
                <div className="text-[9px] text-emerald-400 font-bold bg-emerald-950/30 px-2 py-0.5 rounded border border-emerald-500/20">
                  Libre
                </div>
              </div>

              {/* Home Launcher Workspace */}
              <div className="flex-1 p-5 flex flex-col justify-start space-y-4">
                <div className="bg-gradient-to-r from-emerald-950/30 to-slate-950/40 border border-emerald-500/20 rounded-xl p-3.5">
                  <h4 className="text-xs font-bold text-emerald-300 flex items-center space-x-1 mb-1">
                    <CheckCircle className="h-4 w-4 text-emerald-400" />
                    <span>¡Felicidades, Desafíos Completados!</span>
                  </h4>
                  <p className="text-[10px] text-slate-300 leading-normal">
                    Has completado con éxito tus 10 tareas de matemáticas, tu lección de inglés y tu resumen calificado por Inteligencia Artificial. La tablet ahora es libre para navegar bajo las políticas estándar de Family Link.
                  </p>
                </div>

                {/* Grid of standard available apps on the child's tablet */}
                <div className="space-y-2">
                  <span className="text-[10px] font-mono text-slate-500 uppercase font-bold block mb-1">Aplicaciones del Dispositivo</span>
                  <div className="grid grid-cols-3 gap-3">
                    {STANDARD_APPS.map((app) => {
                      const IconComp = app.icon;
                      return (
                        <button
                          key={app.id}
                          onClick={() => triggerEvadeAttempt(app.pkg, app.name)}
                          className="flex flex-col items-center space-y-1 p-2 bg-slate-950/40 rounded-xl border border-slate-850 hover:bg-slate-850 hover:border-slate-700 transition-all cursor-pointer"
                        >
                          <div className={`w-9 h-9 ${app.color} rounded-xl flex items-center justify-center text-white shadow`}>
                            <IconComp className="h-4.5 w-4.5" />
                          </div>
                          <span className="text-[9.5px] text-slate-300 font-medium truncate max-w-[80px] text-center">
                            {app.name}
                          </span>
                        </button>
                      );
                    })}
                  </div>
                </div>
              </div>

              {/* Remoto Bloqueo Manual trigger */}
              <div className="p-4 bg-slate-950 border-t border-slate-850 shrink-0">
                <button 
                  onClick={() => {
                    onStateChange("LOCKED");
                    onAddLog("SYSTEM", "Kiosco bloqueado manualmente para simular fin de sesión / downtime.", "warning");
                  }}
                  className="w-full py-2 bg-slate-900 border border-slate-800 hover:border-red-500/20 text-red-400 hover:bg-red-950/20 text-xs rounded-xl transition-all font-mono font-semibold flex items-center justify-center space-x-1.5 cursor-pointer"
                >
                  <Lock className="h-3.5 w-3.5 text-red-400" />
                  <span>Cerrar Sesión / Bloquear Tablet</span>
                </button>
              </div>

            </div>
          )}

        </div>

        {/* Physical Android Navigation Bar (Soft Keys) */}
        <div className="h-10 bg-slate-950 flex justify-around items-center px-8 shrink-0 select-none border-t border-slate-900 z-20">
          {/* Back button */}
          <button 
            onClick={() => {
              if (lockState === "LOCKED") {
                onAddLog("WATCHDOG", "Pulsado botón físico ATRÁS. Watchdog impidió la salida.", "warning");
                // Trigger flash animation
                setBounceEffect("back_press");
                setCheatedLog("Watchdog interceptó evento KeyEvent.KEYCODE_BACK. El Kiosco de Desafíos permanece al frente.");
                setTimeout(() => setBounceEffect(null), 3000);
              } else {
                onAddLog("LAUNCHER", "Atrás pulsado (libre).", "info");
              }
            }}
            className="p-1 text-slate-500 hover:text-slate-300 transition-colors"
            title="Atrás"
          >
            <ArrowLeft className="h-5 w-5" />
          </button>

          {/* Home button */}
          <button 
            onClick={() => {
              if (lockState === "LOCKED") {
                onAddLog("WATCHDOG", "Pulsado botón HOME. KioscoSuave reclama el foco principal.", "info");
                setBounceEffect("home_press");
                setCheatedLog("Watchdog interceptó intento de ir al Home de Android. Se reactivó la ventana del Kiosco.");
                setTimeout(() => setBounceEffect(null), 3000);
              } else {
                onAddLog("LAUNCHER", "Home pulsado. Cargando launcher nativo de Android.", "info");
              }
            }}
            className="w-4 h-4 rounded border-2 border-slate-500 hover:border-slate-300 transition-colors"
            title="Inicio"
          ></button>

          {/* Recents button */}
          <button 
            onClick={() => {
              if (lockState === "LOCKED") {
                onAddLog("WATCHDOG", "Pulsado botón RECIENTES. Bloqueado de forma permanente por Device Policy Controller.", "warning");
                setBounceEffect("recents_press");
                setCheatedLog("DPC interceptó apertura de tareas recientes. Se deshabilitó la vista Recents de forma nativa.");
                setTimeout(() => setBounceEffect(null), 3000);
              } else {
                onAddLog("LAUNCHER", "Mostrando Recientes (simulado).", "info");
              }
            }}
            className="w-4 h-4 border-2 border-slate-500 hover:border-slate-300 rounded-sm transition-colors"
            title="Recientes"
          ></button>
        </div>
      </div>

      {/* Controller Area for Cheating Attempts */}
      <div className="w-full max-w-md mt-4 p-4 bg-slate-900 border border-slate-850 rounded-2xl shadow">
        <h4 className="text-xs text-slate-400 uppercase font-mono tracking-wider font-bold mb-3 flex items-center space-x-1.5">
          <Settings className="h-3.5 w-3.5 text-indigo-400" />
          <span>Controles de Simulación (Acciones del Niño)</span>
        </h4>
        <div className="grid grid-cols-2 gap-2">
          <button
            onClick={() => triggerEvadeAttempt("com.android.settings", "Ajustes del Sistema")}
            className="flex items-center space-x-2 p-2 bg-slate-950 hover:bg-red-950/20 border border-slate-800 hover:border-red-500/30 rounded-lg text-left text-xs text-slate-300 hover:text-red-400 transition-all cursor-pointer"
          >
            <Settings className="h-4 w-4 text-red-400 shrink-0" />
            <span className="truncate">Intentar abrir Ajustes</span>
          </button>

          <button
            onClick={() => triggerEvadeAttempt("com.google.android.youtube", "YouTube")}
            className="flex items-center space-x-2 p-2 bg-slate-950 hover:bg-red-950/20 border border-slate-800 hover:border-red-500/30 rounded-lg text-left text-xs text-slate-300 hover:text-red-400 transition-all cursor-pointer"
          >
            <Youtube className="h-4 w-4 text-red-500 shrink-0" />
            <span className="truncate">Cerrar Kiosco / Abrir YouTube</span>
          </button>

          <button
            onClick={() => triggerEvadeAttempt("com.zhiliaoapp.musically", "TikTok")}
            className="flex items-center space-x-2 p-2 bg-slate-950 hover:bg-red-950/20 border border-slate-800 hover:border-red-500/30 rounded-lg text-left text-xs text-slate-300 hover:text-red-400 transition-all cursor-pointer"
          >
            <Play className="h-4 w-4 text-slate-400 shrink-0" />
            <span className="truncate">Intentar TikTok</span>
          </button>

          <button
            onClick={() => triggerEvadeAttempt("emergency", "Marcador de Emergencia")}
            className="flex items-center space-x-2 p-2 bg-slate-950 hover:bg-slate-850 border border-slate-800 rounded-lg text-left text-xs text-slate-300 transition-all cursor-pointer"
          >
            <Phone className="h-4 w-4 text-cyan-400 shrink-0" />
            <span className="truncate">Llamada de Emergencia</span>
          </button>
        </div>
      </div>
    </div>
  );
}
