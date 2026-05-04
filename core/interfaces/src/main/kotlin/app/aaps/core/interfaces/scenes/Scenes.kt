package app.aaps.core.interfaces.scenes

import app.aaps.core.interfaces.configuration.ConfigExportImport

/**
 * Domain owner of scene definitions. Implements [ConfigExportImport] so the scene list
 * preference travels in its own block of the running configuration doc.
 */
interface Scenes : ConfigExportImport
