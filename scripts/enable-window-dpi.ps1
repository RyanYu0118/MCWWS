Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing

if ([Windows.Forms.Application].GetMethod('SetHighDpiMode')) {
    [void][Windows.Forms.Application]::SetHighDpiMode([Windows.Forms.HighDpiMode]::PerMonitorV2)
} else {
    Add-Type -TypeDefinition @"
using System;
using System.Runtime.InteropServices;
public static class SurfaceWindowDpi {
    [DllImport("user32.dll")]
    public static extern IntPtr SetThreadDpiAwarenessContext(IntPtr context);
}
"@
    [void][SurfaceWindowDpi]::SetThreadDpiAwarenessContext([IntPtr](-4))
}
[Windows.Forms.Application]::EnableVisualStyles()
