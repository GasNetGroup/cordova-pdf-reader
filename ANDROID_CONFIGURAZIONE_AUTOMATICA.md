# Configurazione Automatica Android

## Problema

Quando si cancella e si ricrea la platform Android, erano necessarie configurazioni manuali scomode:

1. **Libreria androidwm-light non disponibile su Maven**: La dipendenza `com.huangyz0918:androidwm-light:0.1.2` non è più disponibile nei repository Maven pubblici, causando errori di build.

2. **Configurazione colori mancante**: Il file `platforms/android/app/src/main/res/values/colors.xml` manca dei colori necessari per il plugin, causando errori di build.

## Soluzione Implementata

A partire dalla versione **0.1.2**, il plugin gestisce automaticamente entrambi i problemi durante l'installazione:

### 1. Libreria androidwm-light Locale

La libreria `androidwm-light` è ora inclusa direttamente nel plugin e viene configurata automaticamente.

#### Configurazione in `plugin.xml`

```xml
<!-- Libreria androidwm-light locale (non più disponibile su Maven) -->
<lib-file src="src/android/androidwm-light/androidwm-light-0.1.2.aar"/>
```

Il tag `<lib-file>` copia automaticamente il file `.aar` nella cartella `libs/` della platform Android durante l'installazione del plugin.

#### Configurazione in `src/android/plugin.gradle`

```gradle
repositories {
    jcenter() // needed to resolve mupdf
    maven { url 'https://maven.ghostscript.com' } // needed to resolve androidwm-light (non più disponibile, ma mantenuto per compatibilità)
    
    // Repository locale per androidwm-light (copiato automaticamente in libs/ dal plugin.xml)
    flatDir {
        dirs 'libs'
    }
}

dependencies {
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'

    // Utilizzo della libreria locale invece della dipendenza Maven
    // Il file .aar viene copiato automaticamente in libs/ durante l'installazione del plugin
    implementation(name: 'androidwm-light-0.1.2', ext: 'aar')
    // implementation 'com.huangyz0918:androidwm-light:0.1.2' // Originale Maven (non più disponibile, ripristinare se torna disponibile)

    api 'com.artifex.mupdf:fitz:1.26.10'
}
```

**Dettagli:**
- Viene aggiunto un repository `flatDir` che cerca nella cartella `libs/`
- La dipendenza viene risolta dal file locale invece che da Maven
- La dipendenza Maven originale è commentata per permettere un facile ripristino in futuro se dovesse tornare disponibile

### 2. Configurazione Automatica dei Colori

I colori necessari vengono aggiunti automaticamente al file `colors.xml` durante l'installazione del plugin.

#### Configurazione in `plugin.xml`

```xml
<config-file target="res/values/colors.xml" parent="/resources">
    <color name="colorPrimary">#008577</color>
    <color name="colorPrimaryDark">#00574B</color>
    <color name="colorAccent">#D81B60</color>
    <color name="page_indicator">#C0202020</color>
    <color name="toolbar">#C0202020</color>
</config-file>
```

Il tag `<config-file>` aggiunge automaticamente i colori necessari al file `colors.xml` esistente (o lo crea se non esiste).

## Vantaggi

1. **Automatico**: Tutto viene configurato automaticamente durante l'installazione del plugin
2. **Permanente**: La configurazione è parte integrante del plugin, non della platform
3. **Nessuna configurazione manuale**: Non è più necessario modificare file manualmente dopo aver ricreato la platform Android
4. **Ripristinabile**: La dipendenza Maven originale è commentata, pronta per essere riattivata se dovesse tornare disponibile

## Utilizzo

Non è necessario fare nulla di speciale! Quando installi il plugin:

```bash
cordova plugin add <path-to-plugin>
```

oppure quando ricrei la platform Android:

```bash
cordova platform remove android
cordova platform add android
```

Il plugin configurerà automaticamente:
- La libreria `androidwm-light` locale nella cartella `libs/`
- I colori necessari in `res/values/colors.xml`

## Struttura File della Libreria Locale

La libreria `androidwm-light` è inclusa nel plugin nella seguente posizione:

```
src/android/androidwm-light/
├── androidwm-light-0.1.2.aar        (file principale utilizzato)
├── androidwm-light-0.1.2.jar
├── androidwm-light-0.1.2-javadoc.jar
├── androidwm-light-0.1.2-sources.jar
└── androidwm-light-0.1.2.pom
```

Solo il file `.aar` viene copiato nella platform Android durante l'installazione.

## Ripristino Dipendenza Maven (se dovesse tornare disponibile)

Se in futuro la libreria `androidwm-light` dovesse tornare disponibile su Maven, è possibile ripristinare la dipendenza originale modificando `src/android/plugin.gradle`:

1. Commentare la riga con la libreria locale:
   ```gradle
   // implementation(name: 'androidwm-light-0.1.2', ext: 'aar')
   ```

2. Decommentare la riga con la dipendenza Maven:
   ```gradle
   implementation 'com.huangyz0918:androidwm-light:0.1.2'
   ```

3. Rimuovere il repository `flatDir` se non più necessario (opzionale)

4. Rimuovere il tag `<lib-file>` da `plugin.xml` (opzionale)

## Note Tecniche

- Il repository Maven `https://maven.ghostscript.com` è mantenuto nel file `plugin.gradle` per compatibilità futura, anche se attualmente la libreria non è disponibile
- Il repository `flatDir` viene aggiunto alle configurazioni Gradle esistenti senza sovrascrivere altre configurazioni
- I colori vengono aggiunti al file `colors.xml` esistente, preservando eventuali colori già presenti
- Se il file `colors.xml` non esiste, verrà creato automaticamente con la dichiarazione XML appropriata
