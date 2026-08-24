<#
.SYNOPSIS
Fix ZeroTier: restart service, set Private profile, open firewall for MC + ZeroTier.

.DESCRIPTION
Run as Administrator on the Windows host that runs the Minecraft server.
ZeroTier NLA often resets the profile to Public; firewall rules with -Profile Any
still allow friends through even if the category flips back.

.EXAMPLE
powershell -NoProfile -ExecutionPolicy Bypass -File tools\scripts\fix-zerotier-network.ps1
#>
[CmdletBinding()]
param(
    [int]$MinecraftPort = 25565,
    [string]$ZeroTierExe = ""
)

$ErrorActionPreference = "Stop"

function Test-IsAdmin {
    $id = [Security.Principal.WindowsIdentity]::GetCurrent()
    $p = New-Object Security.Principal.WindowsPrincipal($id)
    return $p.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

if (-not (Test-IsAdmin)) {
    Write-Host "[ERROR] Please run this script as Administrator." -ForegroundColor Red
    Write-Host "Right-click PowerShell -> Run as administrator, then:"
    Write-Host '  powershell -NoProfile -ExecutionPolicy Bypass -File tools\scripts\fix-zerotier-network.ps1'
    exit 1
}

Write-Host "=== 1) ZeroTier service ===" -ForegroundColor Cyan
$svc = Get-Service -Name "ZeroTierOneService" -ErrorAction SilentlyContinue
if (-not $svc) {
    Write-Host "[ERROR] ZeroTierOneService not found. Is ZeroTier One installed?" -ForegroundColor Red
    exit 2
}
Write-Host ("Status before: {0}" -f $svc.Status)
if ($svc.Status -ne "Running") {
    Start-Service ZeroTierOneService
} else {
    Restart-Service ZeroTierOneService -Force
}
Start-Sleep -Seconds 5
$svc.Refresh()
Write-Host ("Status after:  {0}" -f $svc.Status)
if ($svc.Status -ne "Running") {
    Write-Host "[ERROR] Service failed to stay Running. Check services.msc." -ForegroundColor Red
    exit 3
}

Write-Host ""
Write-Host "=== 2) Set ZeroTier profile to Private ===" -ForegroundColor Cyan
$profiles = @(Get-NetConnectionProfile | Where-Object {
    $_.InterfaceAlias -like "ZeroTier*" -or $_.Name -like "*ZeroTier*"
})
if ($profiles.Count -eq 0) {
    Write-Host "[WARN] No ZeroTier connection profile yet. Join a network in ZeroTier UI, then re-run." -ForegroundColor Yellow
} else {
    foreach ($pr in $profiles) {
        Write-Host ("Before: Index={0} Alias='{1}' Category={2}" -f $pr.InterfaceIndex, $pr.InterfaceAlias, $pr.NetworkCategory)
        try {
            Set-NetConnectionProfile -InterfaceIndex $pr.InterfaceIndex -NetworkCategory Private -ErrorAction Stop
            Start-Sleep -Seconds 1
            $after = Get-NetConnectionProfile -InterfaceIndex $pr.InterfaceIndex
            Write-Host ("After:  Index={0} Alias='{1}' Category={2}" -f $after.InterfaceIndex, $after.InterfaceAlias, $after.NetworkCategory)
            if ($after.NetworkCategory -ne "Private") {
                Write-Host "[WARN] Still not Private (NLA may reset it). Firewall rules below still help." -ForegroundColor Yellow
            }
        } catch {
            Write-Host ("[WARN] Set-NetConnectionProfile failed: {0}" -f $_.Exception.Message) -ForegroundColor Yellow
        }
    }
    Write-Host "Wait 30s then re-check (NLA sometimes flips Public again)..."
    Start-Sleep -Seconds 30
    foreach ($pr in $profiles) {
        $now = Get-NetConnectionProfile -InterfaceIndex $pr.InterfaceIndex -ErrorAction SilentlyContinue
        if ($now) {
            Write-Host ("30s later: Index={0} Category={1}" -f $now.InterfaceIndex, $now.NetworkCategory)
        }
    }
}

Write-Host ""
Write-Host "=== 3) Firewall: Minecraft TCP + ZeroTier ===" -ForegroundColor Cyan

$ruleMc = "MCWWS Minecraft $MinecraftPort"
Get-NetFirewallRule -DisplayName $ruleMc -ErrorAction SilentlyContinue | Remove-NetFirewallRule -ErrorAction SilentlyContinue
New-NetFirewallRule -DisplayName $ruleMc -Direction Inbound -Protocol TCP -LocalPort $MinecraftPort -Action Allow -Profile Any | Out-Null
Write-Host ("[OK] Inbound TCP {0} allowed (Profile Any)" -f $MinecraftPort)

$candidates = @(
    $ZeroTierExe,
    "${env:ProgramFiles(x86)}\ZeroTier\One\zerotier-one_x64.exe",
    "${env:ProgramFiles}\ZeroTier\One\zerotier-one_x64.exe",
    "${env:ProgramFiles(x86)}\ZeroTier\One\zerotier-one.exe",
    "${env:ProgramFiles}\ZeroTier\One\zerotier-one.exe"
) | Where-Object { $_ -and (Test-Path $_) }

$ruleZt = "MCWWS ZeroTier UDP"
Get-NetFirewallRule -DisplayName $ruleZt -ErrorAction SilentlyContinue | Remove-NetFirewallRule -ErrorAction SilentlyContinue
if ($candidates.Count -gt 0) {
    $exe = $candidates[0]
    New-NetFirewallRule -DisplayName $ruleZt -Direction Inbound -Protocol UDP -Action Allow -Profile Any -Program $exe | Out-Null
    Write-Host ("[OK] Inbound UDP for ZeroTier: {0}" -f $exe)
} else {
    New-NetFirewallRule -DisplayName $ruleZt -Direction Inbound -Protocol UDP -Action Allow -Profile Any | Out-Null
    Write-Host "[OK] Inbound UDP for all programs (ZeroTier exe not found; broad rule applied)" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "=== 4) Local port check ===" -ForegroundColor Cyan
$tnc = Test-NetConnection -ComputerName 127.0.0.1 -Port $MinecraftPort -WarningAction SilentlyContinue
if ($tnc.TcpTestSucceeded) {
    Write-Host ("[OK] localhost:{0} is listening" -f $MinecraftPort) -ForegroundColor Green
} else {
    Write-Host ("[WARN] localhost:{0} not listening — start Paper first, then ask friend to reconnect." -ForegroundColor Yellow)
}

Write-Host ""
Write-Host "=== Done ===" -ForegroundColor Green
Write-Host "Friend should: ping your ZeroTier IP, then Test-NetConnection <ZT-IP> -Port $MinecraftPort"
Write-Host "If tray still shows 'Waiting for ZeroTier system service...', reboot once after this script."
