package com.termux.app;

import android.app.Application;
import android.content.Context;

import com.termux.BuildConfig;
import com.termux.shared.errors.Error;
import com.termux.shared.file.FileUtils;
import com.termux.shared.logger.Logger;
import com.termux.shared.termux.TermuxBootstrap;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.crash.TermuxCrashUtils;
import com.termux.shared.termux.file.TermuxFileUtils;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.settings.properties.TermuxAppSharedProperties;
import com.termux.shared.termux.shell.command.environment.TermuxShellEnvironment;
import com.termux.shared.termux.shell.am.TermuxAmSocketServer;
import com.termux.shared.termux.shell.TermuxShellManager;
import com.termux.shared.termux.theme.TermuxThemeUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class TermuxApplication extends Application {

    private static final String LOG_TAG = "TermuxApplication";

    public void onCreate() {
        super.onCreate();

        Context context = getApplicationContext();

        // Set crash handler for the app
        TermuxCrashUtils.setDefaultCrashHandler(this);

        // Set log config for the app
        setLogConfig(context);

        Logger.logDebug("Starting Application");

        // Set TermuxBootstrap.TERMUX_APP_PACKAGE_MANAGER and TermuxBootstrap.TERMUX_APP_PACKAGE_VARIANT
        TermuxBootstrap.setTermuxPackageManagerAndVariant(BuildConfig.TERMUX_PACKAGE_VARIANT);

        // Zero-Config Bootstrap: seed ~/.termux/termux.properties with Termux Pro defaults
        // before properties are loaded. An existing user file is never overwritten.
        setupTermuxProDefaults();

        // Init app wide SharedProperties loaded from termux.properties
        TermuxAppSharedProperties properties = TermuxAppSharedProperties.init(context);

        // Init app wide shell manager
        TermuxShellManager shellManager = TermuxShellManager.init(context);

        // Set NightMode.APP_NIGHT_MODE
        TermuxThemeUtils.setAppNightMode(properties.getNightMode());

        // Check and create termux files directory. If failed to access it like in case of secondary
        // user or external sd card installation, then don't run files directory related code
        Error error = TermuxFileUtils.isTermuxFilesDirectoryAccessible(this, true, true);
        boolean isTermuxFilesDirectoryAccessible = error == null;
        if (isTermuxFilesDirectoryAccessible) {
            Logger.logInfo(LOG_TAG, "Termux files directory is accessible");

            error = TermuxFileUtils.isAppsTermuxAppDirectoryAccessible(true, true);
            if (error != null) {
                Logger.logErrorExtended(LOG_TAG, "Create apps/termux-app directory failed\n" + error);
                return;
            }

            // Setup termux-am-socket server
            TermuxAmSocketServer.setupTermuxAmSocketServer(context);
        } else {
            Logger.logErrorExtended(LOG_TAG, "Termux files directory is not accessible\n" + error);
        }

        // Init TermuxShellEnvironment constants and caches after everything has been setup including termux-am-socket server
        TermuxShellEnvironment.init(this);

        if (isTermuxFilesDirectoryAccessible) {
            TermuxShellEnvironment.writeEnvironmentToFile(this);
        }
    }

    /**
     * Create the primary termux.properties file with Termux Pro defaults on first launch.
     *
     * The bootstrap zip only ships an example at $PREFIX/share/examples/termux/termux.properties,
     * so writing here early is safe and won't be overwritten by bootstrap installation.
     * If the user already has a file, it is left untouched.
     */
    private static void setupTermuxProDefaults() {
        File propertiesFile = TermuxConstants.TERMUX_PROPERTIES_PRIMARY_FILE;
        if (propertiesFile.exists()) return;

        Error error = FileUtils.createRegularFile("termux.properties", propertiesFile.getAbsolutePath());
        if (error != null) {
            Logger.logErrorExtended(LOG_TAG, "Failed to create default termux.properties file\n" + error);
            return;
        }

        String defaults =
            "# Termux Pro defaults - generated on first launch, safe to edit.\n" +
            "allow-external-apps = true\n" +
            "extra-keys = [['ESC','/','-','HOME','UP','END','PGUP'],['TAB','CTRL','ALT','LEFT','DOWN','RIGHT','PGDN']]\n" +
            "bell-character = ignore\n" +
            "use-black-ui = true\n" +
            "terminal-cursor-blink-rate = 500\n" +
            "terminal-cursor-style = bar\n";

        try (FileOutputStream out = new FileOutputStream(propertiesFile, false)) {
            out.write(defaults.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Logger.logErrorExtended(LOG_TAG, "Failed to write default termux.properties file: " + e.getMessage());
        }
    }

    public static void setLogConfig(Context context) {
        Logger.setDefaultLogTag(TermuxConstants.TERMUX_APP_NAME);

        // Load the log level from shared preferences and set it to the {@link Logger.CURRENT_LOG_LEVEL}
        TermuxAppSharedPreferences preferences = TermuxAppSharedPreferences.build(context);
        if (preferences == null) return;
        preferences.setLogLevel(null, preferences.getLogLevel());
    }

}
