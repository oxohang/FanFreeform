package com.oxohang.fanfreeform.xposed;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.os.SystemClock;
import android.view.MotionEvent;

import dalvik.system.DexFile;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class FanFreeformHook implements IXposedHookLoadPackage {
    private static final String SYSTEM_UI = "com.android.systemui";
    private static final String MIUI_HOME = "com.miui.home";
    private static final String EVENT_CONTROLLER =
            "com.android.wm.shell.multitasking.miuimultiwinswitch.miuiwindowdecor.MulWinSwitchEventController";
    private static final String EVENT_HANDLER = EVENT_CONTROLLER + "$EventHandler";
    private static final String FREEFORM_CONTROLLER =
            "com.android.wm.shell.multitasking.miuifreeform.MiuiFreeformModeController";
    private static final String FREEFORM_PIN_HANDLER =
            "com.android.wm.shell.multitasking.miuifreeform.MiuiFreeformModePinHandler";
    private static final String FREEFORM_ANIMATION =
            "com.android.wm.shell.multitasking.miuifreeform.MiuiFreeformModeAnimation";
    private static final String[] INPUT_CONTROLLER_CANDIDATES = {
            EVENT_CONTROLLER,
            "com.android.wm.shell.multitasking.miuimultiwinswitch.MulWinSwitchEventController",
            "com.android.wm.shell.multitasking.miuimultiwinswitch.MiuiMultiWinSwitchEventController"
    };

    @SuppressLint("StaticFieldLeak")
    private static volatile FanRuntime runtime;
    @SuppressLint("StaticFieldLeak")
    private static volatile Context systemUiContext;
    private static volatile Object freeformController;
    private static volatile Object eventProxy;
    private static volatile boolean externalLaunchReceiverInstalled;
    private static volatile boolean applicationAttachHookInstalled;
    private static volatile boolean inputControllerHookInstalled;
    private static volatile boolean freeformControllerHookInstalled;
    private static final Set<String> REPORTED_PROXY_METHODS =
            Collections.synchronizedSet(new LinkedHashSet<>());

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        // Only the main process should own the runtime and exported receivers; child
        // processes would otherwise create duplicate sensors, captures and receivers.
        if (!loadPackageParam.packageName.equals(loadPackageParam.processName)) return;
        if (MIUI_HOME.equals(loadPackageParam.packageName)) {
            hookShortcutHost(loadPackageParam.classLoader);
            hookRecentsClearButton(loadPackageParam.classLoader);
            return;
        }
        if (!SYSTEM_UI.equals(loadPackageParam.packageName)) return;
        Log.i("Loading in SystemUI process=" + loadPackageParam.processName);
        hookApplicationAttach(loadPackageParam.classLoader);
        hookFreeformController(loadPackageParam.classLoader);
        hookFreeformPinHandler(loadPackageParam.classLoader);
        hookMiniAnimationTarget(loadPackageParam.classLoader);
        hookEdgePinRestoreTarget(loadPackageParam.classLoader);
        hookShortcutEnterAnimation(loadPackageParam.classLoader);
        hookLandscapeWindowShape(loadPackageParam.classLoader);
        hookInputController(loadPackageParam.classLoader);
        hookStatusBarTouchEntry(loadPackageParam.classLoader);
        hookShadeExpansion(loadPackageParam.classLoader);
        hookControlCenterExpansion(loadPackageParam.classLoader);
        hookAuthoritativePanelState(loadPackageParam.classLoader);
    }

    private static synchronized void hookApplicationAttach(ClassLoader classLoader) {
        if (applicationAttachHookInstalled) return;
        try {
            XposedBridge.hookAllMethods(Application.class, "attach", new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam param) {
                    if (param.args.length == 0 || !(param.args[0] instanceof Context)) return;
                    Context context = (Context) param.args[0];
                    systemUiContext = context.getApplicationContext() == null
                            ? context : context.getApplicationContext();
                    Log.attachReporter(systemUiContext);
                    Log.i("SystemUI application attached package="
                            + systemUiContext.getPackageName());
                    ensureRuntime(systemUiContext, classLoader);
                    probeSystemUiInterfaces(classLoader);
                    // The first class lookup can happen before Application.attach(). Retry
                    // here, and then scan the ROM dex for renamed HyperOS 2 classes below.
                    hookFreeformController(classLoader);
                    hookInputController(classLoader);
                    scanRomClassesAsync(classLoader);
                }
            });
            applicationAttachHookInstalled = true;
            Log.i("SystemUI application attach diagnostic hook installed");
        } catch (Throwable error) {
            Log.e("SystemUI application attach diagnostic hook failed", error);
        }
    }

    private static void probeSystemUiInterfaces(ClassLoader classLoader) {
        probeClass(classLoader, "inputController", INPUT_CONTROLLER_CANDIDATES);
        probeClass(classLoader, "inputHandler", EVENT_HANDLER);
        probeClass(classLoader, "freeformController", FREEFORM_CONTROLLER);
        probeClass(classLoader, "freeformPinHandler", FREEFORM_PIN_HANDLER);
        probeClass(classLoader, "freeformAnimation", FREEFORM_ANIMATION);
        Log.i("SystemUI probe complete uptime=" + SystemClock.uptimeMillis());
    }

    private static void probeClass(ClassLoader classLoader, String role, String... names) {
        for (String name : names) {
            Class<?> type = XposedHelpers.findClassIfExists(name, classLoader);
            if (type == null) {
                Log.i("SystemUI probe " + role + " missing=" + name);
                continue;
            }
            Log.i("SystemUI probe " + role + " present=" + name
                    + " methods=" + methodSummary(type));
            return;
        }
    }

    private static String methodSummary(Class<?> type) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        try {
            for (Method method : type.getDeclaredMethods()) {
                names.add(method.getName() + "/" + method.getParameterTypes().length);
            }
        } catch (Throwable error) {
            return "<unavailable:" + error.getClass().getSimpleName() + ">";
        }
        if (names.isEmpty()) return "[]";
        List<String> sorted = new ArrayList<>(names);
        Collections.sort(sorted);
        if (sorted.size() > 40) sorted = sorted.subList(0, 40);
        return sorted.toString();
    }

    private static void scanRomClassesAsync(ClassLoader classLoader) {
        Context context = systemUiContext;
        boolean needInput = !inputControllerHookInstalled;
        boolean needFreeform = !freeformControllerHookInstalled;
        if (context == null || (!needInput && !needFreeform)) return;
        Thread probe = new Thread(() -> {
            try {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
            } catch (Throwable ignored) { }
            RomClassCandidates candidates = findRelevantRomClasses(
                    context, needInput, needFreeform);
            if (needInput) Log.i("SystemUI ROM input candidates=" + candidates.input);
            if (needFreeform) {
                Log.i("SystemUI ROM freeform candidates=" + candidates.freeform);
            }

            if (!inputControllerHookInstalled) {
                for (String candidate : candidates.input) {
                    if (candidate.contains("$") || !candidate.endsWith("EventController")) {
                        continue;
                    }
                    Log.i("Retrying input hook with ROM candidate=" + candidate);
                    if (hookInputController(classLoader, candidate)) break;
                }
                FanRuntime active = runtime;
                if (!inputControllerHookInstalled && active != null) {
                    active.reportNativeInputMissing();
                }
            }
            if (!freeformControllerHookInstalled) {
                for (String candidate : candidates.freeform) {
                    if (candidate.contains("$")
                            || !candidate.endsWith("FreeformModeController")) continue;
                    Log.i("Retrying freeform hook with ROM candidate=" + candidate);
                    hookFreeformController(classLoader, candidate);
                    if (freeformControllerHookInstalled) break;
                }
            }
        }, "hypergesture-rom-probe");
        probe.setDaemon(true);
        probe.start();
    }

    private static RomClassCandidates findRelevantRomClasses(Context context,
                                                              boolean needInput,
                                                              boolean needFreeform) {
        LinkedHashSet<String> input = new LinkedHashSet<>();
        LinkedHashSet<String> freeform = new LinkedHashSet<>();
        ApplicationInfo info = context.getApplicationInfo();
        List<String> paths = new ArrayList<>();
        if (info.sourceDir != null) paths.add(info.sourceDir);
        if (info.splitSourceDirs != null) {
            Collections.addAll(paths, info.splitSourceDirs);
        }
        for (String path : paths) {
            if ((!needInput || input.size() >= 80)
                    && (!needFreeform || freeform.size() >= 80)) break;
            DexFile dex = null;
            try {
                dex = new DexFile(path);
                Enumeration<String> entries = dex.entries();
                while (entries.hasMoreElements()
                        && ((needInput && input.size() < 80)
                        || (needFreeform && freeform.size() < 80))) {
                    String name = entries.nextElement();
                    String lower = name.toLowerCase(java.util.Locale.US);
                    if (needInput && input.size() < 80
                            && lower.contains("eventcontroller")
                            && (lower.contains("mulwin") || lower.contains("multiwin")
                            || lower.contains("multitask"))) {
                        input.add(name);
                    }
                    if (needFreeform && freeform.size() < 80
                            && lower.contains("freeform")
                            && (lower.contains("controller") || lower.contains("animation")
                            || lower.contains("manager"))) {
                        freeform.add(name);
                    }
                }
            } catch (Throwable error) {
                Log.e("SystemUI ROM class scan failed path=" + path, error);
            } finally {
                if (dex != null) {
                    try { dex.close(); } catch (IOException ignored) { }
                }
            }
        }
        return new RomClassCandidates(new ArrayList<>(input), new ArrayList<>(freeform));
    }

    private static final class RomClassCandidates {
        final List<String> input;
        final List<String> freeform;

        RomClassCandidates(List<String> input, List<String> freeform) {
            this.input = input;
            this.freeform = freeform;
        }
    }

    private static void hookRecentsClearButton(ClassLoader classLoader) {
        try {
            Class<?> container = XposedHelpers.findClassIfExists(
                    "com.miui.home.recents.views.RecentsContainer", classLoader);
            if (container == null) {
                Log.i("HyperOS recents clear-button hook skipped: container class missing");
                return;
            }
            XC_MethodHook updater = new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam param) {
                    RecentsClearButtonRuntime.attach(param.thisObject);
                }
            };
            XposedBridge.hookAllMethods(container, "onFinishInflate", updater);
            XposedBridge.hookAllMethods(container, "onAttachedToWindow", updater);
            Log.i("HyperOS recents clear-button hook installed");
        } catch (Throwable error) {
            Log.e("HyperOS recents clear-button hook failed safely", error);
        }
    }

    private static void hookStatusBarTouchEntry(ClassLoader classLoader) {
        hookSystemPanelTouchView(classLoader,
                "com.android.systemui.statusbar.window.StatusBarWindowView",
                "status bar");
        hookSystemPanelTouchView(classLoader,
                "com.android.systemui.shade.NotificationShadeWindowView",
                "notification shade window");
    }

    private static void hookSystemPanelTouchView(ClassLoader classLoader,
                                                 String className, String source) {
        try {
            Class<?> statusBarWindow = XposedHelpers.findClassIfExists(
                    className, classLoader);
            if (statusBarWindow == null) return;
            XposedBridge.hookAllMethods(statusBarWindow, "dispatchTouchEvent",
                    new XC_MethodHook() {
                        @Override protected void beforeHookedMethod(MethodHookParam param) {
                            if (runtime == null || param.args.length == 0
                                    || !(param.args[0] instanceof MotionEvent)) return;
                            MotionEvent event = (MotionEvent) param.args[0];
                            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                                runtime.onSystemPanelTouchStarted(source);
                            }
                        }
                    });
            Log.i(source + " touch-entry hook installed");
        } catch (Throwable error) {
            Log.e(source + " touch-entry hook failed safely", error);
        }
    }

    private static void hookShortcutHost(ClassLoader classLoader) {
        XC_MethodHook installer = new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                if (param.args.length == 0 || !(param.args[0] instanceof Context)) return;
                ShortcutHostRuntime.install((Context) param.args[0]);
            }
        };
        XposedBridge.hookAllMethods(Application.class, "attach", installer);
        XposedBridge.hookAllMethods(Instrumentation.class, "callApplicationOnCreate",
                new XC_MethodHook() {
                    @Override protected void beforeHookedMethod(MethodHookParam param) {
                        if (param.args.length == 0 || !(param.args[0] instanceof Application)) return;
                        ShortcutHostRuntime.install((Application) param.args[0]);
                    }
                });
        Log.i("Shortcut host hook installed in MiuiHome");
    }

    private static void hookShadeExpansion(ClassLoader classLoader) {
        try {
            Class<?> manager = XposedHelpers.findClassIfExists(
                    "com.android.systemui.shade.ShadeExpansionStateManager", classLoader);
            if (manager == null) return;
            XposedBridge.hookAllMethods(manager, "onPanelExpansionChanged", new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam param) {
                    if (runtime == null || param.args.length < 2
                            || !(param.args[0] instanceof Number)
                            || !(param.args[1] instanceof Boolean)) return;
                    runtime.onShadeExpansionChanged(((Number) param.args[0]).floatValue(),
                            (Boolean) param.args[1]);
                }
            });
            Log.i("Notification shade expansion hook installed");
        } catch (Throwable error) {
            Log.e("Notification shade hook failed safely", error);
        }
    }

    private static void hookControlCenterExpansion(ClassLoader classLoader) {
        try {
            Class<?> eventHandler = XposedHelpers.findClassIfExists(
                    "com.miui.systemui.controlcenter.container.ControlCenterEventHandlerImpl",
                    classLoader);
            if (eventHandler != null) {
                XposedBridge.hookAllMethods(eventHandler, "handleExpandEvent",
                        new XC_MethodHook() {
                            @Override protected void afterHookedMethod(MethodHookParam param) {
                                if (runtime == null || param.args.length == 0
                                        || !(param.args[0] instanceof MotionEvent)) return;
                                MotionEvent event = (MotionEvent) param.args[0];
                                if (event.getActionMasked() == MotionEvent.ACTION_DOWN
                                        && Boolean.TRUE.equals(param.getResult())) {
                                    runtime.onControlCenterExpansionChanged(true);
                                }
                            }
                        });
                Log.i("Control center touch-entry hook installed");
            }
            Class<?> controlCenter = XposedHelpers.findClassIfExists(
                    "com.miui.systemui.controlcenter.ControlCenterImpl", classLoader);
            if (controlCenter != null) {
                XposedBridge.hookAllMethods(controlCenter, "expand", new XC_MethodHook() {
                    @Override protected void beforeHookedMethod(MethodHookParam param) {
                        if (runtime != null) runtime.onControlCenterExpansionChanged(true);
                    }
                });
                XposedBridge.hookAllMethods(controlCenter, "switchShow", new XC_MethodHook() {
                    @Override protected void beforeHookedMethod(MethodHookParam param) {
                        if (runtime != null) runtime.onControlCenterExpansionChanged(true);
                    }
                });
                XposedBridge.hookAllMethods(controlCenter, "collapse", new XC_MethodHook() {
                    @Override protected void afterHookedMethod(MethodHookParam param) {
                        if (runtime != null) runtime.onControlCenterExpansionChanged(false);
                    }
                });
                XposedBridge.hookAllMethods(controlCenter, "switchHide", new XC_MethodHook() {
                    @Override protected void afterHookedMethod(MethodHookParam param) {
                        if (runtime != null) runtime.onControlCenterExpansionChanged(false);
                    }
                });
            }
            Class<?> panelInjector = XposedHelpers.findClassIfExists(
                    "com.android.systemui.shade.NotificationPanelViewControllerInjector", classLoader);
            if (panelInjector != null) {
                XposedBridge.hookAllMethods(panelInjector, "onControlCenterAppearChanged",
                        new XC_MethodHook() {
                            @Override protected void afterHookedMethod(MethodHookParam param) {
                                if (runtime == null || param.args.length == 0
                                        || !(param.args[0] instanceof Boolean)) return;
                                runtime.onControlCenterExpansionChanged((Boolean) param.args[0]);
                            }
                        });
            }
            Class<?> headerController = XposedHelpers.findClassIfExists(
                    "com.android.systemui.controlcenter.shade.ControlCenterHeaderExpandController",
                    classLoader);
            if (headerController != null) {
                XposedBridge.hookAllMethods(headerController, "onExpansionChanged",
                        new XC_MethodHook() {
                            @Override protected void afterHookedMethod(MethodHookParam param) {
                                if (runtime == null || param.args.length == 0
                                        || !(param.args[0] instanceof Number)) return;
                                // This controller can continue reporting 0 while its panel is
                                // visible, so only treat a positive progress as an enter signal.
                                if (((Number) param.args[0]).floatValue() > 0.01f) {
                                    runtime.onControlCenterExpansionChanged(true);
                                }
                            }
                        });
            }
            Log.i("Control center expansion hooks installed");
        } catch (Throwable error) {
            Log.e("Control center expansion hooks failed safely", error);
        }
    }

    private static void hookAuthoritativePanelState(ClassLoader classLoader) {
        XC_MethodHook callback = new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                FanRuntime active = runtime;
                if (active == null) return;
                try {
                    Object observer = XposedHelpers.getObjectField(param.thisObject, "this$0");
                    boolean notification = XposedHelpers.getBooleanField(
                            observer, "mNotificationPanelExpand");
                    boolean control = XposedHelpers.getBooleanField(
                            observer, "mControlCenterExpand");
                    active.onAuthoritativeSystemPanelState(notification, control);
                } catch (Throwable error) {
                    Log.e("Cannot read authoritative system panel state", error);
                }
            }
        };
        try {
            Class<?> controlListener = XposedHelpers.findClassIfExists(
                    "com.miui.systemui.controller.GestureObserver$3", classLoader);
            Class<?> notificationListener = XposedHelpers.findClassIfExists(
                    "com.miui.systemui.controller.GestureObserver$4", classLoader);
            if (controlListener != null) {
                XposedBridge.hookAllMethods(controlListener, "onExpandChanged", callback);
            }
            if (notificationListener != null) {
                XposedBridge.hookAllMethods(notificationListener, "onPanelExpanded", callback);
            }
            Class<?> stateCommit = XposedHelpers.findClassIfExists(
                    "com.miui.systemui.controller.GestureObserver$$ExternalSyntheticLambda2",
                    classLoader);
            if (stateCommit != null) {
                XposedBridge.hookAllMethods(stateCommit, "run", new XC_MethodHook() {
                    @Override protected void afterHookedMethod(MethodHookParam param) {
                        FanRuntime active = runtime;
                        if (active == null) return;
                        try {
                            boolean control = XposedHelpers.getBooleanField(
                                    param.thisObject, "f$1");
                            boolean notification = XposedHelpers.getBooleanField(
                                    param.thisObject, "f$2");
                            active.onAuthoritativeSystemPanelState(notification, control);
                        } catch (Throwable error) {
                            Log.e("Cannot read committed system panel state", error);
                        }
                    }
                });
            }
            Log.i("Authoritative system panel state hooks installed");
        } catch (Throwable error) {
            Log.e("Authoritative system panel state hooks failed safely", error);
        }
    }

    private static void hookInputController(ClassLoader classLoader) {
        for (String candidate : INPUT_CONTROLLER_CANDIDATES) {
            if (hookInputController(classLoader, candidate)) return;
        }
        Log.i("HyperOS input controller is unavailable; corner fallback active; ROM scan pending");
        FanRuntime active = runtime;
        if (active != null) active.reportNativeInputMissing();
    }

    private static synchronized boolean hookInputController(ClassLoader classLoader,
                                                             String controllerName) {
        if (inputControllerHookInstalled) return true;
        try {
            Class<?> controllerClass = XposedHelpers.findClassIfExists(controllerName, classLoader);
            if (controllerClass == null) return false;
            Class<?> handlerClass = findHandlerClass(controllerClass, classLoader, controllerName);
            if (handlerClass == null) {
                Log.i("SystemUI input controller found but handler is missing controller="
                        + controllerName + " nested=" + nestedClassSummary(controllerClass));
                return false;
            }
            String createMethod = findEventReceiverMethod(controllerClass);
            String registerMethod = findEventHandlerRegistrationMethod(controllerClass, handlerClass);
            if (createMethod == null || registerMethod == null) {
                Log.i("SystemUI input methods missing controller=" + controllerName
                        + " create=" + createMethod + " register=" + registerMethod
                        + " methods=" + methodSummary(controllerClass));
                return false;
            }
            int hookCount = XposedBridge.hookAllMethods(controllerClass, createMethod,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            Context context = null;
                            for (Object argument : param.args) {
                                if (argument instanceof Context) {
                                    context = (Context) argument;
                                    break;
                                }
                            }
                            if (context != null) {
                                installNativeInput(context, classLoader, param.thisObject,
                                        handlerClass, registerMethod);
                            } else {
                                Log.i("SystemUI input receiver callback has no Context method="
                                        + createMethod + " args=" + param.args.length);
                            }
                        }
                    }).size();
            if (hookCount == 0) {
                Log.i("SystemUI input receiver method was not hookable controller="
                        + controllerName + " method=" + createMethod);
                return false;
            }
            inputControllerHookInstalled = true;
            Log.i("Input controller hook installed controller=" + controllerName
                    + " create=" + createMethod + " register=" + registerMethod
                    + " hooks=" + hookCount);
            return true;
        } catch (Throwable error) {
            Log.e("Input controller hook failed safely controller=" + controllerName, error);
            return false;
        }
    }

    private static Class<?> findHandlerClass(Class<?> controllerClass, ClassLoader classLoader,
                                             String controllerName) {
        Class<?> exact = XposedHelpers.findClassIfExists(controllerName + "$EventHandler",
                classLoader);
        if (exact != null) return exact;
        try {
            for (Class<?> nested : controllerClass.getDeclaredClasses()) {
                if (nested.getName().toLowerCase(java.util.Locale.US).contains("eventhandler")) {
                    return nested;
                }
            }
        } catch (Throwable error) {
            Log.e("Cannot inspect input controller nested classes controller="
                    + controllerName, error);
        }
        return null;
    }

    private static String nestedClassSummary(Class<?> type) {
        try {
            List<String> names = new ArrayList<>();
            for (Class<?> nested : type.getDeclaredClasses()) names.add(nested.getName());
            Collections.sort(names);
            return names.toString();
        } catch (Throwable error) {
            return "<unavailable:" + error.getClass().getSimpleName() + ">";
        }
    }

    private static String findEventReceiverMethod(Class<?> controllerClass) {
        try {
            for (Method method : controllerClass.getDeclaredMethods()) {
                if ("createEventReceiver".equals(method.getName())) return method.getName();
            }
            for (Method method : controllerClass.getDeclaredMethods()) {
                String name = method.getName().toLowerCase(java.util.Locale.US);
                if (name.contains("eventreceiver") && name.contains("create")) {
                    return method.getName();
                }
            }
        } catch (Throwable error) {
            Log.e("Cannot inspect input receiver methods class=" + controllerClass.getName(), error);
        }
        return null;
    }

    private static String findEventHandlerRegistrationMethod(Class<?> controllerClass,
                                                              Class<?> handlerClass) {
        try {
            for (Method method : controllerClass.getDeclaredMethods()) {
                if ("registerEventHandler".equals(method.getName())
                        && acceptsHandler(method, handlerClass)) return method.getName();
            }
            for (Method method : controllerClass.getDeclaredMethods()) {
                String name = method.getName().toLowerCase(java.util.Locale.US);
                if (name.contains("register") && acceptsHandler(method, handlerClass)) {
                    return method.getName();
                }
            }
        } catch (Throwable error) {
            Log.e("Cannot inspect input handler registration class="
                    + controllerClass.getName(), error);
        }
        return null;
    }

    private static boolean acceptsHandler(Method method, Class<?> handlerClass) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        return parameterTypes.length == 1
                && parameterTypes[0] != Object.class
                && parameterTypes[0].isAssignableFrom(handlerClass);
    }

    private static synchronized FanRuntime ensureRuntime(Context context,
                                                         ClassLoader classLoader) {
        if (runtime != null) return runtime;
        Context appContext = context.getApplicationContext();
        if (appContext == null) appContext = context;
        systemUiContext = appContext;
        runtime = new FanRuntime(appContext, classLoader);
        if (freeformController != null) runtime.setFreeformController(freeformController);
        installExternalLaunchReceiver(appContext);
        Log.i("SystemUI runtime initialized with corner input fallback");
        return runtime;
    }

    private static synchronized void installNativeInput(Context context, ClassLoader classLoader,
                                                        Object inputController,
                                                        Class<?> handlerClass,
                                                        String registerMethod) {
        FanRuntime active = ensureRuntime(context, classLoader);
        if (eventProxy != null) return;
        if (!active.beginNativeInputRegistration()) return;
        try {
            Object proxy = Proxy.newProxyInstance(handlerClass.getClassLoader(),
                    new Class<?>[]{handlerClass},
                    (instance, method, args) -> dispatchProxy(instance, method, args));
            XposedHelpers.callMethod(inputController, registerMethod, proxy);
            eventProxy = proxy;
            active.completeNativeInputRegistration();
            Log.i("Fan gesture event handler registered method=" + registerMethod);
        } catch (Throwable error) {
            eventProxy = null;
            active.failNativeInputRegistration();
            Log.e("Native input registration failed safely; corner fallback restored", error);
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private static synchronized void installExternalLaunchReceiver(Context context) {
        if (externalLaunchReceiverInstalled) return;
        try {
            IntentFilter filter = new IntentFilter(ExternalLaunchContract.ACTION_LAUNCH);
            BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override public void onReceive(Context receiverContext, Intent intent) {
                    if (!ExternalLaunchContract.ACTION_LAUNCH.equals(intent.getAction())) return;
                    if (!ExternalLaunchContract.FLY_PACKAGE.equals(
                            intent.getStringExtra(ExternalLaunchContract.EXTRA_SOURCE_PACKAGE))
                            || !BroadcastSenderValidator.isFromPackage(
                            receiverContext, this, ExternalLaunchContract.FLY_PACKAGE)) {
                        Log.i("Ignored external launch request from unknown source");
                        return;
                    }
                    FanRuntime active = runtime;
                    if (active == null) return;
                    active.launchExternalRequest(
                            intent.getStringExtra(ExternalLaunchContract.EXTRA_PACKAGE),
                            intent.getStringExtra(ExternalLaunchContract.EXTRA_COMPONENT),
                            intent.getStringExtra(ExternalLaunchContract.EXTRA_INTENT_URI));
                }
            };
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
            } else {
                context.registerReceiver(receiver, filter);
            }
            externalLaunchReceiverInstalled = true;
            Log.i("External launch bridge registered for fly");
        } catch (Throwable error) {
            Log.e("External launch bridge registration failed", error);
        }
    }

    private static Object dispatchProxy(Object proxy, Method method, Object[] args) {
        String name = method.getName();
        if ("toString".equals(name)) return "FanFreeformEventHandler";
        if ("hashCode".equals(name)) return System.identityHashCode(proxy);
        if ("equals".equals(name)) return args != null && args.length == 1 && proxy == args[0];

        MotionEvent motionEvent = null;
        Object inputMonitor = null;
        if (args != null) {
            for (Object argument : args) {
                if (argument instanceof MotionEvent) {
                    motionEvent = (MotionEvent) argument;
                } else if (argument != null && inputMonitor == null) {
                    inputMonitor = argument;
                }
            }
        }
        if (motionEvent != null) {
            FanRuntime active = runtime;
            if (active != null) active.postMotionEvent(motionEvent, inputMonitor);
            if (!"onEvent".equals(name) && REPORTED_PROXY_METHODS.add(method.toString())) {
                Log.i("Nonstandard input event callback method=" + method);
            }
            return null;
        }
        if (REPORTED_PROXY_METHODS.add(method.toString())) {
            Log.i("Unhandled input handler callback method=" + method);
        }
        return null;
    }

    private static void hookFreeformController(ClassLoader classLoader) {
        hookFreeformController(classLoader, FREEFORM_CONTROLLER);
    }

    private static synchronized void hookFreeformController(ClassLoader classLoader,
                                                             String controllerName) {
        if (freeformControllerHookInstalled) return;
        try {
            Class<?> controllerClass = XposedHelpers.findClassIfExists(controllerName, classLoader);
            if (controllerClass == null) {
                Log.i("HyperOS freeform controller is unavailable controller=" + controllerName
                        + "; launch tracking disabled safely");
                return;
            }
            XposedBridge.hookAllConstructors(controllerClass, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    freeformController = param.thisObject;
                    FanRuntime active = runtime;
                    if (active != null) active.setFreeformController(param.thisObject);
                }
            });
            XposedBridge.hookAllMethods(controllerClass, "onTaskAppeared", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (runtime != null && param.args.length > 0 && param.args[0] instanceof Number) {
                        runtime.onTaskAppeared(((Number) param.args[0]).intValue());
                    }
                }
            });
            XposedBridge.hookAllMethods(controllerClass, "onTaskModeChanged", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (runtime != null && param.args.length > 0) runtime.onTaskInfo(param.args[0]);
                }
            });
            XposedBridge.hookAllMethods(controllerClass, "onTaskVanished", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (runtime != null && param.args.length > 0 && param.args[0] instanceof Number) {
                        runtime.onTaskVanished(((Number) param.args[0]).intValue());
                    }
                }
            });
            XC_MethodHook leavingInteractiveStateHook = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    FanRuntime active = runtime;
                    if (active == null) return;
                    active.onNativeFreeformLeavingInteractiveState(param.method.getName());
                }
            };
            XposedBridge.hookAllMethods(controllerClass, "startPinAnimation",
                    leavingInteractiveStateHook);
            XposedBridge.hookAllMethods(controllerClass, "fromFreeformToMini",
                    leavingInteractiveStateHook);
            XposedBridge.hookAllMethods(controllerClass, "onImeVisibilityChanged",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (runtime == null || param.args.length < 2
                                    || !(param.args[0] instanceof Boolean)
                                    || !(param.args[1] instanceof Number)) return;
                            runtime.onImeVisibilityChanged((Boolean) param.args[0],
                                    ((Number) param.args[1]).intValue());
                        }
                    });
            XposedBridge.hookAllMethods(controllerClass, "handleTouchEvent",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            FanRuntime active = runtime;
                            if (active == null || param.args.length == 0
                                    || !(param.args[0] instanceof MotionEvent)) return;
                            active.onNativeFreeformMotion((MotionEvent) param.args[0]);
                        }
            });
            freeformControllerHookInstalled = true;
            Log.i("Freeform controller hooks installed controller=" + controllerName);
        } catch (Throwable error) {
            Log.e("Freeform controller hooks failed safely controller=" + controllerName, error);
        }
    }

    private static void hookFreeformPinHandler(ClassLoader classLoader) {
        try {
            Class<?> handlerClass = XposedHelpers.findClassIfExists(
                    FREEFORM_PIN_HANDLER, classLoader);
            if (handlerClass == null) {
                Log.i("HyperOS freeform pin handler is unavailable; direct pin tracking disabled");
                return;
            }
            XposedBridge.hookAllMethods(handlerClass, "startPinAnimation",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            FanRuntime active = runtime;
                            if (active != null) {
                                active.onNativeFreeformLeavingInteractiveState(
                                        "pin handler startPinAnimation");
                            }
                        }
                    });
            Log.i("Freeform pin handler hook installed");
        } catch (Throwable error) {
            Log.e("Freeform pin handler hook failed safely", error);
        }
    }

    private static void hookMiniAnimationTarget(ClassLoader classLoader) {
        try {
            Class<?> animationClass = XposedHelpers.findClassIfExists(
                    FREEFORM_ANIMATION, classLoader);
            if (animationClass == null) {
                Log.i("HyperOS freeform animation unavailable; mini target correction disabled");
                return;
            }
            XposedBridge.hookAllMethods(animationClass, "startGestureAnimation",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            FanRuntime active = runtime;
                            if (active == null || param.args.length < 3
                                    || !(param.args[0] instanceof Number)) return;
                            active.adjustMiniTargetIfNeeded(
                                    ((Number) param.args[0]).intValue(),
                                    param.args[1], param.args[2]);
                        }
                    });
            Log.i("Freeform mini animation target hook installed");
        } catch (Throwable error) {
            Log.e("Freeform mini animation target hook failed safely", error);
        }
    }

    private static void hookEdgePinRestoreTarget(ClassLoader classLoader) {
        try {
            Class<?> pinHandlerClass = XposedHelpers.findClassIfExists(
                    FREEFORM_PIN_HANDLER, classLoader);
            if (pinHandlerClass != null) {
                XposedBridge.hookAllMethods(pinHandlerClass, "setUnPinAnimInfo",
                        new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                FanRuntime active = runtime;
                                if (active == null || param.args.length < 5
                                        || !(param.args[1] instanceof Number)) return;
                                int animationType = ((Number) param.args[1]).intValue();
                                if (animationType == 8 || animationType == 9) {
                                    active.adjustEdgePinRestoreTarget(
                                            param.args[0], param.args[4], null);
                                }
                            }
                        });
            }

            Class<?> animationClass = XposedHelpers.findClassIfExists(
                    FREEFORM_ANIMATION, classLoader);
            if (animationClass != null) {
                XposedBridge.hookAllMethods(animationClass, "startUnpinShellTransition",
                        new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) {
                                FanRuntime active = runtime;
                                if (active == null || param.args.length < 3) return;
                                active.adjustEdgePinRestoreTarget(
                                        param.args[0], null, param.args[2]);
                            }
                        });
            }
            Log.i("Freeform edge-pin restore target hooks installed");
        } catch (Throwable error) {
            Log.e("Freeform edge-pin restore target hooks failed safely", error);
        }
    }

    private static void hookShortcutEnterAnimation(ClassLoader classLoader) {
        try {
            Class<?> animationClass = XposedHelpers.findClassIfExists(
                    FREEFORM_ANIMATION, classLoader);
            if (animationClass == null) return;
            XC_MethodHook hook = new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    FanRuntime active = runtime;
                    if (active == null) return;
                    Object change = null;
                    Object taskInfo = null;
                    for (Object argument : param.args) {
                        if (argument == null) continue;
                        String name = argument.getClass().getName();
                        if (name.endsWith("TransitionInfo$Change")) {
                            change = argument;
                        } else if (name.endsWith("MiuiFreeformModeTaskInfo")) {
                            taskInfo = argument;
                        }
                    }
                    if (change != null && taskInfo != null) {
                        active.prepareShortcutEnterAnimation(change, taskInfo);
                    }
                }
            };
            XposedBridge.hookAllMethods(animationClass,
                    "startMoveToFrontAnimation", hook);
            XposedBridge.hookAllMethods(animationClass,
                    "startFullScreenToFreeformAnimation", hook);
            Log.i("Shortcut enter animation alignment hooks installed");
        } catch (Throwable error) {
            Log.e("Shortcut enter animation alignment hooks failed safely", error);
        }
    }

    private static void hookLandscapeWindowShape(ClassLoader classLoader) {
        try {
            Class<?> animationClass = XposedHelpers.findClassIfExists(
                    FREEFORM_ANIMATION, classLoader);
            int orientationHookCount = 0;
            if (animationClass != null) {
                XC_MethodHook orientationHook = new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        FanRuntime active = runtime;
                        if (active == null || param.args.length == 0) return;
                        if (active.stabilizeLandscapeWindowShape(param.args[0])) {
                            param.setResult(null);
                        }
                    }
                };
                orientationHookCount += XposedBridge.hookAllMethods(animationClass,
                        "adjustFreeformOrientationIfNeed", orientationHook).size();
                orientationHookCount += XposedBridge.hookAllMethods(animationClass,
                        "lambda$startFreeformOrientationChangeShellTransition$4",
                        orientationHook).size();
            }
            int requestHookCount = 0;
            Class<?> controllerClass = XposedHelpers.findClassIfExists(
                    FREEFORM_CONTROLLER, classLoader);
            if (controllerClass != null) {
                requestHookCount = XposedBridge.hookAllMethods(controllerClass,
                        "setRequestedOrientation", new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) {
                                FanRuntime active = runtime;
                                if (active == null || param.args.length < 2
                                        || !(param.args[0] instanceof Number)
                                        || !(param.args[1] instanceof Number)) return;
                                int taskId = ((Number) param.args[0]).intValue();
                                int orientation = ((Number) param.args[1]).intValue();
                                if (active.suppressLandscapeShapeChange(taskId, orientation)) {
                                    param.setResult(null);
                                }
                            }
                        }).size();
            }
            Log.i("Configured landscape freeform shape hooks installed orientation="
                    + orientationHookCount + " requested=" + requestHookCount);
        } catch (Throwable error) {
            Log.e("Configured landscape freeform shape hooks failed safely", error);
        }
    }

}
