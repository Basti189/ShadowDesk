package de.wolfsline.shadowdesk.core;

import com.sun.jna.platform.win32.*;
import com.sun.jna.platform.win32.PhysicalMonitorEnumerationAPI.PHYSICAL_MONITOR;

import java.util.ArrayList;
import java.util.List;

public class MonitorController {

    private static final byte VCP_POWER_MODE = (byte) 0xD6;

    // Hilfsmethode für BOOL → boolean
    private boolean ok(WinDef.BOOL b) {
        return b != null && b.booleanValue();
    }

    /**
     * Liefert eine Liste aller physikalischen Monitore inklusive deren Helligkeit.
     */
    public List<MonitorInfo> listMonitors() {
        List<MonitorInfo> list = new ArrayList<>();
        final int[] globalIndex = {0};

        WinUser.HMONITOR primary = User32.INSTANCE.MonitorFromPoint(
                new WinDef.POINT.ByValue(0, 0),
                User32.MONITOR_DEFAULTTOPRIMARY
        );

        User32.INSTANCE.EnumDisplayMonitors(
                null, null,
                (hMonitor, hdc, rect, data) -> {

                    boolean isPrimary = hMonitor.equals(primary);

                    WinDef.DWORDByReference countRef = new WinDef.DWORDByReference();
                    if (!ok(Dxva2.INSTANCE.GetNumberOfPhysicalMonitorsFromHMONITOR(hMonitor, countRef))) {
                        return 1;
                    }

                    int count = countRef.getValue().intValue();
                    if (count <= 0) return 1;

                    PHYSICAL_MONITOR[] mons = new PHYSICAL_MONITOR[count];
                    if (!ok(Dxva2.INSTANCE.GetPhysicalMonitorsFromHMONITOR(hMonitor, count, mons))) {
                        return 1;
                    }

                    for (PHYSICAL_MONITOR pm : mons) {

                        WinDef.DWORDByReference min = new WinDef.DWORDByReference();
                        WinDef.DWORDByReference cur = new WinDef.DWORDByReference();
                        WinDef.DWORDByReference max = new WinDef.DWORDByReference();

                        boolean gotBrightness =
                                ok(Dxva2.INSTANCE.GetMonitorBrightness(pm.hPhysicalMonitor, min, cur, max));

                        String name = new String(pm.szPhysicalMonitorDescription).trim();

                        if (gotBrightness) {
                            list.add(new MonitorInfo(
                                    globalIndex[0],
                                    name,
                                    min.getValue().intValue(),
                                    cur.getValue().intValue(),
                                    max.getValue().intValue(),
                                    isPrimary
                            ));
                        } else {
                            list.add(new MonitorInfo(
                                    globalIndex[0],
                                    name,
                                    0, 0, 100,
                                    isPrimary
                            ));
                        }

                        globalIndex[0]++;
                    }

                    Dxva2.INSTANCE.DestroyPhysicalMonitors(count, mons);
                    return 1;
                },
                new WinDef.LPARAM(0)
        );

        return list;
    }

    /**
     * Setzt die Helligkeit eines Monitors.
     */
    public void setBrightness(int targetIndex, int newBrightness) {

        final int[] globalIndex = {0};
        final boolean[] done = {false};

        User32.INSTANCE.EnumDisplayMonitors(null, null, (hMonitor, hdc, rect, data) -> {

            if (done[0]) return 0;

            WinDef.DWORDByReference countRef = new WinDef.DWORDByReference();
            if (!ok(Dxva2.INSTANCE.GetNumberOfPhysicalMonitorsFromHMONITOR(hMonitor, countRef)))
                return 1;

            int count = countRef.getValue().intValue();
            if (count <= 0) return 1;

            PHYSICAL_MONITOR[] mons = new PHYSICAL_MONITOR[count];
            if (!ok(Dxva2.INSTANCE.GetPhysicalMonitorsFromHMONITOR(hMonitor, count, mons)))
                return 1;

            for (PHYSICAL_MONITOR pm : mons) {
                if (globalIndex[0] == targetIndex) {

                    if (!ok(Dxva2.INSTANCE.SetMonitorBrightness(pm.hPhysicalMonitor, newBrightness))) {
                        System.err.println("Fehler beim SetMonitorBrightness für Monitor " + targetIndex);
                    }

                    done[0] = true;
                    break;
                }

                globalIndex[0]++;
            }

            Dxva2.INSTANCE.DestroyPhysicalMonitors(count, mons);
            return done[0] ? 0 : 1;

        }, new WinDef.LPARAM(0));
    }

    /**
     * Setzt PowerMode (1 = On, 4 = Standby).
     */
    public void setPowerMode(int targetIndex, int mode) {

        final int[] globalIndex = {0};
        final boolean[] done = {false};

        User32.INSTANCE.EnumDisplayMonitors(null, null, (hMonitor, hdc, rect, data) -> {

            if (done[0]) return 0;

            WinDef.DWORDByReference countRef = new WinDef.DWORDByReference();
            if (!ok(Dxva2.INSTANCE.GetNumberOfPhysicalMonitorsFromHMONITOR(hMonitor, countRef)))
                return 1;

            int count = countRef.getValue().intValue();
            if (count <= 0) return 1;

            PHYSICAL_MONITOR[] mons = new PHYSICAL_MONITOR[count];
            if (!ok(Dxva2.INSTANCE.GetPhysicalMonitorsFromHMONITOR(hMonitor, count, mons)))
                return 1;

            for (PHYSICAL_MONITOR pm : mons) {
                if (globalIndex[0] == targetIndex) {

                    if (!ok(Dxva2.INSTANCE.SetVCPFeature(
                            pm.hPhysicalMonitor,
                            new WinDef.BYTE(VCP_POWER_MODE),
                            new WinDef.DWORD(mode)
                    ))) {
                        System.err.println("SetVCPFeature PowerMode fehlgeschlagen.");
                    }

                    done[0] = true;
                    break;
                }

                globalIndex[0]++;
            }

            Dxva2.INSTANCE.DestroyPhysicalMonitors(count, mons);
            return done[0] ? 0 : 1;

        }, new WinDef.LPARAM(0));
    }

    public void standbyMonitor(int index) { setPowerMode(index, 4); }
    public void wakeMonitor(int index) { setPowerMode(index, 1); }
}
