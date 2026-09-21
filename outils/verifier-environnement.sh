#!/usr/bin/env bash
#
# Préflight du parcours autonome : vérifie que la machine peut exécuter
# l'intégralité de la formation. À lancer une fois, avant le premier exercice.
#
#   bash outils/verifier-environnement.sh
#
# Code de retour 0 si tout est vert, 1 sinon.

set -u

RACINE="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$RACINE" || exit 1

IMAGE_POSTGRES="postgres:17-alpine"
IMAGE_PUBSUB="gcr.io/google.com/cloudsdktool/google-cloud-cli:579.0.0-emulators"

ECHECS=0

ok()   { printf '  \033[32mOK\033[0m    %s\n' "$1"; }
ko()   { printf '  \033[31mKO\033[0m    %s\n' "$1"; ECHECS=$((ECHECS + 1)); }
info() { printf '        %s\n' "$1"; }
titre(){ printf '\n\033[1m%s\033[0m\n' "$1"; }

titre "1. JDK 21 ou plus"
if ! command -v java > /dev/null 2>&1; then
    ko "java introuvable dans le PATH"
    info "Installer un JDK 21+ (https://adoptium.net) puis relancer."
else
    VERSION="$(java -version 2>&1 | head -1)"
    MAJEURE="$(java -version 2>&1 | head -1 | sed -E 's/.*"([0-9]+).*/\1/')"
    if [ "${MAJEURE:-0}" -ge 21 ] 2>/dev/null; then
        ok "$VERSION"
    else
        ko "$VERSION — la formation exige Java 21 ou plus"
    fi
fi

titre "2. Docker"
if ! command -v docker > /dev/null 2>&1; then
    ko "docker introuvable dans le PATH"
    info "Les modules 02, 03 et 04 ne pourront pas s'exécuter."
elif ! docker info > /dev/null 2>&1; then
    ko "le démon Docker ne répond pas"
    info "Démarrer Docker Desktop (ou le service docker) puis relancer."
else
    ok "démon Docker démarré"

    titre "3. Images Testcontainers"
    for image in "$IMAGE_POSTGRES" "$IMAGE_PUBSUB"; do
        if docker image inspect "$image" > /dev/null 2>&1; then
            ok "$image déjà présente"
        else
            info "téléchargement de $image (plusieurs centaines de Mo)…"
            if docker pull "$image" > /dev/null 2>&1; then
                ok "$image téléchargée"
            else
                ko "échec du téléchargement de $image"
            fi
        fi
    done
fi

titre "4. Suite complète (unitaires + intégration, Docker requis)"
if ./mvnw -q test > /tmp/formation-preflight.log 2>&1; then
    ok "./mvnw test est vert"
else
    ko "./mvnw test échoue sur un dépôt qui devrait être propre"
    info "Journal complet : /tmp/formation-preflight.log"
    info "Avez-vous déjà retiré un @Disabled ? Dans ce cas c'est normal."
fi

titre "Résultat"
if [ "$ECHECS" -eq 0 ]; then
    printf '  \033[32mEnvironnement prêt.\033[0m Commencez par docs/00-parcours-autonome.md\n\n'
    exit 0
fi
printf '  \033[31m%s point(s) à corriger.\033[0m Voir « Dépannage » dans README.md\n\n' "$ECHECS"
exit 1
