package com.oxohang.fanfreeform.xposed;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.MotionEvent;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class FanFreeformHook implements IXposedHookLoadPackage {
    private static final String SYSTEM_UI = "com.android.systemui";
    private static final String EVENT_CONTROLLER =
            "com.android.wm.shell.multitasking.miuimultiwinswitch.miuiwindowdecor.MulWinSwitchEventController";
    private static final String EVENT_HANDLER = EVENT_CONTROLLER + "$EventHandler";
    private static final String FREEFORM_CONTROLLER =
            "com.android.wm.shell.multitasking.miuifreeform.MiuiFreeformModeController";
    private static final String FREEFORM_ANIMATION =
            "com.android.wm.shell.multitasking.miuifreeform.MiuiFreeformModeAnimation";

    @SuppressLint("StaticFieldLeak")
    private static volatile FanRuntime runtime;
    private static volatile Object freeformController;
    private static volatile Object eventProxy;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        if (!SYSTEM_UI.equals(loadPackageParam.packageName)) return;
        Log.i("Loading in SystemUI process=" + loadPackageParam.processName);
        hookFreeformController(loadPackageParam.classLoader);
        hookMiniAnimationTarget(loadPackageParam.classLoader);
        hookInputController(loadPackageParam.classLoader);
    }

    private static void hookInputController(ClassLoader classLoader) {
        try {
            Class<?> controllerClass = XposedHelpers.findClassIfExists(EVENT_CONTROLLER, classLoader);
            Class<?> handlerClass = XposedHelpers.findClassIfExists(EVENT_HANDLER, classLoader);
            if (controllerClass == null || handlerClass == null) {
                Log.i("HyperOS input controller is unavailable; module disabled safely");
                return;
            }
            XposedBridge.hookAllMethods(controllerClass, "createEventReceiver", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (param.args.length == 0 || !(param.args[0] instanceof Context)) return;
                    installRuntime((Context) param.args[0], classLoader, param.thisObject, handlerClass);
                }
            });
            Log.i("Input controller hook installed");
        } catch (Throwable error) {
            Log.e("Input controller hook failed safely", error);
        }
    }

    private static synchronized void installRuntime(Context context, ClassLoader classLoader,
                                                    Object inputController, Class<?> handlerClass) {
        try {
            if (runtime == null) {
                runtime = new FanRuntime(context, classLoader);
                if (freeformController != null) runtime.setFreeformController(freeformController);
            }
            if (eventProxy == null) {
                eventProxy = Proxy.newProxyInstance(handlerClass.getClassLoader(), new Class<?>[]{handlerClass},
                        (proxy, method, args) -> dispatchProxy(proxy, method, args));
                XposedHelpers.callMethod(inputController, "registerEventHandler", eventProxy);
                Log.i("Fan gesture event handler registered");
            }
            runtime.reportInputReady();
        } catch (Throwable error) {
            Log.e("Runtime installation failed safely", error);
        }
    }

    private static Object dispatchProxy(Object proxy, Method method, Object[] args) {
        String name = method.getName();
        if ("onEvent".equals(name) && args != null && args.length >= 2 && args[0] instanceof MotionEvent) {
            FanRuntime active = runtime;
            if (active != null) active.onMotion((MotionEvent) args[0], args[1]);
            return null;
        }
        if ("toString".equals(name)) return "FanFreeformEventHandler";
        if ("hashCode".equals(name)) return System.identityHashCode(proxy);
        if ("equals".equals(name)) return args != null && args.length == 1 && proxy == args[0];
        return null;
    }

    private static void hookFreeformController(ClassLoader classLoader) {
        try {
            Class<?> controllerClass = XposedHelpers.findClassIfExists(FREEFORM_CONTROLLER, classLoader);
            if (controllerClass == null) {
                Log.i("HyperOS freeform controller is unavailable; launch tracking disabled safely");
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
            Log.i("Freeform controller hooks installed");
        } catch (Throwable error) {
            Log.e("Freeform controller hooks failed safely", error);
        }
    }

    private static void hookMiniAnimationTarget(ClassLoader classLoader) {
        try {
            Class<?> animationClass = XposedHelpers.findClassIfExists(
                    FREEFORM_ANIMATION, classLoader);
            if (animationClass == null) {
                Log.i("HyperOS freeform animation is unavailable; right-side correction disabled");
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
}
