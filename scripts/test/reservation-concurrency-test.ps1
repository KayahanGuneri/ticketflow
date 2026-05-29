param(
    [string]$BaseUrl = "http://localhost:8081",
    [int]$TotalCapacity = 3,
    [int]$ParallelRequests = 8
)

Write-Host "Checking API availability..."

try {
    $health = Invoke-RestMethod `
        -Method Get `
        -Uri "$BaseUrl/actuator/health"

    Write-Host "API health:"
    $health | ConvertTo-Json -Depth 5
}
catch {
    Write-Host "API is not reachable at $BaseUrl"
    Write-Host "Please start ticketflow-api-service before running this script."
    Write-Host "Example:"
    Write-Host "cd C:\Users\kayah\Desktop\ticketflow\services\ticketflow-api-service"
    Write-Host 'mvn spring-boot:run "-Dspring-boot.run.profiles=local"'
    exit 1
}

Write-Host ""
Write-Host "Creating event for reservation concurrency test..."

$eventBody = @{
    name = "Reservation Concurrency Test"
    description = "Manual concurrency test for stock consistency."
    location = "Istanbul"
    startsAt = "2026-09-01T12:00:00Z"
    totalCapacity = $TotalCapacity
} | ConvertTo-Json

try {
    $event = Invoke-RestMethod `
        -Method Post `
        -Uri "$BaseUrl/api/v1/events" `
        -ContentType "application/json" `
        -Body $eventBody
}
catch {
    Write-Host "Failed to create test event."
    Write-Host $_.Exception.Message
    exit 1
}

$eventId = $event.id

if ([string]::IsNullOrWhiteSpace($eventId)) {
    Write-Host "Event id is empty. Stopping test."
    exit 1
}

Write-Host "Event created:"
$event | ConvertTo-Json -Depth 5

Write-Host ""
Write-Host "Starting parallel reservation requests..."

$jobs = 1..$ParallelRequests | ForEach-Object {
    $requestNumber = $_

    Start-Job -ScriptBlock {
        param($BaseUrl, $eventId, $requestNumber)

        $userId = [guid]::NewGuid().ToString()
        $idempotencyKey = "concurrency-test-key-$requestNumber-$([guid]::NewGuid().ToString())"

        $reservationBody = @{
            eventId = $eventId
            userId = $userId
            ticketCount = 1
        } | ConvertTo-Json

        try {
            $response = Invoke-WebRequest `
                -Method Post `
                -Uri "$BaseUrl/api/v1/reservations" `
                -Headers @{ "Idempotency-Key" = $idempotencyKey } `
                -ContentType "application/json" `
                -Body $reservationBody `
                -UseBasicParsing

            [PSCustomObject]@{
                requestNumber = $requestNumber
                statusCode = $response.StatusCode
                responseBody = $response.Content
            }
        }
        catch {
            $statusCode = "NO_RESPONSE"
            $responseBody = $_.Exception.Message

            if ($_.Exception.Response -ne $null) {
                $statusCode = $_.Exception.Response.StatusCode.value__
                $stream = $_.Exception.Response.GetResponseStream()

                if ($stream -ne $null) {
                    $reader = New-Object System.IO.StreamReader($stream)
                    $responseBody = $reader.ReadToEnd()
                }
            }

            [PSCustomObject]@{
                requestNumber = $requestNumber
                statusCode = $statusCode
                responseBody = $responseBody
            }
        }
    } -ArgumentList $BaseUrl, $eventId, $requestNumber
}

$results = $jobs | Wait-Job | Receive-Job
$jobs | Remove-Job

Write-Host ""
Write-Host "Reservation request results:"
$results | Sort-Object requestNumber | Format-Table -AutoSize

Write-Host ""
Write-Host "Final event stock:"

$finalEvent = Invoke-RestMethod `
    -Method Get `
    -Uri "$BaseUrl/api/v1/events/$eventId"

$finalEvent | ConvertTo-Json -Depth 5

$successCount = ($results | Where-Object { $_.statusCode -eq 201 }).Count

Write-Host ""
Write-Host "Summary:"
Write-Host "Total capacity:" $TotalCapacity
Write-Host "Parallel requests:" $ParallelRequests
Write-Host "Successful reservation count:" $successCount
Write-Host "Final available capacity:" $finalEvent.availableCapacity
Write-Host "Final reserved capacity:" $finalEvent.reservedCapacity

Write-Host ""
Write-Host "Expected safety rule:"
Write-Host "availableCapacity must never be negative."
Write-Host "reservedCapacity must never exceed totalCapacity."
Write-Host "Successful reservation count must not exceed totalCapacity."
