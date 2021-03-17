# RÚIAN Importer

Wrapper around [ruian2pgsql](https://github.com/fordfrog/ruian2pgsql).

## Specifikace postupu

### Prvotní import

1. Import plné dávky (Úplná kopie) "Stát až ZSJ" a "Obec a podřazené" 
2. Import změnových souborů (Přírůstky od data) od posledního dne předcházejícího měsíce.

### Následná aktualizace
1. Import změnových souborů od data posledního importu předchozího změnového souboru.

### Spuštění

    ruian-importer-1.0.0.jar --spring.config.location=~/application.yml