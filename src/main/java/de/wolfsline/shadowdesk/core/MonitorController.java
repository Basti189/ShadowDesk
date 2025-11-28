import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinDef.DWORDByReference;
import com.sun.jna.platform.win32.WinUser;

import java.util.ArrayList;
import java.util.List;

public class MonitorController {

    private static final byte VCP_POWER_MODE = (byte) 0xD6;  // Power Mode

    /**
     * Liefert eine Liste aller physikalischen Monitore mit Helligkeitsinfos.
     */
    public List<MonitorInfo> listMonitors() {
        List<MonitorInfo> result = new ArrayList<>();
        final int[] globalIndex = {0};

        // Primary HMONITOR holen
        WinUser.HMONITOR primaryHandle = User32.INSTANCE.MonitorFromPoint(
                new WinDef.POINT.ByValue(0, 0),
                User32.MONITOR_DEFAULTTOPRIMARY
        );

        User32.INSTANCE.EnumDisplayMonitors(
                null,
                null,
                (WinUser.HMONITOR hMonitor, WinDef.HDC hdc, WinDef.RECT rect, WinDef.LPARAM data) -> {

                    boolean isPrimary = hMonitor.equals(primaryHandle);

                    DWORDByReference countRef = new DWORDByReference();
                    boolean ok = Dxva2.INSTANCE.GetNumberOfPhysicalMonitorsFromHMONITOR(
                            hMonitor,
                            countRef
                    );
                    if (!ok) {
                        System.err.println("GetNumberOfPhysicalMonitorsFromHMONITOR fehlgeschlagen.");
                        return 1;
                    }

                    int count = countRef.getValue().intValue();
                    if (count <= 0)
                        return 1;

                    Dxva2.PHYSICAL_MONITOR[] mons =
                            (Dxva2.PHYSICAL_MONITOR[]) (new Dxva2.PHYSICAL_MONITOR()).toArray(count);

                    ok = Dxva2.INSTANCE.GetPhysicalMonitorsFromHMONITOR(
                            hMonitor,
                            count,
                            mons
                    );

                    if (!ok) {
                        System.err.println("GetPhysicalMonitorsFromHMONITOR fehlgeschlagen.");
                        return 1;
                    }

                    for (int i = 0; i < count; i++) {
                        Dxva2.PHYSICAL_MONITOR mon = mons[i];

                        DWORDByReference minRef = new DWORDByReference();
                        DWORDByReference curRef = new DWORDByReference();
                        DWORDByReference maxRef = new DWORDByReference();

                        boolean got = Dxva2.INSTANCE.GetMonitorBrightness(
                                mon.hPhysicalMonitor,
                                minRef,
                                curRef,
                                maxRef
                        );

                        String desc = new String(mon.szPhysicalMonitorDescription).trim();

                        if (!got) {
                            System.err.println("[" + globalIndex[0] + "] " + desc + " -> Brightness fail");
                        } else {
                            result.add(new MonitorInfo(
                                    globalIndex[0],
                                    desc,
                                    minRef.getValue().intValue(),
                                    curRef.getValue().intValue(),
                                    maxRef.getValue().intValue(),
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

        return result;
    }

    /**
     * Setzt die Helligkeit eines Monitors anhand des globalen Index.
     */
    public void setBrightness(int targetIndex, int newBrightness) {
        System.out.println("Setze Helligkeit von Monitor " + targetIndex + " auf " + newBrightness + " …");

        final int[] globalIndex = {0};
        final boolean[] done = {false};

        User32.INSTANCE.EnumDisplayMonitors(
                null,
                null,
                (WinUser.HMONITOR hMonitor, WinDef.HDC hdc, WinDef.RECT rect, WinDef.LPARAM data) -> {

                    if (done[0]) {
                        return 0; // abbrechen
                    }

                    DWORDByReference countRef = new DWORDByReference();
                    boolean ok = Dxva2.INSTANCE.GetNumberOfPhysicalMonitorsFromHMONITOR(
                            hMonitor,
                            countRef
                    );
                    if (!ok) {
                        System.err.println("GetNumberOfPhysicalMonitorsFromHMONITOR fehlgeschlagen.");
                        return 1;
                    }

                    int count = countRef.getValue().intValue();
                    if (count <= 0) {
                        return 1;
                    }

                    Dxva2.PHYSICAL_MONITOR[] mons =
                            (Dxva2.PHYSICAL_MONITOR[]) (new Dxva2.PHYSICAL_MONITOR()).toArray(count);

                    ok = Dxva2.INSTANCE.GetPhysicalMonitorsFromHMONITOR(
                            hMonitor,
                            count,
                            mons
                    );
                    if (!ok) {
                        System.err.println("GetPhysicalMonitorsFromHMONITOR fehlgeschlagen.");
                        return 1;
                    }

                    for (int i = 0; i < count; i++) {
                        Dxva2.PHYSICAL_MONITOR mon = mons[i];

                        if (globalIndex[0] == targetIndex && !done[0]) {
                            String desc = new String(mon.szPhysicalMonitorDescription).trim();
                            boolean setOk = Dxva2.INSTANCE.SetMonitorBrightness(
                                    mon.hPhysicalMonitor,
                                    newBrightness
                            );
                            if (!setOk) {
                                System.err.println("SetMonitorBrightness fehlgeschlagen für Monitor " +
                                        targetIndex + " (" + desc + ")");
                            } else {
                                System.out.println("Helligkeit gesetzt für Monitor " +
                                        targetIndex + " (" + desc + ")");
                                done[0] = true;
                            }
                        }

                        globalIndex[0]++;
                    }

                    Dxva2.INSTANCE.DestroyPhysicalMonitors(count, mons);

                    return done[0] ? 0 : 1;
                },
                new WinDef.LPARAM(0)
        );

        if (!done[0]) {
            System.err.println("Kein Monitor mit Index " + targetIndex + " gefunden.");
        }
    }

    /**
     * Power-Mode setzen (z.B. 1=On, 4=Off/Standby).
     */
    public void setPowerMode(int targetIndex, int mode) {
        System.out.println("Setze PowerMode von Monitor " + targetIndex + " auf " + mode + " …");

        final int[] globalIndex = {0};
        final boolean[] done = {false};

        User32.INSTANCE.EnumDisplayMonitors(
                null,
                null,
                (WinUser.HMONITOR hMonitor, WinDef.HDC hdc, WinDef.RECT rect, WinDef.LPARAM data) -> {

                    if (done[0]) {
                        return 0; // abbrechen
                    }

                    DWORDByReference countRef = new DWORDByReference();
                    boolean ok = Dxva2.INSTANCE.GetNumberOfPhysicalMonitorsFromHMONITOR(
                            hMonitor,
                            countRef
                    );
                    if (!ok) {
                        System.err.println("GetNumberOfPhysicalMonitorsFromHMONITOR fehlgeschlagen.");
                        return 1;
                    }

                    int count = countRef.getValue().intValue();
                    if (count <= 0) {
                        return 1;
                    }

                    Dxva2.PHYSICAL_MONITOR[] mons =
                            (Dxva2.PHYSICAL_MONITOR[]) (new Dxva2.PHYSICAL_MONITOR()).toArray(count);

                    ok = Dxva2.INSTANCE.GetPhysicalMonitorsFromHMONITOR(
                            hMonitor,
                            count,
                            mons
                    );
                    if (!ok) {
                        System.err.println("GetPhysicalMonitorsFromHMONITOR fehlgeschlagen.");
                        return 1;
                    }

                    for (int i = 0; i < count; i++) {
                        Dxva2.PHYSICAL_MONITOR mon = mons[i];

                        if (globalIndex[0] == targetIndex && !done[0]) {
                            String desc = new String(mon.szPhysicalMonitorDescription).trim();

                            boolean setOk = Dxva2.INSTANCE.SetVCPFeature(
                                    mon.hPhysicalMonitor,
                                    VCP_POWER_MODE,
                                    mode
                            );

                            if (!setOk) {
                                System.err.println("SetVCPFeature (PowerMode) fehlgeschlagen für Monitor " +
                                        targetIndex + " (" + desc + ")");
                            } else {
                                System.out.println("PowerMode gesetzt für Monitor " +
                                        targetIndex + " (" + desc + "), mode=" + mode);
                                done[0] = true;
                            }
                        }

                        globalIndex[0]++;
                    }

                    Dxva2.INSTANCE.DestroyPhysicalMonitors(count, mons);

                    return done[0] ? 0 : 1;
                },
                new WinDef.LPARAM(0)
        );

        if (!done[0]) {
            System.err.println("Kein Monitor mit Index " + targetIndex + " gefunden oder Mode nicht unterstützt.");
        }
    }

    // Convenience-Methoden

    public void standbyMonitor(int targetIndex) {
        setPowerMode(targetIndex, 4);
    }

    public void wakeMonitor(int targetIndex) {
        setPowerMode(targetIndex, 1);
    }
}