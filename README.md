# ShadowDesk
### Multi-Monitor Standby & Brightness Control for Windows (DDC/CI)

ShadowDesk ist ein leichtgewichtiges Windows-Tool zur Steuerung mehrerer Monitore direkt über die Windows-System-Tray-Leiste.  
Mit globalen Hotkeys, echtem DDC/CI-Zugriff, Brightness-Slidern und Standby-Toggle eignet es sich ideal für Multi-Monitor-Setups beim Arbeiten, Gaming oder Streaming.

---

## ✨ Features

### 🖥️ Multi-Monitor Support
- Erkennt automatisch alle DDC/CI-fähigen Monitore
- Primary Monitor wird immer als erster behandelt (Alt + 1)
- Zeigt Modellnamen und Brightness-Bereich

### 🌙 Standby / Wake (VCP 0xD6)
- Monitor in Standby versetzen
- Monitor wieder aktivieren
- Pro Monitor per Tray oder Hotkey steuerbar

### 🔆 Live Brightness Control
- Brightness-Slider pro Monitor
- Sofortige DDC-Anpassung
- Kein OSD am Monitor notwendig

### 🖱️ Tray-Steuerung
- Standby / Wake pro Monitor
- Brightness-Regler
- Alle Monitore Standby
- Alle Monitore wecken
- Monitore neu einlesen
- Beenden

### ⌨️ Hotkeys (systemweit)
| Hotkey | Funktion |
|--------|----------|
| Alt + 1 | Primary Monitor Standby/Wake |
| Alt + 2 | 2. Monitor Standby/Wake |
| Alt + 3 | 3. Monitor Standby/Wake |
| … | beliebig erweiterbar |

---

## 🔧 Installation

### Selbst kompilieren

```bash
git clone https://github.com/Basti189/ShadowDesk.git
cd ShadowDesk
mvn clean package
java -jar target/ShadowDesk.jar
```

---

## 🛠️ Tech Stack

- Java 17+
- JNA (com.sun.jna)
- DXVA2.dll – DDC/CI
- User32.dll – Monitor-Enumeration
- JNativeHook – globale Hotkeys
- Swing UI

---

## ⚠️ Bekannte Einschränkungen

### HDMI-Splitter / KVMs
DDC/CI funktioniert nicht zuverlässig mit:
- HDMI-Splittern
- KVM-Switches
- Capture Cards

Direkte Verbindung empfohlen.