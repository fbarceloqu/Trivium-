/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

export type LockState = "LOCKED" | "UNLOCKED";

export interface AllowedApp {
  id: string;
  name: string;
  packageName: string;
  icon: string; // Lucide icon identifier
  color: string;
}

export interface KioskSettings {
  launcherPackage: string;
  blockSettings: boolean;
  emergencyCalls: boolean;
  watchdogSensitivity: "High" | "Medium" | "Low";
  bounceMethod: "AccessibilityEvent" | "OverlayIntercept" | "ForceHome";
  allowedApps: string[]; // package names
  difficulty: "Easy" | "Medium" | "Hard";
  challengeType: "Math" | "Reading" | "Mixed";
}

export interface Challenge {
  id: string;
  type: "Math" | "Reading";
  question: string;
  options: string[];
  answer: string;
  explanation?: string;
}

export interface SimLog {
  id: string;
  timestamp: string;
  tag: "SYSTEM" | "WATCHDOG" | "CHALLENGE" | "LAUNCHER";
  message: string;
  type: "info" | "warning" | "success" | "error";
}

export interface CodeTemplate {
  name: string;
  filename: string;
  language: string;
  content: string;
  description: string;
}
