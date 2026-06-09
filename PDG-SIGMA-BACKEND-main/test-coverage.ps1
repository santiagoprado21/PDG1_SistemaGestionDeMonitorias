$ErrorActionPreference = "Stop"

Write-Host "=== EJECUTANDO TESTS CON JACOCO ===" -ForegroundColor Cyan
Write-Host ""

# Run Maven with tests and coverage
.\mvnw.cmd clean verify
if ($LASTEXITCODE -ne 0) {
    Write-Host "`nERROR: Los tests fallaron" -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "          COVERAGE SUMMARY" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green

$csvPath = "target\site\jacoco\jacoco.csv"
if (Test-Path $csvPath) {
    $csv = Import-Csv $csvPath

    $totalInst = ($csv | Measure-Object -Property INSTRUCTION_MISSED -Sum).Sum + ($csv | Measure-Object -Property INSTRUCTION_COVERED -Sum).Sum
    $coveredInst = ($csv | Measure-Object -Property INSTRUCTION_COVERED -Sum).Sum
    $totalBranch = ($csv | Measure-Object -Property BRANCH_MISSED -Sum).Sum + ($csv | Measure-Object -Property BRANCH_COVERED -Sum).Sum
    $coveredBranch = ($csv | Measure-Object -Property BRANCH_COVERED -Sum).Sum
    $totalLines = ($csv | Measure-Object -Property LINE_MISSED -Sum).Sum + ($csv | Measure-Object -Property LINE_COVERED -Sum).Sum
    $coveredLines = ($csv | Measure-Object -Property LINE_COVERED -Sum).Sum
    $totalMethods = ($csv | Measure-Object -Property METHOD_MISSED -Sum).Sum + ($csv | Measure-Object -Property METHOD_COVERED -Sum).Sum
    $coveredMethods = ($csv | Measure-Object -Property METHOD_COVERED -Sum).Sum

    function Get-Color($pct) {
        if ($pct -ge 80) { return "Green" } else { return "Yellow" }
    }

    Write-Host ""
    $instPct = [math]::Round($coveredInst/$totalInst*100,2)
    Write-Host ("  {0,-15} {1,8}% ({2,6}/{3,-6})" -f "Instructions", $instPct, $coveredInst, $totalInst) -ForegroundColor (Get-Color $instPct)
    $branchPct = [math]::Round($coveredBranch/$totalBranch*100,2)
    Write-Host ("  {0,-15} {1,8}% ({2,6}/{3,-6})" -f "Branches", $branchPct, $coveredBranch, $totalBranch) -ForegroundColor (Get-Color $branchPct)
    $linesPct = [math]::Round($coveredLines/$totalLines*100,2)
    Write-Host ("  {0,-15} {1,8}% ({2,6}/{3,-6})" -f "Lines", $linesPct, $coveredLines, $totalLines) -ForegroundColor (Get-Color $linesPct)
    $methodsPct = [math]::Round($coveredMethods/$totalMethods*100,2)
    Write-Host ("  {0,-15} {1,8}% ({2,6}/{3,-6})" -f "Methods", $methodsPct, $coveredMethods, $totalMethods) -ForegroundColor (Get-Color $methodsPct)

    Write-Host ""
    Write-Host "Reporte HTML: target/site/jacoco/index.html" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Green
} else {
    Write-Host "No se encontró el reporte CSV en $csvPath" -ForegroundColor Red
}
