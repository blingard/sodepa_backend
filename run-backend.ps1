Get-Content .env.example | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith("#")) {
        $idx = $line.IndexOf("=")
        if ($idx -gt 0) {
            $key = $line.Substring(0, $idx).Trim()
            $val = $line.Substring($idx + 1).Trim()
            Set-Item -Path "env:$key" -Value $val
        }
    }
}
$env:PROFILE = "test"
$env:DB_DDL = "update"
$env:CLICKHOUSE_PASSWORD = ""
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=test"
