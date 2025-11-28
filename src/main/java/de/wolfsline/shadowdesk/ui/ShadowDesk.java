package de.wolfsline.shadowdesk.ui;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.NativeInputEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import de.wolfsline.shadowdesk.core.MonitorController;
import de.wolfsline.shadowdesk.core.MonitorInfo;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tray-App mit:
 * - TrayIcon + Kontextmenü
 * - Alt+1 / Alt+2 / Alt+3 ... als globale Hotkeys zum Standby/Wake des jeweiligen Monitors
 * - Helligkeit-Slider pro Monitor (im Dialog)
 */
public class ShadowDesk {

    private final MonitorController controller;
    private List<MonitorInfo> monitors;
    private final Set<Integer> standbyMonitors = new HashSet<>();
    private TrayIcon trayIcon;

    public ShadowDesk() {
        this.controller = new MonitorController();
        reloadMonitors();
    }

    private void reloadMonitors() {
        monitors = controller.listMonitors();

        // Primary nach vorne sortieren: primary = true → zuerst
        monitors.sort((a, b) -> Boolean.compare(b.primary(), a.primary()));

        standbyMonitors.clear();
        System.out.println("Monitore neu eingelesen (Primary zuerst):");
        for (MonitorInfo info : monitors) {
            System.out.println("  " + info);
        }
        if (trayIcon != null) {
            rebuildTrayMenu();
        }
    }

    public static void main(String[] args) throws Exception {
        // JNativeHook-Logging unterdrücken
        Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.OFF);
        logger.setUseParentHandlers(false);

        ShadowDesk app = new ShadowDesk();
        app.initTray();
        app.initHotkeys();

        System.out.println("ShadowDeskTray gestartet. Alt+1/2/3/... oder Tray-Menü verwenden.");
    }

    // ---------- Tray ----------

    private void initTray() throws AWTException {
        if (!SystemTray.isSupported()) {
            System.err.println("SystemTray wird nicht unterstützt.");
            return;
        }

        SystemTray tray = SystemTray.getSystemTray();
        Image image = createTrayIconImage();

        trayIcon = new TrayIcon(image, "ShadowDesk");
        trayIcon.setImageAutoSize(true);

        rebuildTrayMenu();

        tray.add(trayIcon);
    }

    private void rebuildTrayMenu() {
        if (trayIcon == null) return;

        PopupMenu menu = new PopupMenu();

        MenuItem reloadItem = new MenuItem("Monitore neu einlesen");
        reloadItem.addActionListener(e -> reloadMonitors());
        menu.add(reloadItem);
        menu.addSeparator();

        // 🔁 jetzt über Positions-Index iterieren
        for (int pos = 0; pos < monitors.size(); pos++) {
            MonitorInfo info = monitors.get(pos);
            int hotkeyNumber = pos + 1;          // 1-basiert für Anzeige / Alt+N
            int globalIndex  = info.index();  // DDC-Index für MonitorController

            String title = String.format("%d: %s %s",
                    hotkeyNumber,
                    info.description(),
                    info.primary() ? "(PRIMARY)" : ""
            );

            Menu monitorMenu = new Menu(title);

            MenuItem standbyToggleItem = new MenuItem("Standby / Wake (Alt+" + hotkeyNumber + ")");
            standbyToggleItem.addActionListener(e -> toggleStandby(globalIndex));
            monitorMenu.add(standbyToggleItem);

            MenuItem brightnessItem = new MenuItem("Helligkeit...");
            brightnessItem.addActionListener(e -> openBrightnessDialog(globalIndex));
            monitorMenu.add(brightnessItem);

            menu.add(monitorMenu);
        }

        menu.addSeparator();

        MenuItem exitItem = new MenuItem("Beenden");
        exitItem.addActionListener(e -> {
            try {
                // globale Hotkeys deregistrieren
                GlobalScreen.unregisterNativeHook();
            } catch (Exception ignored) {}

            // TrayIcon entfernen
            SystemTray.getSystemTray().remove(trayIcon);

            // Programm beenden
            System.exit(0);
        });
        menu.add(exitItem);


        trayIcon.setPopupMenu(menu);
    }


    private Image createTrayIconImage() {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 16, 16);
        g.setColor(Color.WHITE);
        g.drawRect(3, 3, 10, 10);
        g.dispose();
        return img;
    }

    // ---------- Standby / Wake ----------

    private void standbyMonitor(int index) {
        controller.standbyMonitor(index);
        standbyMonitors.add(index);
        System.out.println("Monitor " + index + " -> Standby");
    }

    private void wakeMonitor(int index) {
        controller.wakeMonitor(index);
        standbyMonitors.remove(index);
        System.out.println("Monitor " + index + " -> Wake");
    }

    private void toggleStandby(int index) {
        if (standbyMonitors.contains(index)) {
            wakeMonitor(index);
        } else {
            standbyMonitor(index);
        }
    }

    // ---------- Helligkeit-Dialog (Slider) ----------

    private void openBrightnessDialog(int index) {
        MonitorInfo info = monitors.stream()
                .filter(m -> m.index() == index)
                .findFirst()
                .orElse(null);
        if (info == null) {
            System.err.println("Kein Monitor mit Index " + index);
            return;
        }

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Helligkeit - Monitor " + index);
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.setSize(400, 120);
            frame.setLocationRelativeTo(null);

            int initial = (int) Math.round(info.getBrightnessPercent());
            if (initial < 0) initial = 0;
            if (initial > 100) initial = 100;

            JSlider slider = new JSlider(0, 100, initial);
            slider.setMajorTickSpacing(25);
            slider.setMinorTickSpacing(5);
            slider.setPaintTicks(true);
            slider.setPaintLabels(true);

            slider.addChangeListener(e -> {
                int value = slider.getValue();
                controller.setBrightness(index, value);
            });

            frame.getContentPane().add(slider, BorderLayout.CENTER);
            frame.setVisible(true);
        });
    }

    // ---------- Globale Hotkeys (Alt+1 / Alt+2 / Alt+3 / …) ----------

    private void initHotkeys() {
        try {
            GlobalScreen.registerNativeHook();
        } catch (NativeHookException e) {
            System.err.println("Konnte NativeHook nicht registrieren: " + e.getMessage());
            return;
        }

        GlobalScreen.addNativeKeyListener(new NativeKeyListener() {
            @Override
            public void nativeKeyPressed(NativeKeyEvent e) {
                boolean altDown = (e.getModifiers() & NativeInputEvent.ALT_MASK) != 0;
                if (!altDown) return;

                // Offset in der Monitorliste (0 = erster = Primary)
                int offset = -1;
                switch (e.getKeyCode()) {
                    case NativeKeyEvent.VC_1 -> offset = 0;
                    case NativeKeyEvent.VC_2 -> offset = 1;
                    case NativeKeyEvent.VC_3 -> offset = 2;
                    case NativeKeyEvent.VC_4 -> offset = 3;
                    case NativeKeyEvent.VC_5 -> offset = 4;
                    // weiter erweiterbar
                }

                if (offset < 0) return;

                if (offset >= monitors.size()) {
                    System.err.println("Kein Monitor für Alt+" + (offset + 1));
                    return;
                }

                MonitorInfo target = monitors.get(offset);
                int globalIndex = target.index();  // DDC-Index

                toggleStandby(globalIndex);
            }

            @Override public void nativeKeyReleased(NativeKeyEvent e) {}
            @Override public void nativeKeyTyped(NativeKeyEvent e) {}
        });
    }
}