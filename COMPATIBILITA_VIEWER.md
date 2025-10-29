# Documentazione Compatibilità PdfActivity con librerie viewer/

## Indice
1. [Panoramica](#panoramica)
2. [Problemi Identificati](#problemi-identificati)
3. [Correzioni Implementate](#correzioni-implementate)
4. [Architettura delle librerie viewer/](#architettura-delle-librerie-viewer)
5. [Guida all'uso corretto](#guida-alluso-corretto)
6. [Metodi e Classi Principali](#metodi-e-classi-principali)

---

## Panoramica

Il file `PdfActivity.java` utilizza diverse classi dalla cartella `src/android/viewer/` per gestire la visualizzazione e interazione con documenti PDF. Questo documento descrive i problemi di compatibilità identificati, le correzioni implementate e come utilizzare correttamente queste librerie.

---

## Problemi Identificati

### 1. Incompatibilità con OutlineActivity

**Problema:**
- `PdfActivity` utilizzava `intent.putExtras(bundle)` per passare i dati all'`OutlineActivity`
- `OutlineActivity` si aspetta invece di ricevere il bundle tramite il meccanismo `Pallet` usando la chiave `"PALLETBUNDLE"`

**File coinvolti:**
- `src/android/PdfActivity.java` (linee 321-328)
- `src/android/viewer/OutlineActivity.java` (linea 41)
- `src/android/viewer/Pallet.java`

**Conseguenza:**
L'outline non funzionava correttamente perché i dati non venivano passati nel formato corretto.

### 2. Metodi mancanti in PageView per supporto watermark

**Problema:**
Il codice in `PdfActivity.java` tentava di utilizzare:
- `pageView.setmAfterViewRenderedHandler()` - metodo non esistente
- `pageView.getmEntireBm()` - metodo non esistente  
- `PageView.OnAfterImageRenderedListener` - interfaccia non esistente

**File coinvolti:**
- `src/android/PdfActivity.java` (linee 692-702)
- `src/android/viewer/PageView.java`

**Conseguenza:**
Il watermark non poteva essere applicato alle pagine PDF perché mancavano i metodi necessari per intercettare il rendering.

---

## Correzioni Implementate

### Correzione 1: Uso corretto di Pallet con OutlineActivity

**Modifica in `PdfActivity.java`:**

```java
// PRIMA (ERRATO):
Intent intent = new Intent(PdfActivity.this, OutlineActivity.class);
Bundle bundle = new Bundle();
bundle.putInt("POSITION", mDocView.getDisplayedViewIndex());
bundle.putSerializable("OUTLINE", mFlatOutline);
intent.putExtras(bundle);
startActivityForResult(intent, OUTLINE_REQUEST);

// DOPO (CORRETTO):
Intent intent = new Intent(PdfActivity.this, OutlineActivity.class);
Bundle bundle = new Bundle();
bundle.putInt("POSITION", mDocView.getDisplayedViewIndex());
bundle.putSerializable("OUTLINE", mFlatOutline);
intent.putExtra("PALLETBUNDLE", Pallet.sendBundle(bundle));
startActivityForResult(intent, OUTLINE_REQUEST);
```

**Aggiunto import:**
```java
import net.kuama.pdf.viewer.Pallet;
```

**Meccanismo Pallet:**
Il sistema `Pallet` è un meccanismo di scambio dati basato su una struttura HashMap condivisa. Questo permette di passare oggetti `Bundle` serializzabili tra activity senza dover gestire i limiti di dimensione degli Intent extras.

### Correzione 2: Aggiunta supporto watermark in PageView

**Modifiche in `PageView.java`:**

#### 1. Aggiunta interfaccia listener

```java
public interface OnAfterImageRenderedListener {
    void afterRender(ImageView mEntire);
}
```

#### 2. Aggiunto campo privato

```java
private OnAfterImageRenderedListener mAfterViewRenderedHandler;
```

#### 3. Aggiunti metodi pubblici

```java
public void setmAfterViewRenderedHandler(OnAfterImageRenderedListener handler) {
    mAfterViewRenderedHandler = handler;
}

public Bitmap getmEntireBm() {
    return mEntireBm;
}
```

#### 4. Chiamata del listener dopo il rendering

Nel metodo `setPage()`:
```java
@Override
public void onPostExecute(Boolean result) {
    removeView(mBusyIndicator);
    mBusyIndicator = null;
    if (result.booleanValue()) {
        clearRenderError();
        mEntire.setImageBitmap(mEntireBm);
        mEntire.invalidate();
        // Call the after render handler if set
        if (mAfterViewRenderedHandler != null) {
            mAfterViewRenderedHandler.afterRender(mEntire);
        }
    } else {
        setRenderError("Error rendering page");
    }
    setBackgroundColor(Color.TRANSPARENT);
}
```

Nel metodo `update()`:
```java
public void onPostExecute(Boolean result) {
    if (result.booleanValue()) {
        clearRenderError();
        mEntire.setImageBitmap(mEntireBm);
        mEntire.invalidate();
        // Call the after render handler if set
        if (mAfterViewRenderedHandler != null) {
            mAfterViewRenderedHandler.afterRender(mEntire);
        }
    } else {
        setRenderError("Error updating page");
    }
}
```

---

## Architettura delle librerie viewer/

### Struttura delle classi

```
viewer/
├── MuPDFCore.java          - Core rendering del PDF
├── ReaderView.java          - View principale per la navigazione
├── PageView.java            - View per singola pagina
├── PageAdapter.java         - Adapter per le pagine
├── SearchTask.java          - Task per ricerca testo
├── SearchTaskResult.java    - Risultato della ricerca
├── OutlineActivity.java     - Activity per navigazione outline
├── DocumentActivity.java    - Activity di riferimento (non usata da PdfActivity)
├── Pallet.java              - Sistema di scambio dati tra activity
├── ContentInputStream.java  - Stream wrapper per contenuti
├── CancellableAsyncTask.java - Task asincroni cancellabili
└── CancellableTaskDefinition.java - Interfaccia per task
```

### Flusso di lavoro principale

```
PdfActivity
    │
    ├── Crea MuPDFCore (da buffer base64)
    │
    ├── Crea ReaderView
    │   │
    │   └── Setta PageAdapter o WatermarkAdapter
    │       │
    │       └── Crea PageView per ogni pagina
    │           │
    │           └── Renderizza tramite MuPDFCore
    │
    ├── Crea SearchTask per ricerca testo
    │
    └── Gestisce OutlineActivity per navigazione
```

---

## Guida all'uso corretto

### 1. Inizializzazione MuPDFCore

```java
// Apri da buffer
byte buffer[] = Base64.decode(base64pdf, Base64.DEFAULT);
core = new MuPDFCore(buffer, "application/pdf");

// Verificare se aperto correttamente
if (core == null) {
    // Gestire errore
}

// Verificare password se necessario
if (core.needsPassword()) {
    // Richiedere password e autenticare
    core.authenticatePassword(password);
}
```

### 2. Creazione ReaderView con adapter

```java
// Adapter standard
mDocView.setAdapter(new PageAdapter(this, core));

// Adapter con watermark
mDocView.setAdapter(new WatermarkAdapter(this, core, watermarkText));
```

### 3. Impostazione watermark su PageView

```java
PageView pageView = new PageView(mContext, mCore, 
    new Point(parent.getWidth(), parent.getHeight()), 
    mSharedHqBm);

pageView.setmAfterViewRenderedHandler(new PageView.OnAfterImageRenderedListener() {
    @Override
    public void afterRender(ImageView mEntire) {
        Bitmap bitmap = pageView.getmEntireBm();
        // Applicare watermark al bitmap
        WatermarkBuilder
            .create(mContext, bitmap)
            .loadWatermarkText(mWatermarkText)
            .setTileMode(true)
            .getWatermark()
            .setToImageView(mEntire);
    }
});
```

### 4. Passaggio dati a OutlineActivity

```java
// CORRETTO: Usare Pallet
Intent intent = new Intent(this, OutlineActivity.class);
Bundle bundle = new Bundle();
bundle.putInt("POSITION", currentPage);
bundle.putSerializable("OUTLINE", outlineList);
intent.putExtra("PALLETBUNDLE", Pallet.sendBundle(bundle));
startActivityForResult(intent, OUTLINE_REQUEST);

// SBAGLIATO: Non usare putExtras direttamente
// intent.putExtras(bundle); // NON FUNZIONA
```

### 5. Gestione risultato da OutlineActivity

```java
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
        case OUTLINE_REQUEST:
            if (resultCode >= RESULT_FIRST_USER) {
                mDocView.pushHistory();
                mDocView.setDisplayedViewIndex(resultCode - RESULT_FIRST_USER);
            }
            break;
    }
    super.onActivityResult(requestCode, resultCode, data);
}
```

### 6. Implementazione SearchTask

```java
mSearchTask = new SearchTask(this, core) {
    @Override
    protected void onTextFound(SearchTaskResult result) {
        SearchTaskResult.set(result);
        mDocView.setDisplayedViewIndex(result.pageNumber);
        mDocView.resetupChildren();
    }
};

// Avviare ricerca
mSearchTask.go(searchText, direction, displayPage, searchPage);

// Fermare ricerca
mSearchTask.stop();
```

---

## Metodi e Classi Principali

### MuPDFCore

**Costruttori:**
- `MuPDFCore(byte buffer[], String magic)` - Apre da buffer in memoria
- `MuPDFCore(SeekableInputStream stm, String magic)` - Apre da stream

**Metodi principali:**
- `int countPages()` - Numero di pagine
- `String getTitle()` - Titolo del documento
- `boolean hasOutline()` - Verifica se ha outline
- `ArrayList<OutlineActivity.Item> getOutline()` - Ottiene outline
- `PointF getPageSize(int pageNum)` - Dimensioni pagina
- `void onDestroy()` - Cleanup risorse

### ReaderView

**Metodi principali:**
- `void setAdapter(PageAdapter adapter)` - Imposta adapter
- `void setDisplayedViewIndex(int i)` - Cambia pagina visualizzata
- `int getDisplayedViewIndex()` - Ottiene pagina corrente
- `void pushHistory()` - Salva posizione nello storico
- `boolean popHistory()` - Torna alla posizione precedente
- `void resetupChildren()` - Ricrea i child views
- `void applyToChildren(ViewMapper mapper)` - Applica funzione a tutti i child

### PageView

**Costruttore:**
- `PageView(Context c, MuPDFCore core, Point parentSize, Bitmap sharedHqBm)`

**Metodi principali:**
- `void setPage(int page, PointF size)` - Imposta pagina da visualizzare
- `void blank(int page)` - Mostra pagina vuota (in caricamento)
- `void releaseBitmaps()` - Rilascia risorse bitmap
- `void releaseResources()` - Rilascia tutte le risorse
- `void updateHq(boolean update)` - Aggiorna rendering ad alta qualità
- `void removeHq()` - Rimuove rendering ad alta qualità
- `int getPage()` - Ottiene numero pagina corrente
- `void setmAfterViewRenderedHandler(OnAfterImageRenderedListener handler)` - Setta handler per watermark
- `Bitmap getmEntireBm()` - Ottiene bitmap della pagina

### PageAdapter

**Costruttore:**
- `PageAdapter(Context c, MuPDFCore core)`

**Metodi principali:**
- `int getCount()` - Numero di pagine
- `View getView(int position, View convertView, ViewGroup parent)` - Ottiene view per pagina
- `void releaseBitmaps()` - Rilascia bitmap condivise
- `void refresh()` - Ricarica adapter

### SearchTask

**Costruttore:**
- `SearchTask(Context context, MuPDFCore core)`

**Metodi principali:**
- `void go(String text, int direction, int displayPage, int searchPage)` - Avvia ricerca
- `void stop()` - Ferma ricerca

**Metodo astratto da implementare:**
- `protected abstract void onTextFound(SearchTaskResult result)` - Callback quando trova testo

### SearchTaskResult

**Metodi statici:**
- `static SearchTaskResult get()` - Ottiene risultato corrente
- `static void set(SearchTaskResult r)` - Imposta risultato corrente

**Campi:**
- `final String txt` - Testo cercato
- `final int pageNumber` - Numero pagina trovata
- `final Quad searchBoxes[][]` - Box di highlight

### Pallet

**Metodi statici:**
- `static int sendBundle(Bundle bundle)` - Invia bundle, ritorna ID
- `static Bundle receiveBundle(int number)` - Riceve bundle tramite ID
- `static boolean hasBundle(int number)` - Verifica se esiste bundle

**Utilizzo:**
Il sistema Pallet usa una HashMap condivisa per permettere lo scambio di Bundle tra activity. Gli ID sono sequenziali e auto-incrementali.

### OutlineActivity

**Dati richiesti:**
- `"PALLETBUNDLE"` (int) - ID del bundle in Pallet
- Bundle deve contenere:
  - `"POSITION"` (int) - Pagina corrente
  - `"OUTLINE"` (ArrayList<OutlineActivity.Item>) - Lista outline

**Risultato:**
- Ritorna `RESULT_FIRST_USER + pageNumber` se una voce viene selezionata

---

## Checklist di Verifica Compatibilità

Quando si modifica `PdfActivity` o le librerie `viewer/`, verificare:

- [ ] `MuPDFCore` viene creato correttamente da buffer o stream
- [ ] `ReaderView` riceve un `PageAdapter` valido
- [ ] `PageView` ha tutti i metodi necessari per il watermark (se usato)
- [ ] `OutlineActivity` riceve i dati tramite `Pallet`, non `putExtras`
- [ ] `SearchTask` viene fermato in `onPause()`
- [ ] Le risorse vengono rilasciate correttamente in `onDestroy()`
- [ ] Gli import di tutte le classi `viewer/` sono presenti

---

## Note Finali

### Differenze con DocumentActivity

`DocumentActivity` nella cartella `viewer/` è una versione più completa che gestisce:
- Apertura documenti da URI
- Reflowable documents (ePub, XPS)
- Password protection
- Layout settings per documenti reflowable

`PdfActivity` è una versione semplificata che:
- Gestisce solo buffer base64
- Supporta watermark
- Non supporta documenti reflowable

### Best Practices

1. **Sempre fermare SearchTask**: Chiamare `mSearchTask.stop()` in `onPause()`
2. **Rilasciare risorse**: Chiamare `core.onDestroy()` e `releaseBitmaps()` in `onDestroy()`
3. **Gestire null**: Verificare sempre `core != null` prima di usare
4. **Usare Pallet**: Non usare mai `putExtras()` per OutlineActivity
5. **Handler watermark**: Il listener viene chiamato solo se settato, quindi safe

---

## Revisioni

**Data: 2024**
- Verificata compatibilità PdfActivity con librerie viewer/
- Corretto uso di Pallet con OutlineActivity
- Aggiunto supporto watermark in PageView
- Documentazione completa creata

---

## Riferimenti

- File principali:
  - `src/android/PdfActivity.java`
  - `src/android/viewer/PageView.java`
  - `src/android/viewer/ReaderView.java`
  - `src/android/viewer/MuPDFCore.java`
  - `src/android/viewer/OutlineActivity.java`
  - `src/android/viewer/Pallet.java`

