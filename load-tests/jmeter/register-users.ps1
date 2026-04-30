$ErrorActionPreference = "Stop"

$csvPath = "load-tests/jmeter/users.csv"
$registerUrl = "http://localhost:8080/api/auth/register"

if (-not (Test-Path $csvPath)) {
    Write-Error "users.csv not found at $csvPath"
}

$users = Import-Csv -Path $csvPath

foreach ($user in $users) {
    $payload = @{
        name = $user.name
        email = $user.email
        password = $user.password
    } | ConvertTo-Json

    try {
        $null = Invoke-RestMethod -Method Post -Uri $registerUrl -ContentType "application/json" -Body $payload
        Write-Host "Registered user: $($user.email)"
    }
    catch {
        $message = $_.Exception.Message
        if ($message -like "*400*" -or $message -like "*already registered*") {
            Write-Host "User already exists (ignored): $($user.email)"
        }
        else {
            throw
        }
    }
}

Write-Host "Done."
