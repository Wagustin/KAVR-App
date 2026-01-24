package com.thanhng224.app.feature.games

// Usamos un archivo de dominio para compartir modelos y constantes entre el ViewModel y la UI.

/** Coordenada en el tablero del juego. */
typealias Point = Pair<Int, Int>

/** Representa los posibles estados del juego. */
enum class GameState { IDLE, PLAYING, WON, GAMEOVER }

/** Representa las direcciones de movimiento de la serpiente. */
enum class Direction { UP, DOWN, LEFT, RIGHT }

/** El tamaño del tablero (ahora rectangular para mejor visibilidad). */
const val GRID_COLS = 15 // Menos columnas = Celdas mas grandes
const val GRID_ROWS = 25 // Mas filas = Tablero vertical
// const val GRID_SIZE = 20 // DEPRECATED
