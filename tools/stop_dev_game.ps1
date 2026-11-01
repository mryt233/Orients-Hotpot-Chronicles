# 只关 ./gradlew runClient / runServer 开出来的开发版游戏，不碰用户自己用启动器（PCL）开的游戏。
# 坑：正式的 Fabric 游戏主类也是 KnotClient，按 Knot 筛会把用户在玩的游戏一起关掉（用户报）。
# 开发版独有：命令行里有 devlaunchinjector 或 -Dfabric.development=true，且带本工作区路径。
#   powershell -File tools/stop_dev_game.ps1
# 作者：mryt233
$procs = Get-CimInstance Win32_Process -Filter "Name like 'java%'" | Where-Object {
    $c = $_.CommandLine
    $c -and ($c -match 'devlaunchinjector' -or $c -match 'fabric\.development=true') -and $c -match 'MC mod dev'
}
if (-not $procs) { "没有开发版游戏在运行"; exit 0 }
foreach ($p in $procs) {
    Stop-Process -Id $p.ProcessId -Force
    "已关闭开发版游戏 $($p.ProcessId)"
}
