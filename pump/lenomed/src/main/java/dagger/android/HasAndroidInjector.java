package dagger.android;

/**
 * TEMPORARY compilation shim for the decompiled Lenomed pump driver.
 *
 * The driver was reverse-engineered from AAPS 4.4.2.6, which used Dagger-Android
 * (dagger.android.*). The dev branch (4.0.0-dev-d) replaced Dagger with Metro, so
 * dagger-android is no longer on the classpath. This interface reproduces only the
 * surface the ported code actually touches: it stores / passes an injector but never
 * calls androidInjector(), so an empty interface is enough to compile.
 *
 * NOTE: this is NOT functional DI wiring. At runtime the @Inject fields will be
 * uninitialised. A real Metro port must delete this shim and wire dependencies via
 * constructor injection / Metro @Inject + @ContributesIntoMap.
 */
public interface HasAndroidInjector {
}
