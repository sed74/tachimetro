package com.sed.tachimetro

/**
 * Etichetta di versione mostrata in piccolo sul telefono e sulla Surface di Android Auto,
 * cosi' l'utente capisce a colpo d'occhio quale build sta girando (quick 260929-cyv).
 *
 * Chiamanti: `MainActivity` (alto al centro) e `SpeedSurfaceRenderer` (basso a destra della
 * stable area), entrambi con `BuildConfig.VERSION_NAME` / `BuildConfig.VERSION_CODE`.
 *
 * Funzione pura, senza dipendenze Android: unit-testabile su JVM.
 *
 * @param versionName nome di versione (es. "2.1-beta"); spazi ai bordi rimossi, "?" se vuoto
 * @param versionCode codice di versione intero (es. 6)
 * @return etichetta nel formato "v2.1-beta (6)"
 */
fun formatVersionLabel(versionName: String, versionCode: Int): String {
    val name = versionName.trim().ifEmpty { "?" }
    return "v$name ($versionCode)"
}
