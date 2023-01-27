package io.xpipe.app.util;

import io.xpipe.app.core.AppI18n;
import io.xpipe.extension.I18n;
import io.xpipe.extension.Translatable;
import io.xpipe.extension.fxcomps.impl.FancyTooltipAugment;
import io.xpipe.extension.prefs.PrefsChoiceValue;
import io.xpipe.extension.util.DynamicOptionsBuilder;
import lombok.SneakyThrows;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ModuleHelper {

    @SneakyThrows
    public static String getCallerModuleName() {
        var callers = CallingClass.INSTANCE.getCallingClasses();
        for (Class<?> caller : callers) {
            if (caller.equals(CallingClass.class)
                    || caller.equals(ModuleHelper.class)
                    || caller.equals(AppI18n.class)
                    || caller.equals(I18n.class)
                    || caller.equals(FancyTooltipAugment.class)
                    || caller.equals(PrefsChoiceValue.class)
                    || caller.equals(Translatable.class)
                    || caller.equals(DynamicOptionsBuilder.class)) {
                continue;
            }
            var split = caller.getModule().getName().split("\\.");
            return split[split.length - 1];
        }
        return "";
    }

    public static boolean isImage() {
        return ModuleHelper.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .getProtocol()
                .equals("jrt");
    }

    @SneakyThrows
    public static Module getEveryoneModule() {
        Method getDeclaredFields0 = Class.class.getDeclaredMethod("getDeclaredFields0", boolean.class);
        getDeclaredFields0.setAccessible(true);
        Field[] fields = (Field[]) getDeclaredFields0.invoke(Module.class, false);
        Field modifiers = null;
        for (Field each : fields) {
            if ("EVERYONE_MODULE".equals(each.getName())) {
                modifiers = each;
                break;
            }
        }
        modifiers.setAccessible(true);
        return (Module) modifiers.get(null);
    }

    @SneakyThrows
    public static void exportAndOpen(String pkg, Module mod) {
        if (mod.isExported(pkg) && mod.isOpen(pkg)) {
            return;
        }

        Method getDeclaredFields0 = Class.class.getDeclaredMethod("getDeclaredMethods0", boolean.class);
        getDeclaredFields0.setAccessible(true);
        Method[] fields = (Method[]) getDeclaredFields0.invoke(Module.class, false);
        Method modifiers = null;
        for (Method each : fields) {
            if ("implAddExportsOrOpens".equals(each.getName())) {
                modifiers = each;
                break;
            }
        }
        modifiers.setAccessible(true);

        var e = getEveryoneModule();
        modifiers.invoke(mod, pkg, e, false, true);
        modifiers.invoke(mod, pkg, e, true, true);
    }

    @SuppressWarnings("removal")
    public static class CallingClass extends SecurityManager {
        public static final CallingClass INSTANCE = new CallingClass();

        public Class<?>[] getCallingClasses() {
            return getClassContext();
        }
    }
}
