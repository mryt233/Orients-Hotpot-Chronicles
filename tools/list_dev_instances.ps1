# 列出本工程开发服/客户端残留进程（只查看，不结束）。作者：mryt233
$procs = Get-CimInstance Win32_Process -Filter "Name like 'java%'" | Where-Object {
    $c = $_.CommandLine
    $c -and ($c -match 'MC mod dev') -and ($c -match 'run-server|run-client|fabric\.development|forgeclientdev|foruserverdev|gradle')
}
if (-not $procs) { "没有本工程开发实例在运行"; exit 0 }
foreach ($p in $procs) {
    $short = $p.CommandLine
    if ($short.Length -gt 160) { $short = $short.Substring(0, 160) }
    "PID $($p.ProcessId): $short"
}
