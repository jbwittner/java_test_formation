#!/usr/bin/env pwsh
#
# Préflight du parcours autonome : vérifie que la machine peut exécuter
# l'intégralité de la formation. À lancer une fois, avant le premier exercice.
#
#   pwsh -File outils/verifier-environnement.ps1
#   (ou, depuis une console PowerShell : .\outils\verifier-environnement.ps1)
#
# Code de retour 0 si tout est vert, 1 sinon.

Set-StrictMode -Version Latest
# Surtout pas $ErrorActionPreference = 'Stop' : en Windows PowerShell 5.1, la
# moindre ligne écrite sur stderr par java/docker deviendrait une erreur
# terminante. Le script pilote ses échecs via $LASTEXITCODE.
$ErrorActionPreference = 'Continue'

$Racine = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $Racine

$ImagePostgres = 'postgres:17-alpine'
$ImagePubsub   = 'gcr.io/google.com/cloudsdktool/google-cloud-cli:579.0.0-emulators'

$script:Echecs = 0

# Les couleurs ANSI ne sont pas rendues par les vieilles consoles Windows :
# on passe par Write-Host -ForegroundColor, qui fonctionne partout.
function Write-Ok   { param([string]$Message) Write-Host '  OK   ' -ForegroundColor Green -NoNewline; Write-Host " $Message" }
function Write-Ko   { param([string]$Message) Write-Host '  KO   ' -ForegroundColor Red   -NoNewline; Write-Host " $Message"; $script:Echecs++ }
function Write-Info { param([string]$Message) Write-Host "        $Message" }
function Write-Titre { param([string]$Message) Write-Host ''; Write-Host $Message -ForegroundColor White }

function Test-Commande {
    param([string]$Nom)
    $null -ne (Get-Command $Nom -ErrorAction SilentlyContinue)
}

Write-Titre '1. JDK 21 ou plus'
if (-not (Test-Commande 'java')) {
    Write-Ko 'java introuvable dans le PATH'
    Write-Info 'Installer un JDK 21+ (https://adoptium.net) puis relancer.'
} else {
    # `java -version` écrit sur stderr : 2>&1 le ramène dans le pipeline, où
    # 5.1 peut livrer un seul ErrorRecord multiligne — d'où le re-découpage.
    $sortie = (& java -version 2>&1 | ForEach-Object { $_.ToString() }) -join "`n"
    $version = ($sortie -split "`r?`n" | Where-Object { $_.Trim() } | Select-Object -First 1).Trim()
    $majeure = 0
    if ($version -match '"(\d+)') { $majeure = [int]$Matches[1] }
    if ($majeure -ge 21) {
        Write-Ok $version
    } else {
        Write-Ko "$version — la formation exige Java 21 ou plus"
    }
}

Write-Titre '2. Docker'
$dockerPret = $false
if (-not (Test-Commande 'docker')) {
    Write-Ko 'docker introuvable dans le PATH'
    Write-Info "Les modules 02, 03 et 04 ne pourront pas s'exécuter."
} else {
    & docker info *> $null
    if ($LASTEXITCODE -ne 0) {
        Write-Ko 'le démon Docker ne répond pas'
        Write-Info 'Démarrer Docker Desktop (ou le service docker) puis relancer.'
    } else {
        Write-Ok 'démon Docker démarré'
        $dockerPret = $true
    }
}

if ($dockerPret) {
    Write-Titre '3. Images Testcontainers'
    foreach ($image in @($ImagePostgres, $ImagePubsub)) {
        & docker image inspect $image *> $null
        if ($LASTEXITCODE -eq 0) {
            Write-Ok "$image déjà présente"
        } else {
            Write-Info "téléchargement de $image (plusieurs centaines de Mo)…"
            & docker pull $image *> $null
            if ($LASTEXITCODE -eq 0) {
                Write-Ok "$image téléchargée"
            } else {
                Write-Ko "échec du téléchargement de $image"
            }
        }
    }
}

Write-Titre '4. Suite complete (unitaires + integration, Docker requis)'
$journal = Join-Path ([System.IO.Path]::GetTempPath()) 'formation-preflight.log'
# $IsWindows n'existe pas en Windows PowerShell 5.1 : on retombe sur $env:OS.
$surWindows = if (Test-Path variable:IsWindows) { $IsWindows } else { $env:OS -eq 'Windows_NT' }
$mvnw = if ($surWindows) { Join-Path $Racine 'mvnw.cmd' } else { Join-Path $Racine 'mvnw' }
& $mvnw -q test *> $journal
if ($LASTEXITCODE -eq 0) {
    Write-Ok 'mvnw test est vert'
} else {
    Write-Ko 'mvnw test échoue sur un dépôt qui devrait être propre'
    Write-Info "Journal complet : $journal"
    Write-Info "Avez-vous déjà retiré un @Disabled ? Dans ce cas c'est normal."
}

Write-Titre 'Résultat'
if ($script:Echecs -eq 0) {
    Write-Host '  Environnement prêt.' -ForegroundColor Green -NoNewline
    Write-Host ' Commencez par docs/00-parcours-autonome.md'
    Write-Host ''
    exit 0
}
Write-Host "  $($script:Echecs) point(s) à corriger." -ForegroundColor Red -NoNewline
Write-Host ' Voir « Dépannage » dans README.md'
Write-Host ''
exit 1
