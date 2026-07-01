/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState, useEffect } from "react";
import { 
  Lock, Unlock, Shield, Settings, AlertTriangle, CheckCircle, 
  RefreshCw, BookOpen, Binary, Phone, Smartphone, Database, 
  FileCode, Terminal, HelpCircle, Code, Copy, Check, Info, Sparkles,
  Download, Cpu, Plus, Trash2, ArrowRight, Eye, Layers
} from "lucide-react";
import { getKotlinTemplates } from "./data/kotlinTemplates";
import TabletSimulator from "./components/TabletSimulator";
import { KioskSettings, LockState, SimLog } from "./types";

export default function App() {
  // 1. Core Parental Settings (Shared State)
  const [settings, setSettings] = useState<KioskSettings>({
    launcherPackage: "com.controlparental.kioscosuave",
    blockSettings: true,
    emergencyCalls: true,
    watchdogSensitivity: "High",
    bounceMethod: "AccessibilityEvent",
    allowedApps: ["com.google.android.youtube.kids", "com.duolingo"],
    difficulty: "Medium",
    challengeType: "Mixed",
  });

  const [lockState, setLockState] = useState<LockState>("LOCKED");
  const [activeTab, setActiveTab] = useState<"architecture" | "simulator" | "code">("simulator");
  const [selectedFileIdx, setSelectedFileIdx] = useState(1); // Default to KioskAccessibilityService.kt
  const [copiedFile, setCopiedFile] = useState<string | null>(null);
  const [logs, setLogs] = useState<SimLog[]>([
    {
      id: "1",
      timestamp: new Date().toLocaleTimeString(),
      tag: "SYSTEM",
      message: "Motor de Kiosco Suave inicializado con persistencia de base de datos.",
      type: "info",
    },
    {
      id: "2",
      timestamp: new Date().toLocaleTimeString(),
      tag: "WATCHDOG",
      message: "KioskAccessibilityService registrado y a la espera de eventos de ventana.",
      type: "success",
    }
  ]);

  // Add a simulation log
  const addLog = (
    tag: "SYSTEM" | "WATCHDOG" | "CHALLENGE" | "LAUNCHER",
    message: string,
    type: "info" | "warning" | "success" | "error"
  ) => {
    const newLog: SimLog = {
      id: Math.random().toString(),
      timestamp: new Date().toLocaleTimeString(),
      tag,
      message,
      type,
    };
    setLogs((prev) => [newLog, ...prev].slice(0, 100)); // limit to 100 logs
  };

  // Sync settings update with simulated Firestore logging
  const updateSetting = <K extends keyof KioskSettings>(key: K, value: KioskSettings[K]) => {
    setSettings((prev) => {
      const updated = { ...prev, [key]: value };
      return updated;
    });

    // Simulated cloud synchronization delay log
    addLog("SYSTEM", `Transmitiendo cambio en '${key}' a Firestore en tiempo real...`, "info");
    setTimeout(() => {
      addLog("SYSTEM", `Sincronización de Firestore exitosa. Dispositivo receptor actualiza su estado local.`, "success");
    }, 600);
  };

  const toggleAppPermission = (packageName: string) => {
    const currentList = [...settings.allowedApps];
    let newList;
    if (currentList.includes(packageName)) {
      newList = currentList.filter((pkg) => pkg !== packageName);
      addLog("SYSTEM", `App bloqueada: Removida '${packageName}' de la lista de apps permitidas.`, "warning");
    } else {
      newList = [...currentList, packageName];
      addLog("SYSTEM", `App autorizada: Añadida '${packageName}' a la lista de apps permitidas.`, "success");
    }
    updateSetting("allowedApps", newList);
  };

  const forceLockNow = () => {
    setLockState("LOCKED");
    addLog("SYSTEM", "Señal Remota 'Lock Now' emitida. Forzando bloqueo inmediato de la tablet.", "warning");
  };

  // Copy code template clipboard helper
  const handleCopyCode = (content: string, name: string) => {
    navigator.clipboard.writeText(content);
    setCopiedFile(name);
    setTimeout(() => setCopiedFile(null), 2000);
  };

  const kotlinTemplates = getKotlinTemplates(settings);

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-sans selection:bg-indigo-500/30 selection:text-indigo-200">
      
      {/* Header Bar */}
      <header className="border-b border-slate-850 bg-slate-900/80 backdrop-blur-md px-6 py-4 flex flex-col md:flex-row justify-between items-center shrink-0 gap-4">
        <div className="flex items-center space-x-3">
          <div className="bg-indigo-600/10 border border-indigo-500/20 p-2 rounded-xl text-indigo-400">
            <Shield className="h-6 w-6" />
          </div>
          <div>
            <div className="flex items-center space-x-2">
              <h1 className="text-lg font-bold text-white tracking-tight">Kiosco Suave Android</h1>
              <span className="text-[10px] bg-indigo-900/40 text-indigo-400 border border-indigo-500/20 px-1.5 py-0.5 rounded-full font-mono">
                Kotlin Native + Firebase Firestore
              </span>
            </div>
            <p className="text-xs text-slate-400">Esquema arquitectónico de control parental de alto rendimiento compatible con Google Family Link.</p>
          </div>
        </div>
        
        {/* Navigation Tabs */}
        <div className="flex bg-slate-950 p-1 rounded-xl border border-slate-850">
          <button 
            onClick={() => setActiveTab("simulator")}
            className={`px-4 py-1.5 rounded-lg text-xs font-semibold flex items-center space-x-1.5 transition-all ${
              activeTab === "simulator" 
                ? "bg-slate-850 text-white shadow-sm" 
                : "text-slate-400 hover:text-slate-200"
            }`}
          >
            <Smartphone className="h-3.5 w-3.5" />
            <span>Simulador Interactivo</span>
          </button>
          
          <button 
            onClick={() => setActiveTab("code")}
            className={`px-4 py-1.5 rounded-lg text-xs font-semibold flex items-center space-x-1.5 transition-all ${
              activeTab === "code" 
                ? "bg-slate-850 text-white shadow-sm" 
                : "text-slate-400 hover:text-slate-200"
            }`}
          >
            <Code className="h-3.5 w-3.5" />
            <span>Código Fuente Kotlin</span>
          </button>

          <button 
            onClick={() => setActiveTab("architecture")}
            className={`px-4 py-1.5 rounded-lg text-xs font-semibold flex items-center space-x-1.5 transition-all ${
              activeTab === "architecture" 
                ? "bg-slate-850 text-white shadow-sm" 
                : "text-slate-400 hover:text-slate-200"
            }`}
          >
            <Layers className="h-3.5 w-3.5" />
            <span>Análisis Técnico</span>
          </button>
        </div>
      </header>

      {/* Main Grid View */}
      <main className="flex-1 p-6 grid grid-cols-1 lg:grid-cols-12 gap-6 overflow-y-auto">
        
        {/* TAB 1: SIMULATOR & WORKSPACE */}
        {activeTab === "simulator" && (
          <>
            {/* Left: Parent Configuration Dashboard (Simulated cloud console) */}
            <div className="lg:col-span-7 flex flex-col space-y-6">
              
              {/* Rationale Callout Card */}
              <div className="p-4 bg-gradient-to-r from-emerald-950/40 to-indigo-950/30 border border-emerald-500/20 rounded-2xl flex items-start space-x-3 shadow-lg">
                <Sparkles className="h-5 w-5 text-emerald-400 shrink-0 mt-0.5" />
                <div className="space-y-1">
                  <h3 className="text-sm font-bold text-white">¿Por qué Kotlin Nativo es la mejor opción?</h3>
                  <p className="text-xs text-slate-300 leading-relaxed">
                    Las apps de control parental requieren alta integración a nivel de sistema (Watchdogs, Broadcast Receivers, etc.). 
                    <strong> Kotlin</strong> otorga llamadas nativas sin serializaciones y garantiza la supervivencia de los servicios en segundo plano bajo las duras restricciones de <strong>Android 14+</strong>, algo que en frameworks cross-platform (.NET MAUI, Flutter) tiende a congelarse o generar fugas de recursos.
                  </p>
                </div>
              </div>

              {/* Firestore Dashboard Emulator Card */}
              <div className="bg-slate-900 border border-slate-850 rounded-2xl p-6 flex flex-col space-y-6 shadow-sm">
                
                {/* Header Section */}
                <div className="flex justify-between items-center border-b border-slate-850 pb-4">
                  <div className="flex items-center space-x-2">
                    <Database className="h-5 w-5 text-indigo-400 animate-pulse" />
                    <div>
                      <h2 className="text-sm font-bold text-white uppercase tracking-wider">Consola Administrativa del Papá</h2>
                      <p className="text-[11px] text-slate-400">Simulación del canal en la nube (Firestore Spark Tier) en tiempo real</p>
                    </div>
                  </div>
                  <div className="flex items-center space-x-2 bg-slate-950 px-3 py-1 rounded-lg border border-slate-850 text-[10px] font-mono font-bold text-emerald-400">
                    <span className="h-2 w-2 rounded-full bg-emerald-500 animate-ping"></span>
                    <span>ONLINE (SPARK TIER)</span>
                  </div>
                </div>

                {/* Configurations grid */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  
                  {/* Settings section */}
                  <div className="space-y-3">
                    <h3 className="text-xs font-bold text-slate-400 uppercase tracking-widest font-mono">Reglas del Sistema</h3>
                    
                    {/* Launcher Package */}
                    <div className="flex flex-col space-y-1">
                      <label className="text-[11px] text-slate-400 font-semibold">Package Name de la App</label>
                      <input 
                        type="text" 
                        value={settings.launcherPackage}
                        onChange={(e) => updateSetting("launcherPackage", e.target.value)}
                        className="bg-slate-950 border border-slate-800 text-xs rounded-lg p-2 font-mono text-indigo-300 focus:outline-none focus:border-indigo-500"
                      />
                    </div>

                    {/* Checkbox settings */}
                    <div className="space-y-2.5 pt-1.5">
                      <label className="flex items-center space-x-2 text-xs text-slate-300 cursor-pointer select-none">
                        <input 
                          type="checkbox" 
                          checked={settings.blockSettings}
                          onChange={(e) => updateSetting("blockSettings", e.target.checked)}
                          className="rounded border-slate-800 text-indigo-600 bg-slate-950 focus:ring-0 focus:ring-offset-0 h-4 w-4"
                        />
                        <span>Bloquear Ajustes del Dispositivo</span>
                      </label>

                      <label className="flex items-center space-x-2 text-xs text-slate-300 cursor-pointer select-none">
                        <input 
                          type="checkbox" 
                          checked={settings.emergencyCalls}
                          onChange={(e) => updateSetting("emergencyCalls", e.target.checked)}
                          className="rounded border-slate-800 text-indigo-600 bg-slate-950 focus:ring-0 focus:ring-offset-0 h-4 w-4"
                        />
                        <span>Permitir Llamadas de Emergencia (Bypass)</span>
                      </label>
                    </div>

                    {/* Watchdog Sensitivity */}
                    <div className="flex flex-col space-y-1 pt-1.5">
                      <label className="text-[11px] text-slate-400 font-semibold">Sensibilidad del Watchdog (Intervalo)</label>
                      <select 
                        value={settings.watchdogSensitivity}
                        onChange={(e) => updateSetting("watchdogSensitivity", e.target.value as any)}
                        className="bg-slate-950 border border-slate-800 text-xs rounded-lg p-2 text-slate-300 focus:outline-none"
                      >
                        <option value="High">Alta (50ms - Máxima reacción / Mayor consumo CPU)</option>
                        <option value="Medium">Media (100ms - Balanceado recomendado)</option>
                        <option value="Low">Baja (200ms - Suave / Menor consumo de batería)</option>
                      </select>
                    </div>

                  </div>

                  {/* Challenge settings */}
                  <div className="space-y-4">
                    <h3 className="text-xs font-bold text-slate-400 uppercase tracking-widest font-mono">Reglas de Desafío</h3>
                    
                    {/* Challenge type */}
                    <div className="flex flex-col space-y-1">
                      <label className="text-[11px] text-slate-400 font-semibold">Tipo de Ejercicios</label>
                      <div className="grid grid-cols-3 gap-1.5">
                        {["Mixed", "Math", "Reading"].map((t) => (
                          <button
                            key={t}
                            onClick={() => updateSetting("challengeType", t as any)}
                            className={`py-1.5 rounded-lg text-xs font-semibold border transition-all ${
                              settings.challengeType === t 
                                ? "bg-indigo-600/20 border-indigo-500 text-white" 
                                : "bg-slate-950 border-slate-800 text-slate-400 hover:text-slate-300"
                            }`}
                          >
                            {t === "Mixed" ? "Mixto" : t === "Math" ? "Mate" : "Lectura"}
                          </button>
                        ))}
                      </div>
                    </div>

                    {/* Difficulty */}
                    <div className="flex flex-col space-y-1">
                      <label className="text-[11px] text-slate-400 font-semibold">Nivel de Dificultad</label>
                      <div className="grid grid-cols-3 gap-1.5">
                        {["Easy", "Medium", "Hard"].map((d) => (
                          <button
                            key={d}
                            onClick={() => updateSetting("difficulty", d as any)}
                            className={`py-1.5 rounded-lg text-xs font-semibold border transition-all ${
                              settings.difficulty === d 
                                ? "bg-indigo-600/20 border-indigo-500 text-white" 
                                : "bg-slate-950 border-slate-800 text-slate-400 hover:text-slate-300"
                            }`}
                          >
                            {d === "Easy" ? "Fácil (Primaria)" : d === "Medium" ? "Medio (Ecuaciones)" : "Difícil (Álgebra)"}
                          </button>
                        ))}
                      </div>
                    </div>

                    {/* Coexistence explanation box */}
                    <div className="space-y-1.5">
                      <label className="text-[11px] text-slate-400 font-semibold block">Coexistencia con Family Link</label>
                      <div className="p-3 bg-indigo-950/20 rounded-xl border border-indigo-500/10 space-y-2 text-xs text-slate-300">
                        <p className="leading-relaxed">
                          La aplicación <strong>no restringe ni filtra aplicaciones individuales</strong>. En su lugar, actúa como una compuerta total:
                        </p>
                        <ul className="list-disc list-inside space-y-1 text-slate-400 text-[11px]">
                          <li><strong>Bloqueo Activo:</strong> El menor sólo puede usar este Kiosco de Desafíos.</li>
                          <li><strong>Desbloqueo Automático:</strong> Al completar las tareas, se cierra el Kiosco y se habilitan todas las apps permitidas por Family Link.</li>
                          <li><strong>Watchdog de Seguridad:</strong> Si cierran el Kiosco a la fuerza, se vuelve a abrir al instante.</li>
                        </ul>
                      </div>
                    </div>

                  </div>

                </div>

                {/* Cloud Force Lock Actions Block */}
                <div className="pt-2 flex flex-col md:flex-row space-y-2 md:space-y-0 md:space-x-3">
                  <button
                    onClick={forceLockNow}
                    className="flex-1 py-3 px-4 bg-red-600 hover:bg-red-700 active:bg-red-800 text-white font-bold rounded-xl text-xs transition-colors shadow flex items-center justify-center space-x-2 cursor-pointer"
                  >
                    <Lock className="h-4 w-4" />
                    <span>Bloqueo Remoto de Emergencia (Lock Now)</span>
                  </button>
                  <button
                    onClick={() => {
                      setLockState("UNLOCKED");
                      addLog("SYSTEM", "Comando de Desbloqueo forzado enviado desde el Dashboard del Papá.", "success");
                    }}
                    className="py-3 px-4 bg-slate-950 border border-slate-800 hover:border-slate-700 text-slate-300 font-bold rounded-xl text-xs transition-all flex items-center justify-center space-x-2 cursor-pointer"
                  >
                    <Unlock className="h-4 w-4 text-emerald-400" />
                    <span>Forzar Desbloqueo</span>
                  </button>
                </div>

              </div>

              {/* Free Environment Guide Card (Firebase Spark Tier Explainer) */}
              <div className="bg-slate-900 border border-slate-850 rounded-2xl p-6 space-y-4">
                <div className="flex items-center space-x-2 text-emerald-400">
                  <Sparkles className="h-5 w-5" />
                  <h3 className="text-sm font-bold text-white uppercase tracking-wider">¿Por qué es ideal para el Entorno Gratuito?</h3>
                </div>
                <div className="text-xs text-slate-300 space-y-2.5 leading-relaxed">
                  <p>
                    Para un proyecto personal o familiar de control parental, la mejor infraestructura es <strong>Firebase (Spark Plan)</strong>. Es <strong>100% gratuito</strong> y ofrece:
                  </p>
                  <ul className="list-disc pl-5 space-y-1.5 text-slate-400">
                    <li><strong className="text-slate-300">Cloud Firestore:</strong> 50,000 lecturas y 20,000 escrituras al día (más que suficiente para 50 dispositivos familiares sincronizando reglas).</li>
                    <li><strong className="text-slate-300">Firebase Authentication:</strong> Registro e inicio de sesión seguro para el papá (hasta 10,000 verificaciones al mes gratis).</li>
                    <li><strong className="text-slate-300">Notificaciones en Tiempo Real:</strong> Sockets nativos sin costo de polling; los cambios de regla en la web del papá se transmiten al dispositivo Android en menos de 100ms.</li>
                  </ul>
                </div>
              </div>

            </div>

            {/* Right: Virtual Android Tablet Frame Simulator */}
            <div className="lg:col-span-5 flex flex-col space-y-4 items-center justify-start">
              <h3 className="text-xs font-mono font-bold uppercase tracking-wider text-slate-400 flex items-center space-x-1.5">
                <Smartphone className="h-4 w-4 text-emerald-400" />
                <span>Dispositivo del Niño (Simulación)</span>
              </h3>
              
              <TabletSimulator 
                settings={settings}
                lockState={lockState}
                onStateChange={setLockState}
                onAddLog={addLog}
              />

              {/* Live Log Stream Card */}
              <div className="w-full bg-slate-900 border border-slate-850 rounded-2xl p-4 flex flex-col space-y-3 h-[220px] shadow">
                <div className="flex justify-between items-center border-b border-slate-850 pb-2">
                  <span className="text-[10px] font-mono uppercase font-bold text-slate-400 tracking-wider flex items-center space-x-1">
                    <Terminal className="h-3 w-3 text-cyan-400" />
                    <span>Logs de Auditoría Parental (Logcat)</span>
                  </span>
                  <button 
                    onClick={() => setLogs([])}
                    className="text-[9px] font-mono text-slate-500 hover:text-slate-300 transition-colors"
                  >
                    Limpiar
                  </button>
                </div>
                
                {/* Scrollable logs view */}
                <div className="flex-1 overflow-y-auto space-y-1.5 font-mono text-[10px] pr-2">
                  {logs.length === 0 ? (
                    <div className="h-full flex items-center justify-center text-slate-600 italic">
                      No hay logs de simulación recientes.
                    </div>
                  ) : (
                    logs.map((log) => {
                      let tagColor = "text-slate-400 bg-slate-950";
                      let msgColor = "text-slate-300";

                      if (log.tag === "WATCHDOG") tagColor = "text-yellow-400 bg-yellow-950/40 border border-yellow-500/20";
                      if (log.tag === "CHALLENGE") tagColor = "text-cyan-400 bg-cyan-950/40 border border-cyan-500/20";
                      if (log.tag === "LAUNCHER") tagColor = "text-emerald-400 bg-emerald-950/40 border border-emerald-500/20";

                      if (log.type === "success") msgColor = "text-emerald-300 font-semibold";
                      if (log.type === "warning") msgColor = "text-yellow-300";
                      if (log.type === "error") msgColor = "text-red-300 font-semibold";

                      return (
                        <div key={log.id} className="flex items-start space-x-1.5 hover:bg-slate-850/50 p-1 rounded transition-colors leading-relaxed">
                          <span className="text-slate-500 shrink-0 select-none">[{log.timestamp}]</span>
                          <span className={`px-1 py-0.25 text-[8.5px] font-bold rounded shrink-0 ${tagColor}`}>
                            {log.tag}
                          </span>
                          <span className={`flex-1 ${msgColor}`}>
                            {log.message}
                          </span>
                        </div>
                      );
                    })
                  )}
                </div>
              </div>

            </div>
          </>
        )}

        {/* TAB 2: CODE SCAFFOLDING EXPLORER */}
        {activeTab === "code" && (
          <div className="lg:col-span-12 flex flex-col space-y-4">
            
            <div className="p-4 bg-slate-900 border border-slate-850 rounded-2xl flex flex-col md:flex-row justify-between items-start md:items-center gap-4 shadow-sm">
              <div className="space-y-1">
                <h3 className="text-sm font-bold text-white flex items-center space-x-2">
                  <FileCode className="h-5 w-5 text-indigo-400" />
                  <span>C# .NET MAUI vs. Kotlin Nativo — Estructuras Generadas</span>
                </h3>
                <p className="text-xs text-slate-400">
                  Selecciona cualquiera de las plantillas listas para copiar a tu proyecto de Android Studio. Estas clases implementan el bloqueo fino de pantalla, receptor del administrador y watchdog de accesibilidad.
                </p>
              </div>

              {/* Free Deploy Instructions Prompt */}
              <div className="bg-slate-950 px-3 py-2 rounded-xl border border-slate-850 text-[11px] max-w-sm">
                <span className="text-emerald-400 font-bold font-mono">Guía de Despliegue Gratis:</span> 
                <span className="text-slate-300"> Abre una consola Firebase, crea tu base de datos Firestore y listo. Sin cuota inicial ni tarjetas de crédito requeridas.</span>
              </div>
            </div>

            {/* Explorer Architecture Grid */}
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
              
              {/* File selection rail */}
              <div className="lg:col-span-3 bg-slate-900 border border-slate-850 rounded-2xl p-4 flex flex-col space-y-2">
                <span className="text-[10px] font-mono text-slate-500 uppercase font-bold tracking-wider mb-2">Ficheros del Proyecto</span>
                {kotlinTemplates.map((file, idx) => (
                  <button
                    key={idx}
                    onClick={() => setSelectedFileIdx(idx)}
                    className={`p-2.5 rounded-xl text-xs text-left transition-all border font-mono flex items-center space-x-2.5 ${
                      selectedFileIdx === idx
                        ? "bg-indigo-600/10 border-indigo-500/30 text-white font-semibold"
                        : "bg-transparent border-transparent text-slate-400 hover:bg-slate-850 hover:text-slate-300"
                    }`}
                  >
                    <FileCode className={`h-4 w-4 shrink-0 ${selectedFileIdx === idx ? "text-indigo-400" : "text-slate-500"}`} />
                    <div className="truncate flex-1">
                      <span className="block font-medium">{file.name}</span>
                      <span className="block text-[9px] text-slate-500 truncate">{file.filename}</span>
                    </div>
                  </button>
                ))}
              </div>

              {/* Main Code Viewer Block */}
              <div className="lg:col-span-9 flex flex-col bg-slate-900 border border-slate-850 rounded-2xl overflow-hidden shadow-lg h-[650px]">
                
                {/* Code Header Bar */}
                <div className="h-12 bg-slate-950 border-b border-slate-850 flex items-center justify-between px-5 shrink-0">
                  <div className="flex items-center space-x-2.5">
                    <span className="text-[11px] font-mono text-indigo-400 bg-indigo-950/30 border border-indigo-500/20 px-2 py-0.5 rounded uppercase font-bold">
                      {kotlinTemplates[selectedFileIdx].language}
                    </span>
                    <span className="text-xs text-slate-300 font-mono font-semibold">
                      {kotlinTemplates[selectedFileIdx].filename}
                    </span>
                  </div>

                  {/* Right side download and copy button */}
                  <div className="flex items-center space-x-2">
                    <button
                      onClick={() => handleCopyCode(kotlinTemplates[selectedFileIdx].content, kotlinTemplates[selectedFileIdx].name)}
                      className="px-3 py-1 bg-slate-850 hover:bg-slate-800 text-slate-200 text-xs rounded-lg transition-colors border border-slate-750 flex items-center space-x-1.5 font-semibold cursor-pointer"
                    >
                      {copiedFile === kotlinTemplates[selectedFileIdx].name ? (
                        <>
                          <CheckCircle className="h-3.5 w-3.5 text-emerald-400 animate-pulse" />
                          <span className="text-emerald-400">¡Copiado!</span>
                        </>
                      ) : (
                        <>
                          <Copy className="h-3.5 w-3.5" />
                          <span>Copiar Código</span>
                        </>
                      )}
                    </button>
                  </div>
                </div>

                {/* Description of what the file does */}
                <div className="bg-slate-950/30 border-b border-slate-850/50 p-3.5 text-xs text-slate-400 flex items-start space-x-2 shrink-0">
                  <Info className="h-4 w-4 text-slate-500 shrink-0 mt-0.5" />
                  <span>
                    <strong>Descripción:</strong> {kotlinTemplates[selectedFileIdx].description}
                  </span>
                </div>

                {/* Code editor container with line numbering */}
                <div className="flex-1 overflow-auto bg-slate-950 p-5 font-mono text-xs text-slate-300 leading-relaxed relative">
                  <pre className="whitespace-pre">{kotlinTemplates[selectedFileIdx].content}</pre>
                </div>

              </div>

            </div>

          </div>
        )}

        {/* TAB 3: TECHNICAL ARCHITECTURE ANALYSIS */}
        {activeTab === "architecture" && (
          <div className="lg:col-span-12 flex flex-col space-y-6">
            
            {/* Top architectural benefits overview */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              
              <div className="bg-slate-900 border border-slate-850 rounded-2xl p-5 space-y-3">
                <div className="p-2.5 bg-indigo-500/10 border border-indigo-500/20 rounded-xl text-indigo-400 self-start w-fit">
                  <Cpu className="h-5 w-5" />
                </div>
                <h3 className="text-sm font-bold text-white">Rendimiento y Ciclo de Vida</h3>
                <p className="text-xs text-slate-400 leading-relaxed">
                  Android tiene políticas agresivas para detener servicios en segundo plano. En Kotlin, usar un <code>ForegroundService</code> acoplado a un canal de notificación con la propiedad <code>specialUse</code> (Android 14+) garantiza que el recolector de basura de Android no mate el watchdog. Los wrappers cross-platform tienen mayor latencia y propensión a morir en reposo (Doze mode).
                </p>
              </div>

              <div className="bg-slate-900 border border-slate-850 rounded-2xl p-5 space-y-3">
                <div className="p-2.5 bg-emerald-500/10 border border-emerald-500/20 rounded-xl text-emerald-400 self-start w-fit">
                  <Database className="h-5 w-5" />
                </div>
                <h3 className="text-sm font-bold text-white">Sincronización en la Nube Gratis</h3>
                <p className="text-xs text-slate-400 leading-relaxed">
                  Integrando el SDK de Firebase Firestore de manera directa en Kotlin, la tablet del niño tiene un listener de base de datos en tiempo real extremadamente optimizado. Las escrituras en Firestore se guardan de forma nativa e instantánea en el disco de la tablet en modo offline gracias a la persistencia interna del SDK de Firebase, garantizando que el control parental funcione aun si el niño apaga el Wi-Fi.
                </p>
              </div>

              <div className="bg-slate-900 border border-slate-850 rounded-2xl p-5 space-y-3">
                <div className="p-2.5 bg-red-500/10 border border-red-500/20 rounded-xl text-red-400 self-start w-fit">
                  <Shield className="h-5 w-5" />
                </div>
                <h3 className="text-sm font-bold text-white">Estabilidad del Watchdog de Accesibilidad</h3>
                <p className="text-xs text-slate-400 leading-relaxed">
                  El <code>AccessibilityService</code> recibe eventos en ráfaga (por ejemplo, cada vez que cambia el foco de la pantalla). En Kotlin, responder a este evento es instantáneo (menos de 5ms), permitiendo "rebotar" al niño de vuelta al desafío antes de que logre renderizar el contenido de una app prohibida. En frameworks híbridos, el puente (bridge) de serialización añade latencia, dando un molesto "parpadeo" donde la app prohibida se ve por un segundo.
                </p>
              </div>

            </div>

            {/* Step-by-step Setup Checklist Guide for Real Device Testing */}
            <div className="bg-slate-900 border border-slate-850 rounded-2xl p-6 space-y-4">
              <div className="flex items-center space-x-2 text-indigo-400">
                <Settings className="h-5 w-5" />
                <h3 className="text-sm font-bold text-white uppercase tracking-wider">Paso a Paso para Implementar y Probar en Tablet Real</h3>
              </div>
              
              <div className="text-xs text-slate-300 space-y-4 leading-relaxed">
                <p>
                  Sigue estas instrucciones exactas para compilar el código proporcionado e instalar la app en el dispositivo de pruebas:
                </p>
                
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div className="p-4 bg-slate-950 rounded-xl border border-slate-850 space-y-3">
                    <h4 className="font-bold text-white text-xs flex items-center space-x-1.5">
                      <span className="bg-indigo-600 text-white font-mono w-4.5 h-4.5 rounded-full flex items-center justify-center text-[10px]">1</span>
                      <span>Configuración en Android Studio</span>
                    </h4>
                    <ul className="list-disc pl-5 space-y-1.5 text-slate-400">
                      <li>Crea un proyecto nativo con plantilla de "No Activity" o "Empty Activity" seleccionando <strong className="text-slate-300">Kotlin</strong>.</li>
                      <li>Sustituye el contenido de tu <strong className="text-slate-300">AndroidManifest.xml</strong> con la plantilla provista.</li>
                      <li>Agrega las dependencias de Firebase Firestore en tu <strong className="text-slate-300">build.gradle.kts</strong>.</li>
                      <li>Descarga tu archivo <strong className="text-indigo-400 font-mono">google-services.json</strong> desde tu proyecto gratuito de Firebase y colócalo en la carpeta <strong className="font-mono text-slate-300">/app</strong>.</li>
                    </ul>
                  </div>

                  <div className="p-4 bg-slate-950 rounded-xl border border-slate-850 space-y-3">
                    <h4 className="font-bold text-white text-xs flex items-center space-x-1.5">
                      <span className="bg-indigo-600 text-white font-mono w-4.5 h-4.5 rounded-full flex items-center justify-center text-[10px]">2</span>
                      <span>Activación de Permisos Críticos en la Tablet</span>
                    </h4>
                    <ul className="list-disc pl-5 space-y-1.5 text-slate-400">
                      <li><strong className="text-slate-300">Launcher Predeterminado:</strong> Al pulsar el botón Home por primera vez, Android te preguntará qué app usar. Elige tu app de Kiosco Educativo y selecciona <strong className="text-slate-300">"Siempre"</strong>.</li>
                      <li><strong className="text-slate-300">Servicios de Accesibilidad:</strong> Ve a Ajustes del sistema de la tablet → Accesibilidad → Apps descargadas / Servicios instalados, busca tu Watchdog de Kiosco Suave y <strong className="text-yellow-400 font-bold">Actívalo</strong>.</li>
                      <li><strong className="text-slate-300">Administrador de Dispositivo:</strong> Ve a Ajustes de Seguridad → Administradores de Dispositivo, busca tu app y actívala para habilitar anti-desinstalación y la función Lock Now.</li>
                    </ul>
                  </div>
                </div>

                <div className="p-3 bg-indigo-950/20 border border-indigo-500/20 rounded-xl text-indigo-300 text-center text-[11px] leading-relaxed">
                  💡 <strong>¿Cómo conviven con Google Family Link?</strong> Dado que Family Link administra la cuenta del niño y los tiempos máximos (capa externa), nuestro kiosco suave de Kotlin opera dentro de ese periodo activo bloqueando la pantalla con desafíos y permitiendo solo el subconjunto de aplicaciones aprobadas. Ninguno de los dos bloqueos genera conflicto mutuo.
                </div>

              </div>
            </div>

          </div>
        )}

      </main>

      {/* Footer Branding Area */}
      <footer className="h-10 bg-slate-950 border-t border-slate-900 px-6 flex justify-between items-center text-[10px] text-slate-500 shrink-0 font-mono">
        <span>Kiosco Suave Android Parental Control Project</span>
        <span>Google AI Studio • 2026</span>
      </footer>

    </div>
  );
}
